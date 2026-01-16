package net.thevpc.ntexup.lib.geometry2d;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;

import java.util.List;

public interface NTxTriangle2D extends NTxRegion2D {
    NTxPoint2D getPoint1();
    NTxPoint2D getPoint2();
    NTxPoint2D getPoint3();
    List<NTxPoint2D> points();
    NTxPolygon2D toPolygon();
}
