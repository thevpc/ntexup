package net.thevpc.ntexup.api.eval;

import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.nuts.util.NBlankable;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NTxFontBySizeResolver {
    public static final NTxFontBySizeResolver INSTANCE=new NTxFontBySizeResolver();

    private Map<Key, Font> cache = new HashMap<>();

    private static class Key {
        String name;
        int style;
        int wMul100;
        int hMul100;

        public Key(String name,int style, double w, double h) {
            this.name = name;
            this.style = style;
            this.wMul100 = (int)(w*100);
            this.hMul100 = (int)(h*100);
        }

        @Override
        public boolean equals(Object object) {
            if (object == null || getClass() != object.getClass()) return false;
            Key key = (Key) object;
            return style == key.style && wMul100 == key.wMul100 && hMul100 == key.hMul100 && Objects.equals(name, key.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, style, wMul100, hMul100);
        }
    }

    public Font getFont(String name, Font font,int style, double w, double h, NTxGraphics fontMetricsFunction) {
        if(!NBlankable.isBlank(name)) {
            return getFont(name, style, w, h,fontMetricsFunction);
        }else if(font!=null){
            return getFont(font, style, w, h,fontMetricsFunction);
        }else{
            return getFont(new Font("Serif",Font.PLAIN,16), style, w, h,fontMetricsFunction);
        }
    }
    public Font getFont(String name, int style, double w, double h, NTxGraphics fontMetricsFunction) {
        Key k = new Key(name, style,w,h);
        return cache.computeIfAbsent(k, r -> getFittingFont(new Font(name,Font.PLAIN,10), style, w,h, fontMetricsFunction));
    }

    public Font getFont(Font font, int style, double w, double h, NTxGraphics fontMetricsFunction) {
        Key k = new Key(font.getName(), style,w,h);
        return cache.computeIfAbsent(k, r -> getFittingFont(font, style, w,h, fontMetricsFunction));
    }

    public Font getFittingFont(Font baseFont, int style, double boxWidth, double boxHeight, NTxGraphics graphics) {
        int minSize = 1;
        int maxSize = 500;

        // Dynamically grow max size until it exceeds the bounding box
        while (true) {
            Font font = baseFont.deriveFont(style, (float) maxSize);
            FontMetrics fm = graphics.getFontMetrics(font);
            int height = fm.getAscent() + fm.getDescent();
            int width = fm.charWidth('W'); // conservative widest character

            if (height > boxHeight || width > boxWidth) break;

            maxSize *= 2;
            if (maxSize > 10000) {
                throw new IllegalStateException("Font size too large or box too small.");
            }
        }

        Font bestFont = baseFont;
        int bestSize = minSize;

        while (minSize <= maxSize) {
            int midSize = (minSize + maxSize) / 2;
            Font font = baseFont.deriveFont(style, (float) midSize);
            FontMetrics fm = graphics.getFontMetrics(font);
            int height = fm.getAscent() + fm.getDescent();
            int width = fm.charWidth('W');

            if (height <= boxHeight && width <= boxWidth) {
                bestFont = font;
                bestSize = midSize;
                minSize = midSize + 1;
            } else {
                maxSize = midSize - 1;
            }
        }
        return bestFont;
    }
}
