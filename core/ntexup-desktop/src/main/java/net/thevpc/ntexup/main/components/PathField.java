package net.thevpc.ntexup.main.components;

import net.thevpc.ntexup.main.NTxServiceHelper;
import net.thevpc.nuts.util.NBlankable;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class PathField extends JPanel {
    JTextField path;
    JButton button;

    public PathField() {
        super(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.weightx = 2;
        g.fill = GridBagConstraints.BOTH;
        g.anchor = GridBagConstraints.WEST;
        add(path = new JTextField(), g);
        g = new GridBagConstraints();
        g.gridx = 1;
        g.gridy = 0;
        g.anchor = GridBagConstraints.WEST;
        add(button = new JButton("..."), g);
        button.addActionListener(e -> onSelectPath());
    }

    private void onSelectPath() {
        String oldPath = path.getText();
        JFileChooser f = new JFileChooser();
        f.setFileFilter(NTxServiceHelper.NTEXUP_FILTER);
        f.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (!NBlankable.isBlank(oldPath)) {
            f.setCurrentDirectory(new File(oldPath));
        }
        int r = f.showSaveDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            File sf = f.getSelectedFile();
            path.setText(sf.toString());
        }
    }
}
