package net.thevpc.ntexup.engine.parser.ctrlnodes;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.elem.*;

import java.util.ArrayList;
import java.util.List;

public class CtrlNTxNodeIf extends CtrlNTxNodeBase implements Cloneable {
    private NElement __cond;
    private List<NTxNode> __trueBloc;
    private List<NTxNode> __falseBloc;

    public CtrlNTxNodeIf(NTxSource source) {
        super(NTxNodeType.CTRL_IF,source);
    }

    public CtrlNTxNodeIf(NTxSource source, NElement __cond, List<NTxNode> __trueBloc, List<NTxNode> __falseBloc) {
        super(NTxNodeType.CTRL_IF,source);
        this.__cond = __cond;
        this.__trueBloc = __trueBloc;
        this.__falseBloc = __falseBloc;
    }

    public NElement getCond() {
        return __cond;
    }

    public List<NTxNode> getTrueBloc() {
        return __trueBloc;
    }

    public List<NTxNode> getFalseBloc() {
        return __falseBloc;
    }

    @Override
    public NTxNode copy() {
        CtrlNTxNodeIf c = new CtrlNTxNodeIf(source());
        copyTo(c);
        return c;
    }

    @Override
    public NTxNode copyTo(NTxNode other) {
        super.copyTo(other);
        if (other instanceof CtrlNTxNodeIf) {
            CtrlNTxNodeIf oc = (CtrlNTxNodeIf) other;
            oc.__cond = __cond;
            oc.__trueBloc =new ArrayList<>(__trueBloc);
            oc.__falseBloc = new ArrayList<>(__falseBloc);
        }
        return this;
    }
}
