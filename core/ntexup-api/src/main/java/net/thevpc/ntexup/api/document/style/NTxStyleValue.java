package net.thevpc.ntexup.api.document.style;

import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.NTxAlign;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

public class NTxStyleValue {
    public static NOptional<NTxDouble2> toDouble2(Object svv) {
        if (svv == null) {
            return NOptional.ofNamedEmpty("Double2");
        }
        if (svv instanceof NTxDouble2) {
            return NOptional.of((NTxDouble2) svv);
        }
        if (svv instanceof NTxAlign) {
            return ((NTxAlign) svv).toPosition();
        }
        return NOptional.ofEmpty(NMsg.ofC("invalid Double2 from %s", svv));
    }
}
