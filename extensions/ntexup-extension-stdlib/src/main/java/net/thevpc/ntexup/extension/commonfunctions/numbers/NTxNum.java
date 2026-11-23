package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.ntexup.api.eval.NTxFutureUtils;
import net.thevpc.ntexup.api.eval.NTxObj;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.nuts.elem.NElement;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * One argument of a numeric document function, classified.
 * <p>
 * A value the document computes from solver results goes through three states, and the
 * functions must tell them apart rather than print garbage:
 * <ul>
 *   <li>{@link Kind#PENDING}: bound to the empty string a solver seeds its metrics with
 *       before its results land (or to a future). The function renders nothing, exactly like
 *       an interpolated metric that is not there yet.</li>
 *   <li>{@link Kind#UNRESOLVED}: an unbound name, or an expression over one (a typo, or a
 *       metric whose producer never ran). The function hands its own call expression back,
 *       so a {@code ${...}} placeholder stays literal in the text, the engine's convention
 *       for an unbound variable.</li>
 *   <li>{@link Kind#INVALID}: bound to something that is not a number ("abc", a boolean,
 *       an array where a scalar is expected). This is an authoring error and is reported.</li>
 * </ul>
 * A result of an inner function that already reported an error ({@link #isErrorText}) is
 * passed through unchanged so one mistake gives one message.
 */
final class NTxNum {

    enum Kind {NUMBER, PENDING, UNRESOLVED, INVALID, ERROR_TEXT}

    final Kind kind;
    final double value;
    final String text;

    private NTxNum(Kind kind, double value, String text) {
        this.kind = kind;
        this.value = value;
        this.text = text;
    }

    static NTxNum number(double v) {
        return new NTxNum(Kind.NUMBER, v, null);
    }

    static NTxNum of(NElement e) {
        if (e == null || e.isNull()) {
            return new NTxNum(Kind.PENDING, Double.NaN, null);
        }
        if (e.isNumber()) {
            return number(e.asDoubleValue().orElse(Double.NaN));
        }
        if (e.isName()) {
            // an unbound variable evaluates to its own name (NAME counts as a string too)
            return new NTxNum(Kind.UNRESOLVED, Double.NaN, e.asStringValue().orElse("?"));
        }
        if (e.isAnyString()) {
            String s = e.asStringValue().orElse("").trim();
            if (s.isEmpty()) {
                return new NTxNum(Kind.PENDING, Double.NaN, null);
            }
            if (isErrorText(s)) {
                return new NTxNum(Kind.ERROR_TEXT, Double.NaN, s);
            }
            if (s.equalsIgnoreCase("nan") || s.equalsIgnoreCase("n/a")) {
                return number(Double.NaN);
            }
            Double d = NTxValue.of(s).asDouble().orNull();
            if (d != null) {
                return number(d);
            }
            return new NTxNum(Kind.INVALID, Double.NaN, "'" + s + "' is not a number");
        }
        if (e.isCustom()) {
            Object v = e.asCustom().get().value();
            if (NTxFutureUtils.isFuture(e) || NTxFutureUtils.isFuture(v)) {
                return new NTxNum(Kind.PENDING, Double.NaN, null);
            }
            if (v instanceof NTxObj) {
                NElement inner = ((NTxObj) v).toElement();
                if (inner != null && inner != e) {
                    return of(inner);
                }
            }
            if (v instanceof Number) {
                return number(((Number) v).doubleValue());
            }
            return new NTxNum(Kind.INVALID, Double.NaN, "'" + v + "' is not a number");
        }
        if (e.isName()) {
            return new NTxNum(Kind.UNRESOLVED, Double.NaN, e.asStringValue().orElse("?"));
        }
        if (e.isBoolean()) {
            return new NTxNum(Kind.INVALID, Double.NaN, "boolean " + e + " is not a number");
        }
        if (e.isAnyArray()) {
            return new NTxNum(Kind.INVALID, Double.NaN, "an array is not a number");
        }
        // operator, flat expression, call that could not be evaluated yet
        return new NTxNum(containsEmptyString(e) ? Kind.PENDING : Kind.UNRESOLVED, Double.NaN, String.valueOf(e));
    }

    static boolean isErrorText(String s) {
        return s.startsWith("[") && s.endsWith("]") && (s.contains(": ") || s.startsWith("[CLAIM"));
    }

    private static boolean containsEmptyString(NElement e) {
        if (e == null) {
            return false;
        }
        if (e.isAnyString()) {
            return e.asStringValue().orElse("").trim().isEmpty();
        }
        if (e.isNull()) {
            return true;
        }
        if (e.isCustom()) {
            return NTxFutureUtils.isFuture(e);
        }
        if (e.isBinaryOperator()) {
            return containsEmptyString(e.asBinaryOperator().get().firstOperand())
                    || containsEmptyString(e.asBinaryOperator().get().secondOperand());
        }
        if (e.isUnaryOperator()) {
            return containsEmptyString(e.asUnaryOperator().get().operand());
        }
        if (e.asListContainer().isPresent()) {
            List<NElement> children = e.asListContainer().get().children();
            for (NElement c : children) {
                if (containsEmptyString(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ formatting

    /**
     * Fixed number of decimals, half-up on the shortest decimal form of the double
     * (so 7.4675 shows as 7.468, as a reader rounding the printed value would).
     */
    static String fixed(double v, int decimals, boolean forceSign) {
        if (Double.isNaN(v)) {
            return "n/a";
        }
        if (Double.isInfinite(v)) {
            return v > 0 ? "inf" : "-inf";
        }
        BigDecimal b = BigDecimal.valueOf(v).setScale(Math.max(0, decimals), RoundingMode.HALF_UP);
        return signed(b, forceSign);
    }

    /**
     * Significant digits, trailing zeros kept (7.40 to 3 digits stays 7.40), never in
     * scientific notation.
     */
    static String sig(double v, int digits, boolean forceSign) {
        if (Double.isNaN(v)) {
            return "n/a";
        }
        if (Double.isInfinite(v)) {
            return v > 0 ? "inf" : "-inf";
        }
        if (v == 0) {
            return fixed(0, Math.max(0, digits - 1), forceSign);
        }
        BigDecimal b = BigDecimal.valueOf(v).round(new MathContext(Math.max(1, digits), RoundingMode.HALF_UP));
        int exponent = b.precision() - b.scale() - 1;
        int scale = digits - 1 - exponent;
        b = b.setScale(Math.max(0, scale), RoundingMode.HALF_UP);
        return signed(b, forceSign);
    }

    private static String signed(BigDecimal b, boolean forceSign) {
        if (b.signum() == 0) {
            b = b.abs();
            String s = b.toPlainString();
            return forceSign ? "±" + s : s;
        }
        String s = b.toPlainString();
        return (forceSign && b.signum() > 0) ? "+" + s : s;
    }

    /**
     * The default display of a computed value when the author did not ask for a precision:
     * four significant digits, enough for every metric in an antenna table.
     */
    static String auto(double v) {
        return sig(v, 4, false);
    }

    /**
     * A constant as the author wrote it (1, 0.5, 1000): shortest decimal form, no trailing zeros.
     */
    static String compact(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return fixed(v, 0, false);
        }
        return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();
    }
}
