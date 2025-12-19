package net.thevpc.ntexup.extension.commonfunctions.colors;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.eval.*;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.util.NTxElementUtils;
import net.thevpc.ntexup.extension.commonfunctions.util.NTxColorUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;

public class NTxFunctionLinearGradientColor implements NTxFunction {
    @Override
    public String name() {
        return "linearGradientColor";
    }

    @Override
    public NElement invoke(NTxFunctionArgs args, NTxFunctionContext context) {
        boolean cyclic = false;
        NTxDouble2 start = null;
        NTxDouble2 end = null;
        NTxBounds2 selfBounds = NTxValue.of(context.findVar("selfBounds").map(x->x.get()).orNull()).asBounds2().orNull();
        java.util.List<Color> colors = new ArrayList<>();
        for (NTxFunctionArg arg : args.args()) {
            NElement v = arg.eval();
            NTxValue vv = NTxValue.of(v);
            if(vv.isBoolean()) {
                cyclic = v.asBooleanValue().get();
            }else if(vv.isPoint2()){
                if(start==null){
                    start=vv.asDouble2().get();
                }else  if(end==null){
                    end=vv.asDouble2().get();
                }else{
                    context.log().log(NMsg.ofC("%s: unexpected point arg %s", name(), v));
                }
            }else {
                NOptional<Color[]> c = vv.asColorArrayOrColor();
                if(c.isPresent()) {
                    for (Color color : c.get()) {
                        colors.add(color);
                    }
                }else{
                    context.log().log(NMsg.ofC("%s: unexpected point arg %s", name(), v));
                }
            }
        }
        if (colors.isEmpty()) {
            context.log().log(NMsg.ofC("%s: missing colors", name()));
            return NElement.ofNull();
        }
        if (colors.size() == 1) {
            return NTxElementUtils.toElement(colors.get(0));
        }
        if (start == null) {
            start = new NTxDouble2(0, 50);
        }
        if (end == null) {
            end = new NTxDouble2(100 - start.getX(), 100 - start.getY());
        }
        // should consider node size!!
        Point2D start2;
        Point2D end2;
        if(selfBounds!=null){
            start2=new Point2D.Double(
                    start.getX()/100*selfBounds.getWidth()+selfBounds.getX(),
                    start.getY()/100*selfBounds.getHeight()+selfBounds.getY()
            );
            end2=new Point2D.Double(
                    end.getX()/100*selfBounds.getWidth()+selfBounds.getX(),
                    end.getY()/100*selfBounds.getHeight()+selfBounds.getY()
            );
        }else{
            start2 = new Point2D.Double(start.getX(), start.getY());
            end2 = new Point2D.Double(end.getX(), end.getY());
        }

        return NTxElementUtils.toElement(NTxColorUtils.createLinearGradient(start2, end2, colors.toArray(new Color[0]), cyclic));
    }
}
