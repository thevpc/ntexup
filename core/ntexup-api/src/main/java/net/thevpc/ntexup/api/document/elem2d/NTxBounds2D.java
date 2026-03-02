package net.thevpc.ntexup.api.document.elem2d;

import net.thevpc.ntexup.api.util.NTxUtils;

public class NTxBounds2D {
    private Double x;
    private Double y;
    private Double w;
    private Double h;

    public static NTxBounds2D ofFull() {
        return ofWidth(0.0, 0.0, 100.0, 100.0);
    }

    public static NTxBounds2D ofWidth(Number x, Number y, Number w, Number h) {
        return new NTxBounds2D(x, y, w, h);
    }

    public static NTxBounds2D of(Number x1, Number x2, Number y1, Number y2) {
        return new NTxBounds2D(x1, y1, NTxUtils.distanceDouble(x1,x2), NTxUtils.distanceDouble(y1,y2));
    }
    public static NTxBounds2D ofEmpty() {
        return new NTxBounds2D(0.0, 0.0, 0.0, 0.0);
    }

    private NTxBounds2D(Number x, Number y, Number w, Number h) {
        this.x = x == null ? null : x.doubleValue();
        this.y = y == null ? null : y.doubleValue();
        this.w = w == null ? null : w.doubleValue();
        this.h = h == null ? null : h.doubleValue();
    }


    public NTxDouble2 size() {
        return new NTxDouble2(widthX(), widthY());
    }

    public Double widthX() {
        return w;
    }

    public Double widthY() {
        return h;
    }

    public Double minX() {
        return x;
    }

    public Double centerX() {
        if (x != null && w != null) {
            return x + w / 2;
        }
        return null;
    }

    public Double centerY() {
        if (y != null && h != null) {
            return y + h / 2;
        }
        return null;
    }

    public Double minY() {
        return y;
    }

    public Double maxX() {
        if (x == null || w == null) {
            return null;
        }
        return x + w;
    }

    public Double maxY() {
        if (y == null || h == null) {
            return null;
        }
        return y + h;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + w + ", " + h + ')';
    }

    public NTxBounds2D expand(NTxBounds2D s) {
        if (s == null) {
            return new NTxBounds2D(x, y, w, h);
        }

        Double xx1 = NTxUtils.min(minX(), s.minX());
        Double yy1 = NTxUtils.min(minY(), s.minY());
        Double xx2 = NTxUtils.max(maxX(), s.maxX());
        Double yy2 = NTxUtils.max(maxY(), s.maxY());
        return new NTxBounds2D(
                xx1,
                yy1,
                NTxUtils.doubleOf(xx2) - NTxUtils.doubleOf(xx1),
                NTxUtils.doubleOf(yy2) - NTxUtils.doubleOf(yy1)
        );
    }

}
