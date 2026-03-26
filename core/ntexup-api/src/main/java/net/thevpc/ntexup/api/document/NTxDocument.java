package net.thevpc.ntexup.api.document;

import java.util.List;
import java.util.Properties;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.source.NTxSourceMonitor;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;

public interface NTxDocument {
    NTxSource source();

//    NTxSourceMonitor sourceMonitor();

    NTxDocument add(NTxNode part);

    NTxDocumentClass documentClass();

    NTxNode root();

    NOptional<String> name();

    Properties getProperties();

    NOptional<String> getProperty(String name);

    void setDocumentClass(NTxDocumentClass documentClass);

    NTxDocument setProperty(String name, String value);

    void mergeDocument(NTxDocument other);

    NTxDocument copy();

    List<NTxNode> pages();

//    boolean isSuccessfullyLoaded();

}
