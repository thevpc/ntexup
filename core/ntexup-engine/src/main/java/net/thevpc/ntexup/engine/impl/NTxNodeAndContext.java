package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;

public class NTxNodeAndContext {
    public NTxNode node;
    public NTxResolutionContext context;

    public NTxNodeAndContext(NTxNode node, NTxResolutionContext context) {
        this.node = node;
        this.context = context;
    }

    public void run(NTxDocument document, NTxEngine engine, NTxCompiledDocument compiledDocument, NTxCompiledPage compiledPage, boolean inPage) {
        NTxResolutionContext context = this.context;
        if (inPage) {
            context = context.copy();
            context.setInPage(true);
        }
        context.doWithChild(node,
                cc -> engine.compileNode(node, document, compiledDocument, compiledPage, cc, new CompileNodeVisitorRunner())
        );
    }

    @Override
    public String toString() {
        return "NTxNodeAndContext{" +
                "node=" + node +
                ", context=" + context +
                '}';
    }
}
