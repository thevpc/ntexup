package net.thevpc.ntexup.api.eval;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.*;

import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxSizeRef;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.api.util.NtxFontInfo;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NNumberElement;
import net.thevpc.nuts.util.NOptional;

import java.awt.*;
import java.util.Map;

public class NTxValueByName {
    public static NOptional<Paint> getColorProperty(String propName, NTxNode t, NTxNodeRendererContext ctx) {
        NTxValue r = NTxValue.of(ctx.computePropertyValue(t, propName).orElse(null));
        return NOptional.of(r.asColor().orElse(null));
    }

    protected static boolean isPreserveShapeRatio(NTxNode t, NTxNodeRendererContext ctx) {
        return getNodeSizeCache(t, ctx).preserveRatio;
    }

    private static NTxDouble2 getSize(NTxNode t, NTxDouble2 minSize, NTxNodeRendererContext ctx) {
        NTxDouble2 size = getNodeSizeCache(t, ctx).componentSize;

        boolean shapeRatio = isPreserveShapeRatio(t, ctx);
        //ratio depends on the smallest
        double sx = size.getX();
        double sy = size.getY();
        if (minSize != null) {
            sx = Math.max(minSize.getX(), sx);
            sy = Math.max(minSize.getY(), sy);
        }
        if (shapeRatio) {
            if (sx > sy) {
                sx = sy;
            }
            if (sy > sx) {
                sy = sx;
            }
            return new NTxDouble2(
                    sx,
                    sy
            );
        }
        return new NTxDouble2(
                sx,
                sy
        );
    }

    public static NTxDouble2 getOrigin(NTxNode t, NTxNodeRendererContext ctx, NTxDouble2 a) {
        NTxElemNumber2 double2OrHAlign = getNodeSizeCache(t, ctx).origin;
        NTxSizeRef sr = new NTxSizeRef(a.getX(), a.getY(), ctx.getGlobalBounds().getWidth(), ctx.getGlobalBounds().getHeight());
        return new NTxDouble2(
                sr.x(double2OrHAlign.getX()).get(),
                sr.y(double2OrHAlign.getY()).get()
        );
    }

    public static NTxRotation getRotation(NTxNode node, NTxNodeRendererContext ctx) {
        return NTxValueByType.getRotation(node, ctx, NTxPropName.ROTATE).orNull();
    }

    public static NTxDouble2 getPosition(NTxNode t, NTxNodeRendererContext ctx, NTxDouble2 a) {
        NTxElemNumber2 double2OrHAlign = getNodeSizeCache(t, ctx).position;
        NTxSizeRef sr = new NTxSizeRef(a.getX(), a.getY(), ctx.getGlobalBounds().getWidth(), ctx.getGlobalBounds().getHeight());
        return new NTxDouble2(
                sr.x(double2OrHAlign.getX()).get(),
                sr.y(double2OrHAlign.getY()).get()
        );
    }

    public static NElement getStroke(NTxNode t, NTxNodeRendererContext ctx) {
        return getNodeCommonCache(t, ctx).stroke;
    }

    public static NTxBounds2 selfBounds(NTxNode t, NTxDouble2 selfSize, NTxDouble2 minSize, NTxNodeRendererContext ctx) {
//        NTxBounds2 parentBounds = ctx.parentBounds();
        NTxSizeRef parentWithMarginRef = getNodeSizeCache(t, ctx).parentWithMarginRef;
        NTxBounds2 parentBoundsWithMargin = getNodeSizeCache(t, ctx).parentBoundsWithMargin;

        if (selfSize == null) {
            selfSize = getSize(t, minSize, ctx);
        }

        NTxDouble2 pos = getPosition(t, ctx, new NTxDouble2(parentWithMarginRef.getParentWidth(), parentWithMarginRef.getParentHeight()));

        NTxDouble2 origin = getOrigin(t, ctx, selfSize);

        double x = pos.getX() - origin.getX() + parentBoundsWithMargin.getX();
        double y = pos.getY() - origin.getY() + parentBoundsWithMargin.getY();


        return new NTxBounds2(
                x,
                y,
                selfSize.getX(),
                selfSize.getY()
        );
    }

    public static boolean isVisible(NTxNode node, NTxNodeRendererContext ctx) {
        return (boolean) node.getAndSetRenderCache("visible", false,
                () -> {
                    NOptional<Boolean> b = NTxValueByType.getBoolean(node, ctx, NTxPropName.HIDE);
                    if (b.isPresent()) {
                        return !b.get();
                    } else {
                        b = NTxValueByType.getBoolean(node, ctx, "show");
                        if (b.isPresent()) {
                            return !b.get();
                        } else {
                            b = NTxValueByType.getBoolean(node, ctx, "visible");
                            if (b.isPresent()) {
                                return b.get();
                            } else {
                                return true;
                            }
                        }
                    }
                }
        ).get();
    }

    public static double getFontSize(NTxNode t, NTxNodeRendererContext ctx) {
        NTxValueFontCache c = getNodeFontCache(t, ctx);
        return Math.min(c.fontXSize, c.fontYSize);
    }

    public static String getFontFamily(NTxNode t, NTxNodeRendererContext ctx) {
        return getNodeFontCache(t, ctx).fontFamily;
    }

    public static boolean isFontUnderlined(NTxNode t, NTxNodeRendererContext ctx) {
        return getNodeFontCache(t, ctx).fontUnderline;
    }

    public static boolean isFontStrike(NTxNode t, NTxNodeRendererContext ctx) {
        return getNodeFontCache(t, ctx).fontStrike;
    }

    public static boolean isFontBold(NTxNode t, NTxNodeRendererContext ctx) {
        return getNodeFontCache(t, ctx).fontBold;
    }

    public static boolean isFontItalic(NTxNode t, NTxNodeRendererContext ctx) {
        return getNodeFontCache(t, ctx).fontItalic;
    }

    public static NTxValueCommonCache getNodeCommonNoCache(NTxNode node, NTxNodeRendererContext ctx) {
        NTxValueCommonCache renderInfo = new NTxValueCommonCache();
        NElement e = NTxValueByType.getElement(node, ctx, NTxPropName.FONT_SIZE).orNull();
        NTxSizeRef sr = ctx.sizeRef();
        renderInfo.stroke = NTxValueByType.getElement(node, ctx, NTxPropName.STROKE).orNull();


        renderInfo.foregroundColor = NTxValueByType.getPaint(node, ctx, NTxPropName.FOREGROUND_COLOR, "foreground", "color", "fg").orElse(null);
        renderInfo.backgroundColor = NTxValueByType.getPaint(node, ctx, NTxPropName.BACKGROUND_COLOR, "background", "bg").orNull();
        renderInfo.fillBackground = NTxValueByType.getBoolean(node, ctx, NTxPropName.FILL_BACKGROUND, "fill").orElse(false);
        renderInfo.debugLevel = NTxValueByType.getIntOrBoolean(node, ctx, NTxPropName.DEBUG).orElse(0);
        renderInfo.debugColor = (Color) NTxValueByType.getPaint(node, ctx, NTxPropName.DEBUG_COLOR).orElse(Color.GRAY);
        renderInfo.drawContour = NTxValueByType.getBoolean(node, ctx, NTxPropName.DRAW_CONTOUR, "contour").orElse(false);
        return renderInfo;
    }
    public static NTxValueSizeCache getNodeSizeNoCache(NTxNode node, NTxNodeRendererContext ctx) {
        NTxValueSizeCache renderInfo = new NTxValueSizeCache();
        NElement e = NTxValueByType.getElement(node, ctx, NTxPropName.FONT_SIZE).orNull();
        NTxSizeRef sr = ctx.sizeRef();
        renderInfo.preserveRatio = NTxValue.of(ctx.computePropertyValue(node, NTxPropName.PRESERVE_ASPECT_RATIO).orNull()).asBoolean().orElse(false);

        {

            NElement marginElement = ctx.computePropertyValue(node, NTxPropName.MARGIN).orNull();

            NOptional<NElement[]> d = NTxValue.of(marginElement).asElementArray();
            double left = 0;
            double top = 0;
            double right = 0;
            double bottom = 0;
            if (d.isPresent()) {
                NElement[] dd = d.get();
                switch (dd.length) {
                    case 0: {
                        break;
                    }
                    case 1: {
                        double dv = sr.x(dd[0]).orElse(0.0);
                        left = dv;
                        top = dv;
                        right = dv;
                        bottom = dv;
                        break;
                    }
                    case 2: {
                        double dv1 = sr.x(dd[0]).orElse(0.0);
                        double dv2 = sr.y(dd[1]).orElse(0.0);
                        left = dv1;
                        top = dv2;
                        right = dv1;
                        bottom = dv2;
                        break;
                    }
                    case 3: {
                        double dv1 = sr.x(dd[0]).orElse(0.0);
                        double dv2 = sr.y(dd[1]).orElse(0.0);
                        double dv3 = sr.x(dd[2]).orElse(0.0);
                        left = dv1;
                        top = dv2;
                        right = dv3;
                        bottom = dv2;
                        break;
                    }
                    case 4: {
                        left = sr.x(dd[0]).orElse(0.0);
                        top = sr.y(dd[1]).orElse(0.0);
                        right = sr.x(dd[2]).orElse(0.0);
                        bottom = sr.y(dd[3]).orElse(0.0);
                        break;
                    }
                }
            } else {
                double dv = sr.x(marginElement).orElse(0.0);
                left = dv;
                top = dv;
                right = dv;
                bottom = dv;
            }
            renderInfo.margin = new NTxMargin(left, top, right, bottom);
            NTxBounds2 parentBounds = ctx.parentBounds();
            double pw = parentBounds.getWidth();
            double ph = parentBounds.getHeight();
            renderInfo.parentWithMarginRef = new NTxSizeRef(
                    Math.max(pw - renderInfo.margin.getLeft() - renderInfo.margin.getRight(), 0),
                    Math.max(ph - renderInfo.margin.getTop() - renderInfo.margin.getBottom(), 0),
                    ctx.getGlobalBounds().getWidth(),
                    ctx.getGlobalBounds().getHeight()
            );
            renderInfo.parentBoundsWithMargin = new NTxBounds2(
                    parentBounds.getX() + renderInfo.margin.getLeft(),
                    parentBounds.getY() + renderInfo.margin.getTop(),
                    Math.max(pw - renderInfo.margin.getLeft() - renderInfo.margin.getRight(), 0),
                    Math.max(ph - renderInfo.margin.getTop() - renderInfo.margin.getBottom(), 0)
            );
            renderInfo.origin = NTxValueByType.getNNumberElement2Or1OrHAlign(node, ctx, NTxPropName.ORIGIN)
                    .orElseUse(() -> NTxValueByType.getNNumberElement2Or1OrHAlign(node, ctx, NTxPropName.AT))
                    .orElse(new NTxElemNumber2(NElement.ofDouble(0), NElement.ofDouble(0)));
            renderInfo.position = NTxValueByType.getNNumberElement2Or1OrHAlign(node, ctx, NTxPropName.POSITION)
                    .orElseUse(() -> NTxValueByType.getNNumberElement2Or1OrHAlign(node, ctx, NTxPropName.AT))
                    .orElse(new NTxElemNumber2(NElement.ofDouble(0), NElement.ofDouble(0)));

            {
                NTxElemNumber2 double2OrHAlign = NTxValueByType.getNNumberElement2Or1OrHAlign(node, ctx, NTxPropName.SIZE).orElse(
                        new NTxElemNumber2((NNumberElement) NElement.ofDouble(100.0), (NNumberElement) NElement.ofDouble(100.0))
                );

                renderInfo.componentSize = new NTxDouble2(
                        renderInfo.parentWithMarginRef.x(double2OrHAlign.getX()).get(),
                        renderInfo.parentWithMarginRef.y(double2OrHAlign.getY()).get()
                );
            }
        }
        return renderInfo;
    }
    public static NTxValueFontCache getNodeFontNoCache(NTxNode node, NTxNodeRendererContext ctx) {
        NTxValueFontCache renderInfo = new NTxValueFontCache();
        NElement e = NTxValueByType.getElement(node, ctx, NTxPropName.FONT_SIZE).orNull();
        NTxSizeRef sr = ctx.sizeRef();
        NOptional<Double> srpx = sr.x(e);
        NOptional<Double> srpy = sr.y(e);
        renderInfo.fontSize = NTxSize.ofElement(NTxValueByType.getElement(node, ctx, NTxPropName.FONT_SIZE).orNull());
        renderInfo.fontXSize = srpx.orElse(16.0);
        renderInfo.fontYSize = srpy.orElse(16.0);
        renderInfo.fontItalic = NTxValueByType.getBoolean(node, ctx, NTxPropName.FONT_ITALIC, "italic").orElse(false);
        renderInfo.fontStrike = NTxValueByType.getBoolean(node, ctx, NTxPropName.FONT_STRIKE, "strike").orElse(false);
        renderInfo.fontUnderline = NTxValueByType.getBoolean(node, ctx, NTxPropName.FONT_UNDERLINED, "underlined").orElse(false);
        renderInfo.fontBold = NTxValueByType.getBoolean(node, ctx, NTxPropName.FONT_BOLD, "bold").orElse(false);
        renderInfo.fontFamily = NTxValueByType.getStringOrName(node, ctx, NTxPropName.FONT_FAMILY).orElse("Serif");
        renderInfo.font = NTxFontBySizeResolver.INSTANCE.getFont(renderInfo.fontFamily, Font.PLAIN | (renderInfo.fontItalic ? Font.ITALIC : 0) | (renderInfo.fontBold ? Font.BOLD : 0),
                srpx.orElse(16.0),
                srpy.orElse(16.0),
                ctx.graphics()
        );


        NtxFontInfo f = new NtxFontInfo();
        f.size = renderInfo.fontSize;
        f.italic = renderInfo.fontItalic;
        f.bold = renderInfo.fontBold;
        f.family = renderInfo.fontFamily;
        renderInfo.fontInfo = f;
        return renderInfo;
    }

    public static NTxValueCommonCache getNodeCommonCache(NTxNode node, NTxNodeRendererContext ctx) {
        return node.getAndSetRenderCache(NTxValueCommonCache.class, false,
                () -> getNodeCommonNoCache(node, ctx)
        ).get();
    }

    public static NTxValueSizeCache getNodeSizeCache(NTxNode node, NTxNodeRendererContext ctx) {
        return node.getAndSetRenderCache(NTxValueSizeCache.class, false,
                () -> getNodeSizeNoCache(node, ctx)
        ).get();
    }

    public static NTxValueFontCache getNodeFontCache(NTxNode node, NTxNodeRendererContext ctx) {
        return node.getAndSetRenderCache(NTxValueFontCache.class, false,
                () -> getNodeFontNoCache(node, ctx)
        ).get();
    }

    public static Font getFont(NTxNode node, NTxNodeRendererContext ctx) {
        return getNodeFontCache(node, ctx).font;
    }

    public static NtxFontInfo getFontInfo(NTxNode t, NTxNodeRendererContext ctx) {
        return getNodeFontCache(t, ctx).fontInfo;
    }

    public static NTxDouble2 getRoundCornerArcs(NTxNode t, NTxNodeRendererContext ctx) {
        return NTxValue.of(ctx.computePropertyValue(t, NTxPropName.ROUND_CORNER).orNull()).asDouble2OrDouble().orNull();
    }

    public static int getColSpan(NTxNode node, NTxNodeRendererContext ctx) {
        return (int) node.getAndSetRenderCache("colspan", false,
                () -> {
                    {
                        Integer i = NTxValueByType.getInt(node, ctx, NTxPropName.COLSPAN).orElse(1);
                        if (i == null) {
                            return 1;
                        } else if (i <= 0) {
                            return 1;
                        } else {
                            return i;
                        }
                    }
                }
        ).get();
    }

    public static int getRowSpan(NTxNode node, NTxNodeRendererContext ctx) {
        return (int) node.getAndSetRenderCache("rowspan", false,
                () -> {
                    {
                        Integer i = NTxValueByType.getInt(node, ctx, NTxPropName.ROWSPAN).orElse(1);
                        if (i == null) {
                            return 1;
                        } else if (i <= 0) {
                            return 1;
                        } else {
                            return i;
                        }
                    }
                }
        ).get();
    }

    public static double getColWeight(NTxNode node, NTxNodeRendererContext ctx) {
        return (double) node.getAndSetRenderCache(NTxPropName.COLWEIGHT, false,
                () -> {
                    {
                        Double i = NTxValueByType.getDouble(node, ctx, NTxPropName.COLWEIGHT).orElse(0.0);
                        if (i == null || Double.isNaN(i) || Double.isInfinite(i)) {
                            return 0.0;
                        } else if (i <= 0) {
                            return 0.0;
                        } else {
                            return i;
                        }
                    }
                }
        ).get();
    }

    public static double getRowWeight(NTxNode node, NTxNodeRendererContext ctx) {
        return (double) node.getAndSetRenderCache(NTxPropName.ROWWEIGHT, false,
                () -> {
                    {
                        Double i = NTxValueByType.getDouble(node, ctx, NTxPropName.ROWWEIGHT).orElse(0.0);
                        if (i == null || Double.isNaN(i) || Double.isInfinite(i)) {
                            return 0.0;
                        } else if (i <= 0) {
                            return 0.0;
                        } else {
                            return i;
                        }
                    }
                }
        ).get();
    }

    public static Boolean get3D(NTxNode t, NTxNodeRendererContext ctx) {
        return NTxValueByType.getBoolean(t, ctx, NTxPropName.THEED).orElse(false);
    }

    public static Boolean getRaised(NTxNode t, NTxNodeRendererContext ctx) {
        return NTxValueByType.getBoolean(t, ctx, NTxPropName.RAISED).orElse(false);
    }

    public static NOptional<NTxShadow> readStyleAsShadow(NTxNode t, String s, NTxNodeRendererContext ctx) {
        Object sv = ctx.computePropertyValue(t, s).orNull();
        NTxValue o = NTxValue.of(sv);
        NTxSizeRef sr = ctx.sizeRef();
        if (sv instanceof NElement && ((NElement) sv).isListContainer()) {
            NTxShadow shadow = new NTxShadow();
            for (Map.Entry<String, NTxValue> e : o.argsOrBodyMap().entrySet()) {
                switch (e.getKey()) {
                    case "distance":
                    case "shift":
                    case "origin": {
                        NOptional<NTxPoint2D> d = e.getValue().asPoint2DOrDouble();
                        if (d.isPresent()) {
                            shadow.setTranslation(NTxUtils.point2DasRelative(d.get(),sr));
                        } else {
                            //return (NOptional) d;
                        }
                        break;
                    }
                    case "radius": {
                        NOptional<Double> d = e.getValue().asDouble();
                        if (d.isPresent()) {
                            shadow.setRadius(d.get());
                        } else {
                            //return (NOptional) d;
                        }
                        break;
                    }
                    case "alpha": {
                        NOptional<Double> d = e.getValue().asDouble();
                        if (d.isPresent()) {
                            shadow.setAlpha(d.get());
                        } else {
                            //return (NOptional) d;
                        }
                        break;
                    }
                    case "shear": {
                        NOptional<NTxPoint2D> d = e.getValue().asPoint2DOrDouble();
                        if (d.isPresent()) {
                            shadow.setShear(NTxUtils.point2DasRelative(d.get(),sr));
                        } else {
                            //return (NOptional) d;
                        }
                        break;
                    }
                    case "zoom": {
                        NOptional<NTxPoint2D> d = e.getValue().asPoint2DOrDouble();
                        if (d.isPresent()) {
                            shadow.setZoom(NTxUtils.point2DasRelative(d.get(),sr));
                        } else {
                            //return (NOptional) d;
                        }
                        break;
                    }
                    case "color": {
                        NOptional<Color> d = e.getValue().asColor();
                        if (d.isPresent()) {
                            shadow.setColor(d.get());
                        } else {
                            //return (NOptional) d;
                        }
                        break;
                    }
                    default: {
                        return NOptional.ofNamedEmpty("shadow");
                    }
                }
            }
            if(shadow.getTranslation()==null){
                shadow.setTranslation(NTxUtils.point2DasRelative(new NTxPoint2D(1, 1),sr));
            }
            if(shadow.getRadius()<=0){
                shadow.setRadius(2);
            }
            return NOptional.of(shadow);
        }
        NOptional<Boolean> rb = NTxValue.of(sv).asBoolean();
        if (rb.isPresent()) {
            if (rb.get()) {
                NTxShadow ss = new NTxShadow();
                ss.setTranslation(NTxUtils.point2DasRelative(new NTxPoint2D(1, 1),sr));
                ss.setRadius(2);
                return NOptional.of(ss);
            }
        }
        NOptional<NTxPoint2D> rd = NTxValue.of(sv).asPoint2DOrDouble();
        if (rd.isPresent()) {
            NTxShadow ss = new NTxShadow();
            ss.setTranslation(NTxUtils.point2DasRelative(new NTxPoint2D(rd.get().getX(), rd.get().getY()),sr));
            ss.setRadius(2);
            return NOptional.of(ss);
        }
        return NOptional.ofNamedEmpty("shadow");
    }

    public static Paint getForegroundColor(NTxNode t, NTxNodeRendererContext ctx, boolean force) {
        Paint c = getNodeCommonCache(t, ctx).foregroundColor;
        if (force && c == null) {
            return Color.BLACK;
        }
        return c;
    }

    public static Paint resolveGridColor(NTxNode node, NTxNodeRendererContext ctx) {
        return NTxValueByType.getPaint(node, ctx, NTxPropName.GRID_COLOR).orElse(Color.BLACK);
    }

    public static Paint resolveBackgroundColor(NTxNode node, NTxNodeRendererContext ctx) {
        return getNodeCommonCache(node, ctx).backgroundColor;
    }

    public static boolean isDrawContour(NTxNode node, NTxNodeRendererContext ctx) {
        return getNodeCommonCache(node, ctx).drawContour;
    }

    public static boolean requireDrawGrid(NTxNode t, NTxNodeRendererContext ctx) {
        return NTxValueByType.getBoolean(t, ctx, NTxPropName.DRAW_GRID, "grid").orElse(false);
    }

    public static boolean requireFillBackground(NTxNode t, NTxNodeRendererContext ctx) {
        return getNodeCommonCache(t, ctx).fillBackground;
    }

    public static int getColumns(NTxNode node, NTxNodeRendererContext ctx) {
        return NTxValueByType.getInt(node, ctx, NTxPropName.COLUMNS, "cols").orElse(-1);
    }

    public static int getRows(NTxNode node, NTxNodeRendererContext ctx) {
        return NTxValueByType.getInt(node, ctx, NTxPropName.ROWS, "rows").orElse(-1);
    }

    public static boolean isDebug(NTxNode p, NTxNodeRendererContext ctx) {
        return /*true || */getDebugLevel(p, ctx) > 0;
    }

    public static int getDebugLevel(NTxNode node, NTxNodeRendererContext ctx) {
        return getNodeCommonCache(node, ctx).debugLevel;
    }

    public static Color getDebugColor(NTxNode node, NTxNodeRendererContext ctx) {
        return getNodeCommonCache(node, ctx).debugColor;
    }

    public static NOptional<NTxPoint2D> getStyleAsShadowDistance(Object sv, NTxNodeRendererContext ctx) {
        NTxValue o = NTxValue.of(sv);
        double ww = ctx.parentBounds().getWidth();
        double hh = ctx.parentBounds().getHeight();
        if (o.isNumber()) {
            NOptional<Number> n = o.asNumber();
            if (n.isPresent()) {
                return NOptional.of(new NTxPoint2D(
                        n.get().doubleValue() / 100.0 * ww,
                        n.get().doubleValue() / 100.0 * hh
                ));
            }
        } else if(o.isBoolean()){
            return NOptional.of(new NTxPoint2D(
                    1 / 100.0 * ww,
                    1 / 100.0 * hh
            ));
        } else {
            NOptional<NTxPoint2D> n = o.asPoint2D();
            if (n.isPresent()) {
                return NOptional.of(new NTxPoint2D(
                        n.get().getX() / 100.0 * ww,
                        n.get().getY() / 100.0 * hh
                ));
            }
        }
        return NOptional.ofNamedEmpty("shadow-distance");
    }
}
