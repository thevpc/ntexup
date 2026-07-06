package net.thevpc.ntexup.engine.renderer;

import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactoryContext;
import net.thevpc.nuts.util.NStringUtils;

public class NTxDocumentRendererFactoryContextImpl implements NTxDocumentRendererFactoryContext {
    private final DefaultNTxEngine hEngine;
    private final String type;

    public NTxDocumentRendererFactoryContextImpl(DefaultNTxEngine hEngine, String type) {
        this.hEngine = hEngine;
        this.type = NStringUtils.strip(type);
    }

    @Override
    public String rendererType() {
        return type;
    }

    @Override
    public NTxEngine engine() {
        return hEngine;
    }

}
