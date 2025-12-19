package net.thevpc.ntexup.extension.shapes2d.shape;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;

public class NTxSphereBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id(NTxNodeType.SPHERE)
                .renderComponent(this::renderMain)
        ;
        defaultStyles.set(NTxPropName.PRESERVE_ASPECT_RATIO, NElement.ofTrue());
    }

    public void renderMain(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        NTxNode node = rendererContext.node();
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxBounds2 b = rendererContext.selfBounds(node, null, null);
        double x = b.getX();
        double y = b.getY();
        NTxGraphics g = rendererContext.graphics();
        boolean someBG = false;
        if (!rendererContext.isDry()) {
            if (someBG = rendererContext.applyBackgroundColor((NTxNode) node)) {
                g.fillSphere((int) x, (int) y, NTxUtils.intOf(b.getWidth()), NTxUtils.intOf(b.getHeight()), 45, 50f);
            }
            if (rendererContext.applyForeground(node, !someBG)) {
                rendererContext.withStroke(node,()->{
                    g.drawOval((int) x, (int) y, NTxUtils.intOf(b.getWidth()), NTxUtils.intOf(b.getHeight()));
                });
            }
        }
        rendererContext.drawContour();
    }

}
