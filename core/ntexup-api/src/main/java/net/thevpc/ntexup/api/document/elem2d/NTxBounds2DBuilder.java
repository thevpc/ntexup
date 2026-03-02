package net.thevpc.ntexup.api.document.elem2d;

import net.thevpc.ntexup.api.util.NTxMinMax;

public class NTxBounds2DBuilder {
    private NTxMinMax xx = new NTxMinMax();
    private NTxMinMax yy = new NTxMinMax();

    public void add(NTxPoint2D point) {
        xx.registerValue(point.x);
        yy.registerValue(point.y);
    }

    public NTxBounds2D build() {
        return NTxBounds2D.of(
                xx.getMin(),
                xx.getMax(),
                yy.getMin(),
                yy.getMax()
        );
    }
}
