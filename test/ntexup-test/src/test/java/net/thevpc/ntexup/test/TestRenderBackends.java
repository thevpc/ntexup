package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests that the same connector/decorated deck can be consumed by every
 * non-interactive backend: PDF vector, raster image, HTML and TSON dump.
 */
public class TestRenderBackends {

    private static final String DECK = "page{\n"
            + "  rectangle(name:\"a\", at:(10,10), size:(20,10), background-color:white, draw-contour){\n"
            + "    text(\"A\", at:center)\n"
            + "  }\n"
            + "  rectangle(name:\"b\", at:(70,40), size:(20,10), background-color:white, draw-contour){\n"
            + "    text(\"B\", at:center)\n"
            + "  }\n"
            + "  connector(\"a\", \"b\", end-arrow:simple(), label:\"edge\", flow:dots(count:2))\n"
            + "  quad-curve(from:(10,60), ctrl:(50,80), to:(90,60), label:\"curve\", flow:dots(count:1))\n"
            + "}\n";

    private static final String PADDED_TABLE_DECK = """
            styles {
                @padded {
                    font-size: 5%
                    cell { padding: 1% }
                    header(row: 1, col: 1) { background-color: "#ff0000"; color: "#010203" }
                    header(row: 1, col: 2) { background-color: "#00ff00"; color: "#040506" }
                    header(row: 1, col: 3) { background-color: "#0000ff"; color: "#070809" }
                    cell(row: 1, col: 1) { background-color: "#ff8000"; color: "#0a0b0c" }
                    cell(row: 1, col: 2) { background-color: "#00ff80"; color: "#0d0e0f" }
                    cell(row: 1, col: 3) { background-color: "#8000ff"; color: "#101112" }
                    cell(row: 2, col: 1) { background-color: "#ffff00"; color: "#131415" }
                    cell(row: 2, col: 2) { background-color: "#00ffff"; color: "#161718" }
                    cell(row: 2, col: 3) { background-color: "#ff00ff"; color: "#191a1b" }
                }
            }
            page(background-color: white) {
                table(class: padded, at: (20, 20),
                    header-data: [["H1", "H2", "H3"]],
                    data: [["A1", "A2", "A3"], ["B1", "B2", "B3"]])
            }
            """;

    @BeforeAll
    static void init() {
        System.setProperty("java.awt.headless", "true");
        Nuts.require();
    }

    @Test
    public void pdfRendererHandlesConnectorsAndDecorations() {
        byte[] bytes = render("pdf");
        assertTrue(bytes.length > 500, "PDF should not be empty, got " + bytes.length + " bytes");
        assertTrue(new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"),
                "output should be a PDF");
    }

    @Test
    public void rasterRendererHandlesConnectorsAndDecorations() {
        byte[] bytes = render("image");
        assertTrue(bytes.length > 500, "raster image should not be empty, got " + bytes.length + " bytes");
        assertTrue(isPng(bytes), "output should be a PNG");
    }

    @Test
    public void reusableTableClassKeepsEveryPercentageFontCellVisible() throws IOException {
        NTxEngine engine = new DefaultNTxEngine();
        NTxCompiledDocument doc = engine.loadDocument(new ByteArrayInputStream(
                PADDED_TABLE_DECK.getBytes(StandardCharsets.UTF_8)));
        NTxDocumentStreamRenderer renderer = engine.newImageRenderer().get();
        renderer.setStreamRendererConfig(new NTxDocumentStreamRendererConfig()
                .setPageWidth(600)
                .setPageHeight(400)
                .setDpi(72)
                .setShowDate(false)
                .setShowFileName(false)
                .setShowPageNumber(false));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderer.setOutput(out);
        renderer.render(doc);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(out.toByteArray()));

        int[] colors = {
                0xffff0000, 0xff00ff00, 0xff0000ff,
                0xffff8000, 0xff00ff80, 0xff8000ff,
                0xffffff00, 0xff00ffff, 0xffff00ff
        };
        int[] textColors = {
                0xff010203, 0xff040506, 0xff070809,
                0xff0a0b0c, 0xff0d0e0f, 0xff101112,
                0xff131415, 0xff161718, 0xff191a1b
        };
        String[] cells = {
                "header 1/1", "header 1/2", "header 1/3",
                "body 1/1", "body 1/2", "body 1/3",
                "body 2/1", "body 2/2", "body 2/3"
        };
        for (int i = 0; i < colors.length; i++) {
            assertTrue(countColor(image, colors[i]) > 0,
                    cells[i] + " background must remain visible with percentage padding");
            assertTrue(countColor(image, textColors[i]) > 0,
                    cells[i] + " text must remain visible with a percentage font");
        }
    }

    @Test
    public void htmlRendererHandlesConnectorsAndDecorations() {
        byte[] bytes = render("html");
        assertTrue(bytes.length > 500, "HTML package should not be empty, got " + bytes.length + " bytes");
        assertTrue(bytes.length > 4 && bytes[0] == 'P' && bytes[1] == 'K',
                "HTML renderer writes a zip package to a stream");
    }

    private static byte[] render(String type) {
        NTxEngine engine = new DefaultNTxEngine();
        NTxCompiledDocument doc = engine.loadDocument(
                new ByteArrayInputStream(DECK.getBytes(StandardCharsets.UTF_8)));
        NTxDocumentStreamRenderer renderer = switch (type) {
            case "pdf" -> engine.newPdfRenderer().get();
            case "html" -> engine.newHtmlRenderer().get();
            case "image" -> engine.newImageRenderer().get();
            default -> engine.newStreamRenderer(type).get();
        };
        renderer.setStreamRendererConfig(new NTxDocumentStreamRendererConfig()
                .setPageWidth(400)
                .setPageHeight(300)
                .setDpi(72)
                .setShowDate(false)
                .setShowFileName(false)
                .setShowPageNumber(false));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderer.setOutput(out);
        renderer.render(doc);
        return out.toByteArray();
    }

    private static boolean isPng(byte[] bytes) {
        return bytes.length > 8
                && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 'P'
                && bytes[2] == 'N'
                && bytes[3] == 'G';
    }

    private static int countColor(BufferedImage image, int color) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == color) {
                    count++;
                }
            }
        }
        return count;
    }
}
