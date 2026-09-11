package net.thevpc.ntexup.main;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererListener;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;

import java.util.ArrayList;
import java.util.List;

public class NTxDocumentRendererListenerList implements NTxDocumentRendererListener {
    private List<NTxDocumentRendererListener> registeredHDocumentRendererListener = new ArrayList<>();

    public void add(NTxDocumentRendererListener a) {
        if (a != null) {
            registeredHDocumentRendererListener.add(a);
        }
    }

    @Override
    public void onChangedCompiledDocument(NTxCompiledDocument compiledDocument) {
        for (NTxDocumentRendererListener r : registeredHDocumentRendererListener) {
            r.onChangedCompiledDocument(compiledDocument);
        }
    }

    @Override
    public void onChangedPage(NTxCompiledPage page) {
        for (NTxDocumentRendererListener r : registeredHDocumentRendererListener) {
            r.onChangedPage(page);
        }
    }

    @Override
    public void onCloseView() {
        for (NTxDocumentRendererListener r : registeredHDocumentRendererListener) {
            r.onCloseView();
        }
    }

    @Override
    public void onSaveDocument(NTxCompiledDocument document, NTxDocumentStreamRendererConfig config) {
        for (NTxDocumentRendererListener eventListener : registeredHDocumentRendererListener) {
            eventListener.onSaveDocument(document, config);
        }
    }

    @Override
    public void onStartLoadingDocument() {
        for (NTxDocumentRendererListener eventListener : registeredHDocumentRendererListener) {
            eventListener.onStartLoadingDocument();
        }
    }

    @Override
    public void onEndLoadingDocument() {
        for (NTxDocumentRendererListener eventListener : registeredHDocumentRendererListener) {
            eventListener.onEndLoadingDocument();
        }
    }

    public void remove(NTxDocumentRendererListener a) {
        registeredHDocumentRendererListener.remove(a);
    }
}
