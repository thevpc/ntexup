package net.thevpc.ntexup.engine.renderer.special;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.engine.renderer.NTxNodeRendererBase;

public class NTxBlockRenderer extends NTxNodeRendererBase {
    public NTxBlockRenderer() {
        super(NTxNodeType.BLOCK);
    }

    @Override
    public void renderMain(NTxRendererContext ctx) {
        NTxBounds2D b = ctx.selfBounds();

        for (NTxNode child : ctx.node().children()) {
            ctx.resolveNode(child, b)
                    .render();
        }
    }
}
