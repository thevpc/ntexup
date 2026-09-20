package net.thevpc.ntexup.engine.renderer.special;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.engine.parser.ctrlnodes.CtrlNTxNodeError;

import java.awt.*;

public class NTxErrorRenderer extends NTxDoNothingRenderer {
    public NTxErrorRenderer() {
        super(NTxNodeType.CTRL_ERROR);
    }

    @Override
    public void renderMain(NTxRendererContext context) {
        NTxNode n = context.node();
        String text="error";
        if(n instanceof CtrlNTxNodeError){
            text = ((CtrlNTxNodeError) n).getMessage().toString();
        }
        NTxBounds2D bb = context.selfBounds2D();
        NTxGraphics g = context.graphics();
        double x = bb.minX();
        double y = bb.minY();
        if(y<16){
            y=16;
        }
        g.debugString(text, x, y);
    }
}
