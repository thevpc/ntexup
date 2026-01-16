package net.thevpc.ntexup.extension.shapes2d.text;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.renderer.text.NTxTextOptions;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererBuilder;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NTextCode;
import net.thevpc.nuts.text.NTexts;

public class NTxSourceBuilder implements NTxNodeBuilder {

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.SOURCE)
                .parseParam().matchesName().matchesLeading().storeName(NTxPropName.LANG).then()
                .parseParam().matchesNamedPair("text-path").then()
                .parseParam().matchesNamedPair(NTxPropName.VALUE,NTxPropName.FILE,NTxPropName.LANG).then()
                .parseParam().matchesAnyNonPair().storeFirstMissingName(NTxPropName.VALUE).then()
                .renderText().buildText((text, options, p, rendererContext, builder) -> renderTextBuildText(text, options, p, rendererContext, builder))
        ;
    }

    private void renderTextBuildText(String text, NTxTextOptions options, NTxNode p, NTxNodeRendererContext rendererContext, NTxTextRendererBuilder builder) {
        NTexts ttt = NTexts.of();
        NElement lng = p.getPropertyValue(NTxPropName.LANG).orNull();
        String lang = NTxValue.of(lng).asString().orNull();
        text=rendererContext.engine().tools().trimBloc(text);
        NTextCode ncode = ttt.ofCode(lang, text);
        rendererContext.highlightNutsText(lang, text, ncode, p, builder);
    }

}
