package net.thevpc.ntexup.api.parser;

import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.nuts.concurrent.NCallableSupport;
import net.thevpc.nuts.elem.NElement;

public interface NTxNodeParser {
    void init(NTxEngine engine);

    String id();

    boolean isContainer();

    String[] aliases();

    NCallableSupport<NTxItem> parseNode(NTxNodeFactoryParseContext context);

    NElement toElem(NTxNode item);

    NTxNode newNode();

    default boolean validateNode(NTxNode node) {
        return false;
    }
}
