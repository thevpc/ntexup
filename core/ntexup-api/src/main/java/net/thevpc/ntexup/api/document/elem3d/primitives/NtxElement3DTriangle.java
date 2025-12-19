package net.thevpc.ntexup.api.document.elem3d.primitives;

import net.thevpc.ntexup.api.document.elem3d.AbstractNTxElement3DPrimitive;
import net.thevpc.ntexup.api.document.elem3d.NTxElement3DPrimitiveType;
import net.thevpc.ntexup.api.document.elem3d.NTxPoint3D;

public class NtxElement3DTriangle extends AbstractNTxElement3DPrimitive {
    NTxPoint3D p1;
    NTxPoint3D p2;
    NTxPoint3D p3;
    boolean fill = false;
    boolean contour = false;

    public NtxElement3DTriangle(NTxPoint3D p1, NTxPoint3D p2, NTxPoint3D p3, boolean fill, boolean contour) {
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.fill = fill;
        this.contour = contour;
    }

    public NtxElement3DTriangle setFilled(boolean filled) {
        this.fill = filled;
        return this;
    }

    public NtxElement3DTriangle setContour(boolean contour) {
        this.contour = contour;
        return this;
    }

    public NTxPoint3D getP1() {
        return p1;
    }

    public NTxPoint3D getP2() {
        return p2;
    }

    public NTxPoint3D getP3() {
        return p3;
    }

    public boolean isFill() {
        return fill;
    }

    public boolean isContour() {
        return contour;
    }

    @Override
    public NTxElement3DPrimitiveType type() {
        return NTxElement3DPrimitiveType.TRIANGLE;
    }

    @Override
    public NTxPoint3D[] points() {
        return new NTxPoint3D[]{p1,p2,p3};
    }

}
