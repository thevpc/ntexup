package net.thevpc.ntexup.engine.renderer.htmlstatic;

import net.thevpc.nuts.elem.NArrayElementBuilder;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NElementReader;
import net.thevpc.nuts.elem.NObjectElementBuilder;
import net.thevpc.nuts.io.NCompress;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.io.NPathRenameOptions;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererContext;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererSupplier;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererBase;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxDocumentView;
import net.thevpc.ntexup.api.renderer.NTxPageOrientation;
import net.thevpc.ntexup.lib.paintstream.PaintStreamCodec;
import net.thevpc.ntexup.lib.paintstream.PaintStreamCodec.Frame;
import net.thevpc.ntexup.lib.paintstream.PaintStreamImageAssets;
import net.thevpc.ntexup.lib.paintstream.PaintStreamJson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Exports a document as a static, server-free web bundle: the page paint
 * streams, recorded once in java, plus a canvas player that replays them.
 * <p>
 * The exporter is where the equivalence with the screen renderer comes from.
 * Pages are rendered by the real engine, through the real node renderers, into
 * a {@link RecordingGraphics2D} capture of the java2d call stream — the very
 * primitives the swing view paints. Layout, text shaping, gradients, clipping
 * and the geometry of an animated flow all stay on the java side; the browser
 * only ever replays what java decided. That is also why text ships as glyph
 * outlines rather than as strings: a web page cannot be expected to own the
 * fonts, the shaping or the metrics of the machine that exported the deck.
 * <p>
 * Animation is sampled against a virtual clock (see
 * {@link VirtualPageLifecycle}), so a deck that never stops moving is bounded
 * by {@value #PROP_MAX_SECONDS} and one that settles is cut as soon as two
 * consecutive samples agree.
 * <p>
 * Output is a directory (or a zip, when the target is a file or a stream):
 * <pre>
 *   manifest.json          pages, geometry, frame counts, asset index
 *   pages/page-001.json    defs table + frames (full or patched)
 *   assets/img/&lt;sha&gt;.png    every image painted, content addressed
 *   ntexup-player.js       the bundled player, copied so data and player cannot drift
 *   index.html             standalone viewer
 * </pre>
 * The first three are the data half, written by {@link #writeDataBundle}, and are
 * all an {@code .ntxp} container ships: the last two only exist because a browser
 * needs a document to open.
 */
public class HtmlStaticDocumentRenderer extends NTxDocumentStreamRendererBase implements NTxDocumentStreamRenderer {

    /**
     * Page width in CSS pixels; the height follows the document aspect ratio.
     */
    public static final String PROP_WIDTH = "html-static.width";
    /**
     * When false, every page exports as a single static frame.
     */
    public static final String PROP_ANIMATE = "html-static.animate";
    /**
     * Milliseconds between two animation samples, matching the 40 ms repaint
     * period of the screen renderer.
     */
    public static final String PROP_PERIOD = "html-static.periodMillis";
    /**
     * Hard horizon for the animation of one page, in seconds.
     */
    public static final String PROP_MAX_SECONDS = "html-static.maxSeconds";
    /**
     * Consecutive identical samples required to call a page settled. A page that
     * settles ships its last frame only; a page that repeats ships one cycle.
     */
    public static final String PROP_SETTLE = "html-static.settleFrames";
    /**
     * When false, no defs interning: every operation carries its full payload.
     * Only useful to compare the cost of interning.
     */
    public static final String PROP_DEFS = "html-static.defs";
    /**
     * When true, the shipped JSON is decoded again and replayed against the
     * frames that were baked, failing the export on any difference.
     */
    public static final String PROP_SELFCHECK = "html-static.selfcheck";
    /**
     * When true, JSON is written pretty printed for inspection.
     */
    public static final String PROP_PRETTY = "html-static.pretty";

    /**
     * When true, the whole export is one self-contained index.html that opens
     * straight from the disk: no fetch, and so no web server.
     */
    public static final String PROP_STANDALONE = "html-static.standalone";
    /**
     * When false, the shipped player keeps its comments and indentation.
     */
    public static final String PROP_MINIFY = "html-static.minify";

    private static final String PLAYER_RESOURCE = "ntexup-player.js";

    private final NTxDocumentRendererContext rendererContext = new NTxDocumentRendererContextImpl();

    public HtmlStaticDocumentRenderer(NTxEngine engine, NTxDocumentStreamRendererConfig config) {
        super(engine);
        this.config = config;
    }

    @Override
    public NTxDocumentView renderSupplier(NTxDocumentRendererSupplier documentSupplier) {
        NTxCompiledDocument document = documentSupplier.get(rendererContext);
        Object target = output;
        if (target == null) {
            target = NPath.of("ntexup-html");
        }
        if (target instanceof NPath) {
            NPath path = (NPath) target;
            if (booleanProperty(PROP_STANDALONE, false) && path.name().endsWith(".html")) {
                //the double-click shape is a file, not a folder to point a browser at
                NPath scratch = NPath.ofTempFolder();
                writeBundle(document, scratch);
                path.mkParentDirs().writeBytes(scratch.resolve("index.html").readBytes());
                scratch.deleteTree();
                return null;
            }
            //isFile() is true for a missing path in this nuts build, so the choice is made
            //from isDirectory(), the extension and exists() and never after mkdirs()
            boolean archive;
            if (path.isDirectory()) {
                archive = false;
            } else if (path.name().endsWith(".zip")) {
                archive = true;
            } else {
                archive = path.exists();
            }
            if (archive) {
                NPath folder = NPath.ofTempFolder();
                writeBundle(document, folder);
                NPath zip = path;
                if (!zip.name().endsWith(".zip")) {
                    zip = zip.resolveSibling(NPathRenameOptions.ofExtension("zip"));
                }
                zipBundle(folder, zip);
                folder.deleteTree();
            } else {
                if (!path.exists()) {
                    path.mkdirs();
                }
                if (!path.isDirectory()) {
                    throw new IllegalArgumentException("invalid output path " + path);
                }
                writeBundle(document, path);
            }
        } else if (target instanceof OutputStream) {
            //NCompress refuses a plain OutputStream, so the archive is built in a
            //temp file and copied : the stream then carries exactly the bytes a
            //"-o bundle.zip" export would have written
            NPath folder = NPath.ofTempFolder();
            NPath zip = NPath.ofTempFolder().resolveSibling("bundle.zip");
            try {
                writeBundle(document, folder);
                zipBundle(folder, zip);
                ((OutputStream) target).write(zip.readBytes());
            } catch (IOException ex) {
                throw new IllegalStateException("unable to write the html-static bundle", ex);
            } finally {
                folder.deleteTree();
                zip.deleteTree();
            }
        }
        return null;
    }

    /**
     * Archives a bundle so its files sit at the root of the archive, which is what
     * index.html and the player resolve against when the zip is served or unpacked.
     */
    private void zipBundle(NPath folder, NPath zip) {
        //skipRoot: the bundle lives at the archive root, not under the temp folder name
        NCompress.of().source(folder).skipRoot(true).to(zip).run();
    }

    /**
     * Writes the whole bundle into {@code folder} and returns the manifest.
     */
    public NElement writeBundle(NTxCompiledDocument document, NPath folder) {
        if (booleanProperty(PROP_STANDALONE, false)) {
            //one html file carries the paint streams inline: no sibling files to write
            NElement manifest = bake(document, folder, true);
            folder.resolve("index.html").writeString(standaloneHtml(manifest), StandardCharsets.UTF_8);
            return manifest;
        }
        NElement manifest = writeDataBundle(document, folder);
        copyPlayer(folder);
        folder.resolve("index.html").writeString(indexHtml(), StandardCharsets.UTF_8);
        return manifest;
    }

    /**
     * The data half on its own: {@code manifest.json}, {@code pages/*.json} and
     * {@code assets/img}. No html and no javascript, because a viewer is the one
     * thing a player that already knows how to paint does not need.
     */
    public NElement writeDataBundle(NTxCompiledDocument document, NPath folder) {
        NElement manifest = bake(document, folder, false);
        folder.resolve("manifest.json").writeBytes(toJson(manifest));
        return manifest;
    }

    /**
     * Bake every page and lay the result out in {@code folder}. When
     * {@code inline}, nothing is written beside the manifest: each page payload
     * and every png is attached to the manifest instead, so one json can carry the
     * whole deck into a single html file.
     */
    private NElement bake(NTxCompiledDocument document, NPath folder, boolean inline) {
        NTxDocumentStreamRendererConfig rt = engine.tools().validateDocumentStreamRendererConfig(config);
        int[] points = resolvePageSize(rt);
        int pixelWidth = resolveWidth(points[0]);
        int pixelHeight = (int) Math.round(points[1] * (pixelWidth / (double) points[0]));
        boolean animate = booleanProperty(PROP_ANIMATE, true);
        boolean interning = booleanProperty(PROP_DEFS, true);
        boolean selfcheck = booleanProperty(PROP_SELFCHECK, true);
        long period = Math.max(1, intProperty(PROP_PERIOD, 40));
        long maxMillis = Math.max(1, intProperty(PROP_MAX_SECONDS, 8)) * 1000L;
        int settle = Math.max(2, intProperty(PROP_SETTLE, 2));

        PaintStreamImageAssets assets = new PaintStreamImageAssets();
        HtmlStaticBaker baker = new HtmlStaticBaker(engine, assets);
        List<NTxCompiledPage> pages = document.pages();
        if (!inline) {
            folder.resolve("pages").mkdirs();
        }
        List<NObjectElementBuilder> pageEntries = new ArrayList<>();
        List<NElement> pagePayloads = new ArrayList<>();
        long totalBytes = 0;
        for (int i = 0; i < pages.size(); i++) {
            NTxCompiledPage page = pages.get(i);
            HtmlStaticBaker.Options options = new HtmlStaticBaker.Options(
                    pixelWidth, pixelHeight, animate, period, maxMillis, settle);
            HtmlStaticBaker.Result baked = baker.bake(page, i + 1, options);
            List<Frame> frames = baked.frames();
            PaintStreamCodec codec = new PaintStreamCodec(interning);
            NElement shipped = codec.encodePage(frames);
            byte[] bytes = toJson(shipped);
            if (selfcheck) {
                verify(shipped, frames, i + 1);
            }
            String file = "pages/page-" + String.format("%03d", i + 1) + ".json";
            if (!inline) {
                folder.resolve(file).mkParentDirs().writeBytes(bytes);
            }
            totalBytes += bytes.length;
            NObjectElementBuilder pe = NElement.ofObjectBuilder();
            pe.add("index", NElement.of(i + 1));
            pe.add("file", NElement.of(file));
            pe.add("frames", NElement.of(frames.size()));
            pe.add("durationMs", NElement.of(frames.get(frames.size() - 1).millis()));
            pe.add("loopStartMs", NElement.of(baked.loopStartMillis()));
            pe.add("loopEndMs", NElement.of(baked.loopEndMillis()));
            pe.add("animated", NElement.of(frames.size() > 1));
            pe.add("bytes", NElement.of(bytes.length));
            pageEntries.add(pe);
            pagePayloads.add(shipped);
            engine.log().log(NMsg.ofC("html-static: page %s/%s, %s frame(s), %s bytes, loop %s..%s",
                    i + 1, pages.size(), frames.size(), bytes.length,
                    baked.loopStartMillis(), baked.loopEndMillis()));
        }

        NArrayElementBuilder pageArray = NElement.ofArrayBuilder();
        NObjectElementBuilder manifest = NElement.ofObjectBuilder();
        manifest.add("format", NElement.of(PaintStreamCodec.FORMAT_VERSION));
        manifest.add("width", NElement.of(pixelWidth));
        manifest.add("height", NElement.of(pixelHeight));
        manifest.add("periodMs", NElement.of(period));
        manifest.add("animated", NElement.of(animate));
        manifest.add("bytes", NElement.of(totalBytes));
        Map<String, byte[]> images = assets.pngs();
        if (inline) {
            //one file means no sibling pngs to resolve, so the pixels travel inline
            NObjectElementBuilder inlineAssets = NElement.ofObjectBuilder();
            for (Map.Entry<String, byte[]> entry : images.entrySet()) {
                inlineAssets.add(entry.getKey(), NElement.of(dataUrl(entry.getValue())));
            }
            manifest.add("assets", inlineAssets.build());
            for (int i = 0; i < pageEntries.size(); i++) {
                pageEntries.get(i).add("data", pagePayloads.get(i));
            }
        } else {
            NArrayElementBuilder assetIndex = NElement.ofArrayBuilder();
            if (!images.isEmpty()) {
                folder.resolve("assets/img").mkdirs();
                for (Map.Entry<String, byte[]> entry : images.entrySet()) {
                    folder.resolve("assets/img/" + entry.getKey() + ".png").writeBytes(entry.getValue());
                    assetIndex.add(NElement.of(entry.getKey()));
                }
            }
            manifest.add("imageAssets", assetIndex.build());
        }
        for (NObjectElementBuilder entry : pageEntries) {
            pageArray.add(entry.build());
        }
        manifest.add("pages", pageArray.build());
        return manifest.build();
    }

    /**
     * The shipped pixels as a url the player can hand straight to an Image.
     */
    private static String dataUrl(byte[] png) {
        return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(png);
    }

    private void verify(NElement shipped, List<Frame> frames, int pageIndex) {
        NElement reparsed = NElementReader.ofJson().read(new String(toJson(shipped), StandardCharsets.UTF_8));
        List<List<NElement>> decoded = PaintStreamCodec.decode(reparsed);
        if (decoded.size() != frames.size()) {
            throw new IllegalStateException(NMsg.ofC("html-static selfcheck: page %s shipped %s frames but %s were baked",
                    pageIndex, decoded.size(), frames.size()).toString());
        }
        for (int f = 0; f < frames.size(); f++) {
            List<NElement> expected = frames.get(f).ops();
            List<NElement> actual = decoded.get(f);
            if (expected.size() != actual.size()) {
                throw new IllegalStateException(NMsg.ofC("html-static selfcheck: page %s frame %s has %s ops, expected %s",
                        pageIndex, f, actual.size(), expected.size()).toString());
            }
            for (int k = 0; k < expected.size(); k++) {
                String want = expected.get(k).toString();
                String got = PaintStreamCodec.toRecorderForm(actual.get(k)).toString();
                if (!want.equals(got)) {
                    throw new IllegalStateException(NMsg.ofC("html-static selfcheck: page %s frame %s op %s differs%n  want %s%n  got  %s",
                            pageIndex, f, k, want, got).toString());
                }
            }
        }
    }

    private byte[] toJson(NElement element) {
        return PaintStreamJson.write(element, booleanProperty(PROP_PRETTY, false));
    }

    private void copyPlayer(NPath folder) {
        String source = playerSource();
        if (source == null) {
            return;
        }
        folder.resolve("ntexup-player.js").writeBytes(source.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The player as it ships: comments and indentation gone unless the export was
     * asked to keep them. Null when the resource is missing from the jar.
     */
    private String playerSource() {
        //leading slash: the player ships at the jar root, not beside this class
        try (InputStream is = getClass().getResourceAsStream("/" + PLAYER_RESOURCE)) {
            if (is == null) {
                engine.log().log(NMsg.ofC("html-static: %s is not bundled with this renderer, " +
                        "the exported page will need the player served separately", PLAYER_RESOURCE));
                return null;
            }
            String source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return booleanProperty(PROP_MINIFY, true) ? HtmlStaticJsMinifier.minify(source) : source;
        } catch (IOException ex) {
            throw new IllegalStateException("unable to bundle " + PLAYER_RESOURCE, ex);
        }
    }

    private String indexHtml() {
        return viewer("<script src=\"" + PLAYER_RESOURCE + "\"></script>",
                "NTxPlayer.mount({manifest:\"manifest.json\",canvas:\"ntexup-canvas\",");
    }

    /**
     * One file that carries the player, the paint streams and the pixels, so that
     * opening it from the disk is enough: a page read off the filesystem cannot
     * fetch its own directory, which is all the split bundle needs a server for.
     */
    private String standaloneHtml(NElement manifest) {
        String player = playerSource();
        StringBuilder scripts = new StringBuilder();
        if (player != null) {
            scripts.append("<script>").append(player).append("</script>\n");
        }
        scripts.append("<script>var NTEXUP_EXPORT = ")
                .append(embedJson(toJson(manifest))).append(";</script>");
        return viewer(scripts.toString(), "NTxPlayer.mount({data:NTEXUP_EXPORT,canvas:\"ntexup-canvas\",");
    }

    /**
     * Escapes the one sequence that would end the script element early. A slash is
     * legal after a less-than sign in json, so the payload stays valid json.
     */
    private static String embedJson(byte[] json) {
        return new String(json, StandardCharsets.UTF_8).replace("</", "<\\/");
    }

    private String viewer(String scripts, String mount) {
        List<String> lines = new ArrayList<>();
        lines.add("<!doctype html>");
        lines.add("<html lang=\"en\">");
        lines.add("<head>");
        lines.add("<meta charset=\"utf-8\">");
        lines.add("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">");
        lines.add("<title>ntexup</title>");
        lines.add("<style>html,body{margin:0;height:100%;background:#202327;color:#c8ccd2;");
        lines.add("font:13px/1.4 system-ui,sans-serif;display:grid;grid-template-rows:1fr auto}");
        lines.add("#stage{display:grid;place-items:center;min-height:0}");
        lines.add("canvas{max-width:100%;max-height:100%;background:#fff}");
        lines.add("#bar{display:flex;justify-content:space-between;padding:6px 12px}");
        lines.add("#bar b{font-weight:600}</style>");
        lines.add("</head>");
        lines.add("<body>");
        lines.add("<div id=\"stage\"><canvas id=\"ntexup-canvas\"></canvas></div>");
        lines.add("<div id=\"bar\"><span id=\"ntexup-count\"></span><span>&larr; &rarr; to navigate</span></div>");
        lines.add(scripts);
        lines.add("<script>");
        lines.add("var counter = document.getElementById(\"ntexup-count\");");
        lines.add(mount);
        lines.add("onPage:function(i,total){counter.textContent=(i+1)+\" / \"+total;}});");
        lines.add("</script>");
        lines.add("</body>");
        lines.add("</html>");
        return String.join("\n", lines) + "\n";
    }

    private int resolveWidth(int pointsWide) {
        int width = intProperty(PROP_WIDTH, 1280);
        return width > 0 ? width : pointsWide;
    }

    private int[] resolvePageSize(NTxDocumentStreamRendererConfig rt) {
        int w = rt.getPageWidth() > 0 ? rt.getPageWidth() : 595;
        int h = rt.getPageHeight() > 0 ? rt.getPageHeight() : 842;
        if (rt.getOrientation() == NTxPageOrientation.LANDSCAPE && w < h) {
            int t = w;
            w = h;
            h = t;
        }
        return new int[]{w, h};
    }

    private int intProperty(String name, int defaultValue) {
        Integer value = getProperty(name, Integer.class).orNull();
        return value == null || value <= 0 ? defaultValue : value;
    }

    private boolean booleanProperty(String name, boolean defaultValue) {
        Boolean value = getProperty(name, Boolean.class).orNull();
        return value == null ? defaultValue : value;
    }

    @Override
    public NTxDocumentStreamRenderer renderNode(NTxNode part, OutputStream out) {
        throw new UnsupportedOperationException("not supported yet");
    }

    private class NTxDocumentRendererContextImpl implements NTxDocumentRendererContext {

        public NTxDocumentRendererContextImpl() {
        }
    }
}
