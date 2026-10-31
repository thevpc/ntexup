package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.io.NPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
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
    public void testConnectorDeckLoadsAndRenders() {
        NTxEngine engine = new DefaultNTxEngine();
        NPath path = NPath.of("../ntexup-examples/src/ntexup/examples/simple/connector01/main.ntx");
        if (!path.exists()) {
            path = NPath.of("test/ntexup-examples/src/ntexup/examples/simple/connector01/main.ntx");
        }
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
        }
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
}
