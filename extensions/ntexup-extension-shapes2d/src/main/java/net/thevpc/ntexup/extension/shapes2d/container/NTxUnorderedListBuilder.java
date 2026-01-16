/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.extension.shapes2d.container;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.*;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.extension.shapes2d.container.util.NTxListHelper;

import java.util.List;

/**
 * @author vpc
 */
public class NTxUnorderedListBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.UNORDERED_LIST)
                .alias("ul")
                .selfBounds(this::selfBounds)
                .renderComponent(this::renderMain)
        ;
    }

    public NTxBounds2D selfBounds(NTxNodeRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        List<NTxListHelper.NodeWithIndent> all = NTxListHelper.build(node, false,rendererContext);
        NTxBounds2D expectedBounds = rendererContext.defaultSelfBounds();
        for (NTxListHelper.NodeWithIndent a : all) {
            expectedBounds.expand(a.rowBounds);
        }
        return expectedBounds;
    }

    public void renderMain(NTxNodeRendererContext rendererContext) {
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        //NTxBounds2 expectedBounds = ctx.selfBounds(p);
        List<NTxListHelper.NodeWithIndent> all = NTxListHelper.build(rendererContext.node(), false, rendererContext);
        for (int i = 0; i < all.size(); i++) {
            NTxListHelper.NodeWithIndent a = all.get(i);
            rendererContext.withChild(a.bullet,a.bulletBounds).render();
            rendererContext.withChild(a.child,a.childBounds).render();
        }
    }


}
