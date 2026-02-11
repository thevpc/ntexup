package net.thevpc.ntexup.api.eval;

import net.thevpc.nuts.elem.NElement;

public interface NTxVar {
    static NTxVar ofEvaluatedExpression(NElement evaluatedExpr) {
        return ()->evaluatedExpr==null?NElement.ofNull():evaluatedExpr;
    }

    static NTxVar ofNonEvaluatedExpression(NElement evaluatedExpr) {
        return ()->evaluatedExpr==null?NElement.ofNull():evaluatedExpr;
    }

    NElement get();
}
