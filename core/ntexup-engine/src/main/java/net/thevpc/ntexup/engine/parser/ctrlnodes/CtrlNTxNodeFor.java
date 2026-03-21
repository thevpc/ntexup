package net.thevpc.ntexup.engine.parser.ctrlnodes;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.elem.NElement;

import java.util.ArrayList;
import java.util.List;

public class CtrlNTxNodeFor extends CtrlNTxNodeBase implements Cloneable{
    private NElement __varName;
    private NElement __varExpr;
    private List<NElement> __body = new ArrayList<>();

    private CtrlNTxNodeFor(NTxSource source) {
        super(NTxNodeType.CTRL_FOR,source);
    }

    public CtrlNTxNodeFor(NTxSource source, NElement __varName, NElement __varExpr, List<NElement> __body) {
        super(NTxNodeType.CTRL_FOR,source);
        this.__varName=__varName;
        this.__varExpr=__varExpr;
        this.__body=__body;
    }

    public NElement getVarName() {
        return __varName;
    }

    public NElement getVarExpr() {
        return __varExpr;
    }

    public List<NElement> getBody() {
        return __body;
    }

    @Override
    public NTxNode copy() {
        CtrlNTxNodeFor c = new CtrlNTxNodeFor(source());
        copyTo(c);
        return c;
    }

    @Override
    public NTxNode copyTo(NTxNode other) {
        super.copyTo(other);
        if (other instanceof CtrlNTxNodeFor) {
            CtrlNTxNodeFor oc = (CtrlNTxNodeFor) other;
            oc.__varName = __varName;
            oc.__varExpr = __varExpr;
            oc.__body = new ArrayList<>(__body);
        }
        return this;
    }
}
