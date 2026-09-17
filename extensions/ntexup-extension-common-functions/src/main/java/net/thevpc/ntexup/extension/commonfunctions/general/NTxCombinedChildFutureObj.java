package net.thevpc.ntexup.extension.commonfunctions.general;

import net.thevpc.ntexup.api.eval.NTxFutureObj;
import net.thevpc.ntexup.api.eval.NTxObj;
import net.thevpc.nuts.util.NOptional;

public class NTxCombinedChildFutureObj extends NTxFutureObj {
    private final NTxFutureObj parent;
    private final String property;

    public NTxCombinedChildFutureObj(String name, NTxFutureObj parent, String property) {
        super(name, null, () -> {
            NTxObj p = parent.get();
            if (p != null) {
                return p.get(property).orNull();
            }
            return null;
        });
        this.parent = parent;
        this.property = property;
    }

    @Override
    public boolean isDone() {
        if (super.isDone()) {
            return true;
        }
        return parent != null && parent.isDone();
    }

    @Override
    public NOptional<NTxObj> get(String subProp) {
        if (isDone()) {
            NTxObj r = get();
            return r == null ? NOptional.ofEmpty() : r.get(subProp);
        }
        String childName = name() + "." + subProp;
        NTxCombinedChildFutureObj child = new NTxCombinedChildFutureObj(childName, this, subProp);
        this.addListener(child::triggerListeners);
        return NOptional.of(child);
    }
}
