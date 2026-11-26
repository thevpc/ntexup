package net.thevpc.ntexup.engine.renderer.screen.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Thin, arrow-less scroll bar for the viewer's dark side panels: transparent
 * track, rounded translucent thumb that brightens on hover / drag.
 * Install on a scroll pane with {@link #install(JScrollPane, Color)}.
 */
public class SlimScrollBarUI extends BasicScrollBarUI {

    private static final int THICKNESS = 10;
    private static final int THUMB_INSET = 2;
    private final Color trackColor;
    private boolean hover;

    public SlimScrollBarUI(Color trackColor) {
        this.trackColor = trackColor;
    }

    /** styles both bars of {@code scroll} and paints the corner in {@code background} */
    public static void install(JScrollPane scroll, Color background) {
        for (JScrollBar bar : new JScrollBar[]{scroll.getVerticalScrollBar(), scroll.getHorizontalScrollBar()}) {
            bar.setUI(new SlimScrollBarUI(background));
            bar.setOpaque(true);
            bar.setBackground(background);
            bar.setUnitIncrement(16);
            if (bar.getOrientation() == JScrollBar.VERTICAL) {
                bar.setPreferredSize(new Dimension(THICKNESS, 0));
            } else {
                bar.setPreferredSize(new Dimension(0, THICKNESS));
            }
        }
        JPanel corner = new JPanel();
        corner.setBackground(background);
        scroll.setCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER, corner);
        scroll.setCorner(ScrollPaneConstants.LOWER_LEFT_CORNER, corner);
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        scrollbar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                scrollbar.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                scrollbar.repaint();
            }
        });
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return zeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return zeroButton();
    }

    private static JButton zeroButton() {
        JButton b = new JButton();
        Dimension z = new Dimension(0, 0);
        b.setPreferredSize(z);
        b.setMinimumSize(z);
        b.setMaximumSize(z);
        b.setFocusable(false);
        return b;
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return new Dimension(24, 24);
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        g.setColor(trackColor);
        g.fillRect(r.x, r.y, r.width, r.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int alpha = isDragging ? 150 : (hover ? 110 : 60);
        g2.setColor(new Color(255, 255, 255, alpha));
        int x = r.x + THUMB_INSET;
        int y = r.y + THUMB_INSET;
        int w = r.width - 2 * THUMB_INSET;
        int h = r.height - 2 * THUMB_INSET;
        int arc = Math.min(w, h);
        g2.fillRoundRect(x, y, w, h, arc, arc);
        g2.dispose();
    }

    @Override
    protected void setThumbBounds(int x, int y, int width, int height) {
        super.setThumbBounds(x, y, width, height);
        scrollbar.repaint();
    }
}
