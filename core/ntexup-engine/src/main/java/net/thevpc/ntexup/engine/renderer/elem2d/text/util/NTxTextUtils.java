package net.thevpc.ntexup.engine.renderer.elem2d.text.util;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class NTxTextUtils {
    public static void drawThrowable(Throwable str, Rectangle2D.Double bounds, Graphics2D g2d) {
        if (str == null) {
            return;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos);
        str.printStackTrace(ps);
        ps.flush();
        drawString(bos.toString(), bounds, g2d);
    }

    public static void drawString(String str, Rectangle2D.Double bounds, Graphics2D g2d) {
        if (str == null) {
            str = "";
        }
        double x = bounds.getMinX();
        double y = bounds.getMinY();
        FontMetrics fm = g2d.getFontMetrics(g2d.getFont());
        for (String line : str.split("\n")) {
            g2d.drawString(line, (float) x, (float) y);
            Rectangle2D b = fm.getStringBounds(line, g2d);
            y = y + b.getHeight();
        }
    }

    public static java.util.List<String> splitWordsAndSpaces(String text) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }
        StringBuilder sb = new StringBuilder();
        boolean inSpace = Character.isWhitespace(text.charAt(0)) && text.charAt(0) != '\u00A0';
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean isSpace = Character.isWhitespace(c) && c != '\u00A0';
            if (isSpace == inSpace) {
                sb.append(c);
            } else {
                if (sb.length() > 0) {
                    result.add(sb.toString());
                    sb.setLength(0);
                }
                sb.append(c);
                inSpace = isSpace;
            }
        }
        if (sb.length() > 0) {
            result.add(sb.toString());
        }
        return result;
    }
}

