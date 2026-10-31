package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.NTxArrow;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
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
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxSizeRef;
import net.thevpc.ntexup.lib.geometry2d.NTxElement2DFactory;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NNumberElement;
import net.thevpc.nuts.elem.NParamOrChild;

import java.awt.*;
import java.util.List;

public class NTxLineBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.LINE)
                .parseParam().matchesNamedPair(NTxPropName.FROM, NTxPropName.TO, NTxPropName.START_ARROW, NTxPropName.END_ARROW,"arrow","label","flow").then()
                .parseParam().matchesAnyNonPair().storeFirstMissingName(NTxPropName.FROM, NTxPropName.TO).then()
                .renderComponent(this::renderMain)
        ;
    }

    public static NTxPoint2D resolvePoint2D(NElement elem, NTxRendererContext rendererContext, NTxBounds2D b) {
        if (elem == null || elem.isNull()) {
            return new NTxPoint2D(b.minX(), b.minY());
        }
        if (rendererContext != null) {
            elem = rendererContext.evalExpression(elem).orElse(elem);
        }
        NTxBounds2D global = rendererContext.globalBounds2D();
        NTxSizeRef sr = new NTxSizeRef(b.widthX(), b.widthY(), global.widthX(), global.widthY());
        if (elem.isListOrParametrizedContainer()) {
            List<NParamOrChild> items = elem.asListOrParametrizedContainer().get().paramsOrChildren();
            if (items.size() >= 2) {
                NElement e0 = items.get(0).element();
                NElement e1 = items.get(1).element();
                if (rendererContext != null) {
                    e0 = rendererContext.evalExpression(e0).orElse(e0);
                    e1 = rendererContext.evalExpression(e1).orElse(e1);
                }
                double x = resolveCoord(e0, sr.x(e0).orElse(0.0), b.minX(), global.minX());
                double y = resolveCoord(e1, sr.y(e1).orElse(0.0), b.minY(), global.minY());
                return new NTxPoint2D(x, y);
            } else if (items.size() == 1) {
                NElement e0 = items.get(0).element();
                if (rendererContext != null) {
                    e0 = rendererContext.evalExpression(e0).orElse(e0);
                }
                double x = resolveCoord(e0, sr.x(e0).orElse(0.0), b.minX(), global.minX());
                return new NTxPoint2D(x, x);
            }
        }
        if (elem.isNumber()) {
            double x = resolveCoord(elem, sr.x(elem).orElse(0.0), b.minX(), global.minX());
            return new NTxPoint2D(x, x);
        }
        return new NTxPoint2D(b.minX(), b.minY());
    }

    private static double resolveCoord(NElement e, double val, double parentMin, double globalMin) {
        if (e != null && e.isNumber()) {
            String u = ((NNumberElement) e).numberSuffix();
            if (u != null) {
                u = u.toLowerCase().trim();
                if (u.equals("%p")) {
                    return globalMin + val;
                }
            }
        }
        return parentMin + val;
    }

    public void renderMain(NTxRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxBounds2D b = rendererContext.selfBounds2D();
        NTxPoint2D from = resolvePoint2D(node.getPropertyValue(NTxPropName.FROM).orNull(), rendererContext, b);
        NTxPoint2D to = resolvePoint2D(node.getPropertyValue(NTxPropName.TO).orNull(), rendererContext, b);
        NTxGraphics g = rendererContext.graphics();
        if (!rendererContext.isDry()) {
            Paint fc = rendererContext.getLineColor(true);
            NtxElement2DLine li0 = NTxElement2DFactory.line(from, to)
                    .setStartArrow(NTxValueByType.getArrow(rendererContext, NTxPropName.START_ARROW).orNull())
                    .setEndArrow(NTxValueByType.getArrow(rendererContext, NTxPropName.END_ARROW).orNull());
            NTxArrow darrow = NTxValueByType.getArrow(rendererContext, "arrow").orNull();
            if (darrow != null) {
                if (li0.getStartArrow() == null) {
                    li0.setStartArrow(darrow);
                }
                if (li0.getEndArrow() == null) {
                    li0.setEndArrow(darrow);
                }
            }
            NtxElement2DPrimitive li = li0
                    .setLineStroke(g.createStroke(rendererContext.getStroke()))
                    .setLinePaint(fc);
            g.draw2D(li);

            NTxRoutedPath _path = new NTxPolylineRoutedPath(java.util.Arrays.asList(from, to));
            NTxLabelDecorator.draw(rendererContext, g, _path);
            NTxFlow _flow = NTxFlow.parse(node.getPropertyValue("flow").orNull(), rendererContext);
            if (_flow != null) {
                NTxFlowDecorator.draw(rendererContext, g, _path, _flow);
            }
        }

        double minx = Math.min(from.getX(), to.getX());
        double miny = Math.min(from.getY(), to.getY());
        double maxX = Math.max(from.getX(), to.getX());
        double maxY = Math.max(from.getY(), to.getY());
        NTxBounds2D b2 = NTxBounds2D.of(minx, maxX, miny, maxY);
        rendererContext.drawContour();
    }
}
