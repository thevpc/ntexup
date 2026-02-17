package net.thevpc.ntexup.engine.renderer.screen.components;

import net.thevpc.ntexup.engine.renderer.screen.DocumentView;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.engine.renderer.screen.PageView;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

public class RuntimeProgressLayer implements NTxDocumentLayer {
    public void draw(DocumentView doc, PageView pageView, Dimension size, NTxGraphics g) {
        if (doc.isPageLoading()) {
            double w2 = 200;
            double w1 = 100;
            double x0 = (int) size.getWidth() / 2 /*- w2 / 2 - 5*/;
            double y0 = (int) size.getHeight() / 2 /*- w2 / 2 - 5*/;
            long v = System.currentTimeMillis() / 10;
            double to = (v % 360) / 360.0 * 100;
            double from = to/2;
            drawProgress(null, new Color(0xff5500), from, to, x0, y0, w1, w2, g);
        }
    }

    private void drawProgress(String str, Color color, double offset, double percent, double x0, double y0, double w1, double w2, NTxGraphics g) {
        g.setColor(Color.GRAY);
//        g.setColor(Color.RED);
        Color cc = getColorAt((int) percent);
        g.setColor(cc);
        g.setComposite(AlphaComposite.SrcOver.derive(0.5f));
        Area outerCircle = new Area(new Arc2D.Double(
                (x0 - w2 / 2),
                (y0 - w2 / 2),
                w2,
                w2,
                90- (int) (360.0 * (offset / 100)),
                0 - (int) (360.0 * (percent / 100)),
                Arc2D.PIE
        ));
        Area innerCircle = new Area(new Ellipse2D.Double(
                (int) (x0 - w1 / 2),
                (int) (y0 - w1 / 2),
                (int) w1,
                (int) w1
        ));

        outerCircle.subtract(innerCircle);
        g.fill(outerCircle);
        if (str != null && str.length() > 0) {
            Rectangle2D b = g.getStringBounds(str);
            g.setComposite(AlphaComposite.SrcOver.derive(1f));
            g.setColor(Color.BLACK);
            g.drawString(
                    str,
                    (int) (x0 - b.getWidth() / 2),
                    (int) (y0 + b.getHeight() / 2)
            );

            cc = getColorAt((int) percent);
            g.setColor(cc);
            g.setSecondaryColor(getComplementColor(cc));
        }
//        g.fillSphere(100, 100, 10, 10, 45, 0.6f);
    }

    public static Color getColorAt(int x) {
        if (x < 0) x = 0;
        if (x > 100) x = 100;
        float hue = x / 100f;  // Map 0–100 to 0.0–1.0
        return Color.getHSBColor(hue, 1.0f, 1.0f); // Full saturation and brightness
    }

    public static Color getComplementColor(Color c) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        float complementHue = (hsb[0] + 0.5f) % 1.0f; // Add 180° (0.5 in normalized hue)
        return Color.getHSBColor(complementHue, hsb[1], hsb[2]);
    }
}
