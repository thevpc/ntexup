package net.thevpc.ntexup.api.document.elem3d.primitives;

import net.thevpc.ntexup.api.document.elem3d.AbstractNTxElement3DPrimitive;
import net.thevpc.ntexup.api.document.elem3d.NTxElement3DPrimitiveType;
import net.thevpc.ntexup.api.document.elem3d.NTxPoint3D;

public class NtxElement3DPolyline extends AbstractNTxElement3DPrimitive {
    private NTxPoint3D[] nodes;

    public NtxElement3DPolyline(NTxPoint3D[] nodes) {
        this.nodes = nodes;
    }

    public NTxPoint3D[] getNodes() {
        return nodes;
    }

    @Override
    public NTxPoint3D[] points() {
        return nodes;
    }

    @Override
    public NTxElement3DPrimitiveType type() {
        return NTxElement3DPrimitiveType.POLYLINE;
    }
}
