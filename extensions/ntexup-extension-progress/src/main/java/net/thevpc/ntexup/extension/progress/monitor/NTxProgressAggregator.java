package net.thevpc.ntexup.extension.progress.monitor;

import net.thevpc.ntexup.extension.progress.model.NTxProgress;
import net.thevpc.ntexup.extension.progress.registry.NTxPendingBinding;
import net.thevpc.nuts.time.NDuration;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Per-field aggregation logic for combining multiple NTxPendingBinding
 * progress snapshots into a single NTxProgress.
 */
public class NTxProgressAggregator {

    /**
     * Aggregate the given bindings into a single progress.
     * Weights are optional — if null or empty, equal weighting is used.
     */
    public static NTxProgress aggregate(List<NTxPendingBinding> bindings, Map<String, Double> weights) {
        if (bindings == null || bindings.isEmpty()) {
            return null;
        }

        // Filter to non-done bindings for active tracking, but include all for elapsed
        double totalValue = 0;
        double totalWeight = 0;
        boolean anyIndeterminate = false;
        Instant earliestStart = null;
        NDuration lastEta = null;

        for (NTxPendingBinding b : bindings) {
            NTxProgress p = b.currentProgress();
            double w = (weights != null && weights.containsKey(b.name())) ? weights.get(b.name()) : 1.0;

            // value: weighted average over members with non-NaN value
            if (!Double.isNaN(p.value())) {
                totalValue += p.value() * w;
                totalWeight += w;
            }

            // indeterminate: true if any member is indeterminate
            if (p.indeterminate()) {
                anyIndeterminate = true;
            }

            // elapsed: time since earliest member started
            Instant start = b.startedAt();
            if (earliestStart == null || start.isBefore(earliestStart)) {
                earliestStart = start;
            }
        }

        double aggregatedValue;
        if (totalWeight > 0) {
            aggregatedValue = totalValue / totalWeight;
        } else {
            aggregatedValue = Double.NaN;
        }

        NDuration aggregatedElapsed = null;
        if (earliestStart != null) {
            aggregatedElapsed = NDuration.between(earliestStart, Instant.now());
        }

        // eta: computed from aggregated rate if possible
        NDuration aggregatedEta = null;
        if (!Double.isNaN(aggregatedValue) && aggregatedElapsed != null && aggregatedValue > 0) {
            double elapsedSec = aggregatedElapsed.timeAsDoubleSeconds();
            double rate = aggregatedValue / elapsedSec;
            if (rate > 0) {
                double remaining = 1.0 - aggregatedValue;
                aggregatedEta = NDuration.ofMillis((long) ((remaining / rate) * 1000));
            }
        }

        return new NTxProgress(aggregatedValue, anyIndeterminate, aggregatedEta, aggregatedElapsed);
    }
}
