package net.thevpc.ntexup.lib.geometry2d.impl;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.lib.geometry2d.NTxEmptyRegion2D;


public class NTxEmptyRegion2DImpl extends NTxRegion2DBase implements NTxEmptyRegion2D {
    public static final NTxEmptyRegion2D INSTANCE = new NTxEmptyRegion2DImpl();

    public NTxEmptyRegion2DImpl() {
    }

    @Override
    public NTxBounds2D getBounds() {
        return NTxBounds2D.ofEmpty();
    }
}
