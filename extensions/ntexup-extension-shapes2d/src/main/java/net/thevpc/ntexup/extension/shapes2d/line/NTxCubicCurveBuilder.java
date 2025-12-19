package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.elem2d.NTxElement2DFactory;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.eval.NTxValueByType;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;

import java.awt.*;

public class NTxCubicCurveBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id(NTxNodeType.CUBIC_CURVE)
                .parseParam().matchesNamedPair(NTxPropName.FROM, NTxPropName.TO, NTxPropName.START_ARROW, NTxPropName.END_ARROW, NTxPropName.CTRL1, NTxPropName.CTRL2).then()
                .renderComponent((rendererContext, builderContext1) -> renderMain(rendererContext, builderContext1))
        ;
    }

    public void renderMain(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        NTxNode p = rendererContext.node();
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxBounds2 b = rendererContext.selfBounds();
        NTxPoint2D translation = new NTxPoint2D(b.getX(), b.getY());
        NTxPoint2D from = NTxPoint.ofParent(NTxValue.ofProp(p, NTxPropName.FROM).asPoint2D().get()).valueHPoint2D(b, rendererContext.getGlobalBounds())
                .plus(translation);
        NTxPoint2D to = NTxPoint.ofParent(NTxValue.ofProp(p, NTxPropName.TO).asPoint2D().get()).valueHPoint2D(b, rendererContext.getGlobalBounds())
                .plus(translation);
        NTxPoint2D ctrl1 = NTxPoint.ofParent(NTxValue.ofProp(p, NTxPropName.CTRL1).asPoint2D().get()).valueHPoint2D(b, rendererContext.getGlobalBounds())
                .plus(translation);
        NTxPoint2D ctrl2 = NTxPoint.ofParent(NTxValue.ofProp(p, NTxPropName.CTRL2).asPoint2D().get()).valueHPoint2D(b, rendererContext.getGlobalBounds())
                .plus(translation);
        NTxGraphics g = rendererContext.graphics();
        if (!rendererContext.isDry()) {
            Paint fc = rendererContext.getForegroundColor(p, true);
            g.draw2D(NTxElement2DFactory.cubic(from, ctrl1, ctrl2, to)
                    .setStartArrow(NTxValueByType.getArrow(p, rendererContext, NTxPropName.START_ARROW).orNull())
                    .setEndArrow(NTxValueByType.getArrow(p, rendererContext, NTxPropName.END_ARROW).orNull())
                    .setLineStroke(g.createStroke(rendererContext.getStroke(p)))
                    .setLinePaint(fc)
            );
        }
        double minx = Math.min(from.getX(), to.getX());
        double miny = Math.min(from.getY(), to.getY());
        double maxX = Math.max(from.getX(), to.getX());
        double maxY = Math.max(from.getY(), to.getY());
        NTxBounds2 b2 = new NTxBounds2(minx, miny, maxX, maxY);
        rendererContext.drawContour();
    }

}
