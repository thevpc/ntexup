package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class NTxPolylineRoutedPathTest {
    @Test
    void basics() {
        NTxPolylineRoutedPath p = new NTxPolylineRoutedPath(Arrays.asList(
                new NTxPoint2D(0,0), new NTxPoint2D(10,0)));
        assertEquals(10.0, p.length(), 1e-9);
        NTxPoint2D m = p.pointAt(5);
        assertEquals(5, m.x, 1e-9);
        assertEquals(0, m.y, 1e-9);
        NTxPoint2D n = p.normalAt(5);
        assertEquals(0, n.x, 1e-9);
        assertEquals(-1, n.y, 1e-9);
    }

    @Test
    void verticalNormal() {
        NTxPolylineRoutedPath p = new NTxPolylineRoutedPath(Arrays.asList(
                new NTxPoint2D(0,0), new NTxPoint2D(0,10)));
        NTxPoint2D n = p.normalAt(5);
        assertEquals(-1, n.x, 1e-9);
        assertEquals(0, n.y, 1e-9);
    }

    @Test
    void degenerate() {
        NTxPolylineRoutedPath p = new NTxPolylineRoutedPath(Arrays.asList(
                new NTxPoint2D(5,5), new NTxPoint2D(5,5)));
        assertEquals(0.0, p.length(), 1e-9);
        NTxPoint2D pt = p.pointAt(0);
        assertEquals(5, pt.x, 1e-9);
    }

    @Test
    void multiSegmentLengthAndClamping() {
        NTxPolylineRoutedPath p = new NTxPolylineRoutedPath(Arrays.asList(
                new NTxPoint2D(0, 0), new NTxPoint2D(10, 0), new NTxPoint2D(10, 30)));
        assertEquals(40.0, p.length(), 1e-9);
        NTxPoint2D mid = p.pointAt(20);
        assertEquals(10, mid.x, 1e-9);
        assertEquals(10, mid.y, 1e-9);
        assertEquals(0, p.pointAt(-5).x, 1e-9);
        assertEquals(30, p.pointAt(1000).y, 1e-9);
    }

    @Test
    void extendedPointExtrapolatesOutsideTheEnds() {
        NTxPolylineRoutedPath p = new NTxPolylineRoutedPath(Arrays.asList(
                new NTxPoint2D(0, 0), new NTxPoint2D(10, 0), new NTxPoint2D(10, 30)));
        NTxPoint2D before = p.pointAtExtended(-5);
        assertEquals(-5, before.x, 1e-9);
        assertEquals(0, before.y, 1e-9);
        NTxPoint2D after = p.pointAtExtended(45);
        assertEquals(10, after.x, 1e-9);
        assertEquals(35, after.y, 1e-9);
        NTxPoint2D inside = p.pointAtExtended(5);
        assertEquals(5, inside.x, 1e-9);
        assertEquals(0, inside.y, 1e-9);
    }
}