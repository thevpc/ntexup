package net.thevpc.ntexup.extension.shapes2d.shape;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;

public class NTxTriangleBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.TRIANGLE)
                .renderComponent((rendererContext, builderContext1) -> renderMain(rendererContext, builderContext1))
        ;
    }

    public void renderMain(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxPoint2D[] points = new NTxPoint2D[]{
                new NTxPoint2D(0, 100),
                new NTxPoint2D(100, 100),
                new NTxPoint2D(50, 0),
        };
        NTxPolygonHelper.renderPoints(points, rendererContext);
    }

}
