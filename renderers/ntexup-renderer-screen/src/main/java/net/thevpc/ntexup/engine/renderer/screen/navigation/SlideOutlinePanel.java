package net.thevpc.ntexup.engine.renderer.screen.navigation;

import net.thevpc.ntexup.engine.renderer.screen.DocumentView;
import net.thevpc.ntexup.engine.renderer.screen.components.SlimScrollBarUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Outline explorer (Ctrl+Shift+O): a collapsible side panel docked left of
 * the slide, showing chapters/sections → slide titles (with subtitle/kicker
 * when the slide metadata provides one).
 * <p>
 * Content comes from the viewer's cached {@link NTxSlideTitleIndex} (titles
 * are never re-resolved here), grouped by {@link SlideOutlineModel}. The tree
 * model is rebuilt only when a new index arrives (document load/reload) or the
 * filter text changes; page changes just move the selection and expand the
 * current section, so there is no relayout per slide. A click (or Enter)
 * navigates, then keyboard focus goes back to the deck.
 * <p>
 * The right edge is a resize handle: dragging shows a guide line and applies
 * the width on release (the slide is re-laid out once, not per mouse move);
 * double-click fits the width to the longest title.
 */
public class SlideOutlinePanel extends JPanel {

    private static final Color BG = new Color(24, 27, 33);
    private static final Color FIELD_BG = new Color(36, 40, 48);
    private static final Color FG = new Color(226, 230, 236);
    private static final Color FG_DIM = new Color(140, 148, 160);
    private static final Color SECTION_FG = new Color(160, 196, 255);
    private static final Color SELECTION_BG = new Color(52, 84, 140);
    private static final Color BORDER = new Color(255, 255, 255, 30);
    private static final Color GUIDE = new Color(120, 160, 230);
    private static final int DEFAULT_WIDTH = 270;
    private static final int MIN_WIDTH = 170;
    private static final int HANDLE_WIDTH = 6;

    private final DocumentView documentView;
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("outline");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    private final JTree tree = new JTree(treeModel);
    private final JTextField filterField = new JTextField();
    private final JButton clearFilterBtn = new JButton("✕");
    private final JLabel statusLabel = new JLabel("Loading outline…");
    private final JScrollPane scroll;
    private final Map<Integer, DefaultMutableTreeNode> slideNodes = new HashMap<>();
    private final Timer filterTimer;
    private NTxSlideTitleIndex index;
    /** full outline of the current index */
    private SlideOutlineModel model;
    /** what the tree shows: {@link #model} or a filtered view of it */
    private SlideOutlineModel view;
    private String appliedFilter = "";
    private int currentPage = -1;
    private boolean adjusting;

    public SlideOutlinePanel(DocumentView documentView) {
        super(new BorderLayout());
        this.documentView = documentView;
        setBackground(BG);
        setPreferredSize(new Dimension(DEFAULT_WIDTH, 100));

        // ---- header + filter ----
        JLabel header = new JLabel("Outline");
        header.setForeground(FG);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
        JButton close = flatButton("✕", "Hide outline (Ctrl+Shift+O)");
        close.addActionListener(e -> documentView.setOutlineVisible(false));
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(header, BorderLayout.CENTER);
        titleRow.add(close, BorderLayout.EAST);

        filterField.setBackground(FIELD_BG);
        filterField.setForeground(FG);
        filterField.setCaretColor(FG);
        filterField.setSelectionColor(SELECTION_BG);
        filterField.setSelectedTextColor(Color.WHITE);
        filterField.setBorder(new EmptyBorder(5, 8, 5, 4));
        filterField.setToolTipText("Filter by title, subtitle, section, file or slide number");
        filterField.putClientProperty("JTextField.placeholderText", "Filter slides…");
        clearFilterBtn.setVisible(false);
        clearFilterBtn.setToolTipText("Clear filter (Esc)");
        styleFlat(clearFilterBtn);
        clearFilterBtn.addActionListener(e -> {
            filterField.setText("");
            filterField.requestFocusInWindow();
        });
        JPanel fieldBox = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(filterField.isFocusOwner() ? GUIDE : BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        fieldBox.setOpaque(false);
        fieldBox.setBorder(new EmptyBorder(1, 2, 1, 2));
        fieldBox.add(new PlaceholderLayer(filterField, "Filter slides…"), BorderLayout.CENTER);
        fieldBox.add(clearFilterBtn, BorderLayout.EAST);
        filterField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                fieldBox.repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                fieldBox.repaint();
            }
        });

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(8, 10, 8, 6));
        top.add(titleRow, BorderLayout.NORTH);
        top.add(fieldBox, BorderLayout.CENTER);

        filterTimer = new Timer(120, e -> applyFilter());
        filterTimer.setRepeats(false);
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterChanged();
            }
        });
        installFilterKeys();

        // ---- tree ----
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setBackground(BG);
        tree.setRowHeight(0);
        tree.setToggleClickCount(0);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new OutlineRenderer());
        ToolTipManager.sharedInstance().registerComponent(tree);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                TreePath p = tree.getPathForLocation(e.getX(), e.getY());
                if (p == null) {
                    return;
                }
                Object u = ((DefaultMutableTreeNode) p.getLastPathComponent()).getUserObject();
                if (u instanceof NTxSlideTitleIndex.Entry) {
                    navigateToPage(((NTxSlideTitleIndex.Entry) u).pageIndex);
                } else if (u instanceof SlideOutlineModel.Group) {
                    if (tree.isExpanded(p)) {
                        tree.collapsePath(p);
                    } else {
                        tree.expandPath(p);
                    }
                    if (e.getClickCount() >= 2) {
                        navigateToPage(((SlideOutlineModel.Group) u).firstPageIndex());
                    }
                }
            }
        });
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "ntexup.outline.open");
        tree.getActionMap().put("ntexup.outline.open", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSelection();
            }
        });
        // typing in the tree goes to the filter (instead of JTree type-ahead)
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (c != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(c) && c != ' '
                        && (e.getModifiersEx() & (KeyEvent.CTRL_DOWN_MASK | KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) == 0) {
                    e.consume();
                    filterField.requestFocusInWindow();
                    filterField.replaceSelection(String.valueOf(c));
                }
            }
        });

        scroll = new JScrollPane(tree);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG);
        SlimScrollBarUI.install(scroll, BG);

        statusLabel.setForeground(FG_DIM);
        statusLabel.setBorder(new EmptyBorder(4, 10, 6, 10));
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(scroll, BorderLayout.CENTER);
        body.add(statusLabel, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);
        add(new ResizeHandle(), BorderLayout.EAST);
    }

    // ------------------------------------------------------------------
    // content
    // ------------------------------------------------------------------

    /** the index currently displayed (identity is used to skip rebuilds) */
    public NTxSlideTitleIndex index() {
        return index;
    }

    /**
     * Replaces the outline content. A null index keeps the previous tree (so a
     * reload does not blank the panel) and only updates the status line. An
     * active filter is re-applied to the new outline.
     */
    public void setIndex(NTxSlideTitleIndex newIndex) {
        if (newIndex == null) {
            statusLabel.setText("Loading outline…");
            return;
        }
        if (newIndex == index) {
            return;
        }
        this.index = newIndex;
        this.model = SlideOutlineModel.of(newIndex);
        appliedFilter = filterText();
        rebuildTree(model.filter(appliedFilter));
    }

    private String filterText() {
        String t = filterField.getText();
        return t == null ? "" : t.trim();
    }

    private void filterChanged() {
        clearFilterBtn.setVisible(!filterField.getText().isEmpty());
        filterTimer.restart();
    }

    private void applyFilter() {
        filterTimer.stop();
        String q = filterText();
        if (model == null || q.equals(appliedFilter)) {
            return;
        }
        appliedFilter = q;
        rebuildTree(model.filter(q));
    }

    private void rebuildTree(SlideOutlineModel v) {
        this.view = v;
        boolean filtered = !appliedFilter.isEmpty();
        adjusting = true;
        try {
            root.removeAllChildren();
            slideNodes.clear();
            for (SlideOutlineModel.Group g : v.groups()) {
                DefaultMutableTreeNode parent = root;
                if (g.name != null) {
                    parent = new DefaultMutableTreeNode(g, true);
                    root.add(parent);
                }
                for (NTxSlideTitleIndex.Entry e : g.entries) {
                    DefaultMutableTreeNode n = new DefaultMutableTreeNode(e, false);
                    parent.add(n);
                    slideNodes.put(e.pageIndex, n);
                }
            }
            treeModel.reload();
            if (filtered) {
                // show every match
                for (int i = 0; i < root.getChildCount(); i++) {
                    tree.expandPath(new TreePath(((DefaultMutableTreeNode) root.getChildAt(i)).getPath()));
                }
            }
        } finally {
            adjusting = false;
        }
        updateStatus();
        int p = currentPage;
        currentPage = -1;
        setCurrentPage(p);
        if (filtered && tree.getSelectionPath() == null && tree.getRowCount() > 0) {
            tree.scrollRowToVisible(0);
        }
    }

    private void updateStatus() {
        int total = model == null ? 0 : model.slideCount();
        int shown = view == null ? 0 : view.slideCount();
        int sections = 0;
        if (view != null) {
            for (SlideOutlineModel.Group g : view.groups()) {
                if (g.name != null) {
                    sections++;
                }
            }
        }
        String s;
        if (!appliedFilter.isEmpty()) {
            s = shown == 0 ? "No matching slide" : shown + " of " + total + " slides";
        } else {
            s = total + (total == 1 ? " slide" : " slides");
        }
        if (sections > 0) {
            s += " · " + sections + (sections == 1 ? " section" : " sections");
        }
        statusLabel.setText(s);
    }

    /**
     * Highlights {@code pageIndex}: selects its row, expands its section and
     * scrolls it into view. No model change, no relayout of the frame. A slide
     * hidden by the filter just leaves the tree unselected.
     */
    public void setCurrentPage(int pageIndex) {
        if (pageIndex == currentPage && tree.getSelectionPath() != null) {
            return;
        }
        currentPage = pageIndex;
        DefaultMutableTreeNode n = slideNodes.get(pageIndex);
        if (n == null) {
            adjusting = true;
            try {
                tree.clearSelection();
            } finally {
                adjusting = false;
            }
            return;
        }
        TreePath path = new TreePath(n.getPath());
        adjusting = true;
        try {
            if (path.getParentPath() != null) {
                tree.expandPath(path.getParentPath());
            }
            tree.setSelectionPath(path);
            Rectangle r = tree.getPathBounds(path);
            if (r != null) {
                // keep horizontal position: only scroll vertically
                r.x = scroll.getViewport().getViewPosition().x;
                r.width = 1;
                tree.scrollRectToVisible(r);
            }
        } finally {
            adjusting = false;
        }
    }

    public int currentPage() {
        return currentPage;
    }

    // ------------------------------------------------------------------
    // focus / keys
    // ------------------------------------------------------------------

    /** gives keyboard focus to the filter field */
    public void focusFilter() {
        filterField.requestFocusInWindow();
        filterField.selectAll();
    }

    /** gives keyboard focus to the tree (for arrow-key browsing) */
    public void focusTree() {
        tree.requestFocusInWindow();
    }

    /** true when the tree or the filter field owns the keyboard */
    public boolean isTreeFocused() {
        return tree.isFocusOwner() || filterField.isFocusOwner();
    }

    private void installFilterKeys() {
        InputMap im = filterField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = filterField.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ntexup.outline.filter.escape");
        am.put("ntexup.outline.filter.escape", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!filterField.getText().isEmpty()) {
                    filterField.setText("");
                } else {
                    documentView.requestContentPaneFocus();
                }
            }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "ntexup.outline.filter.enter");
        am.put("ntexup.outline.filter.enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyFilter();
                if (tree.getSelectionPath() == null) {
                    selectFirstSlide();
                }
                openSelection();
            }
        });
        AbstractAction toTree = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyFilter();
                if (tree.getSelectionPath() == null) {
                    selectFirstSlide();
                }
                tree.requestFocusInWindow();
            }
        };
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "ntexup.outline.filter.down");
        am.put("ntexup.outline.filter.down", toTree);
        // keep deck navigation keys from firing while typing in the filter
        AbstractAction swallow = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        };
        am.put("ntexup.outline.filter.swallow", swallow);
        for (int k : new int[]{KeyEvent.VK_UP, KeyEvent.VK_PAGE_UP, KeyEvent.VK_PAGE_DOWN, KeyEvent.VK_SPACE}) {
            im.put(KeyStroke.getKeyStroke(k, 0), "ntexup.outline.filter.swallow");
        }
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_DOWN_MASK), "ntexup.outline.filter.swallow");
    }

    private void selectFirstSlide() {
        for (int r = 0; r < tree.getRowCount(); r++) {
            TreePath p = tree.getPathForRow(r);
            Object u = ((DefaultMutableTreeNode) p.getLastPathComponent()).getUserObject();
            if (u instanceof NTxSlideTitleIndex.Entry) {
                adjusting = true;
                try {
                    tree.setSelectionPath(p);
                    tree.scrollRowToVisible(r);
                } finally {
                    adjusting = false;
                }
                return;
            }
        }
    }

    private void openSelection() {
        TreePath p = tree.getSelectionPath();
        if (p == null) {
            return;
        }
        Object u = ((DefaultMutableTreeNode) p.getLastPathComponent()).getUserObject();
        if (u instanceof NTxSlideTitleIndex.Entry) {
            navigateToPage(((NTxSlideTitleIndex.Entry) u).pageIndex);
        } else if (u instanceof SlideOutlineModel.Group) {
            navigateToPage(((SlideOutlineModel.Group) u).firstPageIndex());
        }
    }

    private void navigateToPage(int pageIndex) {
        if (adjusting || pageIndex < 0) {
            return;
        }
        documentView.showPage(pageIndex);
        documentView.requestContentPaneFocus();
    }

    // ------------------------------------------------------------------
    // width
    // ------------------------------------------------------------------

    private int maxWidth() {
        Container p = getParent();
        int w = p == null ? 1200 : p.getWidth();
        return Math.max(MIN_WIDTH, (int) (w * 0.7));
    }

    private void applyWidth(int w) {
        w = Math.max(MIN_WIDTH, Math.min(maxWidth(), w));
        if (w == getPreferredSize().width) {
            return;
        }
        setPreferredSize(new Dimension(w, getPreferredSize().height));
        Container p = getParent();
        if (p instanceof JComponent) {
            ((JComponent) p).revalidate();
            p.repaint();
        }
    }

    /** width that shows the longest row without horizontal scrolling */
    private int fitWidth() {
        int content = tree.getPreferredSize().width;
        Insets in = scroll.getInsets();
        return content + in.left + in.right + scroll.getVerticalScrollBar().getPreferredSize().width + HANDLE_WIDTH + 8;
    }

    /**
     * Right-edge drag handle. While dragging only a guide line (in the
     * layered pane's drag layer) moves; the panel width is applied on release,
     * so the slide is re-laid out once.
     */
    private final class ResizeHandle extends JComponent {
        private JComponent guide;
        private int startScreenX;
        private int startWidth;

        ResizeHandle() {
            setPreferredSize(new Dimension(HANDLE_WIDTH, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            setToolTipText("Drag to resize · double-click to fit titles");
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e)) {
                        return;
                    }
                    startScreenX = e.getXOnScreen();
                    startWidth = SlideOutlinePanel.this.getWidth();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    showGuide(targetWidth(e));
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (guide != null) {
                        int w = targetWidth(e);
                        hideGuide();
                        applyWidth(w);
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        int fit = Math.min(maxWidth(), fitWidth());
                        int cur = SlideOutlinePanel.this.getWidth();
                        applyWidth(Math.abs(cur - fit) <= 2 ? DEFAULT_WIDTH : fit);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    repaint();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private int targetWidth(MouseEvent e) {
            int w = startWidth + (e.getXOnScreen() - startScreenX);
            return Math.max(MIN_WIDTH, Math.min(maxWidth(), w));
        }

        private void showGuide(int width) {
            JRootPane rp = SwingUtilities.getRootPane(SlideOutlinePanel.this);
            if (rp == null) {
                return;
            }
            JLayeredPane lp = rp.getLayeredPane();
            if (guide == null) {
                guide = new JComponent() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        g.setColor(GUIDE);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                };
                lp.add(guide, JLayeredPane.DRAG_LAYER);
            }
            Point origin = SwingUtilities.convertPoint(SlideOutlinePanel.this, 0, 0, lp);
            guide.setBounds(origin.x + width - 2, origin.y, 2, SlideOutlinePanel.this.getHeight());
            guide.repaint();
        }

        private void hideGuide() {
            if (guide != null) {
                Container p = guide.getParent();
                if (p != null) {
                    Rectangle b = guide.getBounds();
                    p.remove(guide);
                    p.repaint(b.x, b.y, b.width, b.height);
                }
                guide = null;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(BG);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(getMousePosition() != null ? GUIDE : BORDER);
            g.fillRect(getWidth() - 1, 0, 1, getHeight());
        }
    }

    // ------------------------------------------------------------------
    // rendering helpers
    // ------------------------------------------------------------------

    private static JButton flatButton(String text, String tip) {
        JButton b = new JButton(text);
        b.setToolTipText(tip);
        styleFlat(b);
        return b;
    }

    private static void styleFlat(JButton b) {
        b.setForeground(FG_DIM);
        b.setBorder(new EmptyBorder(2, 6, 2, 6));
        b.setContentAreaFilled(false);
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** text field wrapper painting a grey hint while the field is empty */
    private static final class PlaceholderLayer extends JPanel {
        private final JTextField field;
        private final String hint;

        PlaceholderLayer(JTextField field, String hint) {
            super(new BorderLayout());
            this.field = field;
            this.hint = hint;
            setOpaque(false);
            field.setOpaque(false);
            add(field, BorderLayout.CENTER);
            field.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    repaint();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    repaint();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);
            if (field.getText().isEmpty()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(FG_DIM);
                g2.setFont(field.getFont());
                Insets in = field.getInsets();
                FontMetrics fm = g2.getFontMetrics();
                int y = field.getY() + in.top + (field.getHeight() - in.top - in.bottom - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(hint, field.getX() + in.left + 2, y);
                g2.dispose();
            }
        }
    }

    private final class OutlineRenderer extends DefaultTreeCellRenderer {
        OutlineRenderer() {
            setBackgroundNonSelectionColor(BG);
            setBackgroundSelectionColor(SELECTION_BG);
            setBorderSelectionColor(SELECTION_BG);
            setTextNonSelectionColor(FG);
            setTextSelectionColor(Color.WHITE);
            setLeafIcon(null);
            setOpenIcon(null);
            setClosedIcon(null);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree t, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(t, value, sel, expanded, leaf, row, false);
            Object u = ((DefaultMutableTreeNode) value).getUserObject();
            setToolTipText(null);
            setBorder(new EmptyBorder(3, 2, 3, 6));
            if (u instanceof SlideOutlineModel.Group) {
                SlideOutlineModel.Group g = (SlideOutlineModel.Group) u;
                setFont(t.getFont().deriveFont(Font.BOLD));
                setForeground(sel ? Color.WHITE : SECTION_FG);
                setText("<html>" + escape(g.name) + "&nbsp;&nbsp;<font color='#8c94a0'>" + g.entries.size() + "</font></html>");
            } else if (u instanceof NTxSlideTitleIndex.Entry) {
                NTxSlideTitleIndex.Entry e = (NTxSlideTitleIndex.Entry) u;
                // same font for every slide row: BasicTreeUI caches row sizes,
                // a bold "current" row would get truncated without a relayout
                setFont(t.getFont().deriveFont(Font.PLAIN));
                String dim = sel ? "#c8d2e6" : "#8c94a0";
                StringBuilder sb = new StringBuilder("<html><nobr><font color='")
                        .append(dim).append("'>")
                        .append(e.userIndex()).append("</font>&nbsp;&nbsp;")
                        .append(escape(e.title)).append("</nobr>");
                if (e.subtitle != null) {
                    sb.append("<br><nobr><font color='").append(dim).append("'>&nbsp;&nbsp;&nbsp;&nbsp;")
                            .append(escape(e.subtitle)).append("</font></nobr>");
                }
                sb.append("</html>");
                setText(sb.toString());
                StringBuilder tip = new StringBuilder();
                tip.append(e.userIndex()).append(". ").append(e.title);
                if (e.subtitle != null) {
                    tip.append(" — ").append(e.subtitle);
                }
                if (e.sourceFileName != null) {
                    tip.append(" (").append(e.sourceFileName).append(")");
                }
                setToolTipText(tip.toString());
            }
            return this;
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '&': sb.append("&amp;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
