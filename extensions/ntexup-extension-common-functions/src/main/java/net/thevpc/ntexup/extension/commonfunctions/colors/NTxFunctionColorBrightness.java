package net.thevpc.ntexup.extension.commonfunctions.colors;

import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.eval.NTxFunctionArgs;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.util.NTxElementUtils;
import net.thevpc.ntexup.extension.commonfunctions.util.NTxColorUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;

import java.awt.*;

public class NTxFunctionColorBrightness implements NTxFunction {
    @Override
    public String name() {
        return "colorBrightness";
    }

    @Override
    public NElement invoke(NTxFunctionArgs args, NTxResolutionContext context) {
        if (args.checkTooFewArgs(1)) {
            return NElement.ofNull();
        }
        args.checkTooManyArgs(2);
        Color c = args.eval(0, x -> NTxValue.of(x).asColor(), "color", null);
        if(c==null){
            return NElement.ofNull();
        }
        if (args.size() == 1) {
            return NTxElementUtils.toElement(c);
        }
        float degrees = args.eval(1, x -> NTxValue.of(x).asFloat(), "degrees", () -> 0.0f);
        return NTxElementUtils.toElement(NTxColorUtils.adjustBrightness(c, degrees));

    }
}
