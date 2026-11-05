package net.thevpc.ntexup.test;

import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.engine.renderer.htmlstatic.HtmlStaticDocumentRenderer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the contract of the html-static export: what a bundle must contain, how
 * animation is sampled, and what a target path means. Several of these cases
 * exist because of a bug that was actually hit while building the exporter —
 * nuts reports a missing path as a file, and a target created by the previous
 * run made the next run nest one level deeper.
 */
public class TestHtmlStaticExport {

    /**
     * Two pages: one settles immediately, one keeps moving.
     */
    private static final String DECK = "page{\n"
            + "  rectangle(name:\"a\", at:(10,10), size:(20,10), background-color:white, draw-contour){\n"
            + "    text(\"A\", at:center)\n"
            + "  }\n"
            + "  rectangle(name:\"b\", at:(70,40), size:(20,10), background-color:white, draw-contour){\n"
            + "    text(\"B\", at:center)\n"
            + "  }\n"
            + "  connector(\"a\", \"b\", end-arrow:simple(), label:\"edge\", flow:dots(count:2))\n"
            + "}\n"
            + "page{\n"
            + "  text(\"still\", at:center)\n"
            + "}\n";

    @BeforeAll
    static void init() {
        System.setProperty("java.awt.headless", "true");
        Nuts.require();
    }

    @Test
    public void bundleShipsPagesPlayerAndViewerWithTheDocumentGeometry() throws IOException {
        NPath folder = tempFolder("layout");
        NElement manifest = export(folder, renderer -> {
        });
        assertTrue(folder.resolve("index.html").exists());
        assertTrue(folder.resolve("pages/page-001.json").exists());
        assertTrue(folder.resolve("pages/page-002.json").exists());
        //the player is copied so the data and the player cannot drift apart
        String player = new String(folder.resolve("ntexup-player.js").readBytes(), StandardCharsets.UTF_8);
        assertTrue(player.contains("NTxPlayer"), "the bundled player must expose NTxPlayer");
        String index = new String(folder.resolve("index.html").readBytes(), StandardCharsets.UTF_8);
        assertTrue(index.contains("ntexup-player.js"), "index.html must load the bundled player");
        assertTrue(index.contains("ntexup-canvas"), "index.html must provide the canvas");

        assertEquals(1, longField(manifest, "format"), "format is the wire version the player guards on");
        //the height follows the page aspect ratio, never a second property
        assertEquals(600, longField(manifest, "width"));
        assertEquals(450, longField(manifest, "height"));
    }

    @Test
    public void animatesMovingPagesAndKeepsSettledPagesAtOneFrame() throws IOException {
        NElement animated = export(tempFolder("anim"), r -> {
            r.setProperty(HtmlStaticDocumentRenderer.PROP_PERIOD, 100);
            r.setProperty(HtmlStaticDocumentRenderer.PROP_MAX_SECONDS, 2);
        });
        List<NElement> pages = pagesOf(animated);
        assertTrue(frames(pages.get(0)) > 1,
                "a page with a dots flow must ship more than one frame, got " + frames(pages.get(0)));
        assertEquals(1, frames(pages.get(1)), "a page that never moves must ship a single frame");
        assertTrue(booleanField(pages.get(0), "animated"));
        assertFalse(booleanField(pages.get(1), "animated"));
        //a page sampled to the horizon carries one frame per period
        assertEquals((frames(pages.get(0)) - 1) * 100L, longField(pages.get(0), "durationMs"));
        for (NElement page : pages) {
            long start = longField(page, "loopStartMs");
            long end = longField(page, "loopEndMs");
            assertTrue(start == -1 ? end == -1 : end > start && start >= 0,
                    "a loop is either absent (-1,-1) or a real window, got " + start + ".." + end);
        }
    }

    @Test
    public void animateFalseFreezesEveryPageOnItsFirstFrame() throws IOException {
        NElement frozen = export(tempFolder("static"), r ->
                r.setProperty(HtmlStaticDocumentRenderer.PROP_ANIMATE, Boolean.FALSE));
        for (NElement page : pagesOf(frozen)) {
            assertEquals(1, frames(page), "no-animation exports must ship exactly one frame per page");
        }
        assertFalse(booleanField(frozen, "animated"));
    }

    @Test
    public void interningDefsShrinksTheShippedPayload() throws IOException {
        long interned = manifestBytes(export(tempFolder("defs-on"), r -> {
        }));
        long raw = manifestBytes(export(tempFolder("defs-off"), r ->
                r.setProperty(HtmlStaticDocumentRenderer.PROP_DEFS, Boolean.FALSE)));
        assertTrue(interned < raw,
                "defs interning should pay for itself, got " + interned + " bytes against " + raw);
    }

    @Test
    public void exportingTwiceIntoTheSameFolderOverwritesInsteadOfNesting() {
        //isFile() is true for a missing path in nuts, and the first run creates the
        //directory the second run then mistakes for an enclosing container
        NPath folder = tempFolder("idempotent");
        NTxEngine engine = new DefaultNTxEngine();
        NTxCompiledDocument doc = load(engine);
        NTxDocumentStreamRenderer renderer = engine.newStreamRenderer("html-static").get();
        renderer.setStreamRendererConfig(config());
        renderer.setOutput(folder);
        renderer.render(doc);
        renderer.render(doc);
        assertTrue(folder.isDirectory());
        assertTrue(folder.resolve("index.html").exists());
        assertFalse(folder.parent().resolve(folder.name() + ".zip").exists(),
                "a folder that exists must stay a folder, not be re-read as a file target");
    }

    @Test
    public void zipTargetShipsTheBundleAtTheArchiveRoot() throws IOException {
        NPath zip = tempFolder("zip").resolve("bundle.zip");
        NTxEngine engine = new DefaultNTxEngine();
        NTxCompiledDocument doc = load(engine);
        NTxDocumentStreamRenderer renderer = engine.newStreamRenderer("html-static").get();
        renderer.setStreamRendererConfig(config());
        renderer.setOutput(zip);
        renderer.render(doc);
        List<String> entries = zipNames(zip);
        assertTrue(entries.contains("index.html"), "got " + entries);
        assertTrue(entries.contains("manifest.json"), "got " + entries);
        assertTrue(entries.contains("ntexup-player.js"), "got " + entries);
        for (String entry : entries) {
            assertFalse(entry.startsWith("bundle"), "the archive must not carry its root folder: " + entry);
        }
    }

    @Test
    public void streamTargetProducesAZipBundle() {
        NTxEngine engine = new DefaultNTxEngine();
        NTxCompiledDocument doc = load(engine);
        NTxDocumentStreamRenderer renderer = engine.newStreamRenderer("html-static").get();
        renderer.setStreamRendererConfig(config());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderer.setOutput(out);
        renderer.render(doc);
        byte[] bytes = out.toByteArray();
        assertTrue(bytes.length > 4 && bytes[0] == 'P' && bytes[1] == 'K',
                "a stream target must be the same zip the cli writes, got " + bytes.length + " bytes");
    }

    @Test
    public void standaloneExportIsOneFileThatNeedsNoServer() throws IOException {
        NPath folder = tempFolder("standalone");
        NElement manifest = export(folder, r ->
                r.setProperty(HtmlStaticDocumentRenderer.PROP_STANDALONE, Boolean.TRUE));
        assertTrue(folder.resolve("index.html").exists());
        assertFalse(folder.resolve("manifest.json").exists(), "one file must not also ship the split bundle");
        assertFalse(folder.resolve("pages").exists(), "no page directory to fetch from");
        assertFalse(folder.resolve("ntexup-player.js").exists(), "the player is inlined");

        String html = read(folder.resolve("index.html"));
        assertTrue(html.contains("NTEXUP_EXPORT"), "the export must be embedded");
        assertTrue(html.contains("NTxPlayer.mount({data:NTEXUP_EXPORT"), "and mounted from memory");
        //the player still knows the string "manifest.json" as its server-mode default;
        //what must be gone is any reference the page itself resolves
        assertFalse(html.contains("<script src="), "no external script to load");
        assertFalse(html.contains("manifest:\""), "no manifest url to fetch");
        for (NElement page : pagesOf(manifest)) {
            assertTrue(field(page, "data") != null, "page " + longField(page, "index") + " carries no payload");
        }
        //the payload must not be able to close the script element it lives in
        int from = html.indexOf("var NTEXUP_EXPORT = ");
        int to = html.indexOf(";</script>", from);
        assertTrue(from >= 0 && to > from, "no embedded export object found");
        assertFalse(html.substring(from, to).contains("</"),
                "the embedded json must escape every closing tag");
    }

    @Test
    public void playerShipsMinifiedAndStaysWhole() throws IOException {
        NPath minified = tempFolder("min");
        export(minified, r -> {
        });
        NPath readable = tempFolder("raw");
        export(readable, r -> r.setProperty(HtmlStaticDocumentRenderer.PROP_MINIFY, Boolean.FALSE));
        String small = read(minified.resolve("ntexup-player.js"));
        String big = read(readable.resolve("ntexup-player.js"));
        assertTrue(small.length() * 3 < big.length() * 2,
                "minified should clear two thirds of the player, got " + small.length() + " of " + big.length());
        assertTrue(big.contains("Format contract"), "the readable player keeps its comments");
        assertFalse(small.contains("Format contract"), "the shipped player drops them");
        assertFalse(small.contains("\n\n"), "and the blank lines between blocks");
        //what must survive the stripper
        for (String player : new String[]{small, big}) {
            assertTrue(player.contains("NTxPlayer"), "the global entry point");
            assertTrue(player.contains("/[^/]*$/"), "the pattern that derives the asset base url");
            assertTrue(player.contains("shape.width / 2") || player.contains("shape.width/2"),
                    "division must not be read as a pattern");
        }
    }

    private static NTxDocumentStreamRendererConfig config() {
        return new NTxDocumentStreamRendererConfig()
                .setPageWidth(400)
                .setPageHeight(300)
                .setDpi(72)
                .setShowDate(false)
                .setShowFileName(false)
                .setShowPageNumber(false);
    }

    private static NTxCompiledDocument load(NTxEngine engine) {
        return engine.loadDocument(new ByteArrayInputStream(DECK.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Exports into a folder and returns the manifest. selfcheck is left on: every
     * case here also proves the shipped json decodes back to the baked frames.
     */
    private static NElement export(NPath folder, java.util.function.Consumer<HtmlStaticDocumentRenderer> tune) {
        NTxEngine engine = new DefaultNTxEngine();
        NTxCompiledDocument doc = load(engine);
        HtmlStaticDocumentRenderer renderer = (HtmlStaticDocumentRenderer) engine.newStreamRenderer("html-static").get();
        renderer.setStreamRendererConfig(config());
        renderer.setProperty(HtmlStaticDocumentRenderer.PROP_WIDTH, 600);
        tune.accept(renderer);
        return renderer.writeBundle(doc, folder);
    }

    private static String read(NPath file) throws IOException {
        return new String(file.readBytes(), StandardCharsets.UTF_8);
    }

    private static NPath tempFolder(String tag) {
        try {
            return NPath.of(Files.createTempDirectory("ntexup-html-static-" + tag).toString());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static NElement field(NElement container, String name) {
        return container.asListContainer().flatMap(c -> c.get(name)).orNull();
    }

    private static long longField(NElement container, String name) {
        NElement e = field(container, name);
        return e == null ? -1 : e.asLongValue().orElse(-1L);
    }

    private static boolean booleanField(NElement container, String name) {
        NElement e = field(container, name);
        return e != null && e.asBooleanValue().orElse(false);
    }

    private static List<NElement> pagesOf(NElement manifest) {
        List<NElement> pages = new ArrayList<>();
        NElement array = field(manifest, "pages");
        if (array != null && array.asArray().isPresent()) {
            for (NElement page : array.asArray().get()) {
                pages.add(page);
            }
        }
        assertEquals(2, pages.size(), "the test deck has two pages");
        return pages;
    }

    private static int frames(NElement page) {
        return (int) longField(page, "frames");
    }

    private static long manifestBytes(NElement manifest) {
        return longField(manifest, "bytes");
    }

    private static List<String> zipNames(NPath zip) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zip.readBytes()))) {
            for (ZipEntry e; (e = zin.getNextEntry()) != null; ) {
                names.add(e.getName());
            }
        }
        return names;
    }
}
