package net.thevpc.ntexup.extension.progress.skin;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.extension.progress.NTxProgressSkin;
import net.thevpc.ntexup.extension.progress.model.NTxProgress;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;

/**
 * Knob skin: needle/arc angle maps to value; continuous rotation when indeterminate.
 * When the bounds are too thin for a circular knob, degrades to a horizontal arc indicator.
 */
public class NTxKnobSkin implements NTxProgressSkin {

    private static final double START_ANGLE = 225;
    private static final double SWEEP_RANGE = 270;
    private static final double MIN_SIZE = 40;

    @Override
    public String id() {
        return "knob";
    }

    @Override
    public void render(NTxGraphics g, NTxBounds2D bounds, NTxProgress progress, boolean animating) {
        double w = bounds.widthX();
        double h = bounds.widthY();

        if (w < MIN_SIZE || h < MIN_SIZE) {
            renderLinear(g, bounds, progress);
            return;
        }

        double cx = bounds.minX() + w / 2.0;
        double cy = bounds.minY() + h / 2.0;
        double radius = Math.min(w, h) / 2.0;
        double pad = radius * 0.15;
        double innerR = radius - pad;

        Arc2D track = new Arc2D.Double(
                cx - radius, cy - radius, radius * 2, radius * 2,
                START_ANGLE, -SWEEP_RANGE, Arc2D.OPEN);
        g.setColor(new Color(0x374151));
        g.setStroke(new BasicStroke((float) (innerR * 0.25), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(track);

        double value = progress.value();
        boolean indet = progress.indeterminate();

        if (!Double.isNaN(value)) {
            double sweep = SWEEP_RANGE * value;
            Arc2D valueArc = new Arc2D.Double(
                    cx - radius, cy - radius, radius * 2, radius * 2,
                    START_ANGLE, -sweep, Arc2D.OPEN);
            g.setColor(new Color(0x3b82f6));
            g.setStroke(new BasicStroke((float) (innerR * 0.25), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(valueArc);
        }

        if (indet) {
            long t = System.currentTimeMillis();
            double angle = (t / 10.0) % 360;
            Arc2D spinArc = new Arc2D.Double(
                    cx - radius, cy - radius, radius * 2, radius * 2,
                    -angle, 90, Arc2D.OPEN);
            g.setColor(new Color(0x60a5fa));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
            g.setStroke(new BasicStroke((float) (innerR * 0.25), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(spinArc);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        double dotR = innerR * 0.12;
        g.setColor(new Color(0xe5e7eb));
        g.fill(new Ellipse2D.Double(cx - dotR, cy - dotR, dotR * 2, dotR * 2));

        renderLabel(g, cx, cy + radius * 0.45, progress);
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
            g.setColor(new Color(0x3b82f6));
            g.fillRoundRect((int) x, (int) y, (int) fillW, (int) h, (int) h, (int) h);
        }

        if (indet) {
            long t = System.currentTimeMillis();
            double phase = (t % 3000) / 3000.0;
            double segW = w * 0.3;
            double segX = x + (w + segW) * phase - segW;
            segX = Math.max(x, Math.min(segX, x + w - segW));
            g.setColor(new Color(0x60a5fa));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            g.fillRoundRect((int) segX, (int) y, (int) segW, (int) h, (int) h, (int) h);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }

        renderCaption(g, bounds, progress);
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
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
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
