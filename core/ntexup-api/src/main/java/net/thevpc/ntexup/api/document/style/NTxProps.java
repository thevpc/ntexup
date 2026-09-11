package net.thevpc.ntexup.api.document.style;

import java.awt.*;

import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.NTxAlign;
import net.thevpc.ntexup.api.document.elem2d.NTxSize;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;

/**
 * @author vpc
 */
public class NTxProps {

    public static NTxProp fontSize(Number value) {
        return NTxProp.of(NTxPropName.FONT_SIZE, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofNumber(value == null ? null : value.doubleValue())));
    }

    public static NTxProp fontFamily(String value) {
        return NTxProp.of(NTxPropName.FONT_FAMILY, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofString(value)));
    }

    public static NTxProp width(NTxSize size) {
        return NTxProp.of(NTxPropName.WIDTH, size);
    }

    public static NTxProp height(NTxSize size) {
        return NTxProp.of(NTxPropName.HEIGHT, size);
    }

//    public static HProp foregroundColor(Paint color) {
//        return new HProp(HPropName.FOREGROUND_COLOR, color);
//    }

    public static NTxProp foregroundColor(int color) {
        return NTxProp.of(NTxPropName.FOREGROUND_COLOR, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofInt(color)));
    }

    public static NTxProp backgroundColor(int rgb) {
        return NTxProp.of(NTxPropName.BACKGROUND_COLOR, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofInt(rgb)));
    }

    public static NTxProp backgroundColor(Color color) {
        return NTxProp.of(NTxPropName.BACKGROUND_COLOR, color == null ? null : NTxUtils.addCompilerDeclarationPathDummy(NElement.ofInt(color.getRGB())));
    }

//    public static HProp lineColor(Paint color) {
//        return new HProp(HPropName.LINE_COLOR, color);
//    }

    public static NTxProp gridColor(Color color) {
        return NTxProp.of(NTxPropName.GRID_COLOR, color == null ? null : NTxUtils.addCompilerDeclarationPathDummy(NElement.ofInt(color.getRGB())));
    }

//    public static HProp lineColor(int color) {
//        return new HProp(HPropName.LINE_COLOR, new Color(color));
//    }

    public static NTxProp fontBold() {
        return fontBold(true);
    }

    public static NTxProp preserveShapeRatio() {
        return preserveShapeRatio(true);
    }

    public static NTxProp preserveShapeRatio(boolean value) {
        return NTxProp.of(NTxPropName.PRESERVE_ASPECT_RATIO, NTxUtils.addCompilerDeclarationPathDummy((NElement.ofBoolean(value))));
    }

    public static NTxProp drawContour(boolean value) {
        return NTxProp.of(NTxPropName.DRAW_CONTOUR, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofBoolean(value)));
    }

    public static NTxProp drawGrid(boolean value) {
        return NTxProp.of(NTxPropName.DRAW_GRID, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofBoolean(value)));
    }

    public static NTxProp fillBackground(boolean value) {
        return NTxProp.of(NTxPropName.FILL_BACKGROUND, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofBoolean(value)));
    }

    public static NTxProp fontBold(boolean value) {
        return NTxProp.of(NTxPropName.FONT_BOLD, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofBoolean(value)));
    }

    public static NTxProp fontItalic(boolean value) {
        return NTxProp.of(NTxPropName.FONT_ITALIC, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofBoolean(value)));
    }

    public static NTxProp fontUnderlined(boolean value) {
        return NTxProp.of(NTxPropName.FONT_UNDERLINED, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofBoolean(value)));
    }

    public static NTxProp fontStrike(boolean value) {
        return NTxProp.of(NTxPropName.FONT_STRIKE, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofBoolean(value)));
    }

    public static NTxProp position(NTxDouble2 p) {
        return NTxProp.of(NTxPropName.POSITION, p);
    }

    public static NTxProp center() {
        return origin(NTxAlign.CENTER);
    }

    public static NTxProp origin(NTxAlign p) {
        return NTxProp.of(NTxPropName.ORIGIN, p);
    }

    public static NTxProp origin(double x, double y) {
        return NTxProp.of(NTxPropName.ORIGIN, new NTxDouble2(x, y));
    }

    public static NTxProp position(double x, double y) {
        return NTxProp.of(NTxPropName.POSITION, new NTxDouble2(x, y));
    }

    public static NTxProp position(NTxAlign p) {
        return NTxProp.of(NTxPropName.POSITION, p);
    }

    public static NTxProp size(double x, double y) {
        return NTxProp.of(NTxPropName.SIZE, new NTxDouble2(x, y));
    }

    public static NTxProp size(double x) {
        return NTxProp.of(NTxPropName.SIZE, new NTxDouble2(x, x));
    }

    public static NTxProp size(NTxDouble2 size) {
        return NTxProp.of(NTxPropName.SIZE, size);
    }

    public static NTxProp roundCorner(NTxDouble2 o) {
        return NTxProp.of(NTxPropName.ROUND_CORNER, o);
    }

    public static NTxProp roundCorner(double x, double y) {
        return NTxProp.of(NTxPropName.ROUND_CORNER, NTxUtils.addCompilerDeclarationPathDummy(new NTxDouble2(x, y).toElement()));
    }

    public static NTxProp threeD(Boolean b) {
        return NTxProp.of(NTxPropName.THEED, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofBoolean(b)));
    }

    public static NTxProp raised(Boolean b) {
        return NTxProp.of(NTxPropName.RAISED, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofBoolean(b)));
    }

    public static NTxProp columns(int o) {
        return NTxProp.of(NTxPropName.COLUMNS, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofInt(o)));
    }

    public static NTxProp rows(int o) {
        return NTxProp.of(NTxPropName.ROWS, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofInt(o)));
    }

    public static NTxProp rowsWeight(double... w) {
        return NTxProp.of(NTxPropName.ROWS_WEIGHT, NElement.ofDoubleArray(w));
    }

    public static NTxProp colspan(int c) {
        return NTxProp.of(NTxPropName.COLSPAN, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofInt(c)));
    }

    public static NTxProp rowspan(int r) {
        return NTxProp.of(NTxPropName.ROWSPAN, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofInt(r)));
    }

    public static NTxProp styleClasses(String... array) {
        return NTxProp.of(NTxPropName.CLASS, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofStringArray(array)));
    }

    public static NTxProp disabled(boolean b) {
        return NTxProp.of(NTxPropName.HIDE, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofBoolean(b)));
    }

    public static NTxProp name(String name) {
        return NTxProp.of(NTxPropName.NAME, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofString(name)));
    }

    public static NTxProp maxX(double x) {
        return NTxProp.of(NTxPropName.MAX_X, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofDouble(x)));
    }

    public static NTxProp maxY(double x) {
        return NTxProp.of(NTxPropName.MAX_Y, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofDouble(x)));
    }

    public static NTxProp margin(double m) {
        return NTxProp.of(NTxPropName.MARGIN, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofDouble(m)));
    }

    public static NTxProp margin(double x, double y) {
        return NTxProp.of(NTxPropName.MARGIN, NTxUtils.addCompilerDeclarationPathDummy(new NTxDouble2(x, y).toElement()));
    }

    public static NTxProp padding(double p) {
        return NTxProp.of(NTxPropName.PADDING, NTxUtils.addCompilerDeclarationPathDummy(NElement.ofDouble(p)));
    }

    public static NTxProp padding(double x, double y) {
        return NTxProp.of(NTxPropName.PADDING, NTxUtils.addCompilerDeclarationPathDummy(new NTxDouble2(x, y).toElement()));
    }

}
