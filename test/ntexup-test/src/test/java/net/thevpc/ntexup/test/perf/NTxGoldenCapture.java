package net.thevpc.ntexup.test.perf;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.nuts.io.NPath;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

public final class NTxGoldenCapture {
    private NTxGoldenCapture() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            throw new IllegalArgumentException(
                    "Usage: NTxGoldenCapture <deck.ntx> <label> <output-dir> <width> <height>");
        }
        NTxPerfSupport.initialize();
        Path outputDirectory = Path.of(args[2]);
        Files.createDirectories(outputDirectory);
        int width = Integer.parseInt(args[3]);
        int height = Integer.parseInt(args[4]);

        NTxEngine imageEngine = NTxPerfSupport.newEngine();
        List<NTxCompiledPage> pages = NTxPerfSupport.loadPages(imageEngine, args[0]);
        StringBuilder manifest = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            String filename = String.format("%s-p%04d.png", args[1], i + 1);
            Path imagePath = outputDirectory.resolve(filename);
            BufferedImage image = imageEngine.renderImage(pages.get(i),
                    new NTxNodeRendererConfig(width, height).withAnimate(false).withPrint(true));
            if (!ImageIO.write(image, "png", imagePath.toFile())) {
                throw new IllegalStateException("No PNG ImageIO writer available for " + imagePath);
            }
            appendHash(manifest, imagePath, filename);
        }

        NTxEngine pdfEngine = NTxPerfSupport.newEngine();
        NTxCompiledDocument document = pdfEngine.loadDocument(NPath.of(args[0]));
        Path pdfPath = outputDirectory.resolve(args[1] + ".pdf");
        NTxDocumentStreamRenderer renderer = pdfEngine.newPdfRenderer().get();
        renderer.setStreamRendererConfig(new NTxDocumentStreamRendererConfig()
                .setPageWidth(width)
                .setPageHeight(height)
                .setDpi(72)
                .setShowDate(false)
                .setShowFileName(false)
                .setShowPageNumber(false));
        try (OutputStream output = Files.newOutputStream(pdfPath)) {
            renderer.setOutput(output);
            renderer.render(document);
        }
        appendHash(manifest, pdfPath, pdfPath.getFileName().toString());
        Files.writeString(outputDirectory.resolve(args[1] + "-SHA256SUMS.txt"), manifest);
        System.out.printf("Captured %d PNGs and %s (%d bytes) in %s%n",
                pages.size(), pdfPath.getFileName(), Files.size(pdfPath), outputDirectory);
    }

    private static void appendHash(StringBuilder manifest, Path path, String name) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
        }
        manifest.append(HexFormat.of().formatHex(digest.digest()))
                .append("  ").append(name).append(System.lineSeparator());
    }
}
