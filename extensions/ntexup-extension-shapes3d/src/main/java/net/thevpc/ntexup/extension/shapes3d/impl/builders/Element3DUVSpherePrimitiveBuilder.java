package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.extension.shapes3d.api.NtxElement3DPrimitive;
import net.thevpc.ntexup.extension.shapes3d.api.NtxElement3D;
import net.thevpc.ntexup.extension.shapes3d.api.NTxPoint3D;
import net.thevpc.ntexup.extension.shapes3d.api.NTxRenderState3D;
import net.thevpc.ntexup.extension.shapes3d.api.composite.NtxElement3DUVSphere;
import net.thevpc.ntexup.extension.shapes3d.api.composite.NTxMesh3D;
import net.thevpc.ntexup.extension.shapes3d.api.primitives.NtxElement3DTriangle;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.extension.shapes3d.api.NTxElement3DRenderer;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.extension.shapes3d.impl.NTx3DUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Element3DUVSpherePrimitiveBuilder implements NTxElement3DRenderer , NTxNodeBuilder {
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

    public void render(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        //do nothing in 2D
    }

    @Override
    public NtxElement3DPrimitive[] toPrimitives(NtxElement3D e, NTxRenderState3D renderState) {
        NtxElement3DUVSphere ee = (NtxElement3DUVSphere) e;
        NTxPoint3D origin = ee.getOrigin();
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

            NTx3DUtils.copyProps(e,tt,"face"+(i+1));

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
}
