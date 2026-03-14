package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DArc;

import java.util.Arrays;
import java.util.List;

public class Element3DArcBuilder implements  NtxElement3DNodeParser {
    @Override
    public List<String> getId3d() {
        return Arrays.asList(NTxNodeType.LINE);
    }

    @Override
    public NtxElement3D createElement3D(NTxRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NTxNode node=rendererContext.node();
        NTxPoint3D from = NtxShapes3dUtils.resolvePosition3D(node, NTxPropName.FROM, rendererContext, b).orElse(NTxPoint3D.ofZero());
        NTxPoint3D to = NtxShapes3dUtils.resolvePosition3D(node, NTxPropName.TO, rendererContext, b).orElse(NTxPoint3D.ofZero());
        double startAngle = NTxValue.ofProp(node, NTxPropName.START_ANGLE).asDouble().orElse(0.0);
        double endAngle = NTxValue.ofProp(node, NTxPropName.END_ANGLE).asDouble().orElse(0.0);
        NtxElement3DArc r = new NtxElement3DArc(from, to, startAngle, endAngle);
        NtxShapes3dUtils.apply3dProps(node, r, rendererContext, b,false);
        return r;
    }

}
