package net.thevpc.ntexup.engine.renderer.screen.search;

import javax.swing.*;
import java.awt.*;

/**
 * Custom cell renderer for the result cards: dark hover/selection states,
 * a "Slide N of Total" line with title + kicker + source file, and a snippet
 * line whose matching terms are tinted with the query color.
 */
public class SlideSearchResultRenderer extends JPanel implements ListCellRenderer<NTxSlideSearcher.SlideSearchResult> {

    private NTxSlideSearcher.SlideSearchResult value;
    private boolean selected;
    private boolean hover;
    private int hoverIndex = -1;

    public SlideSearchResultRenderer() {
        setOpaque(false);
    }

    public void setHoverIndex(int index) {
        if (hoverIndex != index) {
            hoverIndex = index;
            repaint();
        }
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends NTxSlideSearcher.SlideSearchResult> list,
                                                  NTxSlideSearcher.SlideSearchResult value,
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
        int pad = 12;

        // card background
        if (selected) {
            g2.setColor(new Color(255, 255, 255, 34));
            g2.fillRoundRect(2, 1, w - 4, h - 2, 10, 10);
            if (value.queryColor != null) {
                Color qc = value.queryColor;
                g2.setColor(new Color(qc.getRed(), qc.getGreen(), qc.getBlue(), 230));
                g2.fillRoundRect(4, 7, 3, h - 14, 3, 3);
            }
        } else if (hover) {
            g2.setColor(new Color(255, 255, 255, 16));
            g2.fillRoundRect(2, 1, w - 4, h - 2, 10, 10);
        }

        Font base = getFont() != null ? getFont() : new Font("SansSerif", Font.PLAIN, 12);
        Font fMeta = base.deriveFont(Font.BOLD, 11f);
        Font fTitle = base.deriveFont(Font.BOLD, 13f);
        Font fSnippet = base.deriveFont(Font.PLAIN, 12f);
        Font fSource = base.deriveFont(Font.ITALIC, 10f);

        // ---- row 1: "Slide N of T   Title · Kicker" + source (right) ----
        int y1 = 8 + 14;
        g2.setFont(fMeta);
        FontMetrics fmMeta = g2.getFontMetrics();
        String slideInfo = "Slide " + (value.pageIndex + 1) + " of " + Math.max(value.totalSlides, 1);
        g2.setColor(new Color(148, 163, 184));
        g2.drawString(slideInfo, pad, y1);
        int xTitle = pad + fmMeta.stringWidth(slideInfo) + 10;

        String title = value.title == null || value.title.trim().isEmpty() ? "(untitled)" : value.title.trim();
        String src = value.sourceFileName == null ? "" : value.sourceFileName;
        int srcW = 0;
        if (!src.isEmpty()) {
            g2.setFont(fSource);
            srcW = g2.getFontMetrics().stringWidth(src) + pad + 12;
        }
        int kickerBudget = w - xTitle - pad - srcW - 8;
        String kicker = "";
        if (value.kicker != null && !value.kicker.trim().isEmpty() && !value.kicker.trim().equals(title)) {
            kicker = " · " + value.kicker.trim();
        }

        g2.setFont(fTitle);
        FontMetrics fmTitle = g2.getFontMetrics();
        // fit title+kicker in the available width; clip the kicker first, then the title
        if (kickerBudget > 0 && fmTitle.stringWidth(title + kicker) > kickerBudget) {
            g2.setFont(fSource);
            kicker = clip(g2, kicker, Math.max(0, kickerBudget - fmTitle.stringWidth(title)));
            g2.setFont(fTitle);
        }
        if (kickerBudget > 0 && fmTitle.stringWidth(title + kicker) > kickerBudget) {
            title = clip(g2, title, Math.max(8, kickerBudget - g2.getFontMetrics().stringWidth(kicker)));
        }
        g2.setColor(Color.WHITE);
        g2.drawString(title, xTitle, y1);
        int xAfter = xTitle + fmTitle.stringWidth(title);
        if (!kicker.isEmpty()) {
            g2.setFont(fSource);
            g2.setColor(new Color(148, 163, 184));
            g2.drawString(kicker, xAfter, y1);
        }
        if (!src.isEmpty()) {
            g2.setFont(fSource);
            g2.setColor(new Color(100, 116, 139));
            g2.drawString(src, w - pad - srcW + 6, y1);
        }

        // ---- row 2: snippet with colored term highlights ----
        g2.setFont(fSnippet);
        FontMetrics fmSnip = g2.getFontMetrics();
        int y2 = y1 + 8 + fmSnip.getAscent();
        String snip = value.snippet == null ? "" : value.snippet;
        snip = clip(g2, snip, w - 2 * pad);
        if (value.queryColor != null && value.snippetRanges != null && !value.snippetRanges.isEmpty()) {
            Color qc = value.queryColor;
            for (int[] r : value.snippetRanges) {
                int s = Math.max(0, Math.min(r[0], snip.length()));
                int e = Math.max(s, Math.min(r[1], snip.length()));
                if (e <= s) {
                    continue;
                }
                int rx = pad + fmSnip.stringWidth(snip.substring(0, s));
                int rw = fmSnip.stringWidth(snip.substring(s, e));
                g2.setColor(qc);
                g2.fillRoundRect(rx - 1, y2 - fmSnip.getAscent() + 1, rw + 2,
                        fmSnip.getAscent() + fmSnip.getDescent(), 3, 3);
            }
        }
        g2.setColor(new Color(203, 213, 225));
        g2.drawString(snip, pad, y2);

        g2.dispose();
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
