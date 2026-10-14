package net.thevpc.ntexup.api.renderer;

/**
 * Live, per-page lifecycle state exposed to components by renderers that show
 * pages interactively (typically the screen renderer).
 * <p>
 * A {@code NtxPageLifecycle} is owned by the view that hosts a page and is made
 * available to the render pipeline through
 * {@link NTxRendererContext#pageLifecycle()} (stored as the
 * {@link #CAPABILITY_PAGE_LIFECYCLE} capability). Static renders (PDF, image,
 * HTML) do not provide one, so {@link NTxRendererContext#isPageShowing()} stays
 * {@code true} there and {@link #requestFrame()} is never invoked.
 *
 * <h3>Why it exists</h3>
 * Before this contract, animated components had to spin their own
 * {@code javax.swing.Timer} to call {@link NTxRendererContext#repaint()}. With
 * more than one animated component on a slide that meant one timer (and one
 * full-page repaint) per component, and nothing stopped those timers when the
 * slide was left. Instead, components should:
 * <ul>
 *   <li>read {@link #showElapsedSeconds()} to compute their animation phase
 *       (stable across frames, starts at 0 each time the page is shown);</li>
 *   <li>call {@link #requestFrame()} once per render while they still want to
 *       animate. The hosting view runs a single, throttled frame driver and
 *       idles as soon as no component requests a frame.</li>
 * </ul>
 *
 * @see NTxRendererContext#pageLifecycle()
 * @see NTxRendererContext#requestFrame()
 */
public interface NtxPageLifecycle {

    /**
     * Capability key under which a {@code NtxPageLifecycle} is published on
     * {@link NTxRendererContext#getCapability(String)}.
     */
    String CAPABILITY_PAGE_LIFECYCLE = "page-lifecycle";

    /**
     * Index of the page this lifecycle belongs to.
     */
    int pageIndex();

    /**
     * Whether this page is the one currently shown by the hosting view.
     * {@code false} for a page that is only prefetched or already hidden.
     */
    boolean isShowing();

    /**
     * Wall-clock time (in milliseconds, {@link System#currentTimeMillis()} frame)
     * at which the page became visible, or {@code 0} if it has never been shown.
     * This is the anchor used by {@link #showElapsedMillis()}.
     */
    long showStartTime();

    /**
     * Milliseconds elapsed since this page became visible, or {@code 0} while it
     * is not showing. The value is stable across frames so animations keep a
     * continuous phase.
     */
    default long showElapsedMillis() {
        if (!isShowing()) {
            return 0L;
        }
        long t0 = showStartTime();
        if (t0 <= 0L) {
            return 0L;
        }
        return Math.max(0L, System.currentTimeMillis() - t0);
    }

    /**
     * Seconds elapsed since this page became visible; see {@link #showElapsedMillis()}.
     */
    default double showElapsedSeconds() {
        return showElapsedMillis() / 1000.0;
    }

    /**
     * Asks the hosting view to render another animation frame soon. Components
     * call this once per render for as long as they want to keep animating. It
     * is a no-op when there is no live view driving the page.
     */
    void requestFrame();

    /**
     * Registers a periodic animation tick, owned by the page.
     *
     * <p>
     * The hosting view runs {@code tick} every {@code periodMs} while the page is
     * showing, stops it when the page stops showing and disposes it when the page
     * is discarded, so callers never have to clean up. Ticks that share the same
     * {@code periodMs} are grouped and driven by a single underlying timer.
     *
     * <p>
     * Registration is keyed by {@code owner}: calling again with the same owner
     * replaces the previous tick (typically used to refresh the callback on every
     * render), so it is safe to call from every render. The callback runs on the
     * UI thread; it should not capture a transient render context.
     *
     * @param owner    stable identity of the registrant; typically the node
     * @param periodMs desired period between ticks in milliseconds (&gt; 0)
     * @param tick     callback invoked on the UI thread while the page is showing
     * @return a handle to cancel the tick early; never {@code null}
     */
    default NtxAnimationTimer newTimer(Object owner, long periodMs, Runnable tick) {
        return NtxAnimationTimer.NONE;
    }
}
