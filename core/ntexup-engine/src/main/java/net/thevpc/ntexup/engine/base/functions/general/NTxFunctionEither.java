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
        for (int i = 0; i < args.size(); i++) {
            NElement u = args.eval(i);
            if (i < args.size() - 1 && u.isName() && u.equals(args.arg(i).src())) {
                // this is a var that could not be resolved, just skip
                continue;
            }
            if (!NBlankable.isBlank(u)) {
                return u;
            }
        }
        return null;
    }
}
