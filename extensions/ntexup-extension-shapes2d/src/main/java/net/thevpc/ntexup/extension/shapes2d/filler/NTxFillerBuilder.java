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
import net.thevpc.ntexup.api.renderer.NTxRendererContext;

/**
 * @author vpc
 */
public class NTxFillerBuilder implements NTxNodeBuilder {

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.FILLER)
                .renderComponent(this::renderMain);
    }

    public void renderMain(NTxRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        NTxBounds2D bounds = rendererContext.parentBounds2D();
        NTxBounds2D b = NTxBounds2D.ofWidth(
                bounds.minX(),
                bounds.minY(),
                bounds.widthX(),
                bounds.widthY());
        if (!rendererContext.isDry()) {
            rendererContext.paintBackground(bounds);
        }
        rendererContext.drawContour();
    }

}
