package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.extension.shapes3d.api.NtxElement3DPrimitive;
import net.thevpc.ntexup.extension.shapes3d.api.NtxElement3D;
import net.thevpc.ntexup.extension.shapes3d.api.NTxPoint3D;
import net.thevpc.ntexup.extension.shapes3d.api.NTxRenderState3D;
import net.thevpc.ntexup.extension.shapes3d.api.composite.NtxElement3DSurface;
import net.thevpc.ntexup.extension.shapes3d.api.primitives.NtxElement3DTriangle;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxMinMax;
import net.thevpc.ntexup.extension.shapes3d.api.NTxElement3DRenderer;
import net.thevpc.ntexup.extension.shapes3d.impl.NTx3DUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;
import org.locationtech.jts.triangulate.quadedge.QuadEdgeSubdivision;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Element3DSurfacePrimitiveBuilder implements NTxElement3DRenderer , NTxNodeBuilder {
    @Override
    public Class<? extends NtxElement3D> forType() {
        return NtxElement3DSurface.class;
    }

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id("surface3d")
                .parseParam().matchesAny().end()
                .renderComponent(this::render);
    }

    public void render(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        //do nothing in 2D
    }

    @Override
    public NtxElement3DPrimitive[] toPrimitives(NtxElement3D e, NTxRenderState3D renderState) {
        NtxElement3DSurface ee = (NtxElement3DSurface) e;
        NTxPoint3D[] points = ee.getPoints();
        // Perform Delaunay triangulation
        DelaunayTriangulationBuilder builder = new DelaunayTriangulationBuilder();
        builder.setSites(Arrays.asList(points).stream().map(x -> new Coordinate(x.x, x.y, x.z)).collect(Collectors.toList()));
        NTxMinMax m = NTx3DUtils.minMaxZ(points);
        QuadEdgeSubdivision subdivision = builder.getSubdivision();
        // Get the triangles as JTS Geometry objects
        GeometryCollection triangles = (GeometryCollection) subdivision.getTriangles(new GeometryFactory());
        int numGeometries = triangles.getNumGeometries();
        List<NtxElement3DPrimitive> elements = new ArrayList<>();
        for (int i = 0; i < numGeometries; i++) {
            Polygon geometryN = (Polygon) triangles.getGeometryN(i);
            Coordinate[] coordinates = geometryN.getCoordinates();
            double z = (coordinates[0].z + coordinates[1].z + coordinates[2].z) / 3;
            NtxElement3DTriangle tt = new NtxElement3DTriangle(
                    new NTxPoint3D(coordinates[0].x, coordinates[0].y, coordinates[0].z)
                    , new NTxPoint3D(coordinates[1].x, coordinates[1].y, coordinates[1].z)
                    , new NTxPoint3D(coordinates[2].x, coordinates[2].y, coordinates[2].z)
                    , true, true
            );
            NTx3DUtils.copyProps(e,tt,"face"+(i+1));
            tt.setBackgroundPaint(
                    Color.getHSBColor(
                            (float) m.ratio(z),
                            1f,
                            1f
                    )
            );
            elements.add(tt);
        }
        return elements.toArray(new NtxElement3DPrimitive[0]);
    }
}
