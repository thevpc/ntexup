package net.thevpc.ntexup.engine.renderer.screen.components;

import net.thevpc.ntexup.api.document.elem2d.NTxAlign;
import net.thevpc.ntexup.api.renderer.NTxGraphics;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public abstract class SimpleLayer implements NTxDocumentLayer {

    protected void drawStr(String str, NTxAlign a, Dimension size, NTxGraphics g2d) {

        Rectangle2D b = g2d.getStringBounds(str);

        int x = 0;
        int y = 0;
        switch (a) {
            case LEFT: {
                x = 10;
                y = (int) (size.getHeight() - b.getHeight());
                break;
            }
            case RIGHT: {
                x = (int) (size.getWidth() - b.getWidth()) - 10;
                y = (int) (size.getHeight() - b.getHeight());
                break;
            }
        }
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2d.setComposite(AlphaComposite.SrcOver.derive(0.5f));
        g2d.setColor(new Color(0x334155));
        g2d.drawString(str, x, y);
        g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f));
    }
}
