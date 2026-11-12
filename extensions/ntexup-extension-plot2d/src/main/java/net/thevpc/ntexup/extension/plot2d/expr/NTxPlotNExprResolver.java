package net.thevpc.ntexup.extension.plot2d.expr;

import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.extension.plot2d.model.NTxFunctionPlotInfo;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.expr.*;
import net.thevpc.nuts.math.NDoubleFunction;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.util.HashMap;
import java.util.Map;

public class NTxPlotNExprResolver implements NExprResolver {
    private final NTxFunctionPlotInfo e;
    private NExprMutableContext d;
    private Map<String, NExprVar> extraVars = new HashMap<>();


    public static NDoubleFunction compileFunctionX(NTxFunctionPlotInfo e, NTxRendererContext rendererContext) {
        NElement expression = e.fexpr;
        NTxFunctionPlotInfo.CompiledExpression compiled;
        synchronized (e) {
            compiled = e.getCompiledFunction(expression);
            if (compiled == null) {
                NExprMutableContext context = NTxExprHelper.create(rendererContext);
                NOptional<NExprNode> parsed = context.parse(expression.isAnyString()
                        ? expression.asStringValue().get()
                        : NTxUtils.removeCompilerDeclarationPathAnnotations(expression).toString());
                if (!parsed.isPresent()) {
                    rendererContext.log(NMsg.ofC("unable to parse expression %s : %s",
                            parsed.message(), expression));
                    return null;
                }
                e.setCompiledFunction(expression, context, parsed.get());
                compiled = e.getCompiledFunction(expression);
            }
        }
        NExprMutableContext context = compiled.context;
        NExprNode expressionNode = compiled.node;
        return x -> {
            NOptional<Object> r = expressionNode.eval(
                    context.childContext()
                            .declareResolver(new NTxPlotNExprResolver(e, context, x, 0, 0, 0))
                            .build()
            );
            return NTxExprHelper.asDouble(r,rendererContext);
        };
    }

    public NTxPlotNExprResolver(NTxFunctionPlotInfo e, NExprMutableContext d, double x, double y, double z, double t) {
        this.e = e;
        this.d = d;
        addVar(e.var1, x);
        if (e.args >= 2) {
            addVar(e.var2, y);
            if (e.args >= 3) {
                addVar(e.var3, z);
                if (e.args >= 4) {
                    addVar(e.var4, t);
                }
            }
        }
    }

    private void addVar(String varName, Object varValue) {
        extraVars.put(varName, NExprVar.ofVar(varName, varValue));
    }

    @Override
    public NOptional<NExprVar> getVar(String varName, NExprContext context) {
        return NOptional.ofNamed(extraVars.get(varName), "var " + varName);
    }
}
