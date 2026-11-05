package net.thevpc.ntexup.test;

import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NElementReader;
import net.thevpc.nuts.io.NPath;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.engine.renderer.htmlstatic.HtmlStaticDocumentRenderer;
import net.thevpc.ntexup.engine.renderer.ntxp.NtxpDocumentRenderer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the contract of the {@code .ntxp} container: what a player must find
 * before it unpacks anything, that the bundle inside is the html-folder layout
 * unchanged, and that the descriptor cannot disagree with the manifest beside it.
 * The knob-forwarding case exists because that is the one thing this renderer
 * does by hand: the bundle is baked by the html-static renderer, so a property
 * left out of the forwarding list is silently ignored.
 */
public class TestNtxpContainer {

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
    public void containerShipsTheBundleFlatAtTheArchiveRoot() throws IOException {
        Map<String, byte[]> entries = entriesOf(container(r -> {
        }));
        assertEquals(new TreeSet<>(List.of(
                        "META-INF/ntexup.json",
                        "index.html",
                        "manifest.json",
                        "ntexup-player.js",
                        "pages/page-001.json",
                        "pages/page-002.json")),
                new TreeSet<>(entries.keySet()),
                "the bundle must sit at the root, not under the archive's own folder name");
    }

    @Test
    public void descriptorAnnouncesAPlaybackContainerThatNeedsNoEngine() throws IOException {
        Map<String, byte[]> entries = entriesOf(container(r -> {
        }));
        NElement d = json(entries.get("META-INF/ntexup.json"));
        assertEquals(1, longField(d, "format"), "the archive layout version a player guards on");
        assertEquals("playback", stringField(d, "kind"));
        assertFalse(booleanField(d, "source"), "no .ntx travels inside: a player replays, it does not compile");
        for (String key : List.of("entry", "manifest", "player")) {
            String name = stringField(d, key);
            assertNotNull(name, "the descriptor must name its " + key);
            assertTrue(entries.containsKey(name), key + " points at " + name + ", which is not in the archive");
        }
    }

    @Test
    public void descriptorAgreesWithTheManifestItSitsBeside() throws IOException {
        Map<String, byte[]> entries = entriesOf(container(r -> {
        }));
        NElement d = json(entries.get("META-INF/ntexup.json"));
        NElement manifest = json(entries.get("manifest.json"));
        assertEquals(longField(manifest, "format"), longField(d, "paintStreamFormat"));
        assertEquals(longField(manifest, "width"), longField(d, "width"));
        assertEquals(longField(manifest, "height"), longField(d, "height"));
        assertEquals(longField(manifest, "periodMs"), longField(d, "periodMs"));
        assertEquals(booleanField(manifest, "animated"), booleanField(d, "animated"));
        List<NElement> pages = listOf(field(manifest, "pages"));
        assertEquals(pages.size(), longField(d, "pages"), "a player sizes its buffer from the descriptor");
        //the two-page deck exports at 600x450 for a 400x300pt page
        assertEquals(600, longField(d, "width"));
        assertEquals(450, longField(d, "height"));
    }

    @Test
    public void htmlStaticKnobsReachTheBundleInsideTheContainer() throws IOException {
        Map<String, byte[]> frozen = entriesOf(container(r -> {
            r.setProperty(HtmlStaticDocumentRenderer.PROP_ANIMATE, Boolean.FALSE);
            r.setProperty(HtmlStaticDocumentRenderer.PROP_MINIFY, Boolean.FALSE);
        }));
        NElement manifest = json(frozen.get("manifest.json"));
        assertFalse(booleanField(manifest, "animated"), "animate=false must reach the baker");
        for (NElement page : listOf(field(manifest, "pages"))) {
            assertEquals(1, longField(page, "frames"), "a frozen page ships one frame");
        }
        String player = new String(frozen.get("ntexup-player.js"), StandardCharsets.UTF_8);
        assertTrue(player.contains("Format contract"),
                "minify=false must reach the shipped player");
    }

    @Test
    public void pathAndStreamTargetsWriteTheSameContainer() throws IOException {
        NTxEngine engine = new DefaultNTxEngine();
        NTxCompiledDocument document = load(engine);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderer(engine, r -> {
        }).setOutput(out).render(document);
        NPath file = tempFolder("target").resolve("deck.ntxp");
        renderer(engine, r -> {
        }).setOutput(file).render(document);
        Map<String, byte[]> fromStream = entriesOf(out.toByteArray());
        Map<String, byte[]> fromPath = entriesOf(file.readBytes());
        assertEquals(fromPath.keySet(), fromStream.keySet(), "same entries whichever target is used");
        assertEquals(json(fromPath.get("META-INF/ntexup.json")).toString(),
                json(fromStream.get("META-INF/ntexup.json")).toString(),
                "and the same descriptor");
    }

    private static NtxpDocumentRenderer renderer(NTxEngine engine, Consumer<NtxpDocumentRenderer> tune) {
        NtxpDocumentRenderer renderer = (NtxpDocumentRenderer) engine.newStreamRenderer("ntxp").get();
        renderer.setStreamRendererConfig(new NTxDocumentStreamRendererConfig()
                .setPageWidth(400)
                .setPageHeight(300)
                .setDpi(72)
                .setShowDate(false)
                .setShowFileName(false)
                .setShowPageNumber(false));
        renderer.setProperty(HtmlStaticDocumentRenderer.PROP_WIDTH, 600);
        renderer.setProperty(HtmlStaticDocumentRenderer.PROP_PERIOD, 100);
        renderer.setProperty(HtmlStaticDocumentRenderer.PROP_MAX_SECONDS, 2);
        tune.accept(renderer);
        return renderer;
    }

    private static byte[] container(Consumer<NtxpDocumentRenderer> tune) {
        NTxEngine engine = new DefaultNTxEngine();
        return renderer(engine, tune).buildContainer(load(engine));
    }

    private static NTxCompiledDocument load(NTxEngine engine) {
        return engine.loadDocument(new ByteArrayInputStream(DECK.getBytes(StandardCharsets.UTF_8)));
    }

    private static Map<String, byte[]> entriesOf(byte[] zip) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry e; (e = zin.getNextEntry()) != null; ) {
                if (!e.isDirectory()) {
                    entries.put(e.getName(), zin.readAllBytes());
                }
            }
        }
        return entries;
    }

    private static NElement json(byte[] element) {
        return NElementReader.ofJson().read(new String(element, StandardCharsets.UTF_8));
    }

    private static NElement field(NElement container, String name) {
        return container.asListContainer().flatMap(c -> c.get(name)).orNull();
    }

    private static long longField(NElement container, String name) {
        NElement e = field(container, name);
        return e == null ? Long.MIN_VALUE : e.asLongValue().orElse(Long.MIN_VALUE);
    }

    private static String stringField(NElement container, String name) {
        NElement e = field(container, name);
        return e == null ? null : e.asStringValue().orElse(null);
    }

    private static boolean booleanField(NElement container, String name) {
        NElement e = field(container, name);
        assertNotNull(e, "the descriptor must carry " + name);
        return e.asBooleanValue().orElse(false);
    }

    private static List<NElement> listOf(NElement array) {
        List<NElement> list = new ArrayList<>();
        if (array != null) {
            for (NElement e : array.asArray().get()) {
                list.add(e);
            }
        }
        return list;
    }

    private static NPath tempFolder(String tag) throws IOException {
        return NPath.of(Files.createTempDirectory("ntexup-ntxp-" + tag).toString());
    }
}
