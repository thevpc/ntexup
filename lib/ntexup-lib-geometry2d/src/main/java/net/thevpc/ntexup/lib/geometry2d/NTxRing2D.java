package net.thevpc.ntexup.lib.geometry2d;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;

import java.util.List;

public interface NTxRing2D {
    List<NTxPoint2D> getPoints();

    NTxBounds2D getBounds();
}
