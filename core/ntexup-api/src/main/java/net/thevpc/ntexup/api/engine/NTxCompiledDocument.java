package net.thevpc.ntexup.api.engine;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.util.NOptional;

import java.util.Iterator;
import java.util.List;

public interface NTxCompiledDocument {
    NTxSource source();

    NTxDocument compiledDocument();

    boolean isCompiled();

    NTxDocument rawDocument();

    String title();

    NTxEngine engine();

    Iterator<NTxCompiledPage> pagesIterator();

    NOptional<NTxCompiledPage> page(int index);
    List<NTxCompiledPage> pages();

    Throwable currentThrowable();
}
