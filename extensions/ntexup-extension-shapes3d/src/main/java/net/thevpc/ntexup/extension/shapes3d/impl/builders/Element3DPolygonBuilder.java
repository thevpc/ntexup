package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry2d.NTx2DUtils;
import net.thevpc.ntexup.lib.geometry2d.impl.NTxPolygonWithHoles2DImpl;
import net.thevpc.ntexup.lib.geometry2d.impl.NTxRing2DImpl;
import net.thevpc.ntexup.lib.geometry3d.NTxElement3DFactory;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.lib.geometry3d.impl.NTx3DUtils;
import net.thevpc.ntexup.lib.geometry3d.impl.composite.NtxElement3DPrism;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DLine;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DPolygon;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Element3DPolygonBuilder implements  NtxElement3DNodeParser {
    @Override
    public List<String> getId3d() {
        return Arrays.asList(NTxNodeType.LINE);
    }

    @Override
    public NtxElement3D createElement3D(NTxNode node, NTxNodeRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NTxPoint3D position = NTx3DUtils.asPoint3D(NTxValue.of(node.getPropertyValue(NTxPropName.POSITION))).orNull();
        Double thickness = NTxValue.of(node.getPropertyValue("thickness")).asDouble().orNull();
        NTxPoint2D[] points2d = NTx2DUtils.asPoint2DArray(node.getPropertyValue(NTxPropName.POINTS).orNull()).orNull();
        if(points2d!=null  && points2d.length>0){
            NTxPoint2D[][] holes = NTx2DUtils.asPoint2DArray2Or1(node.getPropertyValue("holes").orNull()).orElse(new NTxPoint2D[0][]);
            return new NtxElement3DPrism(
                    position,
                    new NTxPolygonWithHoles2DImpl(new NTxRing2DImpl(Arrays.asList(points2d)), Arrays.asList(holes).stream().map(x->new NTxRing2DImpl(Arrays.asList(x))).collect(Collectors.toList())),
                    thickness==null?0:thickness
            );
        }
        NTxPoint3D[] points3d = NtxShapes3dUtils.resolvePoints(node, NTxPropName.POINTS, "real-points", () -> new NTxPoint3D[0], b, mapper);
        boolean fill = NTxValue.ofProp(node, NTxPropName.FILL_BACKGROUND).asBoolean().orElse(true);
        boolean contour = NTxValue.ofProp(node, NTxPropName.DRAW_CONTOUR).asBoolean().orElse(true);

        NtxElement3DPolygon r = new NtxElement3DPolygon(Arrays.stream(points3d).map(x -> NTx3DUtils.convertPoint(x, b)).toArray(NTxPoint3D[]::new), fill, contour);
        NtxShapes3dUtils.apply3dProps(node, r, rendererContext, b);
        return r;
    }

}
