package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.log.NTxMsg;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Variable declarations ({@code x = value} and {@code x := value}) must work at any
 * nesting level inside node bodies (inside {@code body{}}, {@code group{}}, {@code col{}},
 * {@code scene3d{}}, ...), not only at file/page top level.
 * <ul>
 *     <li>assignments must never fall through to binary-operator evaluation
 *     (no {@code [WARNING] unsupported operator infix} messages),</li>
 *     <li>the variable must resolve for following siblings/descendants in the same scope,</li>
 *     <li>{@code CTRL_ASSIGN}/{@code CTRL_ASSIGN_DEFAULT} nodes are void: no layout footprint
 *     (they are compiled with {@code layout:none}),</li>
 *     <li>{@code =} and {@code :=} behave identically.</li>
 * </ul>
 * See {@code vars-in-body.ntx}, {@code vars-in-col.ntx} and {@code vars-in-scene3d.ntx}.
 */
public class TestVariablesAnywhere {

    @BeforeAll
    static void init() {
        System.setProperty("java.awt.headless", "true");
        Nuts.require();
    }

    /**
     * The reported bug: {@code x = 10mm; y = 20mm; rectangle(size:(x,y))} inside
     * {@code body { }} of a themed-style component. The rectangle must resolve to
     * (10mm, 20mm) and render alone, with no infix warning.
     */
    @Test
    public void variablesInsideBodyResolveAndOnlyRectangleRenders() throws Exception {
        CapturingLogger logs = new CapturingLogger();
        NTxEngine e = engine(logs);
        NTxCompiledDocument d = load(e, "vars-in-body.ntx");
        assertEquals(1, d.pages().size());
        logs.assertNoUnsupportedOperator();

        NTxCompiledPage p = d.page(0).get();
        NTxNode rect = exactlyOne(p.compiledPage(), "rectangle");
        assertSizeIsMm(rect, 10, 20);

        // only the rectangle renders: a single filled region anchored at the body origin
        BufferedImage img = e.renderImage(p, new NTxNodeRendererConfig(2970, 2100));
        Bounds drawn = drawnBounds(img);
        assertTrue(drawn.count > 5000,
                "the rectangle should be painted, found " + drawn.count + " drawn pixels");
        assertTrue(drawn.minX <= 4 && drawn.minY <= 4,
                "the rectangle should start at the body origin, got " + drawn);
        double fillRatio = drawn.count / (double) ((drawn.maxX - drawn.minX + 1) * (drawn.maxY - drawn.minY + 1));
        assertTrue(fillRatio > 0.5,
                "nothing but the rectangle should render (no stray/ghost node), fill ratio " + fillRatio + " for " + drawn);
        assertTrue(drawn.maxX < img.getWidth() / 2 && drawn.maxY < img.getHeight() / 2,
                "unexpected extra content outside the rectangle: " + drawn);
    }

    /**
     * {@code :=} (assign-default) inside a {@code col} must resolve like {@code =},
     * and the assign nodes must not consume any grid cell (no layout footprint).
     */
    @Test
    public void defaultAssignInsideColResolvesWithoutLayoutFootprint() throws Exception {
        CapturingLogger logs = new CapturingLogger();
        NTxEngine e = engine(logs);
        NTxCompiledDocument d = load(e, "vars-in-col.ntx");
        assertEquals(1, d.pages().size());
        logs.assertNoUnsupportedOperator();

        NTxCompiledPage p = d.page(0).get();
        NTxNode rect = exactlyOne(p.compiledPage(), "rectangle");
        assertSizeIsMm(rect, 14, 6);

        List<NTxNode> assigns = new ArrayList<>();
        assigns.addAll(allOfType(p.compiledPage(), "assign"));
        assigns.addAll(allOfType(p.compiledPage(), "defaultassign"));
        assertEquals(2, assigns.size(), "the two := statements should compile to defaultassign nodes");
        for (NTxNode assign : assigns) {
            NElement layout = assign.getPropertyValue("layout").orNull();
            assertNotNull(layout, "assign nodes must carry a layout property (void), got none: " + assign);
            assertEquals("none", layout.asStringValue().orNull(),
                    "assign nodes must be void (layout:none), got: " + layout);
        }

        // the assigns must not consume a grid row above the rectangle
        BufferedImage img = e.renderImage(p, new NTxNodeRendererConfig(2970, 2100));
        Bounds drawn = drawnBounds(img);
        assertTrue(drawn.count > 5000,
                "the rectangle should be painted, found " + drawn.count + " drawn pixels");
        assertTrue(drawn.minY <= 4,
                "assign nodes must have no layout footprint in col: rectangle pushed to y=" + drawn.minY);
    }

    /**
     * Assignments inside {@code scene3d{ }} (a container with its own compile scope)
     * must resolve for the following siblings.
     */
    @Test
    public void variablesInsideScene3dResolve() throws Exception {
        CapturingLogger logs = new CapturingLogger();
        NTxEngine e = engine(logs);
        NTxCompiledDocument d = load(e, "vars-in-scene3d.ntx");
        assertEquals(1, d.pages().size());
        logs.assertNoUnsupportedOperator();

        NTxCompiledPage p = d.page(0).get();
        NTxNode box = exactlyOne(p.compiledPage(), "box");
        assertSizeIsMm(box, 10, 10, 20);
    }

    // ------------------------------------------------------------------ helpers

    private static NTxEngine engine(NTxLogger log) {
        NTxEngine e = new DefaultNTxEngine();
        e.addLog(log);
        return e;
    }

    private static NTxCompiledDocument load(NTxEngine e, String resource) {
        try (InputStream in = TestVariablesAnywhere.class.getResourceAsStream("/" + resource)) {
            assertNotNull(in, "missing test resource " + resource);
            return e.loadDocument(in);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** Asserts the node's {@code size} property is a tuple of the expected mm values (unit preserved). */
    private static void assertSizeIsMm(NTxNode node, double... expected) {
        NOptional<NTxProp> sizeProp = node.getProperty("size");
        assertTrue(sizeProp.isPresent(), node.type() + " should have a size property");
        NElement size = sizeProp.get().getValue();
        assertTrue(size.isTuple(), node.type() + " size should be a tuple, got: " + size);
        List<NElement> params = size.asTuple().get().params();
        assertEquals(expected.length, params.size(), "unexpected size arity: " + size);
        for (int i = 0; i < expected.length; i++) {
            NElement param = params.get(i);
            assertEquals(expected[i], param.asDoubleValue().get(), 1e-9,
                    "size component " + i + " should be " + expected[i] + ", got: " + param);
            assertTrue(param.toString().endsWith("mm"),
                    "size component " + i + " should keep its mm unit, got: " + param);
        }
    }

    private static NTxNode exactlyOne(NTxNode root, String type) {
        List<NTxNode> all = allOfType(root, type);
        assertEquals(1, all.size(), "expected exactly one '" + type + "' node, got " + all.size());
        return all.get(0);
    }

    private static List<NTxNode> allOfType(NTxNode node, String type) {
        List<NTxNode> found = new ArrayList<>();
        collect(node, type, found);
        return found;
    }

    private static void collect(NTxNode node, String type, List<NTxNode> found) {
        if (type.equals(node.type())) {
            found.add(node);
        }
        for (NTxNode child : node.children()) {
            collect(child, type, found);
        }
    }

    /** Bounding box + count of all non-white (drawn) pixels of a rendered page. */
    private record Bounds(int minX, int minY, int maxX, int maxY, long count) {

        @Override
        public String toString() {
            return "drawn bbox (" + minX + "," + minY + ")-(" + maxX + "," + maxY + ") count=" + count;
        }
    }

    private static Bounds drawnBounds(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = -1, maxY = -1;
        long count = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                if (r > 245 && g > 245 && b > 245) {
                    continue; // page background
                }
                count++;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            }
        }
        return new Bounds(minX, minY, maxX, maxY, count);
    }

    /** Captures engine log messages so tests can assert on warnings. */
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

        void assertNoUnsupportedOperator() {
            assertTrue(messages.stream().noneMatch(m -> m.contains("unsupported operator")),
                    "unexpected infix/unsupported-operator warnings: " + messages);
        }
    }
}
