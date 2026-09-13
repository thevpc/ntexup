package net.thevpc.ntexup.api.renderer.text;

import net.thevpc.nuts.text.NText;

import java.awt.geom.Rectangle2D;

public class NTxRichTextToken {

    public NTxRichTextTokenType type;
    public double xOffset;
    public NText tok;
    public Rectangle2D bounds;

    public String text;
    public NTxTextOptions textOptions=new NTxTextOptions();
    public NTxTextRendererBuilder.ImagePainter imagePainter;
    public double ascent;
    public double descent;
    public boolean whitespace;

    public NTxRichTextToken(NTxRichTextTokenType type, String text) {
        this.type = type;
        this.text = text;
        this.whitespace = text != null && !text.isEmpty() && text.trim().isEmpty();
    }

    public NTxRichTextToken(NTxRichTextTokenType type, String text, boolean whitespace) {
        this.type = type;
        this.text = text;
        this.whitespace = whitespace;
    }

    public boolean isWhitespace() {
        return whitespace || (text != null && !text.isEmpty() && text.trim().isEmpty());
    }


}
