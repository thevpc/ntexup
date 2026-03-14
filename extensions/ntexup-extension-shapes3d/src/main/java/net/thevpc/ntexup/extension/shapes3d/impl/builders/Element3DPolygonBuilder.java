package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry2d.NTx2DUtils;
import net.thevpc.ntexup.lib.geometry2d.impl.NTxPolygonWithHoles2DImpl;
import net.thevpc.ntexup.lib.geometry2d.impl.NTxRing2DImpl;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.lib.geometry3d.impl.NTx3DUtils;
import net.thevpc.ntexup.lib.geometry3d.impl.composite.NtxElement3DPrism;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DPolygon;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Element3DPolygonBuilder implements  NtxElement3DNodeParser {
    @Override
    public List<String> getId3d() {
        return Arrays.asList(NTxNodeType.POLYGON);
    }

    @Override
    public NtxElement3D createElement3D(NTxRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NTxNode node=rendererContext.node();
        NTxPoint3D position = NtxShapes3dUtils.resolvePosition3D(node, NTxPropName.POSITION, rendererContext, b).orElse(NTxPoint3D.ofZero());
        double thickness = Math.abs(NtxShapes3dUtils.resolveZDistance(node, "thickness", rendererContext, b).orElse(0.0));
        NTxPoint2D[] points2d = NtxShapes3dUtils.resolvePositions2D(node,NTxPropName.POINTS,rendererContext,b).orNull();
        if(points2d!=null  && points2d.length>0){
            NTxPoint2D[][] holes = NTx2DUtils.asPoint2DArray2Or1(node.getPropertyValue("holes").orNull()).orElse(new NTxPoint2D[0][]);
            return new NtxElement3DPrism(
                    position,
                    new NTxPolygonWithHoles2DImpl(new NTxRing2DImpl(Arrays.asList(points2d)), Arrays.asList(holes).stream().map(x->new NTxRing2DImpl(Arrays.asList(x))).collect(Collectors.toList())),
                    thickness
            );
        }
        NTxPoint3D[] points3d = NtxShapes3dUtils.resolvePositions3D(node, NTxPropName.POINTS, rendererContext,b).get();
        boolean fill = NTxValue.ofProp(node, NTxPropName.FILL_BACKGROUND).asBoolean().orElse(true);
        boolean contour = NTxValue.ofProp(node, NTxPropName.DRAW_CONTOUR).asBoolean().orElse(true);

        NtxElement3DPolygon r = new NtxElement3DPolygon(points3d, fill, contour);
        NtxShapes3dUtils.apply3dProps(node, r, rendererContext, b,fill);
        return r;
    }

}
