package net.thevpc.ntexup.engine.renderer.screen.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Shared chrome for the viewer's frameless popups (Find, Go to slide).
 * <p>
 * They used to be per-pixel translucent windows ({@code setBackground(alpha 0)}
 * painting a 92% opaque round rect). On X11 — and on Windows without DWM
 * composition — every repaint and every resize of such a window goes through
 * an off-screen alpha buffer that is first cleared, which shows as a visible
 * blink while typing. Here the window is fully opaque and only its outline is
 * rounded with {@link Window#setShape(Shape)} (per-pixel <em>transparency</em>,
 * a cheap binary mask), falling back to a plain opaque rectangle where shaping
 * is unsupported. Swing double-buffers the opaque content, so updates are
 * flicker-free.
 */
public final class FramelessPopupSupport {

    private FramelessPopupSupport() {
    }

    /**
     * Opaque content pane painting {@code background} with a hairline
     * {@code border}; install with {@link JDialog#setContentPane}.
     */
    public static JPanel createContentPane(LayoutManager layout, Color background, Color border, int arc) {
        JPanel p = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(background);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                g2.dispose();
            }
        };
        p.setOpaque(true);
        p.setBackground(background);
        p.setDoubleBuffered(true);
        return p;
    }

    /**
     * Makes {@code dialog} undecorated and opaque, rounding its outline when
     * the platform supports shaped windows. Call before the dialog is shown.
     */
    public static void install(JDialog dialog, Color background, int arc) {
        dialog.setUndecorated(true);
        dialog.setBackground(background);
        boolean shaped = false;
        try {
            GraphicsDevice gd = dialog.getGraphicsConfiguration() != null
                    ? dialog.getGraphicsConfiguration().getDevice()
                    : GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            shaped = gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT);
        } catch (Throwable ignored) {
            // headless or exotic toolkit: keep the plain rectangle
        }
        if (shaped) {
            dialog.addComponentListener(new ComponentAdapter() {
                private Dimension last;

                @Override
                public void componentResized(ComponentEvent e) {
                    Dimension d = dialog.getSize();
                    if (d.equals(last)) {
                        return;
                    }
                    last = d;
                    try {
                        dialog.setShape(new RoundRectangle2D.Double(0, 0, d.width, d.height, arc, arc));
                    } catch (Throwable ignored) {
                        // shaping refused at runtime: stay rectangular
                    }
                }
            });
        }
    }

    /**
     * Applies {@code bounds} only when they differ from the current ones: a
     * no-op {@code setBounds} still makes some window managers re-map/repaint
     * the window.
     */
    public static void setBoundsIfChanged(Window w, Rectangle bounds) {
        if (!w.getBounds().equals(bounds)) {
            w.setBounds(bounds);
        }
    }

    /** repaints only the given list rows (hover changes), not the whole popup */
    public static void repaintRows(JList<?> list, int... rows) {
        for (int r : rows) {
            if (r >= 0 && r < list.getModel().getSize()) {
                Rectangle b = list.getCellBounds(r, r);
                if (b != null) {
                    list.repaint(b);
                }
            }
        }
    }
}
