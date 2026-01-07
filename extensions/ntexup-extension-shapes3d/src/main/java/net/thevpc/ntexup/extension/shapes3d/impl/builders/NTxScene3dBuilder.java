package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.extension.shapes3d.api.NTxMatrix3D;
import net.thevpc.ntexup.extension.shapes3d.api.NTxPoint3D;
import net.thevpc.ntexup.extension.shapes3d.api.NtxElement3D;
import net.thevpc.ntexup.extension.shapes3d.api.composite.NtxElement3DBox;
import net.thevpc.ntexup.extension.shapes3d.api.composite.NtxElement3DGroup;
import net.thevpc.ntexup.extension.shapes3d.api.composite.NtxElement3DSurface;
import net.thevpc.ntexup.extension.shapes3d.api.composite.NtxElement3DUVSphere;
import net.thevpc.ntexup.extension.shapes3d.api.primitives.*;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.eval.NTxValueByType;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.NTx3DUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.NTxCamera3DImpl;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxGraphics3DImpl;
import net.thevpc.nuts.elem.*;

import java.util.Arrays;


/**
 *
 */
public class NTxScene3dBuilder implements NTxNodeBuilder {
    public NTxScene3dBuilder() {
    }

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id(NTxNodeType.SCENE3D)
                .parseParam().matchesNamedPair(
                        "camera",
                        "light-orientation",
                        "transform",
                        "rotate",
                        "translate",
                        "scale",
                        "rotate-vector",
                        "rotate-line"
                ).end()
                .renderComponent(this::render)
        ;
    }


    public void render(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext buildContext) {
        NTxNode node = rendererContext.node();
        NElement c = NTxValue.ofProp(node, "camera").asElement().orNull();
        NTxCamera3DImpl camera = null;
        if (c != null) {
            if (c.isAnyString()) {
                switch (c.asStringValue().get()) {
                    case "isometric": {
                        camera = NTxCamera3DImpl.isometric();
                        break;
                    }
                }
            } else if (c.isAnyObject()) {
                NObjectElement o = c.asObject().get();
                Double azimuth = null;
                Double elevation = null;
                Double distance = null;
                NTxPoint3D position = null;
                NTxPoint3D target = null;
                for (NElement child : o.children()) {
                    if (child.isNamedPair()) {
                        NPairElement p = child.asPair().get();
                        switch (p.asStringValue().get()) {
                            case "azimuth": {
                                azimuth = p.value().asDoubleValue().get();
                                break;
                            }
                            case "elevation": {
                                elevation = p.value().asDoubleValue().get();
                                break;
                            }
                            case "distance": {
                                distance = p.value().asDoubleValue().get();
                                break;
                            }
                            case "position": {
                                position = NTx3DUtils.asPoint3D(NTxValue.of(p.value())).orNull();
                                break;
                            }
                            case "target": {
                                target = NTx3DUtils.asPoint3D(NTxValue.of(p.value())).orNull();
                                break;
                            }
                        }
                    }
                }
                if (azimuth != null || elevation != null || distance != null) {
                    camera = NTxCamera3DImpl.fromSpherical(azimuth == null ? -45 : azimuth, elevation == null ? 35.264 : elevation, distance == null ? 1000 : distance);
                } else if (position != null || target != null) {
                    camera = new NTxCamera3DImpl(position == null ? new NTxPoint3D(0, 0, 1000) : position, target == null ? new NTxPoint3D(0, 0, 0) : target);
                }
            }
        }
        if (camera == null) {
            camera = NTxCamera3DImpl.isometric();
        }
        NTxBounds2 b = rendererContext.selfBounds(node, null, null);
        NtxElement3DGroup g = new NtxElement3DGroup();
        for (NTxNode child : node.children()) {
            NtxElement3D cc = toNtxElement3D(child, rendererContext, buildContext, b);
            g.add(cc);
        }
        g.setTransform(resolveTransform(node, b));
        NtxGraphics3DImpl g3 = new NtxGraphics3DImpl(rendererContext.graphics(), rendererContext, buildContext);
        g3.setCamera(camera);
        g3.draw3D(g, new NTxPoint2D(b.getX(), b.getY()));
//        rendererContext.drawContour();
    }

    private NtxElement3D toNtxElement3D(NTxNode node, NTxNodeRendererContext rendererContext, NTxNodeBuilderContext buildContext, NTxBounds2 b) {
        switch (node.type()) {
            case NTxNodeType.LINE: {
                NTxPoint3D from = NTx3DUtils.convertPoint(NTx3DUtils.asPoint3D(node, NTxPropName.FROM).orElse(new NTxPoint3D(0, 0, 0)), b);
                NTxPoint3D to = NTx3DUtils.convertPoint(NTx3DUtils.asPoint3D(node, NTxPropName.TO).orElse(new NTxPoint3D(0, 0, 0)), b);
                NtxElement3DLine r = new NtxElement3DLine(from, to);
                apply3dProps(node, r, rendererContext, buildContext, b);
                return r;
            }
            case NTxNodeType.ARC: {
                NTxPoint3D from = NTx3DUtils.convertPoint(NTx3DUtils.asPoint3D(node, NTxPropName.FROM).orElse(new NTxPoint3D(0, 0, 0)), b);
                NTxPoint3D to = NTx3DUtils.convertPoint(NTx3DUtils.asPoint3D(node, NTxPropName.TO).orElse(new NTxPoint3D(0, 0, 0)), b);
                double startAngle = NTxValue.ofProp(node, NTxPropName.START_ANGLE).asDouble().orElse(0.0);
                double endAngle = NTxValue.ofProp(node, NTxPropName.END_ANGLE).asDouble().orElse(0.0);
                NtxElement3DArc r = new NtxElement3DArc(from, to, startAngle, endAngle);
                apply3dProps(node, r, rendererContext, buildContext, b);
                return r;
            }
            case NTxNodeType.POLYGON: {
                NTxPoint3D[] points = NTx3DUtils.asPoint3DArray(node, NTxPropName.POINTS).orElse(new NTxPoint3D[0]);
                boolean fill = NTxValue.ofProp(node, NTxPropName.FILL_BACKGROUND).asBoolean().orElse(true);
                boolean contour = NTxValue.ofProp(node, NTxPropName.DRAW_CONTOUR).asBoolean().orElse(true);
                NtxElement3DPolygon r = new NtxElement3DPolygon(Arrays.stream(points).map(x -> NTx3DUtils.convertPoint(x, b)).toArray(NTxPoint3D[]::new), fill, contour);
                apply3dProps(node, r, rendererContext, buildContext, b);
                return r;
            }
            case NTxNodeType.TRIANGLE: {
                NTxPoint3D[] points = NTx3DUtils.asPoint3DArray(node, NTxPropName.POINTS).orElse(new NTxPoint3D[0]);
                if (points.length < 3) {
                    points = Arrays.copyOf(points, 3);
                    for (int i = 0; i < 3; i++) {
                        if (points[i] == null) {
                            points[i] = new NTxPoint3D(0, 0, 0);
                        }
                    }
                }
                points = Arrays.stream(points).map(x -> NTx3DUtils.convertPoint(x, b)).toArray(NTxPoint3D[]::new);
                boolean fill = NTxValue.ofProp(node, NTxPropName.FILL_BACKGROUND).asBoolean().orElse(true);
                boolean contour = NTxValue.ofProp(node, NTxPropName.DRAW_CONTOUR).asBoolean().orElse(true);
                NtxElement3DTriangle r = new NtxElement3DTriangle(points[0], points[1], points[2], fill, contour);
                apply3dProps(node, r, rendererContext, buildContext, b);
                return r;
            }
            case NTxNodeType.POLYLINE: {
                NTxPoint3D[] points = NTx3DUtils.asPoint3DArray(node, NTxPropName.POINTS).orElse(new NTxPoint3D[0]);
                points = Arrays.stream(points).map(x -> NTx3DUtils.convertPoint(x, b)).toArray(NTxPoint3D[]::new);
                NtxElement3DPolyline r = new NtxElement3DPolyline(points);
                apply3dProps(node, r, rendererContext, buildContext, b);
                return r;
            }
            case NTxNodeType.GROUP: {
                NtxElement3DGroup r = new NtxElement3DGroup();
                for (NTxNode child : node.children()) {
                    NtxElement3D cc = toNtxElement3D(child, rendererContext, buildContext, b);
                    r.add(cc);
                }
                apply3dProps(node, r, rendererContext, buildContext, b);
                return r;
            }
            case "surface": {
                NTxPoint3D[] points = NTx3DUtils.asPoint3DArray(node, NTxPropName.POINTS).orElse(new NTxPoint3D[0]);
                points = Arrays.stream(points).map(x -> NTx3DUtils.convertPoint(x, b)).toArray(NTxPoint3D[]::new);
                boolean fill = NTxValue.ofProp(node, NTxPropName.FILL_BACKGROUND).asBoolean().orElse(true);
                boolean contour = NTxValue.ofProp(node, NTxPropName.DRAW_CONTOUR).asBoolean().orElse(true);
                NtxElement3DSurface r = new NtxElement3DSurface(points);
                apply3dProps(node, r, rendererContext, buildContext, b);
                return r;
            }
            case "uvsphere":
            case "uv-sphere":
            case "sphere": {
                NTxPoint3D origin = NTx3DUtils.convertPoint(NTx3DUtils.asPoint3D(node, NTxPropName.ORIGIN).orElse(new NTxPoint3D(0, 0, 0)), b);
                NTxPoint3D radius = NTx3DUtils.convertPoint(NTx3DUtils.asPoint3D(node, "radius").orElse(new NTxPoint3D(1, 1, 1)), b);
                int meridians = NTxValue.ofProp(node, "meridians").asInt().orElse(60);
                int parallels = NTxValue.ofProp(node, "parallels").asInt().orElse(60);
                NtxElement3DUVSphere r = new NtxElement3DUVSphere(origin, radius.x, radius.y, radius.y, meridians, parallels);
                apply3dProps(node, r, rendererContext, buildContext, b);
                return r;
            }
            case "box": {
                NTxPoint3D origin = NTx3DUtils.convertPoint(NTx3DUtils.asPoint3D(node, NTxPropName.ORIGIN).orElse(new NTxPoint3D(0, 0, 0)), b);
                NTxPoint3D size = NTx3DUtils.convertPoint(NTx3DUtils.asPoint3D(node, NTxPropName.SIZE).orElse(new NTxPoint3D(0, 0, 0)), b);
                NtxElement3DBox r = new NtxElement3DBox(origin, size.x, size.y, size.z);
                apply3dProps(node, r, rendererContext, buildContext, b);
                return r;
            }
        }
        return null;
    }

    private void apply3dProps(NTxNode node, NtxElement3D g, NTxNodeRendererContext rendererContext, NTxNodeBuilderContext buildContext, NTxBounds2 b) {
        {
            NElement s = NTxValueByType.getElement(node, rendererContext, "line-stroke").orNull();
            if (s != null) {
                g.setLineStroke(rendererContext.graphics().createStroke(s));
            }
        }
        {
            NElement s = NTxValueByType.getElement(node, rendererContext, "contour-stroke").orNull();
            if (s != null) {
                g.setContourStroke(rendererContext.graphics().createStroke(s));
            }
        }
        g.setForegroundPaint(NTxValueByType.getPaint(node, rendererContext, NTxPropName.FOREGROUND_COLOR, "foreground", "color", "fg").orElse(null));
        g.setBackgroundPaint(NTxValueByType.getPaint(node, rendererContext, NTxPropName.BACKGROUND_COLOR, "background", "bg").orNull());
        g.setContourPaint(NTxValueByType.getPaint(node, rendererContext, "contour-color").orElse(null));
        g.setLinePaint(NTxValueByType.getPaint(node, rendererContext, "line-color").orElse(null));
//        g.setComposite(NTxValueByType.getComposite(node, rendererContext, "composite").orElse(null));
        g.setTransform(resolveTransform(node, b));
    }

    private NTxMatrix3D resolveTransform(NTxNode node, NTxBounds2 bounds) {
        NTxPoint3D rotate = NTx3DUtils.asPoint3D(node, "rotate").orElse(new NTxPoint3D(0, 0, 0));
        NObjectElement rotateVector = NTxValue.ofProp(node, "rotate-vector").asElement().flatMap(x -> x.asObject()).orElse(null);
        NObjectElement rotateLine = NTxValue.ofProp(node, "rotate-line").asElement().flatMap(x -> x.asObject()).orElse(null);
        NTxMatrix3D m = NTxMatrix3D.identity();
        m = m.rotate(Math.toRadians(rotate.getX()), Math.toRadians(rotate.getY()), Math.toRadians(rotate.getZ()));
        if (rotateVector != null) {
            NTxPoint3D vector = NTx3DUtils.asPoint3D(NTxValue.of(rotateVector.get("vector"))).orElse(null);
            double angle = NTxValue.of(rotateVector.get("angle")).asDouble().orElse(0.0);
            if (vector != null && angle != 0) {
                m = m.rotateVector(
                        vector,
                        Math.toRadians(angle)

                );
            }
        }
        if (rotateLine != null) {
            NTxPoint3D a = NTx3DUtils.asPoint3D(NTxValue.of(rotateLine.get("from"))).orElse(null);
            NTxPoint3D b = NTx3DUtils.asPoint3D(NTxValue.of(rotateLine.get("to"))).orElse(null);
            double angle = NTxValue.of(rotateLine.get("angle")).asDouble().orElse(0.0);
            if (a != null && b != null && angle != 0) {
                m = m.rotateLine(
                        a,
                        b,
                        Math.toRadians(angle)
                );
            }
        }
        //TODO
        NObjectElement transform = NTxValue.ofProp(node, "transform").asElement().flatMap(x -> x.toObject()).orElse(null);
        if (transform != null) {
            m = applyTransform(m, transform, bounds);
        }

        NTxPoint3D scale = NTx3DUtils.asPoint3D(NTxValue.ofProp(node, "scale")).orElse(new NTxPoint3D(1, 1, 1));
        NTxPoint3D translate = NTx3DUtils.asPoint3D(NTxValue.ofProp(node, "translate")).orElse(new NTxPoint3D(0, 0, 0));

        m = m.translate(translate.getX(), translate.getY(), translate.getZ());
        m = m.scale(scale.getX(), scale.getY(), scale.getZ());
        return m;
    }

    private NTxMatrix3D applyTransform(NTxMatrix3D m, NElement e, NTxBounds2 bounds) {
        if (e.isNamedPair()) {
            NPairElement np = e.asNamedPair().get();
            NElement value = np.value();
            return applyTransform(m, np.key().asStringValue().get(), value.toObject().get().children().toArray(new NElement[0]), bounds);
        }
        if (e.isAnyObject()) {
            NObjectElement np = e.asObject().get();
            if (e.isNamed()) {
                String name = np.name().orElse(null);
                if (name != null) {
                    return applyTransform(m, name, np.children().toArray(new NElement[0]), bounds);
                }
            } else {
                for (NElement o : np.children()) {
                    m = applyTransform(m, o, bounds);
                }
                return m;
            }
        }
        if (e.isAnyArray()) {
            NArrayElement np = e.asArray().get();
            if (e.isNamed()) {
                String name = np.name().orElse(null);
                if (name != null) {
                    return applyTransform(m, name, np.children().toArray(new NElement[0]), bounds);
                }
            } else {
                for (NElement o : np.children()) {
                    m = applyTransform(m, o, bounds);
                }
                return m;
            }
        }
        if (e.isAnyUplet()) {
            NUpletElement np = e.asUplet().get();
            if (e.isNamed()) {
                String name = np.name().orElse(null);
                if (name != null) {
                    return applyTransform(m, name, np.children().toArray(new NElement[0]), bounds);
                }
            } else {
                for (NElement o : np.children()) {
                    m = applyTransform(m, o, bounds);
                }
                return m;
            }
        }
        return m;
    }

    private NTxMatrix3D applyTransform(NTxMatrix3D m, String name, NElement[] args, NTxBounds2 b) {
        switch (NTxUtils.uid(name)) {
            case "translate": {
                NTxPoint3D _dist = null;
                if (args.length == 3
                        && NTxValue.of(args[0]).asDouble().isPresent()
                        && NTxValue.of(args[1]).asDouble().isPresent()
                        && NTxValue.of(args[2]).asDouble().isPresent()
                ) {
                    _dist = new NTxPoint3D(NTxValue.of(args[0]).asDouble().get(),
                            NTxValue.of(args[1]).asDouble().get(),
                            NTxValue.of(args[2]).asDouble().get());
                } else if (args.length == 1
                        && NTxValue.of(args[0]).asDouble().isPresent()
                ) {
                    _dist = new NTxPoint3D(NTxValue.of(args[0]).asDouble().get(),
                            NTxValue.of(args[0]).asDouble().get(),
                            NTxValue.of(args[0]).asDouble().get());
                } else {
                    for (NElement arg : args) {
                        NTxValue u = NTxValue.of(arg);
                        NTxPoint3D pn = NTx3DUtils.asPoint3D(u).orNull();
                        if (pn != null) {
                            _dist = pn;
                            break;
                        }
                    }
                }
                if (_dist != null) {
                    _dist = NTx3DUtils.convertPoint(_dist, b);
                    return m.translate(_dist.getX(), _dist.getY(), _dist.getZ());
                }
                break;
            }
            case "rotate": {
                NTxPoint3D _angle = null;
                NTxPoint3D _lineFrom = null;
                NTxPoint3D _lineTo = null;
                NTxPoint3D _vector = null;
                for (NElement arg : args) {
                    if (arg.isNamedPair()) {
                        NPairElement np = arg.asNamedPair().get();
                        switch (NTxUtils.uid(np.key().asStringValue().get())) {
                            case "from": {
                                NTxPoint3D pn = NTx3DUtils.asPoint3D(NTxValue.of(np.value())).orNull();
                                if (pn != null) {
                                    _lineFrom = pn;
                                }
                                break;
                            }
                            case "to": {
                                NTxPoint3D pn = NTx3DUtils.asPoint3D(NTxValue.of(np.value())).orNull();
                                if (pn != null) {
                                    _lineTo = pn;
                                }
                                break;
                            }
                            case "vector": {
                                NTxPoint3D pn = NTx3DUtils.asPoint3D(NTxValue.of(np.value())).orNull();
                                if (pn != null) {
                                    _vector = pn;
                                }
                                break;
                            }
                            case "angle": {
                                NTxPoint3D pn = NTx3DUtils.asPoint3D(NTxValue.of(np.value())).orNull();
                                if (pn != null) {
                                    _angle = pn;
                                } else {
                                    _angle = new NTxPoint3D(NTxValue.of(np.value()).asDouble().orElse(0.0), 0.0, 0.0);
                                }
                                break;
                            }
                        }
                    } else {
                        NTxValue u = NTxValue.of(arg);
                        NTxPoint3D pn = NTx3DUtils.asPoint3D(u).orNull();
                        if (pn != null) {
                            _angle = pn;
                        } else {
                            _angle = new NTxPoint3D(NTxValue.of(u).asDouble().orElse(0.0), 0.0, 0.0);
                        }
                        break;
                    }
                }
                if (_angle != null) {
                    if ((_lineFrom != null || _lineTo != null) && _vector == null) {
                        return m.rotateLine(
                                NTxPoint3D.denull(_lineFrom),
                                NTxPoint3D.denull(_lineTo),
                                _angle == null ? 0 : Math.toRadians(_angle.x)
                        );
                    }
                    if ((_lineFrom == null && _lineTo == null) && _vector != null) {
                        return m.rotateVector(
                                _vector,
                                _angle == null ? 0 : Math.toRadians(_angle.x)
                        );
                    }
                    if (_lineFrom != null || _lineTo != null || _vector != null) {
                        return m;
                    }
                    return m.rotate(
                            Math.toRadians(_angle.getX()),
                            Math.toRadians(_angle.getY()),
                            Math.toRadians(_angle.getZ())
                    );
                }
                break;
            }
            case "scale": {
                NTxPoint3D _dist = null;
                if (args.length == 3
                        && NTxValue.of(args[0]).asDouble().isPresent()
                        && NTxValue.of(args[1]).asDouble().isPresent()
                        && NTxValue.of(args[2]).asDouble().isPresent()
                ) {
                    return m.scale(
                            NTxValue.of(args[0]).asDouble().get(),
                            NTxValue.of(args[1]).asDouble().get(),
                            NTxValue.of(args[2]).asDouble().get()
                    );
                }
                if (args.length == 1
                        && NTxValue.of(args[0]).asDouble().isPresent()
                ) {
                    return m.scale(
                            NTxValue.of(args[0]).asDouble().get(),
                            NTxValue.of(args[0]).asDouble().get(),
                            NTxValue.of(args[0]).asDouble().get()
                    );
                }
                for (NElement arg : args) {
                    NTxValue u = NTxValue.of(arg);
                    NTxPoint3D pn = NTx3DUtils.asPoint3D(u).orNull();
                    if (pn != null) {
                        _dist = pn;
                        break;
                    }
                }
                if (_dist != null) {
                    return m.scale(_dist.getX(), _dist.getY(), _dist.getZ());
                }
                break;
            }
        }
        return m;
    }

}
