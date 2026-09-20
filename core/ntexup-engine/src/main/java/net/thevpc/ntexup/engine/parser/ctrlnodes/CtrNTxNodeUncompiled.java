package net.thevpc.ntexup.engine.parser.ctrlnodes;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.elem.NElement;

public class CtrNTxNodeUncompiled extends CtrlNTxNodeBase implements Cloneable {

    public CtrNTxNodeUncompiled(NElement body, NTxSource source) {
        super(NTxNodeType.CTRL_UNCOMPILED, source);
        setRaw(body);
    }

    @Override
    public NTxNode copy() {
        CtrNTxNodeUncompiled c = new CtrNTxNodeUncompiled(getRaw(),source());
        copyTo(c);
        return c;
    }

    @Override
    public String toString() {
        return "Uncompiled(" +
                getRaw()
                + ')';
    }
}
