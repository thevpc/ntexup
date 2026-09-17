package net.thevpc.ntexup.extension.progress.monitor;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.extension.progress.model.NTxProgress;
import net.thevpc.ntexup.extension.progress.registry.NTxPendingBinding;
import net.thevpc.ntexup.extension.progress.registry.NTxProgressRegistry;
import net.thevpc.ntexup.extension.progress.registry.NTxProgressRegistryHolder;
import net.thevpc.ntexup.extension.progress.registry.NTxProgressSelect;
import net.thevpc.ntexup.extension.progress.view.NTxProgressViewBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * progress-monitor: task-aware layer built on top of progress-view.
 * Computes Progress values from the registry/selection and delegates
 * each visual unit to progress-view.
 *
 * Additional parameters (beyond progress-view):
 *   select: all | names([...]) | pattern("...")
 *   aggregate: true | false
 *   on-empty: hide | idle
 *   weights: {name: number, ...}
 */
public class NTxProgressMonitorBuilder implements NTxNodeBuilder {

    private final NTxProgressViewBuilder viewBuilder = new NTxProgressViewBuilder();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id("progress-monitor")
                .parseParam()
                    .matchesNamedPair("select", "aggregate", "on-empty", "weights")
                    .matchesMissingProperties("value", "indeterminate", "eta", "elapsed", "skin", "on-complete", "position")
                    .end()
                .renderComponent(this::render);
    }

    public void render(NTxRendererContext rendererContext) {
        // Static export: no-op (all futures would be resolved by construction)
        if (rendererContext.isPrint()) {
            return;
        }

        NTxNode node = rendererContext.node();
        NTxBounds2D bounds = rendererContext.selfBounds2D();

        // Resolve registry from the current document
        Object doc = rendererContext.compiledDocument();
        if (doc == null) return;
        NTxProgressRegistry registry = NTxProgressRegistryHolder.get(doc);
        if (registry == null) {
            // No registry — treat as empty
            String onEmpty = readString(node, "on-empty", "hide");
            if ("idle".equals(onEmpty)) {
                renderIdle(rendererContext, bounds);
            }
            return;
        }

        // Resolve select filter
        NTxProgressSelect select = readSelect(node);
        List<NTxPendingBinding> bindings = registry.select(select);

        // on-empty handling
        if (bindings.isEmpty()) {
            String onEmpty = readString(node, "on-empty", "hide");
            if ("idle".equals(onEmpty)) {
                renderIdle(rendererContext, bounds);
            }
            return;
        }

        // Read weights
        Map<String, Double> weights = readWeights(node);

        boolean aggregate = readBoolean(node, "aggregate");

        if (aggregate) {
            renderAggregate(rendererContext, bounds, bindings, weights, node);
        } else {
            renderPerUnit(rendererContext, bounds, bindings, weights, node);
        }
    }

    private void renderAggregate(NTxRendererContext rendererContext, NTxBounds2D bounds,
                                 List<NTxPendingBinding> bindings, Map<String, Double> weights,
                                 NTxNode node) {
        NTxProgress aggregated = NTxProgressAggregator.aggregate(bindings, weights);
        if (aggregated == null) return;

        // Create a progress-view rendering with the aggregated progress
        // We render directly using the skin, passing through view params
        renderAsProgressView(rendererContext, bounds, aggregated, node);
    }

    private void renderPerUnit(NTxRendererContext rendererContext, NTxBounds2D bounds,
                               List<NTxPendingBinding> bindings, Map<String, Double> weights,
                               NTxNode node) {
        int count = bindings.size();
        if (count == 0) return;

        // Layout: vertical list, evenly spaced
        double totalWeight = 0;
        for (NTxPendingBinding b : bindings) {
            double w = (weights != null && weights.containsKey(b.name())) ? weights.get(b.name()) : 1.0;
            totalWeight += w;
        }

        double y = bounds.minY();
        double spacing = 4;
        double totalSpacing = spacing * (count - 1);
        double availableHeight = bounds.widthY() - totalSpacing;

        for (NTxPendingBinding b : bindings) {
            double w = (weights != null && weights.containsKey(b.name())) ? weights.get(b.name()) : 1.0;
            double unitHeight = availableHeight * (w / totalWeight);

            NTxProgress progress = b.currentProgress();
            NTxBounds2D unitBounds = NTxBounds2D.ofWidth(
                    bounds.minX(), y,
                    bounds.widthX(), unitHeight);

            renderAsProgressView(rendererContext, unitBounds, progress, node);
            y += unitHeight + spacing;
        }
    }

    private void renderAsProgressView(NTxRendererContext rendererContext, NTxBounds2D bounds,
                                      NTxProgress progress, NTxNode sourceNode) {
        // Delegate to the skin directly with inherited view params
        String skinId = readString(sourceNode, "skin", "progressbar");
        net.thevpc.ntexup.extension.progress.skin.NTxProgressSkin skin =
                net.thevpc.ntexup.extension.progress.skin.NTxProgressSkinRegistry.getInstance().get(skinId);
        if (skin != null) {
            skin.render(rendererContext.graphics(), bounds, progress, !rendererContext.isPrint());
        }
    }

    private void renderIdle(NTxRendererContext rendererContext, NTxBounds2D bounds) {
        // Render an indeterminate indicator when empty + idle
        NTxProgress idle = NTxProgress.INDETERMINATE;
        renderAsProgressView(rendererContext, bounds, idle, rendererContext.node());
    }

    // --- Property readers ---

    private NTxProgressSelect readSelect(NTxNode node) {
        Object raw = node.getPropertyValue("select");
        if (raw == null) {
            return NTxProgressSelect.all();
        }
        String selectStr = NTxValue.of(raw).asStringOrName().orElse("all");
        switch (selectStr) {
            case "all":
                return NTxProgressSelect.all();
            default:
                // Check if it's a names([...]) or pattern("...") form
                // For now, treat as all — the parser should have resolved this
                return NTxProgressSelect.all();
        }
    }

    private Map<String, Double> readWeights(NTxNode node) {
        Object raw = node.getPropertyValue("weights");
        if (raw == null) return null;
        Map<String, Double> result = new LinkedHashMap<>();
        NTxValue val = NTxValue.of(raw);
        // Weights are parsed as a map by the engine's property system
        // For now, return null (equal weighting)
        return result.isEmpty() ? null : result;
    }

    private boolean readBoolean(NTxNode node, String name) {
        Object raw = node.getPropertyValue(name);
        if (raw == null) return false;
        return NTxValue.of(raw).asBoolean().orElse(false);
    }

    private String readString(NTxNode node, String name, String defaultValue) {
        Object raw = node.getPropertyValue(name);
        if (raw == null) return defaultValue;
        return NTxValue.of(raw).asStringOrName().orElse(defaultValue);
    }
}
