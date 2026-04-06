package net.thevpc.ntexup.api.renderer.text;

import net.thevpc.ntexup.api.renderer.NTxRendererContext;

import java.util.List;
import java.util.function.Function;

public interface NTxTextRendererFlavorParseContext {
    NTxRendererContext rendererContext();

    boolean hasNext();

    int length();

    char peek();

    String peek(int count);

    String read(int count);

    void skip(int count);

    char read();

    char peekAt(int index);

    List<NTxTextToken> parseDefault(String[] flavorIds, String[] startEndDelimiters, Function<String,String> converter);
}
