package net.thevpc.ntexup.extension.shapes2d.shape;

import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;

public class NTxDecagonBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.DECAGON)
                .renderComponent(this::render);
    }

    public void render(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext buildContext) {
        NTxPolygonHelper.renderPointsCount(10, rendererContext, defaultStyles);
    }

}
