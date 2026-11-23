package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/**
 * {@code within-pct(value, reference, tolerance, decimals = 2)}: the claim "value is within
 * tolerance % of reference". Renders the signed relative difference ("+0.42%") when
 * |pct-diff| &lt;= tolerance, and a visible {@code [CLAIM FAILED: ...]} marker plus an error in
 * the render log when it is not.
 */
public class NTxFunctionWithinPct extends NTxNumericFunction {
    @Override
    public String name() {
        return "within-pct";
    }

    @Override
    protected int minArgs() {
        return 3;
    }

    @Override
    protected int maxArgs() {
        return 4;
    }

    @Override
    protected String usage() {
        return "within-pct(value, reference, tolerance-percent, decimals = 2)";
    }

    @Override
    protected NElement compute(Call c) {
        double value = c.num(0);
        double ref = c.num(1);
        double tol = c.num(2);
        int decimals = c.count(3, 2, 15);
        if (!(tol >= 0)) {
            return c.error("tolerance must be a non-negative percentage, got " + NTxNum.auto(tol));
        }
        if (ref == 0) {
            return c.error("reference is zero, a relative difference is undefined");
        }
        double d = 100.0 * (value - ref) / Math.abs(ref);
        String shown = NTxNum.fixed(d, decimals, true) + "%";
        String tolShown = NTxNum.compact(tol);
        if (Double.isNaN(d)) {
            return NTxClaims.fails(c, "cannot check within " + tolShown + "% (value not available)");
        }
        return Math.abs(d) <= tol
                ? NTxClaims.holds(shown)
                : NTxClaims.fails(c, shown + " is outside ±" + tolShown + "%");
    }
}
