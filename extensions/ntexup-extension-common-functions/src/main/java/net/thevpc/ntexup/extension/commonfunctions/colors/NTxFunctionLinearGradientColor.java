package net.thevpc.ntexup.extension.commonfunctions.colors;

import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.NTxLinearGradientPaint;
import net.thevpc.ntexup.api.eval.*;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.util.NTxElementUtils;
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
    public NElement invoke(NTxFunctionCallContext args) {
        NTxResolutionContext context = args.scopedContext();
        boolean cyclic = false;
        NTxDouble2 start = null;
        NTxDouble2 end = null;
        Double angleDeg = null;
        java.util.List<Color> colors = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            NElement v = args.evalArg(i);
            NTxValue vv = NTxValue.of(v);
            if (vv.isBoolean()) {
                cyclic = v.asBooleanValue().get();
            } else if (vv.isPoint2()) {
                if (start == null) {
                    start = vv.asDouble2().get();
                } else if (end == null) {
                    end = vv.asDouble2().get();
                } else {
                    context.log(NMsg.ofC("%s: unexpected point arg %s", name(), v));
                }
            } else if (vv.isNumber() && colors.isEmpty() && start == null) {
                angleDeg = vv.asDouble().get();
            } else {
                NOptional<Color[]> c = vv.asColorArrayOrColor();
                if (c.isPresent()) {
                    for (Color color : c.get()) {
                        colors.add(color);
                    }
                } else {
                    context.log(NMsg.ofC("%s: unexpected arg %s", name(), v));
                }
            }
        }
        if (colors.isEmpty()) {
            context.log(NMsg.ofC("%s: missing colors", name()));
            return NElement.ofNull();
        }
        if (colors.size() == 1) {
            return NTxElementUtils.toElement(colors.get(0));
        }
        if (angleDeg != null && start == null) {
            double rad = Math.toRadians(angleDeg);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            start = new NTxDouble2(50 - 50 * cos, 50 - 50 * sin);
            end = new NTxDouble2(50 + 50 * cos, 50 + 50 * sin);
        }
        if (start == null) {
            start = new NTxDouble2(0, 50);
        }
        if (end == null) {
            end = new NTxDouble2(100 - start.getX(), 100 - start.getY());
        }

        Point2D pStart = new Point2D.Double(start.getX(), start.getY());
        Point2D pEnd = new Point2D.Double(end.getX(), end.getY());
        MultipleGradientPaint.CycleMethod cycle = cyclic ? MultipleGradientPaint.CycleMethod.REPEAT : MultipleGradientPaint.CycleMethod.NO_CYCLE;

        return NTxElementUtils.toElement(new NTxLinearGradientPaint(pStart, pEnd, null, colors.toArray(new Color[0]), cycle));
    }
}
