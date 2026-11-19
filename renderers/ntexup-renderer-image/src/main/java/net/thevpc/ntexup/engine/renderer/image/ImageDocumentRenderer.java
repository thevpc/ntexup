package net.thevpc.ntexup.engine.renderer.image;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.*;
import net.thevpc.nuts.io.NIOException;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NLiteral;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renderer that produces one image file per slide (page).
 */
public class ImageDocumentRenderer extends NTxDocumentStreamRendererBase implements NTxDocumentStreamRenderer {

    public static final String PROP_PAGES = "image.pages";
    public static final String PROP_FORMAT = "image.format";
    /**
     * Boolean : caption every page with its number and source file (handy to map a
     * visual problem back to the .ntx file to edit).
     */
    public static final String PROP_LABELS = "image.labels";
    /** max width (px) of an N-up sheet when no explicit dpi is given */
    private static final int MAX_SHEET_WIDTH = 2400;

    private static final Pattern IMAGE_EXTENSION = Pattern.compile("\\.(png|jpe?g|gif|bmp)$", Pattern.CASE_INSENSITIVE);

    private final NTxDocumentRendererContext rendererContext = new NTxDocumentRendererContextImpl();

    public ImageDocumentRenderer(NTxEngine engine, NTxDocumentStreamRendererConfig config) {
        super(engine);
        this.config = config;
    }

    @Override
    public NTxDocumentView renderSupplier(NTxDocumentRendererSupplier documentSupplier) {
        NTxCompiledDocument document = documentSupplier.get(rendererContext);
        Object outputTarget = output;
        if (outputTarget == null) {
            outputTarget = NPath.of("document.png");
        }
        if (outputTarget instanceof NPath) {
            renderImages(document, (NPath) outputTarget);
        } else if (outputTarget instanceof OutputStream) {
            renderImages(document, (OutputStream) outputTarget);
        }
        return null;
    }

    public void renderStream(NTxCompiledDocument document, OutputStream stream) {
        renderImages(document, stream);
    }

    public void renderImages(NTxCompiledDocument document, OutputStream stream) {
        List<byte[]> images = isComposed() ? renderComposed(document).images : renderImages(document);
        for (byte[] image : images) {
            try {
                stream.write(image);
            } catch (IOException ex) {
                throw new NIOException(ex);
            }
        }
    }

    /**
     * one image per selected page, in the requested format (no sheets, no labels)
     */
    public List<byte[]> renderImages(NTxCompiledDocument document) {
        String format = resolveFormat();
        List<byte[]> all = new ArrayList<>();
        for (byte[] png : renderPagePngs(document)) {
            all.add(toFormat(png, format));
        }
        return all;
    }

    private List<byte[]> renderPagePngs(NTxCompiledDocument document) {
        NTxDocumentStreamRendererConfig rt = engine.tools().validateDocumentStreamRendererConfig(config);
        List<NTxCompiledPage> allPages = document.pages();
        List<NTxCompiledPage> selectedPages = selectPages(allPages);
        if (selectedPages.isEmpty()) {
            return new ArrayList<>();
        }
        int dpi = rt.getDpi() > 0 ? rt.getDpi() : 200;
        float pointsPerInch = 72f;
        int pixelWidth = (int) (resolvePageWidth(rt) * dpi / pointsPerInch);
        int pixelHeight = (int) (resolvePageHeight(rt) * dpi / pointsPerInch);
        NTxNodeRendererConfig renderConfig = new NTxNodeRendererConfig(pixelWidth, pixelHeight)
                .withAnimate(false)
                .withPrint(true);
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
        if (document.hasPendingFutures()) {
            document.awaitFutures();
        }
        List<byte[]> all = new ArrayList<>();
        for (int pi = 1; pi <= maxIndex; pi++) {
            byte[] bytes = engine.renderImageBytes(allPages.get(pi - 1), renderConfig);
            if (wanted.contains(pi)) {
                all.add(bytes);
            }
        }
        if (document.hasPendingFutures()) {
            document.awaitFutures();
        }
        return all;
    }

    public void renderImages(NTxCompiledDocument document, NPath output) {
        List<NTxCompiledPage> selectedPages = selectPages(document.pages());
        String format = resolveFormat();
        int width = Math.max(2, String.valueOf(selectedPages.size()).length());
        String ext = "." + format;
        boolean dirMode = isDirectoryMode(output);
        if (dirMode && !output.exists()) {
            output.mkdirs();
        }
        if (isComposed() && gridSize()[0] * gridSize()[1] > 1) {
            // N-up : sheet-01.png, sheet-02.png ...
            List<byte[]> sheets = renderComposed(document).images;
            int sw = Math.max(2, String.valueOf(sheets.size()).length());
            for (int i = 0; i < sheets.size(); i++) {
                NPath target = resolveSheetOutput(output, i + 1, sheets.size(), ext, sw, dirMode).mkParentDirs();
                try (OutputStream os = target.outputStream()) {
                    os.write(sheets.get(i));
                } catch (IOException ex) {
                    throw new NIOException(ex);
                }
            }
            return;
        }
        List<byte[]> images = isComposed() ? renderComposed(document).images : renderImages(document);
        for (int i = 0; i < selectedPages.size(); i++) {
            int pageIndex = indexOf(document.pages(), selectedPages.get(i));
            NPath target = resolveOutput(output, pageIndex, selectedPages.size(), format, ext, width, dirMode).mkParentDirs();
            NPath parent = target.parent();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (OutputStream os = target.mkParentDirs().outputStream()) {
                os.write(images.get(i));
            } catch (IOException ex) {
                throw new NIOException(ex);
            }
        }
    }

    private static class Composed {
        final List<byte[]> images = new ArrayList<>();
    }

    private boolean labelsEnabled() {
        Object v = getProperty(PROP_LABELS, Object.class).orNull();
        return v instanceof Boolean ? (Boolean) v : v != null && "true".equalsIgnoreCase(String.valueOf(v));
    }

    private int[] gridSize() {
        NTxDocumentStreamRendererConfig rt = engine.tools().validateDocumentStreamRendererConfig(config);
        return new int[]{Math.max(1, rt.getGridX()), Math.max(1, rt.getGridY())};
    }

    /**
     * true when pages are post-processed : several pages per image (--grid) and/or labels
     */
    private boolean isComposed() {
        int[] g = gridSize();
        return g[0] * g[1] > 1 || labelsEnabled();
    }

    /**
     * Lays the selected pages out gx by gy per image (row by row), each optionally
     * captioned "page N · source". With a 1x1 grid this only adds the captions.
     */
    private Composed renderComposed(NTxCompiledDocument document) {
        NTxDocumentStreamRendererConfig rt = engine.tools().validateDocumentStreamRendererConfig(config);
        int[] g = gridSize();
        int gx = g[0];
        int perSheet = g[0] * g[1];
        boolean labels = labelsEnabled();
        String format = resolveFormat();
        List<NTxCompiledPage> allPages = document.pages();
        List<NTxCompiledPage> selectedPages = selectPages(allPages);
        List<byte[]> pngs = renderPagePngs(document);
        Composed result = new Composed();
        for (int start = 0; start < pngs.size(); start += perSheet) {
            int end = Math.min(pngs.size(), start + perSheet);
            List<BufferedImage> pages = new ArrayList<>();
            List<String> captions = new ArrayList<>();
            for (int i = start; i < end; i++) {
                pages.add(readPng(pngs.get(i)));
                NTxCompiledPage page = selectedPages.get(i);
                captions.add(pageCaption(indexOf(allPages, page), page));
            }
            boolean capWidth = perSheet > 1 && rt.getDpi() <= 0;
            BufferedImage sheet = composeSheet(pages, labels ? captions : null, Math.min(gx, pages.size()), capWidth);
            result.images.add(toFormat(writePng(sheet), format));
        }
        return result;
    }

    private static String pageCaption(int pageIndex, NTxCompiledPage page) {
        String src = null;
        try {
            Object s = page.source();
            if (s instanceof net.thevpc.ntexup.api.source.NTxSource) {
                src = ((net.thevpc.ntexup.api.source.NTxSource) s).shortName();
            } else if (s != null) {
                src = String.valueOf(s);
            }
        } catch (RuntimeException ignored) {
            // a caption must never break rendering
        }
        return src == null || src.isEmpty() ? ("page " + pageIndex) : ("page " + pageIndex + " \u00b7 " + src);
    }

    private static BufferedImage composeSheet(List<BufferedImage> pages, List<String> captions, int cols, boolean capWidth) {
        int pw = pages.get(0).getWidth();
        int ph = pages.get(0).getHeight();
        double scale = 1;
        if (capWidth && cols * pw > MAX_SHEET_WIDTH) {
            scale = (double) MAX_SHEET_WIDTH / (cols * pw);
        }
        int cw = Math.max(1, (int) Math.round(pw * scale));
        int ch = Math.max(1, (int) Math.round(ph * scale));
        int rows = (pages.size() + cols - 1) / cols;
        int gap = Math.max(6, cw / 80);
        int fontSize = Math.max(12, Math.min(28, cw / 32));
        int labelH = captions == null ? 0 : fontSize + gap;
        int w = gap + cols * (cw + gap);
        int h = gap + rows * (labelH + ch + gap);
        BufferedImage sheet = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(72, 72, 72));
            g.fillRect(0, 0, w, h);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
            for (int i = 0; i < pages.size(); i++) {
                int x = gap + (i % cols) * (cw + gap);
                int y = gap + (i / cols) * (labelH + ch + gap);
                if (captions != null) {
                    g.setColor(Color.WHITE);
                    g.drawString(captions.get(i), x, y + fontSize - 2);
                }
                g.drawImage(pages.get(i), x, y + labelH, cw, ch, null);
                g.setColor(new Color(160, 160, 160));
                g.drawRect(x - 1, y + labelH - 1, cw + 1, ch + 1);
            }
        } finally {
            g.dispose();
        }
        return sheet;
    }

    private static BufferedImage readPng(byte[] png) {
        try {
            return ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException ex) {
            throw new NIOException(ex);
        }
    }

    private static byte[] writePng(BufferedImage image) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bos);
            return bos.toByteArray();
        } catch (IOException ex) {
            throw new NIOException(ex);
        }
    }

    private NPath resolveSheetOutput(NPath output, int sheetIndex, int totalSheets, String ext, int width, boolean dirMode) {
        if (dirMode) {
            return output.resolve(String.format("sheet-%0" + width + "d" + ext, sheetIndex));
        }
        String name = output.name();
        Matcher m = IMAGE_EXTENSION.matcher(name);
        boolean hasImageExtension = m.find();
        if (totalSheets == 1 && hasImageExtension) {
            return output;
        }
        String base = hasImageExtension ? name.substring(0, m.start()) : name;
        return output.resolveSibling(String.format(base + "-sheet-%0" + width + "d" + ext, sheetIndex));
    }

    private NPath resolveOutput(NPath output, int pageIndex, int totalSelected, String format, String ext, int width, boolean dirMode) {
        if (dirMode) {
            return output.resolve(String.format("page-%0" + width + "d" + ext, pageIndex));
        }
        String name = output.name();
        Matcher m = IMAGE_EXTENSION.matcher(name);
        boolean hasImageExtension = m.find();
        if (totalSelected == 1 && hasImageExtension) {
            return output;
        }
        String base = hasImageExtension ? name.substring(0, m.start()) : name;
        return output.resolveSibling(String.format(base + "-%0" + width + "d" + ext, pageIndex));
    }

    private boolean isDirectoryMode(NPath output) {
        String name = output.toString();
        if (name.endsWith("/") || name.endsWith("\\")) {
            return true;
        }
        return output.isDirectory();
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

    private String resolveFormat() {
        String format = getProperty(PROP_FORMAT, String.class).orNull();
        if (format == null || format.trim().isEmpty()) {
            format = "png";
        }
        format = format.trim().replace(".", "").toLowerCase(Locale.ROOT);
        return format;
    }

    private byte[] toFormat(byte[] pngBytes, String format) {
        if ("png".equalsIgnoreCase(format)) {
            return pngBytes;
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes));
            if (image == null) {
                return pngBytes;
            }
            String imageWriterFormat = "jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format) ? "jpeg" : format;
            if ("jpeg".equals(imageWriterFormat)) {
                BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgb.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                g.drawImage(image, 0, 0, null);
                g.dispose();
                image = rgb;
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            if (!ImageIO.write(image, imageWriterFormat, bos)) {
                return pngBytes;
            }
            return bos.toByteArray();
        } catch (IOException ex) {
            throw new NIOException(ex);
        }
    }

    @Override
    public NTxDocumentStreamRenderer renderNode(NTxNode part, OutputStream out) {
        renderImages(partToDocument(part), out);
        return this;
    }

    private NTxCompiledDocument partToDocument(NTxNode part) {
        throw new UnsupportedOperationException("not supported yet");
    }

    private class NTxDocumentRendererContextImpl implements NTxDocumentRendererContext {

        public NTxDocumentRendererContextImpl() {
        }
    }
}
