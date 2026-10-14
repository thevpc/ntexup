package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.document.node.NTxNode;

/**
 * Event delivered to a node renderer's
 * {@link NTxNodeRenderer#onPageShow(NtxPageLifecycleEvent)} /
 * {@link NTxNodeRenderer#onPageHide(NtxPageLifecycleEvent)} hooks when the page
 * that contains {@link #node()} starts or stops being shown.
 * <p>
 * Hooks are the counterpart of {@link NtxPageLifecycle#requestFrame()}: use them
 * for exactly-once side effects around a slide transition (start/stop an async
 * computation, reset a chronometer, release a resource), not for per-frame work.
 *
 * @see NtxPageLifecycle
 */
public interface NtxPageLifecycleEvent {

    /**
     * The node instance the owning renderer was resolving when the page
     * transition happened.
     */
    NTxNode node();

    /**
     * The lifecycle of the page being shown or hidden.
     */
    NtxPageLifecycle lifecycle();

    /**
     * Index of the page being shown or hidden.
     */
    default int pageIndex() {
        return lifecycle().pageIndex();
    }

    /**
     * {@code true} while handling an "about to be shown" transition,
     * {@code false} while handling a "just hidden" transition.
     */
    default boolean isShowing() {
        return lifecycle().isShowing();
    }

    /**
     * Convenience: ask for one more animation frame; see
     * {@link NtxPageLifecycle#requestFrame()}.
     */
    default void repaint() {
        lifecycle().requestFrame();
    }
}
