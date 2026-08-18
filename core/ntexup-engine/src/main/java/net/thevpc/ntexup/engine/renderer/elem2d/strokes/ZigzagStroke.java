package net.thevpc.ntexup.engine.renderer.elem2d.strokes;

import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.elem.NElement;

import java.awt.*;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;


/**
 * http://www.jhlabs.com/java/java2d/strokes/WobbleStroke.java
 */
public class ZigzagStroke implements Stroke {
    private float amplitude = 10.0f;
    private float wavelength = 10.0f;
    private Stroke stroke;
    private float flatness = 1;

    public static Stroke of(NElement e, NTxGraphics g) {
        NTxValue o = NTxValue.of(e);
        Stroke base = null;
        double wavelength = 2;
        double amplitude = 2;
        double flatness = 1;
        for (NElement arg : o.args()) {
            if (
                    arg.isAnyTuple()
                            || arg.isAnyArray()
                            || arg.isAnyObject()
            ) {
                if (base == null) {
                    base = g.createStroke(arg);
                }
            } else {
                NOptional<NTxValue.SimplePair> sp = NTxValue.of(arg).asSimplePair();
                if (sp.isPresent()) {
                    NTxValue.SimplePair ke = sp.get();
                    switch (NTxUtils.uid(ke.getName())) {
                        case "amp":
                        case "amplitude": {
                            amplitude = ke.getValue().asDouble().orElse(amplitude);
                            break;
                        }
                        case "length":
                        case "wavelength": {
                            wavelength = ke.getValue().asDouble().orElse(wavelength);
                            break;
                        }
                        case "flatness": {
                            flatness = ke.getValue().asDouble().orElse(flatness);
                            break;
                        }
                    }
                }
            }
        }
        return new ZigzagStroke(base == null ? new BasicStroke() : base, (float) amplitude, (float) wavelength, (float) flatness);
    }

    public ZigzagStroke(Stroke stroke, float amplitude, float wavelength, float flatness) {
        this.stroke = stroke;
        this.amplitude = amplitude;
        this.wavelength = wavelength;
        this.flatness = flatness;
    }

    public Shape createStrokedShape(Shape shape) {
        GeneralPath result = new GeneralPath();
        PathIterator it = new FlatteningPathIterator(shape.getPathIterator(null), flatness);
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
                    next = wavelength / 2;
                    break;

                case PathIterator.SEG_CLOSE:
                    points[0] = moveX;
                    points[1] = moveY;
                    // Fall into....

                case PathIterator.SEG_LINETO:
                    thisX = points[0];
                    thisY = points[1];
                    float dx = thisX - lastX;
                    float dy = thisY - lastY;
                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    if (distance >= next) {
                        float r = 1.0f / distance;
                        float angle = (float) Math.atan2(dy, dx);
                        while (distance >= next) {
                            float x = lastX + next * dx * r;
                            float y = lastY + next * dy * r;
                            float tx = amplitude * dy * r;
                            float ty = amplitude * dx * r;
                            if ((phase & 1) == 0)
                                result.lineTo(x + amplitude * dy * r, y - amplitude * dx * r);
                            else
                                result.lineTo(x - amplitude * dy * r, y + amplitude * dx * r);
                            next += wavelength;
                            phase++;
                        }
                    }
                    next -= distance;
                    first = false;
                    lastX = thisX;
                    lastY = thisY;
                    if (type == PathIterator.SEG_CLOSE)
                        result.closePath();
                    break;
            }
            it.next();
        }

        return stroke.createStrokedShape(result);
    }

}
