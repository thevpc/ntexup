package net.thevpc.ntexup.engine.ext;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.renderer.text.*;

import java.util.*;

class CustomNTxTextRendererFlavorFromBuilder implements NTxTextRendererFlavor {
    private final NTxNodeBuilderContextImpl ctx;
    private final List<String> parsePrefixes;

    public CustomNTxTextRendererFlavorFromBuilder(NTxNodeBuilderContextImpl ctx) {
        this.ctx = ctx;
        LinkedHashSet<String> a = new LinkedHashSet<>();
        for (String s : ctx.idAndAliases()) {
            a.add("[[" + s + ":");
        }
        if (this.ctx.renderTextAction.renderEmbeddedTextStartSeparators != null) {
            for (String s : this.ctx.renderTextAction.renderEmbeddedTextStartSeparators) {
                a.add(s);
            }
        }
        parsePrefixes = Collections.unmodifiableList(new ArrayList<>(a));
    }

    @Override
    public String type() {
        return ctx.id;
    }

    @Override
    public void buildText(String text, NTxTextOptions options, NTxNode p, NTxNodeRendererContext ctx, NTxTextRendererBuilder builder) {
        if (this.ctx.renderTextAction.buildAction != null) {
            this.ctx.renderTextAction.buildAction.buildText(text, options, p, ctx.withBuilderContext(this.ctx), builder);
        }
    }


    @Override
    public List<String> getParsePrefixes() {
        return parsePrefixes;
    }

    @Override
    public List<NTxTextToken> parseTokens(NTxTextRendererFlavorParseContext ctx) {
        if (this.ctx.renderTextAction.parseTokensAction != null) {
            List<NTxTextToken> u = this.ctx.renderTextAction.parseTokensAction.parseTokens(ctx, this.ctx);
            if (u != null) {
                return u;
            }
        }
        return ctx.parseDefault(this.ctx.idAndAliases(), new String[0], null);
    }

    @Override
    public String toString() {
        return "CustomFlavorFromBuilder(" + ctx.id() + ")";
    }
}
