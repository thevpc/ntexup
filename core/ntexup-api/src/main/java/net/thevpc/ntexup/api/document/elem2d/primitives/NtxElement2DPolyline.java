package net.thevpc.ntexup.api.document.elem2d.primitives;

import net.thevpc.ntexup.api.document.NTxArrow;
import net.thevpc.ntexup.api.document.elem2d.NTxElement2DPrimitiveType;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;

public class NtxElement2DPolyline extends AbstractNtxElement2DPrimitive {
    private NTxPoint2D[] nodes;
    private NTxArrow startArrow;
    private NTxArrow endArrow;

    public NtxElement2DPolyline(NTxPoint2D[] nodes) {
        this.nodes = nodes;
    }

    public NTxPoint2D[] getNodes() {
        return nodes;
    }

    public NTxArrow getStartArrow() {
        return startArrow;
    }

    public NtxElement2DPolyline setStartArrow(NTxArrow startArrow) {
        this.startArrow = startArrow;
        return this;
    }

    public NTxArrow getEndArrow() {
        return endArrow;
    }

    public NtxElement2DPolyline setEndArrow(NTxArrow endArrow) {
        this.endArrow = endArrow;
        return this;
    }

    @Override
    public NTxElement2DPrimitiveType type() {
        return NTxElement2DPrimitiveType.POLYLINE;
    }
}
