package net.thevpc.ntexup.lib.geometry2d.impl;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.lib.geometry2d.NTxPolygon2D;
import net.thevpc.ntexup.lib.geometry2d.NTxTriangle2D;

import java.util.Arrays;
import java.util.List;

public class NTxTriangle2DImpl extends NTxRegion2DBase implements NTxTriangle2D {
    private List<NTxPoint2D> points;

    public NTxTriangle2DImpl(NTxPoint2D a, NTxPoint2D b, NTxPoint2D c) {
        this.points = Arrays.asList(new NTxPoint2D[]{a, b, c});
    }

    @Override
    public List<NTxPoint2D> points() {
        return points;
    }

    @Override
    public NTxPoint2D getPoint3() {
        return points.get(2);
    }

    @Override
    public NTxPoint2D getPoint2() {
        return points.get(1);
    }

    @Override
    public NTxPoint2D getPoint1() {
        return points.get(0);
    }

    public NTxPolygon2D toPolygon() {
        return new NTxPolygon2DImpl(points());
    }

}
