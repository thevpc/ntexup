package net.thevpc.ntexup.api.renderer;

import net.thevpc.nuts.elem.NElement;

/**
 * Optional rendering sink offered by the TSON dump renderer.
 * <p>
 * Custom components that rasterize semantic content into pixels (a chart built
 * from a function model, a diagram compiled into an image, ...) normally show
 * up in a dump only as raw paint operations, which hides the underlying data.
 * During a dump render, {@link NTxRendererContext#renderDump()} provides a
 * live sink; a component may call {@link #dump} once per render (typically
 * after building its model, before painting it) to attach the semantic payload
 * as a {@code dump{...}} operation recorded in the component's own paint group.
 * <p>
 * The sink must never be used outside a dump render: the capability is absent,
 * {@code renderDump()} is empty, and a no-op implementation records nothing, so
 * callers can safely use it unconditionally when present.
 *
 * @see NTxRendererContext#renderDump()
 */
public interface NTxRenderDump {

    /**
     * Capability key holding the {@code NTxRenderDump} instance during a dump
     * render; absent for PDF/image/screen/HTML rendering.
     */
    String CAPABILITY = "render-dump";

    /**
     * Attaches component specific semantic data to the current dump.
     *
     * @param ctx  the renderer context of the node producing the data; used to
     *             locate the paint group and to record node identity/bounds
     * @param kind stable discriminator for the payload vocabulary
     *             ({@code "model"}, {@code "diagram"}, {@code "table"}, ...)
     * @param data payload element, usually an object built by the component
     */
    void dump(NTxRendererContext ctx, String kind, NElement data);
}
