package net.thevpc.ntexup.extension.shapes2d;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.lang.reflect.Field;
import java.util.Arrays;


/**
 *
 */
public class NTxDonutBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id(NTxNodeType.DONUT)
                .parseParam().matchesNamedPair(NTxPropName.INNER_RADIUS, NTxPropName.START_ANGLE, NTxPropName.EXTENT_ANGLE, NTxPropName.DASH).then()
                .parseParam().matchesNamedPair(NTxPropName.SLICE_COUNT).then()
                .parseParam().matchesNamedPair(NTxPropName.SLICES).then()
                .parseParam().matchesNamedPair(NTxPropName.COLORS).then()
                .renderComponent(this::render)
                ;
    }


    private void render(NTxNodeRendererContext rendererContext) {
        renderDonutOrPie(rendererContext, defaultStyles, true);
    }

    public static void renderDonutOrPie(NTxNodeRendererContext rendererContext,
                                        NTxProperties defaultStyles,
                                        boolean isDonut
                                  ) {
        NTxNode node=rendererContext.node();
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);

        NTxBounds2D b = rendererContext.selfBounds(node, null, null);
        double x = b.getX();
        double y = b.getY();
        double width = b.getWidth();
        double height = b.getHeight();

        double outerRadius = Math.min(width, height) / 2;
        double innerRadius=0;
        if(isDonut) {
            innerRadius = NTxValue.of(node.getPropertyValue(NTxPropName.INNER_RADIUS)).asDouble().orElse(0.0);
            if (innerRadius <= 0) {
                innerRadius = 50;
            } else if (innerRadius >= 100) {
                innerRadius = 0;
            }
            innerRadius = innerRadius / 100 * outerRadius;
        }

        double startAngle = NTxValue.of(node.getPropertyValue(NTxPropName.START_ANGLE)).asDouble().orElse(0.0);
        double extentAngle = NTxValue.of(node.getPropertyValue(NTxPropName.EXTENT_ANGLE)).asDouble().orElse(360.0);
        double dash = NTxValue.of(node.getPropertyValue(NTxPropName.DASH)).asDouble().orElse(0.0);
        NTxGraphics g = rendererContext.graphics();

        String[] colors = NTxValue.of(node.getPropertyValue(NTxPropName.COLORS)).asStringArray().orElse(null);
        g.setColor(Color.black);
        if (!rendererContext.isDry()) {
            double finalInnerRadius = innerRadius;
            rendererContext.withStroke(node, () -> {
                int sliceCount = NTxValue.of(node.getPropertyValue(NTxPropName.SLICE_COUNT)).asInt().orElse(-1);
                //equal slices
                if (sliceCount > 0) {
                    double sliceAngle = (extentAngle - sliceCount * dash) / sliceCount;
                    double currentAngle = startAngle;

                    for (int i = 0; i < sliceCount; i++) {
                        Color color = getColor(i, colors);

                        Area outerCircle = new Area(new Arc2D.Double(x + (width / 2 - outerRadius), y + (height / 2 - outerRadius), outerRadius * 2, outerRadius * 2, currentAngle, sliceAngle, Arc2D.PIE));
                        if (finalInnerRadius > 0) {
                            Area innerCircle = new Area(new Arc2D.Double(x + (width / 2 - finalInnerRadius), y + (height / 2 - finalInnerRadius), finalInnerRadius * 2, finalInnerRadius * 2, currentAngle, sliceAngle, Arc2D.PIE));
                            outerCircle.subtract(innerCircle);
                        }

                        g.setColor(color);
                        g.fill(outerCircle);

                        currentAngle += sliceAngle + dash;
                    }
                }
                //diff sizes
                else {

                    double[] slicePercentage = NTxValue.of(node.getPropertyValue(NTxPropName.SLICES)).asDoubleArray().orElse(getSlicePercentage(node));
                    slicePercentage = Arrays.stream(slicePercentage).filter(xx -> xx <= 0 || Double.isInfinite(xx) || Double.isNaN(xx)).toArray();
                    if (slicePercentage.length == 0) {
                        slicePercentage = new double[]{1, 1, 1};
                    }
                    double sum = Arrays.stream(slicePercentage).sum();
                    slicePercentage = Arrays.stream(slicePercentage).map(xx -> xx / (sum == 0 ? 1 : sum) * 100.0).toArray();
                    int percentageSliceCount = slicePercentage.length;
                    double totalDash = dash * percentageSliceCount;
                    double[] sliceAngles = new double[percentageSliceCount];

                    for (int i = 0; i < percentageSliceCount; i++) {
                        sliceAngles[i] = (extentAngle - totalDash) * slicePercentage[i] / 100.0;
                    }

                    double currentAngle = startAngle;

                    for (int i = 0; i < percentageSliceCount; i++) {
                        Color color = getColor(i, colors);

                        double sliceAngle = sliceAngles[i];

                        Area outerCircle = new Area(new Arc2D.Double(x + (width / 2 - outerRadius), y + (height / 2 - outerRadius), outerRadius * 2, outerRadius * 2, currentAngle, sliceAngle, Arc2D.PIE));
                        if (finalInnerRadius > 0) {
                            Area innerCircle = new Area(new Arc2D.Double(x + (width / 2 - finalInnerRadius), y + (height / 2 - finalInnerRadius), finalInnerRadius * 2, finalInnerRadius * 2, currentAngle, sliceAngle, Arc2D.PIE));
                            outerCircle.subtract(innerCircle);
                        }
                        g.setColor(color);
                        g.fill(outerCircle);

                        currentAngle += sliceAngle + dash;
                    }
                }
            });


//            if (rendererContext.applyForeground(p, !someBG)) {
//                rendererContext.applyStroke(p);
//
//                double centerX = x + width / 2;
//                double centerY = y + height / 2;
//
//                g.drawOval((int) (centerX - outerRadius), (int) (centerY - outerRadius), (int) (outerRadius * 2), (int) (outerRadius * 2));
//                g.drawOval((int) (centerX - innerRadius), (int) (centerY - innerRadius), (int) (innerRadius * 2), (int) (innerRadius * 2));
//            }

            rendererContext.drawContour();
        }
    }

    private static Color getColor(int sliceIndex, String[] colors) {
        String[] defaultColors = {
                "#402E7A",
                "#4C3BCF",
                "#4B70F5",
                "#3DC2EC",
        };

        String colorStr = (colors != null && colors.length > 0)
                ? colors[sliceIndex % colors.length]
                : defaultColors[sliceIndex % defaultColors.length];

        return parseColor(colorStr);
    }

    private static Color parseColor(String colorStr) {
        if (colorStr.startsWith("#")) {
            return Color.decode(colorStr);
        } else {
            try {
                Field field = Color.class.getField(colorStr.toLowerCase());
                return (Color) field.get(null);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unvalid color: " + colorStr);
            }
        }
    }

    private static double[] getSlicePercentage(NTxNode node) {
        return new double[]{};
    }


}
