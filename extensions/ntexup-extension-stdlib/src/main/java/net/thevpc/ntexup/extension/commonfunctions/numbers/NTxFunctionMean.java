package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/** {@code mean(a, b, ...)}: arithmetic mean; arrays are flattened. */
public class NTxFunctionMean extends NTxNumericFunction {
    @Override
    public String name() {
        return "mean";
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
        return "mean(a, b, ...)";
    }

    @Override
    protected NElement compute(Call c) {
        double[] v = NTxStats.nonEmpty(c, c.flatten());
        double s = 0;
        for (double d : v) {
            s += d;
        }
        return NElement.ofDouble(s / v.length);
    }
}
