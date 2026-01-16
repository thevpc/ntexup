package net.thevpc.ntexup.lib.geometry2d;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;

import java.util.List;

public interface NTxRectangle2D extends NTxRegion2D {
    NTxPoint2D position();

    NTxSize2D size();

    List<NTxPoint2D> points();
}
