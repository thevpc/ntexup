package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.NTxDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NOptional;

public abstract class NTxDocumentRendererBase implements NTxDocumentRenderer {

    private List<NTxDocumentRendererListener> eventListeners = new ArrayList<>();
    private Map<String, Object> props = new HashMap<>();
    protected NTxDocumentRendererListener eventListenerDelegate = new NTxDocumentRendererListener() {
        @Override
        public void onChangedCompiledDocument(NTxCompiledDocument compiledDocument) {
            for (NTxDocumentRendererListener eventListener : eventListeners) {
                eventListener.onChangedCompiledDocument(compiledDocument);
            }
        }

        @Override
        public void onChangedPage(NTxCompiledPage page) {
            for (NTxDocumentRendererListener eventListener : eventListeners) {
                eventListener.onChangedPage(page);
            }
        }

        @Override
        public void onCloseView() {
            for (NTxDocumentRendererListener eventListener : eventListeners) {
                eventListener.onCloseView();
            }
        }

        @Override
        public void onSaveDocument(NTxCompiledDocument document, NTxDocumentStreamRendererConfig config) {
            for (NTxDocumentRendererListener eventListener : eventListeners) {
                eventListener.onSaveDocument(document, config);
            }
        }
    };
    protected final NTxEngine engine;

    public NTxDocumentRendererBase(NTxEngine engine) {
        this.engine = engine;
    }


    @Override
    public void setProperty(String name, Object value) {
        props.put(name, value);
    }

    @Override
    public <T> NOptional<T> getProperty(String name, Class<T> expectedType) {
        return NOptional.ofNamed(props.get(name), name).instanceOf(expectedType);
    }

    @Override
    public NOptional<Object> getProperty(String name) {
        return NOptional.ofNamed(props.get(name), name);
    }

    @Override
    public <T> void setProperty(Class<T> name, T value) {
        setProperty(name.getName(), value);
    }

    @Override
    public <T> NOptional<T> getProperty(Class<T> expectedType) {
        return getProperty(expectedType.getName(), expectedType);
    }

    @Override
    public void addRendererListener(NTxDocumentRendererListener listener) {
        if (listener != null) {
            this.eventListeners.add(listener);
        }
    }

    @Override
    public NTxDocumentView renderPath(NPath path) {
        return renderSupplier(r -> engine.loadCompiledDocument(path));
    }

    @Override
    public NTxDocumentView render(NTxDocument document) {
        return renderSupplier(e -> engine.asCompiledDocument(document));
    }

    @Override
    public NTxDocumentView render(NTxCompiledDocument document) {
        return renderSupplier(e -> document);
    }

}
