package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DPolyline;

import java.util.Arrays;
import java.util.List;

public class Element3DPolylineBuilder implements  NtxElement3DNodeParser {
    @Override
    public List<String> getId3d() {
        return Arrays.asList(NTxNodeType.POLYLINE);
    }

    @Override
    public NtxElement3D createElement3D(NTxRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NTxNode node=rendererContext.node();
        NTxPoint3D[] points = NtxShapes3dUtils.resolvePositions3D(node, NTxPropName.POINTS, rendererContext,b).get();
        NtxElement3DPolyline r = new NtxElement3DPolyline(points);
        NtxShapes3dUtils.apply3dProps(node, r, rendererContext, b,false);
        return r;
    }

}
