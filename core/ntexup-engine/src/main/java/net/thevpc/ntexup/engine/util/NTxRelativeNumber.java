package net.thevpc.ntexup.engine.util;

public class NTxRelativeNumber {
    private final double value;
    private final NTxRelativeNumberMode mode;

    public static NTxRelativeNumber ofParent(double value) {
        return of(value, NTxRelativeNumberMode.PAGE);
    }

    public static NTxRelativeNumber ofPage(double value) {
        return of(value, NTxRelativeNumberMode.PAGE);
    }

    public static NTxRelativeNumber of(double value, NTxRelativeNumberMode mode) {
        if (mode == null) {
            mode = NTxRelativeNumberMode.PARENT;
        }
        return new NTxRelativeNumber(value, mode);
    }

    public NTxRelativeNumber(double value, NTxRelativeNumberMode mode) {
        this.value = value;
        this.mode = mode;
    }

    public double compute(double parentSize, double pageSize) {
        switch (mode == null ? NTxRelativeNumberMode.PARENT : mode) {
            case PAGE: {
                return pageSize * value / 100.0;
            }
        }
        return parentSize * value / 100.0;
    }
}
