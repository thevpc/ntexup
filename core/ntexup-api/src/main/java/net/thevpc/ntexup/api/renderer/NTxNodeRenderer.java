package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;


public interface NTxNodeRenderer {

    NTxSizeRequirements sizeRequirements(NTxNodeRendererContext ctx);

    NTxBounds2D selfBounds(NTxNodeRendererContext ctx);

    void render(NTxNodeRendererContext rendererContext);

    String[] types();
}
