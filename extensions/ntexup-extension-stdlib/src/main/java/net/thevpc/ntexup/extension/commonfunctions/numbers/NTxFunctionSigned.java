package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/** {@code signed(x, decimals = 2)}: like {@code fixed} with an explicit sign ("+0.42", "-1.30"). */
public class NTxFunctionSigned extends NTxNumericFunction {
    @Override
    public String name() {
        return "signed";
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
        return "signed(x, decimals = 2)";
    }

    @Override
    protected NElement compute(Call c) {
        double x = c.num(0);
        return NElement.ofString(NTxNum.fixed(x, c.count(1, 2, 15), true));
    }
}
