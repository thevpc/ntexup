package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

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
}
