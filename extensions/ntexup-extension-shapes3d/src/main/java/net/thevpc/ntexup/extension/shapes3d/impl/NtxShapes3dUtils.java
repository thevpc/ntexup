package net.thevpc.ntexup.extension.shapes3d.impl;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.eval.NTxValueByType;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.lib.geometry3d.NTxMatrix3D;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.lib.geometry3d.NtxFace;
import net.thevpc.ntexup.lib.geometry3d.impl.NTx3DUtils;
import net.thevpc.ntexup.lib.geometry3d.impl.composite.NtxFaceImpl;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.util.NUtils;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class NtxShapes3dUtils {

    public static NElement nodeToElement(NTxNode node) {
        NObjectElementBuilder b = NElement.ofObjectBuilder()
                .name(node.getName());
        for (NTxProp property : node.getProperties()) {
            b.addParam(property.getName(), property.getValue());
        }
        for (NTxNode child : node.children()) {
            b.add(nodeToElement(child));
        }
        return b.build();
    }

    public static NtxFace resolveFace(NElement element, NTxNodeRendererContext rendererContext) {
        if (element == null || !element.isAnyObject()) {
            return null;
        }
        boolean visible = true;
        boolean fillBackground = true;
        boolean drawContour = true;
        Paint background = null;
        Stroke stroke = null;
        boolean any = false;
        if (element.isAnyObject()) {
            NObjectElement o = element.asObject().get();
            for (NElement c : o) {
                if (c.isNamed("visible")) {
                    visible = true;
                    any = true;
                } else if (c.isNamedPair()) {
                    NPairElement p = c.asPair().get();
                    NElement k = p.key();
                    NElement v = p.value();
                    switch (NTxUtils.uid(k.asStringValue().get())) {
                        case "visible": {
                            visible = NTxValue.of(v).asBoolean().orElse(true);
                            any = true;
                            break;
                        }
                        case NTxPropName.FILL_BACKGROUND: {
                            fillBackground = NTxValue.of(v).asBoolean().orElse(true);
                            any = true;
                            break;
                        }
                        case NTxPropName.DRAW_CONTOUR: {
                            drawContour = NTxValue.of(v).asBoolean().orElse(true);
                            any = true;
                            break;
                        }
                        case NTxPropName.BACKGROUND_COLOR:
                        case "background":
                        case "color":
                        case "bg": {
                            background = NTxValue.of(v).asPaint().orNull();
                            any = true;
                            break;
                        }
                        case "stroke": {
                            stroke = rendererContext.graphics().createStroke(v);
                            any = true;
                            break;
                        }
                    }
                }
            }
        }
        if (any) {
            NtxFaceImpl f = new NtxFaceImpl();
            f.setBackground(background);
            f.setVisible(visible);
            f.setDrawContour(drawContour);
            f.setStroke(stroke);
            f.setFillBackground(fillBackground);
            return f;
        }
        return null;
    }

    public static NTxPoint3D resolvePoint(NTxNode node, String relativeName, String realName, Supplier<NTxPoint3D> def, NTxBounds2D b, RealToRelativeMapper mapper) {
        if (node.getPropertyValue(realName).isPresent()) {
            return NTx3DUtils.convertPoint(NTx3DUtils.asPoint3D(node, realName).orElseGet(def), b);
        }
        return NTx3DUtils.convertPoint(NTx3DUtils.asPoint3D(node, relativeName).orElseGet(def), b);
    }

    public static NTxPoint3D[][] resolvePointMatrix(NTxNode node, String relativeName, String realName, NTxBounds2D b, RealToRelativeMapper mapper) {
        if (node.getPropertyValue(realName).isPresent()) {
            NElement nElement = node.getPropertyValue(realName).get();
            if (nElement.isAnyArray()) {
                List<NTxPoint3D[]> collected = nElement.asArray().get()
                        .stream().map(x ->
                                Arrays.stream(NTx3DUtils.asPoint3DArray(NTxValue.of(x)).orElse(new NTxPoint3D[0])).map(mapper::mapPoint).map(y -> NTx3DUtils.convertPoint(y, b)).toArray(NTxPoint3D[]::new)
                        )
                        .collect(Collectors.toList());
                return collected.toArray(new NTxPoint3D[collected.size()][]);
            }
            return new NTxPoint3D[0][];
        }
        if (node.getPropertyValue(relativeName).isPresent()) {
            NElement nElement = node.getPropertyValue(relativeName).get();
            if (nElement.isAnyArray()) {
                List<NTxPoint3D[]> collected = nElement.asArray().get()
                        .stream().map(x ->
                                Arrays.stream(NTx3DUtils.asPoint3DArray(NTxValue.of(x)).orElse(new NTxPoint3D[0])).map(mapper::mapPoint).map(y -> NTx3DUtils.convertPoint(y, b)).toArray(NTxPoint3D[]::new)
                        )
                        .collect(Collectors.toList());
                return collected.toArray(new NTxPoint3D[collected.size()][]);
            }
            return new NTxPoint3D[0][];
        }
        return new NTxPoint3D[0][];
    }

    public static NTxPoint3D[] resolvePoints(NTxNode node, String relativeName, String realName, Supplier<NTxPoint3D[]> def, NTxBounds2D b, RealToRelativeMapper mapper) {
        if (node.getPropertyValue(realName).isPresent()) {
            return Arrays.stream(NTx3DUtils.asPoint3DArray(node, realName).orElseGet(def)).map(mapper::mapPoint).map(x -> NTx3DUtils.convertPoint(x, b)).toArray(NTxPoint3D[]::new);
        }
        return Arrays.stream(NTx3DUtils.asPoint3DArray(node, relativeName).orElseGet(def)).map(x -> NTx3DUtils.convertPoint(x, b)).toArray(NTxPoint3D[]::new);
    }

    public static void apply3dProps(NTxNode node, NtxElement3D g, NTxNodeRendererContext rendererContext, NTxBounds2D b) {
        g.setLineStroke(rendererContext.graphics().createStroke(
                NUtils.firstNonNullLazy(
                        NTxValueByType.getElement(node, rendererContext, "line-stroke").orNull(),
                        () -> NTxValueByType.getElement(node, rendererContext, "stroke").orNull()
                )
        ));
        g.setContourStroke(rendererContext.graphics().createStroke(
                NUtils.firstNonNullLazy(
                        NTxValueByType.getElement(node, rendererContext, "contour-stroke").orNull(),
                        () -> NTxValueByType.getElement(node, rendererContext, "stroke").orNull()
                )
        ));
        g.setForegroundPaint(NTxValueByType.getPaint(node, rendererContext, NTxPropName.FOREGROUND_COLOR, "foreground", "color", "fg").orElse(null));
        g.setBackgroundPaint(NTxValueByType.getPaint(node, rendererContext, NTxPropName.BACKGROUND_COLOR, "background", "bg").orNull());
        g.setContourPaint(NTxValueByType.getPaint(node, rendererContext, "contour-color").orElse(null));
        g.setLinePaint(NTxValueByType.getPaint(node, rendererContext, "line-color").orElse(null));

        Double _maxEdge = NTxValue.ofProp(node, "mesh-precision").asDouble().orNull();
        Boolean _showMesh = NTxValue.ofProp(node, "mesh-visible").asBoolean().orNull();

        g.setMeshVisible(_showMesh);
        g.setMeshPrecision(_maxEdge);
        g.setMeshPaint(NTxValueByType.getPaint(node, rendererContext, "mesh-color").orElse(null));
        g.setMeshStroke(rendererContext.graphics().createStroke(NTxValueByType.getElement(node, rendererContext, "mesh-stroke").orNull()));
//        g.setComposite(NTxValueByType.getComposite(node, rendererContext, "composite").orElse(null));
        g.setTransform(resolveTransform(node, b));
    }

    public static NTxMatrix3D resolveTransform(NTxNode node, NTxBounds2D bounds) {
        NTxPoint3D rotate = NTx3DUtils.asPoint3D(node, "rotate").orElse(NTxPoint3D.ofZero());
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
        NTxPoint3D translate = NTx3DUtils.asPoint3D(NTxValue.ofProp(node, "translate")).orElse(NTxPoint3D.ofZero());

        m = m.translate(translate.getX(), translate.getY(), translate.getZ());
        m = m.scale(scale.getX(), scale.getY(), scale.getZ());
        return m;
    }

    public static NTxMatrix3D applyTransform(NTxMatrix3D m, NElement e, NTxBounds2D bounds) {
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

    public static NTxMatrix3D applyTransform(NTxMatrix3D m, String name, NElement[] args, NTxBounds2D b) {
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
