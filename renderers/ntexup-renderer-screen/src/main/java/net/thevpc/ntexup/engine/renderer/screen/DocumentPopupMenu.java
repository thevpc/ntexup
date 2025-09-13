package net.thevpc.ntexup.engine.renderer.screen;

import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.engine.renderer.screen.utils.JPopupMenuHelper;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NLiteral;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public class DocumentPopupMenu {
    private DocumentView documentView;

    public DocumentPopupMenu(DocumentView documentView) {
        this.documentView = documentView;
    }

    void showPopupMenu(MouseEvent e) {
        JPopupMenuHelper popupMenu = new JPopupMenuHelper();
        popupMenu.addMenuItem("Save as PDF", ev -> {
            PdfConfigDialog configDialog = new PdfConfigDialog((Frame) SwingUtilities.getWindowAncestor((Component) e.getSource()));
            configDialog.setVisible(true);
            if (configDialog.isConfirmed()) {
                NTxDocumentStreamRendererConfig config = configDialog.getConfig();

                if (documentView.listener != null) {

                    documentView.listener.onSaveDocument(documentView.compiledDocument(), config);
                } else {
                    System.err.println("saveDocumentListener is null or document is null");
                }
            }
        });

        NTxSource source = documentView.compiledDocument().source();
        if (source != null) {
            NPath path = source.path().orNull();
            if (path != null) {
                File file = path.toFile().orNull();
                if (file != null) {
                    popupMenu.addMenuItem(file.isDirectory() ? "Open Project in Explorer" : "Open Project",
                            ev -> {
                                try {
                                    Desktop.getDesktop().open(file);
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            });
                }
            }
        }
        popupMenu.addSeparator().addMenuItem("First Page", ev -> {
            documentView.firstPage();
        });
        popupMenu.addMenuItem("Last Page", ev -> {
            documentView.lastPage();
        });
        popupMenu.addMenuItem("Goto Page...", ev -> {
            String s = JOptionPane.showInputDialog("Page Number?");
            Integer v = NLiteral.of(s).asInt().orElse(-1);
            if (v > 0) {
                documentView.showPage(v - 1);
            }
        });

        JMenu sizeMenu = new JMenu("Size");
        for (String s : new String[]{
                "800x480",
                "800x600",
                "960x640",
                "1024x768",
                "1138x640",
                "1152x768",
                "1280x720",
                "1280x800",
                "1280x1024",
                "1366x768",
                "1600x768",
                "1600x900",
                "1600x1280",
                "1600x1280",
                "2048x1080",
                "3024x1964",
        }) {
            JMenuItem menuItem = new JMenuItem(s);
            sizeMenu.add(menuItem);
            menuItem.addActionListener(ev -> {
                String[] u = s.split("x");
                documentView.frame.setSize(Integer.parseInt(u[0]), Integer.parseInt(u[1]));
            });
        }
        popupMenu.addMenu(sizeMenu);
        popupMenu.show(e);
    }
}
