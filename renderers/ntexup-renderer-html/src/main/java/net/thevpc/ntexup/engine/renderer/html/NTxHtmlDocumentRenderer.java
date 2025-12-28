/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.engine.renderer.html;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.*;
import net.thevpc.nuts.io.NIOException;
import net.thevpc.nuts.io.NPath;

/**
 * @author vpc
 */
public class NTxHtmlDocumentRenderer extends NTxDocumentStreamRendererBase implements NTxDocumentStreamRenderer {

    private NTxDocumentRendererContext rendererContext = new NTxDocumentRendererContextImpl();

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
            o = NPath.of("document.pdf");
        }
        if (o instanceof NPath) {
            try (OutputStream os = ((NPath) o).getOutputStream()) {
                renderStream(d, os);
            } catch (IOException ex) {
                throw new NIOException(ex);
            }
        } else if (o instanceof OutputStream) {
            renderStream(d, (OutputStream) o);
        }
        return null;
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
