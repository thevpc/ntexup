package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeDef;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.engine.CompileNodeVisitor;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.extension.NTxFunction;

class CompileNodeVisitorRunner implements CompileNodeVisitor {
    @Override
    public void visitNode(NTxNode node, NTxResolutionContext context) {

    }

    @Override
    public void visitRule(NTxStyleRule a, NTxResolutionContext context) {

    }

    @Override
    public void visitDefinition(NTxNodeDef a, NTxResolutionContext context) {

    }

    @Override
    public void visitFunction(NTxFunction a, NTxResolutionContext context) {

    }

    @Override
    public void visitProperty(NTxProp a, NTxResolutionContext context) {

    }
}
