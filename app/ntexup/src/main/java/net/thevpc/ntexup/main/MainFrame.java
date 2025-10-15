package net.thevpc.ntexup.main;

import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.main.components.EntryComponent;
import net.thevpc.ntexup.engine.util.NTxUtilsImages;
import net.thevpc.nuts.io.NPath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainFrame extends JFrame {

    private NTxServiceHelper serviceHelper;
    private EntryComponent entryComponent;

    public MainFrame(NTxEngine engine) {
        serviceHelper = new NTxServiceHelper(this,engine==null?new DefaultNTxEngine() :  engine);
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
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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

}
