package net.thevpc.ntexup.engine.util;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.NTxSizeD;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;

import java.awt.*;

public class NTxNodeRendererUtils {

    public static NTxSizeD mapDim(NTxSizeD d, NTxSizeD base) {
        return new NTxSizeD(
                d.getWidth() / 100 * base.getWidth(),
                d.getHeight() / 100 * base.getHeight()
        );
    }

    public static Stroke resolveStroke(NTxNode t, NTxGraphics g, NTxNodeRendererContext ctx) {
        NElement strokeElem = NTxValueByName.getStroke(t, ctx);
        if (strokeElem != null) {
            return ctx.graphics().createStroke(strokeElem);
        }
        return null;
    }

    public static boolean withStroke(NTxNode t, NTxGraphics g, NTxNodeRendererContext ctx, Runnable r) {
        Stroke strokeElem = resolveStroke(t, g, ctx);
        if (strokeElem != null) {
            Stroke o = g.getStroke();
            g.setStroke(strokeElem);
            r.run();
            g.setStroke(o);
            return true;
        }else{
            r.run();
            return false;
        }
    }

    public static boolean applyStroke(NTxNode t, NTxGraphics g, NTxNodeRendererContext ctx) {
        Stroke strokeElem = resolveStroke(t, g, ctx);
        if (strokeElem != null) {
            g.setStroke(strokeElem);
            return true;
        }
        return false;
    }

    public static void applyFont(NTxNode t, NTxGraphics g, NTxNodeRendererContext ctx) {
        g.setFont(NTxValueByName.getFont(t, ctx));
    }

    public static NTxSizeD mapDim(double w, double h, NTxNodeRendererContext ctx) {
        NTxBounds2 size = ctx.parentBounds();
        return new NTxSizeD(w / 100 * size.getWidth(), h / 100 * size.getHeight());
    }

    public static NTxBounds2 bounds(NTxNode t, NTxNodeRendererContext ctx) {
        NTxValue oSize = NTxValue.of(ctx.computePropertyValue(t, NTxPropName.SIZE));
        NOptional<NElement[]> a = oSize.asElementArray();
        NTxDouble2 size=null;
        if(a.isPresent()){
            NElement[] tt = a.get();
            switch (tt.length){
                case 1:{
                    size=new NTxDouble2(
                            ctx.sizeRef().x(tt[0]).orElse(100.0),
                            ctx.sizeRef().y(tt[0]).orElse(100.0)
                    );
                    break;
                }
                case 2:{
                    size=new NTxDouble2(
                            ctx.sizeRef().x(tt[0]).orElse(100.0),
                            ctx.sizeRef().y(tt[1]).orElse(100.0)
                    );
                    break;
                }
            }
        }
        if (size == null) {
            size = new NTxDouble2(ctx.parentBounds().getWidth(), ctx.parentBounds().getHeight());
        }
        return new NTxBounds2(
                ctx.parentBounds().getX(),
                ctx.parentBounds().getY(),
                size.getX(),
                size.getY()
        );
    }

    public static boolean applyForeground(NTxNode t, NTxGraphics g, NTxNodeRendererContext ctx, boolean force) {
        if (ctx.isDry()) {
            return false;
        }
        Paint fg = NTxValueByName.getForegroundColor(t, ctx,force);
        if (fg != null) {
            g.setPaint(fg);
            return true;
        }
        return false;
    }

    public static boolean applyBackgroundColor(NTxNode t, NTxGraphics g, NTxNodeRendererContext ctx) {
        if (ctx.isDry()) {
            return false;
        }
        Paint bg = NTxValueByName.resolveBackgroundColor(t, ctx);
        if (bg != null) {
            g.setPaint(bg);
            return true;
        }
        return false;
    }

    public static boolean applyGridColor(NTxNode t, NTxGraphics g, NTxNodeRendererContext ctx, boolean force) {
        if (ctx.isDry()) {
            return false;
        }
        Paint color = NTxValueByName.resolveGridColor(t, ctx);
        if (color != null) {
            g.setPaint(color);
            return true;
        }
        if (force) {
            g.setColor(Color.gray);
            return true;
        }
        return false;
    }

    public static void drawDebugBox(NTxNode node, NTxNodeRendererContext ctx, NTxGraphics g, NTxBounds2 a, boolean force) {
        if (ctx.isDry()) {
            return;
        }
        if (force || NTxValueByName.isDebug(node, ctx)) {
            g.setColor(NTxValueByName.getDebugColor(node, ctx));
            g.drawRect(a);
            NTxBounds2 b = NTxValueByName.getNodeSizeNoCache(node, ctx).parentBoundsWithMargin;
            g.drawRect(b);
            NTxDouble2 origin = NTxValueByName.getOrigin(node, ctx,new NTxDouble2(a.getWidth(),a.getHeight()));
            double x = origin.getX() + a.getX();
            double y = origin.getY() + a.getY();
            g.setColor(NTxValueByName.getDebugColor(node, ctx));
            int originSize = 6;
            g.fillOval(
                    x - originSize / 2, y - originSize / 2,
                    originSize, originSize
            );
        }
    }

    public static void drawDebugBox(NTxNode t, NTxNodeRendererContext ctx, NTxGraphics g, NTxBounds2 a) {
        drawDebugBox(t, ctx, g, a, false);
    }

    public static NOptional<Color> colorFromPaint(Paint p) {
        if(p instanceof Color){
            return NOptional.of((Color) p);
        }
        return NOptional.ofNamedEmpty("color");
    }

    public static void drawBorderLine(NTxNode t, NTxNodeRendererContext ctx, NTxGraphics g, NTxBounds2 a) {
        if (ctx.isDry()) {
            return;
        }
        drawDebugBox(t, ctx, g, a);
        if (NTxValueByName.isDrawContour(t, ctx)) {
            if (applyForeground(t, g, ctx, true)) {
                Stroke s = g.getStroke();
                applyStroke(t, g, ctx);
                g.drawRect(a);
                g.setStroke(s);
            }
        }

    }

    public static void paintBackground(NTxNode t, NTxNodeRendererContext ctx, NTxGraphics g, NTxBounds2 a) {
        if (ctx.isDry()) {
            return;
        }
//        if (HPropValueByNameParser.requireFillBackground(t, ctx)) {
        if (applyBackgroundColor(t, g, ctx)) {
            g.fillRect(a);
        }
//        }
    }
}
