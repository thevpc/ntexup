/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.engine.renderer.html;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.util.List;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.*;
import net.thevpc.nuts.io.NCompress;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.io.NPathRenameOptions;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NException;
import net.thevpc.nuts.util.NIllegalArgumentException;
import net.thevpc.nuts.util.NStringUtils;

import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * @author vpc
 */
public class NTxHtmlDocumentRenderer extends NTxDocumentStreamRendererBase implements NTxDocumentStreamRenderer {

    private final NTxDocumentRendererContext rendererContext = new NTxDocumentRendererContextImpl();

    public NTxHtmlDocumentRenderer(NTxEngine engine) {
        super(engine);
    }

    protected void renderStream(NTxCompiledDocument document, OutputStream os) {
        PrintStream out = new PrintStream(os);
        out.println("<html>");
        out.println("<body>");
        for (NTxCompiledPage page : document.pages()) {
            out.println("<div class=\"page\">");
            renderNode(page.compiledPage(), out);
            out.println("</div>");
        }
        out.println("</body>");
        out.println("</html>");
    }

    @Override
    public NTxDocumentView renderSupplier(NTxDocumentRendererSupplier document) {
        NTxCompiledDocument d = document.get(rendererContext);
        Object o = output;
        if (o == null) {
            o = NPath.of("dist");
        }

        if (o instanceof NPath) {
            NPath pp = (NPath) o;
            if (!pp.exists()) {
                pp.mkdirs();
            }
            if (pp.isDirectory()) {
                writeIntoDirectory(d, pp);
            } else if (pp.isFile()) {
                NPath toRemoveFolder = NPath.ofTempFolder();
                writeIntoDirectory(d, toRemoveFolder);
                if (!pp.name().endsWith(".zip")) {
                    pp = pp.resolveSibling(NPathRenameOptions.ofExtension("zip"));
                }
                NCompress.of().source(toRemoveFolder).to(pp);
                toRemoveFolder.deleteTree();
            } else {
                throw new IllegalArgumentException("invalid output path " + pp);
            }
        } else if (o instanceof OutputStream) {
            NPath toRemoveFolder = NPath.ofTempFolder();
            writeIntoDirectory(d, toRemoveFolder);
            NCompress.of().source(toRemoveFolder).to((OutputStream) o);
            toRemoveFolder.deleteTree();
        }
        return null;
    }

    private void writeIntoDirectory(NTxCompiledDocument d, NPath writeIntoDirectory) {
        NTxDocumentStreamRendererConfig config = engine.tools().validateDocumentStreamRendererConfig(this.config);
        int usableWidth;
        int usableHeight;
        int a4_w = 595;
        int a4_h = 842;
        int marginLeft = 0;
        int marginRight = 0;
        int marginTop = 0;
        int padding = 0;
        int marginBottom = 0;
        int pageIndex = 1;
        if (config.getOrientation() == NTxPageOrientation.LANDSCAPE) {
            usableWidth = a4_h - marginLeft - marginRight - padding;
            usableHeight = a4_w - marginTop - marginBottom - padding;
        } else {
            usableWidth = a4_w - marginLeft - marginRight - padding;
            usableHeight = a4_h - marginTop - marginBottom - padding;
        }
        int pixelWidth = usableWidth;
        int pixelHeight = usableHeight;
        NPath imagesFolder = writeIntoDirectory.resolve("images");
        imagesFolder.list().stream().filter(x -> x.name().matches("page-[0-9]+[.]png"))
                .forEach(x -> x.delete());
        List<NTxCompiledPage> allPages = d.pages();
        int zeros = (int) Math.ceil(Math.log10(allPages.size()));
        if (zeros <= 0) {
            zeros = 1;
        }
        DecimalFormat zformat = new DecimalFormat(NStringUtils.repeat("0", zeros));
        for (NTxCompiledPage page : allPages) {
            try (InputStream is = new ByteArrayInputStream(engine.renderImageBytes(
                    page,
                    new NTxNodeRendererConfig(pixelWidth, pixelHeight)
                            .withAnimate(false)
                            .withPrint(true)
            ))) {
                Image img = ImageIO.read(is);
                if (img == null) {
                    throw new NIllegalArgumentException(NMsg.ofC("invalid image for page %s", pageIndex));
                }
                BufferedImage bi = net.thevpc.ntexup.api.util.NTxUtilsImages.resizeImage(
                        new ImageIcon(img).getImage(),
                        pixelWidth, pixelHeight);
                NPath imageFile = imagesFolder.resolve("page-" + zformat.format(pageIndex) + ".png").toAbsolute();
                ImageIO.write(bi, "png", imageFile.mkParentDirs().toFile().get());
                engine.log().log(NMsg.ofC("rendered page %s to %s", pageIndex, imageFile));
            } catch (IOException ex) {
                throw NException.ofUncheckedException(ex);
            }
            pageIndex++;
        }
    }

    public PrintStream psOf(OutputStream out) {
        if (out instanceof PrintStream) {
            return (PrintStream) out;
        }
        return new PrintStream(out);
    }

    @Override
    public NTxDocumentStreamRenderer renderNode(NTxNode part, OutputStream out) {
        switch (part.type()) {
            case NTxNodeType.PAGE_GROUP:
                break;
            case NTxNodeType.PAGE: {
                PrintStream o = psOf(out);
                for (NTxNode pp : part.children()) {
                    renderNode(pp, o);
                }
                o.flush();
                break;
            }
            default:
                throw new IllegalArgumentException("invalid type " + part);
        }
        return this;
    }

//    public void render(HDocumentPart part, PrintStream out) {
//        switch (part.type()) {
//            case PAGE_GROUP:
//                break;
//            case PAGE:
//                break;
//            case PARAGRAPH:
//                break;
//            case PHRASE:
//                break;
//            default:
//                throw new IllegalArgumentException("invalid type " + part);
//        }
//    }

    private class NTxDocumentRendererContextImpl implements NTxDocumentRendererContext {

        public NTxDocumentRendererContextImpl() {
        }
    }

}
