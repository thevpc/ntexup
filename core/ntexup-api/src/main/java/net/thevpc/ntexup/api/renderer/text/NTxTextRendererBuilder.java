package net.thevpc.ntexup.api.renderer.text;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.nuts.text.NText;

public interface NTxTextRendererBuilder {
    void appendText(String rawText, NTxTextOptions options, NTxNode node, NTxNodeRendererContext ctx);

    void appendNText(String lang, String rawText, NText text, NTxNode node, NTxNodeRendererContext ctx);

    void appendCustom(String lang, String rawText, NTxTextOptions options, NTxNode node, NTxNodeRendererContext ctx);

    public void appendPlain(String text, NTxNodeRendererContext ctx);

    NTxRichTextRow nextLine();

    NTxRichTextRow currRow();

    NTxBounds2D computeBound(NTxNodeRendererContext ctx);

    public void setLang(String lang);

    public void setCode(String rawText);

    interface ImagePainter {
        void paint(NTxGraphics g, double x, double y);

        NTxDouble2 size();
    }

    public void render(NTxNode p, NTxNodeRendererContext ctx, NTxBounds2D bgBounds, NTxBounds2D selfBounds);

    boolean isEmpty();

    void addToken(NTxRichTextToken col);
}
