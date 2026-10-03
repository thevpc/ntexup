package net.thevpc.ntexup.extension.commonfunctions.general;

import net.thevpc.ntexup.api.eval.NTxFunctionCallContext;
import net.thevpc.ntexup.api.eval.NTxFutureUtils;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.nuts.elem.NElement;

public class NTxFunctionAwait implements NTxFunction {
    @Override
    public String name() {
        return "await";
    }

    @Override
    public NElement invoke(NTxFunctionCallContext args) {
        if (args.checkTooFewArgs(1)) {
            return NElement.ofNull();
        }
        NElement arg = args.arg(0).eval();
        Object res = NTxFutureUtils.await(arg);
        if (res instanceof NElement) {
            return (NElement) res;
        }
        return arg;
    }
}
