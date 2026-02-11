package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;


public interface NTxNodeRenderer {

    NTxSizeRequirements sizeRequirements(NTxRendererContext ctx);

    NTxBounds2D selfBounds(NTxRendererContext ctx);

    void render(NTxRendererContext rendererContext);

    String[] types();
}
