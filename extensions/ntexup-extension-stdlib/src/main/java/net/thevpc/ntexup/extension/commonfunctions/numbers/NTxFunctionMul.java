package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/** {@code mul(a, b, ...)}: product; arrays are flattened. */
public class NTxFunctionMul extends NTxNumericFunction {
    @Override
    public String name() {
        return "mul";
    }

    @Override
    protected int minArgs() {
        return 2;
    }

    @Override
    protected int maxArgs() {
        return 64;
    }

    @Override
    protected String usage() {
        return "mul(a, b, ...)";
    }

    @Override
    protected NElement compute(Call c) {
        double s = 1;
        for (double v : c.flatten()) {
            s *= v;
        }
        return NElement.ofDouble(s);
    }
}
