package net.thevpc.ntexup.api.parser;

import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.nuts.concurrent.NScoredCallable;

/**
 * @author vpc
 */
public interface NTxNodeParserFactory {

    NScoredCallable<NTxItem> parseNode(NTxNodeFactoryParseContext context);

}
