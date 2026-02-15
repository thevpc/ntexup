package net.thevpc.ntexup.engine.renderer.special;

import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.engine.renderer.NTxNodeRendererBase;

public class NTxAssignRenderer extends NTxDoNothingRenderer {
    public NTxAssignRenderer() {
        super(NTxNodeType.CTRL_ASSIGN);
    }

    @Override
    public void renderMain(NTxRendererContext ctx) {
    }
}
