package net.thevpc.ntexup.extension.shapes2d.shape;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;

public class NTxRectangleBuilder implements NTxNodeBuilder {
    private NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id(NTxNodeType.RECTANGLE)
                .parseParam().matchesNamedPair(NTxPropName.ROUND_CORNER, NTxPropName.THEED, NTxPropName.RAISED).asFlags().then()
                .renderComponent((rendererContext, builderContext1) -> renderMain(rendererContext, builderContext1))
        ;
    }


    public void renderMain(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxNode node = rendererContext.node();
        NTxBounds2 b = rendererContext.selfBounds();
        double x = b.getX();
        double y = b.getY();
        NTxGraphics g = rendererContext.graphics();
        Boolean threeD = rendererContext.get3D(node);
        Boolean raised = rendererContext.getRaised(node);
        if (raised != null) {
            if (threeD == null) {
                threeD = true;
            }
        }
        NTxDouble2 roundCorners = rendererContext.getRoundCornerArcs(node);
        boolean round = roundCorners != null;
        boolean d3 = threeD == null ? false : threeD;
        if (!rendererContext.isDry()) {
            if (!round && !d3) {
                boolean someBG = false;
                if (someBG = rendererContext.applyBackgroundColor(node)) {
                    g.fillRect((int) x, (int) y, NTxUtils.intOf(b.getWidth()), NTxUtils.intOf(b.getHeight()));
                }
                if (rendererContext.applyForeground(node,  !someBG)) {
                    rendererContext.withStroke(node, () -> {
                        g.drawRect((int) x, (int) y, NTxUtils.intOf(b.getWidth()), NTxUtils.intOf(b.getHeight()));
                    });
                }
            } else if (round) {
                double cx = NTxUtils.doubleOf(roundCorners.getX()) / 100 * rendererContext.getGlobalBounds().getWidth();
                double cy = NTxUtils.doubleOf(roundCorners.getY()) / 100 * rendererContext.getGlobalBounds().getHeight();
                boolean someBG = false;
                if (someBG = rendererContext.applyBackgroundColor(node)) {
                    g.fillRoundRect((int) x, (int) y, NTxUtils.intOf(b.getWidth()), NTxUtils.intOf(b.getHeight()), (int) cx, (int) cy);
                }
                if (rendererContext.applyForeground(node,  !someBG)) {
                    rendererContext.withStroke(node, () -> {
                        g.drawRoundRect((int) x, (int) y, NTxUtils.intOf(b.getWidth()), NTxUtils.intOf(b.getHeight()), (int) cx, (int) cy);
                    });
                }
            } else if (threeD) {
                boolean someBG = false;
                if (someBG = rendererContext.applyBackgroundColor(node)) {
                    g.fill3DRect((int) x, (int) y, NTxUtils.intOf(b.getWidth()), NTxUtils.intOf(b.getHeight()), raised != null && raised);
                }
                if (rendererContext.applyForeground(node,  !someBG)) {
                    rendererContext.withStroke(node, () -> {
                        g.draw3DRect((int) x, (int) y, NTxUtils.intOf(b.getWidth()), NTxUtils.intOf(b.getHeight()), raised != null && raised);
                    });
                }
            }
        }
        rendererContext.drawContour();
    }
}
