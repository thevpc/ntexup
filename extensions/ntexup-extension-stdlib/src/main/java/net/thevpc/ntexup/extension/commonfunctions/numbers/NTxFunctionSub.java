package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/** {@code sub(a, b)}: a - b. */
public class NTxFunctionSub extends NTxNumericFunction {
    @Override
    public String name() {
        return "sub";
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
        return "sub(a, b)";
    }

    @Override
    protected NElement compute(Call c) {
        return NElement.ofDouble(c.num(0) - c.num(1));
    }
}
