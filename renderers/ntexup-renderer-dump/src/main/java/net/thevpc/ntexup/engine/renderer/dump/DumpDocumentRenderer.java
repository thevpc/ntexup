package net.thevpc.ntexup.engine.renderer.dump;

import net.thevpc.nuts.elem.NArrayElementBuilder;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NElementFormatterStyle;
import net.thevpc.nuts.elem.NElementWriter;
import net.thevpc.nuts.elem.NObjectElementBuilder;
import net.thevpc.nuts.io.NIOException;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NLiteral;
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
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxPageOrientation;
import net.thevpc.ntexup.api.renderer.NTxRenderDump;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Renderer that produces no visual output: it paints every page through a
 * recording graphics and writes the resulting tree of rendered primitives as
 * a single TSON document (one file per compiled document, pages nested).
 * <p>
 * Intended for automated validation: coding agents can diff a deck render
 * between revisions, or inspect exactly which primitives (rects, paths,
 * texts, gradients, images...) a page produces.
 * <p>
 * Two capture levels are supported ({@link #PROP_LEVEL}):
 * <ul>
 *     <li>{@code ntxgraphics} (default): semantic primitives straight from the
 *     ntexup paint API (fill/draw/text/image/2D elements) before they are
 *     flattened by java2d.</li>
 *     <li>{@code graphics2d}: the exact java2d call stream (what the PDF vector
 *     bridge and raster renderers consume).</li>
 * </ul>
 * Painting is always the static (non animated, print-like) pass, same as a PDF
 * page render.
 */
public class DumpDocumentRenderer extends NTxDocumentStreamRendererBase implements NTxDocumentStreamRenderer {

    public static final String PROP_PAGES = "dump.pages";
    public static final String PROP_LEVEL = "dump.level";
    public static final String PROP_WITH_IMAGE = "dump.withImage";
    /**
     * When set to {@code false}, the {@code render-dump} capability is not
     * offered to node renderers, so extensions skip contributing their semantic
     * payloads (model data, etc.). Enabled by default.
     */
    public static final String PROP_INFOS = "dump.infos";

    public static final String LEVEL_NTXGRAPHICS = "ntxgraphics";
    public static final String LEVEL_GRAPHICS2D = "graphics2d";

    private final NTxDocumentRendererContext rendererContext = new NTxDocumentRendererContextImpl();

    public DumpDocumentRenderer(NTxEngine engine, NTxDocumentStreamRendererConfig config) {
        super(engine);
        this.config = config;
    }

    @Override
    public NTxDocumentView renderSupplier(NTxDocumentRendererSupplier documentSupplier) {
        NTxCompiledDocument document = documentSupplier.get(rendererContext);
        NElement dump = renderDump(document);
        Object outputTarget = output;
        if (outputTarget == null) {
            outputTarget = NPath.of("document.tson");
        }
        if (outputTarget instanceof NPath) {
            NElementWriter.ofTson().formatter(NElementFormatterStyle.PRETTY).write(dump, ((NPath) outputTarget).mkParentDirs());
        } else if (outputTarget instanceof OutputStream) {
            NElementWriter.ofTson().formatter(NElementFormatterStyle.PRETTY).write(dump, (OutputStream) outputTarget);
        }
        return null;
    }

    /**
     * Renders the document and returns the complete dump element
     * ({@code dump{...}}) without writing anything.
     */
    public NElement renderDump(NTxCompiledDocument document) {
        NTxDocumentStreamRendererConfig rt = engine.tools().validateDocumentStreamRendererConfig(config);
        String level = resolveLevel();
        boolean withImage = Boolean.TRUE.equals(getProperty(PROP_WITH_IMAGE, Boolean.class).orNull());
        boolean infos = !Boolean.FALSE.equals(getProperty(PROP_INFOS, Boolean.class).orNull());
        DumpSink sink = infos ? new DumpSink() : null;
        List<NTxCompiledPage> allPages = document.pages();
        List<NTxCompiledPage> selectedPages = selectPages(allPages);
        if (document.hasPendingFutures()) {
            document.awaitFutures();
        }
        int dpi = rt.getDpi() > 0 ? rt.getDpi() : 200;
        float pointsPerInch = 72f;
        int pixelWidth = (int) (resolvePageWidth(rt) * dpi / pointsPerInch);
        int pixelHeight = (int) (resolvePageHeight(rt) * dpi / pointsPerInch);
        NTxNodeRendererConfig renderConfig = new NTxNodeRendererConfig(pixelWidth, pixelHeight)
                .withAnimate(false)
                .withPrint(true);
        if (sink != null) {
            //offer the dump sink to node renderers (ctx.renderDump()) so extensions
            //can attach semantic payloads (chart models, diagrams...) to the tree
            renderConfig.setCapability(NTxRenderDump.CAPABILITY, sink);
        }
        Set<Integer> wanted = new HashSet<>();
        int maxIndex = 0;
        for (NTxCompiledPage page : selectedPages) {
            int idx = indexOf(allPages, page);
            if (idx > 0) {
                wanted.add(idx);
                if (idx > maxIndex) {
                    maxIndex = idx;
                }
            }
        }
        NArrayElementBuilder pages = NElement.ofArrayBuilder();
        for (int pi = 1; pi <= maxIndex; pi++) {
            if (!wanted.contains(pi)) {
                continue;
            }
            pages.add(renderPageDump(allPages.get(pi - 1), renderConfig, pixelWidth, pixelHeight, level, withImage, pi, sink));
        }
        if (document.hasPendingFutures()) {
            document.awaitFutures();
        }
        NObjectElementBuilder root = DumpFormat.op("dump");
        root.add("version", NElement.of(1));
        root.add("level", NElement.of(level));
        root.add("width", NElement.of(pixelWidth));
        root.add("height", NElement.of(pixelHeight));
        root.add("pages", pages.build());
        return root.build();
    }

    private NElement renderPageDump(NTxCompiledPage page,
                                    NTxNodeRendererConfig renderConfig,
                                    int pixelWidth, int pixelHeight,
                                    String level, boolean withImage, int index, DumpSink sink) {
        BufferedImage img = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D base = img.createGraphics();
        DumpRecorder recorder = new DumpRecorder();
        if (sink != null) {
            sink.setPageRoot(recorder);
        }
        paintOnce(page, renderConfig, base, recorder, pixelWidth, pixelHeight, level, sink);
        NTxCompiledDocument doc = page.document();
        if (doc.hasPendingFutures()) {
            //mirrors DefaultNTxEngine.renderImage(): late async content forces a second static pass
            doc.awaitFutures();
            page.compiledPage().invalidateRenderCache();
            recorder = new DumpRecorder();
            if (sink != null) {
                sink.setPageRoot(recorder);
            }
            paintOnce(page, renderConfig, base, recorder, pixelWidth, pixelHeight, level, sink);
        }
        base.dispose();
        NObjectElementBuilder ob = DumpFormat.op("page");
        ob.add("index", NElement.of(index));
        ob.add("width", NElement.of(pixelWidth));
        ob.add("height", NElement.of(pixelHeight));
        if (withImage) {
            ob.add("image", DumpFormat.op("png").add("sha256", NElement.of(sha256Of(img))).build());
        }
        ob.add("ops", recorder.opsElement());
        return ob.build();
    }

    private void paintOnce(NTxCompiledPage page, NTxNodeRendererConfig renderConfig, Graphics2D base,
                           DumpRecorder recorder, int pixelWidth, int pixelHeight, String level, DumpSink sink) {
        if (LEVEL_GRAPHICS2D.equals(level)) {
            //the engine wraps this recorder in its NTxGraphics implementation: java2d-level capture
            engine.renderPage(page, renderConfig, new RecordingGraphics2D(base, recorder, pixelWidth, pixelHeight, false, sink), null, null);
        } else {
            //pass a recording NTxGraphics so the engine paints semantic primitives through it
            engine.renderPage(page, renderConfig, new RecordingNTxGraphics(engine.createGraphics(base), recorder, pixelWidth, pixelHeight, sink), null, null);
        }
    }

    private static String sha256Of(BufferedImage img) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", bos);
        } catch (IOException e) {
            throw new NIOException(e);
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bos.toByteArray()));
        } catch (NoSuchAlgorithmException e) {
            throw new NIOException(e);
        }
    }

    private String resolveLevel() {
        String level = getProperty(PROP_LEVEL, String.class).orNull();
        if (level == null || level.trim().isEmpty()) {
            level = LEVEL_NTXGRAPHICS;
        }
        level = level.trim().toLowerCase(Locale.ROOT);
        if (!LEVEL_NTXGRAPHICS.equals(level) && !LEVEL_GRAPHICS2D.equals(level)) {
            engine.log().log(NMsg.ofC("unsupported dump level '%s', falling back to '%s'", level, LEVEL_NTXGRAPHICS));
            level = LEVEL_NTXGRAPHICS;
        }
        return level;
    }

    private int resolvePageWidth(NTxDocumentStreamRendererConfig rt) {
        return resolvePageSize(rt)[0];
    }

    private int resolvePageHeight(NTxDocumentStreamRendererConfig rt) {
        return resolvePageSize(rt)[1];
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

    private List<NTxCompiledPage> selectPages(List<NTxCompiledPage> allPages) {
        Object pagesProperty = getProperty(PROP_PAGES, Object.class).orNull();
        int size = allPages.size();
        List<NTxCompiledPage> selected = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        boolean hasFilter = false;
        if (pagesProperty instanceof List) {
            for (Object o : (List<?>) pagesProperty) {
                int idx = o instanceof Number ? ((Number) o).intValue()
                        : NLiteral.of(String.valueOf(o)).asInt().orElse(-1);
                if (idx < 0) {
                    idx = size + idx + 1;
                }
                if (idx >= 1 && idx <= size && seen.add(idx)) {
                    selected.add(allPages.get(idx - 1));
                    hasFilter = true;
                }
            }
        }
        if (!hasFilter) {
            selected.addAll(allPages);
        }
        return selected;
    }

    private int indexOf(List<NTxCompiledPage> allPages, NTxCompiledPage page) {
        for (int i = 0; i < allPages.size(); i++) {
            if (allPages.get(i) == page) {
                return i + 1;
            }
        }
        return -1;
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
