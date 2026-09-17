package net.thevpc.ntexup.extension.progress.skin;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.extension.progress.NTxProgressSkin;
import net.thevpc.ntexup.extension.progress.model.NTxProgress;

import java.awt.*;
import java.util.Locale;

/**
 * Text skin: percentage when value is not NaN; elapsed shown when present;
 * eta appended when present. E.g. "63% — 1m 20s elapsed, ~40s remaining"
 */
public class NTxTextSkin implements NTxProgressSkin {

    @Override
    public String id() {
        return "text";
    }

    @Override
    public void render(NTxGraphics g, NTxBounds2D bounds, NTxProgress progress, boolean animating) {
        String text = formatProgress(progress);
        if (text == null || text.isEmpty()) return;

        double x = bounds.minX();
        double y = bounds.minY();
        double w = bounds.widthX();
        double h = bounds.widthY();

        g.setColor(new Color(0xe5e7eb));
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) Math.max(12, h * 0.6)));

        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(text);
        int textH = fm.getAscent();

        // Center within bounds
        double tx = x + (w - textW) / 2.0;
        double ty = y + (h + textH) / 2.0 - fm.getDescent();

        // If text is wider than bounds, scale font down
        if (textW > w && w > 0) {
            float size = (float) (g.getFont().getSize() * w / textW);
            g.setFont(new Font("SansSerif", Font.PLAIN, (int) Math.max(8, size)));
            fm = g.getFontMetrics();
            textW = fm.stringWidth(text);
            textH = fm.getAscent();
            tx = x + (w - textW) / 2.0;
            ty = y + (h + textH) / 2.0 - fm.getDescent();
        }

        g.drawString(text, (int) tx, (int) ty);
    }

    /**
     * Format the progress for display.
     * "63%" or "63% — 1m 20s elapsed" or "63% — 1m 20s elapsed, ~40s remaining"
     * or just "1m 20s" if only elapsed is available.
     */
    public static String formatProgress(NTxProgress progress) {
        StringBuilder sb = new StringBuilder();

        boolean hasValue = !Double.isNaN(progress.value());
        boolean hasElapsed = progress.elapsed() != null;
        boolean hasEta = progress.eta() != null && hasValue;

        if (hasValue) {
            sb.append(String.format(Locale.US, "%.0f%%", progress.value() * 100));
        }

        if (hasElapsed) {
            String elapsedStr = formatDuration(progress.elapsed());
            if (hasValue) {
                sb.append(" \u2014 ").append(elapsedStr).append(" elapsed");
            } else {
                sb.append(elapsedStr);
            }
        }

        if (hasEta) {
            String etaStr = formatDuration(progress.eta());
            sb.append(", ~").append(etaStr).append(" remaining");
        }

        return sb.toString();
    }

    private static String formatDuration(net.thevpc.nuts.time.NDuration d) {
        long totalSecs = d.toSeconds();
        if (totalSecs < 0) totalSecs = 0;
        if (totalSecs < 60) return totalSecs + "s";
        long mins = totalSecs / 60;
        long secs = totalSecs % 60;
        if (mins < 60) return mins + "m " + secs + "s";
        long hours = mins / 60;
        mins = mins % 60;
        return hours + "h " + mins + "m";
    }
}
