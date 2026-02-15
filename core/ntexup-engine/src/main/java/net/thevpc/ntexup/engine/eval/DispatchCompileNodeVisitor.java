package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.engine.CompileNodeVisitor;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxVar;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NIllegalArgumentException;

public class DispatchCompileNodeVisitor implements CompileNodeVisitor {
    private NTxResolutionContext context;
    private CompileNodeVisitor visitor;
    private boolean compile;

    public DispatchCompileNodeVisitor(CompileNodeVisitor visitor) {
        this(visitor,true,null);
    }

    public DispatchCompileNodeVisitor(CompileNodeVisitor visitor,boolean compile) {
        this(visitor,compile,null);
    }
    public DispatchCompileNodeVisitor(CompileNodeVisitor visitor,boolean compile,NTxResolutionContext context) {
        this.context = context;
        this.visitor = visitor;
        this.compile = compile;
    }

    public void visitItem(NTxItem node, NTxResolutionContext context) {
        if (node instanceof NTxItemList) {
            for (NTxItem item : ((NTxItemList) node).getItems()) {
                visitItem(item, context);
            }
            return;
        }
        if (node instanceof NTxNodeDef) {
            this.visitDefinition((NTxNodeDef) node, context);
        } else if (node instanceof NTxNode) {
            NTxNode n = (NTxNode) node;
            switch (n.type()) {
                case NTxNodeType.FRAGMENT: {
                    for (NTxNode child : ((NTxNode) node).children()) {
                        visitItem(child, context);
                    }
                    return;
                }
            }
            visitNode(n,context);
        } else if (node instanceof NTxStyleRule) {
            visitor.visitRule((NTxStyleRule) node, context);
        } else if (node instanceof NTxFunction) {
            visitor.visitFunction((NTxFunction) node, context);
        } else if (node instanceof NTxProp) {
            visitor.visitProperty((NTxProp) node, context);
        } else {
            throw new NIllegalArgumentException(NMsg.ofC("unexpected"));
        }
    }

    @Override
    public void visitNode(NTxNode node, NTxResolutionContext context) {
        if (compile) {
            getContext(context).doWithSibling(node, cc -> {
                context.engine().compileNode(cc,visitor);
            });
        }else {
            visitor.visitNode(node, getContext(context));
        }
    }

    @Override
    public void visitRule(NTxStyleRule rule, NTxResolutionContext context) {
        visitor.visitRule(rule, getContext(context));
    }

    @Override
    public void visitDefinition(NTxNodeDef def, NTxResolutionContext context) {
        visitor.visitDefinition(def, getContext(context));
    }

    @Override
    public void visitFunction(NTxFunction fct, NTxResolutionContext context) {
        visitor.visitFunction(fct, getContext(context));
    }

    @Override
    public void visitProperty(NTxProp a, NTxResolutionContext context) {
        visitor.visitProperty(a, getContext(context));
    }

    @Override
    public void visitVar(String varName, NTxVar nTxVar, NTxResolutionContext context) {
        visitor.visitVar(varName, nTxVar, getContext(context));
    }

    private NTxResolutionContext getContext(NTxResolutionContext context) {
        return this.context == null ? context : this.context;
    }
}
