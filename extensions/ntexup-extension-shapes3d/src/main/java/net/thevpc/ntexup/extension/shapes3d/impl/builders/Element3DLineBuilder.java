package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.NTxArrow;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.NTxSize;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.eval.NTxValueByType;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.api.util.NtxFontInfo;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry2d.NTx2DUtils;
import net.thevpc.ntexup.lib.geometry3d.*;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DLine;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DLineLabel;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.util.NOptional;

import java.awt.Paint;
import java.util.Arrays;
import java.util.List;

public class Element3DLineBuilder implements NtxElement3DNodeParser {
    @Override
    public List<String> getId3d() {
        return Arrays.asList(NTxNodeType.LINE);
    }

    @Override
    public NtxElement3D createElement3D(NTxRendererContext rendererContext, NTxBounds2D b, RealToRelativeMapper mapper, NtxElement3DNodeParserFactory parserFactory) {
        NTxNode node=rendererContext.node();
        NTxPoint3D from = NtxShapes3dUtils.resolvePosition3D(node, NTxPropName.FROM, rendererContext, b).orElse(NTxPoint3D.ofZero());
        NTxPoint3D to = NtxShapes3dUtils.resolvePosition3D(node, NTxPropName.TO, rendererContext, b).orElse(NTxPoint3D.ofZero());

        NtxElement3DLine r = NTxElement3DFactory.line(from, to);
        r.setStartArrow(NTxValueByType.getArrow(rendererContext, NTxPropName.START_ARROW, "start-arrow", "startArrow").orNull());
        r.setEndArrow(NTxValueByType.getArrow(rendererContext, NTxPropName.END_ARROW, "end-arrow", "endArrow").orNull());
        NTxArrow darrow = NTxValueByType.getArrow(rendererContext, "arrow", "arrows").orNull();
        if (darrow != null) {
            if (r.getStartArrow() == null) {
                r.setStartArrow(darrow);
            }
            if (r.getEndArrow() == null) {
                r.setEndArrow(darrow);
            }
        }
        NtxShapes3dUtils.apply3dProps(node, r, rendererContext, b,false);
        NElement labelElem = node.getPropertyValue("label").orNull();
        if (labelElem == null) {
            labelElem = node.getPropertyValue("text").orNull();
        }
        boolean positionSet = false;
        boolean orientationSet = false;
        if (labelElem != null) {
            if (rendererContext != null) {
                labelElem = rendererContext.evalExpression(labelElem).orElse(labelElem);
            }
            NtxElement3DLineLabel label = new NtxElement3DLineLabel();
            if (labelElem.isAnyString()) {
                label.setText(labelElem.asStringValue().orNull());
            } else if (labelElem.isListOrParametrizedContainer()) {
                for (NParamOrChild c : labelElem.asListOrParametrizedContainer().get().paramsOrChildren()) {
                    NElement ce = c.element();
                    if (rendererContext != null) {
                        ce = rendererContext.evalExpression(ce).orElse(ce);
                    }
                    if (ce.isAnyString()) {
                        label.setText(ce.asStringValue().orNull());
                    } else if (ce.isNamedPair()) {
                        NPairElement p = ce.asNamedPair().get();
                        String key = NTxUtils.uid(p.key().asStringValue().orNull());
                        NElement pval = p.value();
                        if (rendererContext != null) {
                            pval = rendererContext.evalExpression(pval).orElse(pval);
                        }
                        switch (key) {
                            case "text":
                            case "value": {
                                label.setText(pval.asStringValue().orNull());
                                break;
                            }
                            case "position": {
                                applyPosition(label, pval);
                                positionSet = true;
                                break;
                            }
                            case "orientation": {
                                applyOrientation(label, pval);
                                orientationSet = true;
                                break;
                            }
                            case "offset": {
                                applyOffset(label, pval, rendererContext, b);
                                break;
                            }
                            case "offset-perp":
                            case "perp":
                            case "perpendicular": {
                                label.setOffsetPerp(pval.asDoubleValue().orNull());
                                break;
                            }
                            case "offset-parallel":
                            case "parallel":
                            case "tangent": {
                                label.setOffsetParallel(pval.asDoubleValue().orNull());
                                break;
                            }
                            case "offset-3d":
                            case "offset3d": {
                                NOptional<NTxPoint3D> p3d = NtxShapes3dUtils.resolveDistance3DAny(pval, rendererContext, b);
                                if (p3d.isPresent()) {
                                    label.setOffset3d(p3d.get());
                                }
                                break;
                            }
                            case "offset-2d":
                            case "offset2d": {
                                NTxPoint2D z = NTx2DUtils.asPoint2D(pval).orNull();
                                if (z != null) {
                                    label.setOffset2d(z);
                                }
                                break;
                            }
                            case NTxPropName.FOREGROUND_COLOR:
                            case "color":
                            case "fg":
                            case "foreground": {
                                label.setForegroundColor(NTxValue.of(pval).asPaint().orNull());
                                break;
                            }
                            case NTxPropName.BACKGROUND_COLOR:
                            case "bg":
                            case "background": {
                                label.setBackgroundColor(NTxValue.of(pval).asPaint().orNull());
                                break;
                            }
                            case "font-size":
                            case "size":
                            case "fs": {
                                try {
                                    label.setFontSize(NTxSize.ofElement(pval));
                                } catch (Exception e) {
                                    if (pval.isNumber()) {
                                        label.setFontSize(NTxSize.ofPage(pval.asDoubleValue().orElse(2.0)));
                                    }
                                }
                                break;
                            }
                            case "font-family": {
                                label.setFontFamily(pval.asStringValue().orNull());
                                break;
                            }
                            case "font-bold":
                            case "bold": {
                                label.setFontBold(pval.asBooleanValue().orNull());
                                break;
                            }
                            case "font-italic":
                            case "italic": {
                                label.setFontItalic(pval.asBooleanValue().orNull());
                                break;
                            }
                        }
                    }
                }
            }

            if (!positionSet) {
                applyPosition(label, rendererContext.computePropertyValue("line-label-position", "label-position", "label-pos").orNull());
            }
            if (label.getOffset2d() == null && label.getOffset3d() == null && label.getOffsetRel3d() == null && label.getOffsetPerpSize() == null && label.getOffsetParallelSize() == null && label.getOffsetUpSize() == null && label.getOffsetPerp() == null && label.getOffsetParallel() == null) {
                applyOffset(label, rendererContext.computePropertyValue("line-label-offset", "label-offset").orNull(), rendererContext, b);
            }
            if (!orientationSet) {
                applyOrientation(label, rendererContext.computePropertyValue("line-label-orientation", "label-orientation").orNull());
            }
            if (label.getForegroundColor() == null) {
                Paint p = NTxValueByType.getPaint(rendererContext, "line-label-color", "line-label-foreground-color", "line-label-fg", "label-color", "label-foreground-color", "label-fg").orNull();
                if (p == null) {
                    p = NTxValueByType.getPaint(rendererContext, "color", "foreground-color", "fg").orNull();
                }
                label.setForegroundColor(p);
            }
            if (label.getFontSize() == null) {
                NElement fse = rendererContext.computePropertyValue("line-label-font-size", "line-label-size", "line-label-fs", "label-font-size", "label-size", "label-fs").orNull();
                if (fse == null) {
                    fse = rendererContext.computePropertyValue("font-size", "size", "fs").orNull();
                }
                if (fse != null) {
                    try {
                        label.setFontSize(NTxSize.ofElement(fse));
                    } catch (Exception ignored) {
                        if (fse.isNumber()) {
                            label.setFontSize(NTxSize.ofPage(fse.asDoubleValue().orElse(2.0)));
                        }
                    }
                }
                if (label.getFontSize() == null && rendererContext != null) {
                    NtxFontInfo fontInfo = NTxValueByName.getFontInfo(rendererContext);
                    if (fontInfo != null && fontInfo.size != null) {
                        label.setFontSize(fontInfo.size);
                    }
                }
            }
            if (label.getFontBold() == null) {
                label.setFontBold(NTxValueByType.getBoolean(rendererContext, "line-label-bold", "line-label-font-bold", "label-bold", "label-font-bold", "bold", "font-bold").orNull());
            }
            if (label.getFontItalic() == null) {
                label.setFontItalic(NTxValueByType.getBoolean(rendererContext, "line-label-italic", "line-label-font-italic", "label-italic", "label-font-italic", "italic", "font-italic").orNull());
            }
            if (label.getFontFamily() == null) {
                label.setFontFamily(NTxValueByType.getStringOrName(rendererContext, "line-label-font-family", "label-font-family", "font-family").orNull());
            }
            r.setLabel(label);
        }
        return r;
    }

    private boolean hasPhysicalDistanceUnit(NElement e) {
        if (e != null && e.isNumber()) {
            String suffix = net.thevpc.nuts.util.NStringUtils.strip(e.asNumber().get().numberSuffix()).toLowerCase();
            if (!suffix.isEmpty() && !suffix.equals("%") && !suffix.equals("%p")) {
                return true;
            }
        }
        return false;
    }

    private NTxSize parseSizeElement(NElement e) {
        if (e == null) {
            return null;
        }
        try {
            return NTxSize.ofElement(e);
        } catch (Exception ex) {
            if (e.isNumber()) {
                return NTxSize.ofParent(e.asDoubleValue().orElse(0.0));
            }
        }
        return null;
    }

    private void applyPosition(NtxElement3DLineLabel label, NElement pv) {
        if(pv==null){
            return;
        }
        if(pv.isAnyString()){
            switch (NTxUtils.uid(pv.asStringValue().orNull())) {
                case "center":{
                    label.setPosition(50);
                    break;
                }
                case "start":
                {
                    label.setPosition(0);
                    break;
                }
                case "end":
                {
                    label.setPosition(100);
                    break;
                }
            }
        }else if(pv.isNumber()){
            label.setPosition(pv.asDoubleValue().orElse(50.0));
        }
    }

    private void applyOffset(NtxElement3DLineLabel label, NElement pv, NTxRendererContext rendererContext, NTxBounds2D b) {
        if (pv == null) {
            return;
        }
        if (rendererContext != null) {
            pv = rendererContext.evalExpression(pv).orElse(pv);
        }
        if (pv.isNumber()) {
            label.setOffsetPerpSize(parseSizeElement(pv));
            return;
        }
        if (pv.isListOrParametrizedContainer()) {
            NListOrParametrizedContainerElement list = pv.asListOrParametrizedContainer().get();
            List<NParamOrChild> items = list.paramsOrChildren();
            if (items.size() == 1) {
                applyOffset(label, items.get(0).element(), rendererContext, b);
            } else if (items.size() == 2) {
                NElement e0 = items.get(0).element();
                NElement e1 = items.get(1).element();
                if (rendererContext != null) {
                    e0 = rendererContext.evalExpression(e0).orElse(e0);
                    e1 = rendererContext.evalExpression(e1).orElse(e1);
                }
                boolean hasUnits = hasPhysicalDistanceUnit(e0) || hasPhysicalDistanceUnit(e1);
                if (hasUnits) {
                    NOptional<NTxPoint3D> p3d = NtxShapes3dUtils.resolveDistance3DAny(pv, rendererContext, b);
                    if (p3d.isPresent()) {
                        label.setOffset3d(p3d.get());
                    }
                } else {
                    if (e0.isNumber()) {
                        label.setPosition(e0.asDoubleValue().orElse(50.0));
                    }
                    label.setOffsetPerpSize(parseSizeElement(e1));
                }
            } else if (items.size() >= 3) {
                NElement e0 = items.get(0).element();
                NElement e1 = items.get(1).element();
                NElement e2 = items.get(2).element();
                if (rendererContext != null) {
                    e0 = rendererContext.evalExpression(e0).orElse(e0);
                    e1 = rendererContext.evalExpression(e1).orElse(e1);
                    e2 = rendererContext.evalExpression(e2).orElse(e2);
                }
                boolean is3dWorld = hasPhysicalDistanceUnit(e0) || (e0.isNumber() && e0.asDoubleValue().orElse(0.0) == 0.0 && (hasPhysicalDistanceUnit(e1) || hasPhysicalDistanceUnit(e2)));
                if (is3dWorld) {
                    NOptional<NTxPoint3D> p3d = NtxShapes3dUtils.resolveDistance3DAny(pv, rendererContext, b);
                    if (p3d.isPresent()) {
                        label.setOffset3d(p3d.get());
                    }
                } else {
                    if (e0.isNumber()) {
                        label.setPosition(e0.asDoubleValue().orElse(50.0));
                    }
                    label.setOffsetPerpSize(parseSizeElement(e1));
                    label.setOffsetUpSize(parseSizeElement(e2));
                }
            }
            return;
        }
        if (pv.isObject()) {
            NObjectElement obj = pv.asObject().get();
            for (NElement child : obj.children()) {
                if (child.isNamedPair()) {
                    NPairElement pair = child.asNamedPair().get();
                    String key = NTxUtils.uid(pair.key().asStringValue().orNull());
                    NElement val = pair.value();
                    if (rendererContext != null) {
                        val = rendererContext.evalExpression(val).orElse(val);
                    }
                    switch (key) {
                        case "perp":
                        case "perpendicular":
                        case "offset-perp": {
                            label.setOffsetPerpSize(parseSizeElement(val));
                            break;
                        }
                        case "parallel":
                        case "tangent":
                        case "offset-parallel": {
                            label.setOffsetParallelSize(parseSizeElement(val));
                            break;
                        }
                        case "up":
                        case "offset-up":
                        case "z":
                        case "offset-z": {
                            label.setOffsetUpSize(parseSizeElement(val));
                            break;
                        }
                        case "x":
                        case "offset-x": {
                            double curY = label.getOffset2d() != null ? label.getOffset2d().y : 0;
                            label.setOffset2d(new NTxPoint2D(val.asDoubleValue().orElse(0.0), curY));
                            break;
                        }
                        case "y":
                        case "offset-y": {
                            double curX = label.getOffset2d() != null ? label.getOffset2d().x : 0;
                            label.setOffset2d(new NTxPoint2D(curX, val.asDoubleValue().orElse(0.0)));
                            break;
                        }
                        case "3d":
                        case "offset-3d": {
                            NOptional<NTxPoint3D> p3d = NtxShapes3dUtils.resolveDistance3DAny(val, rendererContext, b);
                            if (p3d.isPresent()) {
                                label.setOffset3d(p3d.get());
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private void applyOrientation(NtxElement3DLineLabel label, NElement pv) {
        if(pv==null){
            return;
        }
        if(pv.isAnyString()){
            switch (NTxUtils.uid(pv.asStringValue().orNull())) {
                case "center":{
                    label.setOrientationAngle(90);
                    label.setOrientation3d(true);
                    break;
                }
                case "start":
                {
                    label.setOrientationAngle(0);
                    label.setOrientation3d(true);
                    break;
                }
                case "end":
                {
                    label.setOrientationAngle(180);
                    label.setOrientation3d(true);
                    break;
                }
                case "screen":
                {
                    label.setOrientationAngle(180);
                    label.setOrientation3d(false);
                    break;
                }
            }
        }else if(pv.isNumber()){
            label.setOrientationAngle(pv.asDoubleValue().orElse(50.0));
            label.setOrientation3d(true);
        }
    }

}
