package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.document.NTxArrow;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValueByType;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxShapes3dUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.RealToRelativeMapper;
import net.thevpc.ntexup.lib.geometry2d.NTx2DUtils;
import net.thevpc.ntexup.lib.geometry3d.*;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DLine;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DLineLabel;
import net.thevpc.nuts.elem.*;

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
        r.setStartArrow(NTxValueByType.getArrow(rendererContext, NTxPropName.START_ARROW).orNull());
        r.setEndArrow(NTxValueByType.getArrow(rendererContext, NTxPropName.END_ARROW).orNull());
        NTxArrow darrow = NTxValueByType.getArrow(rendererContext, "arrow").orNull();
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
        boolean positionSet=false;
        boolean orientationSet=false;
        if(labelElem!=null){
            NtxElement3DLineLabel label=new NtxElement3DLineLabel();
            if(labelElem.isAnyString()){
                label.setText(labelElem.asStringValue().orNull());
            }else if(labelElem.isListOrParametrizedContainer()){
                for (NParamOrChild c : labelElem.asListOrParametrizedContainer().get().paramsOrChildren()) {
                    NElement ce = c.element();
                    if(ce.isAnyString()){
                        label.setText(ce.asStringValue().orNull());
                    }else if(ce.isNamedPair()){
                        NPairElement p = ce.asNamedPair().get();
                        switch (NTxUtils.uid(p.key().asStringValue().orNull())) {
                            case "text":
                            case "value":
                            {
                                label.setText(p.value().asStringValue().orNull());
                                break;
                            }
                            case "position":{
                                NElement pv = p.value();
                                applyPosition(label,pv);
                                positionSet=true;
                                break;
                            }
                            case "orientation":{
                                NElement pv = p.value();
                                applyOrientation(label,pv);
                                orientationSet=true;
                                break;
                            }
                            case "offset":{
                                NElement pv = p.value();
                                applyOffset(label,pv);
                                break;
                            }
                        }
                    }
                }
            }

            if(!positionSet){
                applyPosition(label,rendererContext.computePropertyValue("label-position").orNull());
            }
            if(label.getOffset()==null){
                applyOffset(label,rendererContext.computePropertyValue("label-offset").orNull());
            }
            if(!orientationSet){
                applyOrientation(label,rendererContext.computePropertyValue("label-orientation").orNull());
            }

//            public static final String FONT_FAMILY="font-family";
//            public static final String FONT_SIZE="font-size";
//            public static final String FONT_BOLD="font-bold";
//            public static final String FONT_ITALIC="font-italic";
//            public static final String FONT_UNDERLINED="font-underlined";
//            public static final String FONT_STRIKE="font-strike";
//            public static final String FOREGROUND_COLOR="foreground-color";
//            public static final String BACKGROUND_COLOR="background-color";

        }
        return r;
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

    private void applyOffset(NtxElement3DLineLabel label, NElement pv) {
        if(pv==null){
            return;
        }
        NTxPoint2D z = NTx2DUtils.asPoint2D(pv).orNull();
        if(z!=null){
            label.setOffset(z);
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
