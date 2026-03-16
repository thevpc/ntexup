package net.thevpc.ntexup.api.extension;

import net.thevpc.ntexup.api.eval.NTxFunctionCallContext;
import net.thevpc.nuts.elem.NElement;

public interface NTxFunction {
    String name();

    NElement invoke(NTxFunctionCallContext args);
}
