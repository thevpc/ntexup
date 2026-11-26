package net.thevpc.ntexup.engine.renderer;

import net.thevpc.ntexup.api.document.elem2d.NTxLinearGradientPaint;

import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.NTxArrow;
import net.thevpc.ntexup.api.document.NTxArrowType;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.renderer.elem2d.Element2DUIFactory;
import net.thevpc.ntexup.engine.renderer.elem2d.ShapeFactory;
import net.thevpc.ntexup.engine.renderer.elem2d.strokes.CompositeStroke;
import net.thevpc.ntexup.engine.renderer.elem2d.strokes.StrokeFactory;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxGraphicsImageDrawer;
import net.thevpc.ntexup.api.renderer.text.NTxTextOptions;
import net.thevpc.nuts.concurrent.NScoredCallable;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.reflect.NScorable;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.ntexup.api.document.elem2d.primitives.*;
import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.nuts.util.NOptional;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.*;
import java.util.function.DoubleUnaryOperator;

public class NTxGraphicsImpl implements NTxGraphics {

    private Graphics2D g;
    private NTxEngine engine;
    private Color secondaryColor;
    private Element2DUIFactory element2DUIFactory = new Element2DUIFactory();
    private Map<Object, NTxGraphicsImageDrawer> imageCache = new HashMap<>();


    public NTxGraphicsImpl(Graphics2D g, NTxEngine engine) {
        this.g = g;
        this.engine = engine;
    }

    @Override
    public NTxEngine engine() {
        return engine;
    }

    @Override
    public NTxGraphics copy() {
        NTxGraphicsImpl hGraphics = new NTxGraphicsImpl((Graphics2D) g.create(),engine);
//        hGraphics.transform3D = transform3D;
//        hGraphics.light3D = light3D;
//        hGraphics.projection3D = projection3D;
        return hGraphics;
    }

    @Override
    public Font getFont() {
        return g.getFont();
    }

    @Override
    public void setColor(Color c) {
        g.setColor(c);

    }

    @Override
    public AffineTransform getTransform() {
        return g.getTransform();
    }

    @Override
    public void setTransform(AffineTransform tx) {
        g.setTransform(tx);
    }

    @Override
    public void transform(AffineTransform tx) {
        g.transform(tx);
    }

    @Override
    public void scale(double sx, double sy) {
        g.scale(sx, sy);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        g.rotate(theta, x, y);
    }

    @Override
    public void rotate(double theta) {
        g.rotate(theta);
    }

    @Override
    public void dispose() {
        g.dispose();
    }


    public Shape createShape(NElement e) {
        return ShapeFactory.createShape(e);
    }

    public Stroke createStroke(NElement e) {
        if (e == null || e.isNull()) {
            return null;
        }
        NTxValue o = NTxValue.of(e);
        if (o.name() != null) {
            return StrokeFactory.createStroke(o.name(), e, this);
        }
        switch (e.type()) {
            case ARRAY:
            case FULL_ARRAY:
            case PARAM_ARRAY:
            case NAMED_ARRAY:
            {
                return CompositeStroke.of(e, this);
            }
            case TUPLE:
            case NAMED_TUPLE:
            {
                return StrokeFactory.createBasic(o);
            }
            case NAME:
            case DOUBLE_QUOTED_STRING:
            case SINGLE_QUOTED_STRING:
            case BACKTICK_STRING:
            case TRIPLE_DOUBLE_QUOTED_STRING:
            case TRIPLE_SINGLE_QUOTED_STRING:
            case TRIPLE_BACKTICK_STRING:
            case LINE_STRING:
            case BLOCK_STRING:
            {
                return StrokeFactory.createStroke(o.asStringOrName().get(), e, this);
            }
        }
        {
            NOptional<Float> d = o.asFloat();
            if (d.isPresent()) {
                return new BasicStroke(d.get());
            }
        }
        return new BasicStroke();
    }

    @Override
    public void setPaint(Paint c) {
        g.setPaint(c);
    }

    @Override
    public void drawArrayHead(NTxPoint2D origin, NTxVector2D direction, NTxArrow arrow) {
        if (arrow == null || arrow.getType() == null) {
            return;
        }
        NTxArrowType type = arrow.getType();
        Path2D.Double head = new Path2D.Double();
        if (type == NTxArrowType.DEFAULT) {
            type = NTxArrowType.SIMPLE;
        }
        double arrowWidth = arrow.getWidth();
        double arrowHeight = arrow.getHeight();
        if (arrowWidth <= 0) {
            arrowWidth = arrowHeight;
        }
        if (arrowHeight <= 0) {
            arrowHeight = arrowWidth;
        }

        double angle = Math.atan2(direction.y, direction.x);
        AffineTransform originalTransform = g.getTransform();
        g.translate(origin.x, origin.y);
        g.rotate(angle);
        switch (type) {
            case SIMPLE: {
                if (arrowHeight <= 0) {
                    arrowHeight = 10;
                }
                if (arrowWidth <= 0) {
                    arrowWidth = 10;
                }
                head.moveTo(-arrowWidth, -arrowHeight);
                head.lineTo(0, 0);
                head.lineTo(-arrowWidth, arrowHeight);
                g.draw(head);
                break;
            }
            case TRIANGLE_FULL:
            case TRIANGLE: {
                if (arrowHeight <= 0) {
                    arrowHeight = 10;
                }
                if (arrowWidth <= 0) {
                    arrowWidth = 10;
                }
                head.moveTo(0, 0);
                head.lineTo(-arrowWidth, -arrowHeight);
                head.lineTo(-arrowWidth, arrowHeight);
                head.closePath();
                if (type == NTxArrowType.TRIANGLE_FULL) {
                    g.fill(head);
                } else {
                    g.draw(head);
                }
                break;
            }
            // diamond, oval and rectangle heads share the triangle geometry: they
            // extend 'width' back along the line from the tip and +/- 'height'
            // across it (they used to be drawn at half their declared size, so a
            // 14x9 head showed up as a barely visible 7x4.5 blob)
            case DIAMOND_FULL:
            case DIAMOND: {
                if (arrowWidth <= 0) {
                    arrowWidth = 14;
                }
                if (arrowHeight <= 0) {
                    arrowHeight = arrowWidth / 2;
                }
                head.moveTo(0, 0);
                head.lineTo(-arrowWidth / 2, -arrowHeight);
                head.lineTo(-arrowWidth, 0);
                head.lineTo(-arrowWidth / 2, arrowHeight);
                head.closePath();
                if (type == NTxArrowType.DIAMOND_FULL) {
                    g.fill(head);
                } else {
                    g.draw(head);
                }
                break;
            }
            case OVAL_FULL:
            case OVAL: {
                if (arrowWidth <= 0) {
                    arrowWidth = 10;
                }
                if (arrowHeight <= 0) {
                    arrowHeight = arrowWidth / 2;
                }
                Ellipse2D.Double arrowHead = new Ellipse2D.Double(-arrowWidth, -arrowHeight, arrowWidth, 2 * arrowHeight);
                if (type == NTxArrowType.OVAL_FULL) {
                    g.fill(arrowHead);
                } else {
                    g.draw(arrowHead);
                }
                break;
            }
            case RECTANGLE:
            case RECTANGLE_FULL: {
                if (arrowWidth <= 0) {
                    arrowWidth = 10;
                }
                if (arrowHeight <= 0) {
                    arrowHeight = arrowWidth / 2;
                }
                Rectangle2D.Double arrowHead = new Rectangle2D.Double(-arrowWidth, -arrowHeight, arrowWidth, 2 * arrowHeight);
                if (type == NTxArrowType.RECTANGLE_FULL) {
                    g.fill(arrowHead);
                } else {
                    g.draw(arrowHead);
                }
                break;
            }
            default: {
                //unsupported arrow type
                break;
            }
        }
        g.setTransform(originalTransform);
    }

    private double computeArrowSetback(NTxArrow arrow) {
        if (arrow == null || arrow.getType() == null) {
            return 0;
        }
        NTxArrowType type = arrow.getType() == NTxArrowType.DEFAULT
                ? NTxArrowType.SIMPLE : arrow.getType();
        double width = arrow.getWidth();
        double height = arrow.getHeight();
        if (width <= 0) {
            width = height;
        }
        if (height <= 0) {
            height = width;
        }
        return switch (type) {
            case SIMPLE -> 0;
            case TRIANGLE, TRIANGLE_FULL -> width > 0 ? width : 10;
            case DIAMOND, DIAMOND_FULL -> width > 0 ? width : 14;
            case OVAL, OVAL_FULL, RECTANGLE, RECTANGLE_FULL -> width > 0 ? width : 10;
            default -> 0;
        };
    }

    @Override
    public void draw2D(NtxElement2D element2D) {
        NtxElement2DPrimitive[] primitives = element2DUIFactory.toPrimitives(element2D);
        for (NtxElement2DPrimitive primitive : primitives) {
            switch (primitive.type()) {
                case LINE: {
                    draw2DHElement2DLine((NtxElement2DLine) primitive);
                    break;
                }
                case QUAD_CURVE: {
                    draw2DHElement2DQuadCurve((NtxElement2DQuadCurve) primitive);
                    break;
                }
                case CUBIC_CURVE: {
                    draw2DHElement2DCubicCurve((NtxElement2DCubicCurve) primitive);
                    break;
                }
                case POLYLINE: {
                    draw2DHElement2DPolyline((NtxElement2DPolyline) primitive);
                    break;
                }
                case POLYGON: {
                    draw2DHElement2DPolygon((NtxElement2DPolygon) primitive);
                    break;
                }
            }
        }
    }


    @Override
    public void fillOval(double x, double y, double w, double h) {
        if (hasRelativeGradient()) {
            fillWithRelativeGradient(new Ellipse2D.Double((int) x, (int) y, (int) w, (int) h));
            return;
        }
        g.fillOval((int) x, (int) y, (int) w, (int) h);
    }

    @Override
    public void drawOval(double x, double y, double w, double h) {
        g.drawOval((int) x, (int) y, (int) w, (int) h);
    }

    @Override
    public void fillRect(double x, double y, double w, double h) {
        if (hasRelativeGradient()) {
            fillWithRelativeGradient(new Rectangle2D.Double((int) x, (int) y, (int) w, (int) h));
            return;
        }
        g.fillRect((int) x, (int) y, (int) w, (int) h);
    }

    @Override
    public void drawRect(double x, double y, double w, double h) {
        g.drawRect((int) x, (int) y, (int) w, (int) h);
    }


    @Override
    public Rectangle2D getStringBounds(String str) {
        return getFontMetrics().getStringBounds(str == null ? "" : str, graphics2D());
    }

    @Override
    public Rectangle2D getStringBounds(AttributedCharacterIterator iterator) {
        FontRenderContext frc = g.getFontRenderContext();
        TextLayout textLayout = new TextLayout(iterator, frc);
        return textLayout.getBounds();
    }

    @Override
    public FontMetrics getFontMetrics() {
        return g.getFontMetrics();
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return g.getFontMetrics(f);
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return g.getFontRenderContext();
    }

    @Override
    public void fillRect(NTxBounds2D a) {
        fillRect(
                NTxUtils.doubleOf(a.minX()), NTxUtils.intOf(a.minY()),
                NTxUtils.intOf(a.widthX()), NTxUtils.intOf(a.widthY())
        );
    }

    @Override
    public void drawRect(NTxBounds2D a) {
        drawRect(
                NTxUtils.doubleOf(a.minX()), NTxUtils.doubleOf(a.minY()),
                NTxUtils.doubleOf(a.widthX()), NTxUtils.doubleOf(a.widthY())
        );
    }

    @Override
    public Graphics2D graphics2D() {
        return g;
    }

    @Override
    public void setFont(Font font) {
        // java.awt.Graphics contract: a null font is silently ignored.
        // Some Graphics2D implementations (PdfBoxGraphics2D) store it and NPE later.
        if (font != null) {
            g.setFont(font);
        }
    }

    @Override
    public void drawString(String str, double x, double y) {
        g.drawString(str == null ? "" : str, (int) x, (int) y);
    }

    @Override
    public void drawString(String str, double x, double y, NTxTextOptions options) {
        if (str == null || str.length() == 0) {
            return;
        }
        Paint oldPaint = g.getPaint();
        Font oldFont = g.getFont();
        Stroke oldStroke = g.getStroke();
        if (options.getStroke() != null) {
            g.setStroke(options.getStroke());
        }
        if (options.getBaseFont() != null) {
            g.setFont(options.getBaseFont());
        }
//        NTxPoint2D sht = options.getShadowTranslation();
//        if (options.getShadowColor() != null && sht != null && !sht.equals(new NTxPoint2D(0, 0))) {
//            if (options.isStyled()) {
//                AttributedString attrStr = options.createShadowAttributedString(str, this);
//                g.drawString(attrStr.getIterator(), (float) (x + sht.getX()), (float) (y + sht.getY()));
//            } else {
//                g.setPaint(options.getShadowColor());
//                g.drawString(str, (float) (x + sht.getX()), (float) (y + sht.getY()));
//            }
//        }
        {
            if (options.isStyled()) {
                AttributedString attrStr = options.createAttributedString(str, this);
                g.drawString(attrStr.getIterator(), (float) (x), (float) (y));
            } else {
                g.setPaint(options.getForegroundColor());
                g.drawString(str, (float) (x), (float) (y));
            }
        }
        g.setPaint(oldPaint);
        g.setFont(oldFont);
        g.setStroke(oldStroke);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, double x, double y) {
        if (iterator == null) {
            return;
        }
        g.drawString(iterator, (float) x, (float) y);
    }

    @Override
    public void debugString(String str, double x, double y) {
        Color c = g.getColor();
        Font of = g.getFont();
        g.setColor(Color.RED);
        Font arial = new Font("Courrier", Font.PLAIN, 16);
        g.setFont(arial);
        int debugRow = 0;
        Rectangle2D fm = g.getFontMetrics(arial).getStringBounds("Abcdefghijklmnopqrstuvwxyz", g);
        for (String n : String.valueOf(str).trim().split("\n")) {
            g.drawString(n, (int) x, (int) (y + fm.getHeight() * debugRow));
            debugRow++;
        }
        g.setFont(of);
        g.setColor(c);
    }

    @Override
    public Color getColor() {
        return g.getColor();
    }

    @Override
    public void drawArc(double x, double y, double w, double h, double startAngle, double endAngle) {
        g.drawArc((int) x, (int) y, (int) w, (int) h, (int) startAngle, (int) endAngle);
    }

    @Override
    public void drawLine(double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }


    @Override
    public void fillPolygon(double[] xx, double[] yy, int length) {
        if (hasRelativeGradient()) {
            fillWithRelativeGradient(new Polygon(
                    Arrays.stream(xx).mapToInt(d -> (int) d).toArray(),
                    Arrays.stream(yy).mapToInt(d -> (int) d).toArray(),
                    length));
            return;
        }
        g.fillPolygon(
                Arrays.stream(xx).mapToInt(d -> (int) d).toArray(),
                Arrays.stream(yy).mapToInt(d -> (int) d).toArray(),
                length
        );
    }

    @Override
    public void drawPolygon(double[] xx, double[] yy, int length) {
        g.drawPolygon(
                Arrays.stream(xx).mapToInt(d -> (int) d).toArray(),
                Arrays.stream(yy).mapToInt(d -> (int) d).toArray(),
                length
        );
    }

    @Override
    public void drawPolyline(double[] xx, double[] yy, int length) {
        if (length <= 0) {
            return;
        }
        Path2D.Double path = new Path2D.Double();
        path.moveTo(xx[0], yy[0]);
        for (int i = 1; i < length; i++) {
            path.lineTo(xx[i], yy[i]);
        }
        g.draw(path);
    }

    @Override
    public void fillRoundRect(double x, double y, double w, double h, double cx, double cy) {
        fill(new RoundRectangle2D.Double(x, y, w, h, cx, cy));
    }

    @Override
    public void drawRoundRect(double x, double y, double w, double h, double cx, double cy) {
        g.draw(new RoundRectangle2D.Double(x, y, w, h, cx, cy));
    }

    @Override
    public void fill3DRect(double x, double y, double w, double h, boolean raised) {
        g.fill3DRect((int) x, (int) y, (int) w, (int) h, raised);
    }

    @Override
    public void draw3DRect(double x, double y, double w, double h, boolean raised) {
        g.draw3DRect((int) x, (int) y, (int) w, (int) h, raised);
    }

    @Override
    public void drawImage(Image image, double x, double y, ImageObserver o) {
        g.drawImage(image, (int) x, (int) y, o);
    }

    @Override
    public void drawImage(NPath nPath, double x, double y, NTxImageOptions options) {
        NTxGraphicsImageDrawer o = imageCache.get(nPath);
        if (o == null) {
            NOptional<NTxGraphicsImageDrawer> tr = NScorable.<NScoredCallable<NTxGraphicsImageDrawer>>query()
                            .withName(NMsg.ofC("support for image %s ", nPath))
                                    .fromStream(engine.imageTypeRendererFactories().stream()
                                    .map(n -> n.resolveRenderer(nPath, options, NTxGraphicsImpl.this))
                            )
                    .best().map(v->v.call())
                    ;
            if (tr.isPresent()) {
                putCache(new Object[]{nPath}, o = tr.get());
            }
        }
        if (o != null) {
            o.drawImage(x, y, options, this);
        } else {
            NTxGraphicsImageDrawerByPath d = new NTxGraphicsImageDrawerByPath(nPath);
            d.drawImage(x, y, options, this);
        }
    }

    @Override
    public Color getSecondaryColor() {
        return secondaryColor;
    }

    @Override
    public void setSecondaryColor(Color secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    @Override
    public void setComposite(Composite composite) {
        g.setComposite(composite);
    }

    @Override
    public void fill(Shape shape) {
        if (hasRelativeGradient()) {
            fillWithRelativeGradient(shape);
            return;
        }
        g.fill(shape);
    }

    private boolean hasRelativeGradient() {
        return g.getPaint() instanceof NTxLinearGradientPaint;
    }

    /**
     * {@link NTxLinearGradientPaint} expresses its axis in percents of the filled
     * shape and only resolves it lazily in {@code createContext}. Vector
     * backends (the PDF graphics) never call {@code createContext}: they only
     * understand the standard java.awt gradient paints and dropped the fill
     * ("Don't know paint ..."). Resolving it here against the shape bounds gives
     * them a plain {@link LinearGradientPaint} and the raster output is
     * unchanged.
     */
    private void fillWithRelativeGradient(Shape shape) {
        Paint old = g.getPaint();
        NTxLinearGradientPaint p = (NTxLinearGradientPaint) old;
        Rectangle2D b = shape.getBounds2D();
        Point2D s = new Point2D.Double(b.getX() + p.getStart().getX() / 100.0 * b.getWidth(),
                b.getY() + p.getStart().getY() / 100.0 * b.getHeight());
        Point2D e = new Point2D.Double(b.getX() + p.getEnd().getX() / 100.0 * b.getWidth(),
                b.getY() + p.getEnd().getY() / 100.0 * b.getHeight());
        if (s.distance(e) < 1e-4) {
            e = new Point2D.Double(s.getX() + 1, s.getY());
        }
        g.setPaint(new LinearGradientPaint(s, e, p.getFractions(), p.getColors(), p.getCycleMethod()));
        try {
            g.fill(shape);
        } finally {
            g.setPaint(old);
        }
    }

    @Override
    public void draw(Shape shape) {
        g.draw(shape);
    }

    @Override
    public void setStroke(Stroke stroke) {
        g.setStroke(stroke);
    }

    @Override
    public Stroke getStroke() {
        return g.getStroke();
    }

    @Override
    public void fillSphere(double x, double y, double w, double h, double lightAngle, float radius) {
        Color c = getColor();
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        double wh = Math.max(w, h);

        double x0 = x + w / 2;
        double y0 = y + h / 2;
        double radAngle = lightAngle / 180 * Math.PI;
        if (radius > 100) {
            radius = 100;
        } else if (radius < 0) {
            radius = 0;
        }

        int max = (wh <= 10) ? 10 : (wh <= 100) ? 30 : (wh <= 200) ? 50 : 100;
        for (int i = 0; i <= max; i++) {
            double r = i * 1.0 / max * radius;
            double lx = x0 + Math.cos(radAngle) * (r / 100) * w / 2;
            double ly = y0 + Math.sin(radAngle) * (r / 100) * h / 2;
            double lw = (radius - r) / 100 * w * 2;
            double lh = (radius - r) / 100 * h * 2;
            setColor(Color.getHSBColor(hsb[0], 1 - (i * 1.f / max), hsb[2]));
            fillOval(lx - (lw) / 2, ly - lh / 2, lw, lh);
        }
    }


    public void shear(double shx, double shy) {
        g.shear(shx, shy);
    }

    private NTxArrow scaleArrow(NTxArrow arrow, double len, double maxRatio) {
        if (arrow == null || arrow.getType() == null) {
            return arrow;
        }
        double nominalW = arrow.getWidth();
        double nominalH = arrow.getHeight();
        if (nominalW <= 0 && nominalH <= 0) {
            nominalW = 10;
            nominalH = 10;
        } else if (nominalW <= 0) {
            nominalW = nominalH;
        } else if (nominalH <= 0) {
            nominalH = nominalW;
        }
        double maxW = len * maxRatio;
        if (nominalW > maxW && maxW > 0) {
            double scale = maxW / nominalW;
            return new NTxArrow(arrow.getType(), maxW, nominalH * scale);
        }
        return arrow;
    }

    private void draw2DHElement2DLine(NtxElement2DLine pr) {
        NTxPoint2D a = pr.getFrom();
        NTxPoint2D b = pr.getTo();

        // Compute the direction vector and its length
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double len = Math.sqrt(dx * dx + dy * dy);

        NTxArrow startArrow = pr.getStartArrow();
        NTxArrow endArrow   = pr.getEndArrow();

        // Adjusted endpoints: pull each end back by the arrow's setback distance
        NTxPoint2D lineStart = a;
        NTxPoint2D lineEnd   = b;

        if (len > 1e-9) {
            boolean hasStart = (startArrow != null && startArrow.getType() != null);
            boolean hasEnd   = (endArrow != null && endArrow.getType() != null);
            if (hasStart && hasEnd) {
                startArrow = scaleArrow(startArrow, len, 0.3);
                endArrow   = scaleArrow(endArrow, len, 0.3);
            } else if (hasStart) {
                startArrow = scaleArrow(startArrow, len, 0.45);
            } else if (hasEnd) {
                endArrow   = scaleArrow(endArrow, len, 0.45);
            }

            double ux = dx / len; // unit vector a→b
            double uy = dy / len;

            double setbackA = computeArrowSetback(startArrow);
            double setbackB = computeArrowSetback(endArrow);

            // Clamp so the two setbacks don't cross each other
            double totalSetback = setbackA + setbackB;
            if (totalSetback > len) {
                double scale = len / totalSetback;
                setbackA *= scale;
                setbackB *= scale;
            }

            lineStart = new NTxPoint2D(a.x + ux * setbackA, a.y + uy * setbackA);
            lineEnd   = new NTxPoint2D(b.x - ux * setbackB, b.y - uy * setbackB);
        }

        Paint  oldPaint  = g.getPaint();
        Stroke oldStroke = g.getStroke();

        if (pr.getLinePaint()  != null) setPaint(pr.getLinePaint());
        if (pr.getLineStroke() != null) setStroke(pr.getLineStroke());

        drawLine(lineStart.x, lineStart.y, lineEnd.x, lineEnd.y);

        setStroke(oldStroke); // restore before drawing arrows (they use their own geometry)

        // Arrow directions: start arrow points from b toward a (into point a)
        //                   end   arrow points from a toward b (into point b)
        drawArrayHead(a, a.minus(b).asVector(), startArrow);
        drawArrayHead(b, b.minus(a).asVector(), endArrow);

        setPaint(oldPaint);
        setStroke(oldStroke);
    }

    private void draw2DHElement2DQuadCurve(NtxElement2DQuadCurve pr) {
        NTxPoint2D a = pr.getFrom();
        NTxPoint2D b = pr.getTo();
        NTxPoint2D c = pr.getCtrl();
        Paint oldPaint = g.getPaint();
        Stroke oldStroke = g.getStroke();
        if (pr.getLinePaint() != null) {
            setPaint(pr.getLinePaint());
        }
        if (pr.getLineStroke() != null) {
            setStroke(pr.getLineStroke());
        }
        QuadCurve2D q = new QuadCurve2D.Double();
        q.setCurve(a.x, a.y, c.x, c.y, b.x, b.y);

        NTxArrow startArrow = pr.getStartArrow();
        NTxArrow endArrow = pr.getEndArrow();
        boolean hasStart = startArrow != null && startArrow.getType() != null;
        boolean hasEnd = endArrow != null && endArrow.getType() != null;
        if (hasStart || hasEnd) {
            CurveProfile profile = quadProfile(q);
            double ratio = hasStart && hasEnd ? 0.3 : 0.45;
            if (hasStart) {
                startArrow = scaleArrow(startArrow, profile.length(), ratio);
            }
            if (hasEnd) {
                endArrow = scaleArrow(endArrow, profile.length(), ratio);
            }
            double startSetback = computeArrowSetback(startArrow);
            double endSetback = computeArrowSetback(endArrow);
            if (startSetback + endSetback > profile.length() && profile.length() > 0) {
                double scale = profile.length() / (startSetback + endSetback);
                startSetback *= scale;
                endSetback *= scale;
            }
            QuadCurve2D shaft = trimQuadCurve(q, profile, startSetback, endSetback);
            if (shaft != null) {
                g.draw(shaft);
            }
        } else {
            g.draw(q);
        }

        setStroke(oldStroke);
        drawArrayHead(a, a.minus(c).asVector(), startArrow);
        drawArrayHead(b, b.minus(c).asVector(), endArrow);
        setPaint(oldPaint);
        setStroke(oldStroke);
    }

    private void draw2DHElement2DCubicCurve(NtxElement2DCubicCurve pr) {
        NTxPoint2D a = pr.getFrom();
        NTxPoint2D b = pr.getTo();
        NTxPoint2D c1 = pr.getCtrl1();
        NTxPoint2D c2 = pr.getCtrl2();
        Paint oldPaint = g.getPaint();
        Stroke oldStroke = g.getStroke();
        if (pr.getLinePaint() != null) {
            setPaint(pr.getLinePaint());
        }
        if (pr.getLineStroke() != null) {
            setStroke(pr.getLineStroke());
        }
        CubicCurve2D q = new CubicCurve2D.Double();
        q.setCurve(a.x, a.y, c1.x, c1.y, c2.x, c2.y, b.x, b.y);

        NTxArrow startArrow = pr.getStartArrow();
        NTxArrow endArrow = pr.getEndArrow();
        boolean hasStart = startArrow != null && startArrow.getType() != null;
        boolean hasEnd = endArrow != null && endArrow.getType() != null;
        if (hasStart || hasEnd) {
            CurveProfile profile = cubicProfile(q);
            double ratio = hasStart && hasEnd ? 0.3 : 0.45;
            if (hasStart) {
                startArrow = scaleArrow(startArrow, profile.length(), ratio);
            }
            if (hasEnd) {
                endArrow = scaleArrow(endArrow, profile.length(), ratio);
            }
            double startSetback = computeArrowSetback(startArrow);
            double endSetback = computeArrowSetback(endArrow);
            if (startSetback + endSetback > profile.length() && profile.length() > 0) {
                double scale = profile.length() / (startSetback + endSetback);
                startSetback *= scale;
                endSetback *= scale;
            }
            CubicCurve2D shaft = trimCubicCurve(q, profile, startSetback, endSetback);
            if (shaft != null) {
                g.draw(shaft);
            }
        } else {
            g.draw(q);
        }

        setStroke(oldStroke);
        drawArrayHead(a, a.minus(c1).asVector(), startArrow);
        drawArrayHead(b, b.minus(c2).asVector(), endArrow);
        setPaint(oldPaint);
        setStroke(oldStroke);
    }

    private static final int CURVE_TRIM_SAMPLES = 256;
    private static final int CURVE_TRIM_ITERATIONS = 24;

    private record CurveProfile(double[] distances, double length, DoubleUnaryOperator speed) {
    }

    private static CurveProfile quadProfile(QuadCurve2D q) {
        return curveProfile(t -> {
            double u = 1 - t;
            double x = 2 * (u * (q.getCtrlX() - q.getX1()) + t * (q.getX2() - q.getCtrlX()));
            double y = 2 * (u * (q.getCtrlY() - q.getY1()) + t * (q.getY2() - q.getCtrlY()));
            return Math.hypot(x, y);
        });
    }

    private static CurveProfile cubicProfile(CubicCurve2D q) {
        return curveProfile(t -> {
            double u = 1 - t;
            double x = 3 * (u * u * (q.getCtrlX1() - q.getX1())
                    + 2 * u * t * (q.getCtrlX2() - q.getCtrlX1())
                    + t * t * (q.getX2() - q.getCtrlX2()));
            double y = 3 * (u * u * (q.getCtrlY1() - q.getY1())
                    + 2 * u * t * (q.getCtrlY2() - q.getCtrlY1())
                    + t * t * (q.getY2() - q.getCtrlY2()));
            return Math.hypot(x, y);
        });
    }

    private static CurveProfile curveProfile(DoubleUnaryOperator speed) {
        double[] distances = new double[CURVE_TRIM_SAMPLES + 1];
        for (int i = 0; i < CURVE_TRIM_SAMPLES; i++) {
            double t0 = i / (double) CURVE_TRIM_SAMPLES;
            double t1 = (i + 1) / (double) CURVE_TRIM_SAMPLES;
            distances[i + 1] = distances[i] + integratedSpeed(speed, t0, t1);
        }
        return new CurveProfile(distances, distances[CURVE_TRIM_SAMPLES], speed);
    }

    private static double integratedSpeed(DoubleUnaryOperator speed, double t0, double t1) {
        double mid = (t0 + t1) / 2;
        return (t1 - t0) / 6 * (speed.applyAsDouble(t0)
                + 4 * speed.applyAsDouble(mid) + speed.applyAsDouble(t1));
    }

    private static double parameterAtDistance(CurveProfile profile, double distance, boolean fromEnd) {
        if (distance <= 0) {
            return fromEnd ? 1 : 0;
        }
        if (distance >= profile.length()) {
            return fromEnd ? 0 : 1;
        }
        double target = fromEnd ? profile.length() - distance : distance;
        int low = 0;
        int high = CURVE_TRIM_SAMPLES;
        while (high - low > 1) {
            int mid = (low + high) / 2;
            if (profile.distances()[mid] <= target) {
                low = mid;
            } else {
                high = mid;
            }
        }
        double t0 = low / (double) CURVE_TRIM_SAMPLES;
        double t1 = high / (double) CURVE_TRIM_SAMPLES;
        double segmentLength = profile.distances()[high] - profile.distances()[low];
        if (segmentLength <= 0) {
            return t0;
        }
        double segmentStart = t0;
        double remaining = target - profile.distances()[low];
        for (int i = 0; i < CURVE_TRIM_ITERATIONS; i++) {
            double mid = (t0 + t1) / 2;
            if (integratedSpeed(profile.speed(), segmentStart, mid) < remaining) {
                t0 = mid;
            } else {
                t1 = mid;
            }
        }
        return (t0 + t1) / 2;
    }

    private static QuadCurve2D trimQuadCurve(QuadCurve2D q, CurveProfile profile,
                                             double startSetback, double endSetback) {
        if (profile.length() <= 1e-9) {
            return q;
        }
        double t0 = parameterAtDistance(profile, startSetback, false);
        double t1 = parameterAtDistance(profile, endSetback, true);
        if (t1 <= t0) {
            return null;
        }
        if (t0 <= 0 && t1 >= 1) {
            return q;
        }
        QuadCurve2D left = new QuadCurve2D.Double();
        QuadCurve2D right = new QuadCurve2D.Double();
        if (t1 < 1) {
            splitQuadAt(q, t1, left, right);
            q = left;
        }
        if (t0 > 0) {
            splitQuadAt(q, t0 / t1, left, right);
            return right;
        }
        return q;
    }

    private static void splitQuadAt(QuadCurve2D q, double t,
                                    QuadCurve2D left, QuadCurve2D right) {
        double x1 = q.getX1();
        double y1 = q.getY1();
        double x2 = q.getX2();
        double y2 = q.getY2();
        double ax = x1 + (q.getCtrlX() - x1) * t;
        double ay = y1 + (q.getCtrlY() - y1) * t;
        double bx = q.getCtrlX() + (x2 - q.getCtrlX()) * t;
        double by = q.getCtrlY() + (y2 - q.getCtrlY()) * t;
        double mx = ax + (bx - ax) * t;
        double my = ay + (by - ay) * t;
        right.setCurve(mx, my, bx, by, x2, y2);
        left.setCurve(x1, y1, ax, ay, mx, my);
    }

    private static CubicCurve2D trimCubicCurve(CubicCurve2D q, CurveProfile profile,
                                               double startSetback, double endSetback) {
        if (profile.length() <= 1e-9) {
            return q;
        }
        double t0 = parameterAtDistance(profile, startSetback, false);
        double t1 = parameterAtDistance(profile, endSetback, true);
        if (t1 <= t0) {
            return null;
        }
        if (t0 <= 0 && t1 >= 1) {
            return q;
        }
        CubicCurve2D left = new CubicCurve2D.Double();
        CubicCurve2D right = new CubicCurve2D.Double();
        if (t1 < 1) {
            splitCubicAt(q, t1, left, right);
            q = left;
        }
        if (t0 > 0) {
            splitCubicAt(q, t0 / t1, left, right);
            return right;
        }
        return q;
    }

    private static void splitCubicAt(CubicCurve2D q, double t,
                                     CubicCurve2D left, CubicCurve2D right) {
        double x1 = q.getX1();
        double y1 = q.getY1();
        double x2 = q.getX2();
        double y2 = q.getY2();
        double ax = x1 + (q.getCtrlX1() - x1) * t;
        double ay = y1 + (q.getCtrlY1() - y1) * t;
        double bx = q.getCtrlX1() + (q.getCtrlX2() - q.getCtrlX1()) * t;
        double by = q.getCtrlY1() + (q.getCtrlY2() - q.getCtrlY1()) * t;
        double cx = q.getCtrlX2() + (x2 - q.getCtrlX2()) * t;
        double cy = q.getCtrlY2() + (y2 - q.getCtrlY2()) * t;
        double dx = ax + (bx - ax) * t;
        double dy = ay + (by - ay) * t;
        double ex = bx + (cx - bx) * t;
        double ey = by + (cy - by) * t;
        double mx = dx + (ex - dx) * t;
        double my = dy + (ey - dy) * t;
        right.setCurve(mx, my, ex, ey, cx, cy, x2, y2);
        left.setCurve(x1, y1, ax, ay, dx, dy, mx, my);
    }

    private void draw2DHElement2DPolyline(NtxElement2DPolyline pr) {
        Paint oldPaint = g.getPaint();
        Stroke oldStroke = g.getStroke();
        if (pr.getLinePaint() != null) {
            setPaint(pr.getLinePaint());
        }
        if (pr.getLineStroke() != null) {
            setStroke(pr.getLineStroke());
        }
        NTxPoint2D[] nodes = pr.getNodes();
        NTxArrow startArrow = pr.getStartArrow();
        NTxArrow endArrow = pr.getEndArrow();

        NTxPoint2D[] drawNodes = nodes;
        if (nodes != null && nodes.length >= 2) {
            boolean hasStart = startArrow != null && startArrow.getType() != null;
            boolean hasEnd = endArrow != null && endArrow.getType() != null;

            // the arrow is sized against the adjacent segment (same rule as line)
            double lenStart = Math.hypot(nodes[1].x - nodes[0].x, nodes[1].y - nodes[0].y);
            double lenEnd = Math.hypot(nodes[nodes.length - 1].x - nodes[nodes.length - 2].x,
                    nodes[nodes.length - 1].y - nodes[nodes.length - 2].y);
            double ratio = (hasStart && hasEnd) ? 0.3 : 0.45;
            if (hasStart) {
                startArrow = scaleArrow(startArrow, lenStart, ratio);
            }
            if (hasEnd) {
                endArrow = scaleArrow(endArrow, lenEnd, ratio);
            }

            // pull each end back by the arrow's setback distance
            double setbackA = hasStart ? computeArrowSetback(startArrow) : 0;
            double setbackB = hasEnd ? computeArrowSetback(endArrow) : 0;
            if (nodes.length == 2 && setbackA + setbackB > lenStart) {
                // both arrows share one segment: keep the same rule as line
                double scale = lenStart > 0 ? lenStart / (setbackA + setbackB) : 0;
                setbackA *= scale;
                setbackB *= scale;
            } else {
                setbackA = Math.min(setbackA, lenStart);
                setbackB = Math.min(setbackB, lenEnd);
            }
            drawNodes = java.util.Arrays.copyOf(nodes, nodes.length);
            if (hasStart && lenStart > 1e-9) {
                NTxPoint2D a = nodes[0];
                NTxPoint2D b = nodes[1];
                drawNodes[0] = new NTxPoint2D(a.x + (b.x - a.x) / lenStart * setbackA,
                        a.y + (b.y - a.y) / lenStart * setbackA);
            }
            if (hasEnd && lenEnd > 1e-9) {
                NTxPoint2D a = nodes[nodes.length - 2];
                NTxPoint2D b = nodes[nodes.length - 1];
                drawNodes[nodes.length - 1] = new NTxPoint2D(b.x - (b.x - a.x) / lenEnd * setbackB,
                        b.y - (b.y - a.y) / lenEnd * setbackB);
            }
        }

        double[] xx = new double[drawNodes.length];
        double[] yy = new double[drawNodes.length];
        for (int i = 0; i < xx.length; i++) {
            NTxPoint2D pp = drawNodes[i];
            xx[i] = pp.x;
            yy[i] = pp.y;
        }
        drawPolyline(xx, yy, xx.length);

        setStroke(oldStroke);
        if (nodes != null && nodes.length >= 2) {
            if (startArrow != null && startArrow.getType() != null) {
                NTxPoint2D a = nodes[0];
                NTxPoint2D b = nodes[1];
                drawArrayHead(a, a.minus(b).asVector(), startArrow);
            }
            if (endArrow != null && endArrow.getType() != null) {
                NTxPoint2D a = nodes[nodes.length - 2];
                NTxPoint2D b = nodes[nodes.length - 1];
                drawArrayHead(b, b.minus(a).asVector(), endArrow);
            }
        }
        setPaint(oldPaint);
        setStroke(oldStroke);
    }

    private void draw2DHElement2DPolygon(NtxElement2DPolygon pr) {
        Paint oldPaint = g.getPaint();
        Stroke oldStroke = g.getStroke();
        if (pr.getLinePaint() != null) {
            setPaint(pr.getLinePaint());
        }
        if (pr.getLineStroke() != null) {
            setStroke(pr.getLineStroke());
        }
        NTxPoint2D[] nodes = pr.getNodes();
        int[] xx = new int[nodes.length];
        int[] yy = new int[nodes.length];
        for (int i = 0; i < xx.length; i++) {
            NTxPoint2D pp = nodes[i];
            xx[i] = (int) (pp.x);
            yy[i] = (int) (pp.y);
        }

        if (pr.isFill()) {
            Composite oldComposite = g.getComposite();

            if (pr.getLinePaint() != null) {
                setPaint(pr.getLinePaint());
            }
            if (pr.getLineStroke() != null) {
                setStroke(pr.getLineStroke());
            }

            Paint bg = pr.getBackgroundPaint();
            if (bg != null) {
                setPaint(bg);
            }
            if (pr.getComposite() != null) {
                setComposite(pr.getComposite());
            }
            g.fillPolygon(xx, yy, xx.length);
            setPaint(oldPaint);
            setComposite(oldComposite);
        }
        if (pr.isContour()) {
            if (pr.getLinePaint() != null) {
                setPaint(pr.getLinePaint());
            }
            if (pr.getLineStroke() != null) {
                setStroke(pr.getLineStroke());
            }
            g.drawPolygon(xx, yy, xx.length);
            setPaint(oldPaint);
            setStroke(oldStroke);
        }
    }


    private NTxGraphicsImageDrawer putCache(Object[] keys, NTxGraphicsImageDrawer value) {
        for (Object key : keys) {
            imageCache.put(key, value);
        }
        return value;
    }

}
