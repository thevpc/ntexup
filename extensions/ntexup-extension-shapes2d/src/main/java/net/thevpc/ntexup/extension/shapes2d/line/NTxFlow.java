package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElementBuilder;
import net.thevpc.nuts.elem.NPairElement;
import net.thevpc.nuts.elem.NParamOrChild;

import java.awt.Color;
import java.awt.Paint;

/**
 * Parsed {@code flow: dots(count:3, size:2, period:4, speed:20, reverse, color:red)}
 * decoration. A bare {@code flow} flag is accepted too (one dot per period).
 */
public class NTxFlow {
    public int count = 1;
    public double size = 0; // 0 means derived
    public double period = 4;
    /** Page units per second; 0 means "use {@link #period}". */
    public double speed = 0;
    public boolean reverse = false;
    public Paint color = null;
    public String shape = "circle";
    public boolean staticHide = false;

    public static NTxFlow parse(NElement elem, NTxRendererContext ctx) {
        if (elem == null) {
            return null;
        }
        if (ctx != null) {
            elem = ctx.evalExpression(elem).orElse(elem);
        }
        NTxFlow f = new NTxFlow();
        if (elem.isListOrParametrizedContainer()) {
            for (NParamOrChild c : elem.asListOrParametrizedContainer().get().paramsOrChildren()) {
                NElement ce = c.element();
                if (ctx != null) {
                    ce = ctx.evalExpression(ce).orElse(ce);
                }
                if (ce.isNamedPair()) {
                    NPairElement p = ce.asNamedPair().get();
                    String key = p.key().asStringValue().orNull();
                    NElement val = p.value();
                    if (ctx != null) {
                        val = ctx.evalExpression(val).orElse(val);
                    }
                    if ("count".equals(key)) {
                        f.count = val.asIntValue().orElse(1);
                    } else if ("size".equals(key)) {
                        f.size = val.asDoubleValue().orElse(0.0);
                    } else if ("period".equals(key)) {
                        f.period = val.asDoubleValue().orElse(4.0);
                    } else if ("speed".equals(key)) {
                        f.speed = val.asDoubleValue().orElse(0.0);
                    } else if ("reverse".equals(key)) {
                        f.reverse = val.isBoolean() ? val.asBooleanValue().orElse(true) : true;
                    } else if ("color".equals(key)) {
                        f.color = NTxValue.of(val).asPaint().orNull();
                    } else if ("shape".equals(key)) {
                        f.shape = val.asStringValue().orNull();
                    } else if ("static".equals(key)) {
                        String s = val.asStringValue().orNull();
                        if ("hide".equalsIgnoreCase(s)) {
                            f.staticHide = true;
                        }
                    }
                } else if (ce.isBoolean()) {
                    if (ce.asBooleanValue().orElse(false)) {
                        f.reverse = true;
                    }
                } else if (ce.isAnyString()) {
                    String s = ce.asStringValue().orNull();
                    if ("reverse".equalsIgnoreCase(s)) {
                        f.reverse = true;
                    } else if ("static-hide".equalsIgnoreCase(s)) {
                        f.staticHide = true;
                    }
                }
            }
        }
        if (ctx != null && !ctx.isAnimate() && f.staticHide) {
            return null; // omit in static renders
        }
        return f;
    }

    /**
     * Semantic dump of the parsed flow, recorded by the TSON renderer so a
     * validated deck shows the animation parameters, not only the painted dots.
     */
    public NElement toDumpElement() {
        NObjectElementBuilder ob = NElement.ofObjectBuilder("flow");
        ob.add("count", NElement.of(count));
        ob.add("period", NElement.of(period));
        ob.add("reverse", NElement.of(reverse));
        ob.add("shape", NElement.of(shape == null ? "circle" : shape));
        if (size > 0) {
            ob.add("size", NElement.of(size));
        }
        if (speed > 0) {
            ob.add("speed", NElement.of(speed));
        }
        if (color != null) {
            ob.add("color", NElement.of(colorString(color)));
        }
        if (staticHide) {
            ob.add("staticHide", NElement.of(true));
        }
        return ob.build();
    }

    private static String colorString(Paint paint) {
        if (paint instanceof Color c) {
            if (c.getAlpha() == 255) {
                return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
            }
            return String.format("#%02x%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
        }
        return String.valueOf(paint);
    }
}
