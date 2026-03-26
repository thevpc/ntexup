package net.thevpc.ntexup.api.engine;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.nuts.elem.NElement;

public interface NTxCompiledPage {
    int index();

    NElement toElement(boolean semantic);
    NTxCompiledDocument document();

    NTxResolutionContext pageContext();

    NTxNode compiledPage();

    Object source();

    boolean isCompiled();

    NTxNode rawPage();
}
