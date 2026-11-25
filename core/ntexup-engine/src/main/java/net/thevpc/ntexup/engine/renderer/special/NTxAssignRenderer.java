package net.thevpc.ntexup.engine.renderer.special;

import net.thevpc.ntexup.api.document.node.NTxAssign;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;

import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxVar;
import net.thevpc.nuts.elem.NElement;

public class NTxAssignRenderer extends NTxDoNothingRenderer {

    private static final String LAST_VALUE_KEY = "ntexup.assign.last-value";

    public NTxAssignRenderer() {
        super(NTxNodeType.CTRL_ASSIGN);
    }

    @Override
    public void renderMain(NTxRendererContext ctx) {
        NTxNode child = ctx.node();
        NTxProp valProp = child.getProperty(NTxPropName.VALUE).orNull();
        NTxProp nameProp = child.getProperty(NTxPropName.NAME).orNull();
        if (valProp != null && nameProp != null) {
            String varName = nameProp.getValue().asStringValue().orNull();
            if (varName != null) {
                NElement varExpr = valProp.getValue();
                NElement evaluatedExpr = ctx.evalExpression(varExpr).orNull();
                boolean isStatic = NTxAssign.isStatic(child);
                NElement previousExpr = null;
                if (!isStatic) {
                    net.thevpc.nuts.util.NOptional<NElement> previousOptional = ctx.getVarValue(varName);
                    previousExpr = previousOptional == null ? null : previousOptional.orNull();
                }
                NTxVar var = NTxVar.ofEvaluatedExpression(evaluatedExpr);
                ctx.setVar(varName, var);
                NTxResolutionContext p = ctx.parentContext();
                if (p != null) {
                    p.setVar(varName, var);
                }
                if (isStatic) {
                    // Compiler-generated assignments (variable fills, component
                    // parameters, slots) are already evaluated: they only seed
                    // the render context. They must never notify the dependency
                    // graph, otherwise every render pass invalidates the page
                    // it is rendering (endless render/invalidate loop).
                    return;
                }
                NElement lastExpr = (NElement) child.getUserObject(LAST_VALUE_KEY).orNull();
                boolean changed = !sameValue(previousExpr, evaluatedExpr)
                        && !sameValue(lastExpr, evaluatedExpr);
                child.setUserObject(LAST_VALUE_KEY, evaluatedExpr);
                if (changed && ctx.compiledDocument() != null) {
                    ctx.compiledDocument().dependencyGraph().notifyBindingUpdated(varName);
                }
            }
        }
    }

    /**
     * Compares two evaluated values without deep-walking big structures: an
     * array/object value (component body, list, ...) is only considered
     * unchanged when it is the very same instance.
     */
    private static boolean sameValue(NElement a, NElement b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.isArray() || a.isObject() || b.isArray() || b.isObject()) {
            return false;
        }
        return a.equals(b);
    }
}
