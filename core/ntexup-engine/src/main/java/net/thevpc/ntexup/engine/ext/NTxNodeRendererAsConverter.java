package net.thevpc.ntexup.engine.ext;

import net.thevpc.ntexup.engine.renderer.ConvertedNTxNodeRenderer;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;

class NTxNodeRendererAsConverter extends ConvertedNTxNodeRenderer {
    private final NTxNodeBuilderContextImpl ctx;

    public NTxNodeRendererAsConverter(NTxNodeBuilderContextImpl ctx) {
        super(ctx.id());
        this.ctx = ctx;
    }

    @Override
    public NTxNode convert(NTxNode p, NTxRendererContext rendererContext) {
        if (this.ctx.renderConvertAction != null) {
            return this.ctx.renderConvertAction.convert(p, rendererContext.withBuilderContext(this.ctx));
        }
        return p;
    }
}
