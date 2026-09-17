package net.thevpc.ntexup.extension.progress.skin;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.extension.progress.NTxProgressSkin;
import net.thevpc.ntexup.extension.progress.model.NTxProgress;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

/**
 * Sandglass skin: sand level reflects value; shimmer/glow overlay when indeterminate.
 * When bounds are too thin, degrades to a horizontal bar indicator.
 */
public class NTxSandglassSkin implements NTxProgressSkin {

    private static final double MIN_SIZE = 40;

    @Override
    public String id() {
        return "sandglass";
    }

    @Override
    public void render(NTxGraphics g, NTxBounds2D bounds, NTxProgress progress, boolean animating) {
        double w = bounds.widthX();
        double h = bounds.widthY();

        if (w < MIN_SIZE || h < MIN_SIZE) {
            renderLinear(g, bounds, progress);
            return;
        }

        double x = bounds.minX();
        double y = bounds.minY();
        double cx = x + w / 2.0;
        double cy = y + h / 2.0;
        double bulbR = Math.min(w, h) * 0.35;

        drawGlassOutline(g, cx, cy, bulbR);

        double value = progress.value();
        if (!Double.isNaN(value)) {
            drawSandLevel(g, cx, cy, bulbR, value);
        }

        if (progress.indeterminate()) {
            drawShimmer(g, cx, cy, bulbR);
        }

        renderLabel(g, cx, y + h - bulbR * 0.3, progress);
    }

    private void renderLinear(NTxGraphics g, NTxBounds2D bounds, NTxProgress progress) {
        double x = bounds.minX();
        double y = bounds.minY();
        double w = bounds.widthX();
        double h = bounds.widthY();

        g.setColor(new Color(0x374151));
        g.fillRoundRect((int) x, (int) y, (int) w, (int) h, (int) h, (int) h);

        double value = progress.value();
        boolean indet = progress.indeterminate();

        if (!Double.isNaN(value)) {
            double fillW = Math.max(h, w * value);
            g.setColor(new Color(0xd97706));
            g.fillRoundRect((int) x, (int) y, (int) fillW, (int) h, (int) h, (int) h);
        }

        if (indet) {
            long t = System.currentTimeMillis();
            double phase = (t % 3000) / 3000.0;
            double segW = w * 0.3;
            double segX = x + (w + segW) * phase - segW;
            segX = Math.max(x, Math.min(segX, x + w - segW));
            g.setColor(new Color(0xfbbf24));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            g.fillRoundRect((int) segX, (int) y, (int) segW, (int) h, (int) h, (int) h);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        renderCaption(g, bounds, progress);
    }

    private void drawGlassOutline(NTxGraphics g, double cx, double cy, double r) {
        g.setColor(new Color(0x9ca3af));
        g.setStroke(new BasicStroke(2.0f));
        g.draw(new Ellipse2D.Double(cx - r, cy - r * 2.2, r * 2, r * 2));
        g.draw(new Ellipse2D.Double(cx - r, cy + r * 0.2, r * 2, r * 2));
        g.drawLine((int) cx, (int) (cy - r * 0.2), (int) cx, (int) (cy + r * 0.2));
    }

    private void drawSandLevel(NTxGraphics g, double cx, double cy, double r, double value) {
        double sandHeight = r * 1.8 * value;
        double sandBottom = cy + r * 2.0;
        double sandTop = sandBottom - sandHeight;

        Ellipse2D bottomBulb = new Ellipse2D.Double(cx - r, cy + r * 0.2, r * 2, r * 2);

        Area sand = new Area(new Rectangle2D.Double(cx - r, sandTop, r * 2, sandHeight + 1));
        sand.intersect(new Area(bottomBulb));

        GradientPaint gp = new GradientPaint(
                (float) cx, (float) sandTop, new Color(0xd97706),
                (float) cx, (float) sandBottom, new Color(0x92400e));
        g.setPaint(gp);
        g.fill(sand);
        g.setPaint(null);
    }

    private void drawShimmer(NTxGraphics g, double cx, double cy, double r) {
        long t = System.currentTimeMillis();
        double phase = (t % 2000) / 2000.0;
        float alpha = (float) (0.3 + 0.3 * Math.sin(phase * Math.PI * 2));
        g.setColor(new Color(0xfbbf24));
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
        return mins + "m " + secs + "s";
    }
}
