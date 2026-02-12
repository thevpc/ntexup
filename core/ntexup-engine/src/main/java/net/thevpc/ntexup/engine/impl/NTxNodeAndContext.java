package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;

public class NTxNodeAndContext {
    public NTxNode node;
    public NTxResolutionContext context;

    public NTxNodeAndContext(NTxNode node, NTxResolutionContext context) {
        this.node = node;
        this.context = context;
    }
}
