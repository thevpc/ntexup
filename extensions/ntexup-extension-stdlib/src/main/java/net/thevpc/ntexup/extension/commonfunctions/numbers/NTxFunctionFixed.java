package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/** {@code fixed(x, decimals = 2)}: x with a fixed number of decimals, half-up ("7.468"); NaN shows as "n/a". */
public class NTxFunctionFixed extends NTxNumericFunction {
    @Override
    public String name() {
        return "fixed";
    }

    @Override
    protected int minArgs() {
        return 1;
    }

    @Override
    protected int maxArgs() {
        return 2;
    }

    @Override
    protected String usage() {
        return "fixed(x, decimals = 2)";
    }

    @Override
    protected NElement compute(Call c) {
        double x = c.num(0);
        return NElement.ofString(NTxNum.fixed(x, c.count(1, 2, 15), false));
    }
}
