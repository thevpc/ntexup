package net.thevpc.ntexup.engine.renderer.elem2d.strokes;

import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElementBuilder;
import net.thevpc.nuts.util.NOptional;


import java.awt.*;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.Random;

public class SloppyStroke implements Stroke {

    private float width;
    private float sloppyness;
    private Random random;
    private Stroke base;

    public static Stroke of(NElement e, NTxGraphics g) {
        NTxValue o = NTxValue.of(e);
        double sloppyness = 5;
        Stroke base = null;
        NObjectElementBuilder basic = null;
        for (NElement arg : o.args()) {
            if (
                    arg.isAnyTuple()
                            || arg.isAnyArray()
                            || arg.isAnyObject()
            ) {
                if (base == null) {
                    base = g.createStroke(arg);
                } else {
                    base = CompositeStroke.of(base, g.createStroke(arg));
                }
            } else if (arg.type().isAnyNumber()) {
                sloppyness = NTxValue.of(arg).asDouble().orElse(sloppyness);
            } else {
                NOptional<NTxValue.SimplePair> sp = NTxValue.of(arg).asSimplePair();
                if (sp.isPresent()) {
                    NTxValue.SimplePair ke = sp.get();
                    switch (NTxUtils.uid(ke.getName())) {
                        case "width":
                        case "dash-phase":
                        case "miter-limit":
                        case "dash":
                        case "cap":
                        case "join": {
                            if (basic == null) {
                                basic = NElement.ofObjectBuilder();
                            }
                            basic.set(NTxUtils.uid(ke.getName()), (NElement) ke.getValue().raw());
                            break;
                        }
                        case "sloppyness":
                        case "s": {
                            sloppyness = ke.getValue().asDouble().orElse(sloppyness);
                            break;
                        }
                    }
                }else if(arg.isAnyString()){
                    switch (NTxUtils.uid(arg.asStringValue().get())) {
                        case "dashed":
                        case "dash":
                        {
                            if (basic == null) {
                                basic = NElement.ofObjectBuilder();
                            }
                            basic.add(NElement.ofString("dash"));
                            break;
                        }
                    }
                }
            }
        }
        if (base == null) {
            if (basic == null) {
                base = StrokeFactory.createBasic(NTxValue.of(NElement.ofObjectBuilder()));
            } else {
                base = StrokeFactory.createBasic(NTxValue.of(basic));
            }
        } else {
            if (basic != null) {
                base = CompositeStroke.of(StrokeFactory.createBasic(NTxValue.of(basic)), base);
            }
        }
        return new SloppyStroke(
                base,
                (float) sloppyness
        );
    }

    public SloppyStroke(Stroke base, float width) {
        this.width = width;
        this.random = new Random();
        this.base = base == null ? new BasicStroke() : base;
    }

    @Override
    public Shape createStrokedShape(Shape shape) {
        GeneralPath result = new GeneralPath();
        PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), 1);
        float points[] = new float[6];
        float moveX = 0, moveY = 0;
        float lastX = 0, lastY = 0;
        float thisX = 0, thisY = 0;
        int type = 0;
        boolean first = false;
        float next = 0;
        int phase = 0;

        float factor = 1;

        while (!it.isDone()) {
            type = it.currentSegment(points);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                    moveX = lastX = points[0];
                    moveY = lastY = points[1];
                    result.moveTo(moveX, moveY);
                    first = true;
//                    next = wavelength / 2;
                    break;

                case PathIterator.SEG_CLOSE:
                    points[0] = moveX;
                    points[1] = moveY;
                    // Fall into....

                case PathIterator.SEG_LINETO:
                    thisX = points[0];
                    thisY = points[1];
                    result.lineTo(thisX + random.nextFloat() * 2 * width - width, thisY + random.nextFloat() * 2 * width - width);
                    result.lineTo(lastX, lastY);
                    result.lineTo(thisX, thisY);
////                    float dx = thisX - lastX;
////                    float dy = thisY - lastY;
////                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
////                    if (distance >= next) {
////                        float r = 1.0f / distance;
////                        float angle = (float) Math.atan2(dy, dx);
////                        while (distance >= next) {
////                            float x = lastX + next * dx * r;
////                            float y = lastY + next * dy * r;
////                            float tx = amplitude * dy * r;
////                            float ty = amplitude * dx * r;
////                            if ((phase & 1) == 0)
////                                result.lineTo(x + amplitude * dy * r, y - amplitude * dx * r);
////                            else
////                                result.lineTo(x - amplitude * dy * r, y + amplitude * dx * r);
////                            next += wavelength;
////                            phase++;
////                        }
////                    }
//                    next -= distance;
//                    first = false;
                    lastX = thisX;
                    lastY = thisY;
                    if (type == PathIterator.SEG_CLOSE)
                        result.closePath();
                    break;
            }
            it.next();
        }

        return base.createStrokedShape(result);
    }
}
