package net.thevpc.ntexup.engine.base.functions.general;

import net.thevpc.ntexup.api.eval.*;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NArrays;

public class NTxFunctionDsteps implements NTxFunction {
    @Override
    public String name() {
        return "dsteps";
    }

    @Override
    public NElement invoke(NTxFunctionCallContext args) {
        NTxFunctionArg[] argsed = args.args();
        if(argsed.length==0){
            return NElement.ofArray();
        }
        if(argsed.length==1){
            return NElement.ofArray(args.arg(0).eval());
        }
        if(argsed.length==2){
            Double min = NTxValue.of(args.arg(0).eval()).asDouble().orNull();
            if(min==null){
                return NElement.ofArray();
            }
            Double max = NTxValue.of(args.arg(1).eval()).asDouble().orNull();
            if(max==null){
                return NElement.ofArray();
            }
            return NElement.ofDoubleArray(
                    NArrays.range(
                            min,
                            max,
                            1
                    )
            );
        }
        Double min = NTxValue.of(args.arg(0).eval()).asDouble().orNull();
        if(min==null){
            return NElement.ofArray();
        }
        Double max = NTxValue.of(args.arg(1).eval()).asDouble().orNull();
        if(max==null){
            return NElement.ofArray();
        }
        Double s = NTxValue.of(args.arg(2).eval()).asDouble().orNull();
        if(s==null){
            s=1.0;
        }
        return NElement.ofDoubleArray(
                NArrays.range(
                        min,
                        max,
                        s
                )
        );
    }

}
