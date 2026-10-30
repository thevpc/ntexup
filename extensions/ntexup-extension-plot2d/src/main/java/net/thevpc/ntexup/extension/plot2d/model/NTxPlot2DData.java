package net.thevpc.ntexup.extension.plot2d.model;

import net.thevpc.ntexup.api.renderer.NtxPageLifecycle;
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
    public boolean pending = false;

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
        double[] yy = this.yy;
        // 500ms reveal driven by the page clock so that the page's single frame
        // driver owns the repaint: reading System.currentTimeMillis() without
        // requesting a frame left the page painted on the t~0 frame, where every
        // value is scaled to nearly zero, until something else repainted it.
        long revealMillis = 500;
        double td = 1;
        if (rendererContext.isAnimate()) {
            NtxPageLifecycle lifecycle = rendererContext.pageLifecycle().orNull();
            if (lifecycle != null && lifecycle.isShowing()) {
                long t = lifecycle.showElapsedMillis();
                if (t < revealMillis) {
                    td = t / (double) revealMillis;
                    rendererContext.requestFrame();
                }
            }
        }
        double[] yy2 = new double[yy.length];
        for (int i = 0; i < yy.length; i++) {
            yy2[i] = yy[i]*td;
        }
        return yy2;
    }

}
