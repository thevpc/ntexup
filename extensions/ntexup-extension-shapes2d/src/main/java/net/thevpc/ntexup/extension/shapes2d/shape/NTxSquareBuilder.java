package net.thevpc.ntexup.extension.shapes2d.shape;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;

public class NTxSquareBuilder implements NTxNodeBuilder {
    private NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id(NTxNodeType.SQUARE)
                .parseParam().matchesNamedPair(NTxPropName.ROUND_CORNER, NTxPropName.THEED, NTxPropName.RAISED).asFlags().then()
                .renderComponent(this::renderMain)
        ;
    }


    public void renderMain(NTxNodeRendererContext rendererContext) {
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxNode node = rendererContext.node();
        NTxBounds2D b = rendererContext.selfBounds();
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
            int ww = NTxUtils.intOf(b.getWidth());
            int hh = NTxUtils.intOf(b.getHeight());
            ww = Math.min(ww, hh);
            hh = ww;
            int finalWw = ww;
            int finalHh = hh;
            if (!round && !d3) {
                boolean someBG = false;
                if (someBG = rendererContext.applyBackgroundColor(node)) {
                    g.fillRect((int) x, (int) y, ww, hh);
                }
                if (rendererContext.applyForeground(node, !someBG)) {
                    rendererContext.withStroke(node, () -> {
                        g.drawRect((int) x, (int) y, finalWw, finalHh);
                    });
                }
            } else if (round) {
                double cx = NTxUtils.doubleOf(roundCorners.getX()) / 100 * rendererContext.getGlobalBounds().getWidth();
                double cy = NTxUtils.doubleOf(roundCorners.getY()) / 100 * rendererContext.getGlobalBounds().getHeight();
                boolean someBG = false;
                if (someBG = rendererContext.applyBackgroundColor(node)) {
                    g.fillRoundRect((int) x, (int) y, ww, hh, (int) cx, (int) cy);
                }
                if (rendererContext.applyForeground(node, !someBG)) {
                    rendererContext.withStroke(node, () -> {
                        g.drawRoundRect((int) x, (int) y, finalWw, finalHh, (int) cx, (int) cy);
                    });
                }
            } else if (threeD) {
                boolean someBG = false;
                if (someBG = rendererContext.applyBackgroundColor(node)) {
                    g.fill3DRect((int) x, (int) y, ww, hh, raised != null && raised);
                }
                if (rendererContext.applyForeground(node, !someBG)) {
                    rendererContext.withStroke(node, () -> {
                        g.draw3DRect((int) x, (int) y, finalWw, finalHh, raised != null && raised);
                    });
                }
            }
        }
        rendererContext.drawContour();
    }

}
