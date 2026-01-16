package net.thevpc.ntexup.lib.geometry2d.impl;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.lib.geometry2d.*;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NIllegalArgumentException;
import org.locationtech.jts.geom.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JTSHelper {
    // Shared GeometryFactory (thread-safe)
    private static final GeometryFactory GF = new GeometryFactory();

    private static Coordinate toCoordinate(NTxPoint2D p) {
        return new Coordinate(p.x, p.y);
    }

    private static Coordinate[] toCoordinates(List<NTxPoint2D> points) {
        return points.stream().map(JTSHelper::toCoordinate).toArray(Coordinate[]::new);
    }

    public static Geometry toJTS(NTxRegion2D region) {
        if (region instanceof NTxPolygonWithHoles2D) {
            NTxPolygonWithHoles2D poly = (NTxPolygonWithHoles2D) region;
            // Convert exterior ring
            LinearRing shell = toLinearRing(poly.exterior());

            // Convert holes
            List<NTxRing2D> holes = poly.holes();
            LinearRing[] holeRings = new LinearRing[holes.size()];
            for (int i = 0; i < holes.size(); i++) {
                holeRings[i] = toLinearRing(holes.get(i));
            }

            // Build polygon
            Polygon jtsPoly = GF.createPolygon(shell, holeRings);

            // Optional: validate and repair
            if (!jtsPoly.isValid()) {
                jtsPoly = (Polygon) jtsPoly.buffer(0); // repair
            }

            return jtsPoly;
        }
        if (region instanceof NTxPolygon2D) {
            NTxPolygon2D poly = (NTxPolygon2D) region;
            return toJTSFromPointsList(poly.getPoints());
        }
        if (region instanceof NTxTriangle2D) {
            NTxTriangle2D poly = (NTxTriangle2D) region;
            return toJTSFromPointsList(poly.points());
        }
        if (region instanceof NTxRectangle2D) {
            NTxRectangle2D poly = (NTxRectangle2D) region;
            return toJTSFromPointsList(poly.points());
        }
        if (region instanceof NTxRegion2DGroup) {
            NTxRegion2DGroup poly = (NTxRegion2DGroup) region;
            List<NTxRegion2D> regions = poly.children();
            if (regions.isEmpty()) {
                return GF.createPolygon(null, null);
            }
            Geometry g = toJTS(regions.get(0));
            for (int i = 1; i < regions.size(); i++) {
                g = g.union(toJTS(regions.get(i)));
            }
            return g;
        }
        if (region instanceof NTxConcatNTxRegion2D) {
            NTxConcatNTxRegion2D poly = (NTxConcatNTxRegion2D) region;
            List<NTxRegion2D> regions = poly.children();
            if (regions.isEmpty()) {
                return GF.createPolygon(null, null);
            }
            Geometry g = toJTS(regions.get(0));
            for (int i = 1; i < regions.size(); i++) {
                g = g.union(toJTS(regions.get(i)));
            }
            return g;
        }
        if (region instanceof NTxIntersectNTxRegion2D) {
            NTxIntersectNTxRegion2D poly = (NTxIntersectNTxRegion2D) region;
            List<NTxRegion2D> regions = poly.children();

            if (regions.isEmpty()) {
                return GF.createPolygon(null, null);
            }
            Geometry g = toJTS(regions.get(0));
            for (int i = 1; i < regions.size(); i++) {
                g = g.intersection(toJTS(regions.get(i)));
            }
            return g;
        }
        if (region instanceof NTxReverseIntersectNTxRegion2D) {
            NTxReverseIntersectNTxRegion2D rev = (NTxReverseIntersectNTxRegion2D) region;
            return toJTS(rev.first()).difference(toJTS(rev.second()));
        }
        if (region instanceof NTxSubstructNTxRegion2D) {
            NTxSubstructNTxRegion2D poly = (NTxSubstructNTxRegion2D) region;
            return toJTS(poly.first()).difference(toJTS(poly.second()));
        }
        if (region instanceof NTxEmptyRegion2D) {
            return GF.createPolygon(null, null); // valid empty polygon
        }
        if (region instanceof NTxEllipse2D) {
            return toJTSFromEllipse((NTxEllipse2D) region);
        }
        throw new NIllegalArgumentException(NMsg.ofC("unsupported region type", region.getClass()));
    }

    private static Geometry toJTSFromEllipse(NTxEllipse2D ellipse) {
        // Use reasonable default tessellation (e.g., 64 segments)
        return toJTSFromPointsList(tessellateEllipse(
                ellipse.center().getX(),
                ellipse.center().getY(),
                ellipse.radius().getX(), // assuming NTxDistance has value()
                ellipse.radius().getY(),
                64
        ));
    }

    private static List<NTxPoint2D> tessellateEllipse(double cx, double cy, double rx, double ry, int segments) {
        List<NTxPoint2D> pts = new ArrayList<>(segments + 1);
        for (int i = 0; i < segments; i++) {
            double theta = 2 * Math.PI * i / segments;
            pts.add(new NTxPoint2D(cx + rx * Math.cos(theta), cy + ry * Math.sin(theta)));
        }
        pts.add(pts.get(0)); // close
        return pts;
    }

    private static Polygon toJTSFromPointsList(List<NTxPoint2D> points) {
        // Convert exterior ring
        LinearRing shell = toLinearRing(points);


        // Build polygon
        Polygon jtsPoly = GF.createPolygon(shell);

        // Optional: validate and repair
        if (!jtsPoly.isValid()) {
            jtsPoly = (Polygon) jtsPoly.buffer(0); // repair
        }
        return jtsPoly;
    }

    private static LinearRing toLinearRing(NTxRing2D ring) {
        List<NTxPoint2D> pts = ring.getPoints();
        return toLinearRing(pts);
    }

    private static LinearRing toLinearRing(List<NTxPoint2D> pts) {
        if (pts.size() < 3) {
            throw new IllegalArgumentException("Ring must have at least 3 points");
        }
        Coordinate[] coords = pts.stream()
                .map(p -> new Coordinate(p.getX(), p.getY()))
                .toArray(Coordinate[]::new);
        // Ensure closed
        if (!coords[0].equals2D(coords[coords.length - 1])) {
            Coordinate[] closed = new Coordinate[coords.length + 1];
            System.arraycopy(coords, 0, closed, 0, coords.length);
            closed[closed.length - 1] = coords[0];
            coords = closed;
        }
        // JTS requires at least 4 coords for a ring (triangle + closing point)
        if (coords.length < 4) {
            // Handle triangle case
            if (coords.length == 3) {
                Coordinate[] tri = {coords[0], coords[1], coords[2], coords[0]};
                coords = tri;
            } else {
                throw new IllegalArgumentException("Invalid ring");
            }
        }
        return GF.createLinearRing(coords);
    }

    public static NTxRegion2D simplify(NTxRegion2D all) {
        Geometry jts = toJTS(all);
        return fromJTS(jts);
    }
    public static List<NTxPolygonWithHoles2D> toPolygonsWithHolesInternal(NTxRegion2D all) {
        Geometry jts = toJTS(all);
        return toPolygonsWithHolesInternal(jts);
    }


    public static NTxRegion2D fromJTS(Geometry geom) {
        int g = geom.getNumGeometries();
        if(g==0){
            return NTxEmptyRegion2DImpl.INSTANCE;
        }
        List<NTxRegion2D> result = new ArrayList<>();
        // Handle MultiPolygon or single Polygon
        for (int i = 0; i < g; i++) {
            Geometry g2 = geom.getGeometryN(i);
            if (g2 instanceof Polygon) {
                result.add(fromJTSAsPolygon((Polygon) g2));
            }
            // Ignore non-area geometries (shouldn't happen if input was area-only)
        }
        if(result.isEmpty()){
            return NTxEmptyRegion2DImpl.INSTANCE;
        }
        if(result.size()==1){
            return result.get(0);
        }
        return new NTxRegion2DGroupImpl(result);
    }

    public static List<NTxPolygonWithHoles2D> toPolygonsWithHolesInternal(Geometry geom) {
        int g = geom.getNumGeometries();
        if(g==0){
            return Collections.emptyList();
        }
        List<NTxPolygonWithHoles2D> result = new ArrayList<>();
        // Handle MultiPolygon or single Polygon
        for (int i = 0; i < g; i++) {
            Geometry g2 = geom.getGeometryN(i);
            if (g2 instanceof Polygon) {
                Polygon poly = (Polygon) g2;
                // Exterior ring
                NTxRing2D shell = fromLinearRing(poly.getExteriorRing());

                // Interior rings (holes)
                List<NTxRing2D> holes = new ArrayList<>();
                for (int j = 0; j < poly.getNumInteriorRing(); j++) {
                    holes.add(fromLinearRing(poly.getInteriorRingN(j)));
                }
                result.add(new NTxPolygonWithHoles2DImpl(shell, holes));
            }
            // Ignore non-area geometries (shouldn't happen if input was area-only)
        }
        return result;
    }

    private static NTxRegion2D fromJTSAsPolygon(Polygon poly) {
        // Exterior ring
        NTxRing2D shell = fromLinearRing(poly.getExteriorRing());

        // Interior rings (holes)
        List<NTxRing2D> holes = new ArrayList<>();
        for (int i = 0; i < poly.getNumInteriorRing(); i++) {
            holes.add(fromLinearRing(poly.getInteriorRingN(i)));
        }
        if(holes.isEmpty()){
            List<NTxPoint2D> points = shell.getPoints();
            if(points.size()==3){
                return new NTxTriangle2DImpl(points.get(0), points.get(1), points.get(2));
            }
            return new NTxPolygon2DImpl(points);
        }
        return new NTxPolygonWithHoles2DImpl(shell, holes); // implement this class
    }

    private static NTxRing2D fromLinearRing(org.locationtech.jts.geom.LineString ring) {
        Coordinate[] coords = ring.getCoordinates();
        List<NTxPoint2D> points = new ArrayList<>(coords.length);
        for (Coordinate c : coords) {
            points.add(new NTxPoint2D(c.x, c.y)); // assuming NTxPoint2D constructor
        }
        return new NTxRing2DImpl(points); // implement this class
    }

}
