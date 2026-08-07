package net.thevpc.ntexup.extension.plot2d.expr;

import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.extension.plot2d.model.NTxFunctionPlotInfo;
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
        NExprMutableContext d = NTxExprHelper.create(rendererContext);
        NOptional<NExprNode> ne = d.parse(e.fexpr.isAnyString() ? e.fexpr.asStringValue().get() : NTxUtils.removeCompilerDeclarationPathAnnotations(e.fexpr).toString());
        if (!ne.isPresent()) {
            rendererContext.log(NMsg.ofC("unable to parse expression %s : %s", ne.getMessage(), e.fexpr));
            return null;
        }
        NExprNode nExprNode = ne.get();
        return x -> {
            NOptional<Object> r = nExprNode.eval(
                    d.childContext()
                            .declareResolver(new NTxPlotNExprResolver(e, d, x, 0, 0, 0))
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
