package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/** {@code sig(x, digits = 3)}: x to a number of significant digits, trailing zeros kept, never in E notation. */
public class NTxFunctionSig extends NTxNumericFunction {
    @Override
    public String name() {
        return "sig";
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
        return "sig(x, digits = 3)";
    }

    @Override
    protected NElement compute(Call c) {
        double x = c.num(0);
        int d = c.count(1, 3, 17);
        if (d == 0) {
            return c.error("digits must be at least 1");
        }
        return NElement.ofString(NTxNum.sig(x, d, false));
    }
}
