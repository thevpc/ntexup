package net.thevpc.ntexup.lib.paintstream;

import net.thevpc.nuts.elem.NElement;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Replays recorded paint-stream operations onto a real {@link Graphics2D}: the
 * symmetric counterpart of {@link RecordingGraphics2D}, and the java reading of
 * the same stream {@code ntexup-player.js} replays on a canvas.
 * <p>
 * The stream is flat and self-contained. Every operation carries its own
 * absolute transform, clip, paint, stroke and alpha, so operations do not
 * accumulate state, frames do not depend on each other and a frame can be
 * replayed in isolation, in any order, as many times as wanted. That is what
 * makes a java player as cheap as a browser one: no layout, no shaping and no
 * measuring happens here, only geometry that java already decided gets painted.
 * <p>
 * Because a {@code Graphics2D} is available again, three things are restored
 * rather than approximated the way a canvas has to: a rounded rectangle keeps
 * its real arc corners instead of quadratic ones, an arc keeps java's own
 * angle convention instead of being negated, and a {@code RadialGradientPaint}
 * keeps its two-circle form. Those paint the same pixels the exporter painted,
 * which is the point of a java replay.
 * <p>
 * Operations this class cannot express are skipped, never guessed at, and each
 * kind is reported once through {@link #warnings()} so a caller can decide
 * whether a silent gap is acceptable. A recorded number arrives as {@code null}
 * when it was NaN or infinite, and any operation whose geometry is not finite
 * is dropped rather than fed NaN.
 */
public class PaintStreamDecoder {

    /**
     * Where the decoder finds the pixels an image operation names by SHA-256.
     */
    public interface Images {
        /**
         * @return the decoded pixels of {@code sha256}, or {@code null} when they
         * are not (yet) available: an absent image paints nothing and is reported.
         */
        BufferedImage image(String sha256);
    }

    private static final Color DEFAULT_PAINT = Color.BLACK;

    private final Images images;
    private final Map<String, BufferedImage> decodedImages = new LinkedHashMap<>();
    private final Map<String, Shape> shapeCache = new LinkedHashMap<>();
    private final Map<String, java.awt.Paint> paintCache = new LinkedHashMap<>();
    private final Set<String> warnings = new LinkedHashSet<>();

    public PaintStreamDecoder() {
        this(null);
    }

    public PaintStreamDecoder(Images images) {
        this.images = images;
    }

    /**
     * Operation kinds that were skipped or only partly replayed, each named once.
     */
    public Set<String> warnings() {
        return warnings;
    }

    /**
     * Paints one frame at the size it was recorded for.
     */
    public void replay(Graphics2D g, List<NElement> ops) {
        replay(g, ops, 1.0);
    }

    /**
     * Paints one frame, scaled by {@code scale} on the way to the device, which
     * is how a HiDPI canvas or a zoomed view shows the same geometry sharper
     * rather than resampled.
     * <p>
     * The frame starts from an empty surface: a canvas keeps whatever the
     * previous frame left, so replaying without clearing would composite frames
     * on top of each other.
     */
    public void replay(Graphics2D g, List<NElement> ops, double scale) {
        Rectangle device = g.getDeviceConfiguration().getBounds();
        g.setTransform(new AffineTransform());
        g.clearRect(device.x, device.y, device.width, device.height);
        paint(g, ops, new AffineTransform(scale, 0, 0, scale, 0, 0));
    }

    /**
     * Paints one frame wherever {@code device} puts it: the recorded affine of
     * every operation is multiplied on the right, so a player can letterbox a
     * page inside a window or zoom it without a second surface.
     * <p>
     * Nothing is cleared — the caller owns the surface and its background. What
     * it does own is the clip: an operation may draw outside the page it was
     * recorded for, so a caller that places frames side by side clips to its
     * rectangle first, and the per-operation clip intersects with it.
     */
    public void paint(Graphics2D g, List<NElement> ops, AffineTransform device) {
        for (NElement op : ops) {
            paintOp(g, op, device);
        }
    }

    // ------------------------------------------------------------------ state

    /**
     * Applies one operation's absolute state to a private copy of the graphics,
     * so nothing carries over to the next operation.
     */
    private void paintOp(Graphics2D g, NElement op, AffineTransform device) {
        String kind = string(op, "k");
        if (kind == null) {
            return;
        }
        Graphics2D c = (Graphics2D) g.create();
        try {
            c.setTransform(absoluteTransform(op, device));
            //what the engine installs for the whole page (DefaultNTxEngine.applyRenderingHints):
            //the stream only stores deviations from these
            c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            c.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            c.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            c.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            c.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            c.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
            c.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            if (bool(op, "aa") == Boolean.FALSE) {
                c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            }
            if (bool(op, "textAA") == Boolean.FALSE) {
                c.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            }
            if (bool(op, "smooth") == Boolean.FALSE) {
                c.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            }
            double alpha = number(op, "alpha");
            if (isFinite(alpha) && alpha < 1.0) {
                c.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                        (float) Math.max(0, Math.min(1, alpha))));
            }
            NElement clip = field(op, "clip");
            if (clip != null) {
                Shape s = shapeOf(clip);
                if (s != null) {
                    //clip() intersects in user space, exactly as ctx.clip() does
                    c.clip(s);
                }
            }
            dispatch(c, op, kind);
        } finally {
            c.dispose();
        }
    }

    /**
     * The recorded affine is absolute, so the device transform multiplies it on
     * the left instead of composing with a previous operation.
     */
    private static AffineTransform absoluteTransform(NElement op, AffineTransform device) {
        AffineTransform t = new AffineTransform(device);
        double[] m = matrix(op, "transform");
        if (m != null) {
            t.concatenate(new AffineTransform(m));
        }
        return t;
    }

    // -------------------------------------------------------------------- ops

    private void dispatch(Graphics2D c, NElement op, String kind) {
        switch (kind) {
            case "fill": {
                Shape s = shapeValue(op, "shape");
                if (s != null) {
                    c.setPaint(paintStyle(op));
                    c.fill(s);
                }
                break;
            }
            case "draw": {
                Shape s = shapeValue(op, "shape");
                if (s != null) {
                    c.setPaint(paintStyle(op));
                    applyStroke(c, op);
                    c.draw(s);
                }
                break;
            }
            case "fillRect": {
                if (rectIsFinite(op)) {
                    c.setPaint(paintStyle(op));
                    c.fillRect((int) Math.round(number(op, "x")), (int) Math.round(number(op, "y")),
                            (int) Math.round(number(op, "width")), (int) Math.round(number(op, "height")));
                }
                break;
            }
            case "clearRect": {
                if (rectIsFinite(op)) {
                    c.clearRect((int) number(op, "x"), (int) number(op, "y"),
                            (int) number(op, "width"), (int) number(op, "height"));
                }
                break;
            }
            case "fillRoundRect":
            case "fillOval": {
                Shape s = rectLikeShape(op, kind);
                if (s != null) {
                    c.setPaint(paintStyle(op));
                    c.fill(s);
                }
                break;
            }
            case "drawRect":
            case "drawRoundRect":
            case "drawOval":
            case "drawLine": {
                Shape s = rectLikeShape(op, kind);
                if (s != null) {
                    c.setPaint(paintStyle(op));
                    applyStroke(c, op);
                    c.draw(s);
                }
                break;
            }
            case "fillArc":
            case "drawArc": {
                Shape s = arcOf(op, kind);
                if (s != null) {
                    c.setPaint(paintStyle(op));
                    if (kind.equals("fillArc")) {
                        c.fill(s);
                    } else {
                        applyStroke(c, op);
                        c.draw(s);
                    }
                }
                break;
            }
            case "fillPolygon": {
                c.setPaint(paintStyle(op));
                c.fill(pointsShape(op, true));
                break;
            }
            case "drawPolygon": {
                c.setPaint(paintStyle(op));
                applyStroke(c, op);
                c.draw(pointsShape(op, true));
                break;
            }
            case "drawPolyline": {
                c.setPaint(paintStyle(op));
                applyStroke(c, op);
                c.draw(pointsShape(op, false));
                break;
            }
            case "text":
            case "chars":
            case "bytes":
            case "glyphs": {
                drawText(c, op);
                break;
            }
            case "image":
            case "renderedImage": {
                drawImageOp(c, op);
                break;
            }
            case "imagePart": {
                drawImagePart(c, op);
                break;
            }
            case "imageOp": {
                if (field(op, "ref") != null) {
                    drawImageOp(c, op);
                } else {
                    warn("imageOp: a java2d filter baked its own pixels");
                }
                break;
            }
            case "fill3DRect":
            case "draw3DRect": {
                //the bevel is two extra shaded polygons; what ships is the flat rect
                warn(kind + ": replayed without the bevel");
                Shape s = rectLikeShape(op, kind.equals("fill3DRect") ? "fillRect" : "drawRect");
                if (s != null) {
                    c.setPaint(paintStyle(op));
                    if (kind.equals("fill3DRect")) {
                        c.fill(s);
                    } else {
                        applyStroke(c, op);
                        c.draw(s);
                    }
                }
                break;
            }
            case "copyArea":
            case "renderableImage": {
                warn(kind);
                break;
            }
            default: {
                warn("operation: " + kind);
                break;
            }
        }
    }

    /**
     * Text is a glyph outline decided by java at record time, translated to the
     * recorded baseline. Only the outline-less fallback needs a real font, and
     * then it is the font the stream names rather than the browser default.
     */
    private void drawText(Graphics2D c, NElement op) {
        double[] at = point(op, "at");
        if (at == null) {
            return;
        }
        Shape outline = shapeValue(op, "outline");
        c.translate(at[0], at[1]);
        c.setPaint(paintStyle(op));
        if (outline != null) {
            c.fill(outline);
            return;
        }
        String text = string(op, "s");
        if (text == null) {
            NElement textElement = field(op, "text");
            text = textElement == null ? null : string(textElement, "s");
        }
        if (text == null) {
            warn("text: no outline and no string");
            return;
        }
        c.setFont(fontOf(op));
        c.drawString(text, 0, 0);
    }

    private static java.awt.Font fontOf(NElement op) {
        NElement font = field(op, "font");
        if (font == null) {
            NElement text = field(op, "text");
            NElement fonts = text == null ? null : field(text, "fonts");
            if (fonts != null && fonts.asListContainer().isPresent()) {
                for (NElement candidate : fonts.asArray().get()) {
                    font = candidate;
                    break;
                }
            }
        }
        String family = font == null ? null : string(font, "family");
        int style = java.awt.Font.PLAIN;
        String named = font == null ? null : string(font, "style");
        if ("bold".equals(named)) {
            style = java.awt.Font.BOLD;
        } else if ("italic".equals(named)) {
            style = java.awt.Font.ITALIC;
        } else if ("bolditalic".equals(named)) {
            style = java.awt.Font.BOLD | java.awt.Font.ITALIC;
        }
        double size = font == null ? Double.NaN : number(font, "size");
        return new java.awt.Font(family == null ? java.awt.Font.SANS_SERIF : family, style,
                isFinite(size) ? (int) Math.round(size) : 12);
    }

    // ----------------------------------------------------------------- shapes

    /**
     * The shape or clip of a field: a four number array is the compact rectangle
     * form the recorder uses for rectangular clips, an object is one of the
     * semantic primitives or a raw path.
     */
    private Shape shapeValue(NElement op, String name) {
        return shapeOf(field(op, name));
    }

    private Shape shapeOf(NElement s) {
        if (s == null || s.isNull()) {
            return null;
        }
        if (s.asListContainer().isPresent() && !s.asObject().isPresent()) {
            //a four number array is the compact rectangular clip, not a shape object
            double[] r = numbers(s);
            if (r.length != 4 || !isFinite(r[0], r[1], r[2], r[3])) {
                return null;
            }
            return new Rectangle2D.Double(r[0], r[1], r[2], r[3]);
        }
        String kind = string(s, "k");
        if (kind == null) {
            return null;
        }
        //interned shapes are the same element object for every operation that
        //reuses them, so the geometry is built once per page
        String key = s.toString();
        Shape cached = shapeCache.get(key);
        if (cached != null) {
            return cached;
        }
        Shape built = buildShape(kind, s);
        if (built != null) {
            shapeCache.put(key, built);
        }
        return built;
    }

    private Shape buildShape(String kind, NElement s) {
        switch (kind) {
            case "path": {
                return pathOf(string(s, "d"));
            }
            case "rect": {
                return rectIsFinite(s)
                        ? new Rectangle2D.Double(number(s, "x"), number(s, "y"),
                        number(s, "width"), number(s, "height")) : null;
            }
            case "oval": {
                return rectIsFinite(s)
                        ? new Ellipse2D.Double(number(s, "x"), number(s, "y"),
                        number(s, "width"), number(s, "height")) : null;
            }
            case "line": {
                if (!isFinite(number(s, "x1"), number(s, "y1"), number(s, "x2"), number(s, "y2"))) {
                    return null;
                }
                Path2D p = new Path2D.Double();
                p.moveTo(number(s, "x1"), number(s, "y1"));
                p.lineTo(number(s, "x2"), number(s, "y2"));
                return p;
            }
            case "roundRect": {
                if (!rectIsFinite(s)) {
                    return null;
                }
                double aw = number(s, "arcwidth");
                double ah = number(s, "archeight");
                return new RoundRectangle2D.Double(number(s, "x"), number(s, "y"),
                        number(s, "width"), number(s, "height"),
                        isFinite(aw) ? aw : 0, isFinite(ah) ? ah : 0);
            }
            default: {
                warn("shape: " + kind);
                return null;
            }
        }
    }

    /**
     * The grammar of {@link PaintStreamFormat#shapePath}: one command letter per
     * token, coordinates comma separated, only M L Q C Z. Curves stay curves; the
     * recorder never flattened them.
     */
    private static Shape pathOf(String d) {
        Path2D p = new Path2D.Double();
        if (d == null || d.isEmpty()) {
            return p;
        }
        for (String token : d.split(" ")) {
            if (token.isEmpty()) {
                continue;
            }
            char command = token.charAt(0);
            double[] v = coordinates(token.substring(1));
            switch (command) {
                case 'M': {
                    if (v.length >= 2) {
                        p.moveTo(v[0], v[1]);
                    }
                    break;
                }
                case 'L': {
                    if (v.length >= 2) {
                        p.lineTo(v[0], v[1]);
                    }
                    break;
                }
                case 'Q': {
                    if (v.length >= 4) {
                        p.quadTo(v[0], v[1], v[2], v[3]);
                    }
                    break;
                }
                case 'C': {
                    if (v.length >= 6) {
                        p.curveTo(v[0], v[1], v[2], v[3], v[4], v[5]);
                    }
                    break;
                }
                case 'Z': {
                    p.closePath();
                    break;
                }
                default: {
                    return null;
                }
            }
        }
        return p;
    }

    private static double[] coordinates(String text) {
        if (text.isEmpty()) {
            return new double[0];
        }
        String[] parts = text.split(",");
        List<Double> values = new ArrayList<>(parts.length);
        for (String part : parts) {
            try {
                values.add(Double.parseDouble(part));
            } catch (NumberFormatException ex) {
                return new double[0];
            }
        }
        double[] out = new double[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    /**
     * A polygon or polyline shipped as one string of {@code x,y} pairs.
     */
    private static Shape pointsShape(NElement op, boolean close) {
        GeneralPath p = new GeneralPath();
        String points = string(op, "points");
        if (points == null) {
            return p;
        }
        String[] pairs = points.split(" ");
        for (int i = 0; i < pairs.length; i++) {
            String[] xy = pairs[i].split(",");
            if (xy.length < 2) {
                continue;
            }
            double x;
            double y;
            try {
                x = Double.parseDouble(xy[0]);
                y = Double.parseDouble(xy[1]);
            } catch (NumberFormatException ex) {
                continue;
            }
            if (i == 0) {
                p.moveTo(x, y);
            } else {
                p.lineTo(x, y);
            }
        }
        if (close) {
            p.closePath();
        }
        return p;
    }

    /**
     * The rectangle-shaped operations reach java as the shape they were painted
     * from, which is why the arc keeps java's own angle convention instead of the
     * canvas negation the player needs.
     */
    private static Shape rectLikeShape(NElement op, String kind) {
        switch (kind) {
            case "fillRoundRect":
            case "drawRoundRect": {
                if (!rectIsFinite(op)) {
                    return null;
                }
                return new RoundRectangle2D.Double(number(op, "x"), number(op, "y"),
                        number(op, "width"), number(op, "height"),
                        number(op, "arcwidth"), number(op, "archeight"));
            }
            case "fillOval":
            case "drawOval": {
                return rectIsFinite(op)
                        ? new Ellipse2D.Double(number(op, "x"), number(op, "y"),
                        number(op, "width"), number(op, "height")) : null;
            }
            case "drawLine": {
                if (!isFinite(number(op, "x1"), number(op, "y1"), number(op, "x2"), number(op, "y2"))) {
                    return null;
                }
                Path2D p = new Path2D.Double();
                p.moveTo(number(op, "x1"), number(op, "y1"));
                p.lineTo(number(op, "x2"), number(op, "y2"));
                return p;
            }
            default: {
                return rectIsFinite(op)
                        ? new Rectangle2D.Double(number(op, "x"), number(op, "y"),
                        number(op, "width"), number(op, "height")) : null;
            }
        }
    }

    private static Shape arcOf(NElement op, String kind) {
        double w = number(op, "width");
        double h = number(op, "height");
        if (!isFinite(number(op, "x"), number(op, "y"), w, h, number(op, "start"), number(op, "extent"))
                || w <= 0 || h <= 0) {
            return null;
        }
        return new Arc2D.Double(number(op, "x"), number(op, "y"), w, h,
                number(op, "start"), number(op, "extent"),
                kind.equals("fillArc") ? Arc2D.PIE : Arc2D.OPEN);
    }

    // ----------------------------------------------------------------- paints

    /**
     * The paint an operation was recorded with. Gradients are built in the
     * operation's user space, which is where java resolved them, so the current
     * transform needs nothing extra.
     */
    private java.awt.Paint paintStyle(NElement op) {
        return paintOf(field(op, "paint"));
    }

    private java.awt.Paint paintOf(NElement paint) {
        if (paint == null || paint.isNull()) {
            return DEFAULT_PAINT;
        }
        if (paint.asObject().isPresent()) {
            String key = paint.toString();
            java.awt.Paint cached = paintCache.get(key);
            if (cached != null) {
                return cached;
            }
            java.awt.Paint built = buildPaint(paint);
            paintCache.put(key, built);
            return built;
        }
        Color color = colorOf(paint.asStringValue().orNull());
        return color == null ? DEFAULT_PAINT : color;
    }

    private java.awt.Paint buildPaint(NElement paint) {
        String kind = string(paint, "k");
        if ("linearGradient".equals(kind)) {
            double[] start = array(paint, "start");
            double[] end = array(paint, "end");
            List<float[]> fractions = new ArrayList<>();
            List<Color> colors = stops(paint, fractions);
            if (start == null || end == null || colors.size() < 2) {
                warn("paint: linearGradient has no usable geometry");
                return DEFAULT_PAINT;
            }
            return new LinearGradientPaint(new Point2D.Double(start[0], start[1]),
                    new Point2D.Double(end[0], end[1]), fractions.get(0),
                    colors.toArray(new Color[0]), cycleOf(paint));
        }
        if ("radialGradient".equals(kind)) {
            double[] center = array(paint, "center");
            double[] focus = array(paint, "focus");
            double radius = number(paint, "radius");
            List<float[]> fractions = new ArrayList<>();
            List<Color> colors = stops(paint, fractions);
            if (center == null || focus == null || !isFinite(radius) || radius <= 0 || colors.size() < 2) {
                warn("paint: radialGradient has no usable geometry");
                return DEFAULT_PAINT;
            }
            return new RadialGradientPaint((float) center[0], (float) center[1], (float) radius,
                    (float) focus[0], (float) focus[1], fractions.get(0),
                    colors.toArray(new Color[0]), cycleOf(paint));
        }
        warn("paint: " + (kind == null ? "unknown" : kind));
        return DEFAULT_PAINT;
    }

    private static MultipleGradientPaint.CycleMethod cycleOf(NElement paint) {
        //the recorder ships the java enum name, so NO_CYCLE is the plain one
        String cycle = string(paint, "cycle");
        if ("REPEAT".equals(cycle)) {
            return MultipleGradientPaint.CycleMethod.REPEAT;
        }
        if ("REFLECT".equals(cycle)) {
            return MultipleGradientPaint.CycleMethod.REFLECT;
        }
        return MultipleGradientPaint.CycleMethod.NO_CYCLE;
    }

    /**
     * Reads {@code stops:[[fraction,"#color"],...]}. A stop is a pair of mixed
     * types, so it is walked as elements rather than as a number array.
     *
     * @param fractionsOut receives one {@code float[]} entry, in the strictly
     * increasing order the java gradient constructors demand
     * @return the matching colors, empty when the stream carries none
     */
    private List<Color> stops(NElement paint, List<float[]> fractionsOut) {
        NElement stops = field(paint, "stops");
        List<Color> colors = new ArrayList<>();
        if (stops == null || !stops.asListContainer().isPresent()) {
            return colors;
        }
        for (NElement stop : stops.asArray().get()) {
            if (!stop.asListContainer().isPresent() || stop.asListContainer().get().size() < 2) {
                continue;
            }
            List<NElement> parts = new ArrayList<>();
            for (NElement part : stop.asArray().get()) {
                parts.add(part);
            }
            double fraction = parts.get(0).asDoubleValue().orElse(Double.NaN);
            String text = parts.get(1).isString() ? parts.get(1).asStringValue().orNull() : null;
            Color color = colorOf(text);
            if (!isFinite(fraction) || color == null) {
                continue;
            }
            colors.add(color);
            fractionsOut.add(new float[]{(float) Math.max(0, Math.min(1, fraction))});
        }
        if (colors.isEmpty()) {
            warn("paint: a gradient with no usable stop");
            return colors;
        }
        //java rejects fractions that are not strictly increasing, a browser clamps
        float[] f = new float[colors.size()];
        for (int i = 0; i < f.length; i++) {
            f[i] = fractionsOut.get(i)[0];
        }
        Color[] c = colors.toArray(new Color[0]);
        insertionSort(f, c);
        for (int i = 1; i < f.length; i++) {
            if (f[i] <= f[i - 1]) {
                f[i] = Math.min(Math.nextUp(f[i - 1]), 1.0f);
            }
        }
        colors.clear();
        colors.addAll(List.of(c));
        fractionsOut.clear();
        fractionsOut.add(f);
        return colors;
    }

    private static void insertionSort(float[] keys, Color[] values) {
        for (int i = 1; i < keys.length; i++) {
            float key = keys[i];
            Color value = values[i];
            int k = i - 1;
            while (k >= 0 && keys[k] > key) {
                keys[k + 1] = keys[k];
                values[k + 1] = values[k];
                k--;
            }
            keys[k + 1] = key;
            values[k + 1] = value;
        }
    }

    /**
     * The shipped color forms: {@code #rrggbb}, or {@code #rrggbbaa} when the
     * alpha is not 255. A 4 digit form is expanded the way CSS short hex is.
     */
    private static Color colorOf(String text) {
        if (text == null || text.isEmpty() || text.charAt(0) != '#') {
            return null;
        }
        String hex = text.substring(1);
        try {
            if (hex.length() == 3) {
                return new Color(Integer.parseInt(hex.substring(0, 1), 16) * 17,
                        Integer.parseInt(hex.substring(1, 2), 16) * 17,
                        Integer.parseInt(hex.substring(2, 3), 16) * 17);
            }
            if (hex.length() == 6) {
                return new Color(Integer.parseInt(hex, 16));
            }
            if (hex.length() == 8) {
                return new Color((int) Long.parseLong(hex, 16), true);
            }
        } catch (NumberFormatException ex) {
            return null;
        }
        return null;
    }

    private void applyStroke(Graphics2D c, NElement op) {
        NElement stroke = field(op, "stroke");
        if (stroke == null || stroke.isNull()) {
            return;
        }
        float width = (float) number(stroke, "width");
        int cap = capOf(string(stroke, "cap"));
        int join = joinOf(string(stroke, "join"));
        double limit = number(stroke, "miterlimit");
        float miter = isFinite(limit) ? (float) limit : 10f;
        NElement dash = field(stroke, "dash");
        float phase = (float) number(stroke, "dashphase");
        float[] pattern = dashPattern(dash);
        c.setStroke(pattern == null
                ? new BasicStroke(isFinite(width) ? Math.max(width, 0.02f) : 1f, cap, join, miter)
                : new BasicStroke(isFinite(width) ? Math.max(width, 0.02f) : 1f, cap, join, miter,
                pattern, isFinite(phase) ? phase : 0f));
    }

    /**
     * @return a usable dash pattern, or null for a solid stroke.
     */
    private static float[] dashPattern(NElement dash) {
        if (dash == null || !dash.asListContainer().isPresent()) {
            return null;
        }
        List<Float> collected = new ArrayList<>();
        for (NElement item : dash.asArray().get()) {
            double v = item.asDoubleValue().orElse(Double.NaN);
            if (isFinite(v) && v > 0) {
                collected.add((float) v);
            }
        }
        if (collected.isEmpty()) {
            return null;
        }
        float[] out = new float[collected.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = collected.get(i);
        }
        return out;
    }

    private static int capOf(String cap) {
        if ("round".equals(cap)) {
            return BasicStroke.CAP_ROUND;
        }
        if ("square".equals(cap)) {
            return BasicStroke.CAP_SQUARE;
        }
        return BasicStroke.CAP_BUTT;
    }

    private static int joinOf(String join) {
        if ("round".equals(join)) {
            return BasicStroke.JOIN_ROUND;
        }
        if ("bevel".equals(join)) {
            return BasicStroke.JOIN_BEVEL;
        }
        return BasicStroke.JOIN_MITER;
    }

    // ----------------------------------------------------------------- images

    private void drawImageOp(Graphics2D c, NElement op) {
        BufferedImage img = pixelsOf(op);
        if (img == null) {
            return;
        }
        Color bgcolor = colorOf(string(op, "bgcolor"));
        double[] at = matrix(op, "at");
        if (at != null) {
            //drawImage(img, AffineTransform) is how a transformed image lands
            c.transform(new AffineTransform(at));
            c.drawImage(img, 0, 0, bgcolor, null);
            return;
        }
        double[] dest = array(op, "dest");
        if (dest != null) {
            //the recorder captured integer placements, so device pixels stay exact
            c.drawImage(img, (int) dest[0], (int) dest[1], (int) dest[2], (int) dest[3], bgcolor, null);
            return;
        }
        double[] point = point(op, "at");
        if (point != null) {
            c.drawImage(img, (int) point[0], (int) point[1], bgcolor, null);
            return;
        }
        warn("image: no usable placement");
    }

    private void drawImagePart(Graphics2D c, NElement op) {
        BufferedImage img = pixelsOf(op);
        if (img == null) {
            return;
        }
        double[] dest = point(op, "dest");
        double[] dest2 = point(op, "dest2");
        double[] src = point(op, "src");
        double[] src2 = point(op, "src2");
        if (dest == null || dest2 == null || src == null || src2 == null) {
            return;
        }
        c.drawImage(img, (int) src[0], (int) src[1], (int) src2[0], (int) src2[1],
                (int) dest[0], (int) dest[1], (int) dest2[0], (int) dest2[1], null);
    }

    /**
     * The pixels an operation names, decoded once per SHA-256. Null when the
     * stream carries no reference or the asset is not available.
     */
    private BufferedImage pixelsOf(NElement op) {
        NElement ref = field(op, "ref");
        if (ref == null || !"png".equals(string(ref, "k"))) {
            warn("image: no pixels recorded");
            return null;
        }
        String sha = string(ref, "sha256");
        if (sha == null) {
            warn("image: no pixels recorded");
            return null;
        }
        BufferedImage known = decodedImages.get(sha);
        if (known != null) {
            return known;
        }
        if (images == null) {
            warn("image: no asset provider for " + sha);
            return null;
        }
        BufferedImage loaded = images.image(sha);
        if (loaded == null) {
            warn("image: " + sha + " did not load");
            return null;
        }
        decodedImages.put(sha, loaded);
        return loaded;
    }

    // ---------------------------------------------------------------- reading

    private static NElement field(NElement container, String name) {
        return container.asListContainer().flatMap(c -> c.get(name)).orNull();
    }

    private static String string(NElement container, String name) {
        NElement e = field(container, name);
        return e == null || e.isNull() ? null : e.asStringValue().orNull();
    }

    private static Boolean bool(NElement container, String name) {
        NElement e = field(container, name);
        return e == null ? null : e.asBooleanValue().orElse(null);
    }

    /**
     * A recorded number may be null: java rounds to three decimals and NaN has no
     * JSON spelling, so the recorder ships null and this returns NaN.
     */
    private static double number(NElement container, String name) {
        NElement e = container == null ? null : field(container, name);
        return e == null ? Double.NaN : e.asDoubleValue().orElse(Double.NaN);
    }

    private static double[] numbers(NElement array) {
        List<NElement> items = new ArrayList<>();
        for (NElement e : array.asArray().get()) {
            items.add(e);
        }
        double[] out = new double[items.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = items.get(i).asDoubleValue().orElse(Double.NaN);
        }
        return out;
    }

    private static double[] array(NElement container, String name) {
        NElement e = field(container, name);
        if (e == null || !e.asListContainer().isPresent() || e.asObject().isPresent()) {
            return null;
        }
        double[] v = numbers(e);
        for (double value : v) {
            if (!isFinite(value)) {
                return null;
            }
        }
        return v.length == 0 ? null : v;
    }

    private static double[] point(NElement container, String name) {
        double[] v = array(container, name);
        return v != null && v.length == 2 ? v : null;
    }

    /**
     * A six number array is an affine, which is how {@code drawImage} with a
     * transform and a rectangular clip differ from a point in the stream.
     */
    private static double[] matrix(NElement container, String name) {
        double[] v = array(container, name);
        return v != null && v.length == 6 ? v : null;
    }

    private static boolean rectIsFinite(NElement e) {
        return isFinite(number(e, "x"), number(e, "y"), number(e, "width"), number(e, "height"));
    }

    private static boolean isFinite(double... values) {
        for (double v : values) {
            if (!Double.isFinite(v)) {
                return false;
            }
        }
        return true;
    }

    private void warn(String detail) {
        warnings.add(detail);
    }
}
