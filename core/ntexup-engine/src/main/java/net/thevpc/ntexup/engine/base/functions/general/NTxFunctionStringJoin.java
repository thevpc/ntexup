package net.thevpc.ntexup.engine.base.functions.general;

import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxFunctionArg;
import net.thevpc.ntexup.api.eval.NTxFunctionArgs;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NBlankable;

import java.util.ArrayList;
import java.util.List;

public class NTxFunctionStringJoin implements NTxFunction {
    @Override
    public String name() {
        return "stringJoin";
    }

    @Override
    public NElement invoke(NTxFunctionArgs args, NTxResolutionContext context) {
        List<String> all = new ArrayList<>();
        NTxFunctionArg[] argsed = args.args();
        String sep="";
        for (int i = 0; i < argsed.length; i++) {
            NTxFunctionArg arg = argsed[i];
            NElement u = arg.eval();
            if(i==0){
                if(u.isName() || u.isString()){
                    sep=u.asStringValue().orElse("");
                }else{
                    sep=u.toString();
                }
            }else {
                if (!NBlankable.isBlank(u)) {
                    String a="";
                    if(u.isName() || u.isString()){
                        a=u.asStringValue().orElse("");
                    }else{
                        a=u.toString();
                    }
                    if (!isBlank(a,arg)) {
                        all.add(a);
                    }
                }
            }
        }
        return NElement.ofString(String.join(sep,all));
    }
    private boolean isBlank(String str,NTxFunctionArg arg){
        if(NBlankable.isBlank(str)){
            return true;
        }
        //TODO should find a better way !!
        if(str.matches("^##+$")){
            return true;
        }
        return false;
    }
}
