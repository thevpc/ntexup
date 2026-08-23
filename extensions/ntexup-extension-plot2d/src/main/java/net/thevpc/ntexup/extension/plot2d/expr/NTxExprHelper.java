package net.thevpc.ntexup.extension.plot2d.expr;

import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.nuts.expr.*;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

public class NTxExprHelper {
    public static NExprMutableContext create(NTxRendererContext rendererContext){
        NExprMutableContext d = NExprContextBuilder.of()
                .declareBuiltins()
                .declareMathConstants()
                .declarePhysicsConstants()
                .declareMathFunctions()
                .buildMutable();
        d.declareConstant("C",299792458.0);

        return d;
    }

    public static double asDouble(NOptional<Object>  any, NTxRendererContext rendererContext){
        if(any.isError()){
            rendererContext.log(NMsg.ofC("evaluation error : %s",any.message().get()));
        }
        return NTxValue.of(any.orNull()).asDoubleOrNumber().orElse(0.0);
    }
}
