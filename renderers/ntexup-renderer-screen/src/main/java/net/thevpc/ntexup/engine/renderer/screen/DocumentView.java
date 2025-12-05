package net.thevpc.ntexup.engine.renderer.screen;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.source.NTxSourceMonitor;
import net.thevpc.ntexup.api.renderer.*;


import net.thevpc.ntexup.engine.impl.NTxCompiledDocumentImpl;
import net.thevpc.ntexup.engine.renderer.screen.components.RatioPanel;
import net.thevpc.ntexup.engine.util.NTxUtilsImages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NColor;

public class DocumentView {

    NTxCompiledDocument compiledDocument;
    private NTxDocumentRendererSupplier documentSupplier;
    NTxEngine engine;
    private List<PageView> pageViews = new ArrayList<>();
    JFrame frame;
    DocumentViewContentPanel contentPane;
    PageView currentShowingPage;
    private Map<String, PageView> pagesMapById = new HashMap<>();
    private Map<Integer, PageView> pagesMapByIndex = new HashMap<>();
    private Timer timer;
    private boolean inCheckResourcesChanged;
    private boolean inLoadDocument;
    Throwable currentThrowable;
    NTxDocumentRendererListener listener;
    private NTxDocumentRendererContext rendererContext = new NTxDocumentRendererContextImpl();
    private boolean isShown;
    public float defaultDocumentRatio = 842.0F / 595.0F;
    public float documentRatio = defaultDocumentRatio;

    public DocumentView(NTxDocumentRendererSupplier documentSupplier,
                        NTxEngine engine, NTxDocumentRendererListener listener) {
        this.documentSupplier = documentSupplier;
        this.listener = listener;
        this.engine = engine;

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
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        prepareContentPane();
        timer = new Timer("DocumentViewResourcesMonitor", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                animate();
            }
        }, 10, 10);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkResourcesChanged();
            }
        }, 3000, 1000);
    }

    public NTxCompiledDocument compiledDocument() {
        return compiledDocument;
    }

    private void animate() {
        this.contentPane.invalidate();
        this.contentPane.repaint();
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
                NTxSourceMonitor r = compiledDocument.compiledDocument().sourceMonitor();
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
        if (s != null) {
            if (s instanceof NTxSource) {
                NPath path = ((NTxSource) s).path().orNull();
                if (path != null) {
                    return path.getName();
                }
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

    public void prepareContentPane() {
        contentPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    new Thread(() -> nextPage()).start();
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    new DocumentPopupMenu(DocumentView.this).showPopupMenu(e);
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
                    case KeyEvent.VK_SPACE:
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_UP: {
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
                    case KeyEvent.VK_DOWN: {
                        if (e.isControlDown()) {
                            firstPage();
                        } else {
                            previousPage();
                        }
                        break;
                    }
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
        new Thread(() -> {
            reloadDocumentSync();
            if (!isShown && getPagesCount()>0) {
                isShown = true;
                show();
            }
        }).start();
    }

    private boolean reloadDocumentSync() {
        if (inLoadDocument) {
            return false;
        }
        listener.onStartLoadingDocument();
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
                this.compiledDocument=documentSupplier.get(rendererContext);
                SwingUtilities.invokeLater(() -> {
                    frame.setTitle(this.compiledDocument.title());
                });
            } catch (Exception ex) {
                engine.log().log(NMsg.ofC("compile document failed %s", ex));
                this.currentThrowable = ex;
            }
            if (compiledDocument == null) {
                compiledDocument = new NTxCompiledDocumentImpl(engine.documentFactory().ofDocument(null),engine);
            }
            listener.onChangedCompiledDocument(compiledDocument);

            compiledDocument.compiledDocument().sourceMonitor().save();
            pageViews.clear();
            contentPane.removeAll();
            pagesMapById.clear();
            pagesMapByIndex.clear();
            for (int i = 0; i < compiledDocument.pages().size(); i++) {
                pageViews.add(createPageView(compiledDocument.pages().get(i)));
            }
            for (PageView pageView : pageViews) {
                contentPane.add(new RatioPanel(pageView.component(), documentRatio, new Color(NColor.GRAY_15.getRGB())), pageView.id());
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
                        showPage(1);
                    }
                }
            } else {
                showPage(1);
            }
        } finally {
            this.inLoadDocument = false;
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

    void lastPage() {
        showPage(getPagesCount());
    }

    void firstPage() {
        showPage(1);
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
                SwingUtilities.invokeLater(() -> contentPane.doShow(pv.id()));
            } else {
                listener.onChangedPage(null);
            }
        }
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public int getPagesCount() {
        return pageViews.size();
    }

    public void show() {
        showPage(1);
    }

    public void showPage(int index) {
        int count = getPagesCount();
        if(count<=0){
            JOptionPane.showMessageDialog(contentPane,"No Pages to render","Error",JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (index > count) {
            index = count;
        } else if (index < 1) {
            index = 1;
        }
        PageView pageView = pageViews.get(index-1);
        this.showPage(pageView);
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
            if (i - 1 >= 1) {
                showPage(i - 1);
            }
        }
    }


}
