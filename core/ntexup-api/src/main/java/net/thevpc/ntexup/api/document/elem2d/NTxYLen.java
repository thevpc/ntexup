package net.thevpc.ntexup.api.document.elem2d;

import java.util.Objects;

public class NTxYLen {
    private double value;
    private boolean root;

    public static NTxYLen ofMaxParent() {
        return ofParent(100);
    }

    public static NTxYLen ofMaxRoot() {
        return ofRoot(100);
    }

    public static NTxYLen ofMin() {
        return new NTxYLen(0, false);
    }

    public static NTxYLen ofParent(double d) {
        return new NTxYLen(d, false);
    }

    public static NTxYLen ofRoot(double d) {
        return new NTxYLen(d, true);
    }

    public NTxYLen(double value, boolean root) {
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
                return screenBounds.getHeight();
            }
            return value / 100 * screenBounds.getHeight();
        } else {
            if (value >= 100) {
                return parentBounds.getHeight();
            }
            return value / 100 * parentBounds.getHeight();
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
        NTxYLen yLen = (NTxYLen) o;
        return Double.compare(value, yLen.value) == 0 && root == yLen.root;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, root);
    }

    @Override
    public String toString() {
        if (root) {
            return "YLenOfParent(" + value + ")";
        }
        return "YLenOfRoot(" + value + ")";
    }
}
