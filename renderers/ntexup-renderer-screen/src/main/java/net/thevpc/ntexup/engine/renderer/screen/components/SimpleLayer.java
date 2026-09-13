package net.thevpc.ntexup.engine.renderer.screen.components;

import net.thevpc.ntexup.api.document.elem2d.NTxAlign;
import net.thevpc.ntexup.api.renderer.NTxGraphics;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public abstract class SimpleLayer implements NTxDocumentLayer {

    protected void drawStr(String str, NTxAlign a, Dimension size, NTxGraphics g2d) {
        if (str == null || str.trim().isEmpty()) {
            return;
        }

        Font font = new Font("SansSerif", Font.PLAIN, 12);
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics(font);
        int textWidth = fm.stringWidth(str);
        int textHeight = fm.getHeight();
        int ascent = fm.getAscent();

        int padX = 8;
        int padY = 3;
        int pillW = textWidth + padX * 2;
        int pillH = textHeight + padY * 2;

        int pillX = 12;
        if (a == NTxAlign.RIGHT) {
            pillX = (int) (size.getWidth() - pillW - 12);
        }
        int pillY = (int) (size.getHeight() - pillH - 8);

        // Dark translucent pill background with subtle light border
        g2d.setComposite(AlphaComposite.SrcOver.derive(0.75f));
        g2d.setColor(new Color(15, 20, 28, 220));
        g2d.fillRoundRect(pillX, pillY, pillW, pillH, 6, 6);
        g2d.setColor(new Color(255, 255, 255, 70));
        g2d.drawRoundRect(pillX, pillY, pillW, pillH, 6, 6);

        // High contrast ivory text
        g2d.setColor(new Color(245, 245, 250, 245));
        g2d.drawString(str, pillX + padX, pillY + padY + ascent);
        g2d.setComposite(AlphaComposite.SrcOver.derive(1.0f));
    }
}
