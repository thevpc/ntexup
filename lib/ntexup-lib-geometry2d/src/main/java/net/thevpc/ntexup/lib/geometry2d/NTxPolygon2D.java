package net.thevpc.ntexup.lib.geometry2d;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;

import java.util.List;

public interface NTxPolygon2D extends NTxRegion2D {
//    NTxPoint2D getCenter();
    List<NTxPoint2D> getPoints();
}
