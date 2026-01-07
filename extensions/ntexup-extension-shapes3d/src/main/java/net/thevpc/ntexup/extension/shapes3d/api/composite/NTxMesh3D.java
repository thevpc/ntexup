package net.thevpc.ntexup.extension.shapes3d.api.composite;

import net.thevpc.ntexup.extension.shapes3d.api.NTxPoint3D;
import net.thevpc.ntexup.extension.shapes3d.api.NTxVector3D;
import net.thevpc.ntexup.extension.shapes3d.api.primitives.NtxElement3DTriangle;

import java.util.ArrayList;
import java.util.List;

/**
 * thanks to https://github.com/caosdoar/spheres/blob/master/src/spheres.cpp
 */
public class NTxMesh3D {
    List<NTxPoint3D> vertices = new ArrayList<>();
    List<NTxMesh3DTriangleIndices> triangles = new ArrayList<>();

    public List<NTxPoint3D> getVertices() {
        return vertices;
    }

    public List<NTxMesh3DTriangleIndices> getTriangles() {
        return triangles;
    }

    public int size() {
        return triangles.size();
    }

    public NtxElement3DTriangle triangleAt(int index) {
        NTxMesh3DTriangleIndices t = triangles.get(index);
        return new NtxElement3DTriangle(
                vertices.get(t.p1),
                vertices.get(t.p2),
                vertices.get(t.p3)
                , true
                , true
        );
    }

    public void addTriangle(int a, int b, int c) {
        triangles.add(new NTxMesh3DTriangleIndices(a, b, c));
    }

    public void addQuad(int a, int b, int c, int d) {
        triangles.add(new NTxMesh3DTriangleIndices(a, b, c));
        triangles.add(new NTxMesh3DTriangleIndices(a, c, d));
    }

    public void addQuadAlt(int a, int b, int c, int d) {
        triangles.add(new NTxMesh3DTriangleIndices(a, b, d));
        triangles.add(new NTxMesh3DTriangleIndices(b, c, d));
    }

    public void clear() {
        vertices.clear();
        triangles.clear();
    }

    public double distance(NTxPoint3D p, int tidx) {
        NTxMesh3DTriangleIndices tr = triangles.get(tidx);
        int idx0 = tr.p1;
        int idx1 = tr.p2;
        int idx2 = tr.p3;
        NTxVector3D v0 = vertices.get(idx0).asVector();
        NTxVector3D v1 = vertices.get(idx1).asVector();
        NTxVector3D v2 = vertices.get(idx2).asVector();
        NTxVector3D bv = v0;
        NTxVector3D e0 = v1.minus(v0);
        NTxVector3D e1 = v2.minus(v0);
        NTxVector3D dv = bv.minus(p.asVector());
        double a = e0.dot(e0);
        double b = e0.dot(e1);
        double c = e1.dot(e1);
        double d = e0.dot(dv);
        double e = e1.dot(dv);
        double f = dv.dot(dv);

        double det = a * c - b * b;
        double s = b * e - c * d;
        double t = b * d - a * e;

        if (s + t <= det) {
            if (s < 0.0) {
                if (t < 0.0) {
                    // region 4
                    if (d < 0.0) {
                        t = 0.0;
                        s = -d >= a ? 1.0 : -d / a;
                    } else {
                        s = 0.0;
                        t = e >= 0.0 ? 0.0 : (-e >= c ? 1.0 : -e / c);
                    }
                } else {
                    // region 3
                    s = 0.0;
                    t = e >= 0.0 ? 0.0 : (-e >= c ? 1.0 : -e / c);
                }
            } else if (t < 0.0) {
                // region 5
                s = d >= 0.0 ? 0.0 : (-d >= a ? 1.0 : -d / a);
                t = 0.0;
            } else {
                // region 0
                double invDet = 1.0 / det;
                s *= invDet;
                t *= invDet;
            }
        } else {
            if (s < 0.0) {
                // region 2
                double tmp0 = b + d;
                double tmp1 = c + e;
                if (tmp1 > tmp0) {
                    double numer = tmp1 - tmp0;
                    double denom = a - 2.0 * b + c;
                    s = numer >= denom ? 1.0 : numer / denom;
                    t = 1.0 - s;
                } else {
                    s = 0.0;
                    t = (tmp1 <= 0.0 ? 1.0 : (e >= 0.0 ? 0.0 : -e / c));
                }
            } else if (t < 0.0) {
                // region 6
                double tmp0 = b + e;
                double tmp1 = a + d;
                if (tmp1 > tmp0) {
                    double numer = tmp1 - tmp0;
                    double denom = a - 2.0 * b + c;
                    t = numer >= denom ? 1.0 : numer / denom;
                    s = 1.0 - t;
                } else {
                    s = (tmp1 <= 0.0 ? 1.0 : (d >= 0.0 ? 0.0 : -d / a));
                    t = 0.0;
                }
            } else {
                // region 1
                double numer = c + e - b - d;
                if (numer <= 0) {
                    s = 0.0;
                } else {
                    double denom = a - 2.0 * b + c;
                    s = numer >= denom ? 1.0 : numer / denom;
                }
                t = 1.0 - s;
            }
        }

        return (p.asVector().minus((v0.plus(e0.mul(s)).plus(e1.mul(t))))).norm();
    }

    private NTxPoint3D centerPoint(NTxMesh3DTriangleIndices t) {
        NTxPoint3D p1 = vertices.get(t.p1);
        NTxPoint3D p2 = vertices.get(t.p2);
        NTxPoint3D p3 = vertices.get(t.p3);
        double x = (p1.x + p2.x + p3.x) / 3;
        double y = (p1.y + p2.y + p3.y) / 3;
        double z = (p1.z + p2.z + p3.z) / 3;
        return new NTxPoint3D(x, y, z);
    }

    public void sortByZ() {
        triangles.sort((a, b) -> {
            NTxPoint3D aa = centerPoint(a);
            NTxPoint3D bb = centerPoint(b);
            return Double.compare(aa.z, bb.z);
        });
    }

    public double distance(NTxPoint3D p) {
        double min = 10e10;
        for (int i = 0; i < triangles.size(); i += 3) {
            min = Math.min(min, distance(p, i));
        }
        return min;
    }
}
