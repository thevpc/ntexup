package net.thevpc.ntexup.extension.plot2d;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.extension.plot2d.expr.NTxPlotNExprResolver;
import net.thevpc.ntexup.extension.plot2d.model.NTxDrawContext;
import net.thevpc.ntexup.extension.plot2d.model.NTxFunctionPlotInfo;
import net.thevpc.ntexup.extension.plot2d.model.NTxPlot2DData;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.math.NDoubleRange;
import net.thevpc.nuts.mon.NChronometer;
import net.thevpc.nuts.util.NArrays;
import net.thevpc.nuts.math.NDoubleFunction;
import net.thevpc.nuts.text.NMsg;

import java.awt.*;
import java.util.List;

class NTxDrawContextRenderCompiler {
    public static NTxDrawContext compile(NTxRendererContext rendererContext) {
        double[] xValues = NTxValue.of(rendererContext.evalExpression(rendererContext.computePropertyValue("x").orElse(NElement.ofDoubleArray(NArrays.range(-100.0, 100, 1)))).orNull())
                .asDoubleArray().orElse(NArrays.range(100.0, 100, 1));
        if (xValues.length == 0) {
            xValues = new double[]{0};
        }
        double minY = -100;
        double maxY = 100;
        boolean zoom = true;
        NDoubleRange minMaxY = NDoubleRange.of();

        Paint color = rendererContext.getForegroundColor(true);

        NTxBounds2D bounds = rendererContext.parentBounds2D();
        NTxDrawContext drawContext = new NTxDrawContext(bounds, xValues, minY, maxY, zoom, minMaxY);
        java.util.List<NTxFunctionPlotInfo> plotDefinitions = (List<NTxFunctionPlotInfo>) rendererContext.node().getUserObject("def").orNull();

//        int steps = (int) (bounds.getHeight() * 2);

        for (NTxFunctionPlotInfo pld : plotDefinitions) {
            NTxPlot2DData pd = new NTxPlot2DData(pld);
            if (color instanceof java.awt.Color) {
                pd.color = (Color) color;
            }
            if (pld.color != null) {
                NElement ev = rendererContext.evalExpression(pld.color).orNull();
                pd.color = NTxValue.of(ev).asColor().orElse(pd.color);
            }
            if (pld.title != null) {
                NElement ev = rendererContext.evalExpression(pld.title).orNull();
                pd.title = NTxValue.of(ev).asString().orNull();
            }
            if (pld.stroke != null) {
                NElement ev = rendererContext.evalExpression(pld.stroke).orNull();
                if (ev != null && !ev.isNull()) {
                    Stroke stroke = rendererContext.graphics().createStroke(ev);
                    if (stroke != null) {
                        pd.stroke = stroke;
                    }
                }
            }

            if (pd.pld.source == null && pld.y != null) {
                pd.pld.source = pld.x != null ? net.thevpc.ntexup.extension.plot2d.model.NTxPlotSource.VALUE_XY : net.thevpc.ntexup.extension.plot2d.model.NTxPlotSource.VALUE_X;
            }

            if (pd.pld.source != null) {
                switch (pd.pld.source) {
                    case FUNCTION_X: {
                        NDoubleFunction ff = NTxPlotNExprResolver.compileFunctionX(pld, rendererContext);
                        if (ff != null) {
                            NChronometer c = NChronometer.of();
                            pd.prepareX(ff, xValues, minMaxY);
                            c.stop();
                            rendererContext.log(NMsg.ofC("FUNCTION_X : %s", c));
                            drawContext.allData.add(pd);
                        }
                        break;
                    }
                    case VALUE_XY:
                    case VALUE_X: {
                        NElement yElem = rendererContext.evalExpression(pld.y).orNull();
                        double[] yArr = NTxValue.of(yElem).asDoubleArray().orNull();
                        if (yArr == null && yElem != null && yElem.isListContainer()) {
                            java.util.List<Double> yList = new java.util.ArrayList<>();
                            for (NElement c : yElem.asListContainer().get().children()) {
                                net.thevpc.nuts.util.NOptional<Double> dv = NTxValue.of(c).asDouble();
                                if (dv.isPresent()) {
                                    yList.add(dv.get());
                                } else if (c.isNumber()) {
                                    yList.add(c.asDoubleValue().orElse(0.0));
                                }
                            }
                            if (!yList.isEmpty()) {
                                yArr = yList.stream().mapToDouble(Double::doubleValue).toArray();
                            }
                        }
                        if (yArr != null && yArr.length > 0) {
                            double[] xArr = xValues;
                            if (pld.x != null) {
                                NElement xElem = rendererContext.evalExpression(pld.x).orNull();
                                double[] parsedX = NTxValue.of(xElem).asDoubleArray().orNull();
                                if (parsedX != null && parsedX.length > 0) {
                                    xArr = parsedX;
                                }
                            }
                            if (xArr.length != yArr.length) {
                                if (xArr.length == 1) {
                                    double start = xArr[0];
                                    xArr = new double[yArr.length];
                                    for (int i = 0; i < yArr.length; i++) {
                                        xArr[i] = start + i;
                                    }
                                } else {
                                    xArr = NArrays.linear(xArr[0], xArr[xArr.length - 1], yArr.length);
                                }
                            }
                            pd.xx = xArr;
                            pd.yy = yArr;
                            for (double yv : yArr) {
                                if (Double.isFinite(yv)) {
                                    minMaxY.add(yv);
                                }
                            }
                            drawContext.allData.add(pd);
                        }
                        break;
                    }
                }
            }
        }
        drawContext.build();
        return drawContext;
    }
}
