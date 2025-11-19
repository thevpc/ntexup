/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.net.nscoreboard.engine.board;

import net.thevpc.net.nscoreboard.engine.PaintContext;
import net.thevpc.net.nscoreboard.model.NScore;
import net.thevpc.net.nscoreboard.model.NScoreboard;
import net.thevpc.net.nscoreboard.util.Utils;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NStringUtils;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * @author vpc
 */
public class HorizontalScoreBoard extends JComponent {

    public static final String DEFAULT_NAME_FONT_NAME = "JetBrains Mono";
    public static final String DEFAULT_SUBNAME_FONT_NAME = "JetBrains Mono";
    public static final String DEFAULT_SCORE_NAME = "Arial";
    private NScoreboard model = new NScoreboard().reindex();
    private double maxBarHeightRatio = 1.0 / 12;
    private boolean started;
    private double fontFactor = 1;

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        setFont(new Font("Arial", Font.PLAIN, 38));
    }

    public NScoreboard getModel() {
        return model;
    }

    public void setModel(NScoreboard model) {
        this.model = model;
        this.invalidate();
        SwingUtilities.invokeLater(() -> repaint());
    }

    private static class PaintInfo {
        public int nameFontHeight;
        public Rectangle2D scoreBounds;
        public double scoreY;
        NScore score;
        net.thevpc.net.nscoreboard.engine.PaintContext pc;
        int fw;
        double hbar;
        int inset;
        double pos;
        double vr;
        double bw;
        int rectX = inset;
        int rectW;
        int rectH;
        int rectY;
        Graphics2D g2d;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (!started) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        final Dimension s = getSize();
        final NScore[] u = model.scores();
        Color bc = getBackground();
        Color fc = getForeground();
        int count = u.length;
        int inset = 3;
        if (count > 0) {
            int fh = s.height;
            int fw = s.width;
            double hbar = fh / count;
            if (maxBarHeightRatio > 0 && maxBarHeightRatio < 1) {
                if ((1.0 / count) > maxBarHeightRatio) {
                    hbar = maxBarHeightRatio * fh;
                }
            }
            PaintInfo info = new PaintInfo();
            info.fw = fw;
            info.hbar = hbar;
            info.inset = inset;
            info.g2d = g2d;
            for (int i = 0; i < count; i++) {
                info.score = u[i];
                info.pc = new PaintContext() {
                    @Override
                    public Graphics2D graphics() {
                        return info.g2d;
                    }

                    @Override
                    public Rectangle bounds() {
                        return new Rectangle(info.rectX, info.rectY, info.rectW, info.rectH);
                    }
                };
                info.pos = info.score.positionMove;
                info.vr = info.score.score / model.max();
                info.bw = fw * info.vr;

                info.rectX = inset;
                info.rectW = intof(info.bw - 2 * inset);
                info.rectH = intof(hbar) - 2 * inset;
                info.rectY = intof(info.pos * hbar) + inset;
                if (info.rectW < 0) {
                    info.rectW = 0;
                }
                if (info.rectH < 0) {
                    info.rectH = intof(hbar) - 2 * inset;
                }
                paintOne(info);
            }
        }
    }

    private void paintOne(PaintInfo info) {
        if (info.score.background == null) {
            info.g2d.setPaint(Color.RED);
        } else {
            info.score.background.apply(info.pc);
        }
        info.g2d.fillRoundRect(info.inset, info.rectY, info.rectW, info.rectH, 5, 5);

        if (info.score.border == null) {
            info.g2d.setPaint(Color.RED.darker());
        } else {
            info.score.border.apply(info.pc);
        }
        info.g2d.drawRoundRect(info.inset, info.rectY, info.rectW, info.rectH, 5, 5);

        paintName(info);
        paintSubName(info);
        paintScore(info);
        paintPositionString(info);
    }

    private void paintName(PaintInfo info) {
        if (info.score.foreground == null) {
            info.g2d.setPaint(Color.WHITE);
        } else {
            info.score.foreground.apply(info.pc);
        }
        info.g2d.setFont(Utils.prepareFont(Utils.firstNonBlank(model.getNameFontName(), DEFAULT_NAME_FONT_NAME), info.rectW <= 0 ? 1 : info.rectW / 20.0, info.rectH <= 0 ? 1 : info.rectH / 2.0).deriveFont(Font.ITALIC));
        FontMetrics fm = info.g2d.getFontMetrics();
        info.nameFontHeight = fm.getHeight();
        info.g2d.drawString(info.score.name, intof(info.inset + 10), intof(info.pos * info.hbar) + info.nameFontHeight);
    }

    private void paintSubName(PaintInfo info) {
        if (info.score.foreground == null) {
            info.g2d.setPaint(Color.WHITE);
        } else {
            info.score.foreground.apply(info.pc);
        }
        String subname = NStringUtils.trim(info.score.subName);
        if (info.score.finished) {
            if (!NBlankable.isBlank(info.score.disclosedName)) {
                if (!NBlankable.isBlank(subname)) {
                    subname += " ";
                }
                subname += NStringUtils.trim(info.score.disclosedName);
            }
        }
        if (!NBlankable.isBlank(subname)) {
            info.g2d.setFont(Utils.prepareFont(Utils.firstNonBlank(model.getSubNameFontName(), model.getNameFontName(), DEFAULT_SUBNAME_FONT_NAME), info.rectW <= 0 ? 1 : info.rectW / 20.0, info.rectH <= 0 ? 1 : info.rectH / 4.0).deriveFont(Font.ITALIC));
            FontMetrics fm = info.g2d.getFontMetrics();
            int fontHeight2 = fm.getHeight();
            info.g2d.drawString(subname, intof(info.inset + 10), intof(info.pos * info.hbar) + info.nameFontHeight + fontHeight2);
        }
    }

    private void paintScore(PaintInfo info) {
        if (info.score.foreground == null) {
            info.g2d.setPaint(Color.WHITE);
        } else {
            info.score.foreground.apply(info.pc);
        }
        info.g2d.setFont(
                Utils.prepareFont(Utils.firstNonBlank(model.getScoreFontName(), model.getNameFontName(), DEFAULT_SCORE_NAME), info.rectW <= 0 ? 1 : info.rectW / 20.0, info.rectH <= 0 ? 1 : info.rectH / 2.0)
        );
        FontMetrics fm = info.g2d.getFontMetrics();
        String str = new DecimalFormat("0").format(info.score.score);
        Rectangle2D bounds = fm.getStringBounds(str, info.g2d);
        info.scoreBounds = bounds;
        info.scoreY = info.pos * info.hbar + fm.getMaxDescent() + fm.getMaxAscent();
        info.g2d.drawString(
                str
                , intof(info.bw - bounds.getWidth() - 2 * info.inset), intof(info.scoreY));
    }

    private void paintPositionString(PaintInfo info) {
        if (info.score.foreground == null) {
            info.g2d.setPaint(Color.WHITE);
        } else {
            info.score.foreground.apply(info.pc);
        }
        String positionString = null;
        if (info.score.position == 0) {
            positionString = "1st";
        } else if (info.score.position == 1) {
            positionString = "2nd";
        } else if (info.score.position == 2) {
            positionString = "3rd";
        }
        if (positionString != null) {
            info.g2d.setFont(Utils.prepareFont(Utils.firstNonBlank(model.getSubScoreFontName(), model.getScoreFontName(), model.getNameFontName(), "Roboto"), info.rectW <= 0 ? 1 : info.rectW / 20.0, info.rectH <= 0 ? 1 : info.rectH / 4.0));
            FontMetrics fm = info.g2d.getFontMetrics();
            double u = info.scoreBounds.getHeight() + info.scoreBounds.getY();
            Rectangle2D bounds = fm.getStringBounds(positionString, info.g2d);
            info.g2d.drawString(positionString, intof(info.bw - bounds.getWidth() - 2 * info.inset), intof(info.scoreY + u + fm.getHeight()));
        }
    }

    private static int intof(double a) {
        return (int) a;
    }

}
