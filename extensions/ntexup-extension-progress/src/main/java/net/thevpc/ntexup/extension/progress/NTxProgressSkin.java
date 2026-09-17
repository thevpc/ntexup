package net.thevpc.ntexup.extension.progress;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.extension.progress.model.NTxProgress;
import net.thevpc.ntexup.extension.progress.skin.NTxProgressSkinRegistry;
import net.thevpc.nuts.spi.NComponent;

/**
 * Skin contract for rendering a progress indicator.
 * <p>
 * Built-in skins: "progressbar", "knob", "sandglass", "text".
 * Custom skins can be registered via the SPI:
 * {@code META-INF/services/net.thevpc.ntexup.api.extension.NTxProgressSkin}
 *
 * @see NTxProgressSkinRegistry
 */
public interface NTxProgressSkin extends NComponent {

    /**
     * Skin identifier used in the {@code skin:} parameter.
     */
    String id();

    /**
     * Render the progress indicator within the given bounds.
     *
     * @param g          the graphics context
     * @param bounds     the layout bounds to render within
     * @param progress   the current progress state
     * @param animating  true when the indeterminate animation tick is active
     */
    void render(NTxGraphics g, NTxBounds2D bounds, NTxProgress progress, boolean animating);
}
