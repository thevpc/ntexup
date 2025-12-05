package net.thevpc.ntexup.engine.renderer.screen.components;

import javax.swing.*;
import java.awt.*;

public class RatioPanel extends JPanel {
    private double ratio; // width / height
    private Color boundaryColor;
    private final JComponent content;

    public RatioPanel(JComponent content, double ratio, Color boundaryColor) {
        this.ratio = ratio;
        this.boundaryColor = boundaryColor;
        this.content = content;
        setLayout(null);
        add(content);
    }

    public double getRatio() {
        return ratio;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

    public Color getBoundaryColor() {
        return boundaryColor;
    }

    public void setBoundaryColor(Color boundaryColor) {
        this.boundaryColor = boundaryColor;
    }

    @Override
    public Dimension getPreferredSize() {
        // You can return any reasonable non-zero size.
        // No relation to the ratio is required here.
        return new Dimension(400, 300);
    }

    @Override
    public void doLayout() {
        int pw = getWidth();
        int ph = getHeight();

        double panelRatio = (double) pw / ph;

        int cw, ch;

        if (panelRatio > ratio) {
            // too wide: limit by height
            ch = ph;
            cw = (int) (ph * ratio);
        } else {
            // too tall: limit by width
            cw = pw;
            ch = (int) (pw / ratio);
        }

        int x = (pw - cw) / 2;
        int y = (ph - ch) / 2;

        content.setBounds(x, y, cw, ch);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int pw = getWidth();
        int ph = getHeight();

        double panelRatio = (double) pw / ph;

        int cw, ch;

        if (panelRatio > ratio) {
            ch = ph;
            cw = (int) (ph * ratio);
        } else {
            cw = pw;
            ch = (int) (pw / ratio);
        }

        int x = (pw - cw) / 2;
        int y = (ph - ch) / 2;

        // Paint boundaries (letterboxing/pillarboxing)
        g.setColor(boundaryColor);

        // Left
        if (x > 0) g.fillRect(0, 0, x, ph);
        // Right
        if (x + cw < pw) g.fillRect(x + cw, 0, pw - (x + cw), ph);
        // Top
        if (y > 0) g.fillRect(x, 0, cw, y);
        // Bottom
        if (y + ch < ph) g.fillRect(x, y + ch, cw, ph - (y + ch));
    }
}