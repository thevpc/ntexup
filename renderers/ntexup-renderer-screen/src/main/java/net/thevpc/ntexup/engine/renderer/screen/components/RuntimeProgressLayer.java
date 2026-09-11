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
        if (!doc.isPageLoading()) {
            return;
        }
        double cx = size.getWidth() / 2.0;
        double cy = size.getHeight() / 2.0;
        double boxW = 140;
        double boxH = 44;

        // Background pill
        g.setComposite(AlphaComposite.SrcOver.derive(0.85f));
        g.setColor(new Color(0x0f172a)); // Deep slate
        g.fillRoundRect(cx - boxW / 2.0, cy - boxH / 2.0, boxW, boxH, 22, 22);

        // Spinner circle
        double spinnerSize = 20;
        double spinnerX = cx - boxW / 2.0 + 16;
        double spinnerY = cy - spinnerSize / 2.0;
        long angle = (System.currentTimeMillis() / 3) % 360;

        // Track
        g.setComposite(AlphaComposite.SrcOver.derive(0.25f));
        g.setColor(Color.WHITE);
        g.drawArc(spinnerX, spinnerY, spinnerSize, spinnerSize, 0, 360);

        // Spinning Arc
        g.setComposite(AlphaComposite.SrcOver.derive(0.95f));
        g.setColor(new Color(0x38bdf8)); // Light sky blue
        g.drawArc(spinnerX, spinnerY, spinnerSize, spinnerSize, (int) -angle, 120);

        // Text
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("Loading...", (int) (spinnerX + spinnerSize + 10), (int) (cy + 5));
        g.setComposite(AlphaComposite.SrcOver.derive(1.0f));
    }
}
