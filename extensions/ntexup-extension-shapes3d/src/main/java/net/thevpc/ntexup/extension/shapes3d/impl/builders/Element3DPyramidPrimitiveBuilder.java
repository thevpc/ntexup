package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.lib.geometry3d.NtxFace;
import net.thevpc.ntexup.lib.geometry3d.impl.NTx3DUtils;
import net.thevpc.ntexup.lib.geometry3d.impl.composite.NtxElement3DGroup;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DPolygon;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DTriangle;
import net.thevpc.nuts.elem.NArrayElement;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Element3DPyramidPrimitiveBuilder implements NTxNodeBuilder, NtxElement3DNodeParser {
    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id("pyramid").alias("pyramid3d")
                .parseParam().matchesAny().end()
                .renderComponent(this::render);
    }

    public void render(NTxRendererContext rendererContext) {
        // do nothing in 2D
    }

    @Override
    public List<String> getId3d() {
        return Arrays.asList("pyramid", "pyramid3d");
    }

    @Override
    public NtxElement3D createElement3D(NTxRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NTxNode node = rendererContext.node();
        NTxPoint3D position = NtxShapes3dUtils.resolvePosition3D(node, NTxPropName.POSITION, rendererContext, b).orElse(NTxPoint3D.ofZero());
        NTxPoint3D size = NtxShapes3dUtils.resolveDistance(node, NTxPropName.SIZE, rendererContext, b).orElse(NTxPoint3D.ofHundred());

        NTxPoint3D p0 = new NTxPoint3D(position.x, position.y, position.z);
        NTxPoint3D p1 = new NTxPoint3D(position.x + size.x, position.y, position.z);
        NTxPoint3D p2 = new NTxPoint3D(position.x + size.x, position.y, position.z + size.z);
        NTxPoint3D p3 = new NTxPoint3D(position.x, position.y, position.z + size.z);
        NTxPoint3D apex = new NTxPoint3D(position.x + size.x / 2.0, position.y + size.y, position.z + size.z / 2.0);

        NtxElement3DPolygon base = new NtxElement3DPolygon(new NTxPoint3D[]{p0, p1, p2, p3}, true, true);
        NtxElement3DTriangle sideBack = new NtxElement3DTriangle(p0, apex, p1, true, true);
        NtxElement3DTriangle sideRight = new NtxElement3DTriangle(p1, apex, p2, true, true);
        NtxElement3DTriangle sideFront = new NtxElement3DTriangle(p2, apex, p3, true, true);
        NtxElement3DTriangle sideLeft = new NtxElement3DTriangle(p3, apex, p0, true, true);

        NtxElement3D[] faces = new NtxElement3D[]{base, sideBack, sideRight, sideFront, sideLeft};
        String[] faceNames = new String[]{"base", "back", "right", "front", "left"};

        NElement facesElem = rendererContext.computePropertyValue("faces").orNull();
        NObjectElement facesObj = (facesElem != null && facesElem.isAnyObject()) ? facesElem.asObject().get() : null;
        NArrayElement params = facesObj != null ? NElement.ofArray(facesObj.params().orElse(new ArrayList<>()).toArray(new NElement[0])) : null;

        NtxElement3DGroup grp = new NtxElement3DGroup();
        NtxShapes3dUtils.apply3dProps(node, grp, rendererContext, b, true);

        for (int i = 0; i < faces.length; i++) {
            NtxElement3D faceShape = faces[i];
            String name = faceNames[i];
            NTx3DUtils.copyProps(grp, faceShape, name);
            NElement fElem = rendererContext.computePropertyValue(name).orNull();
            if (fElem == null && name.equals("base")) {
                fElem = rendererContext.computePropertyValue("bottom").orNull();
            }
            if (fElem == null && facesObj != null) {
                fElem = facesObj.get(name).orNull();
                if (fElem == null && params != null) {
                    fElem = params.get(name).orNull();
                }
            }
            NtxFace f = fElem != null ? NtxShapes3dUtils.resolveFace(fElem, rendererContext) : null;
            if (f != null) {
                if (!f.isVisible()) {
                    continue;
                }
                NTx3DUtils.copyNonNullProps(f, faceShape);
            }
            grp.add(faceShape);
        }
        return grp;
    }
}
