package net.thevpc.ntexup.engine.renderer.special;

import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.engine.renderer.NTxNodeRendererBase;

import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxVar;
import net.thevpc.nuts.elem.NElement;

public class NTxAssignRenderer extends NTxDoNothingRenderer {
    public NTxAssignRenderer() {
        super(NTxNodeType.CTRL_ASSIGN);
    }

    @Override
    public void renderMain(NTxRendererContext ctx) {
        net.thevpc.ntexup.api.document.node.NTxNode child = ctx.node();
        NTxProp valProp = child.getProperty(NTxPropName.VALUE).orNull();
        NTxProp nameProp = child.getProperty(NTxPropName.NAME).orNull();
        if (valProp != null && nameProp != null) {
            String varName = nameProp.getValue().asStringValue().orNull();
            if (varName != null) {
                NElement varExpr = valProp.getValue();
                NElement evaluatedExpr = ctx.evalExpression(varExpr).orNull();
                NTxVar var = NTxVar.ofEvaluatedExpression(evaluatedExpr);
                ctx.setVar(varName, var);
                NTxResolutionContext p = ctx.parentContext();
                if (p != null) {
                    p.setVar(varName, var);
                }
            }
        }
    }
}
