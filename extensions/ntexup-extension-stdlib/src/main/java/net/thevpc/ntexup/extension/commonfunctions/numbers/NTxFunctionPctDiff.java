package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/**
 * {@code pct-diff(value, reference)}: signed relative difference in percent,
 * 100 (value - reference) / |reference|. The scalar counterpart of {@code relative-error},
 * which compares whole curves.
 */
public class NTxFunctionPctDiff extends NTxNumericFunction {
    @Override
    public String name() {
        return "pct-diff";
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
        return "pct-diff(value, reference)";
    }

    @Override
    protected NElement compute(Call c) {
        double v = c.num(0);
        double r = c.num(1);
        if (r == 0) {
            return c.error("reference is zero, a relative difference is undefined");
        }
        return NElement.ofDouble(100.0 * (v - r) / Math.abs(r));
    }
}
