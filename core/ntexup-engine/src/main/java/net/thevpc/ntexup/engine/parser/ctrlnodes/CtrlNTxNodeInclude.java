package net.thevpc.ntexup.engine.parser.ctrlnodes;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.elem.NElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CtrlNTxNodeInclude extends CtrlNTxNodeBase implements Cloneable {
    private List<NElement> __args = new ArrayList<>();

    public CtrlNTxNodeInclude(NTxSource source, List<NElement> __args) {
        super(NTxNodeType.CTRL_INCLUDE,source);
        this.__args.addAll(__args);
    }


    public List<NElement> getCallArgs() {
        return __args;
    }

    @Override
    public NTxNode copy() {
        CtrlNTxNodeInclude c = new CtrlNTxNodeInclude(source(),__args);
        copyTo(c);
        return c;
    }

    @Override
    public NTxNode copyTo(NTxNode other) {
        super.copyTo(other);
        if (other instanceof CtrlNTxNodeInclude) {
            CtrlNTxNodeInclude oc = (CtrlNTxNodeInclude) other;
            oc.__args = new ArrayList<>(__args);
        }
        return this;
    }


    public CtrlNTxNodeInclude setArgs(List<NElement> __args) {
        this.__args = __args;
        return this;
    }

    @Override
    public String toString() {
        return "Include(" +
                __args.stream().map(x->x.toString()).collect(Collectors.joining(", ")) +
                ')';
    }
}
