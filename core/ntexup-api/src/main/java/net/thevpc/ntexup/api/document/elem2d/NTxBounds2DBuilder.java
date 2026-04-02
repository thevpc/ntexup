package net.thevpc.ntexup.api.document.elem2d;

import net.thevpc.nuts.math.NDoubleRange;

public class NTxBounds2DBuilder {
    private NDoubleRange xx = NDoubleRange.of();
    private NDoubleRange yy = NDoubleRange.of();

    public void add(NTxPoint2D point) {
        xx.add(point.x);
        yy.add(point.y);
    }

    public NTxBounds2D build() {
        return NTxBounds2D.of(
                xx.min(),
                xx.max(),
                yy.min(),
                yy.max()
        );
    }
}
