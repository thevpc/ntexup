package net.thevpc.ntexup.main;

import java.awt.*;

public class NTxUIHelper {
    public static GridBagConstraints forEditor(int x, int y) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.weightx = 2;
        g.fill = GridBagConstraints.BOTH;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(2,2,2,2);
        return g;
    }

    public static GridBagConstraints forLabel(int x, int y) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(2,2,2,2);
        return g;
    }
}
