package net.thevpc.ntexup.main;

import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentView;
import net.thevpc.ntexup.api.renderer.NTxDocumentViewListener;
import net.thevpc.ntexup.api.renderer.NTxDocumentViewManager;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.main.components.EntryComponent;
import net.thevpc.ntexup.engine.util.NTxUtilsImages;
import net.thevpc.ntexup.util.NTexupUtils;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NBlankable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame implements NTxDocumentViewManager {

    private NTxServiceHelper serviceHelper;
    private EntryComponent entryComponent;
    private final List<ProgressItem> progressItems = new ArrayList<>();
    private final JProgressBar progressBar = new JProgressBar();
    private java.util.List<NTxDocumentView> openDocuments=new ArrayList<>();
    private NTxDocumentViewListener documentListener = new NTxDocumentViewListener() {
        @Override
        public void documentClosed(NTxDocumentView view) {
            view.removeDocumentListener(this);
            openDocuments.remove(view);
            tryEffectiveExit();
        }
    };

    public MainFrame(NTxEngine engine) {
        serviceHelper = new NTxServiceHelper(this, engine == null ? new DefaultNTxEngine() : engine);
        setTitle("Ntexup Viewer");
        this.setIconImage(
                NTxUtilsImages.resizeImage(
                        new ImageIcon(getClass().getResource("/net/thevpc/ntexup/ntexup-logo.png")).getImage(),
                        16, 16)
        );
        setContentPane(createCenter());

//        setJMenuBar(jmb);
        setPreferredSize(new Dimension(600, 400));
        pack();
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // example: confirm, save, cleanup, etc.
                if (canExit()) {
                    hideFrame();
                }
            }
        });
    }

    private boolean canExit() {
        return true;
    }

    @Override
    public void exit() {
        for (NTxDocumentView openDocument : this.openDocuments.toArray(new NTxDocumentView[0])) {
            openDocument.close();
        }
        hideFrame();
    }

    protected void tryEffectiveExit() {
        if(isVisible()) {
           return;
        }
        if(!openDocuments.isEmpty()) {
            return;
        }
        effectiveExit();
    }

    protected void effectiveExit() {
        //this should be configurable
        System.exit(0);
    }

    public void openDocument(NTxDocumentView view) {
        if(view!=null){
            this.openDocuments.add(view);
            view.addDocumentListener(documentListener);
        }
    }

    public void hideFrame() {
        NTexupUtils.runUiAsync(() -> {
            setVisible(false);
            tryEffectiveExit();
        });
    }

    public void displayFrame() {
        NTexupUtils.runUiAsync(() -> {
            setVisible(true);
        });
    }

    @Override
    public void openMain() {
        displayFrame();
    }

    private JMenuBar createMenu() {
        JMenuBar jmb = new JMenuBar();
        jmb.add(createMenuFile());
        jmb.add(createMenuView());
        jmb.add(createMenuHelp());
        return jmb;
    }

    private JComponent createCenter() {
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.add(createMenu(), BorderLayout.NORTH);
        entryComponent = new EntryComponent(serviceHelper);
        jPanel.add(entryComponent, BorderLayout.CENTER);
        jPanel.add(progressBar, BorderLayout.SOUTH);
        return jPanel;
    }

    private JMenu createMenuHelp() {
        JMenu menu = new JMenu("Help");
        menu.add(new JMenuItem("About"));
        return menu;
    }

    private JMenu createMenuView() {
        JMenu menu = new JMenu("View");
        menu.add(createMenuItemDebugPane());
        return menu;
    }

    private JMenuItem createMenuItemDebugPane() {
        JMenuItem r = new JMenuItem("Debug Pane");
        r.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                serviceHelper.showDebug();
            }
        });
        return r;
    }

    private JMenu createMenuFile() {
        JMenu menu = new JMenu("File");
        menu.add(createMenuItemOpenFile());
        menu.add(createMenuItemNewFolder());
        menu.add(createMenuItemNewFile());
        menu.addSeparator();
        menu.add(createMenuItemExit());
        return menu;
    }


    private JMenuItem createMenuItemExit() {
        JMenuItem menu = new JMenuItem("Exit");
        menu.addActionListener(e -> serviceHelper.doExit());
        return menu;
    }

    private JMenuItem createMenuItemOpenFile() {
        JMenuItem menu = new JMenuItem("Open....");
        menu.addActionListener(e -> {
            serviceHelper.showOpenFile();
            entryComponent.reload();
        });
        return menu;
    }

    private JMenuItem createMenuItemNewFolder() {
        JMenuItem menu = new JMenuItem("New Project....");
        menu.addActionListener(e -> {
            serviceHelper.showNewProject();
            entryComponent.reload();
        });
        return menu;
    }

    private JMenuItem createMenuItemNewFile() {
        JMenuItem menu = new JMenuItem("New File....");
        menu.addActionListener(e -> {
            serviceHelper.showNewFile();
            entryComponent.reload();
        });
        return menu;
    }

    public void openProject(NPath path) {
        serviceHelper.openProject(path);
    }

    public void showNewProject() {
        serviceHelper.showNewProject();
    }

    public NTxServiceHelper getService() {
        return serviceHelper;
    }

    public NTxEngine getEngine() {
        return serviceHelper.getEngine();
    }

    public ProgressItem addProgressItem() {
        ProgressItem e = new MyProgressItem();
        progressItems.add(e);
        return e;
    }

    public interface ProgressItem {
        void dispose();

        String text();
    }

    private class MyProgressItem implements ProgressItem {
        private String text;

        public String text() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
            doUpdateProgressBar();
        }

        @Override
        public void dispose() {
            synchronized (progressItems) {
                progressItems.remove(this);
                doUpdateProgressBar();
            }
        }
    }

    private static class ProgressInfo {
        private String text;
        private boolean indeterminate;
        private int value;

        public ProgressInfo(String text, boolean indeterminate, int value) {
            this.text = text;
            this.indeterminate = indeterminate;
            this.value = value;
        }
    }

    private ProgressInfo resolveProgressInfo() {
        synchronized (progressItems) {
            if (progressItems.isEmpty()) {
                return new ProgressInfo("", false, 0);
            } else {
                for (int i = progressItems.size() - 1; i >= 0; i--) {
                    ProgressItem u = progressItems.get(i);
                    String t = u.text();
                    if (!NBlankable.isBlank(t)) {
                        progressBar.setIndeterminate(true);
                        progressBar.setString(t);
                        return new ProgressInfo(t, true, 0);
                    }
                }
                return new ProgressInfo("", true, 0);
            }
        }
    }

    private void doUpdateProgressBar() {
        ProgressInfo i = resolveProgressInfo();
        SwingUtilities.invokeLater(() -> {
            progressBar.setIndeterminate(i.indeterminate);
            progressBar.setString(i.text);
            progressBar.setValue(i.value);
        });
    }
}
