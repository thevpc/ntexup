package net.thevpc.ntexup.api.renderer.text;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;

import java.util.List;

public interface NTxTextRendererFlavor {

    String type();

    void buildText(String text, NTxTextOptions options, NTxNode p, NTxNodeRendererContext ctx, NTxTextRendererBuilder builder);

    List<String> getParsePrefixes();

    List<NTxTextToken> parseTokens(NTxTextRendererFlavorParseContext ctx);
}
