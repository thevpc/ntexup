package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure geometry helpers for {@link NTxConnectorBuilder}. Kept separate so the
 * anchor/clipping math can be unit tested without a rendering context.
 */
public final class NTxConnectorGeometry {
    private NTxConnectorGeometry() {
    }

    public static NTxPoint2D center(NTxBounds2D bounds, AffineTransform toRoot) {
        double cx = bounds.minX() + bounds.widthX() / 2.0;
        double cy = bounds.minY() + bounds.widthY() / 2.0;
        Point2D p = toRoot.transform(new Point2D.Double(cx, cy), null);
        return new NTxPoint2D(p.getX(), p.getY());
    }

    /**
     * One of {@code left/right/top/bottom/top-left/.../center}, in the node's
     * local frame and then mapped to the page frame.
     */
    public static NTxPoint2D sidePoint(NTxBounds2D bounds, AffineTransform toRoot, String side) {
        double minX = bounds.minX();
        double maxX = bounds.minX() + bounds.widthX();
        double minY = bounds.minY();
        double maxY = bounds.minY() + bounds.widthY();
        double cx = bounds.minX() + bounds.widthX() / 2.0;
        double cy = bounds.minY() + bounds.widthY() / 2.0;
        double x = cx;
        double y = cy;
        String s = side == null ? "" : side.toLowerCase().trim();
        switch (s) {
            case "left":
                x = minX;
                break;
            case "right":
                x = maxX;
                break;
            case "top":
                y = minY;
                break;
            case "bottom":
                y = maxY;
                break;
            case "top-left":
                x = minX;
                y = minY;
                break;
            case "top-right":
                x = maxX;
                y = minY;
                break;
            case "bottom-left":
                x = minX;
                y = maxY;
                break;
            case "bottom-right":
                x = maxX;
                y = maxY;
                break;
            default:
                break;
        }
        Point2D p = toRoot.transform(new Point2D.Double(x, y), null);
        return new NTxPoint2D(p.getX(), p.getY());
    }

    /**
     * Clips the segment {@code center -> other} against {@code outline} and
     * returns the boundary point closest to {@code center}. If {@code other}
     * is already inside the outline it is returned unchanged; degenerate or
     * non-intersecting cases fall back to {@code center}.
     */
    public static NTxPoint2D intersectOutline(Shape outline, Point2D center, NTxPoint2D other) {
        if (other == null) {
            return new NTxPoint2D(center.getX(), center.getY());
        }
        if (outline == null || outline.contains(other.x, other.y)) {
            return other;
        }
        double bestDist2 = Double.MAX_VALUE;
        double bestX = center.getX();
        double bestY = center.getY();
        for (double[] seg : segments(outline)) {
            double[] ip = segmentIntersection(center.getX(), center.getY(), other.x, other.y,
                    seg[0], seg[1], seg[2], seg[3]);
            if (ip != null) {
                double dist2 = (ip[0] - center.getX()) * (ip[0] - center.getX())
                        + (ip[1] - center.getY()) * (ip[1] - center.getY());
                if (dist2 < bestDist2) {
                    bestDist2 = dist2;
                    bestX = ip[0];
                    bestY = ip[1];
                }
            }
        }
        return new NTxPoint2D(bestX, bestY);
    }

    public static List<double[]> segments(Shape shape) {
        List<double[]> segs = new ArrayList<>();
        if (shape == null) {
            return segs;
        }
        PathIterator pi = shape.getPathIterator(null);
        double[] coords = new double[6];
        double firstX = 0;
        double firstY = 0;
        double prevX = 0;
        double prevY = 0;
        boolean started = false;
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    prevX = coords[0];
                    prevY = coords[1];
                    firstX = prevX;
                    firstY = prevY;
                    started = true;
                    break;
                case PathIterator.SEG_LINETO:
                    segs.add(new double[]{prevX, prevY, coords[0], coords[1]});
                    prevX = coords[0];
                    prevY = coords[1];
                    break;
                case PathIterator.SEG_CLOSE:
                    if (started) {
                        segs.add(new double[]{prevX, prevY, firstX, firstY});
                    }
                    break;
                default:
                    // outline shapes are polygons; ignore curve segments
                    break;
            }
            pi.next();
        }
        return segs;
    }

    /**
     * Intersection of segment {@code (x1,y1)-(x2,y2)} with {@code (x3,y3)-(x4,y4)}.
     * Returns {@code null} for parallel or non-overlapping segments.
     */
    static double[] segmentIntersection(double x1, double y1, double x2, double y2,
                                        double x3, double y3, double x4, double y4) {
        double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(d) < 1e-12) {
            return null;
        }
        double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / d;
        double u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / d;
        if (t >= -1e-9 && t <= 1 + 1e-9 && u >= -1e-9 && u <= 1 + 1e-9) {
            return new double[]{x1 + t * (x2 - x1), y1 + t * (y2 - y1)};
        }
        return null;
    }
}
