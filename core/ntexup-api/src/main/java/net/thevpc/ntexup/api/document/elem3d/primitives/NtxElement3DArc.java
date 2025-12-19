package net.thevpc.ntexup.api.document.elem3d.primitives;

import net.thevpc.ntexup.api.document.elem3d.AbstractNTxElement3DPrimitive;
import net.thevpc.ntexup.api.document.elem3d.NTxElement3DPrimitiveType;
import net.thevpc.ntexup.api.document.elem3d.NTxPoint3D;
import net.thevpc.ntexup.api.document.elem3d.NtxElement3D;

public class NtxElement3DArc extends AbstractNTxElement3DPrimitive {
    private NTxPoint3D from;
    private NTxPoint3D to;
    private double startAngle;
    private double endAngle;

    public NtxElement3DArc(NTxPoint3D from, NTxPoint3D to, double startAngle, double endAngle) {
        this.from = from;
        this.to = to;
        this.startAngle = startAngle;
        this.endAngle = endAngle;
    }

    public double getStartAngle() {
        return startAngle;
    }

    public double getEndAngle() {
        return endAngle;
    }

    public NTxPoint3D getFrom() {
        return from;
    }

    public NTxPoint3D getTo() {
        return to;
    }

    @Override
    public NTxElement3DPrimitiveType type() {
        return NTxElement3DPrimitiveType.ARC;
    }

    @Override
    public NTxPoint3D[] points() {
        return new NTxPoint3D[]{from,to};
    }
}
