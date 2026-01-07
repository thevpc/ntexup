package net.thevpc.ntexup.extension.shapes3d.api.composite;

import net.thevpc.ntexup.extension.shapes3d.api.AbstractNTxElement3D;
import net.thevpc.ntexup.extension.shapes3d.api.NTxPoint3D;

public class NtxElement3DBox extends AbstractNTxElement3D {
    private NTxPoint3D origin;
    private double sizeX;
    private double sizeY;
    private double sizeZ;

    public NtxElement3DBox(NTxPoint3D origin, double sizeX, double sizeY, double sizeZ) {
        this.origin = origin;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public NTxPoint3D getOrigin() {
        return origin;
    }

    public double getSizeX() {
        return sizeX;
    }

    public double getSizeY() {
        return sizeY;
    }

    public double getSizeZ() {
        return sizeZ;
    }

}
