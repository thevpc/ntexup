package net.thevpc.ntexup.extension.commonfunctions.general;

import net.thevpc.ntexup.api.eval.NTxFunctionCallContext;
import net.thevpc.ntexup.api.eval.NTxFutureUtils;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.nuts.elem.NElement;

public class NTxFunctionIsFuture implements NTxFunction {
    @Override
    public String name() {
        return "isFuture";
    }

    @Override
    public NElement invoke(NTxFunctionCallContext args) {
        if (args.checkTooFewArgs(1)) {
            return NElement.ofFalse();
        }
        NElement arg = args.arg(0).eval();
        return NElement.ofBoolean(NTxFutureUtils.isFuture(arg));
    }
}
