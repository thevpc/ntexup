package net.thevpc.ntexup.extension.progress.skin;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.extension.progress.model.NTxProgress;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

/**
 * Sandglass skin: sand level reflects value; shimmer/glow overlay when indeterminate.
 */
public class NTxSandglassSkin implements NTxProgressSkin {

    @Override
    public String id() {
        return "sandglass";
    }

    @Override
    public void render(NTxGraphics g, NTxBounds2D bounds, NTxProgress progress, boolean animating) {
        double x = bounds.minX();
        double y = bounds.minY();
        double w = bounds.widthX();
        double h = bounds.widthY();
        double cx = x + w / 2.0;
        double cy = y + h / 2.0;
        double bulbR = Math.min(w, h) * 0.35;

        // Glass outline — two bulbs connected by a narrow waist
        drawGlassOutline(g, cx, cy, bulbR);

        // Sand fill in bottom bulb
        double value = progress.value();
        if (!Double.isNaN(value)) {
            drawSandLevel(g, cx, cy, bulbR, value);
        }

        // Indeterminate shimmer
        if (progress.indeterminate()) {
            drawShimmer(g, cx, cy, bulbR);
        }

        // Label
        renderLabel(g, cx, y + h - bulbR * 0.3, progress);
    }

    private void drawGlassOutline(NTxGraphics g, double cx, double cy, double r) {
        g.setColor(new Color(0x9ca3af)); // gray-400
        g.setStroke(new BasicStroke(2.0f));
        // Top bulb
        g.draw(new Ellipse2D.Double(cx - r, cy - r * 2.2, r * 2, r * 2));
        // Bottom bulb
        g.draw(new Ellipse2D.Double(cx - r, cy + r * 0.2, r * 2, r * 2));
        // Waist
        g.drawLine((int) cx, (int) (cy - r * 0.2), (int) cx, (int) (cy + r * 0.2));
    }

    private void drawSandLevel(NTxGraphics g, double cx, double cy, double r, double value) {
        // Sand fills the bottom bulb from bottom up
        double sandHeight = r * 1.8 * value;
        double sandBottom = cy + r * 2.0;
        double sandTop = sandBottom - sandHeight;

        Ellipse2D bottomBulb = new Ellipse2D.Double(cx - r, cy + r * 0.2, r * 2, r * 2);

        Area sand = new Area(new Rectangle2D.Double(cx - r, sandTop, r * 2, sandHeight + 1));
        sand.intersect(new Area(bottomBulb));

        // Sand gradient
        GradientPaint gp = new GradientPaint(
                (float) cx, (float) sandTop, new Color(0xd97706), // amber-600
                (float) cx, (float) sandBottom, new Color(0x92400e)); // amber-800
        g.setPaint(gp);
        g.fill(sand);
        g.setPaint(null);
    }

    private void drawShimmer(NTxGraphics g, double cx, double cy, double r) {
        long t = System.currentTimeMillis();
        double phase = (t % 2000) / 2000.0;
        float alpha = (float) (0.3 + 0.3 * Math.sin(phase * Math.PI * 2));
        g.setColor(new Color(0xfbbf24)); // amber-400
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.fill(new Ellipse2D.Double(cx - r * 0.6, cy - r * 0.6, r * 1.2, r * 1.2));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    private void renderLabel(NTxGraphics g, double cx, double cy, NTxProgress progress) {
        String text = null;
        if (!Double.isNaN(progress.value())) {
            text = String.format("%.0f%%", progress.value() * 100);
        } else if (progress.elapsed() != null) {
            text = formatDuration(progress.elapsed());
        }
        if (text == null) return;

        g.setColor(new Color(0xe5e7eb));
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);
        g.drawString(text, (int) (cx - tw / 2.0), (int) cy);
    }

    private String formatDuration(net.thevpc.nuts.time.NDuration d) {
        long secs = d.toSeconds();
        if (secs < 60) return secs + "s";
        long mins = secs / 60;
        secs = secs % 60;
        return mins + "m " + secs + "s";
    }
}
