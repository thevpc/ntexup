package net.thevpc.ntexup.engine.renderer.screen.search;

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
 * Frameless, non-modal Find dialog (styled like {@code PresentationHud}):
 * borderless search input, query chips (color swatch + ✕), and a live
 * filtered result list.
 * <ul>
 *     <li>typing debounces ~100ms → re-search + repaint canvas highlights;</li>
 *     <li>↑/↓ move the selection; Enter jumps and closes (the live query is
 *     committed as a chip first);</li>
 *     <li>Ctrl+Enter commits the chip without jumping;</li>
 *     <li>ESC / focus loss closes but <b>keeps</b> the highlights; canvas
 *     highlights are only cleared by ESC when the dialog is already
 *     closed (handled by {@link DocumentView});</li>
 *     <li>reopening shows the chips, an empty input and focus in the
 *     input.</li>
 * </ul>
 */
public class SlideSearchDialog extends JDialog {

    private static final int VISIBLE_ROWS = 6;
    private static final int CELL_H = 62;
    private static final long DEACTIVATION_GUARD_MS = 300;

    private final DocumentView documentView;
    private final SlideHighlightManager manager;
    private final NTxSlideSearcher searcher = new NTxSlideSearcher();

    private final JTextField input = new JTextField() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(getFont());
                g2.setColor(new Color(148, 163, 184));
                FontMetrics fm = g2.getFontMetrics();
                int baseline = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString("Search slides, text, equations...", 1, baseline);
                g2.dispose();
            }
        }
    };
    private final JButton clearBtn = createFlatButton("✕", "Clear");
    private final JPanel chipsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
    private final DefaultListModel<NTxSlideSearcher.SlideSearchResult> listModel = new DefaultListModel<>();
    private final JList<NTxSlideSearcher.SlideSearchResult> resultList = new JList<>(listModel);
    private final SlideSearchResultRenderer resultRenderer = new SlideSearchResultRenderer();
    private final JScrollPane scrollPane;
    private final JLabel emptyLabel = new JLabel("Type to search slides…", SwingConstants.CENTER);
    private final CardLayout resultsCard = new CardLayout();
    private final JPanel resultsPanel;
    private final JPanel northPanel;
    private final javax.swing.Timer debounceTimer;
    private long shownAt;
    private boolean closing;

    public SlideSearchDialog(JFrame owner, DocumentView documentView) {
        super(owner, false);
        this.documentView = documentView;
        this.manager = documentView.highlightManager();

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        JPanel glass = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 23, 42, 235));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(255, 255, 255, 38));
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
        inputRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel icon = new JLabel("🔍");
        icon.setForeground(Color.WHITE);
        inputRow.add(icon, BorderLayout.WEST);

        input.setOpaque(false);
        input.setForeground(Color.WHITE);
        input.setCaretColor(Color.WHITE);
        input.setSelectionColor(new Color(124, 58, 237, 140));
        input.setFont(new Font("SansSerif", Font.PLAIN, 15));
        input.setBorder(new EmptyBorder(4, 2, 4, 2));
        inputRow.add(input, BorderLayout.CENTER);

        clearBtn.setVisible(false);
        inputRow.add(clearBtn, BorderLayout.EAST);

        // ---- chips ----
        chipsPanel.setOpaque(false);

        northPanel = new JPanel();
        northPanel.setOpaque(false);
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        inputRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        chipsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        northPanel.add(inputRow);
        northPanel.add(Box.createVerticalStrut(8));
        northPanel.add(chipsPanel);

        // ---- results ----
        resultList.setCellRenderer(resultRenderer);
        resultList.setFocusable(false);
        resultList.setOpaque(false);
        resultList.setBackground(new Color(0, 0, 0, 0));
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setFixedCellHeight(CELL_H);
        resultList.setBorder(null);
        resultList.setFont(new Font("SansSerif", Font.PLAIN, 13));

        scrollPane = new JScrollPane(resultList);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        emptyLabel.setForeground(new Color(148, 163, 184));
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

        resultList.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                resultRenderer.setHoverIndex(resultList.locationToIndex(e.getPoint()));
            }
        });
        resultList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                resultRenderer.setHoverIndex(-1);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                int idx = resultList.locationToIndex(e.getPoint());
                if (idx < 0) {
                    return;
                }
                resultList.setSelectedIndex(idx);
                jumpToSelected();
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
                    case KeyEvent.VK_ESCAPE:
                        closeDialog();
                        e.consume();
                        break;
                    case KeyEvent.VK_ENTER:
                        handleEnter(e.isControlDown() || e.isMetaDown());
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
                onInputChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onInputChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onInputChanged();
            }
        });

        debounceTimer = new javax.swing.Timer(100, e -> refresh());
        debounceTimer.setRepeats(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeDialog();
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                // clicking outside (on the deck) dismisses the dialog but
                // keeps the highlights; guard the first moments after
                // opening so activation itself does not close us
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
     * Shows the dialog (or re-focuses it when already open): chips rebuilt,
     * input emptied, first refresh, then focus in the input.
     */
    public void open() {
        if (isShowing()) {
            if (!input.getText().isEmpty()) {
                input.setText("");
            }
            refresh();
            input.requestFocusInWindow();
            return;
        }
        shownAt = System.currentTimeMillis();
        input.setText("");
        rebuildChips();
        refresh();
        layoutDialog();
        setVisible(true);
        SwingUtilities.invokeLater(() -> {
            input.requestFocusInWindow();
            input.selectAll();
        });
    }

    /** the index just finished building while the dialog is open */
    public void onIndexReady() {
        if (isShowing()) {
            refresh();
        }
    }

    /** the document was reloaded: results are stale until the index is rebuilt */
    public void onDocumentReloaded() {
        listModel.clear();
        resultRenderer.setHoverIndex(-1);
        updateEmptyState("Indexing slides…");
        resultsCard.show(resultsPanel, "empty");
        if (isShowing()) {
            layoutDialog();
        }
    }

    /**
     * Closes the dialog, committing the live query as a chip. Highlights are
     * intentionally kept.
     */
    public void closeDialog() {
        if (closing) {
            return;
        }
        closing = true;
        try {
            manager.commitLiveQuery();
            setVisible(false);
            documentView.onFindDialogClosed();
        } finally {
            closing = false;
        }
    }

    // ------------------------------------------------------------------
    // search / refresh
    // ------------------------------------------------------------------

    private void onInputChanged() {
        clearBtn.setVisible(!input.getText().isEmpty());
        debounceTimer.restart();
    }

    /**
     * Live refresh: updates the live query, runs the search (live input wins
     * over chips), rebuilds chips, re-applies canvas highlights and relayouts.
     */
    private void refresh() {
        debounceTimer.stop();
        String text = input.getText().trim();
        manager.setLiveQuery(text);

        List<NTxSlideSearcher.SlideSearchResult> results;
        NTxSlideIndex index = manager.getIndex();
        if (index == null) {
            results = Collections.emptyList();
            updateEmptyState("Indexing slides…");
        } else {
            List<SlideHighlightManager.HighlightQuery> forResults;
            if (!text.isEmpty()) {
                SlideHighlightManager.HighlightQuery live = manager.getLiveQuery();
                forResults = live == null ? Collections.emptyList() : Collections.singletonList(live);
            } else {
                forResults = manager.getQueries();
            }
            results = forResults.isEmpty()
                    ? Collections.emptyList()
                    : searcher.search(index, forResults);
            updateEmptyState(text.isEmpty() && forResults.isEmpty()
                    ? "Type to search slides…"
                    : "No matching slides found");
        }

        listModel.clear();
        for (NTxSlideSearcher.SlideSearchResult r : results) {
            listModel.addElement(r);
        }
        resultRenderer.setHoverIndex(-1);
        if (!results.isEmpty()) {
            resultList.setSelectedIndex(0);
            resultsCard.show(resultsPanel, "list");
        } else {
            resultsCard.show(resultsPanel, "empty");
        }

        rebuildChips();
        manager.applyToDocument();
        if (isShowing()) {
            layoutDialog();
        }
    }

    private void updateEmptyState(String message) {
        emptyLabel.setText(message);
    }

    private void moveSelection(int delta) {
        int size = listModel.size();
        if (size == 0) {
            return;
        }
        int idx = resultList.getSelectedIndex();
        int next = idx < 0 ? 0 : Math.min(size - 1, Math.max(0, idx + delta));
        resultList.setSelectedIndex(next);
        resultList.ensureIndexIsVisible(next);
    }

    private void handleEnter(boolean commitOnly) {
        if (commitOnly) {
            manager.commitLiveQuery();
            input.setText("");
            refresh();
            input.requestFocusInWindow();
            return;
        }
        jumpToSelected();
    }

    private void jumpToSelected() {
        NTxSlideSearcher.SlideSearchResult r = resultList.getSelectedValue();
        if (r == null && listModel.size() > 0) {
            r = listModel.get(0);
        }
        if (r == null) {
            return;
        }
        manager.commitLiveQuery();
        documentView.showPage(r.pageIndex);
        closeDialog();
    }

    // ------------------------------------------------------------------
    // chips
    // ------------------------------------------------------------------

    private void rebuildChips() {
        chipsPanel.removeAll();
        for (SlideHighlightManager.HighlightQuery q : manager.getQueries()) {
            chipsPanel.add(createChip(q.getTerm(), q.getColor(), () -> {
                manager.removeQuery(q);
                refresh();
            }));
        }
        SlideHighlightManager.HighlightQuery live = manager.getLiveQuery();
        if (live != null) {
            boolean sameAsChip = false;
            for (SlideHighlightManager.HighlightQuery q : manager.getQueries()) {
                if (q.getNormalized().equals(live.getNormalized())) {
                    sameAsChip = true;
                    break;
                }
            }
            if (!sameAsChip) {
                chipsPanel.add(createChip(live.getTerm(), live.getColor(), () -> {
                    input.setText("");
                    refresh();
                }));
            }
        }
        chipsPanel.revalidate();
        chipsPanel.repaint();
    }

    private JPanel createChip(String term, Color color, Runnable onRemove) {
        JPanel chip = new JPanel(new BorderLayout(4, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int r = color.getRed();
                int gr = color.getGreen();
                int b = color.getBlue();
                g2.setColor(new Color(r, gr, b, 70));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(r, gr, b, 190));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setOpaque(false);
        chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel swatch = new JLabel(term);
        swatch.setForeground(new Color(226, 232, 240));
        swatch.setFont(new Font("SansSerif", Font.PLAIN, 12));
        swatch.setBorder(new EmptyBorder(3, 10, 3, 0));

        JButton x = createFlatButton("✕", "Remove search");
        x.addActionListener(e -> onRemove.run());

        chip.add(swatch, BorderLayout.CENTER);
        chip.add(x, BorderLayout.EAST);
        return chip;
    }

    private JButton createFlatButton(String text, String tooltip) {
        JButton b = new JButton(text);
        b.setToolTipText(tooltip);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setForeground(new Color(203, 213, 225));
        b.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        b.setFocusPainted(false);
        // keep the keyboard focus in the find input; a focused ✕ button
        // would swallow subsequent typing
        b.setFocusable(false);
        b.setRequestFocusEnabled(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setForeground(new Color(203, 213, 225));
            }
        });
        return b;
    }

    // ------------------------------------------------------------------
    // layout
    // ------------------------------------------------------------------

    /**
     * Sizes the dialog to its content: width follows the owner frame, height
     * = input + chips + up to {@link #VISIBLE_ROWS} result cards.
     */
    private void layoutDialog() {
        Window owner = getOwner();
        int ownerW = owner != null ? owner.getWidth() : 1200;
        int w = Math.min(780, Math.max(420, ownerW - 80));
        setSize(w, Math.max(getHeight(), 200));

        // give the vertical box its width so FlowLayout wraps the chips
        northPanel.setSize(w - 28, northPanel.getHeight());
        int northH = northPanel.getPreferredSize().height;
        int rows = Math.min(Math.max(listModel.size(), 1), VISIBLE_ROWS);
        int h = northH + rows * CELL_H + 6 + 24;
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
}
