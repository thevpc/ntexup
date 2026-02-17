package net.thevpc.ntexup.engine.renderer.special;

import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;

public class NTxAssignDefaultRenderer extends NTxDoNothingRenderer {
    public NTxAssignDefaultRenderer() {
        super(NTxNodeType.CTRL_ASSIGN_DEFAULT);
    }

    @Override
    public void renderMain(NTxRendererContext ctx) {
    }
}
