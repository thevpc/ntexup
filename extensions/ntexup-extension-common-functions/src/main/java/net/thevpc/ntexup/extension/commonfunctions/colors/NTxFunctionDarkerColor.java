package net.thevpc.ntexup.extension.commonfunctions.colors;

import net.thevpc.ntexup.api.eval.NTxFunctionArgs;
import net.thevpc.ntexup.api.eval.NTxFunctionContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.util.NTxElementUtils;
import net.thevpc.ntexup.extension.commonfunctions.util.NTxColorUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;

import java.awt.*;

public class NTxFunctionDarkerColor implements NTxFunction {
    @Override
    public String name() {
        return "darkerColor";
    }

    @Override
    public NElement invoke(NTxFunctionArgs args, NTxFunctionContext context) {
        if (args.size() == 0) {
            return NElement.ofNull();
        }
        Color c = NTxValue.of(args.eval(0)).asColor().get();
        if (args.size() == 1) {
            return NTxElementUtils.toElement(NTxColorUtils.darker(c));
        }
        if (args.size() > 2) {
            context.log().log(NMsg.ofC("%s: expected 2 arguments, got %s", NMsg.ofStyledKeyword(name()), args.size()));
        }
        float degrees = NTxValue.of(args.eval(1)).asFloat().get();
        return NTxElementUtils.toElement(NTxColorUtils.darker(c, degrees));
    }
}
