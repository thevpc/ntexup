package net.thevpc.ntexup.extension.commonfunctions.general;

import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.eval.NTxFunctionCallContext;
import net.thevpc.nuts.elem.NElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NTxFunctionArrayRightRotate implements NTxFunction {
    @Override
    public String name() {
        return "arrayRightRotate";
    }

    @Override
    public NElement invoke(NTxFunctionCallContext args) {
        if (args.checkTooFewArgs(1)) {
            return NElement.ofNull();
        }
        args.checkTooManyArgs(2);
        NElement[] c = args.evalArg(0, x -> NTxValue.of(x).asElementArray(), "array", null);
        if (c == null || c.length == 0) {
            return NElement.ofNull();
        }
        Number n = args.size() == 1 ? 1 : args.evalArg(1, x -> NTxValue.of(x).asNumber(), "number", () -> 1);
        List<NElement> list = new ArrayList<>(Arrays.asList(c));
        Collections.rotate(list, n.intValue());
        return NElement.ofArray(list.toArray(new NElement[0]));
    }
}
