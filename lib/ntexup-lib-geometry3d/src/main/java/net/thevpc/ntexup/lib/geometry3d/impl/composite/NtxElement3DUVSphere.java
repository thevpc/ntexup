package net.thevpc.ntexup.lib.geometry3d.impl.composite;

import net.thevpc.ntexup.lib.geometry3d.impl.AbstractNTxElement3D;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;

public class NtxElement3DUVSphere extends AbstractNTxElement3D {
    private NTxPoint3D position;
    private int meridians;
    private int parallels;
    private double radiusX;
    private double radiusY;
    private double radiusZ;
    private boolean showMesh = true;

    public NtxElement3DUVSphere(NTxPoint3D position
            , double radiusX
            , double radiusY
            , double radiusZ
            , int meridians, int parallels) {
        this.position = position;
        this.parallels = parallels;
        this.meridians = meridians;
        this.radiusX = radiusX;
        this.radiusY = radiusY;
        this.radiusZ = radiusZ;
    }

    public NTxPoint3D getPosition() {
        return position;
    }

    public int getMeridians() {
        return meridians;
    }

    public int getParallels() {
        return parallels;
    }

    public double getRadiusX() {
        return radiusX;
    }

    public double getRadiusY() {
        return radiusY;
    }

    public double getRadiusZ() {
        return radiusZ;
    }

    public boolean isShowMesh() {
        return showMesh;
    }

}
