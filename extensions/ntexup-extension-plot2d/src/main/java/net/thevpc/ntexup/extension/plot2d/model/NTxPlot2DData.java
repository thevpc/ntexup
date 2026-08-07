package net.thevpc.ntexup.extension.plot2d.model;

import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.nuts.math.NDoubleRange;
import net.thevpc.nuts.math.NDoubleFunction;

import java.awt.*;

public class NTxPlot2DData {
    public double[] xx;
    public double[] yy;
    public String title;
    public Color color = Color.black;
    public Stroke stroke = new BasicStroke(2.0f);
    public NTxFunctionPlotInfo pld;
    public boolean lineShapes=false;

    public NTxPlot2DData(NTxFunctionPlotInfo pld) {
        this.pld = pld;
    }

    public void prepareX(NDoubleFunction f, double[] xx, NDoubleRange minMaxY) {
        this.xx = xx;
        this.yy = new double[xx.length];
        for (int i = 0; i < xx.length; i++) {
            double x = xx[i];
            yy[i] = f.apply(x);
            if (Double.isFinite(yy[i])) {
                minMaxY.add(yy[i]);
            }
        }

    }

    public double[] animatedYY(NTxRendererContext rendererContext) {
        double[] xx = this.xx;
        double[] yy = this.yy;
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
        double[] yy2 = new double[yy.length];
        for (int i = 0; i < yy.length; i++) {
            yy2[i] = yy[i]*td;
        }
        return yy2;
    }

}
