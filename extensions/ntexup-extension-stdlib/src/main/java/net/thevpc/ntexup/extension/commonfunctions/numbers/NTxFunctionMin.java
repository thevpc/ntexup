package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/** {@code min(a, b, ...)}: smallest value; arrays are flattened; NaN if any value is NaN. */
public class NTxFunctionMin extends NTxNumericFunction {
    @Override
    public String name() {
        return "min";
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
        return "min(a, b, ...)";
    }

    @Override
    protected NElement compute(Call c) {
        return NElement.ofDouble(NTxStats.min(c, c.flatten()));
    }
}
