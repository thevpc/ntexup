package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.log.NTxMsg;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxPlaced;
import net.thevpc.ntexup.api.renderer.NTxRenderSessionListener;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.engine.renderer.dump.DumpDocumentRenderer;
import net.thevpc.ntexup.extension.shapes2d.line.NTxConnectorGeometry;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestConnectorDeck {

    @BeforeAll
    static void init() {
        System.setProperty("java.awt.headless", "true");
        Nuts.require();
    }

    @Test
    public void testConnectorDeckLoadsAndRenders() throws Exception {
        NTxEngine engine = new DefaultNTxEngine();
        NPath path = connectorDeckPath();
        assertTrue(path.exists(), "connector01/main.ntx should exist");

        NTxCompiledDocument doc = engine.loadDocument(path);
        assertNotNull(doc, "doc should compile");
        List<NTxCompiledPage> pages = doc.pages();
        assertTrue(pages.size() >= 4, "Should have at least 4 pages");

        for (int i = 0; i < pages.size(); i++) {
            NTxCompiledPage page = pages.get(i);
            assertNotNull(page, "page " + i + " should exist");

            // Verify connector node has layout: none
            assertConnectorNodesHaveNoLayout(page.compiledPage());

            // Render static image
            BufferedImage img = engine.renderImage(page,
                    new NTxNodeRendererConfig(1024, 768).withAnimate(false).withPrint(true));
            assertNotNull(img, "page " + i + " should render image");
            assertEquals(1024, img.getWidth());
            assertEquals(768, img.getHeight());
            GoldenImages.assertMatches("connector01-p" + (i + 1), img);
        }
    }

    @Test
    public void testConnectorDeckDumpExposesLayoutAndRoute() {
        NTxEngine engine = new DefaultNTxEngine();
        NPath path = connectorDeckPath();
        assertTrue(path.exists(), "connector01/main.ntx should exist");
        NTxCompiledDocument doc = engine.loadDocument(path);

        NElement dump = renderDump(engine, doc, 595, 842);
        String text = dump.toString();

        assertTrue(text.contains("layout"), "dump should expose the geometry registry");
        assertTrue(text.contains("connector"), "dump should expose connector routes");
        assertTrue(text.contains("endpoint"), "connector dump should expose source/target endpoints");
        assertTrue(text.contains("route"), "connector dump should expose the resolved route kind");
        assertTrue(text.contains("points"), "connector dump should expose the resolved points");
        assertTrue(text.contains("flow"), "flow decorations should be dumped");
        assertTrue(text.contains("period"), "flow dump should expose the animation period");
    }

    @Test
    public void testRoundedConnectorRoutesMultipleViasAndDecorations() throws Exception {
        CapturingLogger logs = new CapturingLogger();
        NTxEngine engine = new DefaultNTxEngine();
        engine.addLog(logs);
        String deck = "page{\n"
                + "  rectangle(name:\"a\", at:(10,20), size:(20,20), draw-contour)\n"
                + "  rectangle(name:\"b\", at:(300,160), size:(20,20), draw-contour)\n"
                + "  connector(from:\"a\", to:\"b\", via:((80px,30px),(160px,30px),(160px,120px),(240px,120px)),\n"
                + "            route:\"rounded\", bend-radius:8, end-arrow:simple(),\n"
                + "            label:{text:\"rounded label\", position:50, offset:0},\n"
                + "            flow:dots(count:1, size:3, period:2))\n"
                + "}";
        NTxCompiledDocument doc = engine.loadDocument(
                new ByteArrayInputStream(deck.getBytes(StandardCharsets.UTF_8)));
        BufferedImage img = engine.renderImage(doc.page(0).get(),
                new NTxNodeRendererConfig(400, 300).withAnimate(false).withPrint(true));
        assertNotNull(img);

        NElement dump = renderDump(engine, doc, 400, 300);
        NElement connector = connectorDump(dump);
        assertNotNull(connector, "rounded connector should have a semantic dump: " + dump);
        NElement data = field(connector, "data");
        assertEquals("rounded", stringField(data, "route"));
        List<Point2D.Double> waypoints = points(field(data, "via"));
        assertEquals(4, waypoints.size(), "dump should retain authored waypoints, not sampled arc points");
        List<Point2D.Double> routed = points(field(data, "points"));
        assertTrue(routed.size() > 20, "multiple rounded junctions should sample arc points: " + routed);

        // The automatic source anchor aims at the first via (the top edge of
        // the source box). Later 8-unit corners are trimmed on both adjoining
        // segments rather than keeping the sharp via.
        assertEquals(48.0, routed.get(0).y, 0.002);
        assertTrue(routed.get(0).x > 72 && routed.get(0).x < 112,
                "automatic source anchor should point toward the first via: " + routed.get(0));
        assertPoint(routed, 152, 30);
        assertPoint(routed, 160, 38);
        assertPoint(routed, 160, 112);
        assertPoint(routed, 168, 120);
        assertTrue(routed.stream().anyMatch(p -> Math.abs(p.y - 120) <= 0.002 && p.x > 238 && p.x < 240),
                "acute final junction should trim its incoming segment: " + routed);

        NElement painted = objects(dump).stream()
                .filter(op -> "POLYLINE".equals(stringField(op, "kind")))
                .findFirst().orElse(null);
        assertNotNull(painted, "dump should contain the painted connector polyline");
        List<Point2D.Double> paintedPoints = polylineNodes(stringField(painted, "nodes"));
        assertEquals(routed.size(), paintedPoints.size(), "painting and routing must share their point list");
        for (int i = 0; i < routed.size(); i++) {
            assertEquals(routed.get(i).x, paintedPoints.get(i).x, 0.002);
            assertEquals(routed.get(i).y, paintedPoints.get(i).y, 0.002);
        }

        NElement label = objects(dump).stream()
                .filter(op -> "rounded label".equals(stringField(op, "s")))
                .findFirst().orElse(null);
        assertNotNull(label, "dump should contain the routed connector label");
        assertLabelAnchorOnPath(label, "rounded label", routed, img);

        NElement flow = field(data, "flow");
        assertNotNull(flow, "connector dump should retain its flow configuration");
        assertEquals(1, intField(flow, "count"));
        assertEquals(2.0, doubleField(flow, "period"), 0.001);
        NElement flowHead = objects(dump).stream()
                .filter(op -> {
                    NElement shape = field(op, "shape");
                    return shape != null && field(shape, "x") != null
                            && field(shape, "y") != null && field(shape, "width") != null
                            && field(shape, "height") != null;
                })
                .findFirst().orElse(null);
        assertNotNull(flowHead, "static flow should paint a round head");
        NElement headShape = field(flowHead, "shape");
        Point2D.Double headCenter = new Point2D.Double(
                doubleField(headShape, "x") + doubleField(headShape, "width") / 2,
                doubleField(headShape, "y") + doubleField(headShape, "height") / 2);
        Point2D.Double expectedHead = pointAt(routed, pathLength(routed) / 2);
        assertEquals(expectedHead.x, headCenter.x, 0.002, "flow must follow the routed midpoint");
        assertEquals(expectedHead.y, headCenter.y, 0.002, "flow must follow the routed midpoint");
        assertTrue(logs.messages.stream().noneMatch(m -> m.contains("not supported")),
                "rounded route should be accepted, got: " + logs.messages);
    }

    @Test
    public void testOrthogonalRouteIsAcceptedWithoutUnsupportedWarning() throws Exception {
        CapturingLogger logs = new CapturingLogger();
        NTxEngine engine = new DefaultNTxEngine();
        engine.addLog(logs);
        String deck = "page{\n"
                + "  connector(from:(10,20), to:(70,60), via:((40px,20px)),\n"
                + "            route:\"orthogonal\", end-arrow:simple())\n"
                + "}";
        NTxCompiledDocument doc = engine.loadDocument(
                new ByteArrayInputStream(deck.getBytes(StandardCharsets.UTF_8)));
        BufferedImage img = engine.renderImage(doc.page(0).get(),
                new NTxNodeRendererConfig(400, 300).withAnimate(false).withPrint(true));
        assertNotNull(img);

        NElement dump = renderDump(engine, doc, 400, 300);
        NElement connector = connectorDump(dump);
        assertNotNull(connector, "orthogonal connector should have a semantic dump: " + dump);
        List<Point2D.Double> routed = points(field(field(connector, "data"), "points"));
        assertEquals(4, routed.size(), "orthogonal route should insert two elbows: " + routed);
        for (int i = 1; i < routed.size(); i++) {
            Point2D.Double a = routed.get(i - 1);
            Point2D.Double b = routed.get(i);
            assertTrue(Math.abs(a.x - b.x) < 0.001 || Math.abs(a.y - b.y) < 0.001,
                    "orthogonal segment should be horizontal or vertical: " + a + " -> " + b);
        }
        assertTrue(logs.messages.stream().noneMatch(m -> m.contains("not supported")),
                "orthogonal route should be accepted, got: " + logs.messages);
    }

    @Test
    public void testMultipleViasResolveAgainstNestedParentBounds() {
        NTxEngine engine = new DefaultNTxEngine();
        String deck = "page{\n"
                + "  group(name:\"outer\", at:(25,20), size:(50,50), draw-contour){\n"
                + "    group(name:\"inner\", at:(10,10), size:(80,80), draw-contour){\n"
                + "      connector(from:(10,10), to:(90,90),\n"
                + "                via:((20,20),(80,20)), route:\"sharp\")\n"
                + "    }\n"
                + "  }\n"
                + "}";
        NTxCompiledDocument doc = engine.loadDocument(
                new ByteArrayInputStream(deck.getBytes(StandardCharsets.UTF_8)));

        NElement dump = renderDump(engine, doc, 400, 300);
        NElement inner = objects(dump).stream()
                .filter(op -> "inner".equals(stringField(op, "name")))
                .findFirst().orElse(null);
        assertNotNull(inner, "nested group should be registered: " + dump);
        List<NElement> bounds = field(inner, "bounds").asArray().get().children();
        double x = bounds.get(0).asDoubleValue().get();
        double y = bounds.get(1).asDoubleValue().get();
        double width = bounds.get(2).asDoubleValue().get();
        double height = bounds.get(3).asDoubleValue().get();

        NElement data = field(connectorDump(dump), "data");
        assertNotNull(data, "nested connector should have a semantic dump: " + dump);
        List<Point2D.Double> waypoints = points(field(data, "via"));
        assertEquals(2, waypoints.size());
        assertEquals(x + width * 0.2, waypoints.get(0).x, 0.002);
        assertEquals(y + height * 0.2, waypoints.get(0).y, 0.002);
        assertEquals(x + width * 0.8, waypoints.get(1).x, 0.002);
        assertEquals(y + height * 0.2, waypoints.get(1).y, 0.002);

        List<Point2D.Double> routed = points(field(data, "points"));
        assertEquals(4, routed.size());
        assertPoint(routed, x + width * 0.1, y + height * 0.1);
        assertPoint(routed, x + width * 0.9, y + height * 0.9);
    }

    @Test
    public void testEdgeAndRadialAnchorsExposeResolvedGeometry() {
        NTxEngine engine = new DefaultNTxEngine();
        String deck = "page{\n"
                + "  rectangle(name:\"a\", position:(10,20), origin:top-left, size:(20,20), draw-contour)\n"
                + "  rectangle(name:\"b\", position:(70,20), origin:top-left, size:(20,20), draw-contour)\n"
                + "  connector(from:\"a@right(20)\", to:\"b@radial(270)\", route:\"rounded\")\n"
                + "}";
        NTxCompiledDocument doc = engine.loadDocument(
                new ByteArrayInputStream(deck.getBytes(StandardCharsets.UTF_8)));
        List<NTxPlaced> placed = renderPlaced(engine, doc);
        NTxPlaced a = placed(placed, "a");
        NTxPlaced b = placed(placed, "b");
        NTxPoint2D expectedFrom = NTxConnectorGeometry.edgePoint(
                a.bounds, a.toRoot, "right", 20);
        NTxPoint2D expectedTo = NTxConnectorGeometry.radialPoint(
                b.bounds, b.toRoot, b.outline(), 270);

        NElement dump = renderDump(engine, doc, 400, 300);
        NElement connector = connectorDump(dump);
        assertNotNull(connector, "parametric anchors should produce a connector dump: " + dump);
        List<Point2D.Double> routed = points(field(field(connector, "data"), "points"));
        assertEquals(expectedFrom.x, routed.get(0).x, 0.002);
        assertEquals(expectedFrom.y, routed.get(0).y, 0.002);
        assertEquals(expectedTo.x, routed.get(routed.size() - 1).x, 0.002);
        assertEquals(expectedTo.y, routed.get(routed.size() - 1).y, 0.002);

        NElement data = field(connector, "data");
        NElement from = field(data, "from");
        NElement to = field(data, "to");
        assertEquals("edge", stringField(from, "anchor"));
        assertEquals(20.0, doubleField(from, "anchor-value"), 0.001);
        assertEquals("radial", stringField(to, "anchor"));
        assertEquals(270.0, doubleField(to, "anchor-value"), 0.001);
    }

    @Test
    public void testRadialAnchorSurvivesRotationAndAimsAutomaticOppositeAnchor() {
        NTxEngine engine = new DefaultNTxEngine();
        String deck = "page{\n"
                + "  group(name:\"rotated\", position:(10,10), origin:top-left, size:(80,70), rotate:30){\n"
                + "    rectangle(name:\"a\", position:(5,15), origin:top-left, size:(20,20), draw-contour)\n"
                + "    rectangle(name:\"b\", position:(65,15), origin:top-left, size:(20,20), draw-contour)\n"
                + "    connector(from:\"a@radial(30)\", to:\"b\", end-arrow:simple())\n"
                + "  }\n"
                + "}";
        NTxCompiledDocument doc = engine.loadDocument(
                new ByteArrayInputStream(deck.getBytes(StandardCharsets.UTF_8)));
        List<NTxPlaced> placed = renderPlaced(engine, doc);
        NTxPlaced a = placed(placed, "a");
        NTxPlaced b = placed(placed, "b");
        NTxPoint2D expectedFrom =
                NTxConnectorGeometry.radialPoint(
                        a.bounds, a.toRoot, a.outline(), 30);
        Point2D centerB = b.toRoot.transform(
                new Point2D.Double(b.bounds.centerX(), b.bounds.centerY()), null);
        NTxPoint2D expectedTo =
                NTxConnectorGeometry.intersectOutline(
                        b.outline(), centerB, expectedFrom);

        NElement connector = connectorDump(renderDump(engine, doc, 400, 300));
        assertNotNull(connector);
        List<Point2D.Double> routed = points(field(field(connector, "data"), "points"));
        assertEquals(expectedFrom.x, routed.get(0).x, 0.002);
        assertEquals(expectedFrom.y, routed.get(0).y, 0.002);
        assertEquals(expectedTo.x, routed.get(routed.size() - 1).x, 0.002);
        assertEquals(expectedTo.y, routed.get(routed.size() - 1).y, 0.002);
    }

    @Test
    public void testMalformedParametricAnchorWarnsAndSkipsConnector() {
        CapturingLogger logs = new CapturingLogger();
        NTxEngine engine = new DefaultNTxEngine();
        engine.addLog(logs);
        String deck = "page{\n"
                + "  rectangle(name:\"a\", size:(20,20))\n"
                + "  rectangle(name:\"b\", position:(70,20), size:(20,20))\n"
                + "  connector(from:\"a@right(101)\", to:\"b\")\n"
                + "}";
        NTxCompiledDocument doc = engine.loadDocument(
                new ByteArrayInputStream(deck.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(engine.renderImage(doc.page(0).get(),
                new NTxNodeRendererConfig(400, 300).withAnimate(false).withPrint(true)));
        assertNull(connectorDump(renderDump(engine, doc, 400, 300)),
                "malformed anchors should not paint a connector");
        assertTrue(logs.messages.stream().anyMatch(m -> m.contains("anchor 'right(101)' is invalid")),
                "malformed anchors should warn, got: " + logs.messages);
    }

    @Test
    public void testUnsupportedRouteWarnsAndStillRenders() throws Exception {
        CapturingLogger logs = new CapturingLogger();
        NTxEngine engine = new DefaultNTxEngine();
        engine.addLog(logs);
        String deck = "page{\n"
                + "  rectangle(name:\"a\", at:(10,10), size:(20,10))\n"
                + "  rectangle(name:\"b\", at:(60,40), size:(20,10))\n"
                + "  connector(from:\"a\", to:\"b\", route:\"dijkstra\", end-arrow:simple())\n"
                + "}";
        NTxCompiledDocument doc = engine.loadDocument(
                new ByteArrayInputStream(deck.getBytes(StandardCharsets.UTF_8)));

        BufferedImage img = engine.renderImage(doc.page(0).get(),
                new NTxNodeRendererConfig(400, 300).withAnimate(false).withPrint(true));

        assertNotNull(img);
        assertTrue(logs.messages.stream().anyMatch(m -> m.contains("not supported")),
                "an unsupported connector route should warn, got: " + logs.messages);
    }

    private static List<NTxPlaced> renderPlaced(NTxEngine engine, NTxCompiledDocument doc) {
        List<NTxPlaced> placed = new ArrayList<>();
        NTxNodeRendererConfig config = new NTxNodeRendererConfig(400, 300)
                .withAnimate(false)
                .withPrint(true);
        config.setCapability(NTxRenderSessionListener.CAPABILITY,
                (NTxRenderSessionListener) session -> placed.addAll(session.geometry().placed()));
        assertNotNull(engine.renderImage(doc.page(0).get(), config));
        return placed;
    }

    private static NTxPlaced placed(List<NTxPlaced> placed, String name) {
        NTxPlaced result = placed.stream().filter(p -> name.equals(p.name)).findFirst().orElse(null);
        assertNotNull(result, "missing placed node " + name + " in " + placed);
        return result;
    }

    private static NElement renderDump(NTxEngine engine, NTxCompiledDocument doc, int width, int height) {
        NTxDocumentStreamRendererConfig cfg = new NTxDocumentStreamRendererConfig()
                .setPageWidth(width)
                .setPageHeight(height)
                .setDpi(72);
        return new DumpDocumentRenderer(engine, cfg).renderDump(doc);
    }

    private static NElement field(NElement container, String name) {
        if (container == null || !container.type().isAnyListContainer()) return null;
        return container.toListContainer().flatMap(c -> c.get(name)).orNull();
    }

    private static String stringField(NElement container, String name) {
        NElement value = field(container, name);
        return value == null ? null : value.asStringValue().orNull();
    }

    private static int intField(NElement container, String name) {
        NElement value = field(container, name);
        return value == null ? -1 : value.asIntValue().orElse(-1);
    }

    private static double doubleField(NElement container, String name) {
        NElement value = field(container, name);
        return value == null ? Double.NaN : value.asDoubleValue().orElse(Double.NaN);
    }

    private static NElement connectorDump(NElement root) {
        return objects(root).stream()
                .filter(op -> "connector".equals(stringField(op, "kind")))
                .findFirst().orElse(null);
    }

    private static List<NElement> objects(NElement root) {
        List<NElement> result = new ArrayList<>();
        collectObjects(root, result);
        return result;
    }

    private static void collectObjects(NElement element, List<NElement> result) {
        if (element == null) return;
        if (element.isAnyObject()) {
            result.add(element);
        }
        if (element.isNamedPair()) {
            collectObjects(element.asNamedPair().get().value(), result);
        } else if (element.type().isAnyListContainer()) {
            for (NElement child : element.toListContainer().get().children()) {
                collectObjects(child, result);
            }
        }
    }

    private static List<Point2D.Double> points(NElement array) {
        assertNotNull(array, "point array should be dumped");
        assertTrue(array.isArray(), "expected a point array, got: " + array);
        List<Point2D.Double> result = new ArrayList<>();
        for (NElement point : array.asArray().get()) {
            result.add(point(point));
        }
        return result;
    }

    private static Point2D.Double point(NElement array) {
        assertNotNull(array, "point should be dumped");
        assertTrue(array.isArray() && array.asArray().get().size() == 2,
                "expected an (x,y) point, got: " + array);
        List<NElement> values = array.asArray().get().children();
        return new Point2D.Double(
                values.get(0).asDoubleValue().orElse(Double.NaN),
                values.get(1).asDoubleValue().orElse(Double.NaN));
    }

    private static List<Point2D.Double> polylineNodes(String nodes) {
        assertNotNull(nodes, "painted polyline should expose its nodes");
        List<Point2D.Double> result = new ArrayList<>();
        for (String node : nodes.trim().split("\\s+")) {
            String[] xy = node.split(",");
            assertEquals(2, xy.length, "invalid polyline node: " + node);
            result.add(new Point2D.Double(Double.parseDouble(xy[0]), Double.parseDouble(xy[1])));
        }
        return result;
    }

    private static void assertPoint(List<Point2D.Double> points, double x, double y) {
        assertTrue(points.stream().anyMatch(p -> Math.abs(p.x - x) <= 0.002 && Math.abs(p.y - y) <= 0.002),
                "rounded route should contain (" + x + "," + y + "), got: " + points);
    }

    private static double pathLength(List<Point2D.Double> points) {
        double length = 0;
        for (int i = 1; i < points.size(); i++) {
            length += points.get(i - 1).distance(points.get(i));
        }
        return length;
    }

    private static Point2D.Double pointAt(List<Point2D.Double> points, double distance) {
        if (points.isEmpty()) return new Point2D.Double();
        double remaining = distance;
        for (int i = 1; i < points.size(); i++) {
            Point2D.Double a = points.get(i - 1);
            Point2D.Double b = points.get(i);
            double segment = a.distance(b);
            if (remaining <= segment || i == points.size() - 1) {
                double t = segment == 0 ? 0 : remaining / segment;
                return new Point2D.Double(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
            }
            remaining -= segment;
        }
        return points.get(points.size() - 1);
    }

    private static void assertLabelAnchorOnPath(NElement label, String text,
                                                List<Point2D.Double> routed, BufferedImage image) {
        NElement at = field(label, "at");
        NElement fontElement = field(label, "font");
        assertNotNull(at, "label dump should expose its baseline");
        assertNotNull(fontElement, "label dump should expose its font");
        String style = stringField(fontElement, "style");
        int fontStyle = "bold".equals(style) ? Font.BOLD
                : "italic".equals(style) ? Font.ITALIC
                : "bolditalic".equals(style) ? Font.BOLD | Font.ITALIC : Font.PLAIN;
        Font font = new Font(stringField(fontElement, "family"), fontStyle, 1)
                .deriveFont((float) doubleField(fontElement, "size"));
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            FontMetrics metrics = graphics.getFontMetrics(font);
            Rectangle2D bounds = metrics.getStringBounds(text, graphics);
            Point2D.Double baseline = point(at);
            Point2D.Double anchor = new Point2D.Double(
                    baseline.x + bounds.getWidth() / 2,
                    baseline.y - metrics.getAscent() + bounds.getHeight() / 2);
            Point2D.Double expected = pointAt(routed, pathLength(routed) / 2);
            assertEquals(expected.x, anchor.x, 2.0, "label should use the routed midpoint");
            assertEquals(expected.y, anchor.y, 2.0, "label should use the routed midpoint");
        } finally {
            graphics.dispose();
        }
    }

    private static NPath connectorDeckPath() {
        NPath path = NPath.of("../ntexup-examples/src/ntexup/examples/simple/connector01/main.ntx");
        if (!path.exists()) {
            path = NPath.of("test/ntexup-examples/src/ntexup/examples/simple/connector01/main.ntx");
        }
        return path;
    }

    private void assertConnectorNodesHaveNoLayout(NTxNode node) {
        if (node == null) return;
        if ("connector".equals(node.type())) {
            // connector must have layout none
            assertEquals("none", node.getPropertyValue("layout").flatMap(x -> x.asStringValue()).orElse(""));
        }
        for (NTxNode child : node.children()) {
            assertConnectorNodesHaveNoLayout(child);
        }
    }

    private static class CapturingLogger implements NTxLogger {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void log(NTxMsg msg) {
            messages.add(String.valueOf(msg.message()));
        }

        @Override
        public void log(NMsg message) {
            messages.add(String.valueOf(message));
        }

        @Override
        public void log(NMsg message, NTxSource source) {
            messages.add(String.valueOf(message));
        }
    }
}
