package net.thevpc.ntexup.extension.shapes2d.text;

import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.renderer.text.NTxTextOptions;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererBuilder;
import net.thevpc.nuts.text.NText;

public class NTxNtfBuilder implements NTxNodeBuilder {

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.NTF)
                .parseParam().matchesNamedPair("text-path").then()
                .parseParam().matchesNamedPair(NTxPropName.VALUE,NTxPropName.FILE).then()
                .parseParam().matchesAnyNonPair().storeFirstMissingName(NTxPropName.VALUE).then()
                .renderText().buildText(this::renderTextBuildText)
        ;
    }

    private void renderTextBuildText(String text, NTxTextOptions options, NTxRendererContext rendererContext, NTxTextRendererBuilder builder) {
        rendererContext.highlightNutsText("ntf", text, NText.of(text), builder);
    }

}
