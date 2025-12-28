package net.thevpc.ntexup.extension.commonfunctions.colors;

import net.thevpc.ntexup.api.eval.NTxFunctionArgs;
import net.thevpc.ntexup.api.eval.NTxFunctionContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.util.NTxElementUtils;
import net.thevpc.ntexup.extension.commonfunctions.util.NTxColorUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;

public class NTxFunctionSinebowColor implements NTxFunction {
    @Override
    public String name() {
        return "sinebowColor";
    }

    @Override
    public NElement invoke(NTxFunctionArgs args, NTxFunctionContext context) {
        if (args.size() == 0) {
            return NElement.ofNull();
        }
        if (args.size() > 1) {
            context.log().log(NMsg.ofC("%s: expected 1 argument, got %s", NMsg.ofStyledKeyword(name()), args.size()));
        }
        float c = NTxValue.of(args.eval(0)).asFloat().get();
        return NTxElementUtils.toElement(NTxColorUtils.sinebow(c));
    }
}
