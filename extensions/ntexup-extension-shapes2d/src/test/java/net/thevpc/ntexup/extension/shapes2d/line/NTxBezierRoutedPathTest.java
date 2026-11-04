package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NTxBezierRoutedPathTest {

    @Test
    void quadStartsAndEndsOnItsAnchors() {
        NTxPoint2D from = new NTxPoint2D(0, 0);
        NTxPoint2D ctrl = new NTxPoint2D(50, 100);
        NTxPoint2D to = new NTxPoint2D(100, 0);
        NTxRoutedPath path = NTxBezierRoutedPath.quad(from, ctrl, to);

        assertPoint(from, path.pointAt(0));
        assertPoint(to, path.pointAt(path.length()));
        assertTrue(path.length() > Math.hypot(to.x - from.x, to.y - from.y),
                "a curved path must be longer than the straight chord");
    }

    @Test
    void quadMidpointFollowsTheControlPoint() {
        NTxRoutedPath path = NTxBezierRoutedPath.quad(
                new NTxPoint2D(0, 0),
                new NTxPoint2D(50, 100),
                new NTxPoint2D(100, 0));
        NTxPoint2D mid = path.pointAt(path.length() / 2);
        assertEquals(50, mid.x, 0.5);
        assertTrue(mid.y > 40, "the midpoint should be pulled toward the control point");
    }

    @Test
    void cubicStartsAndEndsOnItsAnchors() {
        NTxPoint2D from = new NTxPoint2D(0, 0);
        NTxPoint2D to = new NTxPoint2D(100, 100);
        NTxRoutedPath path = NTxBezierRoutedPath.cubic(from,
                new NTxPoint2D(0, 100),
                new NTxPoint2D(100, 0),
                to);

        assertPoint(from, path.pointAt(0));
        assertPoint(to, path.pointAt(path.length()));
        assertTrue(path.vertices().size() > 2);
    }

    @Test
    void normalIsPerpendicularToTheTangent() {
        NTxRoutedPath path = NTxBezierRoutedPath.quad(
                new NTxPoint2D(0, 0),
                new NTxPoint2D(50, 100),
                new NTxPoint2D(100, 0));
        double s = path.length() / 2;
        NTxPoint2D n = path.normalAt(s);
        assertEquals(1.0, Math.hypot(n.x, n.y), 1e-6);
    }

    private static void assertPoint(NTxPoint2D expected, NTxPoint2D actual) {
        assertEquals(expected.x, actual.x, 1e-6);
        assertEquals(expected.y, actual.y, 1e-6);
    }
}
