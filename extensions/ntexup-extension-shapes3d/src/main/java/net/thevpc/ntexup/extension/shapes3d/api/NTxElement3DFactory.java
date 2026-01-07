package net.thevpc.ntexup.extension.shapes3d.api;

import net.thevpc.ntexup.api.document.NTxArrow;
import net.thevpc.ntexup.extension.shapes3d.api.composite.NtxElement3DBox;
import net.thevpc.ntexup.extension.shapes3d.api.composite.NtxElement3DGroup;
import net.thevpc.ntexup.extension.shapes3d.api.composite.NtxElement3DSurface;
import net.thevpc.ntexup.extension.shapes3d.api.composite.NtxElement3DUVSphere;
import net.thevpc.ntexup.extension.shapes3d.api.primitives.NtxElement3DLine;
import net.thevpc.ntexup.extension.shapes3d.api.primitives.NtxElement3DPolygon;
import net.thevpc.ntexup.extension.shapes3d.api.primitives.NtxElement3DPolyline;
import net.thevpc.nuts.util.NDoubleFunction2;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NTxElement3DFactory {
    public static NtxElement3DGroup group() {
        return new NtxElement3DGroup();
    }

    public static NtxElement3D axis(NTxPoint3D origin, double length) {
        NtxElement3DGroup g = group();
        g.add(segment(origin, new NTxVector3D(1, 0, 0), length).setEndArrow(new NTxArrow()).setLinePaint(Color.BLUE));
        g.add(segment(origin, new NTxVector3D(0, 1, 0), length).setEndArrow(new NTxArrow()).setLinePaint(Color.GREEN));
        g.add(segment(origin, new NTxVector3D(0, 0, 1), length).setEndArrow(new NTxArrow()).setLinePaint(Color.RED));
        return g;
    }

    public static NtxElement3DLine segment(NTxPoint3D origin, NTxVector3D orientation, double length) {
        double x1 = origin.x;
        double y1 = origin.y;
        double z1 = origin.z;
        NTxVector3D oo = orientation.normalize();
        return line(origin,
                new NTxPoint3D(
                        x1 + length * oo.x,
                        y1 + length * oo.y,
                        z1 + length * oo.z
                )
        );
    }

    public static NtxElement3DLine line(NTxPoint3D a, NTxPoint3D b) {
        return new NtxElement3DLine(a, b);
    }

    public static NtxElement3D cube(NTxPoint3D origin, double size) {
        return box(origin, size, size, size);
    }

    public static NtxElement3DPolygon polygon(NTxPoint3D... points) {
        return new NtxElement3DPolygon(points, true, true);
    }

    public static NtxElement3DSurface surface(NTxPoint3D... points) {
        return new NtxElement3DSurface(points);
    }

    public static NtxElement3DSurface surface(double[] x, double[] y, NDoubleFunction2 f) {
        List<NTxPoint3D> all = new ArrayList<>();
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < y.length; j++) {
                all.add(new NTxPoint3D(x[i], y[j], f.apply(x[i], y[j])));
            }
        }
        return new NtxElement3DSurface(all.toArray(new NTxPoint3D[0]));
    }

    public static NtxElement3DPolyline polyline(NTxPoint3D... points) {
        return new NtxElement3DPolyline(points);
    }

    public static NtxElement3D box(NTxPoint3D origin, double sizeX, double sizeY, double sizeZ) {
        return new NtxElement3DBox(origin, sizeX, sizeY, sizeZ);
    }

    public static NtxElement3D sphereUV(NTxPoint3D origin, double radiusX, double radiusY, double radiusZ, int meridians, int parallels) {
        return new NtxElement3DUVSphere(origin, radiusX, radiusY, radiusZ, meridians, parallels);
    }

    public static NtxElement3D sphereUV(NTxPoint3D origin, double radius, int meridians, int parallels) {
        return sphereUV(origin, radius, radius, radius, meridians, parallels);
    }

    public static NtxElement3D sphereUV(NTxPoint3D origin, double radius, int meridians) {
        return sphereUV(origin, radius, radius, radius, meridians, meridians);
    }
}
