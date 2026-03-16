package net.thevpc.ntexup.api.eval;

import net.thevpc.ntexup.api.document.node.NTxNode;

public interface NTxNodePath {
    int size();
    NTxNodePath parent();
    NTxNode node();
    NTxNodePath resolve(NTxNode other);

    boolean isRoot();
}
