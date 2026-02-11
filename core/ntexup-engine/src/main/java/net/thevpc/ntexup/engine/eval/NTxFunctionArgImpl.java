package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxFunctionArg;
import net.thevpc.nuts.elem.NElement;

public class NTxFunctionArgImpl implements NTxFunctionArg {
    private final NElement expression;
    private final NTxResolutionContext context;

    public NTxFunctionArgImpl(NElement expression, NTxResolutionContext context) {
        this.expression = expression;
        this.context = context;
    }

    public NElement src() {
        return expression;
    }

    @Override
    public NElement eval() {
        NElement u = context.evalExpression(expression).orNull();
        NElement u2 = context.evalExpression(u).orNull();
        return u2;
    }

    @Override
    public String toString() {
        return String.valueOf(expression);
    }
}
