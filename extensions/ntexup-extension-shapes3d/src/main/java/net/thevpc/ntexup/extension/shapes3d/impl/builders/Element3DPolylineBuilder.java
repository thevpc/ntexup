package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.lib.geometry3d.impl.NTx3DUtils;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DPolyline;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DTriangle;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Element3DPolylineBuilder implements  NtxElement3DNodeParser {
    @Override
    public List<String> getId3d() {
        return Arrays.asList(NTxNodeType.POLYLINE);
    }

    @Override
    public NtxElement3D createElement3D(NTxNode node, NTxNodeRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NTxPoint3D[] points = NtxShapes3dUtils.resolvePoints(node, NTxPropName.POINTS, "real-points", () -> new NTxPoint3D[0], b, mapper);
        NtxElement3DPolyline r = new NtxElement3DPolyline(points);
        NtxShapes3dUtils.apply3dProps(node, r, rendererContext, b);
        return r;
    }

}
