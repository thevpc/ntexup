package net.thevpc.ntexup.api.renderer;

/**
 * Handle for a page-owned periodic animation tick created through
 * {@link NtxPageLifecycle#newTimer}.
 *
 * <p>
 * A timer does not paint by itself. A tick typically advances a component's own
 * state (e.g. a simulation step) and/or asks for a frame through
 * {@link NTxRendererContext#requestFrame()}. Repaint is coalesced by the hosting
 * view at its frame rate (FPS), so many timers may run at their own cadence while
 * the page still repaints at one steady rate.
 *
 * <p>
 * Timers are owned by the page: they are stopped while the page is not showing
 * and disposed when the page is discarded, so callers never manage cleanup.
 * {@link #cancel()} is only needed to end a short-lived animation early.
 *
 * @see NtxPageLifecycle#newTimer(Object, long, Runnable)
 * @see NTxRendererContext#newTimer(long, Runnable)
 */
public interface NtxAnimationTimer {

    /**
     * No-op handle used when no live view drives the page (e.g. PDF/image/HTML
     * renders). Never throws and never fires.
     */
    NtxAnimationTimer NONE = new NtxAnimationTimer() {
        @Override
        public long periodMillis() {
            return 0L;
        }

        @Override
        public boolean isRunning() {
            return false;
        }

        @Override
        public void cancel() {
        }
    };

    /**
     * Requested period between ticks, in milliseconds, or {@code 0} for a no-op
     * handle.
     */
    long periodMillis();

    /**
     * Whether the page is currently driving this timer, i.e. the page is showing
     * and the timer has not been cancelled.
     */
    boolean isRunning();

    /**
     * Permanently releases this timer. After this call {@link #isRunning()}
     * returns {@code false} and the tick is never invoked again.
     */
    void cancel();
}
