package net.thevpc.ntexup.extension.plot2d;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.extension.plot2d.expr.NTxPlotNExprEvaluator;
import net.thevpc.ntexup.extension.plot2d.model.NTxDrawContext;
import net.thevpc.ntexup.extension.plot2d.model.NTxFunctionPlotInfo;
import net.thevpc.ntexup.extension.plot2d.model.NTxPlot2DData;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.math.NDoubleRange;
import net.thevpc.nuts.time.NChronometer;
import net.thevpc.nuts.util.NArrays;
import net.thevpc.nuts.util.NDoubleFunction;
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
                NElement ev = rendererContext.evalExpression(pld.color).orNull();
                if (ev != null && !ev.isNull()) {
                    Stroke stroke = rendererContext.graphics().createStroke(ev);
                    if (stroke != null) {
                        pd.stroke = stroke;
                    }
                }
            }

            switch (pd.pld.source) {
                case FUNCTION_X: {
                    NDoubleFunction ff = NTxPlotNExprEvaluator.compileFunctionX(pld, rendererContext);
                    if (ff != null) {
                        NChronometer c = NChronometer.of();
                        pd.prepareX(ff, xValues, minMaxY);
                        c.stop();
                        rendererContext.log(NMsg.ofC("FUNCTION_X : %s", c));
                        drawContext.allData.add(pd);
                    }
                    break;
                }
            }
        }
        drawContext.build();
        return drawContext;
    }
}
