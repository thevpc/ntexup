package net.thevpc.ntexup.engine.renderer;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxPlaced;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NTxGeometryIndexImplTest {
    @BeforeAll
    static void init() {
        Nuts.require();
    }

    private static NTxNode node(String type, String name) {
        NTxNode n = new DefaultNTxNode(type);
        if (name != null) {
            n.setProperty("name", NElement.ofString(name));
        }
        return n;
    }

    @Test
    void transformComposition() {
        NTxGeometryIndexImpl gi = new NTxGeometryIndexImpl();
        NTxNode root = node("group", "root");
        NTxNode child = node("group", "c");
        gi.enter(root, NTxBounds2D.ofWidth(0, 0, 100, 100), 0, 0, 0, "root");
        // child rotated 90 deg around (50,50)
        gi.enter(child, NTxBounds2D.ofWidth(10, 10, 20, 20), Math.toRadians(90), 50, 50, "c");
        AffineTransform ctr = gi.currentToRoot();
        assertNotNull(ctr);
        assertEquals(0, ctr.getTranslateX(), 1e-9);
        assertEquals(0, ctr.getTranslateY(), 1e-9);

        NTxPlaced p = placedNamed(gi, "c");
        Point2D pt = p.toRoot.transform(new Point2D.Double(10, 10), null);
        // rotate (10,10) by 90 deg around (50,50) -> (90,10)
        assertEquals(90, pt.getX(), 1e-9);
        assertEquals(10, pt.getY(), 1e-9);
        gi.exit(child);
        gi.exit(root);
    }

    @Test
    void scoringPrefersTheNearestCommonAncestor() {
        NTxGeometryIndexImpl gi = new NTxGeometryIndexImpl();
        NTxNode aParent = node("group", "outer");
        NTxNode a = node("group", "x");
        NTxNode bParent = node("group", null);
        NTxNode b = node("group", "x");

        gi.enter(aParent, NTxBounds2D.ofWidth(0, 0, 10, 10), 0, 0, 0, "outer");
        gi.enter(a, NTxBounds2D.ofWidth(0, 0, 10, 10), 0, 0, 0, "x");
        gi.exit(a);
        gi.exit(aParent);

        gi.enter(bParent, NTxBounds2D.ofWidth(0, 0, 10, 10), 0, 0, 0, null);
        gi.enter(b, NTxBounds2D.ofWidth(0, 0, 10, 10), 0, 0, 0, "x");
        gi.exit(b);
        gi.exit(bParent);

        NOptional<NTxPlaced> r = gi.resolve(a, "x");
        assertTrue(r.isPresent());
        assertSame(a, r.get().node);
    }

    @Test
    void dottedReferenceRequiresTheNamedAncestor() {
        NTxGeometryIndexImpl gi = new NTxGeometryIndexImpl();
        NTxNode outer = node("group", "outer");
        NTxNode inner = node("group", "x");
        NTxNode source = node("line", null);
        gi.enter(outer, NTxBounds2D.ofWidth(0, 0, 10, 10), 0, 0, 0, "outer");
        gi.enter(inner, NTxBounds2D.ofWidth(0, 0, 10, 10), 0, 0, 0, "x");
        gi.exit(inner);
        gi.exit(outer);
        gi.enter(source, NTxBounds2D.ofWidth(0, 0, 10, 10), 0, 0, 0, null);

        assertTrue(gi.resolve(source, "outer.x").isPresent());
        assertFalse(gi.resolve(source, "other.x").isPresent());
        // bare name still resolves
        assertTrue(gi.resolve(source, "x").isPresent());
    }

    @Test
    void hiddenNodesStayResolvableButAreFlagged() {
        NTxGeometryIndexImpl gi = new NTxGeometryIndexImpl();
        NTxNode n = node("rectangle", "r");
        gi.enter(n, NTxBounds2D.ofWidth(0, 0, 10, 10), 0, 0, 0, "r");
        gi.markHidden(n);
        NOptional<NTxPlaced> r = gi.resolve(node("line", null), "r");
        assertTrue(r.isPresent());
        assertTrue(r.get().hidden);
        assertTrue(r.get().obstacle);
        gi.exit(n);
    }

    @Test
    void repeatedEnterDoesNotDuplicatePlacements() {
        NTxGeometryIndexImpl gi = new NTxGeometryIndexImpl();
        NTxNode n = node("rectangle", "r");
        gi.enter(n, NTxBounds2D.ofWidth(0, 0, 10, 10), 0, 0, 0, "r");
        gi.exit(n);
        gi.enter(n, NTxBounds2D.ofWidth(0, 0, 10, 10), 0, 0, 0, "r");
        gi.exit(n);
        assertEquals(1, gi.placed().size());
    }

    @Test
    void aLoopedNodeContributesOnePlacementPerIteration() {
        NTxGeometryIndexImpl gi = new NTxGeometryIndexImpl();
        NTxNode n = node("rectangle", "r");
        gi.enter(n, NTxBounds2D.ofWidth(0, 0, 10, 10), 0, 0, 0, "r");
        gi.exit(n);
        gi.enter(n, NTxBounds2D.ofWidth(30, 0, 10, 10), 0, 0, 0, "r");
        gi.exit(n);
        assertEquals(2, gi.placed().size());
    }

    @Test
    void containersAndLinesAreNotObstacles() {
        NTxGeometryIndexImpl gi = new NTxGeometryIndexImpl();
        NTxNode group = node("group", "g");
        NTxNode line = node("line", "l");
        gi.enter(group, NTxBounds2D.ofWidth(0, 0, 10, 10), 0, 0, 0, "g");
        gi.enter(line, NTxBounds2D.ofWidth(0, 0, 10, 10), 0, 0, 0, "l");
        List<NTxPlaced> placed = gi.placed();
        assertTrue(placed.stream().filter(p -> p.name.equals("g")).findFirst().get().container);
        assertFalse(placed.stream().filter(p -> p.name.equals("g")).findFirst().get().obstacle);
        assertFalse(placed.stream().filter(p -> p.name.equals("l")).findFirst().get().obstacle);
    }

    @Test
    void forwardReferenceResolvesAfterTreeRendered() {
        NTxGeometryIndexImpl gi = new NTxGeometryIndexImpl();
        NTxNode connector = node("connector", null);
        NTxNode target = node("rectangle", "target");

        // Connector enters first
        gi.enter(connector, NTxBounds2D.ofWidth(0, 0, 0, 0), 0, 0, 0, null);
        gi.exit(connector);

        // Target enters later (forward reference)
        gi.enter(target, NTxBounds2D.ofWidth(50, 50, 20, 20), 0, 0, 0, "target");
        gi.exit(target);

        // Resolution happens after both have entered
        NOptional<NTxPlaced> res = gi.resolve(connector, "target");
        assertTrue(res.isPresent());
        assertSame(target, res.get().node);
    }

    @Test
    void tiesBrokenByMostRecentlyEntered() {
        NTxGeometryIndexImpl gi = new NTxGeometryIndexImpl();
        NTxNode n1 = node("rectangle", "same");
        NTxNode n2 = node("rectangle", "same");

        gi.enter(n1, NTxBounds2D.ofWidth(10, 10, 10, 10), 0, 0, 0, "same");
        gi.exit(n1);

        gi.enter(n2, NTxBounds2D.ofWidth(20, 20, 10, 10), 0, 0, 0, "same");
        gi.exit(n2);

        NOptional<NTxPlaced> res = gi.resolve(node("line", null), "same");
        assertTrue(res.isPresent());
        assertSame(n2, res.get().node);
    }

    @Test
    void nestedRotationTransformCompositionOutline() {
        NTxGeometryIndexImpl gi = new NTxGeometryIndexImpl();
        NTxNode group = node("group", "grp");
        NTxNode child = node("rectangle", "rect");

        // group rotated 90 deg around (50, 50)
        gi.enter(group, NTxBounds2D.ofWidth(0, 0, 100, 100), Math.toRadians(90), 50, 50, "grp");
        // child in group, rotated 90 deg around (50, 50)
        gi.enter(child, NTxBounds2D.ofWidth(20, 20, 20, 20), Math.toRadians(90), 50, 50, "rect");

        NTxPlaced p = placedNamed(gi, "rect");
        assertNotNull(p);
        java.awt.Shape outline = p.outline();
        assertNotNull(outline);

        // Composition of two 90-degree rotations around (50, 50) is 180 degrees around (50, 50).
        // A point (x, y) rotated 180 around (cx, cy) is (2*cx - x, 2*cy - y).
        // (20, 20) -> (80, 80)
        // (40, 40) -> (60, 60)
        java.awt.geom.Rectangle2D bounds = outline.getBounds2D();
        assertEquals(60, bounds.getMinX(), 1e-6);
        assertEquals(60, bounds.getMinY(), 1e-6);
        assertEquals(20, bounds.getWidth(), 1e-6);
        assertEquals(20, bounds.getHeight(), 1e-6);

        gi.exit(child);
        gi.exit(group);
    }

    private static NTxPlaced placedNamed(NTxGeometryIndexImpl gi, String name) {
        return gi.placed().stream().filter(x -> name.equals(x.name)).findFirst().get();
    }
}
