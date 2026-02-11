package net.thevpc.ntexup.engine.util;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.NTxSizeD;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.eval.NTxValue;
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

    public static Stroke resolveStroke(NTxGraphics g, NTxRendererContext ctx) {
        NElement strokeElem = NTxValueByName.getStroke(ctx);
        if (strokeElem != null) {
            return ctx.graphics().createStroke(strokeElem);
        }
        return null;
    }

    public static boolean withStroke(NTxGraphics g, NTxRendererContext ctx, Runnable r) {
        Stroke strokeElem = resolveStroke(g, ctx);
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

    public static boolean applyStroke(NTxGraphics g, NTxRendererContext ctx) {
        Stroke strokeElem = resolveStroke(g, ctx);
        if (strokeElem != null) {
            g.setStroke(strokeElem);
            return true;
        }
        return false;
    }

    public static void applyFont(NTxGraphics g, NTxRendererContext ctx) {
        g.setFont(NTxValueByName.getFont(ctx));
    }

    public static NTxSizeD mapDim(double w, double h, NTxRendererContext ctx) {
        NTxBounds2D size = ctx.parentBounds();
        return new NTxSizeD(w / 100 * size.getWidth(), h / 100 * size.getHeight());
    }

    public static NTxBounds2D bounds(NTxRendererContext ctx) {
        NTxValue oSize = NTxValue.of(ctx.computePropertyValue(NTxPropName.SIZE));
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
        return new NTxBounds2D(
                ctx.parentBounds().getX(),
                ctx.parentBounds().getY(),
                size.getX(),
                size.getY()
        );
    }

    public static boolean applyForeground(NTxGraphics g, NTxRendererContext ctx, boolean force) {
        if (ctx.isDry()) {
            return false;
        }
        Paint fg = NTxValueByName.getForegroundColor(ctx,force);
        if (fg != null) {
            g.setPaint(fg);
            return true;
        }
        return false;
    }

    public static boolean applyBackgroundColor(NTxGraphics g, NTxRendererContext ctx) {
        if (ctx.isDry()) {
            return false;
        }
        Paint bg = NTxValueByName.resolveBackgroundColor(ctx);
        if (bg != null) {
            g.setPaint(bg);
            return true;
        }
        return false;
    }

    public static boolean applyGridColor(NTxGraphics g, NTxRendererContext ctx, boolean force) {
        if (ctx.isDry()) {
            return false;
        }
        Paint color = NTxValueByName.resolveGridColor(ctx);
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

    public static void drawDebugBox(NTxRendererContext ctx, NTxGraphics g, NTxBounds2D a, boolean force) {
        if (ctx.isDry()) {
            return;
        }
        if (force || NTxValueByName.isDebug(ctx)) {
            g.setColor(NTxValueByName.getDebugColor(ctx));
            g.drawRect(a);
            NTxBounds2D b = NTxValueByName.getNodeSizeNoCache(ctx).parentBoundsWithMargin;
            g.drawRect(b);
            NTxDouble2 origin = NTxValueByName.getOrigin(ctx,new NTxDouble2(a.getWidth(),a.getHeight()));
            double x = origin.getX() + a.getX();
            double y = origin.getY() + a.getY();
            g.setColor(NTxValueByName.getDebugColor(ctx));
            int originSize = 6;
            g.fillOval(
                    x - originSize / 2, y - originSize / 2,
                    originSize, originSize
            );
        }
    }

    public static void drawDebugBox(NTxRendererContext ctx, NTxGraphics g, NTxBounds2D a) {
        drawDebugBox(ctx, g, a, false);
    }

    public static NOptional<Color> colorFromPaint(Paint p) {
        if(p instanceof Color){
            return NOptional.of((Color) p);
        }
        return NOptional.ofNamedEmpty("color");
    }

    public static void drawBorderLine(NTxRendererContext ctx, NTxGraphics g, NTxBounds2D a) {
        if (ctx.isDry()) {
            return;
        }
        drawDebugBox(ctx, g, a);
        if (NTxValueByName.isDrawContour(ctx)) {
            if (applyForeground(g, ctx, true)) {
                Stroke s = g.getStroke();
                applyStroke(g, ctx);
                g.drawRect(a);
                g.setStroke(s);
            }
        }

    }

    public static void paintBackground(NTxRendererContext ctx, NTxGraphics g, NTxBounds2D a) {
        if (ctx.isDry()) {
            return;
        }
//        if (HPropValueByNameParser.requireFillBackground(t, ctx)) {
        if (applyBackgroundColor(g, ctx)) {
            g.fillRect(a);
        }
//        }
    }
}
