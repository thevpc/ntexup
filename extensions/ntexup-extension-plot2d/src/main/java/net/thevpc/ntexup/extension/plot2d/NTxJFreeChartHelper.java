package net.thevpc.ntexup.extension.plot2d;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.extension.plot2d.model.NTxDrawContext;
import net.thevpc.ntexup.extension.plot2d.model.NTxPlot2DData;
import net.thevpc.ntexup.extension.plot2d.model.NTxPlotType;
import net.thevpc.nuts.util.NStringUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.geom.Rectangle2D;

public class NTxJFreeChartHelper {
    static void drawCurves(NTxNode p,NTxNodeRendererContext rendererContext, NTxDrawContext drawContext){
        NTxBounds2 bounds = rendererContext.parentBounds();
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (int j = 0; j < drawContext.allData.size(); j++) {
            NTxPlot2DData pd = drawContext.allData.get(j);
            XYSeries series = new XYSeries(NStringUtils.firstNonBlankTrimmed(pd.title, "Curve " + (j+1)));
            double[] yy2 = pd.animatedYY(rendererContext);
            for (int i = 0; i < pd.xx.length; i++) {
                series.add(pd.xx[i], yy2[i]);
            }
            dataset.addSeries(series);
        }


        NumberAxis xAxis = new NumberAxis(NStringUtils.firstNonBlankTrimmed(drawContext.xLabel,"X"));
        xAxis.setAutoRangeIncludesZero(false);
        NumberAxis yAxis = new NumberAxis(NStringUtils.firstNonBlankTrimmed(drawContext.yLabel,"Y"));
        yAxis.setAutoRange(false); // disable auto-scaling
        yAxis.setRange(drawContext.gridMinY, drawContext.gridMaxY); // fixed min/max
        XYPlot plot = new XYPlot();
        plot.setRangeAxis(yAxis);
        plot.setDomainAxis(xAxis);


        DrawingSupplier supplier = plot.getDrawingSupplier();

        plot.setBackgroundPaint(null);       // plot area
        plot.setOutlinePaint(null);          // remove plot border
        for (int i = 0; i < drawContext.allData.size(); i++) {
            NTxPlot2DData pd = drawContext.allData.get(i);
            if(pd.pld.plotType== NTxPlotType.CURVE) {
                XYSeriesCollection dataset2 = new XYSeriesCollection();
                XYSeries series = new XYSeries(NStringUtils.firstNonBlankTrimmed(pd.title, "Curve " + (i+1)));
                double[] yy2 = pd.animatedYY(rendererContext);
                for (int jx = 0; jx < pd.xx.length; jx++) {
                    series.add(pd.xx[jx], yy2[jx]);
                }
                dataset2.addSeries(series);
                plot.setDataset(i, dataset2);
                XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, pd.lineShapes);
                //XYLineAndShapeRenderer renderer = new GradientXYLineAndShapeRenderer(pd, dataset, drawContext);

                plot.setRenderer(i, renderer);
            }else if(pd.pld.plotType== NTxPlotType.BAR){
                XYSeriesCollection dataset2 = new XYSeriesCollection();
                XYSeries series = new XYSeries(NStringUtils.firstNonBlankTrimmed(pd.title, "Bar " + (i+1)));
                double[] yy2 = pd.animatedYY(rendererContext);
                for (int jx = 0; jx < pd.xx.length; jx++) {
                    series.add(pd.xx[jx], yy2[jx]);
                }
                dataset2.addSeries(series);
                plot.setDataset(i, dataset2);
                plot.setRenderer(i, new XYBarRenderer());
            }else if(pd.pld.plotType== NTxPlotType.AREA){
                XYSeriesCollection dataset2 = new XYSeriesCollection();
                XYSeries series = new XYSeries(NStringUtils.firstNonBlankTrimmed(pd.title, "Area " + (i+1)));
                double[] yy2 = pd.animatedYY(rendererContext);
                for (int jx = 0; jx < pd.xx.length; jx++) {
                    series.add(pd.xx[jx], yy2[jx]);
                }
                dataset2.addSeries(series);
                plot.setDataset(i, dataset2);
                //XYAreaRenderer renderer = new GradientXYAreaRenderer(drawContext.gridMinY,drawContext.gridMaxY);
                XYAreaRenderer renderer = new XYAreaRenderer();
                plot.setRenderer(i, renderer);
            }

            plot.getRenderer().setSeriesPaint(i, supplier.getNextPaint());
        }
        // Create chart

        JFreeChart chart = new JFreeChart(NStringUtils.firstNonBlankTrimmed(drawContext.title,null),
                JFreeChart.DEFAULT_TITLE_FONT,
                plot,
                drawContext.legend);

//        JFreeChart chart = ChartFactory.createXYLineChart(
//                null,
//                "X",
//                "Y",
//                plot,
//                PlotOrientation.VERTICAL,
//                true,
//                true,
//                false
//        );
        chart.setBackgroundPaint(null);      // chart area

        chart.draw(rendererContext.graphics().graphics2D(), new Rectangle2D.Double(bounds.getX(),bounds.getY(),bounds.getWidth(),bounds.getHeight()));
    }

}
