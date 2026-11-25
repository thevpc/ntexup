package net.thevpc.ntexup.debug;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.util.NTxUtilsImages;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererListener;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.engine.renderer.screen.PageView;
import net.thevpc.ntexup.util.NTexupUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class NTxDebugFrame extends JFrame {
    private NTxDebugPanel debugPanel;
    private Runnable onClose;
    /**
     * True when the document/page model changed while the debug frame was
     * hidden. Refreshing the debug panel serializes the whole document, so it
     * must not run on every slide change when nobody is looking at it.
     */
    private volatile boolean pendingRefresh;

    NTxDocumentRendererListener hDocumentRendererListener = new NTxDocumentRendererListener() {
        @Override
        public void onChangedCompiledDocument(NTxCompiledDocument compiledDocument) {
            model().setCompiledDocument(compiledDocument.document());
            model().setRawDocument(compiledDocument.rawDocument());
            requestRefresh();
        }

        @Override
        public void onChangedPage(NTxCompiledPage page) {
            model().setCurrentPage(page==null?null:page.rawPage());
            requestRefresh();
        }

        @Override
        public void onSaveDocument(NTxCompiledDocument document, NTxDocumentStreamRendererConfig config) {

        }

        @Override
        public void onCloseView() {
            model().setCurrentPage(null);
            model().setCompiledDocument(null);
            model().setRawDocument(null);
            requestRefresh();
        }
    };

    public Runnable getOnClose() {
        return onClose;
    }

    public NTxDebugFrame setOnClose(Runnable onClose) {
        this.onClose = onClose;
        return this;
    }

    public NTxDebugFrame(NTxEngine engine) {

        setTitle("DebugFrame");
        setContentPane(debugPanel = new NTxDebugPanel(engine));
        setMinimumSize(new Dimension(400, 600));
        this.setIconImage(
                NTxUtilsImages.resizeImage(
                        new ImageIcon(PageView.class.getResource("/net/thevpc/ntexup/ntexup-logo.png")).getImage(),
                        16, 16)
        );
        addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                if(onClose != null) {
                    onClose.run();
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {
                if(onClose != null) {
                    onClose.run();
                }
            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }
        });
    }

    public NTxLogger messages() {
        return model().messages();
    }

    public NTxDocumentRendererListener rendererListener() {
        return hDocumentRendererListener;
    }

    public NTxDebugModel model() {
        return debugPanel.model();
    }

    public void updateContent() {
        debugPanel.updateContent();
    }

    private void requestRefresh() {
        if (isVisible()) {
            updateContent();
        } else {
            pendingRefresh = true;
        }
    }

    public void run() {
        NTexupUtils.runUiAsync(()->{
            setVisible(true);
            if (pendingRefresh) {
                pendingRefresh = false;
                updateContent();
            }
        });
    }
}
