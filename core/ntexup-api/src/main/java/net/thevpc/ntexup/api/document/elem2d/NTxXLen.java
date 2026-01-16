package net.thevpc.ntexup.api.document.elem2d;

import java.util.Objects;

public class NTxXLen {
    private double value;
    private boolean root;

    public static NTxXLen ofMaxParent() {
        return ofParent(100);
    }

    public static NTxXLen ofMaxRoot() {
        return ofRoot(100);
    }

    public static NTxXLen ofMin() {
        return new NTxXLen(0, false);
    }

    public static NTxXLen ofParent(double d) {
        return new NTxXLen(d, false);
    }

    public static NTxXLen ofRoot(double d) {
        return new NTxXLen(d, true);
    }

    public NTxXLen(double value, boolean root) {
        if (!Double.isFinite(value)) {
            this.value = 0;
        } else if (value <= 0) {
            this.value = 0;
        } else {
            this.value = value;
        }
        this.root = root;
    }

    public double value(NTxBounds2D parentBounds, NTxBounds2D screenBounds) {
        if (this.root) {
            if (value >= 100) {
                return screenBounds.getWidth();
            }
            return value / 100 * screenBounds.getWidth();
        } else {
            if (value >= 100) {
                return parentBounds.getWidth();
            }
            return value / 100 * parentBounds.getWidth();
        }
    }

    public double getValue() {
        return value;
    }

    public boolean isRoot() {
        return root;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NTxXLen xLen = (NTxXLen) o;
        return Double.compare(value, xLen.value) == 0 && root == xLen.root;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, root);
    }

    @Override
    public String toString() {
        if (root) {
            return "XLenOfParent(" + value + ")";
        }
        return "XLenOfRoot(" + value + ")";
    }
}
