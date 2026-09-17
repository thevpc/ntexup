package net.thevpc.ntexup.extension.progress.model;

import net.thevpc.nuts.time.NDuration;

/**
 * Immutable progress state snapshot.
 * <p>
 * {@code value} is normalized to 0..1 (NaN = no determinate data).
 * {@code indeterminate} drives animation when true.
 * {@code eta} and {@code elapsed} are nullable — skins render only what is present.
 */
public class NTxProgress {

    public static final NTxProgress INDETERMINATE = new NTxProgress(Double.NaN, true, null, NDuration.ZERO);
    public static final NTxProgress COMPLETE = new NTxProgress(1.0, false, NDuration.ZERO, NDuration.ZERO);

    private final double value;
    private final boolean indeterminate;
    private final NDuration eta;
    private final NDuration elapsed;

    public NTxProgress(double value, boolean indeterminate, NDuration eta, NDuration elapsed) {
        this.value = value;
        this.indeterminate = indeterminate;
        this.eta = eta;
        this.elapsed = elapsed;
    }

    /**
     * 0..1 or NaN if no determinate data.
     */
    public double value() {
        return value;
    }

    /**
     * True if any unmeasured component is present; drives oscillating animation.
     */
    public boolean indeterminate() {
        return indeterminate;
    }

    /**
     * Estimated time remaining. Null when value is NaN or not supplied.
     */
    public NDuration eta() {
        return eta;
    }

    /**
     * Time since the future/binding started. Null when not available.
     */
    public NDuration elapsed() {
        return elapsed;
    }

    /**
     * Returns a new NTxProgress with the given fields replaced.
     */
    public NTxProgress withValue(double value) {
        return new NTxProgress(value, indeterminate, eta, elapsed);
    }

    public NTxProgress withIndeterminate(boolean indeterminate) {
        return new NTxProgress(value, indeterminate, eta, elapsed);
    }

    public NTxProgress withEta(NDuration eta) {
        return new NTxProgress(value, indeterminate, eta, elapsed);
    }

    public NTxProgress withElapsed(NDuration elapsed) {
        return new NTxProgress(value, indeterminate, eta, elapsed);
    }

    /**
     * Validate and clamp value to 0..1 || NaN.
     */
    public static double clampValue(double v) {
        if (Double.isNaN(v)) {
            return Double.NaN;
        }
        if (v < 0) return 0;
        if (v > 1) return 1;
        return v;
    }

    @Override
    public String toString() {
        return "NTxProgress{value=" + value
                + ", indeterminate=" + indeterminate
                + ", eta=" + eta
                + ", elapsed=" + elapsed + "}";
    }
}
