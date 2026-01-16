package net.thevpc.ntexup.lib.geometry2d.impl;

import net.thevpc.ntexup.lib.geometry2d.NTxPolygonWithHoles2D;
import net.thevpc.ntexup.lib.geometry2d.NTxRing2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NTxPolygonWithHoles2DImpl extends NTxRegion2DBase implements NTxPolygonWithHoles2D {
    private NTxRing2D exterior;
    private List<NTxRing2D> holes;

    public NTxPolygonWithHoles2DImpl(NTxRing2D exterior, List<NTxRing2D> holes) {
        this.exterior = exterior;
        this.holes = Collections.unmodifiableList(new ArrayList<>(holes));
    }

    @Override
    public NTxRing2D exterior() {
        return exterior;
    }

    @Override
    public List<NTxRing2D> holes() {
        return holes;
    }
}
