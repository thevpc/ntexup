package net.thevpc.ntexup.engine.parser.nodeparsers;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.engine.parser.NTxNodeParserBase;

public class CallSpecialParser extends NTxNodeParserBase {
    public CallSpecialParser() {
        super(true, NTxNodeType.CTRL_CALL);
    }

    @Override
    public void compileNode(NTxNode node, NTxResolutionContext context) {

    }
}

