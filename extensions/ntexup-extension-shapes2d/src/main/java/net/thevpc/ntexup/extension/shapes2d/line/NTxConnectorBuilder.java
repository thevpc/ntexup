package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.NTxArrow;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.primitives.NtxElement2DPolyline;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.eval.NTxValueByType;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxGeometryIndex;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxPlaced;
import net.thevpc.ntexup.api.renderer.NTxRenderSession;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.lib.geometry2d.NTxElement2DFactory;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NParamOrChild;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A line whose endpoints are named nodes. Endpoints are clipped against the
 * target outline, then the connector is drawn after the whole page so the
 * geometry index contains every node regardless of source order.
 */
public class NTxConnectorBuilder implements NTxNodeBuilder {
    private static final Set<String> STRAIGHT_ROUTES = new HashSet<>(Arrays.asList(
            "straight", "direct", "line", "none", "auto"
    ));
    private static final Map<NTxNode, Set<String>> WARNED = new WeakHashMap<>();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.CONNECTOR)
                .parseParam().matchesAnyNonPair().storeFirstMissingName(NTxPropName.FROM, NTxPropName.TO).then()
                .parseParam().matchesNamedPair(NTxPropName.FROM, NTxPropName.TO, "via",
                        NTxPropName.START_ARROW, NTxPropName.END_ARROW, "arrow", "label", "flow", "route").then()
                .initializeNodeAction((node, bctx) -> node.setProperty(NTxPropName.LAYOUT, NElement.ofString("none")))
                .renderComponent(this::renderMain);
    }

    public void renderMain(NTxRendererContext ctx) {
        NTxNode node = ctx.node();
        if (node.getPropertyValue("rotate").isPresent()) {
            warnOnce(ctx, node, "connector rotate is meaningless and ignored");
        }
        warnUnsupportedRoute(ctx, node);
        NTxRenderSession session = ctx.session();
        AffineTransform ancestorsToRoot = session.geometry().currentToRoot();
        session.defer(node, ctx, ancestorsToRoot, drawCtx -> draw(drawCtx, node, ancestorsToRoot));
    }

    private void draw(NTxRendererContext ctx, NTxNode node, AffineTransform ancestorsToRoot) {
        NTxGeometryIndex geometry = ctx.session().geometry();

        NElement fromElem = eval(ctx, node.getPropertyValue(NTxPropName.FROM).orNull());
        NElement toElem = eval(ctx, node.getPropertyValue(NTxPropName.TO).orNull());

        Endpoint from = endpoint(ctx, node, geometry, ancestorsToRoot, fromElem);
        if (from == null) {
            return;
        }
        Endpoint to = endpoint(ctx, node, geometry, ancestorsToRoot, toElem);
        if (to == null) {
            return;
        }
        // A hidden target is not an error: draw nothing, silently.
        if (from.hidden || to.hidden) {
            return;
        }

        NTxPoint2D fromAnchor = anchor(from, to);
        NTxPoint2D toAnchor = anchor(to, from);
        if (fromAnchor == null || toAnchor == null) {
            return;
        }

        List<NTxPoint2D> pts = new ArrayList<>();
        pts.add(fromAnchor);
        appendVia(ctx, node, ancestorsToRoot, pts);
        pts.add(toAnchor);

        NTxGraphics g = ctx.graphics();
        Paint linePaint = ctx.getLineColor(true);
        NtxElement2DPolyline polyline = (NtxElement2DPolyline) NTxElement2DFactory.polyline(pts.toArray(new NTxPoint2D[0]))
                .setLineStroke(g.createStroke(ctx.getStroke()))
                .setLinePaint(linePaint);
        applyArrows(ctx, polyline);
        g.draw2D(polyline);

        NTxRoutedPath path = new NTxPolylineRoutedPath(pts);
        NTxLabelDecorator.draw(ctx, g, path);
        NTxFlow flow = NTxFlow.parse(node.getPropertyValue("flow").orNull(), ctx);
        if (flow != null) {
            NTxFlowDecorator.draw(ctx, g, path, flow);
        }

        if (NTxValueByName.isDebug(ctx)) {
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            for (NTxPoint2D pt : pts) {
                minX = Math.min(minX, pt.x);
                minY = Math.min(minY, pt.y);
                maxX = Math.max(maxX, pt.x);
                maxY = Math.max(maxY, pt.y);
            }
            if (minX <= maxX && minY <= maxY) {
                NTxBounds2D rb = NTxBounds2D.of(minX, maxX, minY, maxY);
                g.setColor(NTxValueByName.getDebugColor(ctx));
                g.drawRect(rb);
            }
        }
    }

    private NTxPoint2D anchor(Endpoint endpoint, Endpoint other) {
        if (endpoint.placed == null) {
            return endpoint.point;
        }
        NTxPlaced p = endpoint.placed;
        if (endpoint.side != null && !"auto".equalsIgnoreCase(endpoint.side)) {
            return NTxConnectorGeometry.sidePoint(p.bounds, p.toRoot, endpoint.side);
        }
        NTxPoint2D center = NTxConnectorGeometry.center(p.bounds, p.toRoot);
        NTxPoint2D otherPoint = preliminary(other);
        if (otherPoint == null) {
            otherPoint = center;
        }
        return NTxConnectorGeometry.intersectOutline(p.outline(), new Point2D.Double(center.x, center.y), otherPoint);
    }

    private NTxPoint2D preliminary(Endpoint e) {
        if (e.placed == null) {
            return e.point;
        }
        if (e.side != null && !"auto".equalsIgnoreCase(e.side)) {
            return NTxConnectorGeometry.sidePoint(e.placed.bounds, e.placed.toRoot, e.side);
        }
        return NTxConnectorGeometry.center(e.placed.bounds, e.placed.toRoot);
    }

    private Endpoint endpoint(NTxRendererContext ctx, NTxNode node, NTxGeometryIndex geometry,
                              AffineTransform ancestorsToRoot, NElement elem) {
        Endpoint e = new Endpoint();
        if (elem != null && elem.isAnyString()) {
            String s = elem.asStringValue().get();
            int at = s.indexOf('@');
            if (at >= 0) {
                e.ref = s.substring(0, at).trim();
                e.side = s.substring(at + 1).trim();
            } else {
                e.ref = s.trim();
            }
        } else if (elem != null && elem.isName()) {
            e.ref = elem.toString().trim();
        }
        if (e.ref != null && !e.ref.isEmpty()) {
            NOptional<NTxPlaced> placed = geometry.resolve(node, e.ref);
            if (!placed.isPresent()) {
                warnOnce(ctx, node, "connector unresolved target '" + e.ref + "'");
                return null;
            }
            e.placed = placed.get();
            e.hidden = e.placed.hidden;
            return e;
        }
        NTxPoint2D free = NTxLineBuilder.resolvePoint2D(elem, ctx, freeBounds(ctx));
        Point2D tp = ancestorsToRoot.transform(new Point2D.Double(free.x, free.y), null);
        e.point = new NTxPoint2D(tp.getX(), tp.getY());
        return e;
    }

    /**
     * Free coordinates are percentages of the connector's available (parent)
     * space, not of the zero-sized layout box of a {@code layout:none} node.
     */
    private NTxBounds2D freeBounds(NTxRendererContext ctx) {
        NTxBounds2D parent = ctx.parentBounds2D();
        return parent != null ? parent : ctx.selfBounds2D();
    }

    private void appendVia(NTxRendererContext ctx, NTxNode node, AffineTransform ancestorsToRoot, List<NTxPoint2D> pts) {
        NElement via = eval(ctx, node.getPropertyValue("via").orNull());
        if (via == null) {
            return;
        }
        NOptional<NTxPoint2D[]> arr = NTxValue.of(via).asPoint2DArray();
        if (arr.isPresent()) {
            for (NTxPoint2D vp : arr.get()) {
                pts.add(transform(ancestorsToRoot, vp));
            }
            return;
        }
        if (via.isListContainer()) {
            java.util.List<NElement> children = via.asListContainer().get().children();
            if (children.size() == 2 && children.get(0).isNumber() && children.get(1).isNumber()) {
                pts.add(transform(ancestorsToRoot, NTxLineBuilder.resolvePoint2D(via, ctx, freeBounds(ctx))));
                return;
            }
        }
        if (via.isListOrParametrizedContainer()) {
            for (NParamOrChild c : via.asListOrParametrizedContainer().get().paramsOrChildren()) {
                NElement ce = eval(ctx, c.element());
                pts.add(transform(ancestorsToRoot, NTxLineBuilder.resolvePoint2D(ce, ctx, freeBounds(ctx))));
            }
        }
    }

    private NTxPoint2D transform(AffineTransform t, NTxPoint2D p) {
        Point2D q = t.transform(new Point2D.Double(p.x, p.y), null);
        return new NTxPoint2D(q.getX(), q.getY());
    }

    private void applyArrows(NTxRendererContext ctx, NtxElement2DPolyline polyline) {
        NTxArrow arrow = NTxValueByType.getArrow(ctx, "arrow").orNull();
        NTxArrow start = NTxValueByType.getArrow(ctx, NTxPropName.START_ARROW).orNull();
        NTxArrow end = NTxValueByType.getArrow(ctx, NTxPropName.END_ARROW).orNull();
        if (arrow != null) {
            if (start == null) {
                start = arrow;
            }
            if (end == null) {
                end = arrow;
            }
        }
        polyline.setStartArrow(start);
        polyline.setEndArrow(end);
    }

    private void warnUnsupportedRoute(NTxRendererContext ctx, NTxNode node) {
        NElement route = eval(ctx, node.getPropertyValue("route").orNull());
        if (route == null) {
            return;
        }
        String s = route.asStringValue().orNull();
        if (s == null || s.trim().isEmpty()) {
            return;
        }
        if (!STRAIGHT_ROUTES.contains(s.trim().toLowerCase())) {
            warnOnce(ctx, node, "connector route '" + s.trim() + "' is not supported; using a straight route");
        }
    }

    private NElement eval(NTxRendererContext ctx, NElement e) {
        return e == null ? null : ctx.evalExpression(e).orElse(e);
    }

    private void warnOnce(NTxRendererContext ctx, NTxNode node, String message) {
        synchronized (WARNED) {
            Set<String> set = WARNED.get(node);
            if (set == null) {
                set = new HashSet<>();
                WARNED.put(node, set);
            }
            if (!set.add(message)) {
                return;
            }
        }
        ctx.engine().log().log(NMsg.ofC("%s", message).asWarning(), net.thevpc.ntexup.api.util.NTxUtils.sourceOf(node));
    }

    private static final class Endpoint {
        String ref;
        String side;
        NTxPlaced placed;
        NTxPoint2D point;
        boolean hidden;
    }
}
