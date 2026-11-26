package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import org.junit.jupiter.api.Test;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.*;

public class NTxConnectorGeometryTest {

    @Test
    void clipsAgainstAxisAlignedRectangle() {
        Rectangle2D box = new Rectangle2D.Double(0, 0, 100, 100);
        NTxPoint2D p = NTxConnectorGeometry.intersectOutline(box, new Point2D.Double(50, 50), new NTxPoint2D(200, 50));
        assertEquals(100, p.x, 1e-9);
        assertEquals(50, p.y, 1e-9);
    }

    @Test
    void clipsAgainstRotatedRectangle() {
        NTxBounds2D bounds = NTxBounds2D.ofWidth(0, 0, 100, 50);
        AffineTransform toRoot = AffineTransform.getRotateInstance(Math.PI / 2, 50, 50);
        NTxPoint2D center = NTxConnectorGeometry.center(bounds, toRoot);
        assertEquals(75, center.x, 1e-9);
        assertEquals(50, center.y, 1e-9);

        NTxPoint2D p = NTxConnectorGeometry.intersectOutline(
                toRoot.createTransformedShape(new Rectangle2D.Double(0, 0, 100, 50)),
                new Point2D.Double(center.x, center.y), new NTxPoint2D(200, 50));
        assertEquals(100, p.x, 1e-9);
        assertEquals(50, p.y, 1e-9);
    }

    @Test
    void keepsEndpointThatIsAlreadyInside() {
        Rectangle2D box = new Rectangle2D.Double(0, 0, 100, 100);
        NTxPoint2D p = NTxConnectorGeometry.intersectOutline(box, new Point2D.Double(50, 50), new NTxPoint2D(60, 60));
        assertEquals(60, p.x, 1e-9);
        assertEquals(60, p.y, 1e-9);
    }

    @Test
    void degenerateAndParallelSegmentsHaveNoIntersection() {
        assertNull(NTxConnectorGeometry.segmentIntersection(0, 0, 10, 0, 0, 5, 10, 5));
        assertNull(NTxConnectorGeometry.segmentIntersection(0, 0, 10, 0, 20, 0, 30, 0));
        double[] p = NTxConnectorGeometry.segmentIntersection(0, 0, 10, 10, 0, 10, 10, 0);
        assertNotNull(p);
        assertEquals(5, p[0], 1e-9);
        assertEquals(5, p[1], 1e-9);
    }

    @Test
    void sidePointUsesLocalBoundsThenTransform() {
        NTxBounds2D bounds = NTxBounds2D.ofWidth(0, 0, 10, 20);
        NTxPoint2D br = NTxConnectorGeometry.sidePoint(bounds, new AffineTransform(), "bottom-right");
        assertEquals(10, br.x, 1e-9);
        assertEquals(20, br.y, 1e-9);

        NTxPoint2D top = NTxConnectorGeometry.sidePoint(bounds, new AffineTransform(), "top");
        assertEquals(5, top.x, 1e-9);
        assertEquals(0, top.y, 1e-9);

        NTxPoint2D moved = NTxConnectorGeometry.sidePoint(bounds, AffineTransform.getTranslateInstance(7, 3), "left");
        assertEquals(7, moved.x, 1e-9);
        assertEquals(13, moved.y, 1e-9);
    }

    @Test
    void edgePercentRunsClockwiseFromTopLeft() {
        NTxBounds2D bounds = NTxBounds2D.ofWidth(10, 20, 200, 100);
        AffineTransform identity = new AffineTransform();
        assertPoint(NTxConnectorGeometry.edgePoint(bounds, identity, "top", 25), 60, 20);
        assertPoint(NTxConnectorGeometry.edgePoint(bounds, identity, "right", 20), 210, 40);
        assertPoint(NTxConnectorGeometry.edgePoint(bounds, identity, "bottom", 50), 110, 120);
        assertPoint(NTxConnectorGeometry.edgePoint(bounds, identity, "left", 0), 10, 120);
        assertPoint(NTxConnectorGeometry.edgePoint(bounds, identity, "left", 100), 10, 20);
    }

    @Test
    void edgePercentIsLocalToTheRotatedNode() {
        NTxBounds2D bounds = NTxBounds2D.ofWidth(0, 0, 100, 50);
        AffineTransform rotated = AffineTransform.getRotateInstance(Math.PI / 2, 50, 50);
        assertPoint(NTxConnectorGeometry.edgePoint(bounds, rotated, "right", 20), 90, 100);
        assertPoint(NTxConnectorGeometry.edgePoint(bounds, rotated, "top", 25), 100, 25);
    }

    @Test
    void radialAngleIntersectsTheTransformedOutline() {
        NTxBounds2D square = NTxBounds2D.ofWidth(0, 0, 100, 100);
        Rectangle2D squareOutline = new Rectangle2D.Double(0, 0, 100, 100);
        AffineTransform identity = new AffineTransform();
        assertPoint(NTxConnectorGeometry.radialPoint(square, identity, squareOutline, 0), 100, 50);
        assertPoint(NTxConnectorGeometry.radialPoint(square, identity, squareOutline, 30),
                100, 50 + 50 * Math.tan(Math.toRadians(30)));
        assertPoint(NTxConnectorGeometry.radialPoint(square, identity, squareOutline, 90), 50, 100);
        assertPoint(NTxConnectorGeometry.radialPoint(square, identity, squareOutline, -90), 50, 0);
        assertPoint(NTxConnectorGeometry.radialPoint(square, identity, squareOutline, 450), 50, 100);

        NTxBounds2D wide = NTxBounds2D.ofWidth(0, 0, 100, 50);
        AffineTransform rotated = AffineTransform.getRotateInstance(Math.PI / 2, 50, 50);
        Shape outline = rotated.createTransformedShape(new Rectangle2D.Double(0, 0, 100, 50));
        assertPoint(NTxConnectorGeometry.radialPoint(wide, rotated, outline, 0), 75, 100);
    }

    @Test
    void parsesParametricAnchorsAndRejectsMalformedValues() {
        NTxConnectorGeometry.AnchorSpec edge = NTxConnectorGeometry.parseAnchorSpec(" Right(20) ");
        assertEquals(NTxConnectorGeometry.AnchorKind.EDGE, edge.kind());
        assertEquals("right", edge.side());
        assertEquals(20, edge.value(), 0);

        NTxConnectorGeometry.AnchorSpec radial = NTxConnectorGeometry.parseAnchorSpec("radial(-30)");
        assertEquals(NTxConnectorGeometry.AnchorKind.RADIAL, radial.kind());
        assertEquals(-30, radial.value(), 0);
        assertEquals(NTxConnectorGeometry.AnchorKind.SIDE,
                NTxConnectorGeometry.parseAnchorSpec("legacy-side").kind());

        assertNull(NTxConnectorGeometry.parseAnchorSpec(""));
        assertNull(NTxConnectorGeometry.parseAnchorSpec("right(101)"));
        assertNull(NTxConnectorGeometry.parseAnchorSpec("left(-1)"));
        assertNull(NTxConnectorGeometry.parseAnchorSpec("radial()"));
        assertNull(NTxConnectorGeometry.parseAnchorSpec("radial(NaN)"));
        assertNull(NTxConnectorGeometry.parseAnchorSpec("right(20)extra"));
        assertNull(NTxConnectorGeometry.parseAnchorSpec("unknown(20)"));
    }

    private static void assertPoint(NTxPoint2D point, double x, double y) {
        assertEquals(x, point.x, 1e-9);
        assertEquals(y, point.y, 1e-9);
    }
}
