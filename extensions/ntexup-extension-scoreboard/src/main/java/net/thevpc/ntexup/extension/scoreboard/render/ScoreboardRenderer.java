package net.thevpc.ntexup.extension.scoreboard.render;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxImageOptions;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.extension.scoreboard.ScoreboardPaths;
import net.thevpc.ntexup.extension.scoreboard.model.ScoreEntry;
import net.thevpc.ntexup.extension.scoreboard.model.ScoreboardModel;
import net.thevpc.nuts.io.NPath;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Draws a scoreboard on an ntexup canvas.
 *
 * <p>The visual language is deliberately "modern dashboard": dark glass card,
 * coloured ranked rows, medal-style rank badges, score count-up, staggered
 * entrance and smooth rank reordering. The same code path produces a clean
 * final frame for PDF/image export (no animation capability).</p>
 */
public final class ScoreboardRenderer {

    public void render(NTxRendererContext ctx, ScoreboardModel model) {
        NTxBounds2D bounds = ctx.selfBounds2D();
        if (bounds == null || bounds.widthX() <= 1 || bounds.widthY() <= 1) {
            return;
        }
        ScoreboardStyle style = ScoreboardStyle.read(ctx.node());

        List<ScoreEntry> entries = prepareEntries(model, style);
        if (entries.isEmpty()) {
            drawBackground(ctx, bounds, style);
            drawEmpty(ctx, bounds, style);
            return;
        }

        boolean animate = ctx.isAnimate() && !ctx.isPrint() && ctx.isPageShowing();
        double elapsed = animate ? Math.max(0, ctx.pageElapsedSeconds()) : Double.MAX_VALUE;
        int n = entries.size();

        double finalMax = model.maxScore();
        double axisMax = style.max > 0 ? style.max : finalMax;
        if (axisMax <= 0) {
            axisMax = 1;
        }

        // Final ranking is used for the reveal stagger and for static renders.
        int[] finalRank = finalRanks(entries);
        double revealDelay = 0.18;
        double lastRevealStart = revealDelay;
        double[] shown = new double[n];
        boolean allRevealed = true;
        for (int i = 0; i < n; i++) {
            double start = revealDelay + finalRank[i] * style.stagger * 0.65;
            lastRevealStart = Math.max(lastRevealStart, start);
            double p = animate ? clamp01((elapsed - start) / style.duration) : 1.0;
            if (p < 1.0) {
                allRevealed = false;
            }
            shown[i] = Math.max(0, entries.get(i).getScore() * easeOutCubic(p));
        }
        boolean finished = !animate || allRevealed;

        double animationEnd = lastRevealStart + style.duration + 1.15;
        double[] positions = computePositions(ctx, entries, shown, style, animate, elapsed, animationEnd);

        drawBackground(ctx, bounds, style);
        double headerH = drawHeader(ctx, bounds, style, model.getTitle(), model.getSubtitle());

        double padX = Math.max(1, bounds.widthX() * 0.032);
        double padY = Math.max(1, bounds.widthY() * 0.032);
        double rowsTop = bounds.minY() + padY + headerH;
        if (headerH > 0) {
            rowsTop += bounds.widthY() * 0.018;
        }
        double rowsBottom = bounds.maxY() - padY;
        double rowsHeight = Math.max(1, rowsBottom - rowsTop);
        double rowH = rowsHeight / n;
        double gap = rowH * style.gapRatio;
        double cardH = Math.max(1, rowH - gap);
        double cardX = bounds.minX() + padX;
        double cardW = Math.max(1, bounds.widthX() - 2 * padX);

        // Recompute ranks from the displayed values (fall back to input order
        // when sorting is disabled). This is what makes the badges swap live.
        int[] rankNow = style.sort ? ranks(entries, shown) : identityRanks(n);

        boolean wantsFrame = animate && (elapsed < animationEnd || style.pulse);
        for (int i = 0; i < n; i++) {
            double entrance = animate
                    ? easeOutCubic(clamp01((elapsed - i * style.stagger) / 0.55))
                    : 1.0;
            if (entrance <= 0.001) {
                continue;
            }
            double y = rowsTop + positions[i] * rowH + gap * 0.5;
            drawRow(ctx, cardX, y, cardW, cardH, entries.get(i), rankNow[i],
                    shown[i], axisMax, entrance, finished, style, animate, elapsed);
        }

        if (wantsFrame) {
            ctx.requestAnimationFrame(true);
        }
    }

    private List<ScoreEntry> prepareEntries(ScoreboardModel model, ScoreboardStyle style) {
        List<ScoreEntry> entries = new ArrayList<>(model.getEntries());
        if (style.limit > 0 && entries.size() > style.limit) {
            // Keep the top N by final score, but preserve input order: the
            // animation starts in input order and reorders itself live.
            List<ScoreEntry> ranked = new ArrayList<>(entries);
            ranked.sort(Comparator
                    .comparingDouble(ScoreEntry::getScore).reversed()
                    .thenComparingInt(ScoreEntry::getIndex));
            List<ScoreEntry> top = new ArrayList<>(ranked.subList(0, style.limit));
            entries = new ArrayList<>();
            for (ScoreEntry entry : model.getEntries()) {
                if (top.contains(entry)) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    private double[] computePositions(NTxRendererContext ctx, List<ScoreEntry> entries, double[] shown,
                                      ScoreboardStyle style, boolean animate, double elapsed, double animationEnd) {
        int n = entries.size();
        int[] target = style.sort ? ranks(entries, shown) : identityRanks(n);
        double[] targetPos = new double[n];
        for (int i = 0; i < n; i++) {
            targetPos[i] = target[i];
        }
        if (!animate) {
            return targetPos;
        }
        NTxNode node = ctx.node();
        // User objects survive render-cache invalidation, which matters for
        // hosts that render animated pages with caching disabled.
        AnimState state = node == null ? null
                : node.getAndSetUserObject(AnimState.class, false, AnimState::new).orNull();
        if (state == null) {
            return targetPos;
        }
        if (state.positions == null || state.positions.length != n
                || state.lastElapsed < 0 || elapsed < state.lastElapsed - 0.4) {
            state.positions = new double[n];
            for (int i = 0; i < n; i++) {
                state.positions[i] = i;
            }
            state.lastElapsed = elapsed - (1.0 / 60.0);
        }
        double dt = Math.max(0, Math.min(0.2, elapsed - state.lastElapsed));
        if (dt <= 0.0001) {
            dt = 1.0 / 60.0;
        }
        double smoothing = 1.0 - Math.exp(-dt * 9.0);
        for (int i = 0; i < n; i++) {
            state.positions[i] += (targetPos[i] - state.positions[i]) * smoothing;
        }
        if (elapsed > animationEnd) {
            System.arraycopy(targetPos, 0, state.positions, 0, n);
        }
        state.lastElapsed = elapsed;
        return state.positions;
    }

    private int[] finalRanks(List<ScoreEntry> entries) {
        int n = entries.size();
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator
                .comparingDouble((Integer i) -> entries.get(i).getScore()).reversed()
                .thenComparingInt(i -> entries.get(i).getIndex()));
        int[] ranks = new int[n];
        for (int r = 0; r < n; r++) {
            ranks[order[r]] = r;
        }
        return ranks;
    }

    private int[] ranks(List<ScoreEntry> entries, double[] values) {
        int n = entries.size();
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator
                .comparingDouble((Integer i) -> values[i]).reversed()
                .thenComparingInt(i -> entries.get(i).getIndex()));
        int[] ranks = new int[n];
        for (int r = 0; r < n; r++) {
            ranks[order[r]] = r;
        }
        return ranks;
    }

    private int[] identityRanks(int n) {
        int[] ranks = new int[n];
        for (int i = 0; i < n; i++) {
            ranks[i] = i;
        }
        return ranks;
    }

    // ------------------------------------------------------------------
    // background & header
    // ------------------------------------------------------------------

    private void drawBackground(NTxRendererContext ctx, NTxBounds2D b, ScoreboardStyle style) {
        NTxGraphics g = ctx.graphics();
        double x = b.minX();
        double y = b.minY();
        double w = b.widthX();
        double h = b.widthY();
        double radius = Math.min(w, h) * 0.035;
        Shape card = new RoundRectangle2D.Double(x, y, w, h, radius, radius);

        g.setPaint(new GradientPaint(
                (float) x, (float) y, style.backgroundColor,
                (float) (x + w), (float) (y + h), style.background2Color));
        g.fill(card);

        Paint oldPaint = g.graphics2D().getPaint();
        Shape oldClip = g.graphics2D().getClip();
        Stroke oldStroke = g.graphics2D().getStroke();
        try {
            g.graphics2D().clip(card);
            Color accent = style.palette != null && style.palette.length > 0 ? style.palette[0] : style.titleColor;

            // Two very soft coloured blooms keep the flat card from looking dull.
            // Light/minimal themes stay clean and paper-like.
            if (!style.theme.equals("light") && !style.theme.equals("minimal")) {
                g.setColor(ScoreboardStyle.withAlpha(accent, 22));
                g.fillOval(x + w * 0.58, y - h * 0.22, w * 0.55, h * 0.7);
                g.setColor(ScoreboardStyle.withAlpha(accent, 12));
                g.fillOval(x - w * 0.2, y + h * 0.62, w * 0.45, h * 0.55);
            }

            // Thin top highlight, a common "glass" cue.
            g.setColor(ScoreboardStyle.withAlpha(Color.WHITE, 26));
            g.fillRoundRect(x + w * 0.03, y + h * 0.012, w * 0.94, Math.max(1, h * 0.004), 4, 4);
        } finally {
            g.graphics2D().setClip(oldClip);
            g.graphics2D().setPaint(oldPaint);
            g.graphics2D().setStroke(oldStroke);
        }

        g.setColor(ScoreboardStyle.withAlpha(Color.WHITE, style.theme.equals("light") ? 70 : 24));
        g.setStroke(new BasicStroke((float) Math.max(0.75, Math.min(w, h) * 0.0015)));
        g.draw(card);
        g.setStroke(oldStroke);
    }

    private double drawHeader(NTxRendererContext ctx, NTxBounds2D b, ScoreboardStyle style,
                              String modelTitle, String modelSubtitle) {
        String title = style.showTitle ? blankToNull(modelTitle) : null;
        String subtitle = style.showSubtitle ? blankToNull(modelSubtitle) : null;
        boolean hasIcon = !isBlank(style.iconPath);
        if (title == null && subtitle == null && !hasIcon) {
            return 0;
        }

        double w = b.widthX();
        double h = b.widthY();
        double padX = Math.max(1, w * 0.032);
        double padY = Math.max(1, h * 0.032);
        double headerH = Math.max(h * 0.12, Math.min(h * 0.2, h * 0.16));
        double x = b.minX() + padX;
        double top = b.minY() + padY;

        NTxGraphics g = ctx.graphics();
        Font oldFont = g.getFont();
        Paint oldPaint = g.graphics2D().getPaint();

        double iconSize = hasIcon ? headerH * 0.82 : 0;
        double textWidth = w - 2 * padX - (hasIcon ? iconSize + padX * 0.7 : 0);

        double titleSize = Math.max(7, Math.min(headerH * 0.54, w * 0.052));
        double subtitleSize = Math.max(5, titleSize * 0.42);
        if (title == null) {
            titleSize = 0;
        }
        if (subtitle == null) {
            subtitleSize = 0;
        }

        double blockH = (titleSize > 0 ? titleSize * 1.15 : 0)
                + (subtitleSize > 0 ? subtitleSize * 1.45 : 0);
        double blockTop = top + (headerH - blockH) / 2.0;

        // Accent marker in front of the title.
        double accentW = Math.max(2.5, titleSize * 0.16);
        double accentX = x;
        double accentY = blockTop + (titleSize > 0 ? titleSize * 0.1 : 0);
        double accentH = titleSize > 0 ? titleSize * 0.95 : blockH * 0.7;
        Color accent = style.palette != null && style.palette.length > 0 ? style.palette[0] : style.titleColor;
        g.setColor(accent);
        g.fillRoundRect(accentX, accentY, accentW, accentH, accentW, accentW);

        double textX = accentX + accentW + titleSize * 0.38;
        double textW = textWidth - (textX - x);

        if (titleSize > 0) {
            Font titleFont = new Font(style.fontFamily, Font.BOLD, (int) Math.round(titleSize));
            g.setFont(titleFont);
            FontMetrics fm = g.getFontMetrics();
            String fitted = fit(title, fm, textW);
            g.setColor(style.titleColor);
            g.drawString(fitted, textX, blockTop + fm.getAscent());
        }
        if (subtitleSize > 0) {
            Font subtitleFont = new Font(style.fontFamily, Font.PLAIN, (int) Math.round(subtitleSize));
            g.setFont(subtitleFont);
            FontMetrics fm = g.getFontMetrics();
            double subtitleY = blockTop + (titleSize > 0 ? titleSize * 1.15 : 0) + fm.getAscent();
            g.setColor(style.subtitleColor);
            g.drawString(fit(subtitle, fm, textW), textX, subtitleY);
        }

        if (hasIcon) {
            drawIcon(ctx, b, style, iconSize, b.maxX() - padX - iconSize, top + (headerH - iconSize) / 2.0);
        }

        // Gradient separator under the header.
        double lineY = top + headerH + h * 0.012;
        g.setPaint(new GradientPaint(
                (float) x, (float) lineY, ScoreboardStyle.withAlpha(accent, 90),
                (float) (x + w * 0.6), (float) lineY, ScoreboardStyle.withAlpha(accent, 0)));
        g.fillRoundRect(x, lineY, w - 2 * padX, Math.max(1, h * 0.0025), 2, 2);

        g.setFont(oldFont);
        g.graphics2D().setPaint(oldPaint);
        return headerH + h * 0.012;
    }

    private void drawIcon(NTxRendererContext ctx, NTxBounds2D b, ScoreboardStyle style,
                          double size, double x, double y) {
        NPath path = ScoreboardPaths.resolve(ctx, style.iconPath);
        if (path == null || !path.isRegularFile()) {
            return;
        }
        NTxGraphics g = ctx.graphics();
        Shape oldClip = g.graphics2D().getClip();
        try {
            double radius = size * 0.28;
            g.graphics2D().clip(new RoundRectangle2D.Double(x, y, size, size, radius, radius));
            NTxImageOptions options = new NTxImageOptions()
                    .setSize(new Dimension((int) Math.max(1, size), (int) Math.max(1, size)))
                    .setPreserveAspectRatio(true)
                    .setDisableAnimation(!ctx.isAnimate())
                    .setImageObserver(ctx.imageObserver());
            try {
                g.drawImage(path, x, y, options);
            } catch (RuntimeException ex) {
                ctx.log(net.thevpc.nuts.text.NMsg.ofC("[scoreboard] cannot draw icon %s: %s", path, ex).asWarning(),
                        ctx.source());
            }
        } finally {
            g.graphics2D().setClip(oldClip);
        }
    }

    // ------------------------------------------------------------------
    // rows
    // ------------------------------------------------------------------

    private void drawRow(NTxRendererContext ctx, double cardX, double cardY, double cardW, double cardH,
                         ScoreEntry entry, int rank, double shown, double axisMax, double alpha,
                         boolean finished, ScoreboardStyle style, boolean animate, double elapsed) {
        if (cardH <= 1 || cardW <= 1) {
            return;
        }
        double radius = Math.min(cardH * style.cornerRatio, cardH / 2.0);
        double x = cardX;
        double y = cardY;
        double w = cardW;
        double h = cardH;

        NTxGraphics g = ctx.graphics();
        Composite oldComposite = g.graphics2D().getComposite();
        AffineTransform oldTransform = g.graphics2D().getTransform();
        Stroke oldStroke = g.graphics2D().getStroke();
        Paint oldPaint = g.graphics2D().getPaint();
        Color oldColor = g.getColor();

        try {
            if (animate) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) clamp01(alpha)));
                double shift = -(1.0 - alpha) * w * 0.025;
                g.graphics2D().translate(shift, 0);
            }

            Color rowColor = style.colorForRank(rank, entry.getColor());
            Color barTop = ScoreboardStyle.brighten(rowColor, 0.16);
            Color barBottom = ScoreboardStyle.darken(rowColor, 0.26);

            // Soft drop shadow, then the translucent track.
            g.setColor(ScoreboardStyle.withAlpha(Color.BLACK, style.theme.equals("light") ? 26 : 90));
            g.fillRoundRect(x, y + h * 0.045, w, h, radius, radius);
            g.setColor(style.trackColor);
            g.fillRoundRect(x, y, w, h, radius, radius);

            double fraction = axisMax <= 0 ? 0 : clamp01(shown / axisMax);
            double barW = 0;
            if (shown > 0) {
                barW = Math.min(w, Math.max(h * 0.35, w * fraction));
                Shape bar = new RoundRectangle2D.Double(x, y, barW, h, radius, radius);

                // A subtle glow under the leader makes the ranking readable at a glance.
                if (rank == 0 && style.pulse) {
                    double pulse = 0.5 + 0.5 * Math.sin((animate ? elapsed : 1.3) * 3.0);
                    g.setColor(ScoreboardStyle.withAlpha(barTop, (int) (26 + 30 * pulse)));
                    g.fillRoundRect(x - h * 0.06, y - h * 0.06, barW + h * 0.12, h * 1.12,
                            radius + h * 0.06, radius + h * 0.06);
                    g.setColor(ScoreboardStyle.withAlpha(Color.BLACK, style.theme.equals("light") ? 26 : 90));
                }

                g.setPaint(new GradientPaint(
                        (float) x, (float) y, barTop,
                        (float) (x + Math.max(1, barW)), (float) (y + h), barBottom));
                g.fill(bar);

                // Glass highlight along the top edge of the coloured bar.
                Shape oldClip = g.graphics2D().getClip();
                try {
                    g.graphics2D().clip(bar);
                    g.setColor(ScoreboardStyle.withAlpha(Color.WHITE, 42));
                    g.fillRoundRect(x + radius * 0.35, y + h * 0.08,
                            Math.max(1, barW - radius * 0.7), Math.max(1, h * 0.06),
                            h * 0.03, h * 0.03);
                } finally {
                    g.graphics2D().setClip(oldClip);
                }

                if (rank == 0) {
                    // Winner accent line on the outer edge.
                    g.setColor(ScoreboardStyle.withAlpha(Color.WHITE, 120));
                    g.setStroke(new BasicStroke((float) Math.max(1, h * 0.012)));
                    g.drawRoundRect(x + 1, y + 1, w - 2, h - 2, radius, radius);
                    g.setStroke(oldStroke);
                }
            }

            double badgeR = h * (style.showRank ? 0.27 : 0);
            double contentX = x + h * 0.42;
            if (style.showRank) {
                drawRankBadge(g, x + h * 0.5, y + h / 2.0, badgeR, rank + 1, style);
                contentX = x + h * 0.5 + badgeR + h * 0.2;
            }

            String scoreText = style.showScore
                    ? formatScore(shown, style) + unitSuffix(entry, style)
                    : "";
            double scoreSize = Math.max(5, h * 0.42);
            Font scoreFont = new Font(style.fontFamily, Font.BOLD, (int) Math.round(scoreSize));
            double scoreRight = x + w - h * 0.34;
            double scoreLeft = scoreRight;
            if (style.showScore) {
                g.setFont(scoreFont);
                FontMetrics fm = g.getFontMetrics();
                scoreLeft = scoreRight - fm.stringWidth(scoreText);
            }

            double availableW = Math.max(1, scoreLeft - contentX - h * 0.18);
            String name = entry.getName();
            String subName = entry.getSubName();
            if (finished && !isBlank(entry.getDisclosedName())) {
                subName = isBlank(subName) ? entry.getDisclosedName() : (subName + "  ·  " + entry.getDisclosedName());
            }

            if (!isBlank(name)) {
                double nameSize = Math.max(5, h * 0.35);
                double subSize = Math.max(4, h * 0.22);
                boolean hasSub = !isBlank(subName);
                Font nameFont = new Font(style.fontFamily, Font.BOLD, (int) Math.round(nameSize));
                Font subFont = new Font(style.fontFamily, Font.PLAIN, (int) Math.round(subSize));
                FontMetrics nameFm = g.getFontMetrics(nameFont);
                FontMetrics subFm = g.getFontMetrics(subFont);
                double nameH = nameFm.getAscent() + nameFm.getDescent();
                double subH = hasSub ? subFm.getAscent() + subFm.getDescent() : 0;
                double gap = hasSub ? nameSize * 0.08 : 0;
                double blockH = nameH + gap + subH;
                double top = y + (h - blockH) / 2.0;

                String fittedName = fit(name, nameFm, availableW);
                double nameWidth = nameFm.stringWidth(fittedName);
                boolean nameOverBar = (contentX - x) + nameWidth * 0.5 <= barW;
                Color nameTextColor = nameOverBar
                        ? ScoreboardStyle.textOnBar(rowColor, style.nameColor)
                        : style.onTrackColor;
                g.setFont(nameFont);
                g.setColor(nameTextColor);
                double nameBaseline = top + nameFm.getAscent();
                // A tiny shadow keeps light text legible on saturated bars.
                if (isLightText(nameTextColor)) {
                    g.setColor(ScoreboardStyle.withAlpha(Color.BLACK, 55));
                    g.drawString(fittedName, contentX + 0.75, nameBaseline + 0.75);
                    g.setColor(nameTextColor);
                }
                g.drawString(fittedName, contentX, nameBaseline);

                if (hasSub) {
                    g.setFont(subFont);
                    g.setColor(ScoreboardStyle.withAlpha(nameTextColor, 210));
                    g.drawString(fit(subName, subFm, availableW), contentX, nameBaseline + gap + subFm.getAscent());
                }
            }

            if (style.showScore) {
                g.setFont(scoreFont);
                FontMetrics fm = g.getFontMetrics();
                double scoreWidth = fm.stringWidth(scoreText);
                boolean scoreOverBar = scoreRight - scoreWidth * 0.5 - x <= barW;
                Color scoreTextColor = scoreOverBar
                        ? ScoreboardStyle.textOnBar(rowColor, style.scoreColor)
                        : style.onTrackColor;
                g.setColor(scoreTextColor);
                double baseline = y + (h - (fm.getAscent() + fm.getDescent())) / 2.0 + fm.getAscent();
                if (isLightText(scoreTextColor)) {
                    g.setColor(ScoreboardStyle.withAlpha(Color.BLACK, 55));
                    g.drawString(scoreText, scoreRight - scoreWidth + 0.75, baseline + 0.75);
                    g.setColor(scoreTextColor);
                }
                g.drawString(scoreText, scoreRight - scoreWidth, baseline);
            }
        } finally {
            g.graphics2D().setTransform(oldTransform);
            g.graphics2D().setComposite(oldComposite);
            g.graphics2D().setStroke(oldStroke);
            g.graphics2D().setPaint(oldPaint);
            g.setColor(oldColor);
        }
    }

    private void drawRankBadge(NTxGraphics g, double cx, double cy, double r, int rank, ScoreboardStyle style) {
        Color medal = ScoreboardStyle.medalColor(rank - 1);
        if (medal != null) {
            g.setPaint(new GradientPaint(
                    (float) (cx - r), (float) (cy - r), ScoreboardStyle.brighten(medal, 0.25),
                    (float) (cx + r), (float) (cy + r), ScoreboardStyle.darken(medal, 0.16)));
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
            g.setColor(ScoreboardStyle.withAlpha(Color.WHITE, 120));
            g.setStroke(new BasicStroke((float) Math.max(0.75, r * 0.06)));
            g.drawOval(cx - r, cy - r, r * 2, r * 2);
        } else {
            g.setColor(ScoreboardStyle.withAlpha(Color.WHITE, style.theme.equals("light") ? 120 : 30));
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
            g.setColor(ScoreboardStyle.withAlpha(Color.WHITE, style.theme.equals("light") ? 150 : 55));
            g.setStroke(new BasicStroke((float) Math.max(0.5, r * 0.05)));
            g.drawOval(cx - r, cy - r, r * 2, r * 2);
        }
        String text = String.valueOf(rank);
        Font font = new Font(style.fontFamily, Font.BOLD, (int) Math.round(Math.max(4, r * 1.05)));
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        Color textColor = medal != null
                ? (style.theme.equals("light") ? Color.WHITE : new Color(0x1F2937))
                : (style.theme.equals("light") ? new Color(0x0F172A) : style.rankColor);
        g.setColor(textColor);
        g.drawString(text, cx - fm.stringWidth(text) / 2.0, cy + (fm.getAscent() - fm.getDescent()) / 2.0);
    }

    private void drawEmpty(NTxRendererContext ctx, NTxBounds2D b, ScoreboardStyle style) {
        NTxGraphics g = ctx.graphics();
        Font font = new Font(style.fontFamily, Font.BOLD, (int) Math.round(Math.max(6, b.widthY() * 0.045)));
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        String text = "No scores";
        g.setColor(style.mutedColor);
        g.drawString(text,
                b.centerX() - fm.stringWidth(text) / 2.0,
                b.centerY() + (fm.getAscent() - fm.getDescent()) / 2.0);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private String formatScore(double value, ScoreboardStyle style) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ENGLISH);
        symbols.setGroupingSeparator(',');
        DecimalFormat format;
        if (!isBlank(style.scoreFormat)) {
            format = new DecimalFormat(style.scoreFormat, symbols);
        } else {
            format = new DecimalFormat("0.##", symbols);
        }
        return format.format(value);
    }

    private String unitSuffix(ScoreEntry entry, ScoreboardStyle style) {
        String unit = !isBlank(entry.getUnit()) ? entry.getUnit() : style.unit;
        if (isBlank(unit) || !style.showUnit) {
            return "";
        }
        return unit.startsWith("%") ? unit : (" " + unit);
    }

    private static String fit(String text, FontMetrics fm, double maxWidth) {
        if (text == null) {
            return "";
        }
        if (maxWidth <= 0 || fm.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int lo = 0;
        int hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (fm.stringWidth(text.substring(0, mid) + ellipsis) <= maxWidth) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return lo <= 0 ? ellipsis : text.substring(0, lo).trim() + ellipsis;
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static double easeOutCubic(double t) {
        t = clamp01(t);
        return 1 - Math.pow(1 - t, 3);
    }

    private static boolean isLightText(Color c) {
        return c != null && (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000 > 170;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s;
    }

    /** Render-cache payload: vertical positions between two animation frames. */
    private static final class AnimState {
        double[] positions;
        double lastElapsed = -1;
    }
}
