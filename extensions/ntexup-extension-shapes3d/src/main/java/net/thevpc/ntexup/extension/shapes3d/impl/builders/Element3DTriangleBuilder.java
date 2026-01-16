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
import net.thevpc.ntexup.lib.geometry2d.impl.NTxTriangle2DImpl;
import net.thevpc.ntexup.lib.geometry3d.NTxElement3DFactory;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.lib.geometry3d.impl.NTx3DUtils;
import net.thevpc.ntexup.lib.geometry3d.impl.composite.NtxElement3DPrism;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DLine;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DTriangle;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Element3DTriangleBuilder implements  NtxElement3DNodeParser {
    @Override
    public List<String> getId3d() {
        return Arrays.asList(NTxNodeType.LINE);
    }

    @Override
    public NtxElement3D createElement3D(NTxNode node, NTxNodeRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NTxPoint3D position = NTx3DUtils.asPoint3D(NTxValue.of(node.getPropertyValue(NTxPropName.POSITION))).orNull();
        Double thickness = NTxValue.of(node.getPropertyValue("thickness")).asDouble().orNull();
        NTxPoint2D[] points2d = NTx2DUtils.asPoint2DArray(node.getPropertyValue(NTxPropName.POINTS).orNull()).orNull();
        if(points2d!=null  && points2d.length>=3){
            return new NtxElement3DPrism(
                    position,
                    new NTxTriangle2DImpl(points2d[0],points2d[1],points2d[2]),
                    thickness==null?0:thickness
            );
        }

        NTxPoint3D[] points = NtxShapes3dUtils.resolvePoints(node, NTxPropName.POINTS, "real-points", () -> new NTxPoint3D[0], b, mapper);
        if (points.length < 3) {
            points = Arrays.copyOf(points, 3);
            for (int i = 0; i < 3; i++) {
                if (points[i] == null) {
                    points[i] = NTx3DUtils.convertPoint(NTxPoint3D.ofZero(), b);
                }
            }
        }
        boolean fill = NTxValue.ofProp(node, NTxPropName.FILL_BACKGROUND).asBoolean().orElse(true);
        boolean contour = NTxValue.ofProp(node, NTxPropName.DRAW_CONTOUR).asBoolean().orElse(true);
        NtxElement3DTriangle r = new NtxElement3DTriangle(points[0], points[1], points[2], fill, contour);
        NtxShapes3dUtils.apply3dProps(node, r, rendererContext, b);
        return r;
    }

}
