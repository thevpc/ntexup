package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;

import java.awt.geom.AffineTransform;
import java.util.function.Consumer;

public class NTxRenderSessionNoOp implements NTxRenderSession {
    public static final NTxRenderSessionNoOp INSTANCE = new NTxRenderSessionNoOp();

    @Override
    public NTxGeometryIndex geometry() {
        return NTxGeometryIndexNoOp.INSTANCE;
    }

    @Override
    public NTxGraphics pageGraphics() {
        return null;
    }

    @Override
    public void defer(NTxNode owner, NTxRendererContext ctx, AffineTransform ancestorsToRoot, Consumer<NTxRendererContext> draw) {
    }

    @Override
    public void drainDeferred() {
    }
}