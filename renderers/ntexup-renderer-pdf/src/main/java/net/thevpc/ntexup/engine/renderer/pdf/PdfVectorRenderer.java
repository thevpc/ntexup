/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package net.thevpc.ntexup.engine.renderer.pdf;

import de.rototor.pdfbox.graphics2d.IPdfBoxGraphics2DFontTextDrawer.IFontTextDrawerEnv;
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2DFontTextDrawer;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxPageOrientation;
import net.thevpc.nuts.io.NIOException;
import net.thevpc.nuts.text.NMsg;
import org.apache.fontbox.ttf.CmapLookup;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.util.Matrix;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders document pages as real PDF vector/text primitives (selectable text,
 * lightweight output) using a {@link PdfBoxGraphics2D} bridge.
 */
public class PdfVectorRenderer {

    private static final int DESIGN_WIDTH = 1024;

    private PdfVectorRenderer() {
    }

    public static void renderStream(NTxEngine engine, NTxCompiledDocument document,
                                    NTxDocumentStreamRendererConfig config, OutputStream stream) {
        config = engine.tools().validateDocumentStreamRendererConfig(config);
        int gridX = Math.max(1, config.getGridX());
        int gridY = Math.max(1, config.getGridY());
        int imagesPerPage = gridX * gridY;
        float cellMargin = 10f;
        float marginLeft = config.getMarginLeft() >= 0 ? config.getMarginLeft() : 0;
        float marginRight = config.getMarginRight() >= 0 ? config.getMarginRight() : 0;
        float marginTop = config.getMarginTop() >= 0 ? config.getMarginTop() : 0;
        float marginBottom = config.getMarginBottom() >= 0 ? config.getMarginBottom() : 0;

        double[] ps = resolvePageSize(config);
        double pageW = ps[0];
        double pageH = ps[1];
        double usableWidth = pageW - marginLeft - marginRight - cellMargin;
        double usableHeight = pageH - marginTop - marginBottom - cellMargin;
        double cellWidth = (usableWidth - (gridX - 1) * cellMargin) / gridX;
        double cellHeight = (usableHeight - (gridY - 1) * cellMargin) / gridY;
        if (cellWidth <= 0 || cellHeight <= 0) {
            throw new NIOException(NMsg.ofC("invalid PDF cell size %sx%s for grid %sx%s",
                    cellWidth, cellHeight, gridX, gridY));
        }
        int designHeight = (int) Math.round(cellHeight / cellWidth * DESIGN_WIDTH);
        if (designHeight <= 0) {
            designHeight = 768;
        }

        if (document.hasPendingFutures()) {
            document.awaitFutures();
        }

        List<NTxCompiledPage> pages = document.pages();
        try (PDDocument doc = new PDDocument()) {
            SafeFontTextDrawer fontDrawer = new SafeFontTextDrawer();
            try {
                registerSystemFonts(fontDrawer);
                PDPage currentPdfPage = null;
                PDPageContentStream pageContentStream = null;
                int slideCount = 0;
                int pdfPageNumber = 0;
                try {
                    for (NTxCompiledPage page : pages) {
                        int col = slideCount % gridX;
                        int row = (slideCount / gridX) % gridY;
                        if (slideCount % imagesPerPage == 0) {
                            if (pageContentStream != null) {
                                pageContentStream.close();
                            }
                            currentPdfPage = new PDPage(new PDRectangle((float) pageW, (float) pageH));
                            doc.addPage(currentPdfPage);
                            pageContentStream = new PDPageContentStream(doc, currentPdfPage,
                                    PDPageContentStream.AppendMode.APPEND, true);
                            pdfPageNumber++;
                            if (config.isShowPageNumber()) {
                                drawPageNumber(pageContentStream, (float) pageW, (float) pageH,
                                        marginLeft, marginTop, marginRight, pdfPageNumber);
                            }
                        }
                        double x = marginLeft + col * (cellWidth + cellMargin);
                        double yTop = marginTop + row * (cellHeight + cellMargin);
                        double yPdf = pageH - yTop - cellHeight;

                        Object cellContent = renderCellVector(engine, doc, fontDrawer, page,
                                (float) cellWidth, (float) cellHeight, designHeight, config);
                        pageContentStream.saveGraphicsState();
                        pageContentStream.transform(Matrix.getTranslateInstance((float) x, (float) yPdf));
                        if (cellContent instanceof PDFormXObject) {
                            pageContentStream.drawForm((PDFormXObject) cellContent);
                        } else if (cellContent instanceof PDImageXObject) {
                            pageContentStream.drawImage((PDImageXObject) cellContent, 0, 0,
                                    (float) cellWidth, (float) cellHeight);
                        }
                        pageContentStream.restoreGraphicsState();
                        slideCount++;
                    }
                    if (pageContentStream != null) {
                        pageContentStream.close();
                    }
                } catch (IOException ex) {
                    throw new NIOException(ex);
                } finally {
                    if (pageContentStream != null) {
                        try {
                            pageContentStream.close();
                        } catch (IOException ignored) {
                        }
                    }
                }

                if (document.hasPendingFutures()) {
                    document.awaitFutures();
                }
                if (config.isShowDate() || config.isShowFileName()) {
                    renderInfoPage(doc, config, (float) pageW, (float) pageH, pdfPageNumber);
                }

                doc.save(stream);
            } finally {
                fontDrawer.close();
            }
        } catch (java.io.IOException ex) {
            throw new NIOException(ex);
        }
    }

    private static Object renderCellVector(NTxEngine engine, PDDocument doc,
                                           PdfBoxGraphics2DFontTextDrawer fontDrawer,
                                           NTxCompiledPage page,
                                           float cellWidth, float cellHeight, int designHeight,
                                           NTxDocumentStreamRendererConfig config) {
        float scale = cellWidth / DESIGN_WIDTH;
        try {
            PdfBoxGraphics2D g2 = new PdfBoxGraphics2D(doc, cellWidth, cellHeight);
            try {
                g2.setFontTextDrawer(fontDrawer);
                g2.setTransform(AffineTransform.getScaleInstance(scale, scale));
                NTxNodeRendererConfig rconfig = new NTxNodeRendererConfig(DESIGN_WIDTH, designHeight)
                        .withAnimate(false)
                        .withPrint(true);
                engine.renderPage(page, rconfig, g2, null, null);
            } finally {
                g2.dispose();
            }
            return g2.getXFormObject();
        } catch (Throwable t) {
            engine.log().log(NMsg.ofC("pdf vector rendering failed for page %s, falling back to raster: %s",
                    page.index() + 1, t));
            return renderCellRaster(engine, doc, page, cellWidth, cellHeight, config);
        }
    }

    private static PDImageXObject renderCellRaster(NTxEngine engine, PDDocument doc,
                                                   NTxCompiledPage page,
                                                   float cellWidth, float cellHeight,
                                                   NTxDocumentStreamRendererConfig config) {
        int dpi = config.getDpi() > 0 ? config.getDpi() : 200;
        int pixelWidth = (int) (cellWidth * dpi / 72f);
        int pixelHeight = (int) (cellHeight * dpi / 72f);
        byte[] bytes = engine.renderImageBytes(page,
                new NTxNodeRendererConfig(pixelWidth, pixelHeight)
                        .withAnimate(false)
                        .withPrint(true));
        try {
            return PDImageXObject.createFromByteArray(doc, bytes, "page_" + page.index() + ".png");
        } catch (IOException ex) {
            throw new NIOException(ex);
        }
    }

    private static double[] resolvePageSize(NTxDocumentStreamRendererConfig config) {
        double w;
        double h;
        if (config.getPageWidth() > 0 && config.getPageHeight() > 0) {
            w = config.getPageWidth();
            h = config.getPageHeight();
        } else {
            w = 595;
            h = 842;
        }
        if (config.getOrientation() == NTxPageOrientation.LANDSCAPE) {
            return w >= h ? new double[]{w, h} : new double[]{h, w};
        }
        return h >= w ? new double[]{w, h} : new double[]{h, w};
    }

    private static void drawPageNumber(PDPageContentStream cs, float pageW, float pageH,
                                       float marginLeft, float marginTop, float marginRight,
                                       int pageNumber) throws IOException {
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
        cs.newLineAtOffset(pageW - marginRight - 50, pageH - marginTop - 12);
        cs.showText("Page " + pageNumber);
        cs.endText();
    }

    private static void renderInfoPage(PDDocument doc, NTxDocumentStreamRendererConfig config,
                                       float pageW, float pageH, int afterPageNumber) throws IOException {
        PDPage page = new PDPage(new PDRectangle(pageW, pageH));
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page,
                PDPageContentStream.AppendMode.APPEND, true)) {
            float marginLeft = config.getMarginLeft() >= 0 ? config.getMarginLeft() : 0;
            float marginRight = config.getMarginRight() >= 0 ? config.getMarginRight() : 0;
            float marginTop = config.getMarginTop() >= 0 ? config.getMarginTop() : 0;
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
            float y = pageH - marginTop - 12;
            if (config.isShowFileName()) {
                cs.newLineAtOffset(pageW - marginRight - 70, y);
                cs.showText("File: MyDocument.pdf");
                y -= 14;
            }
            if (config.isShowDate()) {
                cs.newLineAtOffset(pageW - marginRight - 100, y - (config.isShowFileName() ? 14 : 0));
                cs.showText("Date: " + new java.util.Date().toString());
            }
            cs.endText();
        }
    }

    private static void registerSystemFonts(SafeFontTextDrawer drawer) {
        String[] dirs = {
                "/usr/share/fonts",
                System.getProperty("user.home") + "/.fonts",
                System.getProperty("user.home") + "/.local/share/fonts",
                "/Library/Fonts",
                "/System/Library/Fonts",
                "/Windows/Fonts",
                System.getenv("WINDIR") != null ? System.getenv("WINDIR") + "\\Fonts" : null,
                System.getProperty("java.home") + "/lib/fonts"
        };
        List<File> all = new java.util.ArrayList<>();
        for (String dirName : dirs) {
            if (dirName == null) {
                continue;
            }
            File dir = new File(dirName);
            if (!dir.isDirectory()) {
                continue;
            }
            collectFonts(dir, all);
        }
        Face sans = new Face();
        Face serif = new Face();
        Face mono = new Face();
        List<File> usable = new ArrayList<>();
        int skipped = 0;
        for (File f : all) {
            if (!fontLoadable(f)) {
                skipped++;
                continue;
            }
            try {
                drawer.registerFont(f);
            } catch (Exception ex) {
                skipped++;
                continue;
            }
            usable.add(f);
            String n = f.getName().toLowerCase(Locale.ROOT);
            Face target = null;
            boolean isMono = matches(n, "mono", "mono.?space", "courier", "consolas");
            if (isMono) {
                target = mono;
            } else if (!isNonRegularVariant(n) && matches(n, "dejavusans", "liberationsans", "notosans", "carlito", "arial", "helvetica", "roboto", "open.?sans", "lato", "ubuntu")) {
                target = sans;
            } else if (!isNonRegularVariant(n) && matches(n, "dejavuserif", "liberationserif", "notoserif", "times", "georgia", "garamond", "palatino", "cambria", "cmr")) {
                target = serif;
            }
            if (target != null) {
                target.put(f, detectVariant(n));
            }
        }
        if (skipped > 0) {
            engineLog("skipped %d unreadable system fonts", skipped);
        }
        drawer.installFallbackFiles(usable);
        registerLogicalFamily(drawer, "SansSerif", sans);
        registerLogicalFamily(drawer, "Dialog", sans);
        registerLogicalFamily(drawer, "DialogInput", sans);
        registerLogicalFamily(drawer, "Serif", serif);
        registerLogicalFamily(drawer, "Monospaced", mono);
        registerJlmAlias(drawer, all, "jlm_cmr10", "cmr10");
        registerJlmAlias(drawer, all, "jlm_cmmi10", "cmmi10");
        registerJlmAlias(drawer, all, "jlm_cmsy10", "cmsy10");
        registerJlmAlias(drawer, all, "jlm_cmex10", "cmex10");
    }

    private static boolean isNonRegularVariant(String n) {
        return matches(n, "mono", "condensed", "light", "thin", "black", "extra", "semibold", "medium", "book");
    }

    private static void registerJlmAlias(PdfBoxGraphics2DFontTextDrawer drawer, List<File> all,
                                         String alias, String stem) {
        for (File f : all) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (n.startsWith(stem) && n.endsWith(".ttf")) {
                fileToName(drawer, alias, f);
                return;
            }
        }
    }

    private static void fileToName(PdfBoxGraphics2DFontTextDrawer drawer, String name, File f) {
        try {
            drawer.registerFont(name, f);
        } catch (Exception ignored) {
        }
    }

    private static void registerLogicalFamily(PdfBoxGraphics2DFontTextDrawer drawer, String logicalName,
                                              Face face) {
        fileToName(drawer, logicalName, face.forStyle(Font.PLAIN));
        fileToName(drawer, logicalName + ".plain", face.forStyle(Font.PLAIN));
        fileToName(drawer, logicalName + ".bold", face.forStyle(Font.BOLD));
        fileToName(drawer, logicalName + ".italic", face.forStyle(Font.ITALIC));
        fileToName(drawer, logicalName + ".bolditalic", face.forStyle(Font.BOLD | Font.ITALIC));
    }

    private static int detectVariant(String n) {
        boolean bold = n.contains("bold");
        boolean italic = n.contains("italic") || n.contains("oblique") || n.contains("slanted");
        if (bold && italic) {
            return Font.BOLD | Font.ITALIC;
        }
        if (bold) {
            return Font.BOLD;
        }
        if (italic) {
            return Font.ITALIC;
        }
        return Font.PLAIN;
    }

    private static class Face {
        private File regular;
        private File bold;
        private File italic;
        private File boldItalic;

        void put(File f, int style) {
            switch (style) {
                case Font.BOLD | Font.ITALIC:
                    if (boldItalic == null) {
                        boldItalic = f;
                    }
                    break;
                case Font.BOLD:
                    if (bold == null) {
                        bold = f;
                    }
                    break;
                case Font.ITALIC:
                    if (italic == null) {
                        italic = f;
                    }
                    break;
                default:
                    if (regular == null) {
                        regular = f;
                    }
            }
        }

        File forStyle(int style) {
            File f = switch (style) {
                case Font.BOLD | Font.ITALIC -> boldItalic != null ? boldItalic : regular;
                case Font.BOLD -> bold != null ? bold : regular;
                case Font.ITALIC -> italic != null ? italic : regular;
                default -> regular;
            };
            return f;
        }
    }

    private static boolean fontLoadable(File f) {
        try (PDDocument probe = new PDDocument()) {
            PDType0Font.load(probe, f);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean matches(String name, String... keys) {
        for (String k : keys) {
            if (k.indexOf('?') >= 0) {
                if (name.matches(".*" + k + ".*")) {
                    return true;
                }
            } else if (name.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private static void registerAlias(PdfBoxGraphics2DFontTextDrawer drawer, String alias, File fontFile) {
        if (fontFile != null) {
            try {
                drawer.registerFont(alias, fontFile);
            } catch (Exception ignored) {
            }
        }
    }

    private static void engineLog(String fmt, Object... args) {
        System.err.println(fmt.formatted(args));
    }

    private static void collectFonts(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                collectFonts(f, out);
            } else {
                String n = f.getName().toLowerCase(Locale.ROOT);
                if (n.endsWith(".ttf") || n.endsWith(".otf")) {
                    out.add(f);
                }
            }
        }
    }

    private static class SafeFontTextDrawer extends PdfBoxGraphics2DFontTextDrawer {
        private final java.util.Set<String> degraded = new java.util.HashSet<>();
        private final java.util.Set<String> warned = new java.util.HashSet<>();
        private final java.util.Set<String> metricsWarned = new java.util.HashSet<>();
        private final java.util.Map<String, FontMetrics> fontMetricsCache = new java.util.HashMap<>();
        private final List<File> usable = new ArrayList<>();
        private final Map<String, Font> fileFonts = new HashMap<>();

        void installFallbackFiles(List<File> files) {
            usable.addAll(files);
        }

        @Override
        public FontMetrics getFontMetrics(Font font, IFontTextDrawerEnv env)
                throws IOException, FontFormatException {
            FontMetrics base = super.getFontMetrics(font, env);
            if (base == null) {
                return null;
            }
            final String kim = font.getFamily() + "." + font.getStyle();
            try {
                final PDFont pdf = mapFont(font, env);
                if (pdf == null) {
                    return base;
                }
                
if (metricsWarned.add(kim)) {
    engineLog("SEAM getFontMetrics: use embedded %s (%s) for %s",
            kim, pdf.getName(), font.getFamily());
}
fontMetricsCache.put(kim, new PdfConsistentFontMetrics(font, pdf, base));
                return fontMetricsCache.get(kim);
            } catch (IOException | FontFormatException e) {
                return base;
            }
        }

        static class PdfConsistentFontMetrics extends FontMetrics {
            private final PDFont pdf;
            private final FontMetrics base;

            PdfConsistentFontMetrics(Font font, PDFont pdf, FontMetrics base) {
                super(font);
                this.pdf = pdf;
                this.base = base;
            }

            private int em(String s) {
                try {
                    return (int) Math.round(pdf.getStringWidth(s) * font.getSize2D() / 1000.0);
                } catch (IOException e) {
                    return base.stringWidth(s);
                }
            }

            @Override public int stringWidth(String str) { return em(str == null ? "" : str); }
            @Override public int charsWidth(char[] data, int off, int len) { return em(new String(data, off, len)); }
            @Override public int charWidth(char ch) { return em(String.valueOf(ch)); }
            @Override public int charWidth(int codePoint) { return em(new String(Character.toChars(codePoint))); }
            @Override public int getAscent() { return base.getAscent(); }
            @Override public int getDescent() { return base.getDescent(); }
            @Override public int getLeading() { return base.getLeading(); }
            @Override public int getHeight() { return base.getHeight(); }
            @Override public int getMaxAscent() { return base.getMaxAscent(); }
            @Override public int getMaxDescent() { return base.getMaxDescent(); }
            @Override public int getMaxAdvance() { return base.getMaxAdvance(); }
            @Override
            public java.awt.geom.Rectangle2D getStringBounds(String str, java.awt.Graphics context) {
                int w = em(str == null ? "" : str);
                return new java.awt.geom.Rectangle2D.Float(0, -getAscent(), w, getHeight());
            }
        }

        @Override
        public void drawText(AttributedCharacterIterator iterator, IFontTextDrawerEnv env)
                throws IOException, FontFormatException {
            StringBuilder all = new StringBuilder();
            List<Object[]> runs = new ArrayList<>();
            int idx = iterator.getBeginIndex();
            int end = iterator.getEndIndex();
            while (idx < end) {
                iterator.setIndex(idx);
                int runEnd = iterator.getRunLimit();
                Map<AttributedCharacterIterator.Attribute, Object> attrs = iterator.getAttributes();
                Font runFont = (Font) attrs.get(TextAttribute.FONT);
                if (runFont == null) {
                    runFont = env.getFont();
                }
                String part = collectRun(iterator, runEnd);
                Font finalFont = runFont;
                PDFont pdf = mapFont(runFont, env);
                if (pdf != null && containsMissingGlyph(pdf, part)) {
                    Font alt = findCoveringFont(part, runFont, env);
                    if (alt != null) {
                        finalFont = alt;
                    } else {
                        part = substituteMissing(part, pdf);
                    }
                }
                int s = all.length();
                all.append(part);
                runs.add(new Object[]{s, s + part.length(), finalFont, attrs});
                idx = runEnd;
            }
            if (all.length() == 0) {
                return;
            }
            AttributedString as = new AttributedString(all.toString());
            for (Object[] run : runs) {
                int s = (Integer) run[0];
                int e = (Integer) run[1];
                Font font = (Font) run[2];
                @SuppressWarnings("unchecked")
                Map<AttributedCharacterIterator.Attribute, Object> attrs =
                        (Map<AttributedCharacterIterator.Attribute, Object>) run[3];
                for (Map.Entry<AttributedCharacterIterator.Attribute, Object> en : attrs.entrySet()) {
                    if (en.getKey() != TextAttribute.FONT) {
                        as.addAttribute(en.getKey(), en.getValue(), s, e);
                    }
                }
                as.addAttribute(TextAttribute.FONT, font, s, e);
            }
            try {
                super.drawText(as.getIterator(), env);
            } catch (Throwable t) {
                repairOpenTextBlock(env);
                String key = all.length() <= 64 ? all.toString() : shortText(all.toString());
                if (warned.add(key)) {
                    engineLog("text block failed (%s), block skipped from vector output",
                            t.getClass().getSimpleName() + ": " + t.getMessage());
                }
            }
        }

        private Font findCoveringFont(String text, Font orig, IFontTextDrawerEnv env)
                throws IOException, FontFormatException {
            int[] cps = text.codePoints().toArray();
            float size = orig.getSize2D();
            for (File f : usable) {
                Font awt = fontFor(f);
                if (awt == null || awt.getFontName().equals(orig.getFontName())) {
                    continue;
                }
                PDFont pdf = mapFont(awt, env);
                if (!(pdf instanceof PDType0Font)) {
                    continue;
                }
                CmapLookup cmap = ((PDType0Font) pdf).getCmapLookup();
                if (cmap == null) {
                    continue;
                }
                boolean all = true;
                for (int cp : cps) {
                    if (cmap.getGlyphId(cp) <= 0) {
                        all = false;
                        break;
                    }
                }
                if (all) {
                    return awt.deriveFont(size);
                }
            }
            return null;
        }

        private Font fontFor(File f) {
            String key = f.getAbsolutePath();
            Font font = fileFonts.get(key);
            if (font == null) {
                try {
                    font = Font.createFont(Font.TRUETYPE_FONT, f);
                } catch (Exception ex) {
                    font = null;
                }
                fileFonts.put(key, font);
            }
            return font;
        }

        private static boolean containsMissingGlyph(PDFont font, String text) {
            CmapLookup cmap = font instanceof PDType0Font ? ((PDType0Font) font).getCmapLookup() : null;
            if (cmap == null) {
                return false;
            }
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                i += Character.charCount(cp);
                if (cmap.getGlyphId(cp) <= 0) {
                    return true;
                }
            }
            return false;
        }

        private static String substituteMissing(String text, PDFont runPdf) {
            StringBuilder sb = new StringBuilder(text.length());
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                i += Character.charCount(cp);
                if (runPdf instanceof PDType0Font) {
                    CmapLookup cmap = ((PDType0Font) runPdf).getCmapLookup();
                    if (cmap != null && cmap.getGlyphId(cp) <= 0) {
                        char sub = SUBSTITUTE.getOrDefault(cp, '?');
                        sb.append(sub);
                        continue;
                    }
                }
                sb.appendCodePoint(cp);
            }
            return sb.toString();
        }

        private static String collectRun(AttributedCharacterIterator iterator, int runEnd) {
            StringBuilder sb = new StringBuilder();
            while (iterator.getIndex() < runEnd) {
                char c = iterator.current();
                if (c == AttributedCharacterIterator.DONE) {
                    return sb.toString();
                }
                sb.append(c);
                iterator.next();
            }
            return sb.toString();
        }

        private static void repairOpenTextBlock(IFontTextDrawerEnv env) {
            try {
                env.getContentStream().endText();
            } catch (Exception ex) {
                return;
            }
            try {
                env.getContentStream().restoreGraphicsState();
            } catch (Exception ignored) {
            }
        }

        private static String shortText(String text) {
            return text.length() <= 24 ? text : text.substring(0, 24) + "...";
        }

        @Override
        protected PDFont mapFont(Font font, IFontTextDrawerEnv env) throws IOException, FontFormatException {
            try {
                return super.mapFont(font, env);
            } catch (FontFormatException | IOException e) {
                String k = font.getFamily() + "." + font.getStyle();
                if (degraded.add(k)) {
                    engineLog("font %s unavailable (%s), degrade to shapes", k, e.getMessage());
                }
                return null;
            }
        }
    }

    private static final Map<Integer, Character> SUBSTITUTE = new HashMap<>();

    static {
        SUBSTITUTE.put(0x226A, '«');
        SUBSTITUTE.put(0x226B, '»');
        SUBSTITUTE.put(0x2013, '-');
        SUBSTITUTE.put(0x2014, '-');
        SUBSTITUTE.put(0x2212, '-');
    }
}