package net.thevpc.ntexup.api.renderer.text;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.NTxShadow;
import net.thevpc.ntexup.api.document.elem2d.NTxSize;
import net.thevpc.ntexup.api.eval.NTxFontBySizeResolver;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.util.NTxSizeRef;
import net.thevpc.ntexup.api.util.NtxFontInfo;
import net.thevpc.nuts.util.NBlankable;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.text.AttributedString;

public class NTxTextOptions implements Cloneable, NBlankable {
    public NtxFontInfo defaultFont;
    public Paint backgroundColor;
    public Integer foregroundColorIndex;
    public Integer backgroundColorIndex;
    public Paint foregroundColor;
    public Boolean bold;
    public Boolean italic;
    public NTxSize fontSize;
    public String fontFamily;
    public Boolean underlined;
    public Boolean strikeThrough;
    public Font baseFont;
    public NTxShadow shadow;
    public Stroke stroke;
    public NTxSizeRef sr;
    public Font computedFont;

    @Override
    public boolean isBlank() {
        if (backgroundColor != null) {
            return false;
        }
        if (foregroundColorIndex != null) {
            return false;
        }
        if (backgroundColorIndex != null) {
            return false;
        }
        if (bold != null) {
            return false;
        }
        if (italic != null) {
            return false;
        }
        if (fontSize != null) {
            return false;
        }
        if (fontFamily != null) {
            return false;
        }
        if (underlined != null) {
            return false;
        }
        if (strikeThrough != null) {
            return false;
        }
        if (baseFont != null) {
            return false;
        }
        if (shadow != null) {
            return false;
        }
        if (stroke != null) {
            return false;
        }

        return true;
    }

    public Integer getBackgroundColorIndex() {
        return backgroundColorIndex;
    }

    public NTxTextOptions setBackgroundColorIndex(Integer backgroundColorIndex) {
        this.backgroundColorIndex = backgroundColorIndex;
        return this;
    }

    public Integer getForegroundColorIndex() {
        return foregroundColorIndex;
    }

    public NTxTextOptions setForegroundColorIndex(Integer foregroundColorIndex) {
        this.foregroundColorIndex = foregroundColorIndex;
        return this;
    }

    public Boolean getBold() {
        return bold;
    }

    public NTxTextOptions setBold(Boolean bold) {
        this.bold = bold;
        return this;
    }

    public Boolean getItalic() {
        return italic;
    }

    public NTxTextOptions setItalic(Boolean italic) {
        this.italic = italic;
        return this;
    }

    public NTxSize getFontSize() {
        return fontSize;
    }

    public NTxTextOptions setFontSize(NTxSize fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public NTxTextOptions setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
        return this;
    }

    public Boolean getUnderlined() {
        return underlined;
    }

    public NTxTextOptions setUnderlined(Boolean underlined) {
        this.underlined = underlined;
        return this;
    }

    public Boolean getStrikeThrough() {
        return strikeThrough;
    }

    public NTxTextOptions setStrikeThrough(Boolean strikeThrough) {
        this.strikeThrough = strikeThrough;
        return this;
    }

    public Stroke getStroke() {
        return stroke;
    }

    public NTxTextOptions setStroke(Stroke stroke) {
        this.stroke = stroke;
        return this;
    }

    public Paint getBackgroundColor() {
        return backgroundColor;
    }

    public NTxTextOptions setBackgroundColor(Paint backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public Paint getForegroundColor() {
        return foregroundColor;
    }

    public NTxTextOptions setForegroundColor(Paint foregroundColor) {
        this.foregroundColor = foregroundColor;
        return this;
    }


    public Font getBaseFont() {
        return baseFont;
    }

    public Font getComputedFont() {
        return computedFont;
    }

    public NTxTextOptions setBaseFont(Font baseFont) {
        this.baseFont = baseFont;
        return this;
    }

    public NTxShadow getShadow() {
        return shadow;
    }

    public NTxTextOptions setShadow(NTxShadow shadow) {
        this.shadow = shadow;
        return this;
    }

    public boolean isStyled() {
        if (underlined != null) {
            return true;
        }
        if (strikeThrough != null) {
            return true;
        }
        if (foregroundColorIndex != null) {
            return true;
        }
        if (backgroundColorIndex != null) {
            return true;
        }
        if (foregroundColor != null) {
            return true;
        }
        if (bold != null) {
            return true;
        }
        if (italic != null) {
            return true;
        }
        if (fontSize != null) {
            return true;
        }
        if (fontFamily != null) {
            return true;
        }
        if (backgroundColor != null) {
            return true;
        }
        if (shadow != null) {
            return true;
        }
        return false;
    }

    public AttributedString createAttributedString(String text, NTxGraphics g) {
        AttributedString attributedString = new AttributedString(text);
        int strLen = text.length();
        if (underlined != null && underlined) {
            attributedString.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, 0, strLen);
        }
        if (strikeThrough != null && strikeThrough) {
            attributedString.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON, 0, strLen);
        }
        if (foregroundColor != null) {
            attributedString.addAttribute(TextAttribute.FOREGROUND, foregroundColor, 0, strLen);
        }
        if (backgroundColor != null) {
            attributedString.addAttribute(TextAttribute.BACKGROUND, backgroundColor, 0, strLen);
        }
        applyFont(attributedString, g);
        return attributedString;
    }

    public Font resolveFont(NTxGraphics g) {
        return resolveFont(g, false);
    }

    public Font resolveFont(NTxGraphics g, boolean apply) {
        if (computedFont != null) {
            return computedFont;
        }
        if (defaultFont == null) {
            throw new RuntimeException("missing default font");
        }
        Font baseFont = this.baseFont;
        if (baseFont == null) {
            baseFont = defaultFont.baseFont;
        }

        String fontFamily = this.fontFamily;
        if (NBlankable.isBlank(fontFamily)) {
            fontFamily = defaultFont.family;
        }
        Boolean bold = this.bold;
        if (bold == null && defaultFont.bold != null) {
            bold = defaultFont.bold;
        }
        if (bold == null) {
            if (baseFont != null) {
                bold = baseFont.isBold();
            } else {
                bold = false;
            }
        }
        Boolean italic = this.italic;
        if (italic == null && defaultFont.italic != null) {
            italic = defaultFont.italic;
        }
        if (italic == null) {
            if (baseFont != null) {
                italic = baseFont.isItalic();
            } else {
                italic = false;
            }
        }
        NTxSizeRef sr = this.sr;
        if (sr == null) {
            throw new IllegalArgumentException("sr is null");
        }
        NTxSize size = this.fontSize;
        if (size == null) {
            size = defaultFont.size;
        }
        double w = sr.x(size).orElse(16.0);
        double h = sr.y(size).orElse(16.0);
        int newStyle = Font.PLAIN | (italic ? Font.ITALIC : 0) | (bold ? Font.BOLD : 0);
        Font newFont = NTxFontBySizeResolver.INSTANCE.getFont(fontFamily, baseFont, newStyle, w, h, g);
        if (apply) {
            this.italic = italic;
            this.bold = bold;
            this.fontFamily = fontFamily;
            this.fontSize = size;
            this.computedFont = newFont;
        }
        return newFont;
    }

    private void applyFont(AttributedString attributedString, NTxGraphics g) {
        Font f = resolveFont(g, false);
        attributedString.addAttribute(TextAttribute.FONT, f);
    }

//    public AttributedString createShadowAttributedString(String text, NTxGraphics g) {
//        AttributedString attributedString = new AttributedString(text);
//        int strLen = text.length();
//        if (underlined != null && underlined) {
//            attributedString.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, 0, strLen);
//        }
//        if (strikeThrough != null && strikeThrough) {
//            attributedString.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON, 0, strLen);
//        }
//        if (shadowColor != null) {
//            attributedString.addAttribute(TextAttribute.FOREGROUND, shadowColor, 0, strLen);
//        }
//        if (backgroundColor != null) {
//            attributedString.addAttribute(TextAttribute.BACKGROUND, backgroundColor, 0, strLen);
//        }
//        applyFont(attributedString, g);
//        return attributedString;
//    }

    public NTxTextOptions copyNonNullFrom(NTxTextOptions other) {
        if (other != null) {
            if (other.backgroundColor != null) {
                this.backgroundColor = other.backgroundColor;
            }
            if (other.foregroundColor != null) {
                this.foregroundColor = other.foregroundColor;
            }
            if (other.underlined != null) {
                this.underlined = other.underlined;
            }
            if (other.italic != null) {
                this.italic = other.italic;
            }
            if (other.bold != null) {
                this.bold = other.bold;
            }
            if (other.fontSize != null) {
                this.fontSize = other.fontSize;
            }
            if (other.fontFamily != null) {
                this.fontFamily = other.fontFamily;
            }
            if (other.strikeThrough != null) {
                this.strikeThrough = other.strikeThrough;
            }
            if (other.baseFont != null) {
                this.baseFont = other.baseFont;
            }
            if (other.shadow != null) {
                this.shadow = other.shadow;
            }
        }
        return this;
    }

    public NTxTextOptions copy() {
        return clone();
    }

    @Override
    protected NTxTextOptions clone() {
        try {
            return (NTxTextOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
