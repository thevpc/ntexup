package net.thevpc.ntexup.api.eval;

import net.thevpc.nuts.elem.NElement;

public class NTxObjs {
    public static NTxObjFromElem elem(NElement elem) {
        return new NTxObjFromElem(elem);
    }

    public static NTxObjFromMap map(){
        return new NTxObjFromMap();
    }

    public static NTxFutureObj future(String name, java.util.concurrent.Future<?> future, java.util.function.Supplier<NTxObj> resolver) {
        return new NTxFutureObj(name, future, resolver);
    }
}
