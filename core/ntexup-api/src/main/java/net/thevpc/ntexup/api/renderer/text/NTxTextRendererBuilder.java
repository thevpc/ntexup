package net.thevpc.ntexup.api.renderer.text;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.nuts.text.NText;

public interface NTxTextRendererBuilder {
    void appendText(String rawText, NTxTextOptions options, NTxRendererContext ctx);

    void appendNText(String lang, String rawText, NText text, NTxRendererContext ctx);

    void appendCustom(String lang, String rawText, NTxTextOptions options, NTxRendererContext ctx);

    public void appendPlain(String text, NTxRendererContext ctx);

    NTxRichTextRow nextLine();

    NTxRichTextRow currRow();

    NTxBounds2D computeBound(NTxRendererContext ctx);

    public void setLang(String lang);

    public void setCode(String rawText);

    interface ImagePainter {
        void paint(NTxGraphics g, double x, double y);

        NTxDouble2 size();
    }

    public void render(NTxNode p, NTxRendererContext ctx, NTxBounds2D bgBounds, NTxBounds2D selfBounds);

    boolean isEmpty();

    void addToken(NTxRichTextToken col);
}
