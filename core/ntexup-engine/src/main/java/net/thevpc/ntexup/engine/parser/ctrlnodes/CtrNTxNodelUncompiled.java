package net.thevpc.ntexup.engine.parser.ctrlnodes;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.elem.NElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CtrNTxNodelUncompiled extends CtrlNTxNodeBase implements Cloneable {

    public CtrNTxNodelUncompiled(NElement body,NTxSource source) {
        super(NTxNodeType.CTRL_UNCOMPILED, source);
        setRaw(body);
    }

    @Override
    public NTxNode copy() {
        CtrNTxNodelUncompiled c = new CtrNTxNodelUncompiled(getRaw(),source());
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
