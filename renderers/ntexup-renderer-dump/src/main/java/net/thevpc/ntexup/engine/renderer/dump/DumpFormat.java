package net.thevpc.ntexup.engine.renderer.dump;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NArrayElementBuilder;
import net.thevpc.nuts.elem.NObjectElementBuilder;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.text.AttributedCharacterIterator;

/**
 * Serialization helpers shared by {@link RecordingGraphics2D} and
 * {@link RecordingNTxGraphics}. Converts rendered java2d primitives (shapes,
 * paints, strokes, fonts, transforms) into deterministic TSON fragments.
 */
public class DumpFormat {

    private DumpFormat() {
    }

    public static NObjectElementBuilder op(String name) {
        return NElement.ofObjectBuilder(name);
    }

    public static double round(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return v;
        }
        return Math.round(v * 1000.0) / 1000.0;
    }

    public static NElement num(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return NElement.ofNull();
        }
        return NElement.of(round(v));
    }

    public static void add(NObjectElementBuilder ob, String name, double v) {
        ob.add(name, num(v));
    }

    public static void addIf(NObjectElementBuilder ob, String name, Object v) {
        if (v != null) {
            ob.add(name, NElement.of(v));
        }
    }

    public static String fmt(double v) {
        double r = round(v);
        if (Double.isNaN(r) || Double.isInfinite(r)) {
            return String.valueOf(r);
        }
        if (r == Math.rint(r) && Math.abs(r) < 1e15) {
            return String.valueOf((long) r);
        }
        return String.valueOf(r);
    }

    public static String pt(double x, double y) {
        return fmt(x) + "," + fmt(y);
    }

    public static NElement pointArray(double x, double y) {
        NArrayElementBuilder a = NElement.ofArrayBuilder();
        a.add(num(x));
        a.add(num(y));
        return a.build();
    }

    public static NElement rectArray(double x, double y, double w, double h) {
        NArrayElementBuilder a = NElement.ofArrayBuilder();
        a.add(num(x));
        a.add(num(y));
        a.add(num(w));
        a.add(num(h));
        return a.build();
    }

    public static String pointsString(double[] xx, double[] yy, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(fmt(xx[i])).append(',').append(fmt(yy[i]));
        }
        return sb.toString();
    }

    public static String pointsString(double[] xx, double[] yy) {
        return pointsString(xx, yy, Math.min(xx.length, yy.length));
    }

    public static String pointsString(int[] xx, int[] yy, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(xx[i]).append(',').append(yy[i]);
        }
        return sb.toString();
    }

    /**
     * Serializes a {@link Shape} as raw path segments (quadratic/cubic curves
     * are preserved, not flattened).
     */
    public static String shapePath(Shape s) {
        StringBuilder sb = new StringBuilder();
        double[] coords = new double[6];
        PathIterator it = s.getPathIterator(null, 0.0);
        while (!it.isDone()) {
            int type = it.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_MOVETO: {
                    sb.append("M").append(fmt(coords[0])).append(',').append(fmt(coords[1])).append(' ');
                    break;
                }
                case PathIterator.SEG_LINETO: {
                    sb.append("L").append(fmt(coords[0])).append(',').append(fmt(coords[1])).append(' ');
                    break;
                }
                case PathIterator.SEG_QUADTO: {
                    sb.append("Q").append(fmt(coords[0])).append(',').append(fmt(coords[1]))
                            .append(',').append(fmt(coords[2])).append(',').append(fmt(coords[3])).append(' ');
                    break;
                }
                case PathIterator.SEG_CUBICTO: {
                    sb.append("C").append(fmt(coords[0])).append(',').append(fmt(coords[1]))
                            .append(',').append(fmt(coords[2])).append(',').append(fmt(coords[3]))
                            .append(',').append(fmt(coords[4])).append(',').append(fmt(coords[5])).append(' ');
                    break;
                }
                case PathIterator.SEG_CLOSE: {
                    sb.append("Z ");
                    break;
                }
                default: {
                    break;
                }
            }
            it.next();
        }
        return sb.toString().trim();
    }

    /**
     * Classifies a shape into a semantic primitive element:
     * rect / oval / roundRect / line or a generic path (with bounds).
     */
    public static NElement shape(Shape s) {
        if (s == null) {
            return NElement.ofNull();
        }
        if (s instanceof RoundRectangle2D rr) {
            NObjectElementBuilder ob = op("roundRect");
            add(ob, "x", rr.getX());
            add(ob, "y", rr.getY());
            add(ob, "width", rr.getWidth());
            add(ob, "height", rr.getHeight());
            add(ob, "arcwidth", rr.getArcWidth());
            add(ob, "archeight", rr.getArcHeight());
            return ob.build();
        }
        if (s instanceof Ellipse2D e) {
            NObjectElementBuilder ob = op("oval");
            add(ob, "x", e.getX());
            add(ob, "y", e.getY());
            add(ob, "width", e.getWidth());
            add(ob, "height", e.getHeight());
            return ob.build();
        }
        if (s instanceof Line2D l) {
            NObjectElementBuilder ob = op("line");
            add(ob, "x1", l.getX1());
            add(ob, "y1", l.getY1());
            add(ob, "x2", l.getX2());
            add(ob, "y2", l.getY2());
            return ob.build();
        }
        if (s instanceof Rectangle2D r) {
            NObjectElementBuilder ob = op("rect");
            add(ob, "x", r.getX());
            add(ob, "y", r.getY());
            add(ob, "width", r.getWidth());
            add(ob, "height", r.getHeight());
            return ob.build();
        }
        NObjectElementBuilder ob = op("path");
        Rectangle2D b = s.getBounds2D();
        if (b != null) {
            ob.add("bounds", rectArray(b.getX(), b.getY(), b.getWidth(), b.getHeight()));
        }
        ob.add("d", NElement.of(shapePath(s)));
        return ob.build();
    }

    public static NElement matrix(AffineTransform t) {
        if (t == null) {
            return NElement.ofNull();
        }
        NArrayElementBuilder a = NElement.ofArrayBuilder();
        double[] m = new double[6];
        t.getMatrix(m);
        for (double v : m) {
            a.add(num(v));
        }
        return a.build();
    }

    public static boolean isIdentity(AffineTransform t) {
        return t == null || t.isIdentity();
    }

    public static String color(Color c) {
        if (c == null) {
            return null;
        }
        if (c.getAlpha() == 255) {
            return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
        }
        return String.format("#%02x%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    public static NElement paint(Paint p) {
        if (p == null) {
            return NElement.ofNull();
        }
        if (p instanceof Color c) {
            return NElement.of(color(c));
        }
        if (p instanceof net.thevpc.ntexup.api.document.elem2d.NTxLinearGradientPaint lp) {
            NObjectElementBuilder ob = op("linearGradient");
            ob.add("start", pointArray(lp.getStart().getX(), lp.getStart().getY()));
            ob.add("end", pointArray(lp.getEnd().getX(), lp.getEnd().getY()));
            ob.add("stops", gradientStops(lp.getFractions(), lp.getColors()));
            addIf(ob, "cycle", String.valueOf(lp.getCycleMethod()));
            return ob.build();
        }
        if (p instanceof GradientPaint gp) {
            NObjectElementBuilder ob = op("linearGradient");
            ob.add("start", pointArray(gp.getPoint1().getX(), gp.getPoint1().getY()));
            ob.add("end", pointArray(gp.getPoint2().getX(), gp.getPoint2().getY()));
            NArrayElementBuilder stops = NElement.ofArrayBuilder();
            NArrayElementBuilder s0 = NElement.ofArrayBuilder();
            s0.add(NElement.of(0.0));
            s0.add(NElement.of(color(gp.getColor1())));
            NArrayElementBuilder s1 = NElement.ofArrayBuilder();
            s1.add(NElement.of(1.0));
            s1.add(NElement.of(color(gp.getColor2())));
            stops.add(s0.build());
            stops.add(s1.build());
            ob.add("stops", stops.build());
            return ob.build();
        }
        if (p instanceof RadialGradientPaint rp) {
            NObjectElementBuilder ob = op("radialGradient");
            ob.add("center", pointArray(rp.getCenterPoint().getX(), rp.getCenterPoint().getY()));
            add(ob, "radius", rp.getRadius());
            ob.add("focus", pointArray(rp.getFocusPoint().getX(), rp.getFocusPoint().getY()));
            ob.add("stops", gradientStops(rp.getFractions(), rp.getColors()));
            addIf(ob, "cycle", String.valueOf(rp.getCycleMethod()));
            return ob.build();
        }
        if (p instanceof MultipleGradientPaint mgp) {
            NObjectElementBuilder ob = op("linearGradient");
            ob.add("stops", gradientStops(mgp.getFractions(), mgp.getColors()));
            addIf(ob, "cycle", String.valueOf(mgp.getCycleMethod()));
            return ob.build();
        }
        if (p instanceof TexturePaint tp) {
            NObjectElementBuilder ob = op("texture");
            Rectangle2D r = tp.getAnchorRect();
            ob.add("anchor", rectArray(r.getX(), r.getY(), r.getWidth(), r.getHeight()));
            return ob.build();
        }
        // opaque java2d paint wrappers (Paint2 ...) or third party paints
        NObjectElementBuilder ob = op("paint");
        ob.add("class", NElement.of(p.getClass().getName()));
        return ob.build();
    }

    private static NElement gradientStops(float[] fractions, Color[] colors) {
        NArrayElementBuilder stops = NElement.ofArrayBuilder();
        if (fractions != null && colors != null) {
            int n = Math.min(fractions.length, colors.length);
            for (int i = 0; i < n; i++) {
                NArrayElementBuilder s = NElement.ofArrayBuilder();
                s.add(NElement.of(round(fractions[i])));
                s.add(NElement.of(color(colors[i])));
                stops.add(s.build());
            }
        }
        return stops.build();
    }

    public static NElement stroke(Stroke s) {
        if (s == null) {
            return NElement.ofNull();
        }
        if (s instanceof BasicStroke bs) {
            NObjectElementBuilder ob = op("stroke");
            add(ob, "width", bs.getLineWidth());
            ob.add("cap", NElement.of(capName(bs.getEndCap())));
            ob.add("join", NElement.of(joinName(bs.getLineJoin())));
            add(ob, "miterlimit", bs.getMiterLimit());
            float[] dash = bs.getDashArray();
            if (dash != null) {
                NArrayElementBuilder d = NElement.ofArrayBuilder();
                for (float v : dash) {
                    d.add(NElement.of(round(v)));
                }
                ob.add("dash", d.build());
                add(ob, "dashphase", bs.getDashPhase());
            }
            return ob.build();
        }
        NObjectElementBuilder ob = op("stroke");
        ob.add("class", NElement.of(s.getClass().getName()));
        return ob.build();
    }

    public static NElement arrow(net.thevpc.ntexup.api.document.NTxArrow a) {
        if (a == null) {
            return NElement.ofNull();
        }
        NObjectElementBuilder ob = op("arrow");
        addIf(ob, "type", a.getType() == null ? null : a.getType().name());
        if (a.getWidth() != 0 || a.getHeight() != 0) {
            add(ob, "width", a.getWidth());
            add(ob, "height", a.getHeight());
        }
        return ob.build();
    }

    public static String capName(int cap) {
        switch (cap) {
            case BasicStroke.CAP_BUTT:
                return "butt";
            case BasicStroke.CAP_ROUND:
                return "round";
            case BasicStroke.CAP_SQUARE:
                return "square";
            default:
                return "unknown";
        }
    }

    public static String joinName(int join) {
        switch (join) {
            case BasicStroke.JOIN_BEVEL:
                return "bevel";
            case BasicStroke.JOIN_MITER:
                return "miter";
            case BasicStroke.JOIN_ROUND:
                return "round";
            default:
                return "unknown";
        }
    }

    public static NElement font(Font f) {
        if (f == null) {
            return NElement.ofNull();
        }
        NObjectElementBuilder ob = op("font");
        ob.add("family", NElement.of(f.getFamily()));
        add(ob, "size", f.getSize2D());
        ob.add("style", NElement.of(fontStyleName(f.getStyle())));
        return ob.build();
    }

    public static String fontStyleName(int style) {
        switch (style) {
            case Font.PLAIN:
                return "plain";
            case Font.BOLD:
                return "bold";
            case Font.ITALIC:
                return "italic";
            case Font.BOLD | Font.ITALIC:
                return "bolditalic";
            default:
                return "style-" + style;
        }
    }

    /**
     * Appends resolved absolute state (transform, clip, alpha) to an operation.
     * Paint/color/stroke/font are added by the caller only when meaningful for
     * the operation kind.
     */
    public static void state(NObjectElementBuilder ob, Graphics2D g, double canvasWidth, double canvasHeight) {
        AffineTransform t = g.getTransform();
        if (!isIdentity(t)) {
            ob.add("transform", matrix(t));
        }
        Shape clip = g.getClip();
        if (clip != null) {
            Rectangle2D cb = clip.getBounds2D();
            if (cb != null) {
                boolean full = cb.getX() == 0 && cb.getY() == 0
                        && Math.abs(cb.getWidth() - canvasWidth) < 1 && Math.abs(cb.getHeight() - canvasHeight) < 1;
                if (!full) {
                    ob.add("clip", rectArray(cb.getX(), cb.getY(), cb.getWidth(), cb.getHeight()));
                }
            }
        }
        Composite composite = g.getComposite();
        if (composite instanceof AlphaComposite ac && ac.getAlpha() < 1.0f) {
            add(ob, "alpha", ac.getAlpha());
        }
    }

    public static void paintAndColor(NObjectElementBuilder ob, Graphics2D g) {
        Paint paint = g.getPaint();
        if (paint != null) {
            ob.add("paint", paint(paint));
        }
        Color color = g.getColor();
        if (color != null) {
            addIf(ob, "color", color(color));
        }
    }

    /**
     * Flattens an {@link AttributedCharacterIterator} to a TSON text element:
     * the full string plus the distinct fonts used by its runs.
     */
    public static NElement attributedText(AttributedCharacterIterator it) {
        StringBuilder sb = new StringBuilder();
        for (char c = it.first(); c != AttributedCharacterIterator.DONE; c = it.next()) {
            sb.append(c);
        }
        NObjectElementBuilder ob = op("text");
        ob.add("s", NElement.of(sb.toString()));
        java.util.Map<String, NElement> fonts = new java.util.LinkedHashMap<>();
        java.util.Map<String, NElement> foregrounds = new java.util.LinkedHashMap<>();
        boolean underlined = false;
        boolean strikethrough = false;
        for (char c = it.first(); c != AttributedCharacterIterator.DONE; c = it.next()) {
            Object font = it.getAttribute(java.awt.font.TextAttribute.FONT);
            if (font instanceof Font f) {
                NElement fe = font(f);
                fonts.putIfAbsent(fe.toString(), fe);
            }
            Object fg = it.getAttribute(java.awt.font.TextAttribute.FOREGROUND);
            if (fg instanceof Color cc) {
                NElement ce = NElement.of(color(cc));
                foregrounds.putIfAbsent(ce.toString(), ce);
            } else if (fg instanceof Paint pp) {
                NElement pe = paint(pp);
                foregrounds.putIfAbsent(pe.toString(), pe);
            }
            if (it.getAttribute(java.awt.font.TextAttribute.UNDERLINE) != null) {
                underlined = true;
            }
            if (it.getAttribute(java.awt.font.TextAttribute.STRIKETHROUGH) != null) {
                strikethrough = true;
            }
        }
        if (!fonts.isEmpty()) {
            NArrayElementBuilder a = NElement.ofArrayBuilder();
            for (NElement f : fonts.values()) {
                a.add(f);
            }
            ob.add("fonts", a.build());
        }
        if (!foregrounds.isEmpty()) {
            NArrayElementBuilder a = NElement.ofArrayBuilder();
            for (NElement f : foregrounds.values()) {
                a.add(f);
            }
            ob.add("foregrounds", a.build());
        }
        if (underlined) {
            ob.add("underlined", NElement.of(Boolean.TRUE));
        }
        if (strikethrough) {
            ob.add("strike", NElement.of(Boolean.TRUE));
        }
        return ob.build();
    }

    public static String imageInfo(Object image) {
        if (image instanceof BufferedImage bi) {
            return bi.getWidth() + "x" + bi.getHeight();
        }
        if (image instanceof RenderedImage ri) {
            return ri.getWidth() + "x" + ri.getHeight();
        }
        if (image instanceof Image im) {
            int w = im.getWidth(null);
            int h = im.getHeight(null);
            if (w > 0 && h > 0) {
                return w + "x" + h;
            }
        }
        return null;
    }
}
