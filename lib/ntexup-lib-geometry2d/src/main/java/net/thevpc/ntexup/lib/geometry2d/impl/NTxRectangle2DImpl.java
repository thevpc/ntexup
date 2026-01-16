package net.thevpc.ntexup.lib.geometry2d.impl;

import net.thevpc.ntexup.lib.geometry2d.NTxSize2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.lib.geometry2d.NTxPolygon2D;
import net.thevpc.ntexup.lib.geometry2d.NTxRectangle2D;

import java.util.Arrays;
import java.util.List;

public class NTxRectangle2DImpl extends NTxRegion2DBase implements NTxRectangle2D {
    private NTxPoint2D position;
    private NTxSize2D size;

    public NTxRectangle2DImpl(NTxPoint2D position, NTxSize2D size) {
        this.position = position;
        this.size = size;
    }

    public NTxPoint2D position() {
        return position;
    }

    public NTxSize2D size() {
        return size;
    }

    @Override
    public List<NTxPoint2D> points() {
        return Arrays.asList(
                position,                     // (x, y)           → bottom-left
                position.add(size.x, 0),      // (x + w, y)       → bottom-right
                position.add(size.x, size.y), // (x + w, y + h)   → top-right
                position.add(0, size.y)       // (x, y + h)       → top-left
        );
    }

    public NTxPolygon2D toPolygon() {
        return new NTxPolygon2DImpl(points());
    }
}
