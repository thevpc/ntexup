package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.parser.NTxItemParser;
import net.thevpc.ntexup.api.parser.NTxNodeParser;
import net.thevpc.ntexup.api.util.NTxNumberUtils;
import net.thevpc.ntexup.engine.ext.NTxNodeBuilderContextImpl;
import net.thevpc.ntexup.engine.parser.NTxNodeParserDumb;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry3d.NTxNumberElement3;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.lib.geometry3d.impl.DefaultNTx3DMesh;
import net.thevpc.ntexup.lib.geometry3d.impl.NTx3DUtils;
import net.thevpc.ntexup.lib.geometry3d.impl.NTxCamera3DImpl;
import net.thevpc.ntexup.lib.geometry3d.impl.NtxGraphics3DImpl;
import net.thevpc.ntexup.lib.geometry3d.impl.composite.*;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

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
                        "real-size",
                        "real-position"
                ).end()
                .compileNode(this::compileNode)
                .renderComponent((rendererContext) -> render(rendererContext))
        ;
    }

    public void compileNode(NTxNode node, NTxResolutionContext context) {
        NtxElement3DNodeParserFactory parserFactory = new NtxElement3DNodeParserFactory(context.engine());
        NTxResolutionContext newContext = context.pushContext().withItemParser(new NTxItemParser() {
            @Override
            public NOptional<NTxNodeParser> nodeTypeParser(String id) {
                NtxElement3DNodeParser t = parserFactory.resolve(id).orNull();
                if (t != null) {
                    if (t instanceof NTxNodeBuilder) {
                        NTxNodeBuilder h = (NTxNodeBuilder) t;
                        NTxNodeBuilderContextImpl b = new NTxNodeBuilderContextImpl(h, context.engine());
                        h.build(b);
                        b.compile();
                        NTxNodeParser p = b.createParser();
                        if (p != null) {
                            return NOptional.of(p);
                        }
                    }
                    return NOptional.of(new NTxNodeParserDumb(true, id));
                }
                return NOptional.ofNamedEmpty(NMsg.ofC("parser for %s", id));
            }
        });
        NTxEngine engine = context.engine();
        engine.defaultCompileNodeProperties(node, newContext);
        engine.defaultCompileNodeChildren(node, newContext);
    }

    public void render(NTxRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        NTxBounds2D bounds2D = rendererContext.selfBounds2D();
        NTxNumberElement3 realSize = NTx3DUtils.asNumberElement3(NTxValue.ofProp(node, "real-size"),rendererContext).orNull();
        NTxNumberElement3 realPosition = NTx3DUtils.asNumberElement3(NTxValue.ofProp(node, "real-position"),rendererContext).orNull();
        if (realPosition == null) {
            realPosition = NTxNumberElement3.ofZero();
        }
        if (realSize == null) {
            realSize = NTxNumberElement3.ofOne();
        }

        NTxBounds3D realBounds = NTxBounds3D.ofWidth(
                NTxNumberUtils.toMeter(realPosition.x).orElse(1.0),
                NTxNumberUtils.toMeter(realPosition.y).orElse(1.0),
                NTxNumberUtils.toMeter(realPosition.z).orElse(1.0),
                NTxNumberUtils.toMeter(realSize.x).orElse(1.0),
                NTxNumberUtils.toMeter(realSize.y).orElse(1.0),
                NTxNumberUtils.toMeter(realSize.z).orElse(1.0)
        );
        rendererContext = rendererContext
                .withRealGlobalBounds3D(realBounds)
                .withRealBounds3D(realBounds)
        ;
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
                                NOptional<Double> t = NtxShapes3dUtils.resolveDistanceZAny(p.value(), rendererContext, bounds2D);
                                if (t.isPresent()) {
                                    distance = t.get();
                                }
                                break;
                            }
                            case "position": {
                                NOptional<NTxPoint3D> t = NtxShapes3dUtils.resolvePosition3DAny(p.value(), rendererContext, bounds2D);
                                if (t.isPresent()) {
                                    position = t.get();
                                }
                                break;
                            }
                            case "target": {
                                NOptional<NTxPoint3D> t = NtxShapes3dUtils.resolvePosition3DAny(p.value(), rendererContext, bounds2D);
                                if (t.isPresent()) {
                                    target = t.get();
                                }
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
            rendererContext.doWithChild(child, rc -> {
                NtxElement3D cc = toNtxElement3D((NTxRendererContext) rc, bounds2D,
                        //fix me later!!
                        // should transform from real to relative (tow view) position
                        x -> x,
                        nodeParsersMap
                );
                if (cc != null) {
                    g.add(cc);
                }
            });
        }
        g.setTransform(NtxShapes3dUtils.resolveTransform(node, bounds2D));
        NtxGraphics3DImpl g3 = new NtxGraphics3DImpl(rendererContext.graphics(), rendererContext);
        g3.setMesh(new DefaultNTx3DMesh().configureScene(node, rendererContext));
        g3.setCamera(camera);
        g3.draw3D(g, new NTxPoint2D(bounds2D.minX(), bounds2D.minY()));
    }

    private NtxElement3D toNtxElement3D(NTxRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NtxElement3DNodeParser p = parserFactory.resolve(rendererContext.node().type()).orNull();
        if (p == null) {
            return null;
        }
        return p.createElement3D(rendererContext, b, mapper, parserFactory);
    }


}
