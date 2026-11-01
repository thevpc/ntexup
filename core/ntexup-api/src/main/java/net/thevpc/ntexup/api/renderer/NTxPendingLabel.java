package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.nuts.elem.NElement;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The animated label a node shows over its bounds while its content waits on a
 * background computation. Customizable through the {@code pending-text}
 * (false hides it), {@code pending-color} and {@code pending-background} properties.
 */
final class NTxPendingLabel {
    private static final double SPINNER_SIZE = 16;
    private static final double LABEL_HEIGHT = 36;

    private NTxPendingLabel() {
    }

    static void draw(NTxRendererContext ctx, String defaultText) {
        // Requested even when the label is hidden: the frame is also what lets
        // the node notice that the computation has finished.
        ctx.requestAnimationFrame(true);
        String text = resolveText(ctx, defaultText);
        if (text == null) {
            return;
        }
        Paint foreground = NTxValueByName.getColorProperty(NTxPropName.PENDING_COLOR, ctx).orElse(Color.WHITE);
        Paint background = NTxValueByName.getColorProperty(NTxPropName.PENDING_BACKGROUND, ctx).orElse(new Color(0x0f, 0x17, 0x2a, 217));
        NTxBounds2D bounds = ctx.selfBounds2D();

        Graphics2D g = (Graphics2D) ctx.graphics().graphics2D().create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            double w = fm.stringWidth(text) + SPINNER_SIZE + 42;
            double x = bounds.minX() + (bounds.widthX() - w) / 2.0;
            double y = bounds.minY() + (bounds.widthY() - LABEL_HEIGHT) / 2.0;
            g.setPaint(background);
            g.fill(new RoundRectangle2D.Double(x, y, w, LABEL_HEIGHT, 18, 18));

            double sx = x + 14;
            double sy = y + (LABEL_HEIGHT - SPINNER_SIZE) / 2.0;
            double angle = ctx.pageElapsedSeconds() * 360.0;
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 64));
            g.draw(new Ellipse2D.Double(sx, sy, SPINNER_SIZE, SPINNER_SIZE));
            g.setColor(new Color(0x38bdf8));
            g.draw(new Arc2D.Double(sx, sy, SPINNER_SIZE, SPINNER_SIZE, -angle, 120, Arc2D.OPEN));

            g.setPaint(foreground);
            g.drawString(text, (float) (sx + SPINNER_SIZE + 12), (float) (y + (LABEL_HEIGHT + fm.getAscent() - fm.getDescent()) / 2.0));
        } finally {
            g.dispose();
        }
    }

    private static String resolveText(NTxRendererContext ctx, String defaultText) {
        NElement e = ctx.computePropertyValue(NTxPropName.PENDING_TEXT).orNull();
        if (e == null || e.isNull()) {
            return defaultText;
        }
        if (e.isBoolean()) {
            return e.asBooleanValue().orElse(true) ? defaultText : null;
        }
        return e.asStringValue().orElse(defaultText);
    }
}
