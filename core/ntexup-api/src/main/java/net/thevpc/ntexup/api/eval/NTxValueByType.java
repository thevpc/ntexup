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

    public static NOptional<String> getStringOrName(NTxRendererContext ctx, String propName) {
        return NTxValue.of(ctx.computePropertyValue(propName).orNull()).asStringOrName();
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

    public static NOptional<NTxElemNumber2> getNNumberElement2Or1OrHAlign(NTxRendererContext ctx, String s) {
        return NTxValue.of(ctx.computePropertyValue(s).orNull()).asNNumberElement2Or1OrHAlign();
    }

    public static NOptional<NTxDouble4> getDouble4(NTxRendererContext ctx, String s) {
        return NTxValue.of(ctx.computePropertyValue(s).orNull()).asDouble4();
    }

    public static NOptional<double[]> getDoubleArray(NTxRendererContext ctx, String s) {
        return NTxValue.of(ctx.computePropertyValue(s).orNull()).asDoubleArray();
    }

    public static NOptional<NTxMargin> getMargin(NTxRendererContext ctx, String s) {
        return NTxValue.of(ctx.computePropertyValue(s).orNull()).asPadding();
    }

    public static NOptional<NTxRotation> getRotation(NTxRendererContext ctx, String s) {
        return NTxValue.of(ctx.computePropertyValue(s).orNull()).asRotation();
    }
}
