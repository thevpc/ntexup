package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.log.NTxMsg;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.engine.renderer.dump.DumpDocumentRenderer;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

        NTxDocumentStreamRendererConfig cfg = new NTxDocumentStreamRendererConfig()
                .setPageWidth(595)
                .setPageHeight(842)
                .setDpi(72);
        NElement dump = new DumpDocumentRenderer(engine, cfg).renderDump(doc);
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
    public void testUnsupportedRouteWarnsAndStillRenders() throws Exception {
        CapturingLogger logs = new CapturingLogger();
        NTxEngine engine = new DefaultNTxEngine();
        engine.addLog(logs);
        String deck = "page{\n"
                + "  rectangle(name:\"a\", at:(10,10), size:(20,10))\n"
                + "  rectangle(name:\"b\", at:(60,40), size:(20,10))\n"
                + "  connector(from:\"a\", to:\"b\", route:\"orthogonal\", end-arrow:simple())\n"
                + "}";
        NTxCompiledDocument doc = engine.loadDocument(
                new ByteArrayInputStream(deck.getBytes(StandardCharsets.UTF_8)));

        BufferedImage img = engine.renderImage(doc.page(0).get(),
                new NTxNodeRendererConfig(400, 300).withAnimate(false).withPrint(true));

        assertNotNull(img);
        assertTrue(logs.messages.stream().anyMatch(m -> m.contains("not supported")),
                "an unsupported connector route should warn, got: " + logs.messages);
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
