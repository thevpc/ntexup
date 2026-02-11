package net.thevpc.ntexup.engine.parser.ctrlnodes;

import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.source.NTxSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CtrlNTxNodeComplexResult extends CtrlNTxNodeBase {
    private List<NTxItem> __args = new ArrayList<>();

    public CtrlNTxNodeComplexResult(NTxSource source, NTxItem... __args) {
        super(NTxNodeType.CTRL_COMPLEX_RESULT,source);
        this.__args.addAll(Arrays.asList(__args));
    }


    public List<NTxItem> getCallArgs() {
        return __args;
    }

    @Override
    public NTxNode copy() {
        CtrlNTxNodeComplexResult c = new CtrlNTxNodeComplexResult(source(),__args.toArray(new NTxItem[0]));
        copyTo(c);
        return c;
    }

    @Override
    public NTxNode copyTo(NTxNode other) {
        super.copyTo(other);
        if (other instanceof CtrlNTxNodeComplexResult) {
            CtrlNTxNodeComplexResult oc = (CtrlNTxNodeComplexResult) other;
            oc.__args = new ArrayList<>(__args);
        }
        return this;
    }


    public CtrlNTxNodeComplexResult setArgs(List<NTxItem> __args) {
        this.__args = __args;
        return this;
    }

    @Override
    public String toString() {
        return "Result(" +
                __args.stream().map(x->x.toString()).collect(Collectors.joining(", ")) +
                ')';
    }
}
