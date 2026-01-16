/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.extension.shapes2d.container;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author vpc
 */
public class NTxGroupBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.GROUP)
                .renderComponent(this::render)
        ;
    }

    public void render(NTxNodeRendererContext ctx) {
        NTxNode node = ctx.node();
        ctx = ctx.withDefaultStyles(defaultStyles);
        NTxBounds2D selfBounds = ctx.selfBounds();
        if (!ctx.isDry()) {
            ctx.paintBackground(node, selfBounds);
        }
        NTxNodeRendererContext finalCtx = ctx;
        List<NTxNode> texts = node.children()
                .stream().filter(x -> finalCtx.withChild(x,finalCtx.parentBounds()).isVisible(x)).collect(Collectors.toList());
        for (NTxNode text : texts) {
            NTxNodeRendererContext ctx3 = ctx.withChild(text,selfBounds);
            ctx3.render();
        }
        ctx.drawContour();
    }

}
