package net.thevpc.ntexup.extension.simple3d;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem3d.NTxCamera3D;
import net.thevpc.ntexup.api.document.elem3d.NTxPoint3D;

public class NTxProjection3D {
    double focalLength;

    public NTxProjection3D(double focalLength) {
        this.focalLength = focalLength;
    }

    // Project a 3D point onto a 2D plane
    public NTxPoint2D project(NTxPoint3D point) {
        double factor = focalLength / (focalLength + point.z);
        int x = (int) (point.x * factor);
        int y = (int) (point.y * factor);
        return new NTxPoint2D(x, y);
    }
}
