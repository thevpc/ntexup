package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.NTxArrow;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.NtxElement2DPrimitive;
import net.thevpc.ntexup.api.document.elem2d.primitives.NtxElement2DLine;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValueByType;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.lib.geometry2d.NTxElement2DFactory;

import java.awt.*;

public class NTxLineBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.LINE)
                .parseParam().matchesNamedPair(NTxPropName.FROM, NTxPropName.TO, NTxPropName.START_ARROW, NTxPropName.END_ARROW,"arrow","label").then()
                .parseParam().matchesAnyNonPair().storeFirstMissingName(NTxPropName.FROM, NTxPropName.TO).then()
                .renderComponent(this::renderMain)
        ;
    }


    public void renderMain(NTxNodeRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxBounds2D b = rendererContext.selfBounds();
        NTxPoint2D translation = new NTxPoint2D(b.getX(), b.getY());
        NTxPoint2D from = NTxPoint.ofParent(NTxValue.ofProp(node, NTxPropName.FROM).asPoint2D().orElse(new NTxPoint2D(0, 0))).valueHPoint2D(b, rendererContext.getGlobalBounds())
                .plus(translation);
        NTxPoint2D to = NTxPoint.ofParent(NTxValue.ofProp(node, NTxPropName.TO).asPoint2D().orElse(new NTxPoint2D(0, 0))).valueHPoint2D(b, rendererContext.getGlobalBounds())
                .plus(translation);
        NTxGraphics g = rendererContext.graphics();
        if (!rendererContext.isDry()) {
            Paint fc = rendererContext.getForegroundColor(node, true);
            NtxElement2DLine li0 = NTxElement2DFactory.line(from, to)
                    .setStartArrow(NTxValueByType.getArrow(node, rendererContext, NTxPropName.START_ARROW).orNull())
                    .setEndArrow(NTxValueByType.getArrow(node, rendererContext, NTxPropName.END_ARROW).orNull());
            NTxArrow darrow = NTxValueByType.getArrow(node, rendererContext, "arrow").orNull();
            if (darrow != null) {
                if (li0.getStartArrow() == null) {
                    li0.setStartArrow(darrow);
                }
                if (li0.getEndArrow() == null) {
                    li0.setEndArrow(darrow);
                }
            }
            NtxElement2DPrimitive li = li0
                    .setLineStroke(g.createStroke(rendererContext.getStroke(node)))
                    .setLinePaint(fc);
            g.draw2D(li);
        }
        double minx = Math.min(from.getX(), to.getX());
        double miny = Math.min(from.getY(), to.getY());
        double maxX = Math.max(from.getX(), to.getX());
        double maxY = Math.max(from.getY(), to.getY());
        NTxBounds2D b2 = new NTxBounds2D(minx, miny, maxX, maxY);
        rendererContext.drawContour();
    }
}
