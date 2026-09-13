package net.thevpc.ntexup.api.renderer.text;

import net.thevpc.ntexup.api.renderer.NTxRendererContext;

import java.util.Collections;
import java.util.List;

public interface NTxTextRendererFlavor {

    String type();

    default List<String> aliases() {
        return Collections.emptyList();
    }

    void buildText(String text, NTxTextOptions options, NTxRendererContext ctx, NTxTextRendererBuilder builder);

    List<String> getParsePrefixes();

    List<NTxTextToken> parseTokens(NTxTextRendererFlavorParseContext ctx);
}
