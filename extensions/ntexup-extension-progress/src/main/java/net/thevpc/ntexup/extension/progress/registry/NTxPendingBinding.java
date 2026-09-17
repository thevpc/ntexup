package net.thevpc.ntexup.extension.progress.registry;

import net.thevpc.ntexup.api.eval.NTxFuture;
import net.thevpc.ntexup.api.eval.NTxFutureUtils;
import net.thevpc.ntexup.extension.progress.model.NTxProgress;
import net.thevpc.nuts.time.NDuration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A single tracked binding in the progress registry.
 * Wraps an NTxFuture and tracks creation time for elapsed computation.
 */
public class NTxPendingBinding {

    private final String name;
    private final NTxFuture<?> future;
    private final Instant startedAt;
    private final List<Snapshot> history = new ArrayList<>();

    public NTxPendingBinding(String name, NTxFuture<?> future) {
        this.name = name;
        this.future = future;
        this.startedAt = Instant.now();
    }

    public String name() {
        return name;
    }

    public NTxFuture<?> future() {
        return future;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public boolean isDone() {
        return future.isDone();
    }

    /**
     * Compute the current progress snapshot for this binding.
     */
    public NTxProgress currentProgress() {
        Instant now = Instant.now();
        NDuration elapsed = NDuration.between(startedAt, now);
        double rawProgress = future.progress();
        double value = NTxProgress.clampValue(rawProgress);

        recordSnapshot(now, value);

        if (future.isDone()) {
            return new NTxProgress(1.0, false, NDuration.ZERO, elapsed);
        }

        if (Double.isNaN(value)) {
            return new NTxProgress(Double.NaN, true, null, elapsed);
        }

        NDuration eta = computeEta(now, value, elapsed);
        return new NTxProgress(value, false, eta, elapsed);
    }

    private void recordSnapshot(Instant timestamp, double value) {
        history.add(new Snapshot(timestamp, value));
        if (history.size() > 100) {
            history.remove(0);
        }
    }

    /**
     * Smoothed ETA from rolling (timestamp, value) history.
     * Uses linear regression over recent samples rather than a single start-to-now average.
     */
    private NDuration computeEta(Instant now, double value, NDuration elapsed) {
        if (Double.isNaN(value) || value <= 0) {
            return null;
        }
        if (value >= 1.0) {
            return NDuration.ZERO;
        }

        // Need at least 2 samples for rate computation
        if (history.size() < 2) {
            return deriveFromElapsed(value, elapsed);
        }

        // Linear regression over last 20 samples
        int windowSize = Math.min(history.size(), 20);
        int startIdx = history.size() - windowSize;
        long t0 = history.get(startIdx).timestamp.toEpochMilli();
        double v0 = history.get(startIdx).value;
        long t1 = now.toEpochMilli();
        double v1 = value;

        double dt = (t1 - t0) / 1000.0; // seconds
        double dv = v1 - v0;

        if (dt <= 0 || dv <= 0) {
            return deriveFromElapsed(value, elapsed);
        }

        double rate = dv / dt; // progress per second
        double remaining = 1.0 - value;
        double etaSeconds = remaining / rate;

        return NDuration.ofMillis((long) (etaSeconds * 1000));
    }

    private NDuration deriveFromElapsed(double value, NDuration elapsed) {
        if (elapsed == null || Double.isNaN(value) || value <= 0) {
            return null;
        }
        double elapsedSec = elapsed.timeAsDoubleSeconds();
        double rate = value / elapsedSec;
        if (rate <= 0) return null;
        double remaining = 1.0 - value;
        return NDuration.ofMillis((long) ((remaining / rate) * 1000));
    }

    private static class Snapshot {
        final Instant timestamp;
        final double value;

        Snapshot(Instant timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
}
