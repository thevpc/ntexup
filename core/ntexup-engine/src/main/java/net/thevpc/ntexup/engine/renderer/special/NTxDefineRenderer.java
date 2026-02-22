package net.thevpc.ntexup.engine.renderer.special;

import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;

public class NTxDefineRenderer extends NTxDoNothingRenderer {
    public NTxDefineRenderer() {
        super(NTxNodeType.CTRL_DEFINE);
    }

    @Override
    public void renderMain(NTxRendererContext ctx) {
    }
}
