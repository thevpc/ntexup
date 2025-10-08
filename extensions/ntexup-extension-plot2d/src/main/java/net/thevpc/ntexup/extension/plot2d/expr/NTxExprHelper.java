package net.thevpc.ntexup.extension.plot2d.expr;

import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.nuts.expr.*;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

public class NTxExprHelper {
    public static NExprMutableDeclarations create(NTxNodeRendererContext rendererContext){
        NExprMutableDeclarations d = NExprs.of().newMutableDeclarations();
        d.declareConstant("pi",Math.PI);
        d.declareConstant("PI",Math.PI);
        d.declareConstant("π",Math.PI);
        d.declareConstant("C",299792458.0);
        d.declareConstant("ε",8.85418782E-12);
        d.declareConstant("μ",1.25663706E-6);
        d.declareFunction("sin", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.sin(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("cos", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.cos(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("abs", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.abs(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("tan", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.tan(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("tanh", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.tanh(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("sinh", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.sinh(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("cosh", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.cosh(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("signum", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.signum(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("ulp", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.ulp(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("asin", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.asin(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("acos", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.acos(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("toRadians", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.toRadians(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("toDegrees", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.toDegrees(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("exp", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.exp(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("log", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.log(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("log10", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.log10(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("sqrt", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.sqrt(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("cbrt", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.cbrt(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("ceil", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.ceil(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("floor", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.floor(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("rint", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.rint(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("round", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            return Math.rint(asDouble(a.eval(context),rendererContext));
        });
        d.declareFunction("atan2", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            NExprNodeValue b = args.get(1);
            return Math.atan2(
                    asDouble(a.eval(context),rendererContext)
                    ,asDouble(b.eval(context),rendererContext)
            );
        });
        d.declareFunction("pow", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            NExprNodeValue b = args.get(1);
            return Math.pow(
                    asDouble(a.eval(context),rendererContext)
                    ,asDouble(b.eval(context),rendererContext)
            );
        });
        d.declareFunction("max", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            NExprNodeValue b = args.get(1);
            return Math.max(
                    asDouble(a.eval(context),rendererContext)
                    ,asDouble(b.eval(context),rendererContext)
            );
        });
        d.declareFunction("min", (name, args, context) -> {
            NExprNodeValue a = args.get(0);
            NExprNodeValue b = args.get(1);
            return Math.min(
                    asDouble(a.eval(context),rendererContext)
                    ,asDouble(b.eval(context),rendererContext)
            );
        });
        return d;
    }

    public static double asDouble(NOptional<Object>  any,NTxNodeRendererContext rendererContext){
        if(any.isError()){
            rendererContext.log().log(NMsg.ofC("evaluation error : %s",any.getMessage().get()));
        }
        return NTxValue.of(any.orNull()).asDoubleOrNumber().orElse(0.0);
    }
}
