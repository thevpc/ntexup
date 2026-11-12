package net.thevpc.ntexup.test.perf;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.io.NPath;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

final class NTxPerfSupport {
    private NTxPerfSupport() {
    }

    static void initialize() {
        System.setProperty("java.awt.headless", "true");
        Nuts.require();
    }

    static NTxEngine newEngine() {
        return new DefaultNTxEngine();
    }

    static List<NTxCompiledPage> loadPages(NTxEngine engine, String deck) {
        NTxCompiledDocument document = engine.loadDocument(NPath.of(deck));
        return document.pages();
    }

    static void render(NTxEngine engine, NTxCompiledPage page, int width, int height, boolean useCache) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            NTxNodeRendererConfig config = new NTxNodeRendererConfig(width, height);
            config.setCapability(NTxRendererContext.CAPABILITY_ANIMATE, true);
            config.setUseCache(useCache);
            engine.renderPage(page, config, graphics, null, null);
        } finally {
            graphics.dispose();
        }
    }
}
