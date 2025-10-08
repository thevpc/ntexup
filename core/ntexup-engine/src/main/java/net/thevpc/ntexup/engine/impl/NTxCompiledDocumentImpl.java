package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.engine.parser.resources.NTxSourceNew;
import net.thevpc.nuts.text.NMsg;

import java.util.ArrayList;
import java.util.List;

public class NTxCompiledDocumentImpl implements NTxCompiledDocument {
    private NTxDocument rawDocument;
    private NTxDocument document;
    private NTxEngine engine;
    private List<NTxCompiledPage> compiledPages;
    private Throwable currentThrowable;

    public NTxCompiledDocumentImpl(NTxDocument rawDocument, NTxEngine engine) {
        this.rawDocument = rawDocument;
        this.engine = engine;
    }

    @Override
    public NTxSource source(){
        return rawDocument.source();
    }

    @Override
    public NTxDocument compiledDocument() {
        if (document == null) {
            try {
                document = engine.compileDocument(rawDocument.copy()).get();
            } catch (Exception ex) {
                engine.log().log(NMsg.ofC("compile document failed %s", ex));
                this.currentThrowable = ex;
            }
        }
        if (document == null) {
            document = engine.documentFactory().ofDocument(null);
        }
        return document;
    }

    @Override
    public boolean isCompiled() {
        return document != null;
    }

    @Override
    public NTxDocument rawDocument() {
        return rawDocument;
    }

    @Override
    public String title() {
        if (rawDocument == null) {
            return "New Document";
        }
        NTxSource source = rawDocument.root().source();

        if (source == null) {
            return ("New Document");
        } else {
            return (String.valueOf(source));
        }
    }

    @Override
    public NTxEngine engine() {
        return engine;
    }

    @Override
    public List<NTxCompiledPage> pages() {
        if (compiledPages == null) {
            List<NTxCompiledPage> compiledPages2 = new ArrayList<>();
            int index = 0;
            for (NTxNode n : engine.tools().resolvePages(compiledDocument())) {
                compiledPages2.add(new NTxCompiledPageImpl(n, this, ++index));
            }
            if(compiledPages2.isEmpty()){
                NTxNode node = engine.documentFactory().ofPage();
                NTxSource source = rawDocument.source();
                if(source!=null){
                    node.setSource(source);
                }else{
                    node.setSource(new NTxSourceNew());
                }
            }
            compiledPages = compiledPages2;
        }
        return compiledPages;
    }


    @Override
    public Throwable currentThrowable() {
        return currentThrowable;
    }

}
