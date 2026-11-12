package net.thevpc.ntexup.extension.plot2d.model;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.expr.NExprMutableContext;
import net.thevpc.nuts.expr.NExprNode;
import net.thevpc.nuts.math.NDoubleFunction;

public class NTxFunctionPlotInfo {
    private NElement compiledExpression;
    private CompiledExpression compiledFunction;
    private NElement sampledExpression;
    private double[] sampledX;
    private double[] sampledY;
    private int sampledArgs;
    private String sampledVar1;
    private String sampledVar2;
    private String sampledVar3;
    private String sampledVar4;
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

    public synchronized double[] getCachedSamples(NElement expression, double[] xValues) {
        if (!isCacheableSampleExpression(expression)
                || !java.util.Objects.equals(sampledExpression, expression)
                || !java.util.Arrays.equals(sampledX, xValues)
                || sampledArgs != args
                || !java.util.Objects.equals(sampledVar1, var1)
                || !java.util.Objects.equals(sampledVar2, var2)
                || !java.util.Objects.equals(sampledVar3, var3)
                || !java.util.Objects.equals(sampledVar4, var4)) {
            return null;
        }
        return sampledY.clone();
    }

    public synchronized void setCachedSamples(NElement expression, double[] xValues, double[] yValues) {
        if (!isCacheableSampleExpression(expression)) {
            return;
        }
        sampledExpression = expression;
        sampledX = xValues.clone();
        sampledY = yValues.clone();
        sampledArgs = args;
        sampledVar1 = var1;
        sampledVar2 = var2;
        sampledVar3 = var3;
        sampledVar4 = var4;
    }

    private static boolean isCacheableSampleExpression(NElement expression) {
        if (expression == null) {
            return false;
        }
        String source = expression.isAnyString()
                ? expression.asStringValue().get()
                : expression.toString();
        return !source.matches("(?is).*\\b(?:t|time|now|rand|random)\\b.*");
    }
}
