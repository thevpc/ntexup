package net.thevpc.ntexup.extension.commonfunctions.colors;

import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.eval.NTxFunctionCallContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.util.NTxElementUtils;
import net.thevpc.ntexup.extension.commonfunctions.util.NTxColorUtils;
import net.thevpc.nuts.elem.NElement;

import java.awt.*;

public class NTxFunctionColorSaturation implements NTxFunction {
    @Override
    public String name() {
        return "colorSaturation";
    }

    @Override
    public NElement invoke(NTxFunctionCallContext args) {
        if (args.checkTooFewArgs(1)) {
            return NElement.ofNull();
        }
        args.checkTooManyArgs(2);
        Color c = args.evalArg(0, x -> NTxValue.of(x).asColor(), "color", null);
        if(c==null){
            return NElement.ofNull();
        }
        if (args.size() == 1) {
            return NTxElementUtils.toElement(c);
        }
        float degrees = args.evalArg(1, x -> NTxValue.of(x).asFloat(), "degrees", () -> 0.0f);
        return NTxElementUtils.toElement(NTxColorUtils.adjustSaturation(c, degrees));
    }
}
