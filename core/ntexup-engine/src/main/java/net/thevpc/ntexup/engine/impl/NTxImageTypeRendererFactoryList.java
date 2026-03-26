package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.renderer.NTxImageTypeRendererFactory;

public class NTxImageTypeRendererFactoryList extends  NtxServiceListImpl2<NTxImageTypeRendererFactory> {
    public NTxImageTypeRendererFactoryList(DefaultNTxEngine engine) {
        super("image type renderer", NTxImageTypeRendererFactory.class, engine);
    }
}
