package net.thevpc.ntexup.extension.progress.monitor;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.ntexup.extension.progress.model.NTxProgress;
import net.thevpc.ntexup.extension.progress.registry.NTxPendingBinding;
import net.thevpc.ntexup.extension.progress.NTxProgressRegistry;
import net.thevpc.ntexup.extension.progress.registry.NTxProgressRegistryHolder;
import net.thevpc.ntexup.extension.progress.registry.NTxProgressSelect;
import net.thevpc.ntexup.extension.progress.NTxProgressSkin;
import net.thevpc.ntexup.extension.progress.skin.NTxProgressSkinRegistry;

import javax.swing.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * progress-monitor: task-aware layer built on top of progress-view.
 * Computes Progress values from the registry/selection and delegates
 * each visual unit to a progress skin.
 *
 * Additional parameters (beyond progress-view):
 *   select: all | names([...]) | pattern("...")
 *   aggregate: true | false
 *   on-complete: hide | hold | fade-out
 *   on-empty: hide | idle
 *   weights: {name: number, ...}
 */
public class NTxProgressMonitorBuilder implements NTxNodeBuilder {

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id("progress-monitor")
                .parseParam()
                    .matchesNamedPair("select", "aggregate", "on-complete", "on-empty", "weights", "skin")
                    .matchesMissingProperties("value", "indeterminate", "eta", "elapsed", "skin", "position")
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
            handleEmpty(rendererContext, bounds, node);
            return;
        }

        // Resolve select filter
        NTxProgressSelect select = readSelect(node);
        List<NTxPendingBinding> bindings = registry.select(select);

        // on-empty handling
        if (bindings.isEmpty()) {
            handleEmpty(rendererContext, bounds, node);
            return;
        }

        // Check if all bindings are done
        boolean allDone = true;
        for (NTxPendingBinding b : bindings) {
            if (!b.isDone()) {
                allDone = false;
                break;
            }
        }

        // on-complete handling: hide when done
        if (allDone) {
            String onComplete = readString(node, "on-complete", "hold");
            if ("hide".equals(onComplete)) {
                return;
            }
        }

        // Read weights
        Map<String, Double> weights = readWeights(node);
        boolean aggregate = readBoolean(node, "aggregate");

        if (aggregate) {
            renderAggregate(rendererContext, bounds, bindings, weights, node);
        } else {
            renderPerUnit(rendererContext, bounds, bindings, weights, node);
        }

        // Start repaint timer if any binding is still pending
        if (!allDone) {
            startRepaintTick(rendererContext);
        }
    }

    private void handleEmpty(NTxRendererContext rendererContext, NTxBounds2D bounds, NTxNode node) {
        String onEmpty = readString(node, "on-empty", "hide");
        if ("idle".equals(onEmpty)) {
            renderIdle(rendererContext, bounds);
        }
    }

    private void renderAggregate(NTxRendererContext rendererContext, NTxBounds2D bounds,
                                 List<NTxPendingBinding> bindings, Map<String, Double> weights,
                                 NTxNode node) {
        NTxProgress aggregated = NTxProgressAggregator.aggregate(bindings, weights);
        if (aggregated == null) return;
        renderAsSkin(rendererContext, bounds, aggregated, node);
    }

    private void renderPerUnit(NTxRendererContext rendererContext, NTxBounds2D bounds,
                               List<NTxPendingBinding> bindings, Map<String, Double> weights,
                               NTxNode node) {
        int count = bindings.size();
        if (count == 0) return;

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

            renderAsSkin(rendererContext, unitBounds, progress, node);
            y += unitHeight + spacing;
        }
    }

    private void renderAsSkin(NTxRendererContext rendererContext, NTxBounds2D bounds,
                              NTxProgress progress, NTxNode sourceNode) {
        String skinId = readString(sourceNode, "skin", "bar");
        NTxProgressSkin skin = NTxProgressSkinRegistry.getInstance().get(skinId);
        if (skin != null) {
            skin.render(rendererContext.graphics(), bounds, progress, true);
        }
    }

    private void renderIdle(NTxRendererContext rendererContext, NTxBounds2D bounds) {
        NTxProgress idle = NTxProgress.INDETERMINATE;
        renderAsSkin(rendererContext, bounds, idle, rendererContext.node());
    }

    private void startRepaintTick(NTxRendererContext rendererContext) {
        Timer timer = new Timer(40, e -> rendererContext.repaint());
        timer.setCoalesce(true);
        timer.start();
    }

    // --- Property readers ---

    private NTxProgressSelect readSelect(NTxNode node) {
        Object raw = node.getPropertyValue("select").orNull();
        if (raw == null) {
            return NTxProgressSelect.all();
        }
        NTxValue val = NTxValue.of(raw);

        // Try as simple string first (e.g., select: all)
        NOptional<String> single = val.asStringOrName();
        if (single.isPresent()) {
            String s = single.get();
            if ("all".equals(s)) {
                return NTxProgressSelect.all();
            }
        }

        // Try as simple string array (e.g., select: ["hresult"])
        NOptional<String[]> arr = val.asStringArrayOrString();
        if (arr.isPresent()) {
            String[] a = arr.get();
            if (a.length == 1 && "all".equals(a[0])) {
                return NTxProgressSelect.all();
            }
            return NTxProgressSelect.names(Arrays.asList(a));
        }

        // Try as named function form: names(["hresult"]) or pattern("...")
        // The property is parsed as a NAMED_TUPLE/ARRAY with name and body/args
        String name = val.name();
        if (val.asElement().isPresent()) {
            NElement elem = val.asElement().get();

            if (elem.isNamedTuple()) {
                switch (NTxUtils.uid(elem.asNamed().get().name().get())) {
                    case "all":
                        return NTxProgressSelect.all();
                    case "names": {
                        // Extract the inner array from body/args
                        List<NElement> body = elem.asTuple().get().params();
                        if (body.isEmpty()) body = val.args();
                        if (!body.isEmpty()) {
                            NOptional<String[]> names = NTxValue.of(body.get(0)).asStringArrayOrString();
                            if (names.isPresent()) {
                                return NTxProgressSelect.names(Arrays.asList(names.get()));
                            }
                        }
                        break;
                    }
                    case "pattern": {
                        List<NElement> body = elem.asTuple().get().params();
                        if (body.isEmpty()) body = val.args();
                        if (!body.isEmpty()) {
                            NOptional<String> pat = NTxValue.of(body.get(0)).asStringOrName();
                            if (pat.isPresent()) {
                                return NTxProgressSelect.pattern(pat.get());
                            }
                        }
                        break;
                    }
                }
            }
        }
        return NTxProgressSelect.all();
    }

    private Map<String, Double> readWeights(NTxNode node) {
        Object raw = node.getPropertyValue("weights").orNull();
        if (raw == null) return null;
        Map<String, Double> result = new LinkedHashMap<>();
        NTxValue val = NTxValue.of(raw);
        NOptional<String[]> keys = val.asStringArrayOrString();
        if (keys.isPresent()) {
            for (String k : keys.get()) {
                result.put(k, 1.0);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private boolean readBoolean(NTxNode node, String name) {
        Object raw = node.getPropertyValue(name).orNull();
        if (raw == null) return false;
        return NTxValue.of(raw).asBoolean().orElse(false);
    }

    private String readString(NTxNode node, String name, String defaultValue) {
        Object raw = node.getPropertyValue(name).orNull();
        if (raw == null) return defaultValue;
        return NTxValue.of(raw).asStringOrName().orElse(defaultValue);
    }
}
