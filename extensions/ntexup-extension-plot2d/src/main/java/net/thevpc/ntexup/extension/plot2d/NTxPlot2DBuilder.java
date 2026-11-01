package net.thevpc.ntexup.extension.plot2d;


import net.thevpc.nuts.elem.NArrayElementBuilder;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElementBuilder;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.parser.NTxAllArgumentReader;
import net.thevpc.ntexup.api.renderer.NTxRenderDump;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.extension.plot2d.model.NTxDrawContext;
import net.thevpc.ntexup.extension.plot2d.model.NTxFunctionPlotInfo;
import net.thevpc.ntexup.extension.plot2d.model.NTxPlot2DData;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.List;


/**
 *
 */
public class NTxPlot2DBuilder implements NTxNodeBuilder {

    /**
     * Maximum number of points a series contributes to a dump render; beyond
     * that the series is marked {@code truncated:true} to keep the dump small.
     */
    private static final int DUMP_MAX_POINTS = 4096;

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.PLOT2D)
                .parseParam()
                .matchesAny().end()
                .processChildren(this::processChildren)
                .renderComponent(this::renderMain)
        ;
    }

    public void processChildren(NTxAllArgumentReader info, NTxNodeBuilderContext buildContext) {
        List<NTxFunctionPlotInfo> all = new NTxFunctionPlotInfoLoader().loadBody(info.element(), buildContext);
        info.node().setUserObject("def", all);
    }


    public void renderMain(NTxRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        NTxDrawContext drawContext = NTxDrawContextRenderCompiler.compile(rendererContext);
        NTxRenderDump renderDump = rendererContext.renderDump().orNull();
        if (renderDump != null) {
            //the chart is rasterized into java2d primitives by jfreechart, so the
            //model itself would be lost in a dump: contribute it explicitly
            renderDump.dump(rendererContext, "model", toDumpElement(drawContext));
        }
        NTxJFreeChartHelper.drawCurves(node, rendererContext, drawContext);
    }

    static NElement toDumpElement(NTxDrawContext dc) {
        NObjectElementBuilder ob = NElement.ofObjectBuilder();
        if (dc.title != null) {
            ob.add("title", NElement.of(dc.title));
        }
        if (dc.xLabel != null) {
            ob.add("xLabel", NElement.of(dc.xLabel));
        }
        if (dc.yLabel != null) {
            ob.add("yLabel", NElement.of(dc.yLabel));
        }
        if (!dc.legend) {
            ob.add("legend", NElement.of(false));
        }
        NArrayElementBuilder yRange = NElement.ofArrayBuilder();
        yRange.add(NElement.of(dumpNum(dc.gridMinY)));
        yRange.add(NElement.of(dumpNum(dc.gridMaxY)));
        ob.add("yRange", yRange.build());
        NArrayElementBuilder series = NElement.ofArrayBuilder();
        for (NTxPlot2DData d : dc.allData) {
            NObjectElementBuilder s = NElement.ofObjectBuilder();
            NTxFunctionPlotInfo pld = d.pld;
            if (pld != null) {
                if (pld.plotType != null) {
                    s.add("type", NElement.of(pld.plotType.name().toLowerCase()));
                }
                if (pld.fexpr != null) {
                    s.add("expr", pld.fexpr);
                }
            }
            if (d.title != null) {
                s.add("name", NElement.of(d.title));
            }
            if (d.color != null) {
                s.add("color", NElement.of(toHex(d.color)));
            }
            if (d.stroke instanceof BasicStroke bs) {
                s.add("strokeWidth", NElement.of(dumpNum(bs.getLineWidth())));
            }
            if (d.pending) {
                s.add("pending", NElement.of(true));
            }
            if (d.xx != null && d.yy != null) {
                int n = Math.min(d.xx.length, d.yy.length);
                boolean truncated = n > DUMP_MAX_POINTS;
                if (truncated) {
                    n = DUMP_MAX_POINTS;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < n; i++) {
                    if (i > 0) {
                        sb.append(' ');
                    }
                    sb.append(dumpNum(d.xx[i])).append(',').append(dumpNum(d.yy[i]));
                }
                s.add("count", NElement.of(Math.min(d.xx.length, d.yy.length)));
                s.add("points", NElement.of(sb.toString()));
                if (truncated) {
                    s.add("truncated", NElement.of(true));
                }
            }
            series.add(s.build());
        }
        ob.add("series", series.build());
        return ob.build();
    }

    private static String dumpNum(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return String.valueOf(v);
        }
        double r = Math.round(v * 1e6) / 1e6;
        if (r == Math.rint(r) && Math.abs(r) < 1e15) {
            return String.valueOf((long) r);
        }
        return String.valueOf(r);
    }

    private static String toHex(Color c) {
        if (c.getAlpha() == 255) {
            return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        }
        return String.format("#%02x%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }
}
