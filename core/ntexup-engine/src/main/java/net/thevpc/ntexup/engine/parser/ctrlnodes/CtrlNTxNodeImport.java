package net.thevpc.ntexup.engine.parser.ctrlnodes;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.elem.NElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CtrlNTxNodeImport extends CtrlNTxNodeBase {
    private List<NElement> __args = new ArrayList<>();

    public CtrlNTxNodeImport(NTxSource source, List<NElement> __args) {
        super(NTxNodeType.CTRL_IMPORT,source);
        this.__args.addAll(__args);
    }


    public List<NElement> getCallArgs() {
        return __args;
    }

    @Override
    public NTxNode copy() {
        CtrlNTxNodeImport c = new CtrlNTxNodeImport(source(),__args);
        copyTo(c);
        return c;
    }

    @Override
    public NTxNode copyTo(NTxNode other) {
        super.copyTo(other);
        if (other instanceof CtrlNTxNodeImport) {
            CtrlNTxNodeImport oc = (CtrlNTxNodeImport) other;
            oc.__args = new ArrayList<>(__args);
        }
        return this;
    }


    public CtrlNTxNodeImport setArgs(List<NElement> __args) {
        this.__args = __args;
        return this;
    }

    @Override
    public String toString() {
        return "Import(" +
                __args.stream().map(x->x.toString()).collect(Collectors.joining(", ")) +
                ')';
    }
}
