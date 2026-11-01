package net.thevpc.ntexup.engine.renderer.dump;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElementBuilder;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRenderDump;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * {@link NTxRenderDump} sink wired into the node render contexts as the
 * {@code render-dump} capability. Extension components call
 * {@code ctx.renderDump()} from their render method and {@link #dump} then
 * records a {@code dump{node,kind,bounds,data}} operation inside the paint
 * group currently attached to the component's graphics, right next to the
 * primitives it produces.
 * <p>
 * Each {@link RecordingNTxGraphics} / {@link RecordingGraphics2D} registers
 * itself (weakly) so lookups of disposed scopes do not leak. When the graphics
 * is not recognized (detached rendering, dry pass, foreign graphics) the
 * operation falls back to the page root group.
 */
public class DumpSink implements NTxRenderDump {

    private final Map<Object, DumpRecorder> scopes = Collections.synchronizedMap(new WeakHashMap<>());
    private volatile DumpRecorder pageRoot;

    public void bind(Object graphics, DumpRecorder recorder) {
        if (graphics != null && recorder != null) {
            scopes.put(graphics, recorder);
        }
    }

    public void setPageRoot(DumpRecorder recorder) {
        this.pageRoot = recorder;
    }

    @Override
    public void dump(NTxRendererContext ctx, String kind, NElement data) {
        DumpRecorder target = null;
        if (ctx != null && !ctx.isDry()) {
            target = resolve(ctx.graphics());
        }
        if (target == null) {
            target = pageRoot;
        }
        if (target == null) {
            return;
        }
        NObjectElementBuilder ob = DumpFormat.op("dump");
        if (ctx != null) {
            ob.add("node", NElement.of(ctx.node().type()));
            String name = ctx.node().name();
            if (!NBlankable.isBlank(name)) {
                ob.add("name", NElement.of(name));
            }
            try {
                NTxBounds2D b = ctx.globalBounds2D();
                if (b != null) {
                    ob.add("bounds", DumpFormat.rectArray(b.minX(), b.minY(), b.widthX(), b.widthY()));
                }
            } catch (RuntimeException ex) {
                //bounds are best-effort metadata, never fail a dump for them
            }
        }
        if (!NBlankable.isBlank(kind)) {
            ob.add("kind", NElement.of(kind));
        }
        ob.add("data", data == null ? NElement.ofNull() : data);
        target.record(ob.build());
    }

    private DumpRecorder resolve(NTxGraphics g) {
        if (g == null) {
            return null;
        }
        DumpRecorder r = scopes.get(g);
        if (r != null) {
            return r;
        }
        //graphics2d level: the context graphics is a plain NTxGraphicsImpl
        //wrapping the RecordingGraphics2D registered under the java2d instance
        try {
            return scopes.get(g.graphics2D());
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
