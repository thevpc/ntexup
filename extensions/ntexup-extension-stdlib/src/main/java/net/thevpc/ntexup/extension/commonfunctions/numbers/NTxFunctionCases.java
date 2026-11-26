package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.nuts.elem.NElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code cases(value, condition1, text1, condition2, text2, ..., fallback?)}: the sentence that
 * matches a computed value. Conditions are tried in order and the text of the first that holds
 * is rendered; an odd last argument is the fallback. So the wording of a discussion follows
 * the numbers instead of being written for one particular run:
 * <pre>
 * "OpenEMS ${cases(pct-diff(oems_fr, sheen_fr),
 *     'abs&lt;=1', 'falls within 1% of it (@+v%)',
 *     'abs&lt;=3', 'lies @+v% from it',
 *     'misses it by @+v%')}."
 * </pre>
 * A condition is an optional {@code abs} prefix (test |value|) followed by either a comparison
 * {@code < <= > >= == !=} and a number ({@code '<=1'}, {@code 'abs > 2.5'}), or an inclusive
 * range {@code lo..hi} ({@code '7.4..7.6'}, {@code 'abs 1..3'}). {@code 'nan'} matches a value
 * that is not available (NaN), which no comparison matches.
 * <p>
 * In the chosen text {@code @v} is the value with 2 decimals, {@code @+v} the same with an
 * explicit sign and {@code @|v|} its magnitude; a digit right after sets the decimals
 * ({@code @v0}, {@code @+v1}, {@code @|v|3}). The marker is {@code @}, not {@code $}, so the
 * engine's own interpolation leaves it alone, and it contains no {@code }}, which would end
 * the placeholder.
 * <p>
 * A pending value renders nothing, like the other numeric functions. A value that matches no
 * condition, with no fallback, is a render error shown as {@code [cases: ...]} in the text.
 */
public class NTxFunctionCases extends NTxNumericFunction {

    private static final Pattern RANGE = Pattern.compile(
            "([-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?)\\s*\\.\\.\\s*([-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?)");
    private static final Pattern COMPARE = Pattern.compile(
            "(<=|>=|==|!=|<|>|=)\\s*([-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?)");
    private static final Pattern MARKER = Pattern.compile("@(\\+v|\\|v\\||v)(\\d)?");

    @Override
    public String name() {
        return "cases";
    }

    @Override
    protected int minArgs() {
        return 2;
    }

    @Override
    protected int maxArgs() {
        return 101;
    }

    @Override
    protected String usage() {
        return "cases(value, 'condition', 'text', ..., 'fallback'?) with condition [abs] op number,"
                + " [abs] lo..hi, or nan; @v @+v @|v| in the text are the value";
    }

    @Override
    protected NElement compute(Call c) {
        double value = c.num(0);
        int n = c.size();
        // arguments 1..n-1: condition/text pairs, then the fallback when their count is odd
        for (int i = 1; i + 1 < n; i += 2) {
            String cond = c.string(i);
            Boolean ok = matches(cond, value);
            if (ok == null) {
                return c.error("argument " + (i + 1) + ": cannot read condition '" + cond + "', usage: " + usage());
            }
            if (ok) {
                return NElement.ofString(fill(c.string(i + 1), value));
            }
        }
        if ((n - 1) % 2 == 1) {
            return NElement.ofString(fill(c.string(n - 1), value));
        }
        return c.error("no case matches " + NTxNum.auto(value) + " and there is no fallback text");
    }

    /** Whether {@code value} satisfies {@code cond}; null when the condition cannot be read. */
    static Boolean matches(String cond, double value) {
        String s = cond.trim().toLowerCase();
        if (s.equals("nan")) {
            return Double.isNaN(value);
        }
        double v = value;
        if (s.startsWith("abs")) {
            s = s.substring(3).trim();
            v = Math.abs(value);
        }
        Matcher m = RANGE.matcher(s);
        if (m.matches()) {
            double lo = Double.parseDouble(m.group(1));
            double hi = Double.parseDouble(m.group(2));
            return v >= lo && v <= hi;
        }
        m = COMPARE.matcher(s);
        if (m.matches()) {
            double b = Double.parseDouble(m.group(2));
            switch (m.group(1)) {
                case "<":
                    return v < b;
                case "<=":
                    return v <= b;
                case ">":
                    return v > b;
                case ">=":
                    return v >= b;
                case "==":
                case "=":
                    return v == b;
                default:
                    return v != b;
            }
        }
        return null;
    }

    /** Replaces the {@code @v} markers of {@code text} by the formatted value. */
    static String fill(String text, double value) {
        Matcher m = MARKER.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int decimals = m.group(2) == null ? 2 : Integer.parseInt(m.group(2));
            String shown;
            switch (m.group(1)) {
                case "+v":
                    shown = NTxNum.fixed(value, decimals, true);
                    break;
                case "|v|":
                    shown = NTxNum.fixed(Math.abs(value), decimals, false);
                    break;
                default:
                    shown = NTxNum.fixed(value, decimals, false);
                    break;
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(shown));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
