package net.thevpc.ntexup.engine.renderer.page;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.engine.renderer.NTxNodeRendererBase;

public class NTxFragmentRenderer extends NTxNodeRendererBase {
    public NTxFragmentRenderer() {
        super(NTxNodeType.FRAGMENT);
    }

    @Override
    public void renderMain(NTxRendererContext ctx) {
        NTxBounds2D b = ctx.selfBounds2D();
        for (NTxNode child : ctx.node().children()) {
            ctx.resolveNode(child, b)
                    .render();
        }
    }


}
