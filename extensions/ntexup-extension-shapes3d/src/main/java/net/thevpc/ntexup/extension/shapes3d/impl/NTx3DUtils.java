package net.thevpc.ntexup.extension.shapes3d.impl;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble3;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.util.NTxElementUtils;
import net.thevpc.ntexup.api.util.NTxMinMax;
import net.thevpc.ntexup.extension.shapes3d.api.NTxPoint3D;
import net.thevpc.ntexup.extension.shapes3d.api.NTxVector3D;
import net.thevpc.ntexup.extension.shapes3d.api.NtxElement3D;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NTx3DUtils {

    public static NTxVector3D surfaceNormal(NTxPoint3D[] all) {
        return surfaceNormal(all[0], all[1], all[2]);
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
            x += p.x;
            y += p.y;
            z += p.z;
        }
        int n = points.length;
        return new NTxPoint3D(x / n, y / n, z / n);
    }

    public static void copyProps(NtxElement3D from, NtxElement3D to, String name) {
        to.setTransform(from.getTransform());

        to.setLineStroke(from.getLineStroke());
        to.setLinePaint(from.getLinePaint());

        to.setBackgroundPaint(from.getBackgroundPaint());
        to.setForegroundPaint(from.getForegroundPaint());

        to.setContourStroke(from.getContourStroke());
        to.setContourPaint(from.getContourPaint());

        to.setComposite(from.getComposite());
    }

    public static NTxPoint3D convertPoint(NTxPoint3D p, NTxBounds2 b) {
        double h = Math.max(b.getWidth(), b.getHeight());
        return new NTxPoint3D(
                p.x / 100 * b.getWidth()/* + b.getMinX()*/,
                p.y / 100 * b.getHeight()/* + b.getMinY()*/,
                p.z / 100 * h
        );
    }

    public static void copyPropsIfEmptyTarget(NtxElement3D from, NtxElement3D to, String name) {
        if (to.getTransform() == null) {
            to.setTransform(from.getTransform());
        }
        if (to.getLineStroke() == null) {
            to.setLineStroke(from.getLineStroke());
        }
        if (to.getLinePaint() == null) {
            to.setLinePaint(from.getLinePaint());
        }
        if (to.getBackgroundPaint() == null) {
            to.setBackgroundPaint(from.getBackgroundPaint());
        }
        if (to.getForegroundPaint() == null) {
            to.setForegroundPaint(from.getForegroundPaint());
        }

        if (to.getContourStroke() == null) {
            to.setContourStroke(from.getContourStroke());
        }
        if (to.getContourPaint() == null) {
            to.setContourPaint(from.getContourPaint());
        }
        if (to.getComposite() == null) {
            to.setComposite(from.getComposite());
        }
    }

    public static NTxProp ofHPoint3DArray(String name, NTxPoint3D... d) {
        return NTxProp.of(name, NElement.ofArray(Arrays.stream(d).map(NTxPoint3D::toElement).toArray(NElement[]::new)));
    }

    public static NTxProp ofHPoint3D(String name, NTxPoint3D d) {
        return NTxProp.of(name, d == null ? null : d.toElement());
    }

    static public boolean addPoints(NTxNode line, NTxPoint3D[] points) {
        boolean result = false;
        for (NTxPoint3D point : points) {
            result |= addPoint(line, point);
        }
        return result;
    }

    static
    public boolean addPoint(NTxNode line, NTxPoint3D point) {
        if (point != null) {
            NTxValue o = NTxValue.of(line.getPropertyValue(NTxPropName.POINTS).orNull());
            NOptional<NTxPoint3D[]> hPoint2DArray = asPoint3DArray(o);
            List<NTxPoint3D> v = new ArrayList<>();
            if (hPoint2DArray.isPresent()) {
                v.addAll(Arrays.asList(hPoint2DArray.get()));
            }
            v.add(point);
            line.setProperty(NTxPropName.POINTS, NTxElementUtils.toElement(v.toArray(new NTxPoint3D[0])));
            return true;
        }
        return false;
    }


    public static NOptional<NTxPoint3D[]> asPoint3DArray(NTxValue value) {
        NOptional<NTxDouble3[]> u = value.asDouble3Array();
        if (u.isPresent()) {
            return NOptional.of(
                    Arrays.stream(u.get()).map(x -> new NTxPoint3D(x.getX(), x.getY(), x.getZ())).toArray(NTxPoint3D[]::new)
            );
        } else {
            return (NOptional) u;
        }
    }

    public static final NTxMinMax minMaxZ(NTxPoint3D[] points) {
        NTxMinMax m = new NTxMinMax();
        for (NTxPoint3D point : points) {
            m.registerValue(point.z);
        }
        return m;
    }

    public static NOptional<NTxPoint3D> asPoint3D(NTxNode node, String name) {
        return asPoint3D(NTxValue.ofProp(node, name));
    }

    public NOptional<NTxPoint3D> asHPoint3D(NTxNode node, String name) {
        return asHPoint3D(NTxValue.ofProp(node, name));
    }

    public static NOptional<NTxPoint3D[]> asPoint3DArray(NTxNode node, String name) {
        return asPoint3DArray(NTxValue.ofProp(node, name));
    }

    public static NOptional<NTxPoint3D> asPoint3D(NTxValue value) {
        Object element = value.raw();
        if (element instanceof NTxPoint3D) {
            return NOptional.of((NTxPoint3D) element);
        }
        NOptional<double[]> d = value.asDoubleArray();
        if (d.isPresent()) {
            double[] dd = d.get();
            if (dd.length == 3) {
                return NOptional.of(new NTxPoint3D(dd[0], dd[1], dd[2]));
            }
        }
        return NOptional.ofNamedEmpty("Point3D from " + element);
    }

    public NOptional<NTxPoint3D> asHPoint3D(NTxValue value) {
        Object element = value.raw();
        if (element instanceof NTxPoint3D) {
            return NOptional.of((NTxPoint3D) element);
        }
        NOptional<double[]> d = value.asDoubleArray();
        if (d.isPresent()) {
            double[] dd = d.get();
            if (dd.length == 2) {
                return NOptional.of(new NTxPoint3D(dd[0], dd[1], dd[2]));
            }
        }
        return NOptional.ofNamedEmpty("HPoint3D from " + element);
    }


//    public static boolean addPoints(NTxNode line, NTxPoint3D[] points) {
//        return NTxUtilsPoints.addPoints(line, points);
//    }
//    public static boolean addPoint(NTxNode line, NTxPoint3D point) {
//        return NTxUtilsPoints.addPoint(line, point);
//    }


}
