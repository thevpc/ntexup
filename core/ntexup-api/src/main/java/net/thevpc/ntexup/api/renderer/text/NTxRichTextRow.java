package net.thevpc.ntexup.api.renderer.text;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class NTxRichTextRow {

    public List<NTxRichTextToken> tokens = new ArrayList<>();
    public double yOffset;
    public Rectangle2D textBounds;
    public double maxAscent;
    public double maxDescent;

    public void addToken(NTxRichTextToken r) {
        if (r != null) {
            tokens.add(r);
        }
    }
}
