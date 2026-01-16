package net.thevpc.ntexup.extension.plot2d.model;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.util.NTxMinMax;

import java.util.ArrayList;
import java.util.List;

public class NTxDrawContext {

    public String title;
    public double[] gridXvalues;

    public double gridMinY;
    public double gridMaxY;

    public double gridStepX = 10;
    public double gridStepY = 10;

    public double gridSubStepX = 1;
    public double gridSubStepY = 1;

    public double gridWidth;
    public double gridHeight;
    public double componentWidth;
    public double componentHeight;
    public double componentMinX;
    public double componentMinY;
    public List<NTxPlot2DData> allData = new ArrayList<>();
    public boolean legend=true;
    public String xLabel;
    public String yLabel;
    public NTxMinMax minMaxY;
    public boolean zoomY;
    public double userMinY;
    public double userMaxY;

    public NTxDrawContext(NTxBounds2D bounds, double[] gridXvalues, double minY, double maxY, boolean zoomY, NTxMinMax minMaxY) {
        this.componentWidth = bounds.getWidth();
        this.componentHeight = bounds.getHeight();
        this.componentMinX = bounds.getX();
        this.componentMinY = bounds.getY();
        this.gridXvalues = gridXvalues;
        this.minMaxY=minMaxY;
        this.zoomY=zoomY;
        this.userMinY=minY;
        this.userMaxY=maxY;
    }
    public void build(){
        double yWidth = 0;
        double minY=userMinY;
        double maxY=userMaxY;
        if (zoomY && Double.isFinite(minMaxY.getMax()) && Double.isFinite(minMaxY.getMin())) {
            yWidth = minMaxY.getMax() - minMaxY.getMin();
            minY = minMaxY.getMin() - yWidth * 0.1;
            maxY = minMaxY.getMax() + yWidth * 0.1;
        } else {
            if(maxY<=minY){
                maxY=minY+10;
            }
        }
        gridMinY = minY;
        gridMaxY = maxY;
        this.gridWidth = this.gridXvalues[this.gridXvalues.length-1] - this.gridXvalues[0];
        this.gridHeight = gridMaxY - gridMinY;
    }

    public double xPixels(double a) {
        return (a - this.gridXvalues[0]) * componentWidth / gridWidth + componentMinX;
    }

    public double yPixels(double a) {
        return (gridHeight - (a - gridMinY)) * componentHeight / gridHeight + componentMinY;
    }

    public double wPixels(double a) {
        return a * componentWidth / gridWidth;
    }

    public double hPixels(double a) {
        return a * componentHeight / gridHeight;
    }

    public boolean acceptY(double y) {
        return !Double.isNaN(y)
                && Double.isFinite(y)
                && y >= gridMinY
                && y <= gridMaxY;
    }

    public boolean acceptX(double x) {
        return !Double.isNaN(x)
                && Double.isFinite(x)
                && x >= this.gridXvalues[0]
                && x <= this.gridXvalues[this.gridXvalues.length - 1];
    }

}
