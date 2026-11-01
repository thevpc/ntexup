package net.thevpc.ntexup.engine.renderer.dump;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElementBuilder;
import net.thevpc.nuts.io.NPath;
import net.thevpc.ntexup.api.document.NTxArrow;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxElement2DPrimitiveType;
import net.thevpc.ntexup.api.document.elem2d.NTxImageOptions;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.NTxVector2D;
import net.thevpc.ntexup.api.document.elem2d.NtxElement2D;
import net.thevpc.ntexup.api.document.elem2d.NtxElement2DPrimitive;
import net.thevpc.ntexup.api.document.elem2d.primitives.NtxElement2DCubicCurve;
import net.thevpc.ntexup.api.document.elem2d.primitives.NtxElement2DLine;
import net.thevpc.ntexup.api.document.elem2d.primitives.NtxElement2DPolygon;
import net.thevpc.ntexup.api.document.elem2d.primitives.NtxElement2DPolyline;
import net.thevpc.ntexup.api.document.elem2d.primitives.NtxElement2DQuadCurve;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.text.NTxTextOptions;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;
import java.text.AttributedCharacterIterator;

/**
 * Records every {@link NTxGraphics} high-level primitive (rects, ovals,
 * polygons, arcs, texts, images, 2D elements, fills and draws of arbitrary
 * shapes) while delegating all painting to a real underlying graphics so that
 * metrics, colors and side effects stay identical to a normal render.
 */
public class RecordingNTxGraphics implements NTxGraphics {

    private final NTxGraphics delegate;
    private final DumpRecorder recorder;
    private final double canvasWidth;
    private final double canvasHeight;
    private final DumpSink sink;

    public RecordingNTxGraphics(NTxGraphics delegate, DumpRecorder recorder, double canvasWidth, double canvasHeight, DumpSink sink) {
        this.delegate = delegate;
        this.recorder = recorder;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.sink = sink;
        if (sink != null) {
            sink.bind(this, recorder);
        }
    }

    public DumpRecorder getRecorder() {
        return recorder;
    }

    private Graphics2D raw() {
        return delegate.graphics2D();
    }

    private NObjectBuilder base(String op) {
        NObjectBuilder b = new NObjectBuilder(DumpFormat.op(op));
        DumpFormat.state(b.ob, raw(), canvasWidth, canvasHeight);
        return b;
    }

    private void delegateFill(String op, double x, double y, double w, double h) {
        NObjectBuilder b = base(op);
        b.xywh(x, y, w, h);
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        recorder.record(b.build());
    }

    private void delegateDraw(String op, double x, double y, double w, double h) {
        NObjectBuilder b = base(op);
        b.xywh(x, y, w, h);
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        b.ob.add("stroke", DumpFormat.stroke(getStroke()));
        recorder.record(b.build());
    }

    @Override
    public NTxEngine engine() {
        return delegate.engine();
    }

    @Override
    public NTxGraphics copy() {
        return new RecordingNTxGraphics(delegate.copy(), recorder.branch(), canvasWidth, canvasHeight, sink);
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }

    @Override
    public AffineTransform getTransform() {
        return delegate.getTransform();
    }

    @Override
    public void setTransform(AffineTransform t) {
        delegate.setTransform(t);
    }

    @Override
    public void setPaint(Paint red) {
        delegate.setPaint(red);
    }

    @Override
    public void setColor(Color red) {
        delegate.setColor(red);
    }

    @Override
    public Color getColor() {
        return delegate.getColor();
    }

    @Override
    public void fillOval(double x, double y, double w, double h) {
        delegateFill("fillOval", x, y, w, h);
        delegate.fillOval(x, y, w, h);
    }

    @Override
    public void drawOval(double x, double y, double w, double h) {
        delegateDraw("drawOval", x, y, w, h);
        delegate.drawOval(x, y, w, h);
    }

    @Override
    public void fillRect(double x, double y, double w, double h) {
        delegateFill("fillRect", x, y, w, h);
        delegate.fillRect(x, y, w, h);
    }

    @Override
    public void drawRect(double x, double y, double w, double h) {
        delegateDraw("drawRect", x, y, w, h);
        delegate.drawRect(x, y, w, h);
    }

    @Override
    public void fillRect(NTxBounds2D b) {
        fillRect(b.minX(), b.minY(), b.widthX(), b.widthY());
    }

    @Override
    public void drawRect(NTxBounds2D a) {
        drawRect(a.minX(), a.minY(), a.widthX(), a.widthY());
    }

    @Override
    public Rectangle2D getStringBounds(String str) {
        return delegate.getStringBounds(str);
    }

    @Override
    public Rectangle2D getStringBounds(AttributedCharacterIterator iterator) {
        return delegate.getStringBounds(iterator);
    }

    @Override
    public FontMetrics getFontMetrics() {
        return delegate.getFontMetrics();
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return delegate.getFontMetrics(f);
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return delegate.getFontRenderContext();
    }

    @Override
    public Font getFont() {
        return delegate.getFont();
    }

    @Override
    public Graphics2D graphics2D() {
        return delegate.graphics2D();
    }

    @Override
    public void setFont(Font font) {
        delegate.setFont(font);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, double x, double y) {
        NObjectBuilder b = base("text");
        b.ob.add("at", DumpFormat.pointArray(x, y));
        b.ob.add("text", DumpFormat.attributedText(iterator));
        b.ob.add("font", DumpFormat.font(getFont()));
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        recorder.record(b.build());
        delegate.drawString(iterator, x, y);
    }

    @Override
    public void drawString(String str, double x, double y) {
        NObjectBuilder b = base("text");
        b.ob.add("at", DumpFormat.pointArray(x, y));
        DumpFormat.addIf(b.ob, "s", str);
        b.ob.add("font", DumpFormat.font(getFont()));
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        recorder.record(b.build());
        delegate.drawString(str, x, y);
    }

    @Override
    public void drawString(String str, double x, double y, NTxTextOptions options) {
        NObjectBuilder b = base("text");
        b.ob.add("at", DumpFormat.pointArray(x, y));
        DumpFormat.addIf(b.ob, "s", str);
        b.ob.add("font", DumpFormat.font(getFont()));
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        if (options != null) {
            NElement oe = textOptions(options);
            if (oe != null) {
                b.ob.add("options", oe);
            }
        }
        recorder.record(b.build());
        delegate.drawString(str, x, y, options);
    }

    private NElement textOptions(NTxTextOptions o) {
        NObjectBuilder b = new NObjectBuilder(DumpFormat.op("options"));
        if (o.bold != null) {
            b.ob.add("bold", NElement.of(o.bold));
        }
        if (o.italic != null) {
            b.ob.add("italic", NElement.of(o.italic));
        }
        if (o.underlined != null) {
            b.ob.add("underlined", NElement.of(o.underlined));
        }
        if (o.strikeThrough != null) {
            b.ob.add("strikeThrough", NElement.of(o.strikeThrough));
        }
        if (o.fontFamily != null) {
            b.ob.add("fontFamily", NElement.of(o.fontFamily));
        }
        if (o.foregroundColor != null) {
            b.ob.add("foreground", DumpFormat.paint(o.foregroundColor));
        }
        if (o.backgroundColor != null) {
            b.ob.add("background", DumpFormat.paint(o.backgroundColor));
        }
        if (o.computedFont != null) {
            b.ob.add("computedFont", DumpFormat.font(o.computedFont));
        }
        return b.build();
    }

    @Override
    public void debugString(String str, double x, double y) {
        NObjectBuilder b = base("debugText");
        b.ob.add("at", DumpFormat.pointArray(x, y));
        DumpFormat.addIf(b.ob, "s", str);
        recorder.record(b.build());
        delegate.debugString(str, x, y);
    }

    @Override
    public void drawArrayHead(NTxPoint2D origin, NTxVector2D direction, NTxArrow arrow) {
        NObjectBuilder b = base("drawArrayHead");
        if (origin != null) {
            b.ob.add("origin", point(origin));
        }
        if (direction != null) {
            b.ob.add("direction", DumpFormat.pointArray(direction.x, direction.y));
        }
        if (arrow != null) {
            b.ob.add("arrow", DumpFormat.arrow(arrow));
        }
        recorder.record(b.build());
        delegate.drawArrayHead(origin, direction, arrow);
    }

    @Override
    public void drawArc(double x, double y, double w, double h, double startAngle, double endAngle) {
        NObjectBuilder b = base("drawArc");
        b.xywh(x, y, w, h);
        DumpFormat.add(b.ob, "start", startAngle);
        DumpFormat.add(b.ob, "end", endAngle);
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        b.ob.add("stroke", DumpFormat.stroke(getStroke()));
        recorder.record(b.build());
        delegate.drawArc(x, y, w, h, startAngle, endAngle);
    }

    @Override
    public void drawLine(double x1, double y1, double x2, double y2) {
        NObjectBuilder b = base("drawLine");
        DumpFormat.add(b.ob, "x1", x1);
        DumpFormat.add(b.ob, "y1", y1);
        DumpFormat.add(b.ob, "x2", x2);
        DumpFormat.add(b.ob, "y2", y2);
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        b.ob.add("stroke", DumpFormat.stroke(getStroke()));
        recorder.record(b.build());
        delegate.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void fillPolygon(double[] xx, double[] yy, int length) {
        NObjectBuilder b = base("fillPolygon");
        b.ob.add("points", NElement.of(DumpFormat.pointsString(xx, yy, length)));
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        recorder.record(b.build());
        delegate.fillPolygon(xx, yy, length);
    }

    @Override
    public void drawPolygon(double[] xx, double[] yy, int length) {
        NObjectBuilder b = base("drawPolygon");
        b.ob.add("points", NElement.of(DumpFormat.pointsString(xx, yy, length)));
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        b.ob.add("stroke", DumpFormat.stroke(getStroke()));
        recorder.record(b.build());
        delegate.drawPolygon(xx, yy, length);
    }

    @Override
    public void drawPolyline(double[] xx, double[] yy, int length) {
        NObjectBuilder b = base("drawPolyline");
        b.ob.add("points", NElement.of(DumpFormat.pointsString(xx, yy, length)));
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        b.ob.add("stroke", DumpFormat.stroke(getStroke()));
        recorder.record(b.build());
        delegate.drawPolyline(xx, yy, length);
    }

    @Override
    public void fillRoundRect(double x, double y, double w, double h, double cx, double cy) {
        NObjectBuilder b = base("fillRoundRect");
        b.xywh(x, y, w, h);
        DumpFormat.add(b.ob, "arcwidth", cx);
        DumpFormat.add(b.ob, "archeight", cy);
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        recorder.record(b.build());
        delegate.fillRoundRect(x, y, w, h, cx, cy);
    }

    @Override
    public void drawRoundRect(double x, double y, double w, double h, double cx, double cy) {
        NObjectBuilder b = base("drawRoundRect");
        b.xywh(x, y, w, h);
        DumpFormat.add(b.ob, "arcwidth", cx);
        DumpFormat.add(b.ob, "archeight", cy);
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        b.ob.add("stroke", DumpFormat.stroke(getStroke()));
        recorder.record(b.build());
        delegate.drawRoundRect(x, y, w, h, cx, cy);
    }

    @Override
    public void fill3DRect(double x, double y, double w, double h, boolean raised) {
        NObjectBuilder b = base("fill3DRect");
        b.xywh(x, y, w, h);
        b.ob.add("raised", NElement.of(raised));
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        recorder.record(b.build());
        delegate.fill3DRect(x, y, w, h, raised);
    }

    @Override
    public void draw3DRect(double x, double y, double w, double h, boolean raised) {
        NObjectBuilder b = base("draw3DRect");
        b.xywh(x, y, w, h);
        b.ob.add("raised", NElement.of(raised));
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        recorder.record(b.build());
        delegate.draw3DRect(x, y, w, h, raised);
    }

    @Override
    public void drawImage(Image image, double x, double y, ImageObserver o) {
        NObjectBuilder b = base("image");
        b.ob.add("at", DumpFormat.pointArray(x, y));
        DumpFormat.addIf(b.ob, "size", DumpFormat.imageInfo(image));
        recorder.record(b.build());
        delegate.drawImage(image, x, y, o);
    }

    @Override
    public void drawImage(NPath image, double x, double y, NTxImageOptions options) {
        NObjectBuilder b = base("image");
        b.ob.add("src", NElement.of(String.valueOf(image)));
        b.ob.add("at", DumpFormat.pointArray(x, y));
        if (options != null) {
            if (options.getSize() != null) {
                b.ob.add("size", NElement.of(options.getSize().width + "x" + options.getSize().height));
            }
            if (options.isPreserveAspectRatio()) {
                b.ob.add("preserveAspectRatio", NElement.of(true));
            }
            if (options.isDisableAnimation()) {
                b.ob.add("disableAnimation", NElement.of(true));
            }
            if (options.getTransparentColor() != null) {
                b.ob.add("transparentColor", NElement.of(DumpFormat.color(options.getTransparentColor())));
            }
        }
        recorder.record(b.build());
        delegate.drawImage(image, x, y, options);
    }

    @Override
    public Color getSecondaryColor() {
        return delegate.getSecondaryColor();
    }

    @Override
    public void setSecondaryColor(Color secondaryColor) {
        delegate.setSecondaryColor(secondaryColor);
    }

    @Override
    public void fillSphere(double x, double y, double w, double h, double lightAngle, float radius) {
        NObjectBuilder b = base("fillSphere");
        b.xywh(x, y, w, h);
        DumpFormat.add(b.ob, "lightAngle", lightAngle);
        DumpFormat.add(b.ob, "radius", radius);
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        recorder.record(b.build());
        delegate.fillSphere(x, y, w, h, lightAngle, radius);
    }

    @Override
    public void setComposite(Composite composite) {
        delegate.setComposite(composite);
    }

    @Override
    public void fill(Shape shape) {
        NObjectBuilder b = base("fill");
        b.ob.add("shape", DumpFormat.shape(shape));
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        recorder.record(b.build());
        delegate.fill(shape);
    }

    @Override
    public void draw(Shape shape) {
        NObjectBuilder b = base("draw");
        b.ob.add("shape", DumpFormat.shape(shape));
        b.ob.add("paint", DumpFormat.paint(raw().getPaint()));
        b.ob.add("stroke", DumpFormat.stroke(getStroke()));
        recorder.record(b.build());
        delegate.draw(shape);
    }

    @Override
    public void setStroke(Stroke stroke) {
        delegate.setStroke(stroke);
    }

    @Override
    public Stroke getStroke() {
        return delegate.getStroke();
    }

    @Override
    public void draw2D(NtxElement2D element2D) {
        NObjectBuilder b = base("draw2D");
        element2D(element2D, b);
        recorder.record(b.build());
        delegate.draw2D(element2D);
    }

    private void element2D(NtxElement2D e, NObjectBuilder b) {
        if (e == null) {
            b.ob.add("class", NElement.of("null"));
            return;
        }
        if (e instanceof NtxElement2DPrimitive p) {
            NTxElement2DPrimitiveType t = p.type();
            b.ob.add("kind", NElement.of(t == null ? "?" : t.name()));
            switch (t == null ? NTxElement2DPrimitiveType.LINE : t) {
                case LINE: {
                    if (e instanceof NtxElement2DLine line) {
                        b.ob.add("from", point(line.getFrom()));
                        b.ob.add("to", point(line.getTo()));
                        if (line.getStartArrow() != null) {
                            b.ob.add("startArrow", DumpFormat.arrow(line.getStartArrow()));
                        }
                        if (line.getEndArrow() != null) {
                            b.ob.add("endArrow", DumpFormat.arrow(line.getEndArrow()));
                        }
                    }
                    break;
                }
                case QUAD_CURVE: {
                    if (e instanceof NtxElement2DQuadCurve q) {
                        b.ob.add("from", point(q.getFrom()));
                        b.ob.add("ctrl", point(q.getCtrl()));
                        b.ob.add("to", point(q.getTo()));
                    }
                    break;
                }
                case CUBIC_CURVE: {
                    if (e instanceof NtxElement2DCubicCurve c) {
                        b.ob.add("from", point(c.getFrom()));
                        b.ob.add("ctrl1", point(c.getCtrl1()));
                        b.ob.add("ctrl2", point(c.getCtrl2()));
                        b.ob.add("to", point(c.getTo()));
                    }
                    break;
                }
                case POLYLINE: {
                    if (e instanceof NtxElement2DPolyline pl) {
                        b.ob.add("nodes", points(pl.getNodes()));
                    }
                    break;
                }
                case POLYGON: {
                    if (e instanceof NtxElement2DPolygon pg) {
                        b.ob.add("nodes", points(pg.getNodes()));
                    }
                    break;
                }
                default: {
                    b.ob.add("class", NElement.of(e.getClass().getSimpleName()));
                    break;
                }
            }
            if (p.getBackgroundPaint() != null) {
                b.ob.add("backgroundPaint", DumpFormat.paint(p.getBackgroundPaint()));
            }
            if (p.getLinePaint() != null) {
                b.ob.add("linePaint", DumpFormat.paint(p.getLinePaint()));
            }
            if (p.getLineStroke() != null) {
                b.ob.add("lineStroke", DumpFormat.stroke(p.getLineStroke()));
            }
        } else {
            b.ob.add("class", NElement.of(e.getClass().getName()));
        }
    }

    private NElement point(NTxPoint2D p) {
        if (p == null) {
            return NElement.ofNull();
        }
        return DumpFormat.pointArray(p.x, p.y);
    }

    private NElement points(NTxPoint2D[] ps) {
        if (ps == null) {
            return NElement.ofNull();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ps.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            if (ps[i] != null) {
                sb.append(DumpFormat.fmt(ps[i].x)).append(',').append(DumpFormat.fmt(ps[i].y));
            }
        }
        return NElement.of(sb.toString());
    }

    @Override
    public void shear(double shx, double shy) {
        delegate.shear(shx, shy);
    }

    @Override
    public void transform(AffineTransform tx) {
        delegate.transform(tx);
    }

    @Override
    public void scale(double sx, double sy) {
        delegate.scale(sx, sy);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        delegate.rotate(theta, x, y);
    }

    @Override
    public void rotate(double theta) {
        delegate.rotate(theta);
    }

    @Override
    public Shape createShape(NElement e) {
        return delegate.createShape(e);
    }

    @Override
    public Stroke createStroke(NElement strokeElem) {
        return delegate.createStroke(strokeElem);
    }

    /**
     * Tiny wrapper to keep op-building code readable.
     */
    private static class NObjectBuilder {
        final NObjectElementBuilder ob;

        NObjectBuilder(NObjectElementBuilder ob) {
            this.ob = ob;
        }

        void xywh(double x, double y, double w, double h) {
            DumpFormat.add(ob, "x", x);
            DumpFormat.add(ob, "y", y);
            DumpFormat.add(ob, "width", w);
            DumpFormat.add(ob, "height", h);
        }

        NElement build() {
            return ob.build();
        }
    }
}
