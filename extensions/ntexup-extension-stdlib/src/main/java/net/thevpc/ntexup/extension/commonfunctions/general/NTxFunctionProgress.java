package net.thevpc.ntexup.extension.commonfunctions.general;

import net.thevpc.ntexup.api.eval.NTxFunctionCallContext;
import net.thevpc.ntexup.api.eval.NTxFutureUtils;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.nuts.elem.NElement;

public class NTxFunctionProgress implements NTxFunction {
    @Override
    public String name() {
        return "progress";
    }

    @Override
    public NElement invoke(NTxFunctionCallContext args) {
        if (args.checkTooFewArgs(1)) {
            return NElement.ofDouble(1.0);
        }
        NElement arg = args.arg(0).eval();
        return NElement.ofDouble(NTxFutureUtils.progress(arg));
    }
}
