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

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSampleDecksRender {

    @BeforeAll
    static void init() {
        System.setProperty("java.awt.headless", "true");
        Nuts.require();
    }

    @Test
    public void testSampleDecksRender() throws Exception {
        NTxEngine engine = new DefaultNTxEngine();
        List<String> deckRelatives = List.of(
                "examples/curr",
                "examples/simple/circle/circle01",
                "examples/simple/circle/circle02",
                "examples/simple/rectangle/rectangle01",
                "examples/simple/rectangle/rectangle02",
                "examples/simple/grid01",
                "examples/simple/grid02",
                "examples/simple/ul01",
                "examples/simple/ul02",
                "examples/simple/text01",
                "examples/simple/text02"
        );

        String base = "../ntexup-examples/src/ntexup/";
        if (!NPath.of(base).exists()) {
            base = "test/ntexup-examples/src/ntexup/";
        }

        MessageDigest md5 = MessageDigest.getInstance("SHA-256");
        for (String rel : deckRelatives) {
            NPath p = NPath.of(base).resolve(rel);
            if (!p.exists()) {
                continue;
            }
            NTxCompiledDocument doc = engine.loadDocument(p);
            assertNotNull(doc, "Failed to load " + rel);
            List<NTxCompiledPage> pages = doc.pages();
            assertTrue(pages.size() > 0, "No pages in " + rel);
            for (int i = 0; i < pages.size(); i++) {
                byte[] bytes = engine.renderImageBytes(pages.get(i),
                        new NTxNodeRendererConfig(1024, 768).withAnimate(false).withPrint(true));
                assertNotNull(bytes);
                assertTrue(bytes.length > 0);
            }
        }
    }
}
