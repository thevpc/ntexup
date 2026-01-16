package net.thevpc.ntexup.lib.geometry2d.impl;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.lib.geometry2d.NTxPolygon2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NTxPolygon2DImpl extends NTxRegion2DBase implements NTxPolygon2D {
    private List<NTxPoint2D> points;

    public NTxPolygon2DImpl(List<NTxPoint2D> points) {
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
    }

    @Override
    public List<NTxPoint2D> getPoints() {
        return points;
    }

}
