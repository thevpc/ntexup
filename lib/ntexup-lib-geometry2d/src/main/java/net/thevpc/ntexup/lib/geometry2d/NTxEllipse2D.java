package net.thevpc.ntexup.lib.geometry2d;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;

public interface NTxEllipse2D extends NTxRegion2D {
    NTxPoint2D center();

    NTxSize2D radius();
}
