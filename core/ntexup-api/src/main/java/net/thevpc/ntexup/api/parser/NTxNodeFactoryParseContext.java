package net.thevpc.ntexup.api.parser;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.elem.NElement;

public interface NTxNodeFactoryParseContext {

    NTxLogger messages();

    NTxEngine engine();

    NTxNodeFactoryParseContext push(NTxNode node);
    NTxNodeFactoryParseContext push(NTxNode node,NElement element);
    NTxNodeFactoryParseContext push(NElement element);

    NTxNode node();

    NTxDocument document();

    NTxNode[] nodePath();

    NElement element();

    NTxSource source();

//    NPath resolvePath(NStringElement path);
//
//    NPath resolvePath(String path);
//
//    NPath resolvePath(NPath path);

    NTxDocumentFactory documentFactory();

    NElement asPathRef(NElement element);

}
