package net.thevpc.ntexup.extension.shapes2d.shape;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;

public class NTxCircleBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id(NTxNodeType.CIRCLE)
                .renderComponent((ctx, builderContext1) -> renderMain(ctx, builderContext1))
                ;
    }

    public void renderMain(NTxNodeRendererContext nodeRendererContext, NTxNodeBuilderContext builderContext) {
        nodeRendererContext = nodeRendererContext.withDefaultStyles(defaultStyles);
        NTxNode node = nodeRendererContext.node();
        NTxBounds2 b = nodeRendererContext.selfBounds(node, null, null);
        double x = b.getX();
        double y = b.getY();
        NTxGraphics g = nodeRendererContext.graphics();
        boolean someBG = false;
        if (!nodeRendererContext.isDry()) {
            int ww = NTxUtils.intOf(b.getWidth());
            int hh = NTxUtils.intOf(b.getHeight());
            ww=Math.min(ww,hh);
            hh=ww;
            if (someBG = nodeRendererContext.applyBackgroundColor(node)) {
                g.fillOval((int) x, (int) y, ww, hh);
            }
            if (nodeRendererContext.applyForeground(node, !someBG)) {
                int finalWw = ww;
                int finalHh = hh;
                nodeRendererContext.withStroke(node,()->{
                    g.drawOval((int) x, (int) y, finalWw, finalHh);
                });
            }
        }
        nodeRendererContext.drawContour();
    }
}
