package net.thevpc.ntexup.api.extension;

import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxFunctionArgs;
import net.thevpc.nuts.elem.NElement;

public interface NTxFunction {
    String name();

    NElement invoke(NTxFunctionArgs args, NTxResolutionContext context);
}
