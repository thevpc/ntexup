package net.thevpc.ntexup.api.eval;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;

import java.util.Set;

public interface NTxObj {
    Set<String> properties();

    NOptional<NTxObj> get(String property);

    NElement toElement();
}
