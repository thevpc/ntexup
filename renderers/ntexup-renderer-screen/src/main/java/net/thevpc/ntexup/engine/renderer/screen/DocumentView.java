package net.thevpc.ntexup.engine.renderer.screen;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.source.NTxSourceMonitor;
import net.thevpc.ntexup.api.renderer.*;


import net.thevpc.ntexup.engine.impl.NTxCompiledDocumentImpl;
import net.thevpc.ntexup.engine.renderer.screen.components.RatioPanel;
import net.thevpc.ntexup.api.util.NTxUtilsImages;

import net.thevpc.ntexup.engine.renderer.screen.components.PresentationHud;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.Comparator;

import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NColor;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DocumentView implements NTxDocumentView {

    NTxCompiledDocument compiledDocument;
    private NTxDocumentRendererSupplier documentSupplier;
    NTxEngine engine;
    private List<PageView> pageViews = new ArrayList<>();
    JFrame frame;
    DocumentViewContentPanel contentPane;
    PageView currentShowingPage;
    private Map<String, PageView> pagesMapById = new HashMap<>();
    private Map<Integer, PageView> pagesMapByIndex = new HashMap<>();
    private Timer resourceMonitorTimer;
    private javax.swing.Timer loadingAnimationTimer;
    private PresentationHud hud;
    private boolean isFullScreen = false;
    private Rectangle windowedBounds;
    private static final ExecutorService ASYNC_LOADER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DocumentView-AsyncLoader");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicLong RENDER_SEQ = new AtomicLong();
    static final ExecutorService PAGE_RENDER_LOADER =
            new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new PriorityBlockingQueue<>(64, (a, b) -> {
                if (!(a instanceof RenderTask)) {
                    return 0;
                }
                if (!(b instanceof RenderTask)) {
                    return 0;
                }
                RenderTask ta = (RenderTask) a;
                RenderTask tb = (RenderTask) b;
                int c = Integer.compare(tb.priority, ta.priority);
                return c != 0 ? c : Long.compare(ta.seq, tb.seq);
            }), r -> {
                Thread t = new Thread(r, "DocumentView-PageRenderer");
                t.setDaemon(true);
                return t;
            });

    static void submitRender(Runnable r, boolean priority) {
        PAGE_RENDER_LOADER.execute(new RenderTask(r, priority ? 1 : 0, RENDER_SEQ.incrementAndGet()));
    }

    private static final class RenderTask implements Runnable {
        final Runnable delegate;
        final int priority;
        final long seq;

        RenderTask(Runnable delegate, int priority, long seq) {
            this.delegate = delegate;
            this.priority = priority;
            this.seq = seq;
        }

        @Override
        public void run() {
            delegate.run();
        }
    }
    private boolean inCheckResourcesChanged;
    private boolean inLoadDocument;
    Throwable currentThrowable;
    NTxDocumentRendererListener listener;
    private NTxDocumentRendererContext rendererContext = new NTxDocumentRendererContextImpl();
    private boolean isShown;
    private boolean closed;
    public float defaultDocumentRatio = 842.0F / 595.0F;
    public float documentRatio = defaultDocumentRatio;
    public ScreenDocumentRenderer renderer;
    public NTxDocumentViewManager documentViewManager;
    public List<NTxDocumentViewListener> listeners = new ArrayList<>();

    public DocumentView(NTxDocumentRendererSupplier documentSupplier,
                        NTxEngine engine, NTxDocumentRendererListener listener, ScreenDocumentRenderer renderer) {
        this.documentSupplier = documentSupplier;
        this.listener = listener;
        this.engine = engine;
        this.renderer = renderer;
        documentViewManager = renderer.getProperty(NTxDocumentViewManager.class).orNull();
        frame = new JFrame();
        frame.setTitle("NTexup Viewer");
        frame.setIconImage(
                NTxUtilsImages.resizeImage(
                        new ImageIcon(getClass().getResource("/net/thevpc/ntexup/ntexup-logo.png")).getImage(),
                        16, 16)
        );
        contentPane = new DocumentViewContentPanel(this);
//        contentPane.setFocusTraversalKeysEnabled(false);
        frame.setContentPane(contentPane);
        reloadDocumentAsync();
        frame.setSize(PageView.REF_SIZE);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
        prepareContentPane();
        resourceMonitorTimer = new Timer("DocumentViewResourcesMonitor", true);
        resourceMonitorTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkResourcesChanged();
            }
        }, 3000, 1000);
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (PageView pv : pageViews) {
            pv.discard();
        }
        if (resourceMonitorTimer != null) {
            resourceMonitorTimer.cancel();
        }
        stopLoadingAnimation();
        frame.setVisible(false);
        for (NTxDocumentViewListener nTxDocumentViewListener : listeners.toArray(new NTxDocumentViewListener[0])) {
            nTxDocumentViewListener.documentClosed(this);
        }
        frame.dispose();
    }

    @Override
    public void addDocumentListener(NTxDocumentViewListener r) {
        if (r != null) {
            this.listeners.add(r);
        }
    }

    @Override
    public void removeDocumentListener(NTxDocumentViewListener r) {
        this.listeners.remove(r);
    }

    @Override
    public String getTitle() {
        return frame.getTitle();
    }

    public NTxCompiledDocument compiledDocument() {
        return compiledDocument;
    }

    public boolean isFullScreen() {
        return isFullScreen;
    }

    public void toggleFullScreen() {
        setFullScreen(!isFullScreen);
    }

    public void setFullScreen(boolean fullScreen) {
        if (this.isFullScreen == fullScreen) {
            return;
        }
        this.isFullScreen = fullScreen;
        SwingUtilities.invokeLater(() -> {
            frame.dispose();
            if (isFullScreen) {
                windowedBounds = frame.getBounds();
                frame.setUndecorated(true);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice gd = ge.getDefaultScreenDevice();
                try {
                    if (gd.isFullScreenSupported()) {
                        gd.setFullScreenWindow(frame);
                    } else {
                        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                        frame.setVisible(true);
                    }
                } catch (Exception ex) {
                    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    frame.setVisible(true);
                }
            } else {
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice gd = ge.getDefaultScreenDevice();
                if (gd.getFullScreenWindow() == frame) {
                    gd.setFullScreenWindow(null);
                }
                frame.setUndecorated(false);
                frame.setExtendedState(JFrame.NORMAL);
                if (windowedBounds != null) {
                    frame.setBounds(windowedBounds);
                } else {
                    frame.setSize(PageView.REF_SIZE);
                    frame.setLocationRelativeTo(null);
                }
                frame.setVisible(true);
            }
            repositionHud();
            contentPane.requestFocusInWindow();
        });
    }

    public void repositionHud() {
        if (hud != null) {
            int hudW = Math.min(410, frame.getWidth() - 40);
            int hudH = 38;
            int hudX = (frame.getWidth() - hudW) / 2;
            int hudY = frame.getHeight() - hudH - (isFullScreen ? 25 : 55);
            hud.setBounds(hudX, hudY, hudW, hudH);
        }
    }

    private void startLoadingAnimation() {
        SwingUtilities.invokeLater(() -> {
            if (loadingAnimationTimer == null) {
                loadingAnimationTimer = new javax.swing.Timer(40, e -> {
                    if (isPageLoading()) {
                        contentPane.repaint();
                    } else {
                        stopLoadingAnimation();
                    }
                });
            }
            if (!loadingAnimationTimer.isRunning()) {
                loadingAnimationTimer.start();
            }
        });
    }

    private void stopLoadingAnimation() {
        SwingUtilities.invokeLater(() -> {
            if (loadingAnimationTimer != null && loadingAnimationTimer.isRunning()) {
                loadingAnimationTimer.stop();
            }
            contentPane.repaint();
        });
    }

    private void checkResourcesChanged() {
        if (inLoadDocument) {
            return;
        }
        if (inCheckResourcesChanged) {
            return;
        }
        this.inCheckResourcesChanged = true;
        try {
            if (inLoadDocument) {
                return;
            }
            if (compiledDocument != null) {
                NTxSourceMonitor r = compiledDocument.sourceMonitor();
                if (r.changed()) {
                    reloadDocumentAsync();
                }
            }
        } finally {
            this.inCheckResourcesChanged = false;
        }
    }

    public boolean isPageLoading() {
        PageView cp = currentShowingPage;
        return isLoading() || (cp != null && cp.isLoading());
    }

    public boolean isLoading() {
        return inLoadDocument;
    }

    public String getPageSourceName() {
        Object s = getPageSource();
        if (s == null && currentShowingPage != null && currentShowingPage.page() != null) {
            s = currentShowingPage.page().source();
            if (s == null && currentShowingPage.page().rawPage() != null) {
                s = net.thevpc.ntexup.api.util.NTxUtils.sourceOf(currentShowingPage.page().rawPage());
            }
        }
        if (s != null) {
            if (s instanceof NTxSource) {
                NTxSource src = (NTxSource) s;
                String sn = src.shortName();
                if (sn != null && !sn.isEmpty()) {
                    return sn;
                }
                NPath path = src.path().orNull();
                if (path != null) {
                    return path.name();
                }
            }
            if (s instanceof String) {
                return (String) s;
            }
        }
        return null;
    }

    public int getPageUserIndex() {
        int index = currentShowingPage == null ? 0 : currentShowingPage.index();
        int currentIndex = index + 1;
        return currentIndex;
    }

    public int getPageIndex() {
        int index = currentShowingPage == null ? 0 : currentShowingPage.index();
        return index;
    }

    public Object getPageSource() {
        return currentShowingPage == null ? null : currentShowingPage.source();
    }

    public JFrame getFrame() {
        return frame;
    }

    public void prepareContentPane() {
        hud = new PresentationHud(this);
        frame.getLayeredPane().add(hud, Integer.valueOf(JLayeredPane.POPUP_LAYER));
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                repositionHud();
            }
        });

        contentPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    nextPage();
                    if (hud != null) {
                        hud.ping();
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    new DocumentPopupMenu(DocumentView.this).showPopupMenu(e);
                }
            }
        });

        contentPane.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (hud != null) {
                    hud.ping();
                }
            }
        });

        contentPane.setFocusTraversalKeysEnabled(false);
        contentPane.setFocusable(true);
        contentPane.requestFocus();
//        this.requestFocusInWindow();
        contentPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_F11: {
                        toggleFullScreen();
                        break;
                    }
                    case KeyEvent.VK_ESCAPE: {
                        if (isFullScreen) {
                            setFullScreen(false);
                        }
                        break;
                    }
                    case KeyEvent.VK_HOME: {
                        firstPage();
                        break;
                    }
                    case KeyEvent.VK_END: {
                        lastPage();
                        break;
                    }
                    case KeyEvent.VK_PAGE_UP: {
                        previousPage();
                        break;
                    }
                    case KeyEvent.VK_PAGE_DOWN: {
                        nextPage();
                        break;
                    }
                    case KeyEvent.VK_SPACE:
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_DOWN: {
                        if (e.isControlDown()) {
                            lastPage();
                        } else {
                            nextPage();
                        }
                        break;
                    }
                    case KeyEvent.VK_F5: {
                        reloadDocumentAsync();
                        break;
                    }
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_UP: {
                        if (e.isControlDown()) {
                            firstPage();
                        } else {
                            previousPage();
                        }
                        break;
                    }
                }
                if (hud != null) {
                    hud.ping();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }
        });
    }


    private void reloadDocumentAsync() {
        if (inLoadDocument) {
            return;
        }
        ASYNC_LOADER.submit(() -> {
            reloadDocumentSync();
            if (!isShown && getPagesCount() > 0) {
                isShown = true;
                show();
            }
        });
    }

    private boolean reloadDocumentSync() {
        if (inLoadDocument) {
            return false;
        }
        listener.onStartLoadingDocument();
        startLoadingAnimation();
        this.inLoadDocument = true;
        try {
            PageView oldPage = this.currentShowingPage;
            int oldIndex = 0;
            String oldId = null;
            if (oldPage != null) {
                oldIndex = oldPage.index();
                oldId = oldPage.id();
            }
            this.currentShowingPage = null;
            this.currentThrowable = null;
            try {
                this.compiledDocument = documentSupplier.get(rendererContext);
                SwingUtilities.invokeLater(() -> {
                    frame.setTitle(this.compiledDocument.title());
                });
            } catch (Exception ex) {
                engine.log().log(NMsg.ofC("compile document failed %s", ex));
                this.currentThrowable = ex;
            }
            if (compiledDocument == null) {
                compiledDocument = new NTxCompiledDocumentImpl(engine.documentFactory().ofDocument(null), engine);
            }
            listener.onChangedCompiledDocument(compiledDocument);

            compiledDocument.sourceMonitor().save();
            for (PageView pv : pageViews) {
                pv.discard();
            }
            pageViews.clear();
            contentPane.removeAll();
            pagesMapById.clear();
            pagesMapByIndex.clear();
            List<NTxCompiledPage> pages = compiledDocument.pages();
            for (NTxCompiledPage page : pages) {
                pageViews.add(createPageView(page));
            }
            for (PageView pageView : pageViews) {
                contentPane.add(new RatioPanel(pageView.component(), documentRatio, new Color(NColor.GRAY_15.rgb())), pageView.id());
                pagesMapById.put(pageView.id(), pageView);
                pagesMapByIndex.put(pageView.index(), pageView);
            }
            if (oldId != null) {
                PageView oo = pagesMapById.get(oldId);
                if (oo != null) {
                    showPage(oo.index());
                } else {
                    oo = pagesMapByIndex.get(oldIndex);
                    if (oo != null) {
                        showPage(oo.index());
                    } else {
                        showPage(0);
                    }
                }
            } else {
                showPage(0);
            }
        } finally {
            this.inLoadDocument = false;
            stopLoadingAnimation();
            listener.onEndLoadingDocument();
        }
        return true;
    }

    public void applyRatio(double ratio) {
        for (Component component : contentPane.getComponents()) {
            if (component instanceof RatioPanel) {
                ((RatioPanel) component).setRatio(ratio);
            }
        }
    }

    public void lastPage() {
        showPage(getPagesCount()-1);
    }

    public void firstPage() {
        showPage(0);
    }

    public PageView createPageView(NTxCompiledPage node) {
        return new PageView(
                compiledDocument,
                node,
                engine()
        );
    }

    public void showPage(PageView pv) {
        synchronized (this) {
            if (currentShowingPage != null) {
                currentShowingPage.onHide();
            }
            this.currentShowingPage = pv;
            if (pv != null) {
                listener.onChangedPage(pv.page());
                pv.onShow();
                refreshPageCache(pv.index());
                SwingUtilities.invokeLater(() -> contentPane.doShow(pv.id()));
            } else {
                listener.onChangedPage(null);
            }
        }
        if (hud != null) {
            SwingUtilities.invokeLater(() -> {
                hud.updateState(getPageUserIndex(), getPagesCount(), isFullScreen);
                hud.ping();
            });
        }
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public int getPagesCount() {
        return pageViews.size();
    }

    public void show() {
        showPage(0);
    }

    public void showPage(int index) {
        int count = getPagesCount();
        if (count <= 0) {
            //JOptionPane.showMessageDialog(contentPane,"No Pages to render","Error",JOptionPane.ERROR_MESSAGE);
//            return;
            this.showPage(null);
            return;
        }
        if (index >= count) {
            index = count-1;
        } else if (index < 0) {
            index = 0;
        }
        PageView pageView = pageViews.get(index);
        this.showPage(pageView);
    }

    private void refreshPageCache(int centerIndex) {
        int evictRadius = 4;
        int prefetchRadius = 3;
        List<PageView> prefetch = new ArrayList<>();
        for (PageView pv : pageViews) {
            int d = Math.abs(pv.index() - centerIndex);
            if (d > evictRadius) {
                pv.evictCache();
            } else if (d > 0 && d <= prefetchRadius) {
                prefetch.add(pv);
            }
        }
        prefetch.sort(Comparator.comparingInt(pv -> Math.abs(pv.index() - centerIndex)));
        for (PageView pv : prefetch) {
            pv.prefetch();
        }
    }

    public NTxEngine engine() {
        return engine;
    }

    public synchronized void nextPage() {
        if (currentShowingPage != null) {
            int i = currentShowingPage.index();
            if (i + 1 < pageViews.size()) {
                showPage(i + 1);
            }
        }
    }

    public void previousPage() {
        if (currentShowingPage != null) {
            int i = currentShowingPage.index();
            if (i - 1 >= 0) {
                showPage(i - 1);
            }
        }
    }


}
