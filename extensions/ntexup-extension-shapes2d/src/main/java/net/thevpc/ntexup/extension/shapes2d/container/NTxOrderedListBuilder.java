/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.extension.shapes2d.container;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.extension.shapes2d.container.util.NTxListHelper;

import java.util.List;

/**
 * @author vpc
 */
public class NTxOrderedListBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();
    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.ORDERED_LIST)
                .alias("ol")
                .selfBounds(this::selfBounds)
                .renderComponent(this::renderMain)
                ;
    }

    public NTxBounds2 selfBounds(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxNode node = rendererContext.node();
        List<NTxListHelper.NodeWithIndent> all = NTxListHelper.build(node, true,rendererContext, builderContext);
        NTxBounds2 expectedBounds = rendererContext.defaultSelfBounds();
        for (NTxListHelper.NodeWithIndent a : all) {
            expectedBounds.expand(a.rowBounds);
        }
        return expectedBounds;
    }

    public void renderMain(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        //NTxBounds2 expectedBounds = ctx.selfBounds(p);
        List<NTxListHelper.NodeWithIndent> all = NTxListHelper.build(rendererContext.node(),true, rendererContext, builderContext);
        for (int i = 0; i < all.size(); i++) {
            NTxListHelper.NodeWithIndent a = all.get(i);
            rendererContext.withChild(a.bullet,a.bulletBounds).render();
            rendererContext.withChild(a.child,a.childBounds).render();
        }
    }

}
