package net.thevpc.ntexup.extension.commonfunctions.general;

import net.thevpc.ntexup.api.eval.NTxFutureObj;
import net.thevpc.ntexup.api.eval.NTxFutureUtils;
import net.thevpc.ntexup.api.eval.NTxObj;
import net.thevpc.nuts.util.NOptional;

import java.util.function.Supplier;

public class NTxCombinedFutureObj extends NTxFutureObj {
    private final Object[] dependencies;

    public NTxCombinedFutureObj(String name, Supplier<NTxObj> resolver, Object... dependencies) {
        super(name, null, resolver);
        this.dependencies = dependencies != null ? dependencies : new Object[0];
        for (Object dep : this.dependencies) {
            NTxFutureUtils.addListener(dep, () -> {
                if (isDone()) {
                    triggerListeners();
                }
            });
        }
    }

    @Override
    public boolean isDone() {
        if (super.isDone()) {
            return true;
        }
        for (Object dep : dependencies) {
            if (!NTxFutureUtils.isReady(dep)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public NOptional<NTxObj> get(String property) {
        if (isDone()) {
            NTxObj r = get();
            return r == null ? NOptional.ofEmpty() : r.get(property);
        }
        String childName = name() + "." + property;
        NTxCombinedChildFutureObj child = new NTxCombinedChildFutureObj(childName, this, property);
        this.addListener(child::triggerListeners);
        return NOptional.of(child);
    }
}
