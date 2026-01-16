package net.thevpc.ntexup.lib.geometry2d.impl;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.lib.geometry2d.NTxSize2D;
import net.thevpc.ntexup.lib.geometry2d.NTxEllipse2D;

public class NTxEllipse2DImpl extends NTxRegion2DBase implements NTxEllipse2D {
    private NTxPoint2D center;
    private NTxSize2D radius;

    public NTxEllipse2DImpl(NTxPoint2D center, NTxSize2D radius) {
        this.center = center;
        this.radius = radius;
    }

    @Override
    public NTxPoint2D center() {
        return center;
    }

    @Override
    public NTxSize2D radius() {
        return radius;
    }
}
