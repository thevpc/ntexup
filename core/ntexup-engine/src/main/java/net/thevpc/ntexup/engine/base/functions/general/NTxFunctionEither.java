package net.thevpc.ntexup.engine.base.functions.general;

import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.eval.NTxFunctionArg;
import net.thevpc.ntexup.api.eval.NTxFunctionArgs;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NBlankable;

public class NTxFunctionEither implements NTxFunction {
    @Override
    public String name() {
        return "either";
    }

    @Override
    public NElement invoke(NTxFunctionArgs args, NTxResolutionContext context) {
        for (NTxFunctionArg arg : args.args()) {
            NElement u = arg.eval();
            if (!NBlankable.isBlank(u)) {
                return u;
            }
        }
        return null;
    }
}
