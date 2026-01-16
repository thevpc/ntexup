package net.thevpc.ntexup.lib.geometry2d;

import java.util.List;

public interface NTxPolygonWithHoles2D extends NTxRegion2D {
    NTxRing2D exterior();
    List<NTxRing2D> holes();
}
