package net.thevpc.ntexup.api.eval;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.document.style.*;

import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxSizeRef;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.api.util.NtxFontInfo;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NNumberElement;
import net.thevpc.nuts.util.NOptional;

import java.awt.*;
import java.util.Map;

public class NTxValueByName {
    public static final boolean AUTO_FORCE=false;
    public static NOptional<Paint> getColorProperty(String propName, NTxRendererContext ctx) {
        NTxValue r = NTxValue.of(ctx.computePropertyValue(propName).orElse(null));
        return NOptional.of(r.asColor().orElse(null));
    }

    protected static boolean isPreserveShapeRatio(NTxRendererContext ctx) {
        return getNodeSizeCache(ctx).preserveRatio;
    }

//    private static NTxDouble2 getSize(NTxDouble2 minSize, NTxRendererContext ctx) {
//        NTxDouble2 size = getNodeSizeCache(ctx).componentSize;
//
//        boolean shapeRatio = isPreserveShapeRatio(ctx);
//        //ratio depends on the smallest
//        double sx = size.getX();
//        double sy = size.getY();
//        if (minSize != null) {
//            sx = Math.max(minSize.getX(), sx);
//            sy = Math.max(minSize.getY(), sy);
//        }
//        if (shapeRatio) {
//            if (sx > sy) {
//                sx = sy;
//            }
//            if (sy > sx) {
//                sy = sx;
//            }
//            return new NTxDouble2(
//                    sx,
//                    sy
//            );
//        }
//        return new NTxDouble2(
//                sx,
//                sy
//        );
//    }

    public static NTxDouble2 getOrigin(NTxRendererContext ctx, NTxDouble2 a) {
        NTxElemNumber2 double2OrHAlign = getNodeSizeCache(ctx).origin;
        NTxSizeRef sr = new NTxSizeRef(a.getX(), a.getY(), ctx.getGlobalBounds().getWidth(), ctx.getGlobalBounds().getHeight());
        return new NTxDouble2(
                sr.x(double2OrHAlign.getX()).get(),
                sr.y(double2OrHAlign.getY()).get()
        );
    }

    public static NTxDouble2 getInnerOrigin(NTxRendererContext ctx) {
        NTxDouble2 size = getNodeSizeCache(ctx).parentBoundsWithMargin.size();
        NTxElemNumber2 double2OrHAlign = getNodeSizeCache(ctx).innerOrigin;
        NTxSizeRef sr = new NTxSizeRef(size.getX(), size.getY(), size.getX(), size.getY());
        return new NTxDouble2(
                sr.x(double2OrHAlign.getX()).get(),
                sr.y(double2OrHAlign.getY()).get()
        );
    }

    public static NTxDouble2 getInnerPosition(NTxRendererContext ctx) {
        NTxDouble2 size = getNodeSizeCache(ctx).parentBoundsWithMargin.size();
        NTxElemNumber2 double2OrHAlign = getNodeSizeCache(ctx).innerPosition;
        NTxSizeRef sr = new NTxSizeRef(size.getX(), size.getY(), size.getX(), size.getY());
        return new NTxDouble2(
                sr.x(double2OrHAlign.getX()).get(),
                sr.y(double2OrHAlign.getY()).get()
        );
    }


    public static NTxRotation getRotation(NTxRendererContext ctx) {
        return NTxValueByType.getRotation(ctx, NTxPropName.ROTATE).orNull();
    }

    public static NTxDouble2 getPosition(NTxRendererContext ctx, NTxDouble2 a) {
        NTxElemNumber2 double2OrHAlign = getNodeSizeCache(ctx).position;
        NTxSizeRef sr = new NTxSizeRef(a.getX(), a.getY(), ctx.getGlobalBounds().getWidth(), ctx.getGlobalBounds().getHeight());
        return new NTxDouble2(
                sr.x(double2OrHAlign.getX()).get(),
                sr.y(double2OrHAlign.getY()).get()
        );
    }

    public static NElement getStroke(NTxRendererContext ctx) {
        return getNodeCommonCache(ctx).stroke;
    }

    public static NTxBounds2D defaultSelfBounds(NTxRendererContext ctx) {
        //        NTxBounds2 parentBounds = ctx.parentBounds();
        NTxSizeRef parentSizeWithMarginRef = getNodeSizeCache(ctx).parentWithMarginRef;
        NTxBounds2D parentBoundsWithMargin = getNodeSizeCache(ctx).parentBoundsWithMargin;

        NTxDouble2 referenceSelfSize = getNodeSizeCache(ctx).referenceComponentSize;

        NTxDouble2 pos = getPosition(ctx, new NTxDouble2(parentSizeWithMarginRef.getParentWidth(), parentSizeWithMarginRef.getParentHeight()));

        NTxDouble2 origin = getOrigin(ctx, referenceSelfSize);


        NTxBounds2D p2 = new NTxBounds2D(
                pos.getX() - origin.getX() + parentBoundsWithMargin.getX(),
                pos.getY() - origin.getY() + parentBoundsWithMargin.getY(),
                referenceSelfSize.getX(),
                referenceSelfSize.getY()
        );

        NTxDouble2 pos2 = getInnerPosition(ctx);

        NTxDouble2 origin2 = getInnerOrigin(ctx);

        NTxBounds2D p3 = new NTxBounds2D(
                pos2.getX() - origin2.getX() + p2.getX(),
                pos2.getY() - origin2.getY() + p2.getY(),
                referenceSelfSize.getX(),
                referenceSelfSize.getY()
        );
//        if(true){
//            return p2;
//        }
        return p3;
    }

    public static NTxBounds2D selfBounds(NTxDouble2 selfSize, NTxDouble2 minSize, NTxRendererContext ctx) {
//        NTxBounds2 parentBounds = ctx.parentBounds();
        NTxSizeRef parentWithMarginRef = getNodeSizeCache(ctx).parentWithMarginRef;
        NTxBounds2D parentBoundsWithMargin = getNodeSizeCache(ctx).parentBoundsWithMargin;

//        if (selfSize == null) {
//            selfSize = getSize(minSize, ctx);
//        }

        NTxDouble2 pos = getPosition(ctx, new NTxDouble2(parentWithMarginRef.getParentWidth(), parentWithMarginRef.getParentHeight()));

        NTxDouble2 origin = getOrigin(ctx, selfSize);


        NTxBounds2D p2 = new NTxBounds2D(
                pos.getX() - origin.getX() + parentBoundsWithMargin.getX(),
                pos.getY() - origin.getY() + parentBoundsWithMargin.getY(),
                selfSize.getX(),
                selfSize.getY()
        );

        NTxDouble2 pos2 = getInnerPosition(ctx);

        NTxDouble2 origin2 = getInnerOrigin(ctx);

        NTxBounds2D p3 = new NTxBounds2D(
                pos2.getX() - origin2.getX() + p2.getX(),
                pos2.getY() - origin2.getY() + p2.getY(),
                selfSize.getX(),
                selfSize.getY()
        );
//        if(true){
//            return p2;
//        }
        return p3;
    }

    public static boolean isVisible(NTxRendererContext ctx) {
        return (boolean) ctx.node().getAndSetRenderCache("visible", AUTO_FORCE,
                () -> {
                    NOptional<Boolean> b = NTxValueByType.getBoolean(ctx, NTxPropName.HIDE);
                    if (b.isPresent()) {
                        return !b.get();
                    } else {
                        b = NTxValueByType.getBoolean(ctx, "show");
                        if (b.isPresent()) {
                            return !b.get();
                        } else {
                            b = NTxValueByType.getBoolean(ctx, "visible");
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

    public static double getFontSize(NTxRendererContext ctx) {
        NTxValueFontCache c = getNodeFontCache(ctx);
        return Math.min(c.fontXSize, c.fontYSize);
    }

    public static String getFontFamily(NTxRendererContext ctx) {
        return getNodeFontCache(ctx).fontFamily;
    }

    public static boolean isFontUnderlined(NTxRendererContext ctx) {
        return getNodeFontCache(ctx).fontUnderline;
    }

    public static boolean isFontStrike(NTxRendererContext ctx) {
        return getNodeFontCache(ctx).fontStrike;
    }

    public static boolean isFontBold(NTxRendererContext ctx) {
        return getNodeFontCache(ctx).fontBold;
    }

    public static boolean isFontItalic(NTxRendererContext ctx) {
        return getNodeFontCache(ctx).fontItalic;
    }

    public static NTxValueCommonCache getNodeCommonNoCache(NTxRendererContext ctx) {
        NTxValueCommonCache renderInfo = new NTxValueCommonCache();
        NElement e = NTxValueByType.getElement(ctx, NTxPropName.FONT_SIZE).orNull();
        NTxSizeRef sr = ctx.sizeRef();
        renderInfo.stroke = NTxValueByType.getElement(ctx, NTxPropName.STROKE).orNull();


        renderInfo.foregroundColor = NTxValueByType.getPaint(ctx, NTxPropName.FOREGROUND_COLOR, "foreground", "color", "fg").orElse(null);
        renderInfo.backgroundColor = NTxValueByType.getPaint(ctx, NTxPropName.BACKGROUND_COLOR, "background", "bg").orNull();
        renderInfo.fillBackground = NTxValueByType.getBoolean(ctx, NTxPropName.FILL_BACKGROUND, "fill").orElse(false);
        renderInfo.debugLevel = NTxValueByType.getIntOrBoolean(ctx, NTxPropName.DEBUG).orElse(0);
        renderInfo.debugColor = (Color) NTxValueByType.getPaint(ctx, NTxPropName.DEBUG_COLOR).orElse(Color.GRAY);
        renderInfo.drawContour = NTxValueByType.getBoolean(ctx, NTxPropName.DRAW_CONTOUR, "contour").orElse(false);
        return renderInfo;
    }

    public static NTxValueSizeCache getNodeSizeNoCache(NTxRendererContext ctx) {
        NTxValueSizeCache renderInfo = new NTxValueSizeCache();
        NTxSizeRef sr = ctx.sizeRef();
        renderInfo.preserveRatio = NTxValue.of(ctx.computePropertyValue(NTxPropName.PRESERVE_ASPECT_RATIO).orNull()).asBoolean().orElse(false);
        {

            NElement marginElement = ctx.computePropertyValue(NTxPropName.MARGIN).orNull();

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
            NTxBounds2D parentBounds = ctx.parentBounds();
            double pw = parentBounds.getWidth();
            double ph = parentBounds.getHeight();
            renderInfo.parentWithMarginRef = new NTxSizeRef(
                    Math.max(pw - renderInfo.margin.getLeft() - renderInfo.margin.getRight(), 0),
                    Math.max(ph - renderInfo.margin.getTop() - renderInfo.margin.getBottom(), 0),
                    ctx.getGlobalBounds().getWidth(),
                    ctx.getGlobalBounds().getHeight()
            );
            renderInfo.parentBoundsWithMargin = new NTxBounds2D(
                    parentBounds.getX() + renderInfo.margin.getLeft(),
                    parentBounds.getY() + renderInfo.margin.getTop(),
                    Math.max(pw - renderInfo.margin.getLeft() - renderInfo.margin.getRight(), 0),
                    Math.max(ph - renderInfo.margin.getTop() - renderInfo.margin.getBottom(), 0)
            );

            renderInfo.origin = NTxValueByType.getNNumberElement2Or1OrHAlign(ctx, NTxPropName.ORIGIN,NTxPropName.AT)
                    .orElse(new NTxElemNumber2(NElement.ofDouble(0), NElement.ofDouble(0)));

            renderInfo.innerOrigin = NTxValueByType.getNNumberElement2Or1OrHAlign(ctx, NTxPropName.CONTENT_ORIGIN,NTxPropName.ALIGN)
                    .orElse(new NTxElemNumber2(NElement.ofDouble(0), NElement.ofDouble(0)));

            renderInfo.position = NTxValueByType.getNNumberElement2Or1OrHAlign(ctx, NTxPropName.POSITION,NTxPropName.AT)
                    .orElse(new NTxElemNumber2(NElement.ofDouble(0), NElement.ofDouble(0)));

            renderInfo.innerPosition = NTxValueByType.getNNumberElement2Or1OrHAlign(ctx, NTxPropName.CONTENT_POSITION)
                    .orElseGetOptionalFrom(() -> NTxValueByType.getNNumberElement2Or1OrHAlign(ctx, NTxPropName.ALIGN))
                    .orElse(new NTxElemNumber2(NElement.ofDouble(0), NElement.ofDouble(0)));

            {
                NTxElemNumber2 double2OrHAlign = NTxValueByType.getNNumberElement2Or1OrHAlign(ctx, NTxPropName.SIZE).orElse(
                        new NTxElemNumber2((NNumberElement) NElement.ofDouble(100.0), (NNumberElement) NElement.ofDouble(100.0))
                );
                renderInfo.allowedComponentSize = new NTxDouble2(
                        renderInfo.parentWithMarginRef.x(double2OrHAlign.getX()).get(),
                        renderInfo.parentWithMarginRef.y(double2OrHAlign.getY()).get()
                );
            }

            //ratio depends on the smallest
            double sx = renderInfo.allowedComponentSize.getX();
            double sy = renderInfo.allowedComponentSize.getY();
            if (renderInfo.preserveRatio) {
                if (sx > sy) {
                    sx = sy;
                }
                if (sy > sx) {
                    sy = sx;
                }
                renderInfo.referenceComponentSize=new NTxDouble2(
                        sx,
                        sy
                );
            }else {
                renderInfo.referenceComponentSize=new NTxDouble2(
                        sx,
                        sy
                );
            }

        }
        return renderInfo;
    }

    public static NTxValueFontCache getNodeFontNoCache(NTxRendererContext ctx) {
        NTxValueFontCache renderInfo = new NTxValueFontCache();
        NElement e = NTxValueByType.getElement(ctx, NTxPropName.FONT_SIZE).orNull();
        NTxSizeRef sr = ctx.sizeRef();
        NOptional<Double> srpx = sr.x(e);
        NOptional<Double> srpy = sr.y(e);
        renderInfo.fontSize = NTxSize.ofElement(NTxValueByType.getElement(ctx, NTxPropName.FONT_SIZE).orNull());
        renderInfo.fontXSize = srpx.orElse(16.0);
        renderInfo.fontYSize = srpy.orElse(16.0);
        renderInfo.fontItalic = NTxValueByType.getBoolean(ctx, NTxPropName.FONT_ITALIC, "italic").orElse(false);
        renderInfo.fontStrike = NTxValueByType.getBoolean(ctx, NTxPropName.FONT_STRIKE, "strike").orElse(false);
        renderInfo.fontUnderline = NTxValueByType.getBoolean(ctx, NTxPropName.FONT_UNDERLINED, "underlined").orElse(false);
        renderInfo.fontBold = NTxValueByType.getBoolean(ctx, NTxPropName.FONT_BOLD, "bold").orElse(false);
        renderInfo.fontFamily = NTxValueByType.getStringOrName(ctx, NTxPropName.FONT_FAMILY).orElse("Serif");
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

    public static NTxValueCommonCache getNodeCommonCache(NTxRendererContext ctx) {
        return ctx.node().getAndSetRenderCache(NTxValueCommonCache.class, AUTO_FORCE,
                () -> getNodeCommonNoCache(ctx)
        ).get();
    }

    public static NTxValueSizeCache getNodeSizeCache(NTxRendererContext ctx) {
        return ctx.node().getAndSetRenderCache(NTxValueSizeCache.class, AUTO_FORCE,
                () -> getNodeSizeNoCache(ctx)
        ).get();
    }

    public static NTxValueFontCache getNodeFontCache(NTxRendererContext ctx) {
        return ctx.node().getAndSetRenderCache(NTxValueFontCache.class, AUTO_FORCE,
                () -> getNodeFontNoCache(ctx)
        ).get();
    }

    public static Font getFont(NTxRendererContext ctx) {
        return getNodeFontCache(ctx).font;
    }

    public static NtxFontInfo getFontInfo(NTxRendererContext ctx) {
        return getNodeFontCache(ctx).fontInfo;
    }

    public static NTxDouble2 getRoundCornerArcs(NTxRendererContext ctx) {
        return NTxValue.of(ctx.computePropertyValue(NTxPropName.ROUND_CORNER).orNull()).asDouble2OrDouble().orNull();
    }

    public static int getColSpan(NTxRendererContext ctx) {
        return (int) ctx.node().getAndSetRenderCache("colspan", AUTO_FORCE,
                () -> {
                    {
                        Integer i = NTxValueByType.getInt(ctx, NTxPropName.COLSPAN).orElse(1);
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

    public static int getRowSpan(NTxRendererContext ctx) {
        return (int) ctx.node().getAndSetRenderCache("rowspan", AUTO_FORCE,
                () -> {
                    {
                        Integer i = NTxValueByType.getInt(ctx, NTxPropName.ROWSPAN).orElse(1);
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

    public static double getColWeight(NTxRendererContext ctx) {
        return (double) ctx.node().getAndSetRenderCache(NTxPropName.COLWEIGHT, AUTO_FORCE,
                () -> {
                    {
                        Double i = NTxValueByType.getDouble(ctx, NTxPropName.COLWEIGHT).orElse(0.0);
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

    public static double getRowWeight(NTxRendererContext ctx) {
        return (double) ctx.node().getAndSetRenderCache(NTxPropName.ROWWEIGHT, AUTO_FORCE,
                () -> {
                    {
                        Double i = NTxValueByType.getDouble(ctx, NTxPropName.ROWWEIGHT).orElse(0.0);
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

    public static Boolean get3D(NTxRendererContext ctx) {
        return NTxValueByType.getBoolean(ctx, NTxPropName.THEED).orElse(false);
    }

    public static Boolean getRaised(NTxRendererContext ctx) {
        return NTxValueByType.getBoolean(ctx, NTxPropName.RAISED).orElse(false);
    }

    public static NOptional<NTxShadow> readStyleAsShadow(String s, NTxRendererContext ctx) {
        Object sv = ctx.computePropertyValue(s).orNull();
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
                            shadow.setTranslation(NTxUtils.point2DasRelative(d.get(), sr));
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
                            shadow.setShear(NTxUtils.point2DasRelative(d.get(), sr));
                        } else {
                            //return (NOptional) d;
                        }
                        break;
                    }
                    case "zoom": {
                        NOptional<NTxPoint2D> d = e.getValue().asPoint2DOrDouble();
                        if (d.isPresent()) {
                            shadow.setZoom(NTxUtils.point2DasRelative(d.get(), sr));
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
            if (shadow.getTranslation() == null) {
                shadow.setTranslation(NTxUtils.point2DasRelative(new NTxPoint2D(1, 1), sr));
            }
            if (shadow.getRadius() <= 0) {
                shadow.setRadius(2);
            }
            return NOptional.of(shadow);
        }
        NOptional<Boolean> rb = NTxValue.of(sv).asBoolean();
        if (rb.isPresent()) {
            if (rb.get()) {
                NTxShadow ss = new NTxShadow();
                ss.setTranslation(NTxUtils.point2DasRelative(new NTxPoint2D(1, 1), sr));
                ss.setRadius(2);
                return NOptional.of(ss);
            }
        }
        NOptional<NTxPoint2D> rd = NTxValue.of(sv).asPoint2DOrDouble();
        if (rd.isPresent()) {
            NTxShadow ss = new NTxShadow();
            ss.setTranslation(NTxUtils.point2DasRelative(new NTxPoint2D(rd.get().getX(), rd.get().getY()), sr));
            ss.setRadius(2);
            return NOptional.of(ss);
        }
        return NOptional.ofNamedEmpty("shadow");
    }

    public static Paint getForegroundColor(NTxRendererContext ctx, boolean force) {
        Paint c = getNodeCommonCache(ctx).foregroundColor;
        if (force && c == null) {
            return Color.BLACK;
        }
        return c;
    }

    public static Paint resolveGridColor(NTxRendererContext ctx) {
        return NTxValueByType.getPaint(ctx, NTxPropName.GRID_COLOR).orElse(Color.BLACK);
    }

    public static Paint resolveBackgroundColor(NTxRendererContext ctx) {
        return getNodeCommonCache(ctx).backgroundColor;
    }

    public static boolean isDrawContour(NTxRendererContext ctx) {
        return getNodeCommonCache(ctx).drawContour;
    }

    public static boolean requireDrawGrid(NTxRendererContext ctx) {
        return NTxValueByType.getBoolean(ctx, NTxPropName.DRAW_GRID, "grid").orElse(false);
    }

    public static boolean requireFillBackground(NTxRendererContext ctx) {
        return getNodeCommonCache(ctx).fillBackground;
    }

    public static int getColumns(NTxRendererContext ctx) {
        return NTxValueByType.getInt(ctx, NTxPropName.COLUMNS, "cols").orElse(-1);
    }

    public static int getRows(NTxRendererContext ctx) {
        return NTxValueByType.getInt(ctx, NTxPropName.ROWS, "rows").orElse(-1);
    }

    public static boolean isDebug(NTxRendererContext ctx) {
        return /*true || */getDebugLevel(ctx) > 0;
    }

    public static int getDebugLevel(NTxRendererContext ctx) {
        return getNodeCommonCache(ctx).debugLevel;
    }

    public static Color getDebugColor(NTxRendererContext ctx) {
        return getNodeCommonCache(ctx).debugColor;
    }

    public static NOptional<NTxPoint2D> getStyleAsShadowDistance(Object sv, NTxRendererContext ctx) {
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
        } else if (o.isBoolean()) {
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
