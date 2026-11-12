package net.thevpc.ntexup.extension.plot2d.model;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.expr.NExprMutableContext;
import net.thevpc.nuts.expr.NExprNode;
import net.thevpc.nuts.math.NDoubleFunction;

public class NTxFunctionPlotInfo {
    private NElement compiledExpression;
    private CompiledExpression compiledFunction;
    public NTxPlotType plotType;
    public NTxPlotSource source;
    public NElement fexpr;
    public NElement fval;
    public String var1;
    public String var2;
    public String var3;
    public String var4;
    public int args;
    public NElement title;
    public NElement color;
    public NElement stroke;
    public NElement x;
    public NElement y;
    public NElement z;
    public NElement t;
    public NDoubleFunction f;

    public static final class CompiledExpression {
        public final NExprMutableContext context;
        public final NExprNode node;

        public CompiledExpression(NExprMutableContext context, NExprNode node) {
            this.context = context;
            this.node = node;
        }
    }

    public synchronized CompiledExpression getCompiledFunction(NElement expression) {
        return java.util.Objects.equals(compiledExpression, expression) ? compiledFunction : null;
    }

    public synchronized void setCompiledFunction(
            NElement expression, NExprMutableContext context, NExprNode node) {
        compiledExpression = expression;
        compiledFunction = new CompiledExpression(context, node);
    }
}
