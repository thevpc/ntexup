package net.thevpc.ntexup.engine.renderer.elem2d.strokes;

import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.nuts.elem.NElement;

import java.awt.*;
import java.awt.geom.Area;

/**
 * gpl2
 * http://www.jhlabs.com/java/java2d/strokes/CompoundStroke.java
 */
public class CompoundStroke implements Stroke {
    public static enum Op {
        ADD,
        SUBTRACT,
        INTERSECT,
        DIFFERENCE
    }

    private Stroke stroke1, stroke2;
    private Op operation;

    public static Stroke of(NElement ee, CompoundStroke.Op op, NTxGraphics g) {
        NTxValue o = NTxValue.of(ee);
        Stroke base1 = null;
        Stroke base2 = null;
        for (NElement arg : o.args()) {
            if (
                    arg.isAnyTuple()
                            || arg.isAnyArray()
                            || arg.isAnyObject()
            ) {
                Stroke stroke = g.createStroke(arg);
                if (base1 == null) {
                    base1 = stroke;
                } else if (base2 == null) {
                    base2 = stroke;
                }
            }
        }
        if (base1 == null && base2 == null) {
            return StrokeFactory.createBasic(NTxValue.of(null));
        }
        if (base1 == null) {
            return base2;
        }
        if (base2 == null) {
            return base1;
        }
        return new CompoundStroke(base1, base2, op);
    }

    public CompoundStroke(Stroke stroke1, Stroke stroke2, Op operation) {
        this.stroke1 = stroke1;
        this.stroke2 = stroke2;
        this.operation = operation;
    }

    public Shape createStrokedShape(Shape shape) {
        Area area1 = new Area(stroke1.createStrokedShape(shape));
        Area area2 = new Area(stroke2.createStrokedShape(shape));
        switch (operation) {
            case ADD:
                area1.add(area2);
                break;
            case SUBTRACT:
                area1.subtract(area2);
                break;
            case INTERSECT:
                area1.intersect(area2);
                break;
            case DIFFERENCE:
                area1.exclusiveOr(area2);
                break;
        }
        return area1;
    }
}
