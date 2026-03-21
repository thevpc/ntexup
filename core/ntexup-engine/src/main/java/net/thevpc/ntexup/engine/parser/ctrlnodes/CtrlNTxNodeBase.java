package net.thevpc.ntexup.engine.parser.ctrlnodes;

import net.thevpc.ntexup.api.document.node.NTxNodeCtrl;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;

public abstract class CtrlNTxNodeBase extends DefaultNTxNode implements NTxNodeCtrl, Cloneable {
    public CtrlNTxNodeBase(String nodeType, NTxSource source) {
        super(nodeType,source);
    }
}
