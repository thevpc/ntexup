package net.thevpc.ntexup.player.swing;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.ntexup.lib.paintstream.PaintStreamDecoder;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * The java window that opens a {@code .ntxp} container.
 *
 * It is the counterpart of the browser player and of {@code ntexup show} in the
 * same way the container is the counterpart of an html export: the archive
 * already holds every decision the engine made, so this window only has to
 * schedule frames and paint them. There is no engine, no compiler and no
 * document here — {@link PaintStreamContainer} hands over operations and
 * {@link PaintStreamDecoder} turns them onto the panel.
 * <p>
 * Navigation follows the screen viewer's keys exactly, because a deck the user
 * already knows how to drive must not need new muscles: the same page, first,
 * last and fullscreen bindings, and Escape leaving fullscreen before it does
 * anything else.
 */
public class SwingNtxpPlayer {

    /**
     * Window size before a page is fitted into it, the same reference the screen
     * viewer opens with.
     */
    private static final Dimension REF_SIZE = new Dimension(1024, 768);
    private static final Color STAGE = new Color(0x2b2b2b);

    private final PaintStreamContainer container;
    private final JFrame frame;
    private final Stage stage;
    private final Timer timer;
    private final PaintStreamDecoder decoder;

    private int page;
    private int sample;
    private boolean fullScreen;
    private Rectangle windowedBounds;

    private SwingNtxpPlayer(PaintStreamContainer container) {
        this.container = container;
        this.decoder = new PaintStreamDecoder(container.images());
        this.frame = new JFrame("ntexup");
        this.stage = new Stage();
        this.frame.setContentPane(stage);
        this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.timer = new Timer((int) container.periodMillis(), e -> tick());
    }

    /**
     * Opens a container and shows it. A descriptor that does not describe a
     * playback archive fails here, before a window appears.
     */
    public static SwingNtxpPlayer open(NPath path) {
        return open(PaintStreamContainer.open(path));
    }

    /**
     * Shows an already opened container, which is how a caller that reads the
     * archive on another thread keeps the window honest.
     */
    public static SwingNtxpPlayer open(PaintStreamContainer container) {
        SwingNtxpPlayer player = new SwingNtxpPlayer(container);
        player.prepare();
        return player;
    }

    public JFrame frame() {
        return frame;
    }

    public int page() {
        return page;
    }

    public void show() {
        frame.setVisible(true);
        stage.requestFocusInWindow();
    }

    /**
     * Releases the archive; the window is on its own afterwards.
     */
    public void dispose() {
        timer.stop();
        frame.dispose();
        container.close();
    }

    private void prepare() {
        frame.setTitle(container.title() == null || container.title().isEmpty()
                ? "ntexup" : "ntexup — " + container.title());
        frame.setSize(REF_SIZE);
        frame.setLocationRelativeTo(null);
        stage.setFocusTraversalKeysEnabled(false);
        installShortcuts();
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });
        showPage(0);
    }

    // ------------------------------------------------------------- navigation

    /**
     * The screen viewer's bindings, installed on the root pane rather than as a
     * key listener so they keep working whatever has focus, exactly as they do
     * there.
     */
    private void installShortcuts() {
        JRootPane root = frame.getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        bind(im, am, "ntexup.first", new Navigate(true, 0),
                KeyStroke.getKeyStroke("HOME"), KeyStroke.getKeyStroke("control UP"),
                KeyStroke.getKeyStroke("control LEFT"));
        bind(im, am, "ntexup.last", new Navigate(true, Integer.MAX_VALUE),
                KeyStroke.getKeyStroke("END"), KeyStroke.getKeyStroke("control DOWN"),
                KeyStroke.getKeyStroke("control RIGHT"), KeyStroke.getKeyStroke("control SPACE"));
        bind(im, am, "ntexup.previous", new Navigate(false, -1),
                KeyStroke.getKeyStroke("PAGE_UP"), KeyStroke.getKeyStroke("UP"),
                KeyStroke.getKeyStroke("LEFT"));
        bind(im, am, "ntexup.next", new Navigate(false, 1),
                KeyStroke.getKeyStroke("PAGE_DOWN"), KeyStroke.getKeyStroke("DOWN"),
                KeyStroke.getKeyStroke("RIGHT"), KeyStroke.getKeyStroke("SPACE"));
        bind(im, am, "ntexup.fullscreen", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                setFullScreen(!fullScreen);
            }
        }, KeyStroke.getKeyStroke("F11"));
        bind(im, am, "ntexup.escape", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (fullScreen) {
                    setFullScreen(false);
                }
            }
        }, KeyStroke.getKeyStroke("ESCAPE"));
    }

    private static void bind(InputMap im, ActionMap am, String name, AbstractAction action,
                             KeyStroke... strokes) {
        for (KeyStroke stroke : strokes) {
            im.put(stroke, name);
        }
        am.put(name, action);
    }

    /**
     * Step or jump. A deck is not a loop: both stop at the ends instead of
     * wrapping, which is what the screen viewer does.
     */
    private class Navigate extends AbstractAction {
        private final boolean absolute;
        private final int target;

        Navigate(boolean absolute, int target) {
            this.absolute = absolute;
            this.target = target;
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            int last = container.pageCount() - 1;
            int to = absolute ? target : page + target;
            showPage(Math.min(Math.max(to, 0), last));
        }
    }

    private void showPage(int index) {
        page = index;
        sample = 0;
        container.frames(index);
        container.prefetch(index);
        if (container.animated() && container.page(index).frames() > 1) {
            timer.restart();
        } else {
            timer.stop();
        }
        stage.repaint();
    }

    /**
     * One period of the virtual clock. The archive already decided when each
     * frame shows, including where a proven cycle wraps, so the player only
     * counts samples; a page that settles simply stops.
     */
    private void tick() {
        if (container.frameIndex(page, sample + 1) == container.frameIndex(page, sample)) {
            //the archive has no later frame for this page: hold this one
            timer.stop();
            return;
        }
        sample++;
        stage.repaint();
    }

    // -------------------------------------------------------------- fullscreen

    /**
     * The screen viewer's own dance: a decorated frame cannot become a full
     * screen window, so it is disposed and rebuilt undecorated, with a maximised
     * window as the fallback on the devices that refuse it.
     */
    public void setFullScreen(boolean on) {
        if (fullScreen == on) {
            return;
        }
        fullScreen = on;
        SwingUtilities.invokeLater(() -> {
            frame.dispose();
            GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice();
            if (fullScreen) {
                windowedBounds = frame.getBounds();
                frame.setUndecorated(true);
                try {
                    if (device.isFullScreenSupported()) {
                        device.setFullScreenWindow(frame);
                    } else {
                        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                        frame.setVisible(true);
                    }
                } catch (Exception ex) {
                    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                    frame.setVisible(true);
                }
            } else {
                if (device.getFullScreenWindow() == frame) {
                    device.setFullScreenWindow(null);
                }
                frame.setUndecorated(false);
                frame.setExtendedState(JFrame.NORMAL);
                if (windowedBounds != null) {
                    frame.setBounds(windowedBounds);
                } else {
                    frame.setSize(REF_SIZE);
                    frame.setLocationRelativeTo(null);
                }
                frame.setVisible(true);
            }
            stage.requestFocusInWindow();
        });
    }

    // ------------------------------------------------------------------ stage

    /**
     * The surface a page is painted on: it letterboxes the recorded geometry into
     * whatever the window is, asks the decoder for the frame at the current
     * sample and shows where in the deck the player stands.
     */
    private class Stage extends JComponent {

        Stage() {
            setOpaque(true);
            setBackground(STAGE);
            setFocusable(true);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            g.setColor(STAGE);
            g.fillRect(0, 0, getWidth(), getHeight());
            List<List<NElement>> frames = container.frames(page);
            if (frames.isEmpty()) {
                return;
            }
            int index = Math.min(container.frameIndex(page, sample), frames.size() - 1);
            double scale = Math.min(getWidth() / (double) container.width(),
                    getHeight() / (double) container.height());
            if (!Double.isFinite(scale) || scale <= 0) {
                return;
            }
            int paintedWidth = (int) Math.round(container.width() * scale);
            int paintedHeight = (int) Math.round(container.height() * scale);
            int x = (getWidth() - paintedWidth) / 2;
            int y = (getHeight() - paintedHeight) / 2;
            //the decoder installs an absolute transform per operation, so the
            //placement has to ride in the device argument rather than on g
            AffineTransform device = new AffineTransform(g.getTransform());
            device.translate(x, y);
            device.scale(scale, scale);
            Graphics2D c = (Graphics2D) g.create();
            try {
                c.clip(new Rectangle(x, y, paintedWidth, paintedHeight));
                decoder.paint(c, frames.get(index), device);
            } finally {
                c.dispose();
            }
            paintCounter(g, x + paintedWidth, y + paintedHeight);
        }

        private void paintCounter(Graphics2D g, int right, int bottom) {
            String text = (page + 1) + " / " + container.pageCount();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.setFont(getFont().deriveFont(13f));
            int width = g.getFontMetrics().stringWidth(text);
            g.drawString(text, right - width - 10, bottom - 10);
        }
    }
}
