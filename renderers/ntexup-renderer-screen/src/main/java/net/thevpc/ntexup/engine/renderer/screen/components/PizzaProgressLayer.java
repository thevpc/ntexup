package net.thevpc.ntexup.engine.renderer.screen.components;

import net.thevpc.ntexup.engine.renderer.screen.DocumentView;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.engine.renderer.screen.PageView;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

public class PizzaProgressLayer implements NTxDocumentLayer {
    public void draw(DocumentView doc, PageView pageView, Dimension size, NTxGraphics g) {
        if (doc.isLoading() || doc.getPagesCount() <= 0) {
            return;
        }
        int total = doc.getPagesCount();
        int current = doc.getPageIndex() + 1;
        double w = size.getWidth();
        double h = size.getHeight();

        // Sleek bottom progress bar (3px high)
        double progressWidth = (w * current) / (double) total;

        // Subtle background track
        g.setComposite(AlphaComposite.SrcOver.derive(0.3f));
        g.setColor(new Color(0x64748b));
        g.fillRect(0, h - 3, w, 3);

        // Accent progress fill (sapphire blue)
        g.setComposite(AlphaComposite.SrcOver.derive(0.9f));
        g.setColor(new Color(0x2563eb));
        g.fillRect(0, h - 3, progressWidth, 3);
        g.setComposite(AlphaComposite.SrcOver.derive(1.0f));
    }
}
