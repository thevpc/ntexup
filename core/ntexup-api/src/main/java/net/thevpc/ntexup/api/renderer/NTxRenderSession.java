package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.document.node.NTxNode;

import java.awt.geom.AffineTransform;
import java.util.function.Consumer;

/**
 * Per-page rendering session shared by all node renderers.
 *
 * <p>It exposes the current geometry index and a deferred-render queue. A node
 * that depends on the final page geometry (for example a connector between two
 * named nodes) can {@link #defer defer} a draw action; the engine drains the
 * queue at the end of the page, after every node has been laid out and placed.
 *
 * <p>Renderers that run outside a page (measurement, detached rendering) get a
 * no-op session, so deferring is always safe.
 */
public interface NTxRenderSession {
    /**
     * Geometry of the nodes rendered so far on this page.
     */
    NTxGeometryIndex geometry();

    /**
     * Root graphics of the page, used to draw deferred elements without the
     * transform of the node that requested them.
     */
    NTxGraphics pageGraphics();

    /**
     * Schedule {@code draw} to run after the page is laid out.
     *
     * @param owner           node that owns the deferred draw (used for logging)
     * @param ctx             renderer context captured now
     * @param ancestorsToRoot transform from the owner's parent frame to root;
     *                        deferred actions use it for free coordinates
     * @param draw            action to run on the page graphics
     */
    void defer(NTxNode owner, NTxRendererContext ctx, AffineTransform ancestorsToRoot,
               Consumer<NTxRendererContext> draw);

    /**
     * Run every deferred action, including actions deferred by other actions.
     */
    void drainDeferred();
}
