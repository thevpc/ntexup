package net.thevpc.ntexup.extension.shapes2d;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.NTxShadow;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.util.NOptional;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;


public class NTxCylinderBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();
    private static final Color DEFAULT_SIDE_COLOR = new Color(0x00215E);
    //private static final Color DEFAULT_TOP_COLOR = new Color(0xD1D8C5);

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id(NTxNodeType.CYLINDER)
                .parseParam().matchesNamedPair(NTxPropName.ELLIPSE_H, NTxPropName.TOP_COLOR, NTxPropName.SEGMENT_COUNT).end()
                .renderComponent(this::render);
    }




    public void render(NTxNodeRendererContext rendererContext) {
        NTxNode node=rendererContext.node();
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NOptional<NTxShadow> shadowOptional = rendererContext.readStyleAsShadow(node, NTxPropName.SHADOW);


        NTxBounds2D b = rendererContext.selfBounds(node, null, null);
        double x = b.getX();
        double y = b.getY();
        double width = b.getWidth();
        double height = b.getHeight();

        double arcStroke = NTxValue.of(node.getPropertyValue(NTxPropName.STROKE)).asDouble().orElse(5.0);
        double ellipse_height = NTxValue.of(node.getPropertyValue(NTxPropName.ELLIPSE_H)).asDouble().orElse(50.0);
        ellipse_height = ellipse_height / 100 * height;
        double arcY = y + height - ellipse_height / 2;


        Color sideColor = NTxValue.of(node.getPropertyValue(NTxPropName.BACKGROUND_COLOR)).asColor().orElse(DEFAULT_SIDE_COLOR);
        Color topColor = NTxValue.of(node.getPropertyValue(NTxPropName.TOP_COLOR)).asColor().orElse(sideColor.brighter());

        int segmentCount = NTxValue.of(node.getPropertyValue(NTxPropName.SEGMENT_COUNT)).asInt().orElse(0);

        boolean someBG = false;
        NTxGraphics g = rendererContext.graphics();

        if (!rendererContext.isDry()) {
            if (shadowOptional.isPresent()) {
                NTxShadow shadow = shadowOptional.get();
                NTxPoint2D translation = shadow.getTranslation();
                if (translation == null) {
                    translation = new NTxPoint2D(0, 0);
                }
                Paint shadowColor = shadow.getColor();
                if (shadowColor == null) {
                    shadowColor = sideColor.darker();
                }
                NTxPoint2D shear = shadow.getShear();
                if (shear == null) {
                    shear = new NTxPoint2D(0, 0);
                }

                g.setColor((Color) shadowColor);
                AffineTransform t = g.getTransform();
                g.shear(-1, 0);

                g.fillRect((int) (x + translation.getX()), (int) (y + ellipse_height / 2 + translation.getY()), NTxUtils.intOf(width), NTxUtils.intOf(height - ellipse_height));
                g.fillOval((int) (x + translation.getX()), (int) (arcY - ellipse_height / 2 + translation.getY()), NTxUtils.intOf(width), NTxUtils.intOf(ellipse_height));
                g.fillOval((int) (x + translation.getX()), (int) (y + translation.getY()), NTxUtils.intOf(width), NTxUtils.intOf(ellipse_height));

                g.setTransform(t);


            }
            double finalEllipse_height = ellipse_height;
            if (someBG = rendererContext.applyBackgroundColor(node)) {

                rendererContext.withStroke(node, () -> {
                    g.setColor(sideColor);
                    g.fillRect(x, y + finalEllipse_height / 2, width, height - finalEllipse_height);
                    g.fillOval((int) x, (int) arcY - finalEllipse_height / 2, NTxUtils.intOf(width), NTxUtils.intOf(finalEllipse_height));

                    g.setColor(sideColor.darker());
                    Area rightButtomArc = new Area(new Arc2D.Double(x, (int) arcY - finalEllipse_height / 2, width, finalEllipse_height, -90, -180, Arc2D.PIE));
                    g.fill(rightButtomArc);

                    g.fillRect(x, y + finalEllipse_height / 2, width / 2, height - finalEllipse_height);


                    g.setColor(topColor);
                    g.fillOval((int) x, (int) y, width, finalEllipse_height);

                    Area rightHalfEllipse = new Area(new Arc2D.Double(x, y, width, finalEllipse_height, 90, 180, Arc2D.PIE));
                    g.setColor(topColor.darker());
                    g.fill(rightHalfEllipse);


                    double segmentHeight = (height - finalEllipse_height) / (segmentCount + 1);
                    for (int i = 1; i <= segmentCount; i++) {
                        double segY = y + i * segmentHeight;

                        g.setColor(topColor.darker());
                        g.drawArc(x, segY + arcStroke, width, finalEllipse_height, 0, -180);
                        g.setColor(topColor);
                        g.drawArc(x, segY + arcStroke, width, finalEllipse_height, 0, -90);
                        g.setColor(Color.gray);
                        g.drawArc(x, segY, width, finalEllipse_height, 0, -180);

                    }
                });
            }


            if (rendererContext.applyForeground(node, !someBG)) {
                rendererContext.withStroke(node, () -> {
                    g.drawOval((int) x, (int) y, NTxUtils.doubleOf(width), NTxUtils.intOf(finalEllipse_height));

                    g.drawLine((int) x, (int) (y + finalEllipse_height / 2), (int) x, (int) (arcY));
                    g.drawLine((int) (x + width), (int) (y + finalEllipse_height / 2), (int) (x + width), (int) (arcY));

                    g.drawArc((int) x, (int) arcY - finalEllipse_height / 2, NTxUtils.intOf(width), NTxUtils.intOf(finalEllipse_height), 0, -180);

                    double segmentHeight = (height - finalEllipse_height) / (segmentCount + 1);
                    for (int i = 1; i <= segmentCount; i++) {
                        double segY = y + i * segmentHeight;
                        g.drawArc(x, segY, NTxUtils.doubleOf(width), NTxUtils.doubleOf(finalEllipse_height), 0, -180);
                    }
                });
            }
        }
    }
}
