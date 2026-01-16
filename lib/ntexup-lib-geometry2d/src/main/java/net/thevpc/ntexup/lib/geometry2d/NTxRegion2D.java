package net.thevpc.ntexup.lib.geometry2d;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;

import java.util.List;

public interface NTxRegion2D {

    /**
     * Internal method for 3D rendering — not part of public API.
     * Converts region to list of polygons with holes.
     * Implemented by concrete classes.
     */
    List<NTxPolygonWithHoles2D> toPolygonsWithHolesInternal();

    NTxBounds2D getBounds();

    NTxRegion2D simplify();

    NTxRegion2D concat(NTxRegion2D region);

    NTxRegion2D substruct(NTxRegion2D region);

    NTxRegion2D intersect(NTxRegion2D region);

    NTxRegion2D reverseIntersect(NTxRegion2D region);

    boolean isEmpty();

}
