package net.thevpc.ntexup.api.util;

import net.thevpc.ntexup.api.document.elem2d.NTxSize;

import java.awt.*;

public class NtxFontInfo {
    public NTxSize size;
    public Font baseFont;
    public Boolean italic;
    public Boolean bold;
    public String family;

    public NtxFontInfo() {
    }

    public NtxFontInfo(NTxSize size, Font baseFont, Boolean italic, Boolean bold, String family) {
        this.size = size;
        this.baseFont = baseFont;
        this.italic = italic;
        this.bold = bold;
        this.family = family;
    }

    public NtxFontInfo copy() {
        return new NtxFontInfo(size, baseFont, italic, bold, family);
    }

    public NtxFontInfo applyDefaults(NtxFontInfo defaultFont) {
        if (defaultFont != null) {
            if (this.size == null && defaultFont.size != null) {
                this.size = defaultFont.size;
            }
            if (this.baseFont == null && defaultFont.baseFont != null) {
                this.baseFont = defaultFont.baseFont;
            }
            if (this.italic == null && defaultFont.italic != null) {
                this.italic = defaultFont.italic;
            }
            if (this.bold == null && defaultFont.bold != null) {
                this.bold = defaultFont.bold;
            }
            if (this.family == null && defaultFont.family != null) {
                this.family = defaultFont.family;
            }
        }
        return this;
    }
}
