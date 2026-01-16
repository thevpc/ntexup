package net.thevpc.ntexup.lib.geometry2d.impl;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.lib.geometry2d.NTxEmptyRegion2D;
import net.thevpc.ntexup.lib.geometry2d.NTxPolygonWithHoles2D;
import net.thevpc.ntexup.lib.geometry2d.NTxRegion2D;

import java.util.ArrayList;
import java.util.List;

public abstract class NTxRegion2DBase implements NTxRegion2D {
    @Override
    public NTxRegion2D concat(NTxRegion2D region) {
        List<NTxRegion2D> all = new ArrayList<>();
        all.add(this);
        all.add(region);
        return new NTxConcatNTxRegion2D(all);
    }

    @Override
    public NTxRegion2D intersect(NTxRegion2D region) {
        List<NTxRegion2D> all = new ArrayList<>();
        all.add(this);
        all.add(region);
        return new NTxIntersectNTxRegion2D(all);
    }

    @Override
    public NTxRegion2D substruct(NTxRegion2D region) {
        return new NTxSubstructNTxRegion2D(this, region);
    }

    @Override
    public NTxRegion2D reverseIntersect(NTxRegion2D region) {
        return new NTxReverseIntersectNTxRegion2D(this, region);
    }

    @Override
    public NTxRegion2D simplify() {
        return JTSHelper.simplify(this);
    }

    @Override
    public List<NTxPolygonWithHoles2D> toPolygonsWithHolesInternal() {
        return JTSHelper.toPolygonsWithHolesInternal(this);
    }

    @Override
    public NTxBounds2D getBounds() {
        return simplify().getBounds();
    }

    @Override
    public boolean isEmpty() {
        NTxRegion2D s = simplify();
        if (s instanceof NTxEmptyRegion2D) {
            return true;
        }
        return false;
    }



}
