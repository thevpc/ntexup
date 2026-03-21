package net.thevpc.ntexup.engine.parser.ctrlnodes;

import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.elem.NElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CtrlNTxNodeSimpleResult extends CtrlNTxNodeBase implements Cloneable{
    private NElement __args;

    public CtrlNTxNodeSimpleResult(NTxSource source, NElement __args) {
        super(NTxNodeType.CTRL_COMPLEX_RESULT,source);
        this.__args=__args;
    }


    public NElement getCallArgs() {
        return __args;
    }

    @Override
    public NTxNode copy() {
        CtrlNTxNodeSimpleResult c = new CtrlNTxNodeSimpleResult(source(),__args);
        copyTo(c);
        return c;
    }

    @Override
    public NTxNode copyTo(NTxNode other) {
        super.copyTo(other);
        if (other instanceof CtrlNTxNodeSimpleResult) {
            CtrlNTxNodeSimpleResult oc = (CtrlNTxNodeSimpleResult) other;
            oc.__args = __args;
        }
        return this;
    }


    public CtrlNTxNodeSimpleResult setArgs(NElement __args) {
        this.__args = __args;
        return this;
    }

    @Override
    public String toString() {
        return "Result(" +
                __args+
                ')';
    }
}
