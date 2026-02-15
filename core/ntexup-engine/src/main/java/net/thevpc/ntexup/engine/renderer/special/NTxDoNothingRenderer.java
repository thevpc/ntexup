package net.thevpc.ntexup.engine.renderer.special;

import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.engine.renderer.NTxNodeRendererBase;

public class NTxDoNothingRenderer extends NTxNodeRendererBase {
    public NTxDoNothingRenderer(String name) {
        super(name);
    }

    @Override
    public void renderMain(NTxRendererContext ctx) {
    }
}
