package net.thevpc.ntexup.engine.renderer.ntxp;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElementBuilder;
import net.thevpc.nuts.io.NCompress;
import net.thevpc.nuts.io.NPath;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererContext;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererSupplier;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererBase;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxDocumentView;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.engine.renderer.htmlstatic.HtmlStaticDocumentRenderer;
import net.thevpc.ntexup.lib.paintstream.PaintStreamJson;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Exports a document as an {@code .ntxp} container: one zip that carries the
 * paint streams of every page, plus a {@value #DESCRIPTOR} entry that says what
 * the archive is before any of it has to be unpacked.
 * <p>
 * The container is data, not a program. Everything that makes a page — geometry,
 * timing and the image assets — comes from
 * {@link HtmlStaticDocumentRenderer#writeDataBundle} and keeps its exact layout,
 * so a swing player, a js loader and {@code ntexup-generate html-folder} all read
 * the same files. What it does not carry is a viewer: an html page and a
 * javascript player would be dead weight in an archive whose whole point is being
 * opened by something that already knows how to paint. What the descriptor adds is
 * the part no bundle states: that this archive is an ntexup container, which format
 * it is and how many pages it holds.
 * <p>
 * Replay only: no {@code .ntx} source travels inside, so a player gets the paint
 * streams and nothing to recompile. That is what lets the java side stay simple
 * while the container is still being proved, and the source flag is the hook a
 * later {@code source: true} variant hangs on.
 */
public class NtxpDocumentRenderer extends NTxDocumentStreamRendererBase implements NTxDocumentStreamRenderer {

    /**
     * Archive layout version, independent of the paint stream format the
     * manifest reports. Bumped only when the descriptor or the file layout moves.
     */
    public static final int CONTAINER_FORMAT = 1;
    /**
     * First entry a player reads: it is what makes an arbitrary zip an ntexup
     * container rather than some other archive.
     */
    public static final String DESCRIPTOR = "META-INF/ntexup.json";
    public static final String MANIFEST = "manifest.json";
    /**
     * Nothing in the container is recompiled, so a player never needs a java
     * engine; {@code source} is the flag a future source-carrying variant flips.
     */
    public static final String KIND = "playback";

    /**
     * The html-static knobs a container export has to pass through, because the
     * bundle is baked by that renderer and not by this one.
     */
    private static final String[] FORWARDED_PROPERTIES = {
            HtmlStaticDocumentRenderer.PROP_WIDTH,
            HtmlStaticDocumentRenderer.PROP_ANIMATE,
            HtmlStaticDocumentRenderer.PROP_PERIOD,
            HtmlStaticDocumentRenderer.PROP_MAX_SECONDS,
            HtmlStaticDocumentRenderer.PROP_SETTLE,
            HtmlStaticDocumentRenderer.PROP_DEFS,
            HtmlStaticDocumentRenderer.PROP_SELFCHECK,
            HtmlStaticDocumentRenderer.PROP_PRETTY,
    };

    private final NTxDocumentRendererContext rendererContext = new NTxDocumentRendererContextImpl();

    public NtxpDocumentRenderer(NTxEngine engine, NTxDocumentStreamRendererConfig config) {
        super(engine);
        this.config = config;
    }

    @Override
    public NTxDocumentView renderSupplier(NTxDocumentRendererSupplier documentSupplier) {
        NTxCompiledDocument document = documentSupplier.get(rendererContext);
        byte[] container = buildContainer(document);
        Object target = output;
        if (target == null) {
            target = NPath.of("ntexup-container.ntxp");
        }
        if (target instanceof NPath) {
            NPath path = (NPath) target;
            if (path.isDirectory()) {
                path = path.resolve("document.ntxp");
            }
            path.mkParentDirs().writeBytes(container);
        } else {
            try {
                ((OutputStream) target).write(container);
            } catch (IOException ex) {
                throw new IllegalStateException("unable to write the ntxp container", ex);
            }
        }
        return null;
    }

    /**
     * The container bytes: paint streams in a folder, descriptor beside them,
     * archive flattened so those files sit at the root.
     * <p>
     * The archive is built in a temp file and returned as bytes because
     * {@link NCompress} refuses a plain {@code OutputStream} and an output name
     * like {@code deck.ntxp} is not an extension it recognises; the caller gets
     * the same bytes either way.
     */
    public byte[] buildContainer(NTxCompiledDocument document) {
        NPath folder = NPath.ofTempFolder();
        NPath zip = NPath.ofTempFolder().resolveSibling("container.zip");
        try {
            NElement manifest = bundleRenderer().writeDataBundle(document, folder);
            folder.resolve(DESCRIPTOR).mkParentDirs().writeBytes(descriptor(manifest, document));
            NCompress.of().source(folder).skipRoot(true).to(zip).run();
            return zip.readBytes();
        } finally {
            folder.deleteTree();
            zip.deleteTree();
        }
    }

    /**
     * Described from the manifest that was just written, never recomputed: page
     * count and geometry cannot disagree with the bundle they describe.
     */
    private byte[] descriptor(NElement manifest, NTxCompiledDocument document) {
        NObjectElementBuilder d = NElement.ofObjectBuilder();
        d.add("format", NElement.of(CONTAINER_FORMAT));
        d.add("kind", NElement.of(KIND));
        d.add("source", NElement.of(false));
        d.add("generator", NElement.of("ntexup-renderer-ntxp"));
        d.add("paintStreamFormat", valueOf(manifest, "format"));
        d.add("manifest", NElement.of(MANIFEST));
        d.add("pages", NElement.of(pageCount(manifest)));
        d.add("width", valueOf(manifest, "width"));
        d.add("height", valueOf(manifest, "height"));
        d.add("periodMs", valueOf(manifest, "periodMs"));
        d.add("animated", valueOf(manifest, "animated"));
        NObjectElementBuilder origin = NElement.ofObjectBuilder();
        origin.add("title", NElement.of(document.title()));
        origin.add("name", NElement.of(documentName(document)));
        d.add("document", origin.build());
        return PaintStreamJson.write(d.build(), true);
    }

    private static NElement valueOf(NElement manifest, String name) {
        return manifest.asListContainer().flatMap(c -> c.get(name)).orNull();
    }

    private static int pageCount(NElement manifest) {
        NElement pages = valueOf(manifest, "pages");
        int count = 0;
        if (pages != null) {
            for (NElement ignored : pages.asArray().get()) {
                count++;
            }
        }
        return count;
    }

    /**
     * The document as its author knows it, file name only: an absolute path from
     * the exporting machine would mean nothing to whoever opens the container and
     * would say where their deck came from.
     */
    private static String documentName(NTxCompiledDocument document) {
        NTxSource source = document.source();
        NPath path = source == null ? null : source.path().orNull();
        return path == null ? "" : path.name();
    }

    private HtmlStaticDocumentRenderer bundleRenderer() {
        HtmlStaticDocumentRenderer renderer = new HtmlStaticDocumentRenderer(engine, config);
        for (String name : FORWARDED_PROPERTIES) {
            Object value = getProperty(name).orNull();
            if (value != null) {
                renderer.setProperty(name, value);
            }
        }
        return renderer;
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
