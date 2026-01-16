package net.thevpc.ntexup.api.document.elem2d;


public class NTxPoint {
    private double x;
    private double y;
    private boolean root;

    public static NTxPoint ofRoot(double x, double y) {
        return new NTxPoint(x, y, true);
    }

    public static NTxPoint ofParent(NTxDouble2 d) {
        return new NTxPoint(d.getX(), d.getY(), false);
    }

    public static NTxPoint ofParent(NTxPoint2D d) {
        return new NTxPoint(d.getX(), d.getY(), false);
    }

    public static NTxPoint ofParent(double x, double y) {
        return new NTxPoint(x, y, false);
    }

    public NTxPoint(double x, double y, boolean root) {
        this.x = x;
        this.y = y;
        this.root = root;
    }

    public NTxDouble2 valueDouble2(NTxBounds2D parentBounds, NTxBounds2D screenBounds) {
        return new NTxDouble2(
                (root ? NTxXLen.ofRoot(x) : NTxXLen.ofParent(x)).value(parentBounds, screenBounds),
                (root ? NTxYLen.ofRoot(y) : NTxYLen.ofParent(y)).value(parentBounds, screenBounds)
        );
    }

    public NTxPoint2D valueHPoint2D(NTxBounds2D parentBounds, NTxBounds2D screenBounds) {
        return new NTxPoint2D(
                (root ? NTxXLen.ofRoot(x) : NTxXLen.ofParent(x)).value(parentBounds, screenBounds),
                (root ? NTxYLen.ofRoot(y) : NTxYLen.ofParent(y)).value(parentBounds, screenBounds)
        );
    }
}
