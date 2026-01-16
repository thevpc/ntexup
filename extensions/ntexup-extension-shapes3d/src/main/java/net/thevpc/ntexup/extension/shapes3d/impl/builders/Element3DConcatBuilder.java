package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry2d.NTxElement2DFactory;
import net.thevpc.ntexup.lib.geometry2d.NTxRegion2D;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.lib.geometry3d.impl.composite.NtxElement3DPrism;

import java.util.Arrays;
import java.util.List;

public class Element3DConcatBuilder implements NtxElement3DNodeParser {
    @Override
    public List<String> getId3d() {
        return Arrays.asList();
    }

    @Override
    public NtxElement3D createElement3D(NTxNode node, NTxNodeRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NTxRegion2D r = NTxElement2DFactory.region(NtxShapes3dUtils.nodeToElement(node)).orNull();
        if (r != null) {
            NTxPoint3D position = NtxShapes3dUtils.resolvePoint(node, NTxPropName.POSITION, "real-position", NTxPoint3D::ofZero, b, mapper);
            double thickness = NTxValue.ofProp(node, "thickness").asDouble().orElse(0.0);
            return new NtxElement3DPrism(
                    position,
                    r,
                    thickness
            );
        }
        return null;
    }


}
