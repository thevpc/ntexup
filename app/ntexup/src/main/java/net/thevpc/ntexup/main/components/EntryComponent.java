package net.thevpc.ntexup.main.components;

import net.thevpc.ntexup.config.NTxProject;
import net.thevpc.ntexup.main.NTxServiceHelper;
import net.thevpc.nuts.io.NPath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class EntryComponent extends JPanel {
    private JList recentFilesComponent;
    private DefaultListModel recentFilesComponentModel;
    private NTxServiceHelper serviceHelper;
    public EntryComponent(NTxServiceHelper serviceHelper) {
        super(new GridBagLayout());
        this.serviceHelper=serviceHelper;
        recentFilesComponentModel = new DefaultListModel();
        recentFilesComponent = new JList(recentFilesComponentModel);
        recentFilesComponent.setCellRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                boolean valid=true;
                Object showObj=value;
                if(value instanceof NTxProject){
                    NTxProject pr = (NTxProject) value;
                    String path = pr.getPath();
                    NPath p = NPath.of(path);
                    valid=p.exists();
                    showObj="<html><b><span style='color:#00ffff'>"+p.name()+"</span></b> (<small>"+path+"</small>), last accessed <b>"+pr.getLastAccess()+"</b>";
                }
                setEnabled(valid);
                super.getListCellRendererComponent(list, showObj, index, isSelected, cellHasFocus);
                return this;
            }
        });
        recentFilesComponent.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    Object selectedValue = recentFilesComponent.getSelectedValue();
                    if(selectedValue instanceof String){
                        serviceHelper.showOpenFile();
                    }else if(selectedValue instanceof NTxProject){
                        serviceHelper.openProject(
                                NPath.of(((NTxProject) selectedValue).getPath())
                        );
                    }
                    reload();
                }
            }
        });

        GridBagConstraints g = new GridBagConstraints();
        g.gridx=0;
        g.gridy=0;
        g.gridheight=2;
        g.weightx=1;
        g.gridwidth=1;
        g.insets=new Insets(10,10,10,10);

        this.add(createButtons(), g);

        g = new GridBagConstraints();
        g.gridx=1;
        g.gridy=0;
        g.fill=GridBagConstraints.BOTH;
        g.weighty=2;
        g.weightx=3;
        g.gridwidth=3;
        g.anchor=GridBagConstraints.NORTH;
        g.insets=new Insets(10,10,10,10);
        this.add(createRecentFiles(), g);

        updateRecentFiles();
    }

    public void reload() {
        this.updateRecentFiles();
    }

    private void updateRecentFiles() {
        DefaultListModel recentFilesComponentModel = new DefaultListModel();
        for (NTxProject p : serviceHelper.config().getRecentProjects()) {
            recentFilesComponentModel.addElement(p);
        }
        SwingUtilities.invokeLater(() -> {
            this.recentFilesComponentModel = recentFilesComponentModel;
            recentFilesComponent.setModel(recentFilesComponentModel);
        });
    }

    private JComponent createButtons(){
        JPanel p=new JPanel(new GridBagLayout());

        JButton newProjectButton = new JButton("New Project...");
        newProjectButton.addActionListener(e->{
            serviceHelper.showNewProject();
            reload();
        });
        {
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.gridy = 0;
            g.fill = GridBagConstraints.BOTH;
            g.weighty = 1;
            g.weightx = 1;
            g.anchor = GridBagConstraints.NORTH;
            g.insets = new Insets(20,20,20,20);
            p.add(newProjectButton,g);
        }
//        JButton newFileButton = new JButton("New File...");
//        newFileButton.addActionListener(e->{
//            serviceHelper.showNewFile();
//            reload();
//        });
//        p.add(newFileButton);

        JButton openExisting = new JButton("Open existing...");
        openExisting.addActionListener(e->{
            serviceHelper.showOpenFile();
            reload();
        });
        {
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.gridy = 1;
            g.fill = GridBagConstraints.BOTH;
            g.weighty = 1;
            g.weightx = 1;
            g.anchor = GridBagConstraints.NORTH;
            g.insets = new Insets(20,20,20,20);

            p.add(openExisting,g);
        }
        return p;
    }

    private JComponent createRecentFiles(){
        JPanel center=new JPanel(new GridBagLayout());

        GridBagConstraints g = new GridBagConstraints();
        g.gridx=0;
        g.gridy=0;
        g.anchor=GridBagConstraints.WEST;
        center.add(new JLabel("Recent Projects..."), g);

        g = new GridBagConstraints();
        g.gridx=0;
        g.gridy=1;
        g.fill=GridBagConstraints.BOTH;
        g.weighty=2;
        g.weightx=2;
        g.anchor=GridBagConstraints.NORTH;
        center.add(new JScrollPane(recentFilesComponent), g);
        return center;
    }
}
