package net.thevpc.ntexup.engine.document;

import net.thevpc.ntexup.api.document.node.NTxNodePage;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.source.NTxSource;

public class DefaultNTxNodePage extends DefaultNTxNode implements NTxNodePage {
    private int index=-1;
    public DefaultNTxNodePage() {
        super(NTxNodeType.PAGE);
    }

    public DefaultNTxNodePage(NTxSource source) {
        super(NTxNodeType.PAGE,source);
    }

    public DefaultNTxNodePage setIndex(int index) {
        this.index = index;
        return this;
    }

    @Override
    public int index() {
        return index;
    }
}
