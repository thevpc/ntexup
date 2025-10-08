package net.thevpc.ntexup.extension.plot2d;

import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.extension.plot2d.model.NTxFunctionPlotInfo;
import net.thevpc.ntexup.extension.plot2d.model.NTxPlotSource;
import net.thevpc.ntexup.extension.plot2d.model.NTxPlotType;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NNameFormat;
import net.thevpc.nuts.util.NStringUtils;

import java.util.ArrayList;
import java.util.List;

public class NTxFunctionPlotInfoLoader {

//    public NTxGlobalPlotData loadGlobalPlotData(NTxNode p, NTxNodeRendererContext rendererContext, NTxNodeBuilderContext buildContext) {
//        NTxGlobalPlotData d = new NTxGlobalPlotData();
//        NTxEngine engine = rendererContext.engine();
//        class A {
//            Double asDouble(String name, Double d) {
//                return NTxValue.of(engine.evalExpression(p.getPropertyValue(name).orElse(NElement.ofDouble(d)), p, rendererContext.varProvider())).asDouble().orElse(d);
//            }
//
//            Boolean asBoolean(String name, Boolean d) {
//                return NTxValue.of(engine.evalExpression(p.getPropertyValue(name).orElse(NElement.ofBoolean(d)), p, rendererContext.varProvider())).asBoolean().orElse(d);
//            }
//
//            Color asColor(String name, Color d) {
//                return NTxValue.of(engine.evalExpression(p.getPropertyValue(name).orElse(NTxUtils.toElement(d)), p, rendererContext.varProvider())).asColor().orElse(d);
//            }
//
//            Stroke asStroke(String name, Stroke d) {
//                NElement ev = engine.evalExpression(p.getPropertyValue(name).orElse(NTxUtils.toElement(d)), p, rendererContext.varProvider());
//                if (ev != null && !ev.isNull()) {
//                    Stroke stroke = rendererContext.graphics().createStroke(ev);
//                    if (stroke != null) {
//                        return stroke;
//                    }
//                }
//                return d;
//            }
//        }
//        A a = new A();
//        d.xmin = a.asDouble("xmin", -100.0);
//        d.xmax = a.asDouble("xmax", +100.0);
//        d.ymin = a.asDouble("ymin", null);
//        d.ymax = a.asDouble("ymax", null);
//        d.majorGridSpacing = a.asDouble("majorGridSpacing", 10.0);
//        d.showMajorGrid = a.asBoolean("showMajorGrid", true);
//        d.majorGridColor =a.asColor("majorGridColor", null);
//        d.majorGridStroke =a.asStroke("majorGridStroke", null);
//        d.minorGridSpacing = a.asDouble("minorGridSpacing", 1.0);
//        d.showMinorGrid = a.asBoolean("showMinorGrid", true);
//        d.minorGridColor =a.asColor("minorGridColor", null);
//        d.minorGridStroke =a.asStroke("minorGridStroke", null);
//        return d;
//    }

    public java.util.List<NTxFunctionPlotInfo> loadBody(NElement element, NTxNodeBuilderContext buildContext) {
        NListContainerElement nListContainerElement = element.asListContainer().orNull();
        java.util.List<NTxFunctionPlotInfo> all = new ArrayList<>();
        if (nListContainerElement != null && !nListContainerElement.isAnyUplet()) {
            List<NElement> c = nListContainerElement.children();
            for (NElement child : c) {
                if (child.isNamedObject()) {
                    NTxFunctionPlotInfo a = load(child.asObject().get(), buildContext);
                    if (a != null) {
                        all.add(a);
                    }
                }else{
                    buildContext.engine().log().log(NMsg.ofC("unexpected plot child %s", NTxUtils.snippet(child)));
                }
            }
        }
        if (all.isEmpty()) {
            NTxFunctionPlotInfo f = new NTxFunctionPlotInfo();
            f.args=1;
            f.var1="x";
            f.plotType= NTxPlotType.CURVE;
            f.source= NTxPlotSource.FUNCTION_X;
            f.fexpr=NElement.ofString("sin(x)");
            all.add(f);
        }
        return all;
    }

    private NTxFunctionPlotInfo load(NObjectElement child, NTxNodeBuilderContext buildContext) {
        NTxFunctionPlotInfo i = new NTxFunctionPlotInfo();
        switch (NNameFormat.VAR_NAME.format(NStringUtils.firstNonNull(child.name().orNull(),"").trim())) {
            case "curve": {
                i.plotType= NTxPlotType.CURVE;
                break;
            }
            case "area": {
                i.plotType= NTxPlotType.AREA;
                break;
            }
            case "bar": {
                i.plotType= NTxPlotType.BAR;
                break;
            }
            default: {
                buildContext.engine().log().log(NMsg.ofC("unexpected plot type %s", child.name().orNull()));
                return null;
            }
        }
        for (NElement e : child.children()) {
            if (e.isNamedPair()) {
                switch (e.asPair().get().key().asStringValue().get()) {
                    case "title": {
                        i.title = e.asPair().get().value();
                        break;
                    }
                    case "color": {
                        i.color = e.asPair().get().value();
                        break;
                    }
                    case "stroke": {
                        i.stroke = e.asPair().get().value();
                        break;
                    }
                    case "x": {
                        i.x = e.asPair().get().value();
                        break;
                    }
                    case "y": {
                        i.y = e.asPair().get().value();
                        break;
                    }
                    case "z": {
                        i.z = e.asPair().get().value();
                        break;
                    }
                    case "t": {
                        i.t = e.asPair().get().value();
                        break;
                    }
                    case "f": {
                        i.fval = e.asPair().get().value();
                        if(i.t!=null) {
                            i.source = NTxPlotSource.VALUE_XYZT;
                        }else if(i.z!=null) {
                            i.source = NTxPlotSource.VALUE_XYZ;
                        }else if(i.y!=null) {
                            i.source = NTxPlotSource.VALUE_XY;
                        }else if(i.x!=null) {
                            i.source = NTxPlotSource.VALUE_X;
                        }
                        break;
                    }
                    default: {
                        buildContext.engine().log().log(NMsg.ofC("unexpected function declaration %s", NTxUtils.snippet(e)));
                    }
                }
            } else if (e.isPair()) {
                NPairElement p = e.asPair().get();
                NElement k = p.key();
                if (k.isNamedUplet("f")) {
                    NUpletElement fk = k.asUplet().get();
                    if (fk.params().size() == 1 && fk.params().get(0).isName()) {
                        i.fexpr = p.value();
                        i.var1 = fk.params().get(0).asNameValue().get();
                        i.args = 1;
                        i.source = NTxPlotSource.FUNCTION_X;
                    } else if (fk.params().size() == 2 && fk.params().get(0).isName() && fk.params().get(1).isName()) {
                        i.fexpr = p.value();
                        i.var1 = fk.params().get(0).asNameValue().get();
                        i.var2 = fk.params().get(1).asNameValue().get();
                        i.args = 2;
                        i.source = NTxPlotSource.FUNCTION_XY;
                    } else if (fk.params().size() == 3 && fk.params().get(0).isName() && fk.params().get(1).isName() && fk.params().get(2).isName()) {
                        i.fexpr = p.value();
                        i.var1 = fk.params().get(0).asNameValue().get();
                        i.var2 = fk.params().get(1).asNameValue().get();
                        i.var3 = fk.params().get(2).asNameValue().get();
                        i.args = 3;
                        i.source = NTxPlotSource.FUNCTION_XYZ;
                    } else if (fk.params().size() == 4 && fk.params().get(0).isName() && fk.params().get(1).isName() && fk.params().get(2).isName() && fk.params().get(3).isName()) {
                        i.fexpr = p.value();
                        i.var1 = fk.params().get(0).asNameValue().get();
                        i.var2 = fk.params().get(1).asNameValue().get();
                        i.var3 = fk.params().get(2).asNameValue().get();
                        i.var4 = fk.params().get(3).asNameValue().get();
                        i.args = 4;
                        i.source = NTxPlotSource.FUNCTION_XYZT;
                    } else {
                        buildContext.engine().log().log(NMsg.ofC("unexpected function declaration %s", NTxUtils.snippet(k)));
                    }
                } else {
                    buildContext.engine().log().log(NMsg.ofC("unexpected function declaration %s", NTxUtils.snippet(k)));
                }
            }
        }
        if(i.source==null){
            if(i.t!=null) {
                i.source = NTxPlotSource.VALUE_XYZT;
            }else if(i.z!=null) {
                i.source = NTxPlotSource.VALUE_XYZ;
            }else if(i.y!=null) {
                i.source = NTxPlotSource.VALUE_XY;
            }else if(i.x!=null) {
                i.source = NTxPlotSource.VALUE_X;
            }
        }
        return i;
    }
}
