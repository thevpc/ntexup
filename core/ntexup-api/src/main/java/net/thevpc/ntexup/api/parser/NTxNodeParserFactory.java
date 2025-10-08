package net.thevpc.ntexup.api.parser;

import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.nuts.concurrent.NScorableCallable;

/**
 * @author vpc
 */
public interface NTxNodeParserFactory {

    NScorableCallable<NTxItem> parseNode(NTxNodeFactoryParseContext context);

}
