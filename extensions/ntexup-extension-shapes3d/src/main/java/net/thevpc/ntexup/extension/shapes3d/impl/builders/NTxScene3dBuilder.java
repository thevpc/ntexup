package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.lib.geometry3d.impl.DefaultNTx3DMesh;
import net.thevpc.ntexup.lib.geometry3d.impl.NTx3DUtils;
import net.thevpc.ntexup.lib.geometry3d.impl.NTxCamera3DImpl;
import net.thevpc.ntexup.lib.geometry3d.impl.NtxGraphics3DImpl;
import net.thevpc.ntexup.lib.geometry3d.impl.composite.*;
import net.thevpc.nuts.elem.*;

import java.util.Map;

/**
 *
 */
public class NTxScene3dBuilder implements NTxNodeBuilder {
    public NTxScene3dBuilder() {
    }

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id(NTxNodeType.SCENE3D)
                .parseParam().matchesNamedPair(
                        "camera",
                        "light-orientation",
                        "transform",
                        "rotate",
                        "translate",
                        "scale",
                        "rotate-vector",
                        "rotate-line",
                        "real-size"
                ).end()
                .renderComponent((rendererContext) -> render(rendererContext))
        ;
    }


    public void render(NTxNodeRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        NTxBounds2D b = rendererContext.selfBounds(node, null, null);
        NTxPoint3D realSize = NTx3DUtils.asPoint3D(node, "real-size").orNull();
        NElement c = NTxValue.ofProp(node, "camera").asElement().orNull();
        NTxCamera3DImpl camera = null;
        NtxElement3DNodeParserFactory nodeParsersMap = new NtxElement3DNodeParserFactory(rendererContext.engine());
        if (c != null) {
            if (c.isAnyString()) {
                switch (c.asStringValue().get()) {
                    case "isometric": {
                        camera = NTxCamera3DImpl.isometric();
                        break;
                    }
                }
            } else if (c.isAnyObject()) {
                NObjectElement o = c.asObject().get();
                Double azimuth = null;
                Double elevation = null;
                Double distance = null;
                NTxPoint3D position = null;
                NTxPoint3D target = null;
                for (NElement child : o.children()) {
                    if (child.isNamedPair()) {
                        NPairElement p = child.asPair().get();
                        switch (p.asStringValue().get()) {
                            case "azimuth": {
                                azimuth = p.value().asDoubleValue().get();
                                break;
                            }
                            case "elevation": {
                                elevation = p.value().asDoubleValue().get();
                                break;
                            }
                            case "distance": {
                                distance = p.value().asDoubleValue().get();
                                break;
                            }
                            case "position": {
                                position = NTx3DUtils.asPoint3D(NTxValue.of(p.value())).orNull();
                                break;
                            }
                            case "target": {
                                target = NTx3DUtils.asPoint3D(NTxValue.of(p.value())).orNull();
                                break;
                            }
                        }
                    }
                }
                if (azimuth != null || elevation != null || distance != null) {
                    camera = NTxCamera3DImpl.fromSpherical(azimuth == null ? -45 : azimuth, elevation == null ? 35.264 : elevation, distance == null ? 1000 : distance);
                } else if (position != null || target != null) {
                    camera = new NTxCamera3DImpl(position == null ? new NTxPoint3D(0, 0, 1000) : position, target == null ? NTxPoint3D.ofZero() : target);
                }
            }
        }
        if (camera == null) {
            camera = NTxCamera3DImpl.isometric();
        }
        NtxElement3DGroup g = new NtxElement3DGroup();
        for (NTxNode child : node.children()) {
            NtxElement3D cc = toNtxElement3D(child, rendererContext, b,
                    //fix me later!!
                    // should transform from real to relative (tow view) position
                    x -> x,
                    nodeParsersMap
            );
            if (cc != null) {
                g.add(cc);
            }
        }
        g.setTransform(NtxShapes3dUtils.resolveTransform(node, b));
        NtxGraphics3DImpl g3 = new NtxGraphics3DImpl(rendererContext.graphics(), rendererContext);
        g3.setMesh(new DefaultNTx3DMesh().configureScene(node, rendererContext));
        g3.setCamera(camera);
        g3.draw3D(g, new NTxPoint2D(b.getX(), b.getY()));
    }

    private NtxElement3D toNtxElement3D(NTxNode node, NTxNodeRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NtxElement3DNodeParser p = parserFactory.resolve(node.type()).orNull();
        if (p == null) {
            return null;
        }
        return p.createElement3D(node, rendererContext, b, mapper,parserFactory);
    }


}
