package net.thevpc.ntexup.extension.plot2d.deprecated;

import net.thevpc.ntexup.api.document.NTxArrow;
import net.thevpc.ntexup.api.document.NTxArrowType;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.NTxVector2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.extension.plot2d.model.NTxDrawContext;
import net.thevpc.ntexup.extension.plot2d.model.NTxPlot2DData;
import net.thevpc.ntexup.extension.plot2d.util.NTxArrayUtils;

import java.awt.*;

public class NTxDeprecatedDrawHelper {
    public static void drawCurves(NTxNode p,NTxNodeRendererContext rendererContext, NTxDrawContext drawContext) {
        NTxBounds2D selfBounds = rendererContext.selfBounds(p, null, null);
        NTxGraphics g = rendererContext.graphics();
        drawAxises(drawContext, g, p, rendererContext);
        for (NTxPlot2DData pd : drawContext.allData) {
            drawFunction(pd, drawContext, g, p, rendererContext);
        }
        rendererContext.drawContour();
    }


    private static  void drawFunction(NTxPlot2DData pd, NTxDrawContext drawContext, NTxGraphics g, NTxNode p, NTxNodeRendererContext rendererContext) {
        //draw function
        Stroke ostroke = g.getStroke();
        g.setColor(pd.color);
        g.setStroke(pd.stroke);
        double[] xx = pd.xx;
        double[] yy = pd.yy;
        boolean animate = rendererContext.isAnimate();
        long pageStartTime = rendererContext.pageStartTime();
        long now = System.currentTimeMillis();
        long max=500;
        double td = 1;
        if(animate && pageStartTime>0){
            long t=now-pageStartTime;
            if(t<=0) {
                t = max;
            }else if(t>=max){
                t=max;
            }
            td=t/(double)max;
        }

        for (int i = 1; i < xx.length; i++) {
            double yy1 = yy[i];
            if (drawContext.acceptY(yy1)) {
                double yy0 = yy[i - 1];
                if (drawContext.acceptY(yy0)) {
                    if(animate){
                        yy1=yy1*td;
                        yy0=yy0*td;
                    }
                    int fromX = (int) drawContext.xPixels(xx[i - 1]);
                    int fromY = (int) drawContext.yPixels(yy0);
                    int toX = (int) drawContext.xPixels(xx[i]);
                    int toY = (int) drawContext.yPixels(yy1);
                    g.drawLine(fromX, fromY, toX, toY);
                }
            }
        }
        g.setStroke(ostroke);
    }

    private static void drawAxises(NTxDrawContext drawContext, NTxGraphics g, NTxNode p, NTxNodeRendererContext rendererContext) {
        Stroke mainStroke = new BasicStroke(1.0f);
        Stroke stepStroke = new BasicStroke(1.0f, // Line width of 2 pixels
                BasicStroke.CAP_BUTT, // No added decoration at line ends
                BasicStroke.JOIN_MITER, // How segments join
                10.0f, // Miter limit
                new float[]{10.0f, 10.0f}, // The dash pattern array
                0.0f);
        Stroke subStepStroke = new BasicStroke(1.0f, // Line width of 2 pixels
                BasicStroke.CAP_BUTT, // No added decoration at line ends
                BasicStroke.JOIN_MITER, // How segments join
                10.0f, // Miter limit
                new float[]{5.0f, 5.0f}, // The dash pattern array
                0.0f);
        Color mainColor = Color.gray;
        Color stepColor = Color.lightGray;
        Color subStepColor = Color.lightGray;

        double epsilon = 1E-10;
        if (drawContext.gridSubStepX >= 0 && drawContext.gridSubStepX != drawContext.gridStepX) {
            g.setColor(subStepColor);
            Stroke ostroke = g.getStroke();
            g.setStroke(subStepStroke);
            for (double xi : NTxArrayUtils.findMultiplesFastDouble(drawContext.gridXvalues[0], drawContext.gridXvalues[drawContext.gridXvalues.length-1], drawContext.gridSubStepX, epsilon)) {
                int pxi = (int) drawContext.xPixels(xi);
                g.drawLine(pxi, drawContext.componentMinY, pxi, drawContext.componentMinY + drawContext.componentHeight);
            }
            g.setStroke(ostroke);
        }

        if (drawContext.gridSubStepY >= 0 && drawContext.gridSubStepY != drawContext.gridStepY) {
            g.setColor(subStepColor);
            Stroke ostroke = g.getStroke();
            g.setStroke(subStepStroke);
            for (double yi : NTxArrayUtils.findMultiplesFastDouble(drawContext.gridMinY, drawContext.gridMaxY, drawContext.gridSubStepY, epsilon)) {
                int pyi = (int) drawContext.yPixels(yi);
                g.drawLine(drawContext.componentMinX, pyi, drawContext.componentMinX + drawContext.componentWidth, pyi);
            }
            g.setStroke(ostroke);
        }

        if (drawContext.gridStepX >= 0) {
            g.setColor(stepColor);
            Stroke ostroke = g.getStroke();
            g.setStroke(stepStroke);
            for (double xi : NTxArrayUtils.findMultiplesFastDouble(drawContext.gridXvalues[0], drawContext.gridXvalues[drawContext.gridXvalues.length-1], drawContext.gridStepX, epsilon)) {
                int pxi = (int) drawContext.xPixels(xi);
                if (Math.abs(xi) <= epsilon) {
                    g.setColor(mainColor);
                    g.setStroke(mainStroke);
                    g.drawLine(pxi, drawContext.componentMinY, pxi, drawContext.componentMinY + drawContext.componentHeight);
                    g.drawArrayHead(new NTxPoint2D(0, 0), new NTxVector2D(0, 1), new NTxArrow(NTxArrowType.TRIANGLE_FULL));
                    g.setStroke(stepStroke);
                    g.setColor(stepColor);
                } else {
                    g.drawLine(pxi, drawContext.componentMinY, pxi, drawContext.componentMinY + drawContext.componentHeight);
                }
            }
            g.setStroke(ostroke);
        }

        if (drawContext.gridStepY >= 0) {
            g.setColor(stepColor);
            Stroke ostroke = g.getStroke();
            g.setStroke(stepStroke);
            for (double yi : NTxArrayUtils.findMultiplesFastDouble(drawContext.gridMinY, drawContext.gridMaxY, drawContext.gridStepY, epsilon)) {
                int pyi = (int) drawContext.yPixels(yi);
                if (Math.abs(yi) <= epsilon) {
                    g.setColor(mainColor);
                    g.setStroke(mainStroke);
                    g.drawLine(drawContext.componentMinX, pyi, drawContext.componentMinX + drawContext.componentWidth, pyi);
                    g.drawArrayHead(new NTxPoint2D(0, 0), new NTxVector2D(1, 0), new NTxArrow(NTxArrowType.TRIANGLE_FULL));
                    g.setStroke(stepStroke);
                    g.setColor(stepColor);
                } else {
                    g.drawLine(drawContext.componentMinX, pyi, drawContext.componentMinX + drawContext.componentWidth, pyi);
                }
            }
            g.setStroke(ostroke);
        }
    }

}
