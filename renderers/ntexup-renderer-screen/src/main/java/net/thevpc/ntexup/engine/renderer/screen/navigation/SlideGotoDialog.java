package net.thevpc.ntexup.engine.renderer.screen.navigation;

import net.thevpc.ntexup.engine.renderer.screen.DocumentView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.List;

/**
 * Frameless, non-modal "Go to slide" navigator (styled like
 * {@code SlideSearchDialog}): one borderless input over a live list of slides
 * showing their number and title.
 * <ul>
 *     <li>empty input → the full slide list; the current slide is selected and
 *     scrolled into view;</li>
 *     <li>digits → slide number (exact match first, out-of-range shows a subtle
 *     invalid state, never a modal error);</li>
 *     <li>text → case/diacritic-insensitive title filter;</li>
 *     <li>↑/↓ move the selection while the focus stays in the input; Enter jumps
 *     and closes; ESC closes; PageUp/PageDown scroll the list; double-click
 *     jumps.</li>
 * </ul>
 * The slide outline is resolved statically by {@link NTxSlideTitleIndex} and
 * cached by the viewer; this dialog never resolves titles itself.
 */
public class SlideGotoDialog extends JDialog {

    private static final int VISIBLE_ROWS = 12;
    private static final int CELL_H = 30;
    private static final long DEACTIVATION_GUARD_MS = 300;

    // -- palette (dark, coherent with the Find dialog; to move to the theme) --
    private static final Color GLASS_BG = new Color(15, 23, 42, 235);
    private static final Color GLASS_BORDER = new Color(255, 255, 255, 38);
    private static final Color TEXT_FG = Color.WHITE;
    private static final Color PLACEHOLDER_FG = new Color(148, 163, 184);
    private static final Color ICON_FG = Color.WHITE;
    private static final Color SELECTION_BG = new Color(124, 58, 237, 140);
    private static final Color INVALID_FG = new Color(248, 113, 113);
    private static final Color EMPTY_FG = new Color(148, 163, 184);

    private final DocumentView documentView;

    private final GotoInput input = new GotoInput();
    private final DefaultListModel<NTxSlideTitleIndex.Entry> listModel = new DefaultListModel<>();
    private final JList<NTxSlideTitleIndex.Entry> slideList = new JList<>(listModel);
    private final SlideGotoListRenderer renderer = new SlideGotoListRenderer();
    private final JScrollPane scrollPane;
    private final JLabel emptyLabel = new JLabel("Indexing slides…", SwingConstants.CENTER);
    private final CardLayout resultsCard = new CardLayout();
    private final JPanel resultsPanel;
    private final JPanel northPanel;

    private long shownAt;
    private boolean closing;

    public SlideGotoDialog(JFrame owner, DocumentView documentView) {
        super(owner, false);
        this.documentView = documentView;

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        JPanel glass = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GLASS_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(GLASS_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        glass.setOpaque(false);
        glass.setBorder(new EmptyBorder(12, 14, 12, 14));
        setContentPane(glass);

        // ---- input row ----
        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        JLabel icon = new JLabel("☰");
        icon.setForeground(ICON_FG);
        icon.setFont(new Font("SansSerif", Font.BOLD, 14));
        inputRow.add(icon, BorderLayout.WEST);

        input.setOpaque(false);
        input.setForeground(TEXT_FG);
        input.setCaretColor(TEXT_FG);
        input.setSelectionColor(SELECTION_BG);
        input.setFont(new Font("SansSerif", Font.PLAIN, 15));
        input.setBorder(new EmptyBorder(4, 2, 4, 2));
        inputRow.add(input, BorderLayout.CENTER);

        northPanel = new JPanel();
        northPanel.setOpaque(false);
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        inputRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        northPanel.add(inputRow);

        // ---- results ----
        slideList.setCellRenderer(renderer);
        slideList.setFocusable(false);
        slideList.setOpaque(false);
        slideList.setBackground(new Color(0, 0, 0, 0));
        slideList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        slideList.setFixedCellHeight(CELL_H);
        slideList.setBorder(null);
        slideList.setFont(new Font("SansSerif", Font.PLAIN, 13));

        scrollPane = new JScrollPane(slideList);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(CELL_H);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        emptyLabel.setForeground(EMPTY_FG);
        emptyLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        emptyLabel.setOpaque(false);

        resultsPanel = new JPanel(resultsCard);
        resultsPanel.setOpaque(false);
        resultsPanel.add(scrollPane, "list");
        resultsPanel.add(emptyLabel, "empty");

        glass.add(northPanel, BorderLayout.NORTH);
        glass.add(resultsPanel, BorderLayout.CENTER);

        // drag support for the frameless dialog
        final Point[] offset = new Point[1];
        MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                offset[0] = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (offset[0] == null) {
                    offset[0] = e.getPoint();
                    return;
                }
                Point p = getLocationOnScreen();
                setLocation(p.x + e.getX() - offset[0].x, p.y + e.getY() - offset[0].y);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                offset[0] = null;
            }
        };
        glass.addMouseListener(drag);
        glass.addMouseMotionListener(drag);
        northPanel.addMouseListener(drag);
        northPanel.addMouseMotionListener(drag);

        slideList.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                renderer.setHoverIndex(slideList.locationToIndex(e.getPoint()));
            }
        });
        slideList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                renderer.setHoverIndex(-1);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                int idx = slideList.locationToIndex(e.getPoint());
                if (idx < 0) {
                    return;
                }
                slideList.setSelectedIndex(idx);
                // single click selects; double click navigates
                if (e.getClickCount() >= 2) {
                    jumpToSelected();
                } else {
                    input.requestFocusInWindow();
                }
            }
        });

        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN:
                        moveSelection(1);
                        e.consume();
                        break;
                    case KeyEvent.VK_UP:
                        moveSelection(-1);
                        e.consume();
                        break;
                    case KeyEvent.VK_PAGE_DOWN:
                        scrollList(1);
                        e.consume();
                        break;
                    case KeyEvent.VK_PAGE_UP:
                        scrollList(-1);
                        e.consume();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        closeDialog();
                        e.consume();
                        break;
                    case KeyEvent.VK_ENTER:
                        jumpToSelected();
                        e.consume();
                        break;
                    default:
                        break;
                }
            }
        });

        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeDialog();
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                // clicking outside (on the deck) dismisses the dialog; guard
                // the first moments after opening so activation itself does
                // not close us
                if (closing || !isVisible()) {
                    return;
                }
                if (System.currentTimeMillis() - shownAt < DEACTIVATION_GUARD_MS) {
                    return;
                }
                closeDialog();
            }
        });
    }

    // ------------------------------------------------------------------
    // lifecycle
    // ------------------------------------------------------------------

    /**
     * Shows the dialog (or re-focuses it when already open): the current slide
     * is marked, the full list is shown and the input focused.
     */
    public void open() {
        renderer.setCurrentPageIndex(documentView.getPageIndex());
        if (!isShowing()) {
            shownAt = System.currentTimeMillis();
            input.setText("");
            refresh();
            layoutDialog();
            setVisible(true);
        } else {
            refresh();
        }
        SwingUtilities.invokeLater(() -> {
            input.requestFocusInWindow();
            input.selectAll();
        });
    }

    /** the outline just finished building while the dialog is open */
    public void onIndexReady() {
        if (isShowing()) {
            renderer.setCurrentPageIndex(documentView.getPageIndex());
            refresh();
        }
    }

    /** the document was reloaded: the outline is stale until rebuilt */
    public void onDocumentReloaded() {
        listModel.clear();
        renderer.setHoverIndex(-1);
        updateEmptyState("Indexing slides…");
        resultsCard.show(resultsPanel, "empty");
        if (isShowing()) {
            layoutDialog();
        }
    }

    /** Closes the dialog and restores focus to the viewer. */
    public void closeDialog() {
        if (closing) {
            return;
        }
        closing = true;
        try {
            setVisible(false);
            documentView.onGotoDialogClosed();
        } finally {
            closing = false;
        }
    }

    // ------------------------------------------------------------------
    // filtering / refresh
    // ------------------------------------------------------------------

    private void refresh() {
        String text = input.getText().trim();
        renderer.setQuery(text);

        NTxSlideTitleIndex index = documentView.slideTitleIndex();
        boolean outOfRange = false;
        boolean selectCurrent = false;
        List<NTxSlideTitleIndex.Entry> entries;
        if (index == null) {
            entries = Collections.emptyList();
            updateEmptyState("Indexing slides…");
        } else {
            renderer.setShowSection(index.isGrouped());
            if (text.isEmpty()) {
                entries = index.entries();
                selectCurrent = true;
            } else {
                SlideGotoFilter.Result r = SlideGotoFilter.filter(index.entries(), text);
                entries = r.entries;
                outOfRange = r.outOfRange;
            }
            updateEmptyState(entries.isEmpty()
                    ? (outOfRange ? "No slide number " + text : "No matching slides")
                    : "");
        }
        input.setInvalid(outOfRange);

        listModel.clear();
        for (NTxSlideTitleIndex.Entry e : entries) {
            listModel.addElement(e);
        }
        renderer.setHoverIndex(-1);

        if (!entries.isEmpty()) {
            resultsCard.show(resultsPanel, "list");
            if (selectCurrent) {
                selectCurrentSlide();
            } else {
                slideList.setSelectedIndex(0);
            }
            scrollSelectedIntoView();
        } else {
            slideList.clearSelection();
            resultsCard.show(resultsPanel, "empty");
        }
        layoutDialog();
    }

    private void updateEmptyState(String message) {
        emptyLabel.setText(message);
    }

    private void selectCurrentSlide() {
        int current = documentView.getPageIndex();
        int row = -1;
        for (int i = 0; i < listModel.size(); i++) {
            if (listModel.get(i).pageIndex == current) {
                row = i;
                break;
            }
        }
        slideList.setSelectedIndex(row >= 0 ? row : 0);
    }

    private void moveSelection(int delta) {
        int size = listModel.size();
        if (size == 0) {
            return;
        }
        int idx = slideList.getSelectedIndex();
        int next = idx < 0 ? 0 : Math.min(size - 1, Math.max(0, idx + delta));
        slideList.setSelectedIndex(next);
        scrollSelectedIntoView();
    }

    private void scrollSelectedIntoView() {
        int idx = slideList.getSelectedIndex();
        if (idx >= 0) {
            slideList.ensureIndexIsVisible(idx);
        }
    }

    private void scrollList(int direction) {
        JScrollBar sb = scrollPane.getVerticalScrollBar();
        int page = Math.max(scrollPane.getViewport().getHeight() - CELL_H, CELL_H);
        int next = sb.getValue() + direction * page;
        sb.setValue(Math.max(sb.getMinimum(), Math.min(sb.getMaximum(), next)));
    }

    private void jumpToSelected() {
        NTxSlideTitleIndex.Entry e = slideList.getSelectedValue();
        if (e == null && listModel.size() > 0) {
            e = listModel.get(0);
        }
        if (e == null) {
            return;
        }
        documentView.showPage(e.pageIndex);
        closeDialog();
    }

    // ------------------------------------------------------------------
    // layout
    // ------------------------------------------------------------------

    /**
     * Sizes the dialog to its content: width follows the owner frame, height
     * = input + up to {@link #VISIBLE_ROWS} rows.
     */
    private void layoutDialog() {
        Window owner = getOwner();
        int ownerW = owner != null ? owner.getWidth() : 1200;
        int w = Math.min(560, Math.max(360, ownerW - 120));
        northPanel.setSize(w - 28, northPanel.getHeight());
        int northH = northPanel.getPreferredSize().height;
        int rows = Math.min(Math.max(listModel.size(), 1), VISIBLE_ROWS);
        int h = northH + 8 + rows * CELL_H + 6 + 12;
        setSize(w, h);

        int x;
        int y;
        if (owner != null) {
            Rectangle b = owner.getBounds();
            x = b.x + (b.width - w) / 2;
            y = b.y + (documentView.isFullScreen() ? 40 : 70);
        } else {
            Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
            x = (scr.width - w) / 2;
            y = 60;
        }
        setLocation(x, y);
    }

    // ------------------------------------------------------------------
    // input field: placeholder + subtle invalid underline
    // ------------------------------------------------------------------

    private final class GotoInput extends JTextField {
        private boolean invalid;

        void setInvalid(boolean invalid) {
            if (this.invalid != invalid) {
                this.invalid = invalid;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (getText().isEmpty()) {
                g2.setFont(getFont());
                g2.setColor(PLACEHOLDER_FG);
                FontMetrics fm = g2.getFontMetrics();
                int baseline = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString("Go to slide (number or title)…", 1, baseline);
            }
            if (invalid) {
                g2.setColor(INVALID_FG);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            }
            g2.dispose();
        }
    }
}
