package net.thevpc.ntexup.examples;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.io.NPath;

public class TestPdf {
    public static void main(String[] args) {
        Nuts.openWorkspace().share();
        NTxEngine e = new DefaultNTxEngine();
        NPath file = NPath.of("documentation/ntexup-doc").toAbsolute().normalize();
        NTxCompiledDocument doc = e.loadDocument(file);
        NTxDocumentStreamRenderer renderer = e.newPdfRenderer().get();
        renderer.setOutput(file.resolve("output.pdf"));
        renderer.render(doc);
    }
}
