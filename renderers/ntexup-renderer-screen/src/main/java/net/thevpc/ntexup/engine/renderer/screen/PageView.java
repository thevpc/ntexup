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
    private static final int DEFAULT_RENDER_WIDTH = 1024;
    private static final int FRAME_PERIOD_MS = 40;
    private static final int MIN_TIMER_PERIOD_MS = 5;
    private final NTxCompiledPage page;
    private volatile long pageStartTime;
    private volatile boolean showing;
    private volatile boolean frameRequested;
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
    private final double ratio = 16.0 / 9.0;
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
                    repaintDirty();
                }
            });
        }
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
            cancelInFlightRender();
            pageImage = null;
            renderInvalidated = true;
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
        requestRender(false);
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
            stopFrameTimer();
            return;
        }
        if (frameRequested) {
            frameRequested = false;
            forceRender();
        } else {
            // nothing wants to animate right now: idle until asked again
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

    protected void repaintHierarchy() {
        this.revalidate();
        this.repaint();
        Component c = this;
        while (c != null) {
            c.revalidate();
            c.repaint();
            if (c instanceof Window) {
                ((Window) c).validate();
                ((Window) c).repaint();
                break;
            }
            c = c.getParent();
        }
    }

    private Dimension currentRenderSize() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 16 || h <= 16) {
            w = DEFAULT_RENDER_WIDTH;
            h = (int) (w / ratio);
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
            Dimension d = currentRenderSize();
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
            }
            if (error != null) {
                engine.log().log(NMsg.ofC("failed to render page %s: %s", page.index() + 1, error));
            }
            int cw;
            int ch;
            boolean more;
            synchronized (renderLock) {
                if (discarded || epoch != renderEpoch) {
                    return;
                }
                Dimension d = currentRenderSize();
                cw = d.width;
                ch = d.height;
                if (cw == tw && ch == th && img != null) {
                    pageImage = img;
                    renderW = tw;
                    renderH = th;
                }
                renderPending = false;
                more = renderInvalidated || (cw != tw || ch != th);
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
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, oldRendering);
    }

    public Object source() {
        return page.source();
    }

    public NTxCompiledPage page() {
        return page;
    }
}