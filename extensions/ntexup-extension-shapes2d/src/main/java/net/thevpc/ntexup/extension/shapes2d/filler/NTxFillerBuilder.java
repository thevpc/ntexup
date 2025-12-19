/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.extension.shapes2d.filler;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;

/**
 * @author vpc
 */
public class NTxFillerBuilder implements NTxNodeBuilder {

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.FILLER)
                .renderComponent((ctx, builderContext1) -> renderMain(ctx, builderContext1));
    }

    public void renderMain(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        NTxNode node = rendererContext.node();
        NTxBounds2 bounds = rendererContext.parentBounds();
        NTxBounds2 b = new NTxBounds2(
                bounds.getMinX(),
                bounds.getMinY(),
                bounds.getWidth(),
                bounds.getHeight());
        if (!rendererContext.isDry()) {
            rendererContext.paintBackground(node, bounds);
        }
        rendererContext.drawContour();
    }

}
