package net.thevpc.ntexup.api.renderer;

public interface NTxDocumentView {
    String getTitle();
    void close();
    void addDocumentListener(NTxDocumentViewListener r);
    void removeDocumentListener(NTxDocumentViewListener r);
}
