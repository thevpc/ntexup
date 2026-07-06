package net.thevpc.ntexup.lib.geometry2d;


import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.primitives.*;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.lib.geometry2d.impl.*;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NStringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NTxElement2DFactory {
    public static NtxElement2DLine line(NTxPoint2D from, NTxPoint2D to) {
        return new NtxElement2DLine(from, to);
    }

    public static NtxElement2DQuadCurve quad(NTxPoint2D from, NTxPoint2D ctrl, NTxPoint2D to) {
        return new NtxElement2DQuadCurve(from, to).setCtrl(ctrl);
    }

    public static NtxElement2DCubicCurve cubic(NTxPoint2D from, NTxPoint2D ctrl1, NTxPoint2D ctrl2, NTxPoint2D to) {
        return new NtxElement2DCubicCurve(from, to).setCtrl1(ctrl1).setCtrl2(ctrl2);
    }

    public static NtxElement2DPolygon polygon(NTxPoint2D... points) {
        return new NtxElement2DPolygon(points, true, true);
    }

    public static NtxElement2DPolyline polyline(NTxPoint2D... points) {
        return new NtxElement2DPolyline(points);
    }

    public static NOptional<NTxRegion2D> region(NElement region) {
        if (region == null) {
            return NOptional.ofNamedEmpty(NMsg.ofC("region for null"));
        }
        if (region.isArray()) {
            return NTx2DUtils.asPoint2DArray(region).map(x -> new NTxPolygon2DImpl(Arrays.asList(x)));
        }
        if (region.isNamed() && region.isListOrParametrizedContainer()) {
            NListOrParametrizedContainerElement li = region.asListOrParametrizedContainer().get();
            NParametrizedContainerElement lp = region.asParametrizedContainer().orNull();
            String name = li.asNamed().flatMap(x -> x.name()).orNull();

            switch (NTxUtils.uid(NStringUtils.strip(name))) {
                case "substruct": {
                    List<NTxRegion2D> acceptableChildren = new ArrayList<>();
                    for (NParamOrChild pchild : lp.paramsOrChildren()) {
                        if (!pchild.element().isNamed()) {
                            NTxRegion2D x = region(pchild.element()).orNull();
                            if (x != null) {
                                acceptableChildren.add(x);
                            }
                        }
                    }
                    if (acceptableChildren.size() == 2) {
                        return NOptional.of(new NTxSubstructNTxRegion2D(acceptableChildren.get(0), acceptableChildren.get(1)));
                    }
                    return NOptional.ofNamedEmpty(NMsg.ofC("region for %s",region));
                }
                case "reverse-intersect": {
                    List<NTxRegion2D> acceptableChildren = new ArrayList<>();
                    for (NParamOrChild pchild : lp.paramsOrChildren()) {
                        if (!pchild.element().isNamed()) {
                            NTxRegion2D x = region(pchild.element()).orNull();
                            if (x != null) {
                                acceptableChildren.add(x);
                            }
                        }
                    }
                    if (acceptableChildren.size() == 2) {
                        return NOptional.of(new NTxReverseIntersectNTxRegion2D(acceptableChildren.get(0), acceptableChildren.get(1)));
                    }
                    return NOptional.ofNamedEmpty(NMsg.ofC("region for %s",region));
                }
                case "intersect": {
                    List<NTxRegion2D> acceptableChildren = new ArrayList<>();
                    for (NParamOrChild pchild : lp.paramsOrChildren()) {
                        if (!pchild.element().isNamed()) {
                            NTxRegion2D x = region(pchild.element()).orNull();
                            if (x != null) {
                                acceptableChildren.add(x);
                            }
                        }
                    }
                    return NOptional.of(new NTxIntersectNTxRegion2D(acceptableChildren));
                }
                case "union":
                case "concat": {
                    List<NTxRegion2D> acceptableChildren = new ArrayList<>();
                    for (NParamOrChild pchild : lp.paramsOrChildren()) {
                        if (!pchild.element().isNamed()) {
                            NTxRegion2D x = region(pchild.element()).orNull();
                            if (x != null) {
                                acceptableChildren.add(x);
                            }
                        }
                    }
                    return NOptional.of(new NTxConcatNTxRegion2D(acceptableChildren));
                }
                case "polygon": {
                    List<NTxPoint2D> points = new ArrayList<>();
                    List<List<NTxPoint2D>> holes = new ArrayList<>();
                    for (NParamOrChild pchild : lp.paramsOrChildren()) {
                        NElement child = pchild.element();
                        if (!child.isNamedPair()) {
                            NTxPoint2D[] y = NTx2DUtils.asPoint2DArray(child).orNull();
                            if (y != null) {
                                points.addAll(Arrays.asList(y));
                            }
                        } else {
                            NPairElement p = child.asPair().get();
                            String key = p.key().asStringValue().get();
                            switch (NTxUtils.uid(key)) {
                                case "points": {
                                    NTxPoint2D[] a = NTx2DUtils.asPoint2DArray(p.value()).orNull();
                                    if (a != null) {
                                        points.addAll(Arrays.asList(a));
                                    }
                                    break;
                                }
                                case "hole":
                                case "holes": {
                                    NElement value = p.value();
                                    if (value.isArray()) {
                                        for (NElement nElement : value.asArray().get().children()) {
                                            holes.add(Arrays.asList(NTx2DUtils.asPoint2DArray(nElement).orNull()));
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    if (points.size() >= 3) {
                        if (!holes.isEmpty()) {
                            return NOptional.of(new NTxPolygonWithHoles2DImpl(
                                    new NTxRing2DImpl(points),
                                    holes.stream().map(x -> new NTxRing2DImpl(x)).collect(Collectors.toList())
                            ));
                        }
                        return NOptional.of(new NTxPolygon2DImpl(points));
                    }
                    return NOptional.ofNamedEmpty(NMsg.ofC("region for %s",region));
                }
                case "rectangle": {
                    NTxPoint2D position = null;
                    NTxSize2D size = null;
                    for (NParamOrChild pchild : lp.paramsOrChildren()) {
                        NElement child = pchild.element();
                        if (!child.isNamedPair()) {
                            //
                        } else {
                            NPairElement p = child.asPair().get();
                            String key = p.key().asStringValue().get();
                            switch (NTxUtils.uid(key)) {
                                case "position": {
                                    position = NTx2DUtils.asPoint2D(p.value()).orNull();
                                    break;
                                }
                                case "size": {
                                    size = NTx2DUtils.asSize2D(p.value()).orNull();
                                    break;
                                }
                            }
                        }
                    }
                    if (position == null) {
                        position = new NTxPoint2D(0.0, 0.0);
                    }
                    if (size == null) {
                        size = new NTxSize2D(100, 100);
                    }
                    return NOptional.of(new NTxRectangle2DImpl(position, size));
                }
                case "square": {
                    NTxPoint2D position = null;
                    NTxSize2D size = null;
                    for (NParamOrChild pchild : lp.paramsOrChildren()) {
                        NElement child = pchild.element();
                        if (!child.isNamedPair()) {
                            //
                        } else {
                            NPairElement p = child.asPair().get();
                            String key = p.key().asStringValue().get();
                            switch (NTxUtils.uid(key)) {
                                case "position": {
                                    position = NTx2DUtils.asPoint2D(p.value()).orNull();
                                    break;
                                }
                                case "size": {
                                    Double v = NTxValue.of(p.value()).asDouble().orElse(0.0);
                                    size = new NTxSize2D(v,v);
                                    break;
                                }
                            }
                        }
                    }
                    if (position == null) {
                        position = new NTxPoint2D(0.0, 0.0);
                    }
                    if (size == null) {
                        size = new NTxSize2D(100, 100);
                    }
                    return NOptional.of(new NTxRectangle2DImpl(position, size));
                }
                case "triangle": {
                    List<NTxPoint2D> points = new ArrayList<>();
                    for (NParamOrChild pchild : lp.paramsOrChildren()) {
                        NElement child = pchild.element();
                        if (!child.isNamedPair()) {
                            NTxPoint2D[] y = NTx2DUtils.asPoint2DArray(child).orNull();
                            if (y != null) {
                                points.addAll(Arrays.asList(y));
                            }
                        } else {
                            NPairElement p = child.asPair().get();
                            String key = p.key().asStringValue().get();
                            switch (NTxUtils.uid(key)) {
                                case "points": {
                                    NTxPoint2D[] a = NTx2DUtils.asPoint2DArray(p.value()).orNull();
                                    if (a != null) {
                                        points.addAll(Arrays.asList(a));
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    if (points.size() >= 3) {
                        return NOptional.of(new NTxTriangle2DImpl(points.get(0), points.get(1), points.get(2)));
                    }
                    return NOptional.ofNamedEmpty(NMsg.ofC("region for %s",region));
                }
                case "empty": {
                    return NOptional.of(new NTxEmptyRegion2DImpl());
                }
                case "ellipse": {
                    NTxPoint2D position = null;
                    NTxPoint2D center = null;
                    NTxSize2D radius = null;
                    NTxSize2D size = null;
                    for (NParamOrChild pchild : lp.paramsOrChildren()) {
                        NElement child = pchild.element();
                        if (!child.isNamedPair()) {
                            //
                        } else {
                            NPairElement p = child.asPair().get();
                            String key = p.key().asStringValue().get();
                            switch (NTxUtils.uid(key)) {
                                case "position": {
                                    position = NTx2DUtils.asPoint2D(p.value()).orNull();
                                    break;
                                }
                                case "center": {
                                    center = NTx2DUtils.asPoint2D(p.value()).orNull();
                                    break;
                                }
                                case "size": {
                                    size = NTx2DUtils.asSize2D(p.value()).orNull();
                                    break;
                                }
                                case "radius": {
                                    radius = NTx2DUtils.asSize2D(p.value()).orNull();
                                    break;
                                }
                            }
                        }
                    }
                    if (radius == null) {
                        if (size == null) {
                            size = new NTxSize2D(100, 100);
                        }
                        radius = size.mul(0.5);
                    }
                    if (center == null) {
                        if (position == null) {
                            position = new NTxPoint2D(0.0, 0.0);
                        }
                        center = position.plus(radius.asPoint());
                    }
                    return NOptional.of(new NTxEllipse2DImpl(center, size));
                }
                case "circle": {
                    NTxPoint2D position = null;
                    NTxPoint2D center = null;
                    NTxSize2D radius = null;
                    NTxSize2D size = null;
                    for (NParamOrChild pchild : lp.paramsOrChildren()) {
                        NElement child = pchild.element();
                        if (!child.isNamedPair()) {
                            //
                        } else {
                            NPairElement p = child.asPair().get();
                            String key = p.key().asStringValue().get();
                            switch (NTxUtils.uid(key)) {
                                case "position": {
                                    position = NTx2DUtils.asPoint2D(p.value()).orNull();
                                    break;
                                }
                                case "center": {
                                    Double v = NTxValue.of(p.value()).asDouble().orElse(0.0);
                                    center = new NTxPoint2D(v, v);
                                    break;
                                }
                                case "size": {
                                    Double v = NTxValue.of(p.value()).asDouble().orElse(0.0);
                                    size = new NTxSize2D(v,v);
                                    break;
                                }
                                case "radius": {
                                    Double v = NTxValue.of(p.value()).asDouble().orElse(0.0);
                                    radius = new NTxSize2D(v,v);
                                    break;
                                }
                            }
                        }
                    }
                    if (radius == null) {
                        if (size == null) {
                            size = new NTxSize2D(100, 100);
                        }
                        radius = size.mul(0.5);
                    }
                    if (center == null) {
                        if (position == null) {
                            position = new NTxPoint2D(0.0, 0.0);
                        }
                        center = position.plus(radius.asPoint());
                    }
                    return NOptional.of(new NTxEllipse2DImpl(center, size));
                }
            }
        }
        return NOptional.ofNamedEmpty(NMsg.ofC("region for %s",region));
    }
}
