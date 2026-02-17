package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeDef;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.engine.CompileNodeVisitor;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxVar;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FillDocumentCompileNodeVisitor implements CompileNodeVisitor, AutoCloseable {
    private final NTxNode container; // The node we are currently filling    boolean someChanges = false;
    private final NTxEngine engine; // The node we are currently filling    boolean someChanges = false;

    public FillDocumentCompileNodeVisitor(NTxNode container, NTxEngine engine) {
        this.container = container;
        this.engine = engine;
    }

    @Override
    public void visitVar(String varName, NTxVar nTxVar, NTxResolutionContext context) {
        //visitNode(DefaultNTxNode.ofAssign(varName, nTxVar.get(), context.source()), context);
    }


    @Override
    public void visitNode(NTxNode node, NTxResolutionContext context) {
        container.add(node);
    }

    /**
     * This must be called when the recursive 'compileNodeTree'
     * using THIS visitor instance finishes.
     */
    public void close() {
    }

    public void visitRule(NTxStyleRule rule, NTxResolutionContext context) {
        container.addRule(rule);
    }

    @Override
    public void visitDefinition(NTxNodeDef def, NTxResolutionContext context) {
        container.add(def);
    }

    @Override
    public void visitFunction(NTxFunction fct, NTxResolutionContext context) {
        throw new IllegalArgumentException("unsupported");
    }

    @Override
    public void visitProperty(NTxProp prop, NTxResolutionContext context) {
        container.setProperty(prop);
    }


}
