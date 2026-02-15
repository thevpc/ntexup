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

public class NTxFunctionComplementaryColor implements NTxFunction {
    @Override
    public String name() {
        return "complementaryColor";
    }

    @Override
    public NElement invoke(NTxFunctionArgs args, NTxResolutionContext context) {
        if (args.checkTooFewArgs(1)) {
            return NElement.ofNull();
        }
        args.checkTooManyArgs(1);
        Color c = args.eval(0, x -> NTxValue.of(x).asColor(), "color", null);
        if(c==null){
            return NElement.ofNull();
        }
        return NTxElementUtils.toElement(NTxColorUtils.complementary(c));
    }
}