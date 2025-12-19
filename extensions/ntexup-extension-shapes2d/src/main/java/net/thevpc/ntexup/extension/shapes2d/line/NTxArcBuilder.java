package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValueByType;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;

import java.awt.*;

public class NTxArcBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.ARC)
                .parseParam().matchesNamedPair(NTxPropName.FROM, NTxPropName.TO).then()
                .renderComponent((ctx, builderContext1) -> renderMain(ctx, builderContext1));
    }


    public void renderMain(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        NTxNode node = rendererContext.node();
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxBounds2 b = rendererContext.selfBounds(node, null, null);
        double x = b.getX();
        double y = b.getY();
        double startAngle = NTxValueByType.getDouble(node,rendererContext, NTxPropName.FROM).orElse(0.0);
        double endAngle = NTxValueByType.getDouble(node,rendererContext, NTxPropName.TO).orElse(0.0);
        NTxGraphics g = rendererContext.graphics();
        if (!rendererContext.isDry()) {
            rendererContext.applyForeground(node , true);
            Stroke oldStroke=g.getStroke();
            Stroke stroke= rendererContext.resolveStroke(node);
            if(stroke!=null){
                g.setStroke(stroke);
            }
            g.drawArc((int) x, (int) y, NTxUtils.intOf(b.getWidth()), NTxUtils.intOf(b.getHeight()),
                    (int) startAngle,
                    (int) endAngle
            );
            g.setStroke(oldStroke);
        }
        rendererContext.drawContour();
    }

//    public void renderMain(NTxNode p, NTxNodeRendererContext ctx) {
//        ctx = ctx.withDefaultStyles(p, defaultStyles);
//        Bounds2 b = selfBounds(p, ctx);
//        NTxPoint2D translation = new NTxPoint2D(b.getX(), b.getY());
//        NTxPoint2D from = NTxPoint.ofParent(ObjEx.ofProp(p, NTxPropName.FROM).asNTxPoint2D().get()).valueNTxPoint2D(b, ctx.getGlobalBounds())
//                .plus(translation);
//        NTxPoint2D to = NTxPoint.ofParent(ObjEx.ofProp(p, NTxPropName.TO).asNTxPoint2D().get()).valueNTxPoint2D(b, ctx.getGlobalBounds())
//                .plus(translation);
//        NTxGraphics g = ctx.graphics();
//        double startAngle = (double) p.getPropertyValue(NTxPropName.START_ANGLE).orElse(0.0);
//        double endAngle = (double) p.getPropertyValue(NTxPropName.END_ANGLE).orElse(0.0);
//
//        if (!ctx.isDry()) {
//            Paint fc = ctx.getForegroundColor(p, true);
//            g.setPaint(fc);
//            ctx.applyStroke(p);
//            g.drawArc( from.getX(), from.getY(),
//                    to.getX(), to.getY(),
//                    startAngle,
//                    endAngle
//            );
////            g.draw2D(NTxElement2DFactory.line(from, to)
////                    .setStartArrow(NTxValueByType.getArrow(p, ctx, NTxPropName.START_ARROW).orNull())
////                    .setEndArrow(NTxValueByType.getArrow(p, ctx, NTxPropName.END_ARROW).orNull())
////                    .setLineStroke(g.createStroke(ctx.getStroke(p)))
////                    .setLinePaint(fc)
////            );
//        }
//        double minx = Math.min(from.getX(), to.getX());
//        double miny = Math.min(from.getY(), to.getY());
//        double maxX = Math.max(from.getX(), to.getX());
//        double maxY = Math.max(from.getY(), to.getY());
//        Bounds2 b2 = new Bounds2(minx, miny, maxX, maxY);
//        ctxs.paintDebugBox(p, b2);
//    }

}
