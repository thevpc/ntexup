package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapts Bezier curves to the {@link NTxRoutedPath} contract so labels and
 * flow particles can ride quadratic and cubic curves exactly like they ride
 * lines and polylines.
 *
 * <p>The curve is flattened to a dense polyline (the same representation used
 * by the geometry helpers); the sampling is fine enough that the label anchor
 * and particle trail are visually continuous, and it keeps the path math in
 * one tested place instead of duplicating Newton iterations per renderer.
 */
public final class NTxBezierRoutedPath {
    private static final int STEPS = 128;

    private NTxBezierRoutedPath() {
    }

    public static NTxRoutedPath quad(NTxPoint2D from, NTxPoint2D ctrl, NTxPoint2D to) {
        List<NTxPoint2D> pts = new ArrayList<>(STEPS + 1);
        for (int i = 0; i <= STEPS; i++) {
            double t = (double) i / STEPS;
            double mt = 1.0 - t;
            double a = mt * mt;
            double b = 2.0 * mt * t;
            double c = t * t;
            pts.add(new NTxPoint2D(
                    a * from.x + b * ctrl.x + c * to.x,
                    a * from.y + b * ctrl.y + c * to.y));
        }
        return new NTxPolylineRoutedPath(pts);
    }

    public static NTxRoutedPath cubic(NTxPoint2D from, NTxPoint2D ctrl1, NTxPoint2D ctrl2, NTxPoint2D to) {
        List<NTxPoint2D> pts = new ArrayList<>(STEPS + 1);
        for (int i = 0; i <= STEPS; i++) {
            double t = (double) i / STEPS;
            double mt = 1.0 - t;
            double a = mt * mt * mt;
            double b = 3.0 * mt * mt * t;
            double c = 3.0 * mt * t * t;
            double d = t * t * t;
            pts.add(new NTxPoint2D(
                    a * from.x + b * ctrl1.x + c * ctrl2.x + d * to.x,
                    a * from.y + b * ctrl1.y + c * ctrl2.y + d * to.y));
        }
        return new NTxPolylineRoutedPath(pts);
    }
}
