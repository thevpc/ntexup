package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeDef;
import net.thevpc.ntexup.api.document.node.NTxNodeDefParam;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import net.thevpc.ntexup.engine.parser.ctrlnodes.CtrlNTxNodeInclude;

import java.util.ArrayList;

public class NTxNodeDefImpl extends DefaultNTxNode implements NTxNodeDef, Cloneable {
    private NTxNodeDefParam[] params;
    private NTxNode bodyContainer;

    public NTxNodeDefImpl(NTxNode parent, String templateName, NTxNodeDefParam[] params, NTxNode bodyContainer, NTxSource source) {
        super(NTxNodeType.CTRL_DEFINE);
        setName(templateName);
        setParent(parent);
        setSource(source);
        this.params = params;
        this.bodyContainer = bodyContainer;
        bodyContainer.setParent(parent);
        for (NTxNode n : bodyContainer.children()) {
            n.setParent(bodyContainer);
        }
    }

    @Override
    public NTxNode copy() {
        NTxNodeDefImpl c = new NTxNodeDefImpl((NTxNode) parent(),name(),params,bodyContainer,source());
        copyTo(c);
        return c;
    }

    @Override
    public NTxNode copyTo(NTxNode other) {
        super.copyTo(other);
        if (other instanceof NTxNodeDefImpl) {
            NTxNodeDefImpl oc = (NTxNodeDefImpl) other;
            oc.params = params;
            oc.bodyContainer = bodyContainer;
            bodyContainer.setParent(parent);
            for (NTxNode n : bodyContainer.children()) {
                n.setParent(bodyContainer);
            }
        }
        return this;
    }

    @Override
    public NTxNodeDefParam[] params() {
        return params;
    }

    @Override
    public NTxNode[] body() {
        return bodyContainer.children().toArray(new NTxNode[0]);
    }

    public NTxNode bodyContainer() {
        return bodyContainer;
    }
}
