package net.thevpc.ntexup.api.eval;

import net.thevpc.nuts.elem.NElement;

public class NTxObjs {
    public static NTxObjFromElem elem(NElement elem) {
        return new NTxObjFromElem(elem);
    }

    public static NTxObjFromMap map(){
        return new NTxObjFromMap();
    }
}
