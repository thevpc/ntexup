package net.thevpc.ntexup.lib.geometry3d.impl;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble3;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxElementUtils;
import net.thevpc.ntexup.api.util.NTxNumberUtils;
import net.thevpc.ntexup.lib.geometry3d.*;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.math.NDoubleRange;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NLiteral;
import net.thevpc.nuts.util.NOptional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NTx3DUtils {

    public static NTxNumberElement3 resolveSize3DSI(NElement e, NTxResolutionContext context) {
        NTxNumberElement3 p = resolveSize3D(e, context);
        if(p!=null){
            return toSIUnit(p).orNull();
        }
        return null;
    }

    public static NTxNumberElement3 resolveSize3D(NElement e, NTxResolutionContext context) {
        if (e.isUplet()) {
            NUpletElement u = e.asUplet().get();
            if (u.size() == 3) {
                NNumberElement p1 = NTxNumberUtils.asNumberElement(u.get(0).get(), context);
                if (p1 == null) {
                    context.log().log(NMsg.ofC("invalid point %s", e).asError());
                }
                NNumberElement p2 = NTxNumberUtils.asNumberElement(u.get(1).get(), context);
                if (p2 == null) {
                    context.log().log(NMsg.ofC("invalid point %s", e).asError());
                }
                NNumberElement p3 = NTxNumberUtils.asNumberElement(u.get(2).get(), context);
                if (p3 == null) {
                    context.log().log(NMsg.ofC("invalid point %s", e).asError());
                }
                return new NTxNumberElement3(p1, p2, p3);
            }
        }
        context.log().log(NMsg.ofC("invalid point %s", e).asError());
        return null;
    }


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

    public static void copyNonNullProps(NtxFace face, NtxElement3D to) {
        if (face != null) {
            if (face.getBackground() != null) {
                to.setBackgroundPaint(face.getBackground());
            }
            if (face.getLineColor() != null) {
                to.setLinePaint(face.getLineColor());
                to.setContourPaint(face.getLineColor());
            }
            if (face.getStroke() != null) {
                to.setContourStroke(face.getStroke());
            }
//            if(face.isDrawContour()){
//                to.setContourPaint(face.getContourPaint());
//            }
//            to.setComposite(face.getComposite());
        }
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

    public static NTxPoint3D convertPoint(NTxPoint3D p, NTxBounds2D b) {
        double h = Math.max(b.widthX(), b.widthY());
        return new NTxPoint3D(
                p.x / 100 * b.widthX()/* + b.getMinX()*/,
                p.y / 100 * b.widthY()/* + b.getMinY()*/,
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
        if (to.getMeshPrecision() == null) {
            to.setMeshPrecision(from.getMeshPrecision());
        }
        if (to.getMeshVisible() == null) {
            to.setMeshVisible(from.getMeshVisible());
        }
        if (to.getMeshPaint() == null) {
            to.setMeshPaint(from.getMeshPaint());
        }
        if (to.getMeshStroke() == null) {
            to.setMeshStroke(from.getMeshStroke());
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

    public static final NDoubleRange minMaxZ(NTxPoint3D[] points) {
        NDoubleRange m = NDoubleRange.of();
        for (NTxPoint3D point : points) {
            m.add(point.z);
        }
        return m;
    }

    public static NOptional<NTxPoint3D> asPoint3D(NTxNode node, String name) {
        return asPoint3D(NTxValue.ofProp(node, name));
    }

//    public NOptional<NTxPoint3D> asHPoint3D(NTxNode node, String name) {
//        return asHPoint3D(NTxValue.ofProp(node, name));
//    }

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

    public static NOptional<NTxNumberElement3[]> asElementNumber3Array(NTxValue value,NTxRendererContext rendererContext) {
        Object element = value.raw();
        if (element instanceof NTxNumberElement3[]) {
            return NOptional.of((NTxNumberElement3[]) element);
        }
        List<NTxNumberElement3> all=new ArrayList<>();
        if(element instanceof NElement){
            NOptional<NElement> t = rendererContext.evalExpression((NElement) element);
            if(!t.isPresent()) {
                return NOptional.ofNamedEmpty("NTxElementNumber3[] from " + element);
            }
            element= t.get();
        }
        if(element instanceof NListContainerElement){
            for (NElement child : ((NListContainerElement) element).children()) {
                NOptional<NTxNumberElement3> r = asNumberElement3(NTxValue.of(child),rendererContext);
                if(!r.isPresent()) {
                    return NOptional.ofNamedEmpty("NTxElementNumber3[] from " + element);
                }
                all.add(r.get());
            }
            return NOptional.of(all.toArray(new NTxNumberElement3[0]));
        }
        return NOptional.ofNamedEmpty("NTxElementNumber3[] from " + element);
    }

    public static NOptional<NTxNumberElement2[]> asElementNumber2Array(NTxValue value,NTxRendererContext rendererContext) {
        Object element = value.raw();
        if (element instanceof NTxNumberElement2[]) {
            return NOptional.of((NTxNumberElement2[]) element);
        }
        List<NTxNumberElement2> all=new ArrayList<>();
        if(element instanceof NListContainerElement){
            for (NElement child : ((NListContainerElement) element).children()) {
                NOptional<NTxNumberElement2> r = asNumberElement2(NTxValue.of(child));
                if(!r.isPresent()) {
                    return NOptional.ofNamedEmpty("NTxNumberElement2[] from " + element);
                }
                all.add(r.get());
            }
            return NOptional.of(all.toArray(new NTxNumberElement2[0]));
        }
        return NOptional.ofNamedEmpty("NTxNumberElement2[] from " + element);
    }

    public static NOptional<NTxNumberElement2[][]> asElementNumber2Array2(NTxValue value) {
        Object element = value.raw();
        if (element instanceof NTxNumberElement2[][]) {
            return NOptional.of((NTxNumberElement2[][]) element);
        }
        List<NTxNumberElement2[]> all0=new ArrayList<>();
        if(element instanceof NListContainerElement){
            for (NElement child0 : ((NListContainerElement) element).children()) {
                if(child0 instanceof NListContainerElement) {
                    List<NTxNumberElement2> all1=new ArrayList<>();
                    for (NElement child : ((NListContainerElement) child0).children()) {
                        NOptional<NTxNumberElement2> r = asNumberElement2(NTxValue.of(child));
                        if (r.isPresent()) {
                            all1.add(r.get());
                        }else {
                            return NOptional.ofNamedEmpty("NTxNumberElement2[][] from " + element);
                        }
                    }
                    all0.add(all1.toArray(new NTxNumberElement2[0]));
                }else{
                    return NOptional.ofNamedEmpty("NTxNumberElement2[][] from " + element);
                }
            }
            return NOptional.of(all0.toArray(new NTxNumberElement2[0][]));
        }else {
            return NOptional.ofNamedEmpty("NTxNumberElement2[][] from " + element);
        }
    }

    public static NOptional<NNumberElement> asNumberElement(NTxValue value, NTxRendererContext rendererContext) {
        Object element = value.raw();
        if (element instanceof NElement) {
            NOptional<NElement> t = rendererContext.evalExpression((NElement) element);
            if(!t.isPresent()) {
                return NOptional.ofNamedEmpty("NTxNumberElement3 from " + element);
            }
            element= t.get();
        }
        if (element instanceof NNumberElement) {
            return NOptional.of((NNumberElement) element);
        }
        if (element instanceof Number) {
            return NOptional.of((NNumberElement) NElement.ofNumber((Number) element));
        }
        if (element instanceof String) {
            NOptional<Number> n = NLiteral.of(element).asNumber();
            if(n.isPresent()) {
                return NOptional.of((NNumberElement) NElement.ofNumber(n.get()));
            }
        }
        return NOptional.ofNamedEmpty("NNumberElement from " + element);
    }

    public static NOptional<NTxNumberElement3> asNumberElement3(NTxValue value, NTxRendererContext rendererContext) {
        Object element = value.raw();
        if (element instanceof NTxNumberElement3) {
            return NOptional.of((NTxNumberElement3) element);
        }
        if(element instanceof NElement) {
            NOptional<NElement> t = rendererContext.evalExpression((NElement) element);
            if(!t.isPresent()) {
                return NOptional.ofNamedEmpty("NTxNumberElement3 from " + element);
            }
            element= t.get();
        }
        if (element instanceof NUpletElement) {
            NUpletElement u = (NUpletElement) element;
            if (u.children().size() == 3) {
                List<NNumberElement> nn = new ArrayList<>();
                for (int i = 0; i < 3; i++) {
                    NElement t = u.children().get(i);
                    NOptional<NElement> e=rendererContext.evalExpression((NElement) t);
                    if(e.isPresent()) {
                        if (t.isNull()) {
                            nn.add(null);
                        } else if (t.isNumber()) {
                            nn.add(t.asNumber().get());
                        } else {
                            return NOptional.ofNamedEmpty("NTxNumberElement3 from " + element);
                        }
                    }else{
                        return NOptional.ofNamedEmpty("NTxNumberElement3 from " + element);
                    }
                }
                return NOptional.of(
                        new NTxNumberElement3(
                                nn.get(0),
                                nn.get(1),
                                nn.get(2)
                        )
                );
            }
        }
        return NOptional.ofNamedEmpty("NTxNumberElement3 from " + element);
    }
    public static NOptional<NTxNumberElement2> asNumberElement2(NTxValue value) {
        Object element = value.raw();
        if (element instanceof NTxNumberElement2) {
            return NOptional.of((NTxNumberElement2) element);
        }
        if (element instanceof NUpletElement) {
            NUpletElement u = (NUpletElement) element;
            if (u.children().size() == 2) {
                List<NNumberElement> nn = new ArrayList<>();
                for (int i = 0; i < 2; i++) {
                    NElement t = u.children().get(i);
                    if (t.isNull()) {
                        nn.add(null);
                    } else if (t.isNumber()) {
                        nn.add(t.asNumber().get());
                    } else {
                        return NOptional.ofNamedEmpty("NTxNumberElement2 from " + element);
                    }
                }
                return NOptional.of(
                        new NTxNumberElement2(
                                nn.get(0),
                                nn.get(1)
                        )
                );
            }
        }
        return NOptional.ofNamedEmpty("NTxNumberElement2 from " + element);
    }

    public static NOptional<NTxNumberElement3> toSIUnit(NTxNumberElement3 ss) {
        NOptional<NNumberElement> x = NTxNumberUtils.toSIUnit(ss.x);
        NOptional<NNumberElement> y = NTxNumberUtils.toSIUnit(ss.y);
        NOptional<NNumberElement> z = NTxNumberUtils.toSIUnit(ss.z);
        if(x.isPresent() && y.isPresent() && z.isPresent()) {
            return  NOptional.of(new NTxNumberElement3(
                    x.get(),y.get(),z.get()
            ));
        }
        return  NOptional.ofNamedEmpty("NTxNumberElement3 from " + ss);
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
