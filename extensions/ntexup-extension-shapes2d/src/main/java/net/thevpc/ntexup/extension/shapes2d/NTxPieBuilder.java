package net.thevpc.ntexup.extension.shapes2d;

import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;


/**
 *
 */
public class NTxPieBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id(NTxNodeType.PIE)
                .parseParam().matchesNamedPair(NTxPropName.START_ANGLE, NTxPropName.EXTENT_ANGLE, NTxPropName.DASH).then()
                .parseParam().matchesNamedPair(NTxPropName.SLICE_COUNT).then()
                .parseParam().matchesNamedPair(NTxPropName.SLICES).then()
                .parseParam().matchesNamedPair(NTxPropName.COLORS).then()
                .renderComponent((rendererContext) -> render(rendererContext))
                ;
    }


    private void render(NTxNodeRendererContext rendererContext) {
        NTxDonutBuilder.renderDonutOrPie(rendererContext, defaultStyles, false);
    }



}
