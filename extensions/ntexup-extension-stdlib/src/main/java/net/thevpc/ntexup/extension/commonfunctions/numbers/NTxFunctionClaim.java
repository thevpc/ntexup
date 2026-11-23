package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

/**
 * {@code claim(value, op, bound, decimals?)}: a sentence's number stated as a checked
 * condition. Renders the computed value (to {@code decimals} decimals, else 4 significant
 * digits) when {@code value op bound} holds, and a visible {@code [CLAIM FAILED: ...]} marker
 * plus an error in the render log when it does not, or when the value is NaN.
 * <p>
 * {@code op} is one of {@code '<' '<=' '>' '>=' '==' '!='} or {@code lt le gt ge eq ne}.
 * Example: {@code "OpenEMS is ${claim(abs(pct-diff(oems_fr, sheen_fr)), '<', 1, 2)}% off"}.
 */
public class NTxFunctionClaim extends NTxNumericFunction {
    @Override
    public String name() {
        return "claim";
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
        return "claim(value, op, bound, decimals?) with op one of < <= > >= == != (or lt le gt ge eq ne)";
    }

    @Override
    protected NElement compute(Call c) {
        String op = canonical(c.string(1));
        if (op == null) {
            return c.error("unknown comparison '" + c.string(1) + "', usage: " + usage());
        }
        double value = c.num(0);
        double bound = c.num(2);
        int decimals = c.count(3, -1, 15);
        String shown = decimals < 0 ? NTxNum.auto(value) : NTxNum.fixed(value, decimals, false);
        String boundShown = NTxNum.compact(bound);
        if (Double.isNaN(value) || Double.isNaN(bound)) {
            return NTxClaims.fails(c, "cannot check " + shown + " " + op + " " + boundShown + " (value not available)");
        }
        boolean ok;
        switch (op) {
            case "<":
                ok = value < bound;
                break;
            case "<=":
                ok = value <= bound;
                break;
            case ">":
                ok = value > bound;
                break;
            case ">=":
                ok = value >= bound;
                break;
            case "==":
                ok = value == bound;
                break;
            default:
                ok = value != bound;
                break;
        }
        return ok ? NTxClaims.holds(shown) : NTxClaims.fails(c, shown + " " + op + " " + boundShown + " is false");
    }

    private static String canonical(String op) {
        switch (op.toLowerCase()) {
            case "<":
            case "lt":
                return "<";
            case "<=":
            case "le":
                return "<=";
            case ">":
            case "gt":
                return ">";
            case ">=":
            case "ge":
                return ">=";
            case "==":
            case "=":
            case "eq":
                return "==";
            case "!=":
            case "<>":
            case "ne":
                return "!=";
        }
        return null;
    }
}
