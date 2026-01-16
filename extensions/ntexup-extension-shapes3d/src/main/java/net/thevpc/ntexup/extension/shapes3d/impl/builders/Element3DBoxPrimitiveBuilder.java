package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry3d.*;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.lib.geometry3d.impl.NTx3DUtils;
import net.thevpc.ntexup.lib.geometry3d.impl.composite.NtxElement3DBox;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DPolygon;
import net.thevpc.nuts.elem.NArrayElement;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Element3DBoxPrimitiveBuilder implements NTxElement3DRenderer, NTxNodeBuilder, NtxElement3DNodeParser {
    @Override
    public Class<? extends NtxElement3D> forType() {
        return NtxElement3DBox.class;
    }

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id("box").alias("box3d")
                .parseParam().matchesAny().end()
                .renderComponent(this::render);
    }

    @Override
    public List<String> getId3d() {
        return Arrays.asList("box", "box3d");
    }

    public void render(NTxNodeRendererContext rendererContext) {
        //do nothing in 2D
    }

    @Override
    public NtxElement3D createElement3D(NTxNode node, NTxNodeRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NTxPoint3D position = NtxShapes3dUtils.resolvePoint(node, NTxPropName.POSITION, "real-position", NTxPoint3D::ofZero, b, mapper);
        NTxPoint3D size = NtxShapes3dUtils.resolvePoint(node, NTxPropName.SIZE, "real-size", NTxPoint3D::ofHundred, b, mapper);
        NtxElement3DBox r = (NtxElement3DBox) NTxElement3DFactory.box(position, size.x, size.y, size.z);
        NtxShapes3dUtils.apply3dProps(node, r, rendererContext, b);
        NElement faces = rendererContext.computePropertyValue(node, "faces").orNull();
        if (faces == null) {
            r.setTop(NtxShapes3dUtils.resolveFace(rendererContext.computePropertyValue(node, "top").orNull(), rendererContext));
            r.setBottom(NtxShapes3dUtils.resolveFace(rendererContext.computePropertyValue(node, "bottom").orNull(), rendererContext));
            r.setLeft(NtxShapes3dUtils.resolveFace(rendererContext.computePropertyValue(node, "left").orNull(), rendererContext));
            r.setRight(NtxShapes3dUtils.resolveFace(rendererContext.computePropertyValue(node, "right").orNull(), rendererContext));
            r.setFront(NtxShapes3dUtils.resolveFace(rendererContext.computePropertyValue(node, "front").orNull(), rendererContext));
            r.setBack(NtxShapes3dUtils.resolveFace(rendererContext.computePropertyValue(node, "back").orNull(), rendererContext));

            for (String s : new String[]{"top", "bottom", "left", "right", "front", "back"}) {
                NtxFace f = NtxShapes3dUtils.resolveFace(rendererContext.computePropertyValue(node, s).orNull(), rendererContext);
                setBoxFace(s, r, f);
            }
        } else if (faces.isAnyObject()) {
            NObjectElement o = faces.asObject().get();
            NArrayElement params = NElement.ofArray(o.params().orElse(new ArrayList<>()).toArray(new NElement[0]));
            for (String s : new String[]{"top", "bottom", "left", "right", "front", "back"}) {
                NElement ee = o.get(s).orNull();
                if (ee == null) {
                    ee = params.get(s).orNull();
                }
                setBoxFace(s, r, NtxShapes3dUtils.resolveFace(ee, rendererContext));
            }
        }
        return r;
    }


    public static void setBoxFace(String s, NtxElement3DBox r, NtxFace f) {
        switch (s) {
            case "top": {
                r.setTop(f);
                break;
            }
            case "bottom": {
                r.setBottom(f);
                break;
            }
            case "back": {
                r.setBack(f);
                break;
            }
            case "front": {
                r.setFront(f);
                break;
            }
            case "left": {
                r.setLeft(f);
                break;
            }
            case "right": {
                r.setRight(f);
                break;
            }
        }
    }

    @Override
    public NtxElement3DPrimitive[] toPrimitives(NtxElement3D e, NTxRenderState3D renderState) {
        NtxElement3DBox ee = (NtxElement3DBox) e;
        NTxPoint3D origin = ee.getPosition();
        double sizeX = ee.getSizeX();
        double sizeY = ee.getSizeY();
        double sizeZ = ee.getSizeZ();
        NTxPoint3D[] vertices = {
                new NTxPoint3D(origin.x, origin.y, origin.z),
                new NTxPoint3D(origin.x + sizeX, origin.y, origin.z),
                new NTxPoint3D(origin.x + sizeX, origin.y + sizeY, origin.z),
                new NTxPoint3D(origin.x, origin.y + sizeY, origin.z),
                new NTxPoint3D(origin.x, origin.y, origin.z + sizeZ),
                new NTxPoint3D(origin.x + sizeX, origin.y, origin.z + sizeZ),
                new NTxPoint3D(origin.x + sizeX, origin.y + sizeY, origin.z + sizeZ),
                new NTxPoint3D(origin.x, origin.y + sizeY, origin.z + sizeZ)
        };
        // Define the edges of the cube
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0}, // back face
                {4, 5}, {5, 6}, {6, 7}, {7, 4}, // front face
                {0, 4}, {1, 5}, {2, 6}, {3, 7}  // connecting edges
        };
        int[][] surfaces = {
                {0, 1, 2, 3}, //FRONT
                {4, 5, 6, 7}, // BACK
                {0, 4, 5, 1}, //TOP
                {3, 2, 6, 7}, //BOTTOM
                {1, 2, 6, 5}, //RIGHT
                {0, 3, 7, 4}, //LEFT
        };
        List<NtxElement3DPrimitive> elements = new ArrayList<>();
        for (int i = 0; i < surfaces.length; i++) {
            NtxFace face = null;
            switch (i) {
                case 0: {
                    face = ee.getBottom();
                    break;
                }
                case 1: {
                    face = ee.getTop();
                    break;
                }
                case 2: {
                    face = ee.getBack();
                    break;
                }
                case 3: {
                    face = ee.getFront();
                    break;
                }
                case 4: {
                    face = ee.getRight();
                    break;
                }
                case 5: {
                    face = ee.getLeft();
                    break;
                }
            }
            if (face != null && !face.isVisible()) {
                continue;
            }
            int[] surface = surfaces[i];
            NTxPoint3D[] points = new NTxPoint3D[surface.length];
            for (int j = 0; j < points.length; j++) {
                points[j] = vertices[surface[j]];
            }
            NtxElement3DPolygon tt = new NtxElement3DPolygon(points, true, false);
            NTx3DUtils.copyProps(e, tt, "face" + (i + 1));
            NTx3DUtils.copyNonNullProps(face, tt);
            elements.add(tt);
        }
//        for (int i = 0; i < edges.length; i++) {
//            int[] edge = edges[i];
//            NtxElement3DLine tt = NTxElement3DFactory.line(vertices[edge[0]], vertices[edge[1]]);
//            NTx3DUtils.copyProps(e,tt,"edge"+(i+1));
//            elements.add(tt);
//        }
        return elements.toArray(new NtxElement3DPrimitive[0]);
    }
}
