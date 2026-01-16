package net.thevpc.ntexup.lib.geometry2d.impl;

import net.thevpc.ntexup.lib.geometry2d.NTxRegion2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NTxConcatNTxRegion2D extends NTxRegion2DBase {
    private List<NTxRegion2D> children = new ArrayList<>();

    public NTxConcatNTxRegion2D(List<NTxRegion2D> children) {
        this.children = Collections.unmodifiableList(children);
    }

    @Override
    public NTxRegion2D concat(NTxRegion2D region) {
        List<NTxRegion2D> all = new ArrayList<>(this.children);
        if (region instanceof NTxConcatNTxRegion2D) {
            all.addAll(((NTxConcatNTxRegion2D) region).children);
        } else {
            all.add(region);
        }
        return new NTxConcatNTxRegion2D(all);
    }
    public List<NTxRegion2D> children() {
        return children;
    }


    @Override
    public boolean isEmpty() {
        if (children.isEmpty()) {
            return true;
        }
        for (NTxRegion2D a : children) {
            if (!a.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
