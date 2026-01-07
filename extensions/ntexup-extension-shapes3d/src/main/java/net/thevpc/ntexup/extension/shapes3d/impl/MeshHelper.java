package net.thevpc.ntexup.extension.shapes3d.impl;

import net.thevpc.ntexup.extension.shapes3d.api.NTxPoint3D;
import net.thevpc.ntexup.extension.shapes3d.api.primitives.NtxElement3DPolygon;
import net.thevpc.ntexup.extension.shapes3d.api.primitives.NtxElement3DTriangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MeshHelper {


    public static List<NtxElement3DTriangle> initialTriangulation(NtxElement3DPolygon poly) {
        boolean fill = poly.isFill();
        boolean contour = poly.isContour();
        NTxPoint3D[] pts = poly.points();
        List<NtxElement3DTriangle> triangles = new ArrayList<>();
        if (pts.length < 3) return triangles;

        // Standard Fan Triangulation
        for (int i = 1; i < pts.length - 1; i++) {
            NtxElement3DTriangle tri = new NtxElement3DTriangle(pts[0], pts[i], pts[i + 1],fill,contour);
            triangles.add(tri);
        }
        return triangles;
    }
    public static void refineTriangle(NtxElement3DTriangle tri, List<NtxElement3DTriangle> out, double maxEdge) {
        boolean fill = tri.isFill();
        boolean contour = tri.isContour();
        NTxPoint3D[] p = tri.points();
        double d01 = p[0].distance(p[1]);
        double d12 = p[1].distance(p[2]);
        double d20 = p[2].distance(p[0]);

        if (d01 > maxEdge || d12 > maxEdge || d20 > maxEdge) {
            // Split into 4 smaller triangles using midpoints
            NTxPoint3D m01 = midpoint(p[0], p[1]);
            NTxPoint3D m12 = midpoint(p[1], p[2]);
            NTxPoint3D m20 = midpoint(p[2], p[0]);

            refineTriangle(new NtxElement3DTriangle(p[0], m01, m20,fill, contour), out, maxEdge);
            refineTriangle(new NtxElement3DTriangle(p[1], m12, m01,fill, contour), out, maxEdge);
            refineTriangle(new NtxElement3DTriangle(p[2], m20, m12,fill, contour), out, maxEdge);
            refineTriangle(new NtxElement3DTriangle(m01, m12, m20,fill, contour), out, maxEdge);
        } else {
            out.add(tri);
        }
    }
    public static List<NtxElement3DTriangle> splitTriangle(NtxElement3DTriangle tri) {
        NTxPoint3D[] p = tri.points();
        double d01 = p[0].distance(p[1]);
        double d12 = p[1].distance(p[2]);
        double d20 = p[2].distance(p[0]);

        boolean fill = tri.isFill();
        boolean contour = tri.isContour();
        // Find the longest edge and split it in the middle
        if (d01 >= d12 && d01 >= d20) {
            NTxPoint3D m = midpoint(p[0], p[1]);
            return Arrays.asList(new NtxElement3DTriangle(p[2], p[0], m, fill,contour), new NtxElement3DTriangle(p[2], m, p[1], fill,contour));
        } else if (d12 >= d01 && d12 >= d20) {
            NTxPoint3D m = midpoint(p[1], p[2]);
            return Arrays.asList(new NtxElement3DTriangle(p[0], p[1], m, fill,contour), new NtxElement3DTriangle(p[0], m, p[2], fill,contour));
        } else {
            NTxPoint3D m = midpoint(p[2], p[0]);
            return Arrays.asList(new NtxElement3DTriangle(p[1], p[2], m, fill,contour), new NtxElement3DTriangle(p[1], m, p[0], fill,contour));
        }
    }

    private static NTxPoint3D midpoint(NTxPoint3D a, NTxPoint3D b) {
        return new NTxPoint3D(
                (a.x + b.x) / 2,
                (a.y + b.y) / 2,
                (a.z + b.z) / 2
        );
    }
}
