package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds3D;


public interface NTxNodeRenderer {

    NTxSizeRequirements sizeRequirements(NTxRendererContext ctx);

    NTxBounds2D selfBounds2D(NTxRendererContext ctx);
    NTxBounds3D selfBounds3D(NTxRendererContext ctx);

    void render(NTxRendererContext rendererContext);

    String[] types();

    /**
     * Called when the page containing the node starts being shown. Default is a
     * no-op; override (typically via
     * {@code NTxNodeBuilderContext.onPageShow(...)}) for exactly-once
     * side effects around a slide transition.
     */
    default void onPageShow(NtxPageLifecycleEvent event) {
    }

    /**
     * Called when the page containing the node stops being shown. Default is a
     * no-op.
     */
    default void onPageHide(NtxPageLifecycleEvent event) {
    }
}
