package net.thevpc.ntexup.engine.ext;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.engine.renderer.text.NTxTextRendererBase;

class NTxNodeRendererAsText extends NTxTextRendererBase {
    private final NTxNodeBuilderContextImpl ctx;

    public NTxNodeRendererAsText(NTxNodeBuilderContextImpl ctx) {
        super(ctx.id, ctx.id);
        this.ctx = ctx;
    }

    @Override
    public void renderMain(NTxNodeRendererContext ctx) {
        if (this.ctx.renderMainAction != null) {
            this.ctx.renderMainAction.renderMain(ctx);
        }else {
            super.renderMain(ctx);
        }
    }

    @Override
    public NTxSizeRequirements sizeRequirements(NTxNodeRendererContext ctx) {
        if (this.ctx.sizeRequirementsAction != null) {
            NTxSizeRequirements u = this.ctx.sizeRequirementsAction.sizeRequirements(ctx.withBuilderContext(this.ctx));
            if (u != null) {
                return u;
            }
        }
        return super.sizeRequirements(ctx);
    }

    @Override
    public NTxBounds2D selfBounds(NTxNodeRendererContext ctx) {
        if (this.ctx.selfBoundsAction != null) {
            NTxBounds2D u = this.ctx.selfBoundsAction.selfBounds(ctx);
            if (u != null) {
                return u;
            }
        }
        return super.selfBounds(ctx);
    }
}
