package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.NTxArrow;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.primitives.NtxElement2DCubicCurve;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.eval.NTxValueByType;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.lib.geometry2d.NTxElement2DFactory;

import java.awt.*;

public class NTxCubicCurveBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id(NTxNodeType.CUBIC_CURVE)
                .parseParam().matchesNamedPair(NTxPropName.FROM, NTxPropName.TO, NTxPropName.START_ARROW, NTxPropName.END_ARROW, NTxPropName.CTRL1, NTxPropName.CTRL2).then()
                .renderComponent(this::renderMain)
        ;
    }

    public void renderMain(NTxRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxBounds2D b = rendererContext.selfBounds2D();
        NTxPoint2D translation = new NTxPoint2D(b.minX(), b.minY());
        NTxPoint2D from = NTxPoint.ofParent(NTxValue.ofProp(node, NTxPropName.FROM).asPoint2D().get()).valueHPoint2D(b, rendererContext.globalBounds2D())
                .plus(translation);
        NTxPoint2D to = NTxPoint.ofParent(NTxValue.ofProp(node, NTxPropName.TO).asPoint2D().get()).valueHPoint2D(b, rendererContext.globalBounds2D())
                .plus(translation);
        NTxPoint2D ctrl1 = NTxPoint.ofParent(NTxValue.ofProp(node, NTxPropName.CTRL1).asPoint2D().get()).valueHPoint2D(b, rendererContext.globalBounds2D())
                .plus(translation);
        NTxPoint2D ctrl2 = NTxPoint.ofParent(NTxValue.ofProp(node, NTxPropName.CTRL2).asPoint2D().get()).valueHPoint2D(b, rendererContext.globalBounds2D())
                .plus(translation);
        NTxGraphics g = rendererContext.graphics();
        if (!rendererContext.isDry()) {
            Paint fc = rendererContext.getForegroundColor(true);
            NtxElement2DCubicCurve li = NTxElement2DFactory.cubic(from, ctrl1, ctrl2, to)
                    .setStartArrow(NTxValueByType.getArrow(rendererContext, NTxPropName.START_ARROW).orNull())
                    .setEndArrow(NTxValueByType.getArrow(rendererContext, NTxPropName.END_ARROW).orNull());
            NTxArrow darrow = NTxValueByType.getArrow(rendererContext, "arrow").orNull();
            if (darrow != null) {
                if (li.getStartArrow() == null) {
                    li.setStartArrow(darrow);
                }
                if (li.getEndArrow() == null) {
                    li.setEndArrow(darrow);
                }
            }
            g.draw2D(li
                    .setLineStroke(g.createStroke(rendererContext.getStroke()))
                    .setLinePaint(fc)
            );
        }
        double minx = Math.min(from.getX(), to.getX());
        double miny = Math.min(from.getY(), to.getY());
        double maxX = Math.max(from.getX(), to.getX());
        double maxY = Math.max(from.getY(), to.getY());
        NTxBounds2D b2 = NTxBounds2D.of(minx, maxX, miny, maxY);
        rendererContext.drawContour();
    }

}
