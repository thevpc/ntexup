package net.thevpc.ntexup.extension.shapes2d.shape;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.eval.NTxValueSizeCache;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.parser.NTxAllArgumentReader;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;

public class NTxCircleBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id(NTxNodeType.CIRCLE)
                .renderComponent(this::renderMain)
                .initializeNodeAction(this::initializeNode)
        ;
        defaultStyles.set(NTxPropName.PRESERVE_ASPECT_RATIO, NElement.ofTrue());
    }

    public void initializeNode(NTxNode p, NTxNodeBuilderContext ctx) {
        p.setProperty(NTxPropName.PRESERVE_ASPECT_RATIO, NElement.ofTrue());
    }

    public void renderMain(NTxRendererContext nodeRendererContext) {
        nodeRendererContext = nodeRendererContext.withDefaultStyles(defaultStyles);
        NTxValueSizeCache z = NTxValueByName.getNodeSizeCache(nodeRendererContext);
        NTxBounds2D b = nodeRendererContext.selfBounds();
        double x = b.getX();
        double y = b.getY();
        NTxGraphics g = nodeRendererContext.graphics();
        boolean someBG = false;
        if (!nodeRendererContext.isDry()) {
            int ww = NTxUtils.intOf(b.getWidth());
            int hh = NTxUtils.intOf(b.getHeight());
            ww = Math.min(ww, hh);
            hh = ww;
            if (someBG = nodeRendererContext.applyBackgroundColor()) {
                g.fillOval((int) x, (int) y, ww, hh);
            }
            if (nodeRendererContext.applyForeground(!someBG)) {
                int finalWw = ww;
                int finalHh = hh;
                nodeRendererContext.withStroke(() -> {
                    g.drawOval((int) x, (int) y, finalWw, finalHh);
                });
            }
        }
        nodeRendererContext.drawContour();
    }
}
