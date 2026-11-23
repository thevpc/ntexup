package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/**
 * {@code spread-pct(a, b, ...)}: (max - min) / |mean| in percent, the "the solvers agree
 * within x %" number; arrays are flattened.
 */
public class NTxFunctionSpreadPct extends NTxNumericFunction {
    @Override
    public String name() {
        return "spread-pct";
    }

    @Override
    protected int minArgs() {
        return 1;
    }

    @Override
    protected int maxArgs() {
        return 64;
    }

    @Override
    protected String usage() {
        return "spread-pct(a, b, ...)";
    }

    @Override
    protected NElement compute(Call c) {
        double[] v = NTxStats.nonEmpty(c, c.flatten());
        double spread = NTxStats.max(c, v) - NTxStats.min(c, v);
        double s = 0;
        for (double d : v) {
            s += d;
        }
        double mean = s / v.length;
        if (mean == 0) {
            return c.error("mean is zero, a relative spread is undefined");
        }
        return NElement.ofDouble(100.0 * spread / Math.abs(mean));
    }
}
