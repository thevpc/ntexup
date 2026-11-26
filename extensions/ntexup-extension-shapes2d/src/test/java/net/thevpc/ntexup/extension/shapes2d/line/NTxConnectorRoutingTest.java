package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NTxConnectorRoutingTest {
    private final List<NTxPoint2D> points = Arrays.asList(
            new NTxPoint2D(0, 0), new NTxPoint2D(40, 0), new NTxPoint2D(40, 40)
    );

    @Test
    void sharpRouteKeepsWaypoints() {
        assertEquals(points, NTxConnectorRouting.route("polyline", points, 12));
    }

    @Test
    void orthogonalRouteUsesHorizontalAndVerticalSegments() {
        List<NTxPoint2D> routed = NTxConnectorRouting.route("orthogonal", Arrays.asList(
                new NTxPoint2D(0, 0), new NTxPoint2D(40, 40), new NTxPoint2D(80, 0)
        ), 12);
        assertEquals(Arrays.asList(
                new NTxPoint2D(0, 0), new NTxPoint2D(40, 0),
                new NTxPoint2D(40, 40), new NTxPoint2D(80, 40),
                new NTxPoint2D(80, 0)
        ), routed);
        assertTrue(horizontalOrVertical(routed), () -> routed.toString());
    }

    @Test
    void roundedRouteTrimsCornersAndPreservesEndpoints() {
        List<NTxPoint2D> routed = NTxConnectorRouting.route("rounded", points, 12);
        assertTrue(routed.size() > points.size());
        assertEquals(points.get(0), routed.get(0));
        assertEquals(points.get(points.size() - 1), routed.get(routed.size() - 1));
        assertFalse(routed.contains(points.get(1)));
        assertEquals(28, routed.get(1).x, 1e-9);
        assertEquals(0, routed.get(1).y, 1e-9);
    }

    @Test
    void roundedRouteHandlesMultipleJunctions() {
        List<NTxPoint2D> routed = NTxConnectorRouting.route("rounded", Arrays.asList(
                new NTxPoint2D(0, 0), new NTxPoint2D(40, 0),
                new NTxPoint2D(40, 40), new NTxPoint2D(80, 40)
        ), 8);
        NTxPoint2D firstEntry = new NTxPoint2D(32, 0);
        NTxPoint2D firstExit = new NTxPoint2D(40, 8);
        NTxPoint2D secondEntry = new NTxPoint2D(40, 32);
        NTxPoint2D secondExit = new NTxPoint2D(48, 40);
        assertPoint(routed, firstEntry);
        assertPoint(routed, firstExit);
        assertPoint(routed, secondEntry);
        assertPoint(routed, secondExit);
        assertFalse(routed.contains(new NTxPoint2D(40, 0)));
        assertFalse(routed.contains(new NTxPoint2D(40, 40)));
        assertEquals(indexOfPoint(routed, secondEntry), indexOfPoint(routed, firstExit) + 1);
        assertEquals(new NTxPoint2D(80, 40), routed.get(routed.size() - 1));
    }

    @Test
    void bendRadiusIsLimitedByAdjacentSegments() {
        List<NTxPoint2D> routed = NTxConnectorRouting.route("rounded", Arrays.asList(
                new NTxPoint2D(0, 0), new NTxPoint2D(4, 0), new NTxPoint2D(4, 8)
        ), 100);
        assertEquals(2, routed.get(1).x, 1e-9);
        assertEquals(0, routed.get(1).y, 1e-9);
    }

    @Test
    void shortSegmentBetweenJunctionsIsSharedByBothFillets() {
        List<NTxPoint2D> routed = NTxConnectorRouting.route("rounded", Arrays.asList(
                new NTxPoint2D(0, 0), new NTxPoint2D(40, 0),
                new NTxPoint2D(40, 4), new NTxPoint2D(80, 4)
        ), 12);
        assertTrue(routed.contains(new NTxPoint2D(38, 0)));
        assertTrue(routed.contains(new NTxPoint2D(40, 2)));
        assertTrue(routed.contains(new NTxPoint2D(42, 4)));
    }

    @Test
    void acuteJunctionUsesTheRequestedRadius() {
        List<NTxPoint2D> routed = NTxConnectorRouting.route("rounded", Arrays.asList(
                new NTxPoint2D(0, 0), new NTxPoint2D(40, 0), new NTxPoint2D(60, 20)
        ), 12);
        assertEquals(40 - 12 * Math.tan(Math.PI / 8), routed.get(1).x, 1e-9);
        assertEquals(0, routed.get(1).y, 1e-9);
    }

    @Test
    void roundedRouteIgnoresRepeatedWaypoints() {
        List<NTxPoint2D> expected = NTxConnectorRouting.route("rounded", points, 12);
        List<NTxPoint2D> routed = NTxConnectorRouting.route("rounded", Arrays.asList(
                new NTxPoint2D(0, 0), new NTxPoint2D(40, 0),
                new NTxPoint2D(40, 0), new NTxPoint2D(40, 40)
        ), 12);
        assertEquals(expected, routed);
    }

    @Test
    void aliasesAndUnsupportedRoutesAreClassified() {
        assertTrue(NTxConnectorRouting.isStraight("auto"));
        assertTrue(NTxConnectorRouting.isStraight("direct"));
        assertTrue(NTxConnectorRouting.isSupported("orthogonal"));
        assertTrue(NTxConnectorRouting.isSupported("curved"));
        assertFalse(NTxConnectorRouting.isSupported("dijkstra"));
    }

    private static void assertPoint(List<NTxPoint2D> routed, NTxPoint2D expected) {
        assertTrue(routed.stream().anyMatch(p -> Math.abs(p.x - expected.x) <= 1e-9
                        && Math.abs(p.y - expected.y) <= 1e-9),
                () -> "routed path should contain " + expected + ", got " + routed);
    }

    private static int indexOfPoint(List<NTxPoint2D> routed, NTxPoint2D expected) {
        for (int i = 0; i < routed.size(); i++) {
            NTxPoint2D actual = routed.get(i);
            if (Math.abs(actual.x - expected.x) <= 1e-9 && Math.abs(actual.y - expected.y) <= 1e-9) {
                return i;
            }
        }
        return -1;
    }

    private static boolean horizontalOrVertical(List<NTxPoint2D> routed) {
        for (int i = 1; i < routed.size(); i++) {
            NTxPoint2D a = routed.get(i - 1);
            NTxPoint2D b = routed.get(i);
            if (Math.abs(a.x - b.x) > 1e-9 && Math.abs(a.y - b.y) > 1e-9) {
                return false;
            }
        }
        return true;
    }
}
