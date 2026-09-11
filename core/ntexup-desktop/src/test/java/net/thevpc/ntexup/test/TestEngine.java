package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.NTxDocumentLoadingResult;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.io.NPath;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;

public class TestEngine {
    public static void main(String[] args) {
        Nuts.openWorkspace().share();
        NTxEngine e=new DefaultNTxEngine();
        try {
            NPath path = NPath.of("/home/vpc/.gemini/antigravity/brain/21469776-ef97-4bbf-a0bf-2a679d938cb4/scratch/test-eniso.ntx");
            NTxCompiledDocument doc = e.loadDocument(path);
            System.out.println("Loaded pages: " + doc.pages().size());
            NTxDocumentStreamRenderer renderer = e.newPdfRenderer().get();
            renderer.setOutput(NPath.of("/home/vpc/.gemini/antigravity/brain/21469776-ef97-4bbf-a0bf-2a679d938cb4/scratch/test-eniso.pdf"));
            renderer.render(doc);
            System.out.println("Render success!");
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
