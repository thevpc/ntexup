package net.thevpc.ntexup.api.eval;

import net.thevpc.ntexup.api.document.NTxArrow;
import net.thevpc.ntexup.api.document.NTxArrowType;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;

import java.awt.*;

public class NTxValueByType {

    public static NOptional<NTxArrowType> getArrowType(NTxRendererContext ctx, String propName, String... propNames) {
        return NTxValue.of(ctx.computePropertyValue(propName, propNames).orNull()).asArrowType();
    }

    public static NOptional<NTxArrow> getArrow(NTxRendererContext ctx, String propName, String... propNames) {
        return NTxValue.of(ctx.computePropertyValue(propName, propNames).orNull()).asArrow();
    }

    public static NOptional<Boolean> getBoolean(NTxRendererContext ctx, String propName, String... propNames) {
        return NTxValue.of(ctx.computePropertyValue(propName, propNames).orNull()).asBoolean();
    }

    public static NOptional<Integer> getIntOrBoolean(NTxRendererContext ctx, String propName, String... propNames) {
        return NTxValue.of(ctx.computePropertyValue(propName, propNames).orNull()).asIntOrBoolean();
    }

    public static NOptional<Integer> getInt(NTxRendererContext ctx, String propName, String... propNames) {
        return NTxValue.of(ctx.computePropertyValue(propName, propNames).orNull()).asInt();
    }

    public static NOptional<String> getStringOrName(NTxRendererContext ctx, String propName, String... synonyms) {
        return NTxValue.of(ctx.computePropertyValue(propName, synonyms).orNull()).asStringOrName();
    }

    public static NOptional<net.thevpc.ntexup.api.renderer.text.NTxTextWrap> getTextWrap(NTxRendererContext ctx) {
        String s = getStringOrName(ctx, net.thevpc.ntexup.api.document.style.NTxPropName.TEXT_WRAP, net.thevpc.ntexup.api.document.style.NTxPropName.WRAP).orNull();
        return net.thevpc.ntexup.api.renderer.text.NTxTextWrap.parse(s);
    }

    public static NOptional<net.thevpc.ntexup.api.renderer.text.NTxTextAlign> getTextAlign(NTxRendererContext ctx) {
        String s = getStringOrName(ctx, net.thevpc.ntexup.api.document.style.NTxPropName.TEXT_ALIGN, net.thevpc.ntexup.api.document.style.NTxPropName.TEXT_HALIGN).orNull();
        if (s == null) {
            String a = getStringOrName(ctx, net.thevpc.ntexup.api.document.style.NTxPropName.ALIGN).orNull();
            if (a != null) {
                NOptional<net.thevpc.ntexup.api.renderer.text.NTxTextAlign> parsed = net.thevpc.ntexup.api.renderer.text.NTxTextAlign.parse(a);
                if (parsed.isPresent()) {
                    return parsed;
                }
            }
        }
        return net.thevpc.ntexup.api.renderer.text.NTxTextAlign.parse(s);
    }

    public static NOptional<Paint> getPaint(NTxRendererContext ctx, String propName, String... propNames) {
        NTxValue r = NTxValue.of(ctx.computePropertyValue(propName, propNames).orNull());
        return NOptional.of(r.asPaint().orNull());
    }

    public static NOptional<Color> getColor(NTxRendererContext ctx, String propName, String... propNames) {
        NTxValue r = NTxValue.of(ctx.computePropertyValue(propName, propNames).orNull());
        return NOptional.of(r.asColor().orNull());
    }

    public static NOptional<Double> getDouble(NTxRendererContext ctx, String s) {
        return NTxValue.of(ctx.computePropertyValue(s).orNull()).asDouble();
    }

    public static NOptional<NTxDouble2> getDouble2(NTxRendererContext ctx, String s) {
        return NTxValue.of(ctx.computePropertyValue(s).orNull()).asDouble2();
    }

    public static NOptional<NElement> getElement(NTxRendererContext ctx, String s) {
        return ctx.computePropertyValue(s);
    }

    public static NOptional<NTxDouble2> getDouble2OrHAlign(NTxRendererContext ctx, String s) {
        return NTxValue.of(ctx.computePropertyValue(s).orNull()).asDouble2OrHAlign();
    }

    public static NOptional<NTxElemNumber2> getNNumberElement2Or1OrHAlign(NTxRendererContext ctx, String s,String... synonyms) {
        return NTxValue.of(ctx.computePropertyValue(s,synonyms).orNull()).asNNumberElement2Or1OrHAlign();
    }

    public static NOptional<NTxDouble4> getDouble4(NTxRendererContext ctx, String s) {
        return NTxValue.of(ctx.computePropertyValue(s).orNull()).asDouble4();
    }

    public static NOptional<double[]> getDoubleArray(NTxRendererContext ctx, String s) {
        return NTxValue.of(ctx.computePropertyValue(s).orNull()).asDoubleArray();
    }

    public static NOptional<double[]> getDoubleArray(NTxRendererContext ctx, String s, String... synonyms) {
        return NTxValue.of(ctx.computePropertyValue(s, synonyms).orNull()).asDoubleArray();
    }

    public static NOptional<NTxMargin> getMargin(NTxRendererContext ctx, String s) {
        return NTxValue.of(ctx.computePropertyValue(s).orNull()).asPadding();
    }

    public static NOptional<NTxRotation> getRotation(NTxRendererContext ctx, String s) {
        return NTxValue.of(ctx.computePropertyValue(s).orNull()).asRotation();
    }
}
