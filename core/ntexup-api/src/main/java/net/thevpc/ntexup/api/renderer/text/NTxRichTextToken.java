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

    public NTxRichTextToken(NTxRichTextTokenType type, String text) {
        this.type = type;
        this.text = text;
    }


}
