package net.thevpc.ntexup.extension.shapes3d;

import net.thevpc.ntexup.api.document.elem3d.NTxPoint3D;
import net.thevpc.ntexup.api.document.elem3d.NTxVector3D;

public class NTxD3Utils {

    public static NTxVector3D surfaceNormal(NTxPoint3D[] all) {
        return  surfaceNormal(all[0], all[1], all[2]);
    }

    public static NTxVector3D surfaceNormal(NTxPoint3D p1, NTxPoint3D p2, NTxPoint3D p3) {
        return surfaceNormal(p1.asVector(), p2.asVector(), p3.asVector());
    }

    public static NTxVector3D surfaceNormal(NTxVector3D p1, NTxVector3D p2, NTxVector3D p3) {
        return ((p2.minus(p1)).cross(p3.minus(p1))).normalize();
    }

    public static NTxPoint3D computeCentroid(NTxPoint3D[] points) {
        double x = 0, y = 0, z = 0;
        for (NTxPoint3D p : points) {
            x += p.x; y += p.y; z += p.z;
        }
        int n = points.length;
        return new NTxPoint3D(x/n, y/n, z/n);
    }

}
