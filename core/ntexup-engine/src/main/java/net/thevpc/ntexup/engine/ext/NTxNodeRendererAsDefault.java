package net.thevpc.ntexup.engine.ext;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds3D;
import net.thevpc.ntexup.engine.renderer.NTxNodeRendererBase;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;

class NTxNodeRendererAsDefault extends NTxNodeRendererBase {
    private final NTxNodeBuilderContextImpl ctx;

    public NTxNodeRendererAsDefault(NTxNodeBuilderContextImpl ctx) {
        super(ctx.id);
        this.ctx = ctx;
    }

    @Override
    public void renderMain(NTxRendererContext ctx) {
        if (this.ctx.renderMainAction != null) {
            this.ctx.renderMainAction.renderMain(ctx.withBuilderContext(this.ctx));
        }
    }

    @Override
    public NTxSizeRequirements sizeRequirements(NTxRendererContext ctx) {
        if (this.ctx.sizeRequirementsAction != null) {
            NTxSizeRequirements u = this.ctx.sizeRequirementsAction.sizeRequirements(ctx.withBuilderContext(this.ctx));
            if (u != null) {
                return u;
            }
        }
        return super.sizeRequirements(ctx);
    }

    @Override
    public NTxBounds2D selfBounds2D(NTxRendererContext ctx) {
        if (this.ctx.selfBounds2DAction != null) {
            NTxBounds2D u = this.ctx.selfBounds2DAction.selfBounds2D(ctx.withBuilderContext(this.ctx));
            if (u != null) {
                return u;
            }
        }
        return super.selfBounds2D(ctx);
    }

    @Override
    public NTxBounds3D selfBounds3D(NTxRendererContext ctx) {
        if (this.ctx.selfBounds3DAction != null) {
            NTxBounds3D u = this.ctx.selfBounds3DAction.selfBounds3D(ctx.withBuilderContext(this.ctx));
            if (u != null) {
                return u;
            }
        }
        return super.selfBounds3D(ctx);
    }
}
