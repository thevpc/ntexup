package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/** {@code max(a, b, ...)}: largest value; arrays are flattened; NaN if any value is NaN. */
public class NTxFunctionMax extends NTxNumericFunction {
    @Override
    public String name() {
        return "max";
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
        return "max(a, b, ...)";
    }

    @Override
    protected NElement compute(Call c) {
        return NElement.ofDouble(NTxStats.max(c, c.flatten()));
    }
}
