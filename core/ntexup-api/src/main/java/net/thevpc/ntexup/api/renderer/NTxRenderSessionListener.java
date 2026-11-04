package net.thevpc.ntexup.api.renderer;

/**
 * Optional capability offered to a page render so the host can observe the
 * per-page {@link NTxRenderSession} once rendering (and deferred draining) has
 * completed.
 *
 * <p>This is used by diagnostic renderers (the TSON dump) to expose the final
 * geometry registry: every node that was placed on the page, with its bounds,
 * anchor transform and visibility flags. Visual renderers ignore it.
 *
 * @see NTxRenderSession
 * @see NTxGeometryIndex
 */
public interface NTxRenderSessionListener {

    /**
     * Capability key holding the {@link NTxRenderSessionListener} instance.
     */
    String CAPABILITY = "render-session";

    /**
     * Called after the page has been fully rendered and all deferred actions
     * have run. The session is final and safe to inspect.
     *
     * @param session completed page session
     */
    void onRenderSession(NTxRenderSession session);
}
