package net.thevpc.ntexup.engine.renderer.htmlstatic;

import net.thevpc.ntexup.api.renderer.NtxAnimationTimer;
import net.thevpc.ntexup.api.renderer.NtxPageLifecycle;

/**
 * Virtual clock offered to node renderers through the page-lifecycle
 * capability. The exporter decides what "now" is, which turns animation into a
 * pure function of a frame sample instead of wall-clock time: the same sample
 * index always paints the same frame.
 * <p>
 * {@link #requestFrame()} is observed rather than acted upon: a component that
 * asks for another frame only tells us the page has not settled yet. Timers are
 * not replayed because components driven by them also read the elapsed time.
 */
class VirtualPageLifecycle implements NtxPageLifecycle {

    private final int pageIndex;
    private long elapsedMillis;
    private boolean frameRequested;

    VirtualPageLifecycle(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    void setElapsedMillis(long elapsedMillis) {
        this.elapsedMillis = elapsedMillis;
        this.frameRequested = false;
    }

    boolean isFrameRequested() {
        return frameRequested;
    }

    @Override
    public int pageIndex() {
        return pageIndex;
    }

    @Override
    public boolean isShowing() {
        return true;
    }

    @Override
    public long showStartTime() {
        return System.currentTimeMillis() - elapsedMillis;
    }

    @Override
    public long showElapsedMillis() {
        return elapsedMillis;
    }

    @Override
    public void requestFrame() {
        frameRequested = true;
    }

    @Override
    public NtxAnimationTimer newTimer(Object owner, long periodMs, Runnable tick) {
        return NtxAnimationTimer.NONE;
    }
}
