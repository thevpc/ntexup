package net.thevpc.ntexup.api.engine;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeDef;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.extension.NTxFunction;

public interface CompileNodeVisitor {
    void visitNode(NTxNode node, NTxResolutionContext context);

    void visitRule(NTxStyleRule a, NTxResolutionContext context);

    void visitDefinition(NTxNodeDef a, NTxResolutionContext context);

    void visitFunction(NTxFunction a, NTxResolutionContext context);

    void visitProperty(NTxProp a, NTxResolutionContext context);
}
