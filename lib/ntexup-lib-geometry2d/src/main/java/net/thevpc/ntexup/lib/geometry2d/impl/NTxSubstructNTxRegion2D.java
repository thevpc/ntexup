package net.thevpc.ntexup.lib.geometry2d.impl;

import net.thevpc.ntexup.lib.geometry2d.NTxRegion2D;

public class NTxSubstructNTxRegion2D extends NTxRegion2DBase {
    private NTxRegion2D first;
    private NTxRegion2D second;

    public NTxSubstructNTxRegion2D(NTxRegion2D first, NTxRegion2D second) {
        this.first = first;
        this.second = second;
    }
    public NTxRegion2D first(){
        return first;
    }
    public NTxRegion2D second(){
        return second;
    }

    @Override
    public boolean isEmpty() {
        if (!first.isEmpty()) {
            return false;
        }
        if (!second.isEmpty()) {
            return false;
        }
        return true;
    }

}
