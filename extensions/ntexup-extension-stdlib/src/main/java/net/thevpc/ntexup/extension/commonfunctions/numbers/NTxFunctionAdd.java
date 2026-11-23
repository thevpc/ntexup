package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/** {@code add(a, b, ...)}: sum; arrays are flattened. Unlike the {@code +} operator it never concatenates strings. */
public class NTxFunctionAdd extends NTxNumericFunction {
    @Override
    public String name() {
        return "add";
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
        return "add(a, b, ...)";
    }

    @Override
    protected NElement compute(Call c) {
        double s = 0;
        for (double v : c.flatten()) {
            s += v;
        }
        return NElement.ofDouble(s);
    }
}
