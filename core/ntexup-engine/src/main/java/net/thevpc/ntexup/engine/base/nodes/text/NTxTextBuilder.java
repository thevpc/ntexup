/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.engine.base.nodes.text;

import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.renderer.*;
import net.thevpc.ntexup.api.renderer.text.*;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.collections.NCharQueue;

import java.util.*;

/**
 * @author vpc
 */
public class NTxTextBuilder implements NTxNodeBuilder {

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.TEXT)
                .parseAny(x -> true)
                .parseParam().matchesNamedPair(NTxPropName.VALUE,NTxPropName.FILE).then()
                .parseParam().matchesNamedPair("text-path").then()
                .parseParam().matchesAnyNonPair().storeFirstMissingName(NTxPropName.VALUE).then()
                .renderText().buildText(this::buildText)
                .parseTokens(this::parseTokens)
        ;
    }

    public void buildText(String text, NTxTextOptions options, NTxRendererContext ctx, NTxTextRendererBuilder builder) {
        NTxTextTokenParseHelper aa = new NTxTextTokenParseHelper(ctx, NCharQueue.of(ctx.engine().tools().trimBloc(text).toCharArray()), ctx.buildContext());
        List<NTxTextToken> all = aa.parse();
        for (NTxTextToken a : all) {
            consumeSpecialTokenType(a, ctx, builder);
        }
    }

    private void consumeSpecialTokenType(NTxTextToken a, NTxRendererContext ctx, NTxTextRendererBuilder builder) {
        if (a == null) {
            return;
        }
        if (a instanceof NTxTextTokenFlavored) {
            NTxTextTokenFlavored b = (NTxTextTokenFlavored) a;
            // interpolation is the to be done by the flavor implementation
            builder.appendCustom(b.flavor(), b.value(), b.options(), ctx);
        } else {
            NTxTextTokenText b = (NTxTextTokenText) a;
            // apply interpolation
            String txt2 = ctx.evalExpression(NElement.ofString(b.value())).flatMap(NElement::asStringValue).get();
            builder.appendText(txt2, b.options(), ctx);
        }
    }

    public List<NTxTextToken> parseTokens(NTxTextRendererFlavorParseContext ctx, NTxNodeBuilderContext builderContext) {
        NTxTextTokenParseHelper aa = new NTxTextTokenParseHelper(ctx, builderContext);
        return aa.parse();
    }


}
