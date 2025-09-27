package net.thevpc.ntexup.debug;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.engine.util.NTxUtilsImages;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererListener;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.util.NTexupUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class NTxDebugFrame extends JFrame {
    private NTxDebugPanel debugPanel;
    private Runnable onClose;
    NTxDocumentRendererListener hDocumentRendererListener = new NTxDocumentRendererListener() {
        @Override
        public void onChangedCompiledDocument(NTxCompiledDocument compiledDocument) {
            model().setCompiledDocument(compiledDocument.compiledDocument());
            model().setRawDocument(compiledDocument.rawDocument());
            updateContent();
        }

        @Override
        public void onChangedPage(NTxCompiledPage page) {
            model().setCurrentPage(page.rawPage());
            updateContent();
        }

        @Override
        public void onSaveDocument(NTxCompiledDocument document, NTxDocumentStreamRendererConfig config) {

        }

        @Override
        public void onCloseView() {
            model().setCurrentPage(null);
            model().setCompiledDocument(null);
            model().setRawDocument(null);
            updateContent();
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
                        new ImageIcon(getClass().getResource("/net/thevpc/ntexup/ntexup-logo.png")).getImage(),
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

    public void run() {
        NTexupUtils.runUiAsync(()->{
            updateContent();
            setVisible(true);
        });
    }
}
