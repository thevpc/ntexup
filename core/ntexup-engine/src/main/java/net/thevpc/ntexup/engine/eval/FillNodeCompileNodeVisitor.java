package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeDef;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.engine.CompileNodeVisitor;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxVar;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;

public class FillNodeCompileNodeVisitor implements CompileNodeVisitor {
    private final NTxNode root;

    public FillNodeCompileNodeVisitor(NTxNode root) {
        this.root = root;
    }

    @Override
    public void visitNode(NTxNode node, NTxResolutionContext context) {
        root.add(node);
    }

    @Override
    public void visitRule(NTxStyleRule a, NTxResolutionContext context) {
        root.addRule(a);
    }

    @Override
    public void visitDefinition(NTxNodeDef a, NTxResolutionContext context) {
        root.add(a);
    }

    @Override
    public void visitFunction(NTxFunction a, NTxResolutionContext context) {
        throw new IllegalArgumentException("unsupported");
    }

    @Override
    public void visitVar(String varName, NTxVar nTxVar, NTxResolutionContext context) {
        visitNode(DefaultNTxNode.ofAssign(varName,nTxVar.get(),context.source()),context);
    }

    @Override
    public void visitProperty(NTxProp a, NTxResolutionContext context) {
        root.setProperty(a);
    }
}
