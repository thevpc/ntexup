package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/** {@code div(a, b)}: a / b, reported when b is zero. Typical use: div(oems_fr, 1e9) for GHz. */
public class NTxFunctionDiv extends NTxNumericFunction {
    @Override
    public String name() {
        return "div";
    }

    @Override
    protected int minArgs() {
        return 2;
    }

    @Override
    protected int maxArgs() {
        return 2;
    }

    @Override
    protected String usage() {
        return "div(a, b)";
    }

    @Override
    protected NElement compute(Call c) {
        double a = c.num(0);
        double b = c.num(1);
        if (b == 0) {
            return c.error("division by zero");
        }
        return NElement.ofDouble(a / b);
    }
}
