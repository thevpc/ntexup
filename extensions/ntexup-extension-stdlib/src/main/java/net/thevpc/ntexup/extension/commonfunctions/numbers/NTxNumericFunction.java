package net.thevpc.ntexup.extension.commonfunctions.numbers;

import net.thevpc.ntexup.api.eval.NTxFunctionCallContext;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * Base of the numeric document functions ({@code fixed}, {@code sig}, {@code pct-diff},
 * {@code min}, {@code claim}, ...).
 * <p>
 * They exist so that every number a deck prints is computed from results: a solver metric is
 * formatted, compared or aggregated where it is written, instead of being copied by hand into
 * the text. They are usable as expressions ({@code text(fixed(x, 2))}) and inside string
 * interpolation ({@code "fr = ${fixed(oems_frGHz, 3)} GHz"}).
 * <p>
 * Every function shares the same handling of values that are not numbers yet (see
 * {@link NTxNum}): pending renders as the empty string, unresolved hands the call back
 * (the placeholder stays literal), invalid input is logged as an error and rendered as a
 * visible {@code [name: reason]} marker in place of the number.
 */
abstract class NTxNumericFunction implements NTxFunction {

    /**
     * Thrown by the argument readers to stop the evaluation with a given outcome.
     */
    static final class Stop extends RuntimeException {
        final NElement result;

        Stop(NElement result) {
            super(null, null, false, false);
            this.result = result;
        }
    }

    protected abstract int minArgs();

    protected abstract int maxArgs();

    protected abstract String usage();

    protected abstract NElement compute(Call call);

    @Override
    public final NElement invoke(NTxFunctionCallContext args) {
        Call call = new Call(this, args);
        if (args.size() < minArgs() || args.size() > maxArgs()) {
            return call.error((args.size() < minArgs() ? "too few" : "too many")
                    + " arguments (" + args.size() + "), usage: " + usage());
        }
        try {
            return compute(call);
        } catch (Stop s) {
            return s.result;
        }
    }

    /**
     * Argument access for one invocation.
     */
    static final class Call {
        private final NTxNumericFunction fn;
        final NTxFunctionCallContext args;
        private final NElement[] evaluated;

        Call(NTxNumericFunction fn, NTxFunctionCallContext args) {
            this.fn = fn;
            this.args = args;
            this.evaluated = new NElement[args.size()];
        }

        int size() {
            return args.size();
        }

        NElement raw(int i) {
            if (evaluated[i] == null) {
                NElement e = args.arg(i).eval();
                evaluated[i] = e == null ? NElement.ofNull() : e;
            }
            return evaluated[i];
        }

        /**
         * The i-th argument as a number, or stops the call with the pending / unresolved /
         * error outcome.
         */
        double num(int i) {
            return accept(NTxNum.of(raw(i)), i);
        }

        private double accept(NTxNum n, int i) {
            switch (n.kind) {
                case NUMBER:
                    return n.value;
                case PENDING:
                    throw new Stop(pending());
                case UNRESOLVED:
                    throw new Stop(unresolved());
                case ERROR_TEXT:
                    throw new Stop(NElement.ofString(n.text));
                default:
                    throw new Stop(error("argument " + (i + 1) + ": " + n.text));
            }
        }

        /**
         * A non-negative integer argument (decimals, digits); {@code def} when absent.
         */
        int count(int i, int def, int max) {
            if (i >= size()) {
                return def;
            }
            double d = num(i);
            if (!(d >= 0 && d <= max && d == Math.rint(d))) {
                throw new Stop(error("argument " + (i + 1) + " must be an integer between 0 and " + max + ", got " + NTxNum.auto(d)));
            }
            return (int) d;
        }

        /**
         * Every argument, arrays flattened, as numbers. Stops on the first pending /
         * unresolved / invalid element.
         */
        double[] flatten() {
            List<Double> all = new ArrayList<>();
            for (int i = 0; i < size(); i++) {
                NElement e = raw(i);
                if (e.isListContainer() && !e.isAnyString() && e.isAnyArray()) {
                    for (NElement c : e.asListContainer().get().children()) {
                        all.add(accept(NTxNum.of(c), i));
                    }
                } else {
                    all.add(accept(NTxNum.of(e), i));
                }
            }
            double[] r = new double[all.size()];
            for (int i = 0; i < r.length; i++) {
                r[i] = all.get(i);
            }
            return r;
        }

        String string(int i) {
            NElement e = raw(i);
            if (e.isAnyString() || e.isName()) {
                return e.asStringValue().orElse("").trim();
            }
            if (e.type() != null && e.type().name().equals("CHAR")) {
                return String.valueOf(e.asStringValue().orElse("")).trim();
            }
            throw new Stop(error("argument " + (i + 1) + " must be a string, got " + e));
        }

        NElement pending() {
            return NElement.ofString("");
        }

        NElement unresolved() {
            return args.callExpression();
        }

        NElement error(String message) {
            args.log(NMsg.ofC("%s: %s", fn.name(), message).asError(), args.scopedContext().source());
            return NElement.ofString("[" + fn.name() + ": " + message + "]");
        }
    }
}
