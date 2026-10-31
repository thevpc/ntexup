package net.thevpc.ntexup.engine.renderer;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxGeometryIndex;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRenderSession;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.nuts.text.NMsg;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Per-page render session: the geometry index plus a queue of node renderers
 * (connectors) that need the whole page to be laid out before they can run.
 */
public class NTxRenderSessionImpl implements NTxRenderSession {
    private final NTxGraphics pageGraphics;
    private final NTxGeometryIndex geometry;
    private final List<DeferredEntry> deferred = new ArrayList<>();

    public NTxRenderSessionImpl(NTxGraphics pageGraphics) {
        this.pageGraphics = pageGraphics;
        this.geometry = new NTxGeometryIndexImpl();
    }

    @Override
    public NTxGeometryIndex geometry() {
        return geometry;
    }

    @Override
    public NTxGraphics pageGraphics() {
        return pageGraphics;
    }

    @Override
    public void defer(NTxNode owner, NTxRendererContext ctx, AffineTransform ancestorsToRoot,
                      Consumer<NTxRendererContext> draw) {
        if (owner != null) {
            for (DeferredEntry e : deferred) {
                if (e.owner == owner) {
                    return;
                }
            }
        }
        deferred.add(new DeferredEntry(owner, ctx, ancestorsToRoot, draw));
    }

    @Override
    public void drainDeferred() {
        // A deferred renderer may itself defer another one (nested connectors);
        // keep draining until the queue is empty.
        while (!deferred.isEmpty()) {
            List<DeferredEntry> list = new ArrayList<>(deferred);
            deferred.clear();
            for (DeferredEntry e : list) {
                try {
                    NTxRendererContext c = e.ctx.withGraphics(pageGraphics);
                    e.draw.accept(c);
                } catch (RuntimeException ex) {
                    e.ctx.engine().log().log(NMsg.ofC("unable to render deferred node %s : %s",
                            e.owner.type(), ex).asFineFail(ex));
                }
            }
        }
    }

    private static final class DeferredEntry {
        final NTxNode owner;
        final NTxRendererContext ctx;
        final AffineTransform ancestorsToRoot;
        final Consumer<NTxRendererContext> draw;

        DeferredEntry(NTxNode owner, NTxRendererContext ctx, AffineTransform ancestorsToRoot,
                      Consumer<NTxRendererContext> draw) {
            this.owner = owner;
            this.ctx = ctx;
            this.ancestorsToRoot = ancestorsToRoot;
            this.draw = draw;
        }
    }
}
