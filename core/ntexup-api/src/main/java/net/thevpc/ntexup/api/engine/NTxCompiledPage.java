package net.thevpc.ntexup.api.engine;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;

public interface NTxCompiledPage {
    int index();

    NTxCompiledDocument document();

    NTxResolutionContext pageContext();

    NTxNode compiledPage();

    Object source();

    boolean isCompiled();

    NTxNode rawPage();
}
