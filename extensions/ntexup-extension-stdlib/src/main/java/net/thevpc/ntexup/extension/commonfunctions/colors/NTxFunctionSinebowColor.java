package net.thevpc.ntexup.extension.commonfunctions.colors;

import net.thevpc.ntexup.api.eval.NTxFunctionCallContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.util.NTxElementUtils;
import net.thevpc.ntexup.extension.commonfunctions.util.NTxColorUtils;
import net.thevpc.nuts.elem.NElement;

public class NTxFunctionSinebowColor implements NTxFunction {
    @Override
    public String name() {
        return "sinebowColor";
    }

    @Override
    public NElement invoke(NTxFunctionCallContext args) {
        if (args.size() == 0) {
            return NElement.ofNull();
        }
        args.checkTooManyArgs(1);
        float ratio = args.evalArg(0, x -> NTxValue.of(x).asFloat(), "ratio", () -> 0.0f);
        return NTxElementUtils.toElement(NTxColorUtils.sinebow(ratio));
    }
}
