package net.thevpc.ntexup.extension.commonfunctions.colors;

import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.eval.NTxFunctionCallContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.util.NTxElementUtils;
import net.thevpc.ntexup.extension.commonfunctions.util.NTxColorUtils;
import net.thevpc.nuts.elem.NElement;

import java.awt.*;

public class NTxFunctionComplementaryColor implements NTxFunction {
    @Override
    public String name() {
        return "complementaryColor";
    }

    @Override
    public NElement invoke(NTxFunctionCallContext args) {
        if (args.checkTooFewArgs(1)) {
            return NElement.ofNull();
        }
        args.checkTooManyArgs(1);
        Color c = args.evalArg(0, x -> NTxValue.of(x).asColor(), "color", null);
        if(c==null){
            return NElement.ofNull();
        }
        return NTxElementUtils.toElement(NTxColorUtils.complementary(c));
    }
}