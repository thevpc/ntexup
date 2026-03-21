package net.thevpc.ntexup.engine.parser.ctrlnodes;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.elem.NElement;

public class CtrlNTxNodeName extends CtrlNTxNodeBase implements Cloneable{
    private NElement __varName;

    private CtrlNTxNodeName(NTxSource source) {
        super(NTxNodeType.CTRL_NAME,source);
    }

    public CtrlNTxNodeName(NTxSource source, NElement __varName) {
        super(NTxNodeType.CTRL_NAME,source);
        this.__varName=__varName;
    }

    public NElement getVarName() {
        return __varName;
    }

    @Override
    public NTxNode copy() {
        CtrlNTxNodeName c = new CtrlNTxNodeName(source());
        copyTo(c);
        return c;
    }

    @Override
    public NTxNode copyTo(NTxNode other) {
        super.copyTo(other);
        if (other instanceof CtrlNTxNodeName) {
            CtrlNTxNodeName oc = (CtrlNTxNodeName) other;
            oc.__varName = __varName;
        }
        return this;
    }

    @Override
    public String toString() {
        return "Name("+__varName+')';
    }
}
