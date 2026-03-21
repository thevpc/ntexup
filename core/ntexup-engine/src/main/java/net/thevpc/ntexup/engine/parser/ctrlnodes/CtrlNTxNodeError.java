package net.thevpc.ntexup.engine.parser.ctrlnodes;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.text.NMsg;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class CtrlNTxNodeError extends CtrlNTxNodeBase implements Cloneable{
    private NMsg message;

    public CtrlNTxNodeError(NTxSource source, NMsg message) {
        super(NTxNodeType.CTRL_ERROR,source);
        this.message=message;
    }

    public NMsg getMessage() {
        return message;
    }

    @Override
    public NTxNode copy() {
        CtrlNTxNodeError c = new CtrlNTxNodeError(source(),message);
        copyTo(c);
        return c;
    }

    @Override
    public NTxNode copyTo(NTxNode other) {
        super.copyTo(other);
        if (other instanceof CtrlNTxNodeError) {
            CtrlNTxNodeError oc = (CtrlNTxNodeError) other;
            oc.message = message;
        }
        return this;
    }


    public CtrlNTxNodeError setMessage(NMsg __args) {
        this.message = message;
        return this;
    }

    @Override
    public String toString() {
        return "Error(" +
                message+
                ')';
    }
}
