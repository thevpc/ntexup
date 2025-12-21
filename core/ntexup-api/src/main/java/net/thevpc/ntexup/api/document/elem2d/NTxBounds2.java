package net.thevpc.ntexup.api.document.elem2d;

import net.thevpc.ntexup.api.util.NTxUtils;

public class NTxBounds2 {
    private Double x;
    private Double y;
    private Double w;
    private Double h;

    public static  NTxBounds2 ofFull() {
        return new NTxBounds2(0.0, 0.0, 100.0, 100.0);
    }

    public NTxBounds2(Number x, Number y, Number w, Number h) {
        this.x = x == null ? null : x.doubleValue();
        this.y = y == null ? null : y.doubleValue();
        this.w = w == null ? null : w.doubleValue();
        this.h = h == null ? null : h.doubleValue();
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public Double getWidth() {
        return w;
    }

    public Double getHeight() {
        return h;
    }

    public Double getMinX() {
        return x;
    }

    public Double getCenterX() {
        if (x != null && w != null) {
            return x + w / 2;
        }
        return null;
    }

    public Double getCenterY() {
        if (y != null && h != null) {
            return y + h / 2;
        }
        return null;
    }

    public Double getMinY() {
        return y;
    }

    public Double getMaxX() {
        if (x == null || w == null) {
            return null;
        }
        return x + w;
    }

    public Double getMaxY() {
        if (y == null || h == null) {
            return null;
        }
        return y + h;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + w + ", " + h + ')';
    }

    public NTxBounds2 expand(NTxBounds2 s) {
        if (s == null) {
            return new NTxBounds2(x, y, w, h);
        }

        Double xx1 = NTxUtils.min(getMinX(), s.getMinX());
        Double yy1 = NTxUtils.min(getMinY(), s.getMinY());
        Double xx2 = NTxUtils.max(getMaxX(), s.getMaxX());
        Double yy2 = NTxUtils.max(getMaxY(), s.getMaxY());
        return new NTxBounds2(
                xx1,
                yy1,
                NTxUtils.doubleOf(xx2) - NTxUtils.doubleOf(xx1),
                NTxUtils.doubleOf(yy2) - NTxUtils.doubleOf(yy1)
        );
    }

}
