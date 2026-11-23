package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/** {@code spread(a, b, ...)}: max - min; arrays are flattened. */
public class NTxFunctionSpread extends NTxNumericFunction {
    @Override
    public String name() {
        return "spread";
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
        return "spread(a, b, ...)";
    }

    @Override
    protected NElement compute(Call c) {
        double[] v = c.flatten();
        return NElement.ofDouble(NTxStats.max(c, v) - NTxStats.min(c, v));
    }
}
