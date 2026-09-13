package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DPolyline;

import java.util.Arrays;
import java.util.List;

public class Element3DHelixBuilder implements NTxNodeBuilder, NtxElement3DNodeParser {
    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id("helix").alias("helix3d", "spiral3d")
                .parseParam().matchesAny().end()
                .renderComponent(this::render);
    }

    public void render(NTxRendererContext rendererContext) {
        // do nothing in 2D
    }

    @Override
    public List<String> getId3d() {
        return Arrays.asList("helix", "helix3d", "spiral3d");
    }

    @Override
    public NtxElement3D createElement3D(NTxRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NTxNode node = rendererContext.node();
        NTxPoint3D position = NtxShapes3dUtils.resolvePosition3D(node, NTxPropName.POSITION, rendererContext, b).orElse(NTxPoint3D.ofZero());
        NTxPoint3D radius = NtxShapes3dUtils.resolveDistance(node, "radius", rendererContext, b).orElse(new NTxPoint3D(30, 30, 30));
        NTxPoint3D size = NtxShapes3dUtils.resolveDistance(node, NTxPropName.SIZE, rendererContext, b).orElse(new NTxPoint3D(0, 80, 0));
        double height = size.y != 0 ? size.y : 80.0;
        Double customHeight = NtxShapes3dUtils.resolveZDistance(node, "height", rendererContext, b).orNull();
        if (customHeight != null && customHeight != 0) {
            height = Math.abs(customHeight);
        }

        double turns = NTxValue.of(rendererContext.computePropertyValue("turns").orNull()).asDouble().orElse(3.0);
        int pointsCount = NTxValue.of(rendererContext.computePropertyValue("points").orNull()).asInt().orElse((int) Math.max(30, turns * 40));

        NTxPoint3D[] pts = new NTxPoint3D[pointsCount + 1];
        for (int i = 0; i <= pointsCount; i++) {
            double t = (double) i / pointsCount;
            double angle = 2.0 * Math.PI * turns * t;
            double x = position.x + radius.x * Math.cos(angle);
            double y = position.y + height * t;
            double z = position.z + radius.z * Math.sin(angle);
            pts[i] = new NTxPoint3D(x, y, z);
        }

        NtxElement3DPolyline polyline = new NtxElement3DPolyline(pts);
        NtxShapes3dUtils.apply3dProps(node, polyline, rendererContext, b, false);
        return polyline;
    }
}
