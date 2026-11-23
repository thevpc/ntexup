package net.thevpc.ntexup.extension.commonfunctions.numbers;

/**
 * Aggregates over flattened arguments. A NaN (a metric that could not be measured, e.g. no
 * -10 dB band) propagates instead of being skipped: a spread silently computed over the
 * solvers that happened to succeed would read as agreement.
 */
final class NTxStats {

    static double[] nonEmpty(NTxNumericFunction.Call c, double[] v) {
        if (v.length == 0) {
            throw new NTxNumericFunction.Stop(c.error("no values (empty array)"));
        }
        return v;
    }

    static double min(NTxNumericFunction.Call c, double[] v) {
        double m = Double.POSITIVE_INFINITY;
        for (double d : nonEmpty(c, v)) {
            if (Double.isNaN(d)) {
                return Double.NaN;
            }
            m = Math.min(m, d);
        }
        return m;
    }

    static double max(NTxNumericFunction.Call c, double[] v) {
        double m = Double.NEGATIVE_INFINITY;
        for (double d : nonEmpty(c, v)) {
            if (Double.isNaN(d)) {
                return Double.NaN;
            }
            m = Math.max(m, d);
        }
        return m;
    }

    private NTxStats() {
    }
}
