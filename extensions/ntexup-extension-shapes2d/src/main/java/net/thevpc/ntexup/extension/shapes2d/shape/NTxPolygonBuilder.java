package net.thevpc.ntexup.extension.shapes2d.shape;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxEngineTools;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.parser.NTxArgumentReader;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NPairElement;
import net.thevpc.nuts.util.NOptional;

import java.util.Arrays;

public class NTxPolygonBuilder implements NTxNodeBuilder {
    private NTxProperties defaultStyles = new NTxProperties();
    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.POLYGON)
                .parseParam().matchesNamedPair(NTxPropName.COUNT).then()
                .parseParam(new NTxNodeBuilderContext.ProcessParamAction() {
                    @Override
                    public boolean processParam(NTxArgumentReader info, NTxNodeBuilderContext buildContext) {
                        NElement k = info.peek();
                        NTxEngineTools tools = buildContext.engine().tools();
                        if (k != null) {
                            if(k.isInt()) {
                                info.read();
                                info.node().setProperty(NTxPropName.COUNT,k);
                                return true;
                            }else if(k.isNamedPair()) {
                                NPairElement kk=k.asPair().get();
                                String ks = kk.key().asStringValue().orNull();
                                if (NTxPropName.POINT.equals(ks)) {
                                    buildContext.addParamName(NTxPropName.POINTS);
//                                    if (buildContext.isAncestorScene3D(info.node())) {
//                                        NOptional<NTxPoint3D> p2d = NTxValue.of(kk.value()).asHPoint3D();
//                                        if (p2d.isPresent()) {
//                                            tools.addPoint(info.node(), p2d.get());
//                                            info.read();
//                                            return true;
//                                        } else {
//                                            return false;
//                                        }
//                                    } else {
                                        NOptional<NTxPoint2D> p2d = NTxValue.of(kk.value()).asPoint2D();
                                        if (p2d.isPresent()) {
                                            tools.addPoint(info.node(), p2d.get());
                                            info.read();
                                            return true;
                                        } else {
                                            return false;
                                        }
//                                    }
                                } else if (NTxPropName.POINTS.equals(ks)) {
                                    buildContext.addParamName(NTxPropName.POINTS);
//                                    if (buildContext.isAncestorScene3D(info.node())) {
//                                        NOptional<NTxPoint3D[]> p2d = NTxValue.of(kk.value()).asPoint3DArray();
//                                        if (p2d.isPresent()) {
//                                            tools.addPoints(info.node(), p2d.get());
//                                            info.read();
//                                            return true;
//                                        } else {
//                                            return false;
//                                        }
//                                    } else {
                                        NOptional<NTxPoint2D[]> p2d = NTxValue.of(kk.value()).asPoint2DArray();
                                        if (p2d.isPresent()) {
                                            info.read();
                                            tools.addPoints(info.node(), p2d.get());
                                            return true;
                                        } else {
                                            return false;
                                        }
//                                    }
                                }
                            }else if(k.isUplet()){
//                                if (buildContext.isAncestorScene3D(info.node())) {
//                                    NOptional<NTxPoint3D> p2d = NTxValue.of(k).asHPoint3D();
//                                    if (p2d.isPresent()) {
//                                        buildContext.addParamName(NTxPropName.POINTS);
//                                        tools.addPoint(info.node(), p2d.get());
//                                        info.read();
//                                        return true;
//                                    } else {
//                                        return false;
//                                    }
//                                } else {
                                    NOptional<NTxPoint2D> p2d = NTxValue.of(k).asPoint2D();
                                    if (p2d.isPresent()) {
                                        buildContext.addParamName(NTxPropName.POINTS);
                                        tools.addPoint(info.node(), p2d.get());
                                        info.read();
                                        return true;
                                    } else {
                                        return false;
                                    }
//                                }
                            }
                        }
                        return false;
                    }
                })
                .renderComponent((rendererContext, buildContext) -> render(rendererContext, buildContext));
    }

    private void render(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext buildContext) {
        NTxNode node = rendererContext.node();
        Integer count = NTxValue.ofProp(node, NTxPropName.COUNT).asInt().orNull();
        if (buildContext.isAncestorScene3D(node)) {
            //really, where is 3D!!
            NTxPoint2D[] points = NTxValue.ofProp(node, NTxPropName.POINTS).asPoint2DArray().orNull();
            if(points!=null && points.length>=3) {
                if (count != null && count >= 3 && count > points.length) {
                    points = Arrays.copyOf(points, count);
                }
                NTxPolygonHelper.renderPoints(points, rendererContext);
            }else{
                NTxPolygonHelper.renderPointsCount(count==null?3:count.intValue(), rendererContext, defaultStyles);
            }
        }else{
            NTxPoint2D[] points = NTxValue.ofProp(node, NTxPropName.POINTS).asPoint2DArray().orNull();
            if(points!=null && points.length>=3) {
                if (count != null && count >= 3 && count > points.length) {
                    points = Arrays.copyOf(points, count);
                }
                NTxPolygonHelper.renderPoints(points, rendererContext);
            }else{
                NTxPolygonHelper.renderPointsCount(count==null?3:count.intValue(), rendererContext, defaultStyles);
            }
        }
    }

}
