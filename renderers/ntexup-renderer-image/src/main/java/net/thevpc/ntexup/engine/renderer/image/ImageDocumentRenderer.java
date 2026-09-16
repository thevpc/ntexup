package net.thevpc.ntexup.engine.renderer.image;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.*;
import net.thevpc.nuts.io.NIOException;
import net.thevpc.nuts.io.NPath;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renderer that produces one image file per slide (page).
 */
public class ImageDocumentRenderer extends NTxDocumentStreamRendererBase implements NTxDocumentStreamRenderer {

    public static final String PROP_PAGES = "image.pages";
    public static final String PROP_FORMAT = "image.format";

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
        List<byte[]> images = renderImages(document);
        for (byte[] image : images) {
            try {
                stream.write(image);
            } catch (IOException ex) {
                throw new NIOException(ex);
            }
        }
    }

    public List<byte[]> renderImages(NTxCompiledDocument document) {
        NTxDocumentStreamRendererConfig rt = engine.tools().validateDocumentStreamRendererConfig(config);
        String format = resolveFormat();
        List<NTxCompiledPage> allPages = document.pages();
        List<NTxCompiledPage> selectedPages = selectPages(allPages);
        if (selectedPages.isEmpty()) {
            return new ArrayList<>();
        }
        int dpi = rt.getDpi() > 0 ? rt.getDpi() : 200;
        float pointsPerInch = 72f;
        int pixelWidth = (int) (resolvePageWidth(rt) * dpi / pointsPerInch);
        int pixelHeight = (int) (resolvePageHeight(rt) * dpi / pointsPerInch);
        List<byte[]> all = new ArrayList<>();
        for (NTxCompiledPage page : selectedPages) {
            byte[] bytes = engine.renderImageBytes(
                    page,
                    new NTxNodeRendererConfig(pixelWidth, pixelHeight)
                            .withAnimate(false)
                            .withPrint(true)
            );
            bytes = toFormat(bytes, format);
            all.add(bytes);
        }
        return all;
    }

    public void renderImages(NTxCompiledDocument document, NPath output) {
        List<byte[]> images = renderImages(document);
        List<NTxCompiledPage> selectedPages = selectPages(document.pages());
        String format = resolveFormat();
        int width = Math.max(2, String.valueOf(selectedPages.size()).length());
        String ext = "." + format;
        boolean dirMode = isDirectoryMode(output);
        if (dirMode && !output.exists()) {
            output.mkdirs();
        }
        for (int i = 0; i < selectedPages.size(); i++) {
            int pageIndex = indexOf(document.pages(), selectedPages.get(i));
            NPath target = resolveOutput(output, pageIndex, selectedPages.size(), format, ext, width, dirMode);
            NPath parent = target.parent();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (OutputStream os = target.outputStream()) {
                os.write(images.get(i));
            } catch (IOException ex) {
                throw new NIOException(ex);
            }
        }
    }

    private NPath resolveOutput(NPath output, int pageIndex, int totalSelected, String format, String ext, int width, boolean dirMode) {
        if (dirMode) {
            return output.resolve(String.format("page-%0" + width + "d" + ext, pageIndex));
        }
        String name = output.toString();
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
        List<NTxCompiledPage> selected = new ArrayList<>();
        boolean hasFilter = false;
        if (pagesProperty instanceof List) {
            for (Object o : (List<?>) pagesProperty) {
                int idx = o instanceof Number ? ((Number) o).intValue() : Integer.parseInt(String.valueOf(o));
                if (idx >= 1 && idx <= allPages.size()) {
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