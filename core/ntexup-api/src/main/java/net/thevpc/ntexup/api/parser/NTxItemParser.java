package net.thevpc.ntexup.api.parser;

import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.nuts.util.NOptional;

public interface NTxItemParser {
    NOptional<NTxNodeParser> nodeTypeParser(String id);
}
