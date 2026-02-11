package net.thevpc.ntexup.api.parser;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.elem.NElement;

import java.util.List;
import java.util.Map;

public interface NTxArgumentReader {
    NTxSource source();

    String getId();

    String getUid();

    NElement element();

    NTxNode node();

    int availableCount();

    List<NElement> availableArguments();

    NElement[] allArguments();

    boolean isEmpty();

    NElement peek();

    NElement read();

    NTxDocumentFactory documentFactory();

    NTxResolutionContext parseContext();

    Map<String, Object> props();

    int currentIndex();
}
