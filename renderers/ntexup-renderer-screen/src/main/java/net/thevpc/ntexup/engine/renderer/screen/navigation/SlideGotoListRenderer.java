package net.thevpc.ntexup.engine.renderer.screen.navigation;

import net.thevpc.ntexup.api.document.node.NTxHighlights;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Custom cell renderer for the "Go to slide" rows: a fixed-width, right-aligned
 * slide number, a title (with the typed fragment tinted), an optional subtle
 * section name, an accent marker on the current slide, and hover/selection
 * states coherent with {@code SlideSearchResultRenderer}.
 * <p>
 * Colors are centralized here so the future light/dark theme work can fold
 * them into a shared theme (see {@code documentation/todo/ligh-dark-laf}).
 */
public class SlideGotoListRenderer extends JPanel implements ListCellRenderer<NTxSlideTitleIndex.Entry> {

    // -- palette (dark, coherent with the Find dialog; to move to the theme) --
    private static final Color SELECTED_BG = new Color(255, 255, 255, 34);
    private static final Color HOVER_BG = new Color(255, 255, 255, 16);
    private static final Color ACCENT = new Color(167, 139, 250);
    private static final Color INDEX_FG = new Color(148, 163, 184);
    private static final Color TITLE_FG = Color.WHITE;
    private static final Color SECTION_FG = new Color(100, 116, 139);
    private static final Color MATCH_BG = new Color(124, 58, 237, 110);

    private static final int PAD = 10;
    private static final int MARKER_W = 14;
    private static final int INDEX_W = 44;

    private NTxSlideTitleIndex.Entry value;
    private boolean selected;
    private boolean hover;
    private int hoverIndex = -1;
    private int currentPageIndex = -1;
    private boolean showSection;
    private String query = "";

    public SlideGotoListRenderer() {
        setOpaque(false);
    }

    public void setHoverIndex(int index) {
        if (hoverIndex != index) {
            hoverIndex = index;
            repaint();
        }
    }

    /** 0-based index of the slide currently shown by the viewer (marked), or -1 */
    public void setCurrentPageIndex(int currentPageIndex) {
        this.currentPageIndex = currentPageIndex;
    }

    public void setShowSection(boolean showSection) {
        this.showSection = showSection;
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query.trim();
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends NTxSlideTitleIndex.Entry> list,
                                                  NTxSlideTitleIndex.Entry value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        this.value = value;
        this.selected = isSelected;
        this.hover = index == hoverIndex;
        setEnabled(list.isEnabled());
        setFont(list.getFont());
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (value == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        boolean current = value.pageIndex == currentPageIndex;

        if (selected) {
            g2.setColor(SELECTED_BG);
            g2.fillRoundRect(2, 1, w - 4, h - 2, 8, 8);
        } else if (hover) {
            g2.setColor(HOVER_BG);
            g2.fillRoundRect(2, 1, w - 4, h - 2, 8, 8);
        }

        Font base = getFont() != null ? getFont() : new Font("SansSerif", Font.PLAIN, 13);
        Font fIndex = base.deriveFont(Font.BOLD, 12f);
        Font fTitle = base.deriveFont(Font.PLAIN, 13f);
        Font fSection = base.deriveFont(Font.ITALIC, 11f);

        // current-slide marker
        int markerX = PAD;
        g2.setFont(fIndex);
        if (current) {
            g2.setColor(ACCENT);
            g2.drawString("▸", markerX, baseline(g2));
        }

        // right-aligned slide number
        int indexRight = PAD + MARKER_W + INDEX_W;
        g2.setFont(fIndex);
        FontMetrics fmIndex = g2.getFontMetrics();
        String num = Integer.toString(value.userIndex());
        g2.setColor(current ? ACCENT : INDEX_FG);
        g2.drawString(num, indexRight - fmIndex.stringWidth(num), baseline(g2));

        // section (subtle, right-aligned) — only when it helps
        int rightLimit = w - PAD;
        String section = (showSection && value.section != null && !value.section.isEmpty()
                && !value.section.equalsIgnoreCase(value.title)) ? value.section : "";
        int sectionW = 0;
        if (!section.isEmpty()) {
            g2.setFont(fSection);
            FontMetrics fmSection = g2.getFontMetrics();
            sectionW = fmSection.stringWidth(section);
            int maxSection = Math.max(0, (w - (PAD + MARKER_W + INDEX_W + 8)) / 2);
            if (sectionW > maxSection) {
                section = clip(g2, section, maxSection);
                sectionW = g2.getFontMetrics().stringWidth(section);
            }
            sectionW += 10;
        }

        // title, with the typed fragment tinted
        int titleX = PAD + MARKER_W + INDEX_W + 8;
        int titleW = Math.max(0, rightLimit - titleX - sectionW);
        g2.setFont(fTitle);
        String title = value.title == null ? "" : value.title;
        FontMetrics fmTitle = g2.getFontMetrics();
        String shownTitle = title;
        if (fmTitle.stringWidth(shownTitle) > titleW) {
            shownTitle = clip(g2, shownTitle, titleW);
        }
        drawTitle(g2, shownTitle, titleX, baseline(g2));

        if (!section.isEmpty()) {
            g2.setFont(fSection);
            g2.setColor(SECTION_FG);
            g2.drawString(section, rightLimit - g2.getFontMetrics().stringWidth(section), baseline(g2));
        }

        g2.dispose();
    }

    private void drawTitle(Graphics2D g2, String title, int x, int baseline) {
        FontMetrics fm = g2.getFontMetrics();
        String needle = SlideGotoFilter.norm(query);
        if (needle.isEmpty()) {
            g2.setColor(TITLE_FG);
            g2.drawString(title, x, baseline);
            return;
        }
        List<int[]> ranges = NTxHighlights.findMatches(title, needle);
        if (ranges.isEmpty()) {
            g2.setColor(TITLE_FG);
            g2.drawString(title, x, baseline);
            return;
        }
        // tint matching ranges behind the text
        for (int[] r : ranges) {
            int s = Math.max(0, Math.min(r[0], title.length()));
            int e = Math.max(s, Math.min(r[1], title.length()));
            if (e <= s) {
                continue;
            }
            int rx = x + fm.stringWidth(title.substring(0, s));
            int rw = fm.stringWidth(title.substring(s, e));
            g2.setColor(MATCH_BG);
            g2.fillRoundRect(rx - 1, baseline - fm.getAscent() + 1, rw + 2,
                    fm.getAscent() + fm.getDescent(), 3, 3);
        }
        g2.setColor(TITLE_FG);
        g2.drawString(title, x, baseline);
    }

    private int baseline(Graphics2D g2) {
        FontMetrics fm = g2.getFontMetrics();
        return (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
    }

    private String clip(Graphics2D g2, String s, int maxW) {
        if (s.isEmpty() || maxW <= 0) {
            return "";
        }
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(s) <= maxW) {
            return s;
        }
        int ellipsisW = fm.stringWidth("…");
        StringBuilder sb = new StringBuilder();
        int acc = 0;
        for (int i = 0; i < s.length(); i++) {
            int cw = fm.charWidth(s.charAt(i));
            if (acc + cw + ellipsisW > maxW) {
                break;
            }
            acc += cw;
            sb.append(s.charAt(i));
        }
        sb.append('…');
        return sb.toString();
    }
}
