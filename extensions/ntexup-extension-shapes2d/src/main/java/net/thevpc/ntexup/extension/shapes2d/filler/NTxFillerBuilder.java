/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.extension.shapes2d.filler;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
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
                .renderComponent(this::renderMain);
    }

    public void renderMain(NTxNodeRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        NTxBounds2D bounds = rendererContext.parentBounds();
        NTxBounds2D b = new NTxBounds2D(
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
