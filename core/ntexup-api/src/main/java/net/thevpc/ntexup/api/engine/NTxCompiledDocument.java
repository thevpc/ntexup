package net.thevpc.ntexup.api.engine;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.security.NTxManifest;
import net.thevpc.ntexup.api.document.security.NTxManifestOptions;
import net.thevpc.ntexup.api.eval.NTxObj;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.source.NTxSourceMonitor;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;

import java.util.Iterator;
import java.util.List;

public interface NTxCompiledDocument {
    NTxSource source();

    NTxDocument document();

    boolean isCompiled();

    NTxDocument rawDocument();

    String title();

    NTxEngine engine();

    Iterator<NTxCompiledPage> pagesIterator();

    NOptional<NTxCompiledPage> page(int index);

    List<NTxCompiledPage> pages();

    Throwable currentThrowable();

    NElement toElement(boolean semantic);

    NTxSourceMonitor sourceMonitor();

    NTxManifest computeManifest(NTxManifestOptions options);

    NOptional<NTxObj> getGlobalObject(String name);

    NTxCompiledDocument setGlobalObject(String name, NTxObj obj);
}
