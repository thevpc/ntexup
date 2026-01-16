package net.thevpc.ntexup.lib.geometry2d.impl;

import net.thevpc.ntexup.lib.geometry2d.NTxRegion2D;
import net.thevpc.ntexup.lib.geometry2d.NTxRegion2DGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NTxRegion2DGroupImpl extends NTxRegion2DBase implements NTxRegion2DGroup {
    private List<NTxRegion2D> children = new ArrayList<>();

    public NTxRegion2DGroupImpl(List<? extends NTxRegion2D> children) {
        this.children = Collections.unmodifiableList(children);
    }

    @Override
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
