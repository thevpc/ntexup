package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NOptional;

public interface NTxDocumentRenderer {

    void setProperty(String name, Object value);

    <T> void setProperty(Class<T> name, T value);

    NOptional<Object> getProperty(String name);

    <T> NOptional<T> getProperty(String name, Class<T> expectedType);

    <T> NOptional<T> getProperty(Class<T> expectedType);

    NTxDocumentView render(NTxCompiledDocument document);

    NTxDocumentView render(NTxDocument document);

    NTxDocumentView renderPath(NPath path);

    NTxDocumentView renderSupplier(NTxDocumentRendererSupplier document);

    void addRendererListener(NTxDocumentRendererListener listener);

}
