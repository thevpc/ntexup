package net.thevpc.ntexup.engine.renderer.screen;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxNodeRenderer;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.renderer.NtxAnimationTimer;
import net.thevpc.ntexup.api.renderer.NtxPageLifecycle;
import net.thevpc.ntexup.api.renderer.NtxPageLifecycleEvent;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.text.NMsg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PageView extends JComponent {
    public static Dimension REF_SIZE = new Dimension(1024, 768);
    private static final int FRAME_PERIOD_MS = 40;
    private static final int MIN_TIMER_PERIOD_MS = 5;
    /**
     * Number of empty frame ticks tolerated before the driver idles. A short
     * grace window prevents losing a requestFrame() issued by an in-flight
     * render (the "animation freezes on the first frame" race).
     */
    private static final int MAX_IDLE_FRAME_TICKS = 25;
    private final NTxCompiledPage page;
    private volatile long pageStartTime;
    private volatile boolean showing;
    private volatile boolean frameRequested;
    private volatile int idleFrameTicks;
    private javax.swing.Timer frameTimer;
    private final NtxPageLifecycle lifecycle = new Lifecycle();
    // Page-owned animation timers, grouped by period so ticks that share a speed
    // are driven by a single underlying Swing timer.
    private final Object timerLock = new Object();
    private final IdentityHashMap<Object, TimerHandle> timersByOwner = new IdentityHashMap<>();
    private final Map<Long, TimerGroup> timerGroups = new HashMap<>();
    private String uuid;
    private final NTxEngine engine;
    private final NTxCompiledDocument document;

    private final Object renderLock = new Object();
    private volatile BufferedImage pageImage;
    private volatile int renderW = -1;
    private volatile int renderH = -1;
    private volatile boolean renderPending;
    private volatile boolean renderInvalidated;
    private int pendingW = -1;
    private int pendingH = -1;
    private volatile boolean renderCacheDirty;
    private volatile boolean discarded;
    private volatile int renderEpoch;
    /**
     * True while this page's paint pass runs on the render pool. Invalidation
     * events fired by the pass itself (assign nodes update a binding, the
     * dependency graph notifies the pages that read it) must not schedule a new
     * pass: that is a render -> invalidate -> render loop that pins the CPU and
     * floods the event queue.
     */
    private volatile boolean rendering;

    /**
     * Serialises paint passes of this page: the render pool is multi-threaded, so
     * two passes can otherwise mutate the same node render cache at once.
     */
    private final Object pageRenderLock = new Object();

    public PageView(
            NTxCompiledDocument document,
            NTxCompiledPage page,
            NTxEngine engine
    ) {
        this.document = document;
        this.page = page;
        this.uuid = UUID.randomUUID().toString();
        this.engine = engine;
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                requestRender();
            }
        });
        if (document != null && document.dependencyGraph() != null) {
            document.dependencyGraph().addPageInvalidationListener(pageIndex -> {
                if (pageIndex == this.page.index() || pageIndex < 0) {
                    onExternalInvalidation();
                }
            });
        }
    }

    /**
     * A binding this page reads changed. If the change comes from the pass
     * currently rendering this very page, the image being produced already
     * reflects it: only drop the node caches for the next pass instead of
     * scheduling a new render (which would loop forever, since each pass
     * re-executes the assignment that fired the notification).
     */
    private void onExternalInvalidation() {
        if (rendering) {
            synchronized (renderLock) {
                if (discarded) {
                    return;
                }
                renderCacheDirty = true;
            }
            return;
        }
        repaintDirty();
    }

    public void repaintDirty() {
        // The cache is never dropped here: a solver thread that invalidates the
        // page while the render pool is painting it leaves a node half-populated,
        // and the next pass repaints a stale (pending) draw context. Mark it and
        // let doRender clear it on the thread that owns the node tree.
        synchronized (renderLock) {
            if (discarded) {
                return;
            }
            renderCacheDirty = true;
            renderInvalidated = true;
        }
        requestRender();
        SwingUtilities.invokeLater(this::repaintHierarchy);
    }

    /**
     * Drops the rendered image cache so the next {@link #requestRender()} re-renders.
     * Used when the page is far away from the current one to bound memory usage.
     */
    public void evictCache() {
        synchronized (renderLock) {
            if (discarded) {
                return;
            }
            boolean hadImage = pageImage != null;
            cancelInFlightRender();
            pageImage = null;
            renderInvalidated = true;
            // Drop the per-node render caches too. Keeping them for pages far
            // from the current slide pins images and text layouts and makes a
            // long deck grow without bound.
            if (hadImage && page.isCompiled()) {
                try {
                    NTxNode p = page.compiledPage();
                    if (p != null) {
                        p.invalidateRenderCache();
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /**
     * Invalidates the render pass in flight so {@link #requestRender(boolean)} cannot
     * coalesce a later request into it. Must be called with {@code renderLock} held.
     * Without the {@code renderPending} reset, the killed pass leaves the flag set and
     * every subsequent request at the same size is swallowed: the page keeps
     * {@code pageImage == null} and reports "loading" forever.
     */
    private void cancelInFlightRender() {
        renderEpoch++;
        renderPending = false;
        pendingW = -1;
        pendingH = -1;
        renderW = -1;
        renderH = -1;
    }

    /**
     * Called when the view is discarded (document reload / viewer close).
     */
    public void discard() {
        boolean wasShowing = showing;
        showing = false;
        frameRequested = false;
        idleFrameTicks = 0;
        stopFrameTimer();
        stopAndClearTimers();
        synchronized (renderLock) {
            discarded = true;
            cancelInFlightRender();
            pageImage = null;
            renderInvalidated = false;
        }
        if (wasShowing) {
            firePageLifecycle(false);
        }
    }

    public NTxEngine engine() {
        return engine;
    }

    public JComponent component() {
        return this;
    }

    public String id() {
        return uuid;
    }

    public int index() {
        return page.index();
    }

    void onHide() {
        boolean wasShowing = showing;
        showing = false;
        frameRequested = false;
        idleFrameTicks = 0;
        stopFrameTimer();
        applyTimerState();
        if (wasShowing) {
            firePageLifecycle(false);
        }
    }

    synchronized void onShow() {
        if (discarded) {
            return;
        }
        page.compiledPage();
        this.pageStartTime = System.currentTimeMillis();
        this.showing = true;
        applyTimerState();
        firePageLifecycle(true);
        forceRender();
    }

    public void prefetch() {
        // Off-screen pages are hidden by the CardLayout (bounds 0x0): they
        // cannot be rendered yet. Warm their compilation only — the image is
        // produced when the page gets a real size.
        if (discarded) {
            return;
        }
        DocumentView.submitRender(() -> {
            if (discarded) {
                return;
            }
            try {
                page.compiledPage();
            } catch (Throwable ex) {
                if (engine != null) {
                    engine.log().log(NMsg.ofC("failed to precompile page %s: %s", page.index() + 1, ex));
                }
            }
        }, false);
    }

    /**
     * Triggers a render even when the cached image is still considered valid, so
     * a transition (page shown, animation frame) always recomputes content.
     */
    private void forceRender() {
        synchronized (renderLock) {
            if (discarded) {
                return;
            }
            renderInvalidated = true;
        }
        requestRender(true);
    }

    // ------------------------------------------------------------------
    // Page lifecycle (visibility + single throttled frame driver)
    // ------------------------------------------------------------------

    private class Lifecycle implements NtxPageLifecycle {
        @Override
        public int pageIndex() {
            return page.index();
        }

        @Override
        public boolean isShowing() {
            return showing;
        }

        @Override
        public long showStartTime() {
            return pageStartTime;
        }

        @Override
        public void requestFrame() {
            frameRequested = true;
            idleFrameTicks = 0;
            if (SwingUtilities.isEventDispatchThread()) {
                ensureFrameTimer();
            } else {
                SwingUtilities.invokeLater(PageView.this::ensureFrameTimer);
            }
        }

        @Override
        public NtxAnimationTimer newTimer(Object owner, long periodMs, Runnable tick) {
            return registerTimer(owner, periodMs, tick);
        }
    }

    private void ensureFrameTimer() {
        if (discarded || !showing) {
            return;
        }
        if (frameTimer == null) {
            frameTimer = new javax.swing.Timer(FRAME_PERIOD_MS, e -> tickFrame());
            frameTimer.setCoalesce(true);
        }
        if (!frameTimer.isRunning()) {
            frameTimer.start();
        }
    }

    private void tickFrame() {
        if (discarded || !showing) {
            idleFrameTicks = 0;
            stopFrameTimer();
            return;
        }
        if (frameRequested) {
            frameRequested = false;
            idleFrameTicks = 0;
            forceRender();
        } else if (renderPending) {
            // A render is still in flight. It may be about to call
            // requestFrame() for the next animation frame; stopping the timer
            // in this narrow window loses that request and freezes animations.
            idleFrameTicks = 0;
        } else if (++idleFrameTicks >= MAX_IDLE_FRAME_TICKS) {
            // A component may have just finished a render and be about to ask
            // for the next frame. Keep the driver alive for a short grace
            // period before idling, so requestFrame() alone is reliable.
            idleFrameTicks = 0;
            stopFrameTimer();
        }
    }

    private void stopFrameTimer() {
        javax.swing.Timer t = frameTimer;
        if (t != null && t.isRunning()) {
            if (SwingUtilities.isEventDispatchThread()) {
                t.stop();
            } else {
                SwingUtilities.invokeLater(t::stop);
            }
        }
    }

    // ------------------------------------------------------------------
    // Page-owned, period-grouped animation timers
    // ------------------------------------------------------------------

    private NtxAnimationTimer registerTimer(Object owner, long periodMs, Runnable tick) {
        if (owner == null || tick == null) {
            return NtxAnimationTimer.NONE;
        }
        long p = Math.max(MIN_TIMER_PERIOD_MS, periodMs);
        TimerHandle h;
        synchronized (timerLock) {
            if (discarded) {
                return NtxAnimationTimer.NONE;
            }
            h = timersByOwner.get(owner);
            if (h == null) {
                h = new TimerHandle(owner, p, tick);
                timersByOwner.put(owner, h);
                TimerGroup g = groupOf(p);
                g.handles.add(h);
                h.group = g;
            } else {
                h.tick = tick;
                if (h.periodMs != p) {
                    TimerGroup old = h.group;
                    if (old != null) {
                        old.handles.remove(h);
                        if (old.handles.isEmpty()) {
                            dropGroup(old);
                        }
                    }
                    h.periodMs = p;
                    TimerGroup g = groupOf(p);
                    g.handles.add(h);
                    h.group = g;
                }
            }
        }
        runOnUi(this::applyTimerState);
        return h;
    }

    private void cancelTimer(TimerHandle h) {
        if (h == null) {
            return;
        }
        synchronized (timerLock) {
            if (h.cancelled) {
                return;
            }
            h.cancelled = true;
            timersByOwner.remove(h.owner);
            TimerGroup g = h.group;
            if (g != null) {
                g.handles.remove(h);
                if (g.handles.isEmpty()) {
                    dropGroup(g);
                }
            }
        }
    }

    /** starts/stops every group according to the current visibility state. */
    private void applyTimerState() {
        List<TimerGroup> groups;
        boolean run;
        synchronized (timerLock) {
            groups = new ArrayList<>(timerGroups.values());
            run = showing && !discarded;
        }
        for (TimerGroup g : groups) {
            if (run) {
                g.start();
            } else {
                g.stop();
            }
        }
    }

    private void stopAndClearTimers() {
        List<TimerGroup> groups;
        synchronized (timerLock) {
            groups = new ArrayList<>(timerGroups.values());
            timerGroups.clear();
            timersByOwner.clear();
        }
        for (TimerGroup g : groups) {
            g.handles.clear();
            g.stop();
        }
    }

    /** caller must hold {@link #timerLock}. */
    private TimerGroup groupOf(long periodMs) {
        TimerGroup g = timerGroups.get(periodMs);
        if (g == null) {
            g = new TimerGroup(periodMs);
            timerGroups.put(periodMs, g);
        }
        return g;
    }

    /** caller must hold {@link #timerLock}. */
    private void dropGroup(TimerGroup g) {
        timerGroups.remove(g.periodMs);
        g.stop();
    }

    private void runOnUi(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    private class TimerGroup {
        final long periodMs;
        final LinkedHashSet<TimerHandle> handles = new LinkedHashSet<>();
        javax.swing.Timer timer;

        TimerGroup(long periodMs) {
            this.periodMs = periodMs;
        }

        void start() {
            if (timer == null) {
                int d = (int) Math.max(MIN_TIMER_PERIOD_MS, Math.min(Integer.MAX_VALUE, periodMs));
                timer = new javax.swing.Timer(d, e -> fire());
                timer.setCoalesce(true);
            }
            if (!timer.isRunning()) {
                timer.start();
            }
        }

        void stop() {
            javax.swing.Timer t = timer;
            if (t != null && t.isRunning()) {
                t.stop();
            }
        }

        boolean isRunning() {
            javax.swing.Timer t = timer;
            return t != null && t.isRunning();
        }

        void fire() {
            List<TimerHandle> snapshot;
            synchronized (timerLock) {
                if (handles.isEmpty()) {
                    stop();
                    return;
                }
                snapshot = new ArrayList<>(handles);
            }
            for (TimerHandle h : snapshot) {
                if (h.cancelled) {
                    continue;
                }
                try {
                    h.tick.run();
                } catch (Throwable ex) {
                    if (engine != null) {
                        engine.log().log(NMsg.ofC("animation timer tick failed: %s", ex));
                    }
                }
            }
        }
    }

    private class TimerHandle implements NtxAnimationTimer {
        final Object owner;
        volatile long periodMs;
        volatile Runnable tick;
        volatile TimerGroup group;
        volatile boolean cancelled;

        TimerHandle(Object owner, long periodMs, Runnable tick) {
            this.owner = owner;
            this.periodMs = periodMs;
            this.tick = tick;
        }

        @Override
        public long periodMillis() {
            return periodMs;
        }

        @Override
        public boolean isRunning() {
            TimerGroup g = group;
            return !cancelled && g != null && g.isRunning();
        }

        @Override
        public void cancel() {
            cancelTimer(this);
        }
    }

    private void firePageLifecycle(boolean show) {
        NTxNode root;
        try {
            root = page.compiledPage();
        } catch (Throwable ignored) {
            return;
        }
        firePageLifecycleNode(root, show);
    }

    private void firePageLifecycleNode(NTxNode node, boolean show) {
        if (node == null) {
            return;
        }
        if (engine != null) {
            NTxNodeRenderer r = engine.getRenderer(node.type()).orNull();
            if (r != null) {
                NtxPageLifecycleEvent event = new PageLifecycleEvent(node);
                if (show) {
                    r.onPageShow(event);
                } else {
                    r.onPageHide(event);
                }
            }
        }
        for (NTxNode child : node.children()) {
            firePageLifecycleNode(child, show);
        }
    }

    private class PageLifecycleEvent implements NtxPageLifecycleEvent {
        private final NTxNode node;

        PageLifecycleEvent(NTxNode node) {
            this.node = node;
        }

        @Override
        public NTxNode node() {
            return node;
        }

        @Override
        public NtxPageLifecycle lifecycle() {
            return lifecycle;
        }
    }

    private void repaintHierarchy() {
        // Only this page's image changed: repaint it. Revalidating the page and
        // every ancestor (and forcing Window.validate()) once per rendered frame
        // re-laid out the whole viewer — including the hundreds of off-screen
        // CardLayout cards — and made resizing interactively unusable.
        repaint();
    }

    /**
     * Size to render at, or {@code null} when the component has not been laid
     * out yet (or is a hidden CardLayout card, whose bounds are 0x0). Rendering
     * at a fallback size in that case compiled every off-screen page of the
     * deck on startup.
     */
    private Dimension currentRenderSize() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 16 || h <= 16) {
            return null;
        }
        return new Dimension(w, h);
    }

    private void requestRender() {
        requestRender(true);
    }

    private void requestRender(boolean priority) {
        synchronized (renderLock) {
            if (discarded) {
                return;
            }
            if (!showing) {
                // CardLayout resizes every card (hidden ones included) when the
                // window is laid out; hidden pages must not render: that compiled
                // and painted the whole deck at startup. onShow() forces the
                // first pass once the page becomes visible.
                return;
            }
            Dimension d = currentRenderSize();
            if (d == null) {
                // not laid out yet; the resize event that gives us a real size
                // will schedule the first pass
                return;
            }
            boolean sizeOk = pageImage != null && d.width == renderW && d.height == renderH;
            if (sizeOk && !renderInvalidated) {
                return;
            }
            if (renderPending && d.width == pendingW && d.height == pendingH) {
                //A render at this exact size is already in flight: coalesce this
                //request into it (the in-flight pass will schedule the follow-up
                //through the trailing `more` check) instead of bumping the epoch
                //and killing the pass. Without coalescing, the 40ms animation tick
                //discards every frame of a page whose render is slower than the
                //tick, leaving stale images: the animation looks frozen or moves
                //in huge steps, and gets worse while resizing (slower passes).
                renderInvalidated = true;
                return;
            }
            renderPending = true;
            renderInvalidated = false;
            pendingW = d.width;
            pendingH = d.height;
            boolean reuse = pageImage != null && d.width == renderW && d.height == renderH;
            final int tw = d.width;
            final int th = d.height;
            final boolean useCache = reuse;
            final int epoch = ++renderEpoch;
            DocumentView.submitRender(() -> doRender(tw, th, useCache, epoch), priority);
        }
    }

    private void doRender(final int tw, final int th, final boolean useCache, final int epoch) {
        if (discarded || epoch != renderEpoch) {
            return;
        }
        synchronized (pageRenderLock) {
            if (discarded || epoch != renderEpoch) {
                return;
            }
            if (renderCacheDirty) {
                renderCacheDirty = false;
                if (page.isCompiled()) {
                    NTxNode p = page.compiledPage();
                    if (p != null) {
                        p.invalidateRenderCache();
                    }
                }
            }
            BufferedImage img = null;
            Throwable error = null;
            rendering = true;
            try {
                img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                NTxNodeRendererConfig config = new NTxNodeRendererConfig();
                config.setWidth(tw);
                config.setHeight(th);
                config.setCapability(NTxRendererContext.CAPABILITY_ANIMATE, true);
                config.setCapability(NTxRendererContext.CAPABILITY_SEARCH_HIGHLIGHTS, true);
                config.setCapability(NtxPageLifecycle.CAPABILITY_PAGE_LIFECYCLE, lifecycle);
                config.setStartTime(pageStartTime);
                config.setUseCache(useCache);
                engine.renderPage(page, config, g, this, this::onRenderInvalidated);
                g.dispose();
            } catch (Throwable t) {
                error = t;
            } finally {
                rendering = false;
            }
            if (error != null) {
                engine.log().log(NMsg.ofC("failed to render page %s: %s", page.index() + 1, error));
            }
            synchronized (renderLock) {
                if (discarded || epoch != renderEpoch) {
                    return;
                }
                Dimension d = currentRenderSize();
                int cw = d == null ? -1 : d.width;
                int ch = d == null ? -1 : d.height;
                if (cw == tw && ch == th && img != null) {
                    pageImage = img;
                    renderW = tw;
                    renderH = th;
                }
                renderPending = false;
                boolean more = renderInvalidated || (d != null && (cw != tw || ch != th));
                renderInvalidated = false;
                if (more) {
                    requestRender(true);
                }
            }
            if (error != null) {
                engine.log().log(NMsg.ofC("render page %s failed %s", page.index() + 1, error));
            } else {
                SwingUtilities.invokeLater(this::repaintHierarchy);
            }
        }
    }

    private void onRenderInvalidated() {
        synchronized (renderLock) {
            if (discarded) {
                return;
            }
            renderInvalidated = true;
            renderCacheDirty = true;
            if (rendering) {
                // the pass in flight will notice renderInvalidated and schedule
                // exactly one follow-up when it completes
                return;
            }
        }
        requestRender(true);
    }

    public boolean isLoading() {
        if (!page.isCompiled() || !document.isCompiled()) {
            return true;
        }
        // `renderPending` is set again on every animation frame (and on resize/invalidation),
        // so it is true almost permanently on an animated page. It must not raise the "loading"
        // indicator: the page already has a frame to show. Only report loading until this page
        // has produced its first image.
        return renderPending && pageImage == null;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage img = pageImage;
        if (img == null) {
            if (page.isCompiled()) {
                g.setColor(Color.DARK_GRAY);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            return;
        }
        Graphics2D g2d = (Graphics2D) g;
        Object oldInterpolation = g2d.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Object oldRendering = g2d.getRenderingHint(RenderingHints.KEY_RENDERING);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2d.drawImage(img, 0, 0, getWidth(), getHeight(), null);
        if (oldInterpolation != null) {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
        }
        if (oldRendering != null) {
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, oldRendering);
        }
    }

    public Object source() {
        return page.source();
    }

    public NTxCompiledPage page() {
        return page;
    }
}