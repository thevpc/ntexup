package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry2d.NTxElement2DFactory;
import net.thevpc.ntexup.lib.geometry2d.NTxPolygonWithHoles2D;
import net.thevpc.ntexup.lib.geometry2d.NTxRegion2D;
import net.thevpc.ntexup.lib.geometry2d.NTxRing2D;
import net.thevpc.ntexup.lib.geometry3d.*;
import net.thevpc.ntexup.lib.geometry3d.impl.composite.NtxElement3DPrism;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DPolygon;
import net.thevpc.nuts.elem.NArrayElement;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElement;

import java.util.*;

public class Element3DPrismPrimitiveBuilder implements NTxElement3DRenderer, NTxNodeBuilder, NtxElement3DNodeParser {
    @Override
    public Class<? extends NtxElement3D> forType() {
        return NtxElement3DPrism.class;
    }

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .ids(getId3d().toArray(new String[0]))
                .parseParam().matchesAny().end()
                .renderComponent(this::render);
    }

    @Override
    public List<String> getId3d() {
        return Arrays.asList("prism", "prism3d");
    }

    public void render(NTxNodeRendererContext rendererContext) {
        //do nothing in 2D
    }

    @Override
    public NtxElement3DPrimitive[] toPrimitives(NtxElement3D e, NTxRenderState3D renderState) {
        NtxElement3DPrism ee = (NtxElement3DPrism) e;
        NTxPoint3D origin = ee.position();
        NTxRegion2D region2d = ee.region();
        List<NtxElement3DPrimitive> elements = new ArrayList<>();


        List<NTxPolygonWithHoles2D> polys = region2d.toPolygonsWithHolesInternal();

        // Now proceed exactly as in the earlier implementation...
        double z0 = origin.z;
        double z1 = origin.z + ee.thickness();

        for (NTxPolygonWithHoles2D poly : polys) {
            if (ee.getBottom() == null || ee.getBottom().isVisible()) {
                // Bottom face (reversed)
                List<NTxPoint3D> bottom = to3D(poly.exterior().getPoints(), origin.x, origin.y, z0);
                Collections.reverse(bottom);
                elements.add(new NtxElement3DPolygon(bottom.toArray(new NTxPoint3D[0]), true, false));
            }


            if (ee.getTop() == null || ee.getTop().isVisible()) {
                // Top face
                List<NTxPoint3D> top = to3D(poly.exterior().getPoints(), origin.x, origin.y, z1);
                elements.add(new NtxElement3DPolygon(top.toArray(new NTxPoint3D[0]), true, false));
            }
            if (ee.getSide() == null || ee.getSide().isVisible()) {
                // Side walls
                addVerticalWalls(poly.exterior().getPoints(), origin.x, origin.y, z0, z1, elements, false);
                for (NTxRing2D hole : poly.holes()) {
                    addVerticalWalls(hole.getPoints(), origin.x, origin.y, z0, z1, elements, true);
                }
            }
        }
        return elements.toArray(new NtxElement3DPrimitive[0]);

    }

    private List<NTxPoint3D> to3D(List<NTxPoint2D> points2d, double offsetX, double offsetY, double z) {
        List<NTxPoint3D> points3d = new ArrayList<>(points2d.size());
        for (NTxPoint2D p : points2d) {
            points3d.add(new NTxPoint3D(p.x + offsetX, p.y + offsetY, z));
        }
        return points3d;
    }

    // Overload for default (exterior = no reverse)
    private void addVerticalWalls(
            List<NTxPoint2D> ring2d,
            double offsetX, double offsetY,
            double z0, double z1,
            List<NtxElement3DPrimitive> target
    ) {
        addVerticalWalls(ring2d, offsetX, offsetY, z0, z1, target, false);
    }

    private void addVerticalWalls(
            List<NTxPoint2D> ring2d,
            double offsetX, double offsetY,
            double z0, double z1,
            List<NtxElement3DPrimitive> target,
            boolean reverseWinding
    ) {
        int n = ring2d.size();
        if (n < 2) return;

        // Ensure ring is treated as closed (even if last != first)
        boolean closed = ring2d.get(0).equals(ring2d.get(n - 1));
        int loopEnd = closed ? n - 1 : n;

        for (int i = 0; i < loopEnd; i++) {
            int j = (i + 1) % loopEnd;
            if (closed && j == 0) j = loopEnd; // avoid wrap if already closed

            NTxPoint2D p0 = ring2d.get(i);
            NTxPoint2D p1 = ring2d.get(j % ring2d.size());

            NTxPoint3D v0 = new NTxPoint3D(p0.x + offsetX, p0.y + offsetY, z0);
            NTxPoint3D v1 = new NTxPoint3D(p1.x + offsetX, p1.y + offsetY, z0);
            NTxPoint3D v2 = new NTxPoint3D(p1.x + offsetX, p1.y + offsetY, z1);
            NTxPoint3D v3 = new NTxPoint3D(p0.x + offsetX, p0.y + offsetY, z1);

            NTxPoint3D[] quad;
            if (reverseWinding) {
                // For holes: inward-facing normals → reverse vertex order
                quad = new NTxPoint3D[]{v0, v3, v2, v1};
            } else {
                // For exterior: outward-facing normals
                quad = new NTxPoint3D[]{v0, v1, v2, v3};
            }

            // Split quad into two triangles if your renderer doesn't support quads
            // But assuming NtxElement3DPolygon supports quads:
            target.add(new NtxElement3DPolygon(quad, true, false));
        }
    }


    public static void setBoxFace(String s, NtxElement3DPrism r, NtxFace f) {
        switch (s) {
            case "top": {
                r.setTop(f);
                break;
            }
            case "bottom": {
                r.setBottom(f);
                break;
            }
            case "side": {
                r.setSide(f);
                break;
            }
        }
    }

    @Override
    public NtxElement3D createElement3D(NTxNode node, NTxNodeRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NTxPoint3D position = NtxShapes3dUtils.resolvePoint(node, NTxPropName.POSITION, "real-position", NTxPoint3D::ofZero, b, mapper);
        double thickness = NTxValue.ofProp(node, "thickness").asDouble().orElse(100.0);
        NTxRegion2D region = NTxElement2DFactory.region(NTxValue.ofProp(node, "region").asElement().orNull()).orNull();
        if(region == null) {
            return null;
        }
        NtxElement3DPrism r = NTxElement3DFactory.prism(position, region, thickness);
        NtxShapes3dUtils.apply3dProps(node, r, rendererContext, b);
        NElement faces = rendererContext.computePropertyValue(node, "faces").orNull();
        if (faces == null) {
            r.setTop(NtxShapes3dUtils.resolveFace(rendererContext.computePropertyValue(node, "top").orNull(), rendererContext));
            r.setBottom(NtxShapes3dUtils.resolveFace(rendererContext.computePropertyValue(node, "bottom").orNull(), rendererContext));
            r.setSide(NtxShapes3dUtils.resolveFace(rendererContext.computePropertyValue(node, "side").orNull(), rendererContext));
            for (String s : new String[]{"top", "bottom", "side"}) {
                NtxFace f = NtxShapes3dUtils.resolveFace(rendererContext.computePropertyValue(node, s).orNull(), rendererContext);
                setBoxFace(s, r, f);
            }
        } else if (faces.isAnyObject()) {
            NObjectElement o = faces.asObject().get();
            NArrayElement params = NElement.ofArray(o.params().orElse(new ArrayList<>()).toArray(new NElement[0]));
            for (String s : new String[]{"top", "bottom", "side"}) {
                NElement ee = o.get(s).orNull();
                if (ee == null) {
                    ee = params.get(s).orNull();
                }
                setBoxFace(s, r, NtxShapes3dUtils.resolveFace(ee, rendererContext));
            }
        }
        return r;
    }
}
