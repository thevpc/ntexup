package net.thevpc.ntexup.test;

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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRegistryDebugDeck {

    @BeforeAll
    static void init() {
        System.setProperty("java.awt.headless", "true");
        Nuts.require();
    }

    @Test
    public void testRegistryDebugRenders() {
        NTxEngine engine = new DefaultNTxEngine();
        NPath path = NPath.of("../ntexup-examples/src/ntexup/examples/connectors/00-registry-debug.ntx");
        if (!path.exists()) {
            path = NPath.of("test/ntexup-examples/src/ntexup/examples/connectors/00-registry-debug.ntx");
        }
        assertTrue(path.exists(), "00-registry-debug.ntx should exist");
        NTxCompiledDocument doc = engine.loadDocument(path);
        assertNotNull(doc);
        assertTrue(doc.pages().size() > 0, "should have at least 1 page");
        NTxCompiledPage page = doc.page(0).get();
        BufferedImage img = engine.renderImage(page, new NTxNodeRendererConfig(1024, 768).withAnimate(false).withPrint(true));
        assertNotNull(img);
        assertTrue(img.getWidth() == 1024);
        assertTrue(img.getHeight() == 768);
    }
}
