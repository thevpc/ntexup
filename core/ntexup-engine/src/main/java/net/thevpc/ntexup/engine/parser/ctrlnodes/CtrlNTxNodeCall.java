package net.thevpc.ntexup.engine.parser.ctrlnodes;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CtrlNTxNodeCall extends CtrlNTxNodeBase implements Cloneable{
    private List<NElement> __args = new ArrayList<>();
    private String callName;
    private List<NElement> __callBody = new ArrayList<>();

    public CtrlNTxNodeCall(NTxSource source) {
        super(NTxNodeType.CTRL_CALL,source);
    }



    public CtrlNTxNodeCall setCallName(String callName) {
        this.callName = callName;
        return this;
    }

    public String getCallName() {
        return callName;
    }

    public List<NElement> getCallArgs() {
        return __args;
    }

    public List<NElement> getCallBody() {
        return __callBody;
    }

    @Override
    public NTxNode copy() {
        CtrlNTxNodeCall c = new CtrlNTxNodeCall(source());
        copyTo(c);
        return c;
    }

    @Override
    public NTxNode copyTo(NTxNode other) {
        super.copyTo(other);
        if (other instanceof CtrlNTxNodeCall) {
            CtrlNTxNodeCall oc = (CtrlNTxNodeCall) other;
            oc.callName = callName;
            oc.__args = new ArrayList<>(__args);
            oc.__callBody = new ArrayList<>(__callBody);
        }
        return this;
    }


    public CtrlNTxNodeCall setArgs(List<NElement> __args) {
        this.__args = __args;
        return this;
    }


    public CtrlNTxNodeCall setCallBody(List<NElement> __callBody) {
        this.__callBody = __callBody;
        return this;
    }

    @Override
    public String toString() {
        List<NElement> a = new ArrayList<>();
        a.addAll(__args);
        return "call::" + callName + "("+a.stream().map(x -> NTxUtils.snippet(x)).collect(Collectors.joining(","))+")";
    }
}
