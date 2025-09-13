/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.engine.base.nodes.text;

import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.renderer.text.NTxRichTextToken;
import net.thevpc.ntexup.api.renderer.text.NTxRichTextTokenType;
import net.thevpc.ntexup.api.renderer.text.NTxTextOptions;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererBuilder;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NStringUtils;

/**
 * @author vpc
 */
public class NTxPlainTextBuilder implements NTxNodeBuilder {

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.PLAIN)
                .parseParam().matchesNamedPair(NTxPropName.VALUE,NTxPropName.FILE).then()
                .parseParam().matchesNamedPair("text-path").then()
                .parseParam().matchesAnyNonPair().storeFirstMissingName(NTxPropName.VALUE).then()
                .renderText().buildText(this::renderTextBuildText)
        ;
    }

    private void renderTextBuildText(String text, NTxTextOptions options, NTxNode p, NTxNodeRendererContext rendererContext, NTxTextRendererBuilder builder, NTxNodeBuilderContext buildContext) {
//        Paint fg = rendererContext.getForegroundColor(p,true);
        NElement d = p.getPropertyValue(NTxPropName.VALUE).orElse(NElement.ofString(""));

        String message = NStringUtils.trim(d.asStringValue().get());
        String[] allLines = message.trim().split("[\n]");
        for (int i = 0; i < allLines.length; i++) {
            allLines[i] = allLines[i].trim();
            NTxRichTextToken c = new NTxRichTextToken(NTxRichTextTokenType.PLAIN, allLines[i]);
            NTxGraphics g = rendererContext.graphics();
            g.setFont(c.textOptions.baseFont);
            c.bounds = g.getStringBounds(c.text);
            builder.nextLine().addToken(c);
        }
    }

}
