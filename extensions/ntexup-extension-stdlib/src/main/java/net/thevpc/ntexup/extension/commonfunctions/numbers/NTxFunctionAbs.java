package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/** {@code abs(x)}: absolute value. */
public class NTxFunctionAbs extends NTxNumericFunction {
    @Override
    public String name() {
        return "abs";
    }

    @Override
    protected int minArgs() {
        return 1;
    }

    @Override
    protected int maxArgs() {
        return 1;
    }

    @Override
    protected String usage() {
        return "abs(x)";
    }

    @Override
    protected NElement compute(Call c) {
        return NElement.ofDouble(Math.abs(c.num(0)));
    }
}
