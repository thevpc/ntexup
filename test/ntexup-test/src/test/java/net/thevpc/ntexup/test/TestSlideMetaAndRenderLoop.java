package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.engine.NTxSlideMeta;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.io.NPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression tests for the viewer freeze: rendering a page re-executes its
 * assignment nodes; the compiler-generated ones are static and must not notify
 * the dependency graph, otherwise the page currently being rendered gets
 * invalidated and rendered again forever.
 * <p>
 * Also checks that the component call arguments (title/subtitle/chapter) are
 * captured as slide metadata, so tools (search, go-to-slide) no longer mistake
 * the document title for the slide title.
 */
public class TestSlideMetaAndRenderLoop {

    @BeforeAll
    static void init() {
        System.setProperty("java.awt.headless", "true");
        Nuts.require();
    }

    private static NTxCompiledDocument load(NTxEngine e, String name) {
        NPath p = NPath.of(Path.of("src/test/resources", name).toAbsolutePath().toString());
        NTxCompiledDocument d = e.loadDocument(p);
        assertNotNull(d);
        return d;
    }

    @Test
    void renderPassDoesNotInvalidateThePageBeingRendered() {
        NTxEngine e = new DefaultNTxEngine();
        NTxCompiledDocument d = load(e, "vars-in-body.ntx");
        NTxCompiledPage page = d.page(0).get();

        int[] notifications = {0};
        d.dependencyGraph().addPageInvalidationListener(i -> notifications[0]++);

        NTxNodeRendererConfig cfg = new NTxNodeRendererConfig(800, 600).withAnimate(false).withPrint(true);
        e.renderImage(page, cfg);
        int afterFirstRender = notifications[0];

        e.renderImage(page, cfg);
        assertEquals(afterFirstRender, notifications[0],
                "a second render pass must not invalidate any page (assign nodes are static / unchanged)");
    }

    @Test
    void slideMetadataIsCapturedFromTheComponentCall() {
        NTxEngine e = new DefaultNTxEngine();
        NTxCompiledDocument d = load(e, "vars-in-body.ntx");
        NTxCompiledPage page = d.page(0).get();
        page.compiledPage();

        assertEquals("Variables in body", NTxSlideMeta.get(page.rawPage(), NTxSlideMeta.TITLE),
                "the outer component call title must be captured as slide metadata");
    }
}
