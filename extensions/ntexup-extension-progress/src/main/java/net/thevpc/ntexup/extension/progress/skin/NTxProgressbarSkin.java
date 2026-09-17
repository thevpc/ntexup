package net.thevpc.ntexup.extension.progress.skin;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.extension.progress.model.NTxProgress;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Progressbar skin: fill portion sized to value, oscillating segment when indeterminate.
 */
public class NTxProgressbarSkin implements NTxProgressSkin {

    @Override
    public String id() {
        return "progressbar";
    }

    @Override
    public void render(NTxGraphics g, NTxBounds2D bounds, NTxProgress progress, boolean animating) {
        double x = bounds.minX();
        double y = bounds.minY();
        double w = bounds.widthX();
        double h = bounds.widthY();

        // Background track
        g.setColor(new Color(0x374151)); // gray-700
        g.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));

        double value = progress.value();
        boolean indet = progress.indeterminate();

        // Fill portion
        if (!Double.isNaN(value)) {
            double fillW = Math.max(h, w * value);
            g.setColor(new Color(0x3b82f6)); // blue-500
            g.fill(new RoundRectangle2D.Double(x, y, fillW, h, h, h));
        }

        // Indeterminate oscillating segment
        if (indet) {
            long t = System.currentTimeMillis();
            double phase = (t % 3000) / 3000.0; // 0..1 over 3s
            double segW = w * 0.3;
            double segX = x + (w + segW) * phase - segW;
            // clamp to bounds
            segX = Math.max(x, Math.min(segX, x + w - segW));

            g.setColor(new Color(0x60a5fa)); // blue-400
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            g.fill(new RoundRectangle2D.Double(segX, y, segW, h, h, h));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        // Text caption
        renderCaption(g, bounds, progress);
    }

    private void renderCaption(NTxGraphics g, NTxBounds2D bounds, NTxProgress progress) {
        String caption = buildCaption(progress);
        if (caption == null) return;

        double x = bounds.minX();
        double y = bounds.minY();
        double w = bounds.widthX();
        double h = bounds.widthY();

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, (int) Math.max(10, h * 0.6)));
        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(caption);
        int textH = fm.getAscent();
        double tx = x + (w - textW) / 2.0;
        double ty = y + (h + textH) / 2.0 - fm.getDescent();
        g.drawString(caption, (int) tx, (int) ty);
    }

    private String buildCaption(NTxProgress progress) {
        StringBuilder sb = new StringBuilder();
        if (!Double.isNaN(progress.value())) {
            sb.append(String.format("%.0f%%", progress.value() * 100));
        }
        if (progress.elapsed() != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(formatDuration(progress.elapsed()));
        }
        if (!Double.isNaN(progress.value()) && progress.eta() != null) {
            sb.append(" ~").append(formatDuration(progress.eta()));
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private String formatDuration(net.thevpc.nuts.time.NDuration d) {
        long secs = d.toSeconds();
        if (secs < 60) return secs + "s";
        long mins = secs / 60;
        secs = secs % 60;
        if (mins < 60) return mins + "m " + secs + "s";
        long hours = mins / 60;
        mins = mins % 60;
        return hours + "h " + mins + "m";
    }
}
