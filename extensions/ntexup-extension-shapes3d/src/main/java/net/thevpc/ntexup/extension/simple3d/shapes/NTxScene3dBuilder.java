package net.thevpc.ntexup.extension.simple3d.shapes;

import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.document.elem3d.NTxMatrix3D;
import net.thevpc.ntexup.api.document.elem3d.NTxPoint3D;
import net.thevpc.ntexup.api.document.elem3d.NtxElement3D;
import net.thevpc.ntexup.api.document.elem3d.composite.NtxElement3DBox;
import net.thevpc.ntexup.api.document.elem3d.composite.NtxElement3DGroup;
import net.thevpc.ntexup.api.document.elem3d.composite.NtxElement3DSurface;
import net.thevpc.ntexup.api.document.elem3d.composite.NtxElement3DUVSphere;
import net.thevpc.ntexup.api.document.elem3d.primitives.*;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.eval.NTxValueByType;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.extension.simple3d.NtxGraphics3DImpl;
import net.thevpc.ntexup.extension.simple3d.Shape3DHelper;
import net.thevpc.nuts.elem.*;

import java.awt.*;
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
                .parseParam().matchesNamedPair("camera-orientation", "light-orientation", "transform", "rotate", "translate", "scale", "rotate-vector", "rotate-line").end()
                .renderComponent(this::render)
        ;
    }


    public void render(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext buildContext) {
        NTxNode node = rendererContext.node();
        NTxBounds2 b = rendererContext.selfBounds(node, null, null);
        NtxElement3DGroup g = new NtxElement3DGroup();
        for (NTxNode child : node.children()) {
            NtxElement3D cc = toNtxElement3D(child, rendererContext, buildContext, b);
            g.add(cc);
        }
        g.setTransform(resolveTransform(node,b));
        NtxGraphics3DImpl g3 = new NtxGraphics3DImpl(rendererContext.graphics(), rendererContext, buildContext);
        g3.draw3D(g, new NTxPoint2D(b.getX(), b.getY()));
//        rendererContext.drawContour();
    }

    private NtxElement3D toNtxElement3D(NTxNode node, NTxNodeRendererContext rendererContext, NTxNodeBuilderContext buildContext, NTxBounds2 b) {
        switch (node.type()) {
            case NTxNodeType.LINE: {
                NTxPoint3D from = Shape3DHelper.convertPoint(NTxValue.ofProp(node, NTxPropName.FROM).asPoint3D().orElse(new NTxPoint3D(0, 0, 0)), b);
                NTxPoint3D to = Shape3DHelper.convertPoint(NTxValue.ofProp(node, NTxPropName.TO).asPoint3D().orElse(new NTxPoint3D(0, 0, 0)), b);
                NtxElement3DLine r = new NtxElement3DLine(from, to);
                apply3dProps(node, r, rendererContext, buildContext, b);
                return r;
            }
            case NTxNodeType.ARC: {
                NTxPoint3D from = Shape3DHelper.convertPoint(NTxValue.ofProp(node, NTxPropName.FROM).asPoint3D().orElse(new NTxPoint3D(0, 0, 0)), b);
                NTxPoint3D to = Shape3DHelper.convertPoint(NTxValue.ofProp(node, NTxPropName.TO).asPoint3D().orElse(new NTxPoint3D(0, 0, 0)), b);
                double startAngle = NTxValue.ofProp(node, NTxPropName.START_ANGLE).asDouble().orElse(0.0);
                double endAngle = NTxValue.ofProp(node, NTxPropName.END_ANGLE).asDouble().orElse(0.0);
                NtxElement3DArc r = new NtxElement3DArc(from, to, startAngle, endAngle);
                apply3dProps(node, r, rendererContext, buildContext, b);
                return r;
            }
            case NTxNodeType.POLYGON: {
                NTxPoint3D[] points = NTxValue.ofProp(node, NTxPropName.POINTS).asPoint3DArray().orElse(new NTxPoint3D[0]);
                boolean fill = NTxValue.ofProp(node, NTxPropName.FILL_BACKGROUND).asBoolean().orElse(true);
                boolean contour = NTxValue.ofProp(node, NTxPropName.DRAW_CONTOUR).asBoolean().orElse(true);
                NtxElement3DPolygon r = new NtxElement3DPolygon(Arrays.stream(points).map(x -> Shape3DHelper.convertPoint(x, b)).toArray(NTxPoint3D[]::new), fill, contour);
                apply3dProps(node, r, rendererContext, buildContext, b);
                return r;
            }
            case NTxNodeType.TRIANGLE: {
                NTxPoint3D[] points = NTxValue.ofProp(node, NTxPropName.POINTS).asPoint3DArray().orElse(new NTxPoint3D[0]);
                if (points.length < 3) {
                    points = Arrays.copyOf(points, 3);
                    for (int i = 0; i < 3; i++) {
                        if (points[i] == null) {
                            points[i] = new NTxPoint3D(0, 0, 0);
                        }
                    }
                }
                points = Arrays.stream(points).map(x -> Shape3DHelper.convertPoint(x, b)).toArray(NTxPoint3D[]::new);
                boolean fill = NTxValue.ofProp(node, NTxPropName.FILL_BACKGROUND).asBoolean().orElse(true);
                boolean contour = NTxValue.ofProp(node, NTxPropName.DRAW_CONTOUR).asBoolean().orElse(true);
                NtxElement3DTriangle r = new NtxElement3DTriangle(points[0], points[1], points[2], fill, contour);
                apply3dProps(node, r, rendererContext, buildContext, b);
                return r;
            }
            case NTxNodeType.POLYLINE: {
                NTxPoint3D[] points = NTxValue.ofProp(node, NTxPropName.POINTS).asPoint3DArray().orElse(new NTxPoint3D[0]);
                points = Arrays.stream(points).map(x -> Shape3DHelper.convertPoint(x, b)).toArray(NTxPoint3D[]::new);
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
                NTxPoint3D[] points = NTxValue.ofProp(node, NTxPropName.POINTS).asPoint3DArray().orElse(new NTxPoint3D[0]);
                points = Arrays.stream(points).map(x -> Shape3DHelper.convertPoint(x, b)).toArray(NTxPoint3D[]::new);
                boolean fill = NTxValue.ofProp(node, NTxPropName.FILL_BACKGROUND).asBoolean().orElse(true);
                boolean contour = NTxValue.ofProp(node, NTxPropName.DRAW_CONTOUR).asBoolean().orElse(true);
                NtxElement3DSurface r = new NtxElement3DSurface(points);
                apply3dProps(node, r, rendererContext, buildContext, b);
                return r;
            }
            case "uvsphere":
            case "uv-sphere":
            case "sphere": {
                NTxPoint3D origin = Shape3DHelper.convertPoint(NTxValue.ofProp(node, NTxPropName.ORIGIN).asPoint3D().orElse(new NTxPoint3D(0, 0, 0)), b);
                NTxPoint3D radius = Shape3DHelper.convertPoint(NTxValue.ofProp(node, "radius").asPoint3D().orElse(new NTxPoint3D(1, 1, 1)), b);
                int meridians = NTxValue.ofProp(node, "meridians").asInt().orElse(60);
                int parallels = NTxValue.ofProp(node, "parallels").asInt().orElse(60);
                NtxElement3DUVSphere r = new NtxElement3DUVSphere(origin, radius.x, radius.y, radius.y, meridians, parallels);
                apply3dProps(node, r, rendererContext, buildContext, b);
                return r;
            }
            case "box": {
                NTxPoint3D origin = Shape3DHelper.convertPoint(NTxValue.ofProp(node, NTxPropName.ORIGIN).asPoint3D().orElse(new NTxPoint3D(0, 0, 0)), b);
                NTxPoint3D size = Shape3DHelper.convertPoint(NTxValue.ofProp(node, NTxPropName.SIZE).asPoint3D().orElse(new NTxPoint3D(0, 0, 0)), b);
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
        NTxPoint3D rotate = NTxValue.ofProp(node, "rotate").asPoint3D().orElse(new NTxPoint3D(0, 0, 0));
        NObjectElement rotateVector = NTxValue.ofProp(node, "rotate-vector").asElement().flatMap(x -> x.asObject()).orElse(null);
        NObjectElement rotateLine = NTxValue.ofProp(node, "rotate-line").asElement().flatMap(x -> x.asObject()).orElse(null);
        NTxMatrix3D m = NTxMatrix3D.identity();
        m = m.rotate(Math.toRadians(rotate.getX()), Math.toRadians(rotate.getY()), Math.toRadians(rotate.getZ()));
        if (rotateVector != null) {
            NTxPoint3D vector = NTxValue.of(rotateVector.get("vector")).asPoint3D().orElse(null);
            double angle = NTxValue.of(rotateVector.get("angle")).asDouble().orElse(0.0);
            if (vector != null && angle != 0) {
                m = m.rotateVector(
                        vector,
                        Math.toRadians(angle)

                );
            }
        }
        if (rotateLine != null) {
            NTxPoint3D a = NTxValue.of(rotateLine.get("from")).asPoint3D().orElse(null);
            NTxPoint3D b = NTxValue.of(rotateLine.get("to")).asPoint3D().orElse(null);
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

        NTxPoint3D scale = NTxValue.ofProp(node, "scale").asPoint3D().orElse(new NTxPoint3D(1, 1, 1));
        NTxPoint3D translate = NTxValue.ofProp(node, "translate").asPoint3D().orElse(new NTxPoint3D(0, 0, 0));

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
                    m = applyTransform(m, o,bounds);
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
                    _dist=new NTxPoint3D(NTxValue.of(args[0]).asDouble().get(),
                            NTxValue.of(args[1]).asDouble().get(),
                            NTxValue.of(args[2]).asDouble().get());
                }else  if (args.length == 1
                        && NTxValue.of(args[0]).asDouble().isPresent()
                ) {
                    _dist=new NTxPoint3D(NTxValue.of(args[0]).asDouble().get(),
                            NTxValue.of(args[0]).asDouble().get(),
                            NTxValue.of(args[0]).asDouble().get());
                }else {
                    for (NElement arg : args) {
                        NTxValue u = NTxValue.of(arg);
                        NTxPoint3D pn = u.asPoint3D().orNull();
                        if (pn != null) {
                            _dist = pn;
                            break;
                        }
                    }
                }
                if (_dist != null) {
                    _dist = Shape3DHelper.convertPoint(_dist, b);
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
                                NTxPoint3D pn = NTxValue.of(np.value()).asPoint3D().orNull();
                                if (pn != null) {
                                    _lineFrom = pn;
                                }
                                break;
                            }
                            case "to": {
                                NTxPoint3D pn = NTxValue.of(np.value()).asPoint3D().orNull();
                                if (pn != null) {
                                    _lineTo = pn;
                                }
                                break;
                            }
                            case "vector": {
                                NTxPoint3D pn = NTxValue.of(np.value()).asPoint3D().orNull();
                                if (pn != null) {
                                    _vector = pn;
                                }
                                break;
                            }
                            case "angle": {
                                NTxPoint3D pn = NTxValue.of(np.value()).asPoint3D().orNull();
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
                        NTxPoint3D pn = u.asPoint3D().orNull();
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
                                _lineFrom == null ? new NTxPoint3D(0, 0, 0) : _lineFrom,
                                _lineTo == null ? new NTxPoint3D(0, 0, 0) : _lineTo,
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
                    NTxPoint3D pn = u.asPoint3D().orNull();
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
