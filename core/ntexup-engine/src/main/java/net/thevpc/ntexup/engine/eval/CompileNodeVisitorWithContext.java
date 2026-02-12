package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeDef;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.engine.CompileNodeVisitor;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.extension.NTxFunction;

public class CompileNodeVisitorWithContext implements CompileNodeVisitor {
    private NTxResolutionContext context;
    private CompileNodeVisitor visitor;

    public CompileNodeVisitorWithContext(NTxResolutionContext context, CompileNodeVisitor visitor) {
        this.context = context;
        this.visitor = visitor;
    }

    @Override
    public void visitNode(NTxNode node, NTxResolutionContext context) {
        visitor.visitNode(node,this.context==null?context:this.context);
    }

    @Override
    public void visitRule(NTxStyleRule rule, NTxResolutionContext context) {
        visitor.visitRule(rule,this.context==null?context:this.context);
    }

    @Override
    public void visitDefinition(NTxNodeDef def, NTxResolutionContext context) {
        visitor.visitDefinition(def,this.context==null?context:this.context);
    }

    @Override
    public void visitFunction(NTxFunction fct, NTxResolutionContext context) {
        visitor.visitFunction(fct,this.context==null?context:this.context);
    }

    @Override
    public void visitProperty(NTxProp a, NTxResolutionContext context) {
        visitor.visitProperty(a,this.context==null?context:this.context);
    }
}
