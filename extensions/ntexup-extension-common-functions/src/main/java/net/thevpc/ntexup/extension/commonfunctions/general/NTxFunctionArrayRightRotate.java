package net.thevpc.ntexup.extension.commonfunctions.general;

import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.eval.NTxFunctionArgs;
import net.thevpc.ntexup.api.eval.NTxFunctionContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

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
    public NElement invoke(NTxFunctionArgs args, NTxFunctionContext context) {
        if (args.size() == 0) {
            return NElement.ofNull();
        }
        if (args.size() == 1) {
            return args.eval(0);
        }
        if (args.size() > 2) {
            context.log().log(NMsg.ofC("%s: expected 2 arguments, got %s", NMsg.ofStyledKeyword(name()), args.size()));
        }
        NElement ue0 = args.eval(0);
        NOptional<NElement[]> uv0 = NTxValue.of(ue0).asElementArray();
        if (!uv0.isPresent()) {
            context.log().log(NMsg.ofC("unable to call %s, first arg '%s' is not a color array", NMsg.ofStyledKeyword(name()), NTxUtils.snippet(ue0)));
            return ue0;
        }

        NElement ue1 = args.eval(1);
        NOptional<Number> uv1 = NTxValue.of(ue1).asNumber();
        if (!uv1.isPresent()) {
            context.log().log(NMsg.ofC("unable to call %s, second arg '%s' is not a number", NMsg.ofStyledKeyword(name()), NTxUtils.snippet(ue1)));
            return ue0;
        }
        if (uv0.get().length == 0) {
            return ue0;
        }
        List<NElement> list = new ArrayList<>(Arrays.asList(uv0.get()));
        Collections.rotate(list, uv1.get().intValue());
        return NElement.ofArray(list.toArray(new NElement[0]));

    }
}
