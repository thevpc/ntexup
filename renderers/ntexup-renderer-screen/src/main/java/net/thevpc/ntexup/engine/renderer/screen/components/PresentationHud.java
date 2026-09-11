package net.thevpc.ntexup.engine.renderer.screen.components;

import net.thevpc.ntexup.engine.renderer.screen.DocumentView;
import net.thevpc.ntexup.engine.renderer.screen.PdfConfigDialog;
import net.thevpc.nuts.util.NLiteral;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PresentationHud extends JPanel {
    private final DocumentView documentView;
    private final JLabel pageLabel = new JLabel("1 / 1");
    private final JButton prevBtn = createHudButton("◀", "Previous Slide (Left / Up / PageUp)");
    private final JButton nextBtn = createHudButton("▶", "Next Slide (Right / Down / Space / PageDown)");
    private final JButton fullscreenBtn = createHudButton("⛶", "Fullscreen (F11)");
    private final JButton pdfBtn = createHudButton("PDF", "Export to PDF");
    private final Timer autoHideTimer;
    private boolean mouseInside = false;

    public PresentationHud(DocumentView documentView) {
        this.documentView = documentView;
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.CENTER, 8, 4));
        setBorder(new EmptyBorder(4, 12, 4, 12));

        prevBtn.addActionListener(e -> {
            documentView.previousPage();
            ping();
        });
        nextBtn.addActionListener(e -> {
            documentView.nextPage();
            ping();
        });
        fullscreenBtn.addActionListener(e -> {
            documentView.toggleFullScreen();
            ping();
        });
        pdfBtn.addActionListener(e -> {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            PdfConfigDialog configDialog = new PdfConfigDialog(owner, documentView);
            configDialog.setVisible(true);
        });

        pageLabel.setForeground(Color.WHITE);
        pageLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        pageLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pageLabel.setToolTipText("Click to jump to page");
        pageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String input = JOptionPane.showInputDialog(documentView.getFrame(), "Go to page number:", documentView.getPageUserIndex());
                if (input != null && !input.trim().isEmpty()) {
                    int p = NLiteral.of(input.trim()).asInt().orElse(-1);
                    if (p > 0) {
                        documentView.showPage(p - 1);
                    }
                }
            }
        });

        add(prevBtn);
        add(pageLabel);
        add(nextBtn);
        add(createSeparator());
        add(fullscreenBtn);
        add(pdfBtn);

        autoHideTimer = new Timer(2500, e -> {
            if (!mouseInside) {
                setVisible(false);
            }
        });
        autoHideTimer.setRepeats(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mouseInside = true;
                autoHideTimer.stop();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseInside = false;
                autoHideTimer.restart();
            }
        });

        ping();
    }

    private JComponent createSeparator() {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 16));
        sep.setForeground(new Color(255, 255, 255, 60));
        return sep;
    }

    private JButton createHudButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setToolTipText(tooltip);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(255, 255, 255, 20));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 40), 1, true),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setOpaque(true);
                btn.setBackground(new Color(255, 255, 255, 45));
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setOpaque(false);
                btn.setBackground(new Color(255, 255, 255, 20));
                btn.repaint();
            }
        });
        return btn;
    }

    public void ping() {
        setVisible(true);
        autoHideTimer.restart();
    }

    public void updateState(int currentPage, int totalPages, boolean isFullScreen) {
        pageLabel.setText(currentPage + " / " + Math.max(totalPages, 1));
        fullscreenBtn.setText(isFullScreen ? "✕" : "⛶");
        fullscreenBtn.setToolTipText(isFullScreen ? "Exit Fullscreen (F11 / Esc)" : "Fullscreen (F11)");
        prevBtn.setEnabled(currentPage > 1);
        nextBtn.setEnabled(currentPage < totalPages);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Translucent background
        g2.setColor(new Color(15, 23, 42, 215)); // Slate 900 with ~85% alpha
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);

        // Subtle border
        g2.setColor(new Color(255, 255, 255, 40));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);

        g2.dispose();
        super.paintComponent(g);
    }
}
