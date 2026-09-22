package net.thevpc.ntexup.engine.renderer.screen;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.collections.NMaps;
import net.thevpc.nuts.text.NMsg;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.UUID;

public class PageView extends JComponent {
    public static Dimension REF_SIZE = new Dimension(1024, 768);
    private static final int DEFAULT_RENDER_WIDTH = 1024;
    private final NTxCompiledPage page;
    private volatile long pageStartTime;
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
    private volatile boolean discarded;
    private volatile int renderEpoch;

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
        if (this.page.isCompiled()) {
            NTxNode p = this.page.compiledPage();
            if (p != null) {
                p.invalidateRenderCache();
            }
        }
        synchronized (renderLock) {
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
            renderEpoch++;
            pageImage = null;
            renderW = -1;
            renderH = -1;
            renderInvalidated = true;
        }
    }

    /**
     * Called when the view is discarded (document reload / viewer close).
     */
    public void discard() {
        synchronized (renderLock) {
            discarded = true;
            renderEpoch++;
            pageImage = null;
            renderInvalidated = false;
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

    }

    synchronized void onShow() {
        if (discarded) {
            return;
        }
        page.compiledPage();
        this.pageStartTime = System.currentTimeMillis();
        requestRender(true);
    }

    public void prefetch() {
        requestRender(false);
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
            renderPending = true;
            renderInvalidated = false;
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
        BufferedImage img = null;
        Throwable error = null;
        try {
            img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            NTxNodeRendererConfig config = new NTxNodeRendererConfig();
            config.setWidth(tw);
            config.setHeight(th);
            config.setCapabilities(NMaps.of(NTxRendererContext.CAPABILITY_ANIMATE, true));
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

    private void onRenderInvalidated() {
        synchronized (renderLock) {
            if (discarded) {
                return;
            }
            renderInvalidated = true;
        }
        requestRender(true);
    }

    public boolean isLoading() {
        return !page.isCompiled() || !document.isCompiled() || renderPending;
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