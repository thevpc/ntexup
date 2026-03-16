package net.thevpc.ntexup.extension.commonfunctions.colors;

import net.thevpc.ntexup.api.eval.*;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.util.NTxElementUtils;
import net.thevpc.ntexup.extension.commonfunctions.util.NTxColorUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;

import java.awt.*;

public class NTxFunctionInvertColor implements NTxFunction {
    @Override
    public String name() {
        return "invertColor";
    }

    @Override
    public NElement invoke(NTxFunctionCallContext args) {
        if (args.size() == 0) {
            return NElement.ofNull();
        }
        if (args.size() > 1) {
            args.scopedContext().log().log(NMsg.ofC("%s: expected 1 argument, got %s",NMsg.ofStyledKeyword(name()), args.size()));
        }
        Color c = NTxValue.of(args.evalArg(0)).asColor().get();
        if(c==null){
            return NElement.ofNull();
        }
        return NTxElementUtils.toElement(NTxColorUtils.invert(c));
    }
}
