package net.thevpc.ntexup.lib.geometry2d.impl;

import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.lib.geometry2d.NTxRing2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NTxRing2DImpl implements NTxRing2D {
    private List<NTxPoint2D> points;
    private NTxBounds2D bounds;

    public NTxRing2DImpl(List<NTxPoint2D> points) {
        List<NTxPoint2D> points2 = new ArrayList<>();
        NTxBounds2DBuilder bb = new NTxBounds2DBuilder();
        if (points != null) {
            for (NTxPoint2D point : points) {
                if (point != null) {
                    bb.add(point);
                    points2.add(point);
                }
            }
        }
        this.points = Collections.unmodifiableList(points2);
        this.bounds = bb.build();
    }

    @Override
    public List<NTxPoint2D> getPoints() {
        return points;
    }

    @Override
    public NTxBounds2D getBounds() {
        return bounds;
    }
}
