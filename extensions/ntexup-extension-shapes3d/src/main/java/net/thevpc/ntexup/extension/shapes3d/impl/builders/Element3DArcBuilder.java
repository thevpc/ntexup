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
import net.thevpc.ntexup.lib.geometry3d.NTxElement3DFactory;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DArc;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DLine;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Element3DArcBuilder implements  NtxElement3DNodeParser {
    @Override
    public List<String> getId3d() {
        return Arrays.asList(NTxNodeType.LINE);
    }

    @Override
    public NtxElement3D createElement3D(NTxNode node, NTxNodeRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NTxPoint3D from = NtxShapes3dUtils.resolvePoint(node, NTxPropName.FROM, "from-real", NTxPoint3D::ofZero, b, mapper);
        NTxPoint3D to = NtxShapes3dUtils.resolvePoint(node, NTxPropName.TO, "to-real", NTxPoint3D::ofZero, b, mapper);
        double startAngle = NTxValue.ofProp(node, NTxPropName.START_ANGLE).asDouble().orElse(0.0);
        double endAngle = NTxValue.ofProp(node, NTxPropName.END_ANGLE).asDouble().orElse(0.0);
        NtxElement3DArc r = new NtxElement3DArc(from, to, startAngle, endAngle);
        NtxShapes3dUtils.apply3dProps(node, r, rendererContext, b);
        return r;
    }

}
