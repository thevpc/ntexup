package net.thevpc.ntexup.extension.progress.view;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.extension.progress.model.NTxProgress;
import net.thevpc.ntexup.extension.progress.skin.NTxProgressSkin;
import net.thevpc.ntexup.extension.progress.skin.NTxProgressSkinRegistry;
import net.thevpc.nuts.time.NDuration;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * progress-view: standalone presentational component.
 * No dependency on futures or registry — renders whatever progress-like state it is given.
 *
 * Parameters:
 *   value: number (0..1 or NaN)
 *   indeterminate: boolean
 *   eta: NDuration (optional)
 *   elapsed: NDuration (optional)
 *   skin: sandglass | progressbar | knob | text
 *   on-complete: hide | hold | fade-out
 *   position: coordinates or anchor keywords
 */
public class NTxProgressViewBuilder implements NTxNodeBuilder {

    private static final String[] SKIN_IDS = {"progressbar", "knob", "sandglass", "text"};

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id("progress-view")
                .parseParam()
                    .matchesNamedPair("value", "indeterminate", "eta", "elapsed", "skin", "on-complete")
                    .end()
                .renderComponent(this::render);
    }

    public void render(NTxRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        NTxBounds2D bounds = rendererContext.selfBounds2D();

        // Read properties
        double value = readValue(node, "value");
        boolean indeterminate = readBoolean(node, "indeterminate");
        NDuration eta = readDuration(node, "eta");
        NDuration elapsed = readDuration(node, "elapsed");
        String skinId = readString(node, "skin", "progressbar");
        String onComplete = readString(node, "on-complete", "hold");

        // Validate and clamp value
        value = NTxProgress.clampValue(value);

        // Build progress snapshot
        NTxProgress progress = new NTxProgress(value, indeterminate, eta, elapsed);

        // Static export: render a static frame (no animation)
        if (rendererContext.isPrint()) {
            renderSkin(rendererContext, bounds, progress, false);
            return;
        }

        // Animation tick for indeterminate mode
        AtomicBoolean animating = new AtomicBoolean(indeterminate);
        if (indeterminate) {
            startAnimationTick(rendererContext, animating);
        }

        // Completion handling
        if (!indeterminate && value >= 1.0) {
            switch (onComplete) {
                case "hide":
                    // Don't render at all
                    return;
                case "fade-out":
                    // Render with reduced alpha (simplified: just render at full for now)
                    break;
                case "hold":
                default:
                    break;
            }
        }

        renderSkin(rendererContext, bounds, progress, animating.get());
    }

    private void renderSkin(NTxRendererContext rendererContext, NTxBounds2D bounds,
                            NTxProgress progress, boolean animating) {
        NTxNode node = rendererContext.node();
        String skinId = readString(node, "skin", "progressbar");
        NTxProgressSkin skin = NTxProgressSkinRegistry.getInstance().get(skinId);
        if (skin != null) {
            skin.render(rendererContext.graphics(), bounds, progress, animating);
        }
    }

    /**
     * Start a lightweight animation tick that calls repaint() on the renderer context.
     * Does NOT trigger a full page re-layout — just repaints this component.
     */
    private void startAnimationTick(NTxRendererContext rendererContext, AtomicBoolean running) {
        javax.swing.Timer timer = new javax.swing.Timer(40, e -> {
            if (running.get()) {
                rendererContext.repaint();
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        timer.setCoalesce(true);
        timer.start();
    }

    // --- Property readers ---

    private double readValue(NTxNode node, String name) {
        Object raw = node.getPropertyValue(name);
        if (raw == null) return Double.NaN;
        return NTxValue.of(raw).asDouble().orElse(Double.NaN);
    }

    private boolean readBoolean(NTxNode node, String name) {
        Object raw = node.getPropertyValue(name);
        if (raw == null) return false;
        return NTxValue.of(raw).asBoolean().orElse(false);
    }

    private NDuration readDuration(NTxNode node, String name) {
        Object raw = node.getPropertyValue(name);
        if (raw == null) return null;
        return NTxValue.of(raw).as(NDuration.class).orElse(null);
    }

    private String readString(NTxNode node, String name, String defaultValue) {
        Object raw = node.getPropertyValue(name);
        if (raw == null) return defaultValue;
        return NTxValue.of(raw).asStringOrName().orElse(defaultValue);
    }
}
