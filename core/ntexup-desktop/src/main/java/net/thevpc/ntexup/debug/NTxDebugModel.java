package net.thevpc.ntexup.debug;

import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.document.node.NTxNode;

public class NTxDebugModel {
    private NTxEngine engine;
    private NTxDocument rawDocument;
    private NTxNode currentPage;
    private NTxDocument compiledDocument;
    private NTxLogger messageList;

    public NTxEngine getEngine() {
        return engine;
    }

    public NTxDebugModel setEngine(NTxEngine engine) {
        this.engine = engine;
        return this;
    }

    public NTxNode getCurrentPage() {
        return currentPage;
    }

    public NTxDebugModel setCurrentPage(NTxNode currentPage) {
        this.currentPage = currentPage;
        return this;
    }

    public NTxDebugModel setRawDocument(NTxDocument rawDocument) {
        this.rawDocument = rawDocument;
        return this;
    }

    public NTxDebugModel setCompiledDocument(NTxDocument compiledDocument) {
        this.compiledDocument = compiledDocument;
        return this;
    }

    public NTxDocument getRawDocument() {
        return rawDocument;
    }

    public NTxDocument getCompiledDocument() {
        return compiledDocument;
    }

    public NTxLogger messages() {
        return messageList;
    }

    public NTxDebugModel setMessageList(NTxLogger messageList) {
        this.messageList = messageList;
        return this;
    }
}
