package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry3d.*;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.lib.geometry3d.impl.NTx3DUtils;
import net.thevpc.ntexup.lib.geometry3d.impl.composite.NTxMesh3D;
import net.thevpc.ntexup.lib.geometry3d.impl.composite.NtxElement3DUVSphere;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DTriangle;

import java.util.*;

public class Element3DUVSpherePrimitiveBuilder implements NTxElement3DRenderer, NTxNodeBuilder, NtxElement3DNodeParser {
    @Override
    public Class<? extends NtxElement3D> forType() {
        return NtxElement3DUVSphere.class;
    }


    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id("uvsphere").alias("sphere3d")
                .parseParam().matchesAny().end()
                .renderComponent(this::render);
    }

    @Override
    public List<String> getId3d() {
        return Arrays.asList(
                "sphere",
                "uvsphere",
                "uv-sphere",
                "sphere",
                "sphere3d"
        );
    }

    public void render(NTxNodeRendererContext rendererContext) {
        //do nothing in 2D
    }

    @Override
    public NtxElement3DPrimitive[] toPrimitives(NtxElement3D e, NTxRenderState3D renderState) {
        NtxElement3DUVSphere ee = (NtxElement3DUVSphere) e;
        NTxPoint3D origin = ee.getPosition();
        double radiusX = ee.getRadiusX();
        double radiusY = ee.getRadiusY();
        double radiusZ = ee.getRadiusZ();
        int meridians = ee.getMeridians();
        int parallels = ee.getParallels();
        boolean showMesh = ee.isShowMesh();
        NTxMesh3D mesh = new NTxMesh3D();
        mesh.getVertices().add(new NTxPoint3D(origin.x, origin.y + radiusY, origin.z));
        for (int j = 0; j < parallels - 1; ++j) {
            double polar = Math.PI * (j + 1) / (parallels);
            double sp = Math.sin(polar);
            double cp = Math.cos(polar);
            for (int i = 0; i < meridians; ++i) {
                double azimuth = 2.0 * Math.PI * (i) / (meridians);
                double sa = Math.sin(azimuth);
                double ca = Math.cos(azimuth);
                double x = sp * ca * radiusX + origin.x;
                double y = cp * radiusY + origin.x;
                double z = sp * sa * radiusZ + origin.x;
                mesh.getVertices().add(new NTxPoint3D(x, y, z));
            }
        }
        mesh.getVertices().add(new NTxPoint3D(origin.x, origin.y - radiusY, origin.z));

        for (int i = 0; i < meridians; ++i) {
            int a = i + 1;
            int b = (i + 1) % meridians + 1;
            mesh.addTriangle(0, b, a);
        }

        for (int j = 0; j < parallels - 2; ++j) {
            int aStart = j * meridians + 1;
            int bStart = (j + 1) * meridians + 1;
            for (int i = 0; i < meridians; ++i) {
                int a = aStart + i;
                int a1 = aStart + (i + 1) % meridians;
                int b = bStart + i;
                int b1 = bStart + (i + 1) % meridians;
                mesh.addQuad(a, a1, b1, b);
            }
        }

        for (int i = 0; i < meridians; ++i) {
            int a = i + meridians * (parallels - 2) + 1;
            int b = (i + 1) % meridians + meridians * (parallels - 2) + 1;
            mesh.addTriangle(mesh.getVertices().size() - 1, a, b);
        }

        mesh.sortByZ();

        List<NtxElement3DPrimitive> elements = new ArrayList<>();
        int t = mesh.size();
        HashSet<NtxElement3D> contours = new HashSet<>();
        for (int i = 0; i < t; i++) {
            NtxElement3DTriangle tt = mesh.triangleAt(i);
            tt.setContour(showMesh);
            tt.setFilled(true);

            NTx3DUtils.copyProps(e, tt, "face" + (i + 1));

            elements.add(tt);
            NTxPoint3D p1 = tt.getP1();
            NTxPoint3D p2 = tt.getP2();
            NTxPoint3D p3 = tt.getP3();
//            if (showMesh) {
//                for (Element3DPrimitive e : new Element3DPrimitive[]{
//                        Element3DFactory.line(p1, p2),
//                        Element3DFactory.line(p2, p3),
//                        Element3DFactory.line(p3, p1),
//                }) {
//                    if (contours.add(e)) {
//                        elements.add(e);
//                    }
//                }
//            }
        }
        return elements.toArray(new NtxElement3DPrimitive[0]);
    }

    @Override
    public NtxElement3D createElement3D(NTxNode node, NTxNodeRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NTxPoint3D position = NtxShapes3dUtils.resolvePoint(node, NTxPropName.POSITION, "real-position", NTxPoint3D::ofZero, b, mapper);
        NTxPoint3D radius = NtxShapes3dUtils.resolvePoint(node, "radius", "real-radius", NTxPoint3D::ofOne, b, mapper);
        int meridians = NTxValue.ofProp(node, "meridians").asInt().orElse(60);
        int parallels = NTxValue.ofProp(node, "parallels").asInt().orElse(60);
        NtxElement3D r = NTxElement3DFactory.sphereUV(position, radius.x, radius.y, radius.y, meridians, parallels);
        NtxShapes3dUtils.apply3dProps(node, r, rendererContext, b);
        return r;
    }
}
