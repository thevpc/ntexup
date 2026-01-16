package net.thevpc.ntexup.lib.geometry3d.impl.composite;

import net.thevpc.ntexup.lib.geometry2d.NTxRegion2D;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;
import net.thevpc.ntexup.lib.geometry3d.NtxFace;
import net.thevpc.ntexup.lib.geometry3d.impl.AbstractNTxElement3D;

public class NtxElement3DPrism extends AbstractNTxElement3D {
    private NTxPoint3D position;
    private NTxRegion2D region;
    private double thickness;
    private NtxFace top;
    private NtxFace bottom;
    private NtxFace side;

    public NtxElement3DPrism(NTxPoint3D position, NTxRegion2D region, double thickness) {
        this.position = position;
        this.region=region;
        this.thickness = thickness;
    }

    public NTxPoint3D position() {
        return position;
    }

    public NTxRegion2D region() {
        return region;
    }

    public double thickness() {
        return thickness;
    }

    public NtxFace getTop() {
        return top;
    }

    public NtxElement3DPrism setTop(NtxFace top) {
        this.top = top;
        return this;
    }

    public NtxFace getBottom() {
        return bottom;
    }

    public NtxElement3DPrism setBottom(NtxFace bottom) {
        this.bottom = bottom;
        return this;
    }

    public NtxFace getSide() {
        return side;
    }

    public NtxElement3DPrism setSide(NtxFace side) {
        this.side = side;
        return this;
    }
}
