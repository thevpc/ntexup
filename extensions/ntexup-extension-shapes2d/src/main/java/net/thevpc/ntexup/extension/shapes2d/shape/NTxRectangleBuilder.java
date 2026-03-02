package net.thevpc.ntexup.extension.shapes2d.shape;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;

public class NTxRectangleBuilder implements NTxNodeBuilder {
    private NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id(NTxNodeType.RECTANGLE)
                .parseParam().matchesNamedPair(NTxPropName.ROUND_CORNER, NTxPropName.THEED, NTxPropName.RAISED).asFlags().then()
                .renderComponent(this::renderMain)
        ;
    }


    public void renderMain(NTxRendererContext rendererContext) {
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxNode node = rendererContext.node();
        NTxBounds2D b = rendererContext.selfBounds2D();
        double x = b.minX();
        double y = b.minY();
        NTxGraphics g = rendererContext.graphics();
        Boolean threeD = rendererContext.get3D();
        Boolean raised = rendererContext.getRaised();
        if (raised != null) {
            if (threeD == null) {
                threeD = true;
            }
        }
        NTxDouble2 roundCorners = rendererContext.getRoundCornerArcs();
        boolean round = roundCorners != null;
        boolean d3 = threeD == null ? false : threeD;
        if (!rendererContext.isDry()) {
            if (!round && !d3) {
                boolean someBG = false;
                if (someBG = rendererContext.applyBackgroundColor()) {
                    g.fillRect((int) x, (int) y, NTxUtils.intOf(b.widthX()), NTxUtils.intOf(b.widthY()));
                }
                if (rendererContext.applyForeground(!someBG)) {
                    rendererContext.withStroke(() -> {
                        g.drawRect((int) x, (int) y, NTxUtils.intOf(b.widthX()), NTxUtils.intOf(b.widthY()));
                    });
                }
            } else if (round) {
                double cx = NTxUtils.doubleOf(roundCorners.getX()) / 100 * rendererContext.globalBounds2D().widthX();
                double cy = NTxUtils.doubleOf(roundCorners.getY()) / 100 * rendererContext.globalBounds2D().widthY();
                boolean someBG = false;
                if (someBG = rendererContext.applyBackgroundColor()) {
                    g.fillRoundRect((int) x, (int) y, NTxUtils.intOf(b.widthX()), NTxUtils.intOf(b.widthY()), (int) cx, (int) cy);
                }
                if (rendererContext.applyForeground(!someBG)) {
                    rendererContext.withStroke(() -> {
                        g.drawRoundRect((int) x, (int) y, NTxUtils.intOf(b.widthX()), NTxUtils.intOf(b.widthY()), (int) cx, (int) cy);
                    });
                }
            } else if (threeD) {
                boolean someBG = false;
                if (someBG = rendererContext.applyBackgroundColor()) {
                    g.fill3DRect((int) x, (int) y, NTxUtils.intOf(b.widthX()), NTxUtils.intOf(b.widthY()), raised != null && raised);
                }
                if (rendererContext.applyForeground(!someBG)) {
                    rendererContext.withStroke(() -> {
                        g.draw3DRect((int) x, (int) y, NTxUtils.intOf(b.widthX()), NTxUtils.intOf(b.widthY()), raised != null && raised);
                    });
                }
            }
        }
        rendererContext.drawContour();
    }
}
