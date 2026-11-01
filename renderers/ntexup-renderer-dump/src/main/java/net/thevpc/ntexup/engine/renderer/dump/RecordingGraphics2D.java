package net.thevpc.ntexup.engine.renderer.dump;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElementBuilder;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;
import java.nio.charset.StandardCharsets;

/**
 * A recording {@link Graphics2D} delegate: forwards every java2d call to a
 * backing graphics (a real image surface in practice, so metrics stay exact)
 * while capturing the exact stream of primitives that the paint pipeline
 * produces for a page.
 * <p>
 * This is the ground-truth capture level: it sees what the PDF bridge
 * ({@code PdfBoxGraphics2D}) and the raster renderer see, including paths
 * produced by stroke expansion, text shaping and component sub-graphics
 * (each {@code create()} opens a nested group closed by {@code dispose()}).
 */
public class RecordingGraphics2D extends Graphics2D {

    private final Graphics2D d;
    private final DumpRecorder recorder;
    private final double canvasWidth;
    private final double canvasHeight;
    private final boolean ownsGroup;
    private final DumpSink sink;

    public RecordingGraphics2D(Graphics2D d, DumpRecorder recorder, double canvasWidth, double canvasHeight, boolean ownsGroup, DumpSink sink) {
        this.d = d;
        this.recorder = recorder;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.ownsGroup = ownsGroup;
        this.sink = sink;
        if (sink != null) {
            sink.bind(this, recorder);
        }
    }

    public DumpRecorder getRecorder() {
        return recorder;
    }

    private NObjectElementBuilder op(String name) {
        NObjectElementBuilder ob = DumpFormat.op(name);
        DumpFormat.state(ob, d, canvasWidth, canvasHeight);
        return ob;
    }

    private void recordShape(String name, Shape s, boolean stroked) {
        NObjectElementBuilder ob = op(name);
        ob.add("shape", DumpFormat.shape(s));
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        if (stroked) {
            ob.add("stroke", DumpFormat.stroke(d.getStroke()));
        }
        recorder.record(ob.build());
    }

    private void recordText(String name, String s, double x, double y) {
        NObjectElementBuilder ob = op(name);
        ob.add("at", DumpFormat.pointArray(x, y));
        ob.add("s", NElement.of(s));
        ob.add("font", DumpFormat.font(d.getFont()));
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        recorder.record(ob.build());
    }

    // ---------------- state: forward, not recorded ----------------

    @Override
    public void setPaint(Paint paint) {
        d.setPaint(paint);
    }

    @Override
    public void setColor(Color c) {
        d.setColor(c);
    }

    @Override
    public Color getColor() {
        return d.getColor();
    }

    @Override
    public Paint getPaint() {
        return d.getPaint();
    }

    @Override
    public void setBackground(Color tag) {
        d.setBackground(tag);
    }

    @Override
    public Color getBackground() {
        return d.getBackground();
    }

    @Override
    public void setStroke(Stroke s) {
        d.setStroke(s);
    }

    @Override
    public Stroke getStroke() {
        return d.getStroke();
    }

    @Override
    public void setFont(Font font) {
        d.setFont(font);
    }

    @Override
    public Font getFont() {
        return d.getFont();
    }

    @Override
    public FontMetrics getFontMetrics() {
        return d.getFontMetrics();
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return d.getFontMetrics(f);
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return d.getFontRenderContext();
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return d.getDeviceConfiguration();
    }

    @Override
    public void setComposite(Composite comp) {
        d.setComposite(comp);
    }

    @Override
    public Composite getComposite() {
        return d.getComposite();
    }

    @Override
    public void setTransform(AffineTransform Tx) {
        d.setTransform(Tx);
    }

    @Override
    public AffineTransform getTransform() {
        return d.getTransform();
    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        d.setRenderingHint(hintKey, hintValue);
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return d.getRenderingHint(hintKey);
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints) {
        d.setRenderingHints(hints);
    }

    @Override
    public void addRenderingHints(Map<?, ?> hints) {
        d.addRenderingHints(hints);
    }

    @Override
    public RenderingHints getRenderingHints() {
        return d.getRenderingHints();
    }

    @Override
    public void setPaintMode() {
        d.setPaintMode();
    }

    @Override
    public void setXORMode(Color c1) {
        d.setXORMode(c1);
    }

    // ---------------- transform ops: forward, recorded as state changes ----------------

    @Override
    public void translate(int x, int y) {
        d.translate(x, y);
    }

    @Override
    public void translate(double tx, double ty) {
        d.translate(tx, ty);
    }

    @Override
    public void rotate(double theta) {
        d.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        d.rotate(theta, x, y);
    }

    @Override
    public void scale(double sx, double sy) {
        d.scale(sx, sy);
    }

    @Override
    public void shear(double shx, double shy) {
        d.shear(shx, shy);
    }

    @Override
    public void transform(AffineTransform Tx) {
        d.transform(Tx);
    }

    // ---------------- clip ops: forwarded; resolved clip embedded in every painted op ----------------

    @Override
    public void clip(Shape s) {
        d.clip(s);
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        d.clipRect(x, y, width, height);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        d.setClip(x, y, width, height);
    }

    @Override
    public void setClip(Shape clip) {
        d.setClip(clip);
    }

    @Override
    public Shape getClip() {
        return d.getClip();
    }

    @Override
    public Rectangle getClipBounds() {
        return d.getClipBounds();
    }

    @Override
    public Rectangle getClipBounds(Rectangle r) {
        return d.getClipBounds(r);
    }

    // ---------------- painting ops: forwarded and recorded ----------------

    @Override
    public void draw(Shape s) {
        recordShape("draw", s, true);
        d.draw(s);
    }

    @Override
    public void fill(Shape s) {
        recordShape("fill", s, false);
        d.fill(s);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        NObjectElementBuilder ob = op("drawLine");
        DumpFormat.add(ob, "x1", x1);
        DumpFormat.add(ob, "y1", y1);
        DumpFormat.add(ob, "x2", x2);
        DumpFormat.add(ob, "y2", y2);
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        ob.add("stroke", DumpFormat.stroke(d.getStroke()));
        recorder.record(ob.build());
        d.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        NObjectElementBuilder ob = op("drawRect");
        DumpFormat.add(ob, "x", x);
        DumpFormat.add(ob, "y", y);
        DumpFormat.add(ob, "width", width);
        DumpFormat.add(ob, "height", height);
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        ob.add("stroke", DumpFormat.stroke(d.getStroke()));
        recorder.record(ob.build());
        d.drawRect(x, y, width, height);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        NObjectElementBuilder ob = op("fillRect");
        DumpFormat.add(ob, "x", x);
        DumpFormat.add(ob, "y", y);
        DumpFormat.add(ob, "width", width);
        DumpFormat.add(ob, "height", height);
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        recorder.record(ob.build());
        d.fillRect(x, y, width, height);
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        NObjectElementBuilder ob = op("clearRect");
        DumpFormat.add(ob, "x", x);
        DumpFormat.add(ob, "y", y);
        DumpFormat.add(ob, "width", width);
        DumpFormat.add(ob, "height", height);
        recorder.record(ob.build());
        d.clearRect(x, y, width, height);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        NObjectElementBuilder ob = op("drawRoundRect");
        DumpFormat.add(ob, "x", x);
        DumpFormat.add(ob, "y", y);
        DumpFormat.add(ob, "width", width);
        DumpFormat.add(ob, "height", height);
        DumpFormat.add(ob, "arcwidth", arcWidth);
        DumpFormat.add(ob, "archeight", arcHeight);
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        ob.add("stroke", DumpFormat.stroke(d.getStroke()));
        recorder.record(ob.build());
        d.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        NObjectElementBuilder ob = op("fillRoundRect");
        DumpFormat.add(ob, "x", x);
        DumpFormat.add(ob, "y", y);
        DumpFormat.add(ob, "width", width);
        DumpFormat.add(ob, "height", height);
        DumpFormat.add(ob, "arcwidth", arcWidth);
        DumpFormat.add(ob, "archeight", arcHeight);
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        recorder.record(ob.build());
        d.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        NObjectElementBuilder ob = op("drawOval");
        DumpFormat.add(ob, "x", x);
        DumpFormat.add(ob, "y", y);
        DumpFormat.add(ob, "width", width);
        DumpFormat.add(ob, "height", height);
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        ob.add("stroke", DumpFormat.stroke(d.getStroke()));
        recorder.record(ob.build());
        d.drawOval(x, y, width, height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        NObjectElementBuilder ob = op("fillOval");
        DumpFormat.add(ob, "x", x);
        DumpFormat.add(ob, "y", y);
        DumpFormat.add(ob, "width", width);
        DumpFormat.add(ob, "height", height);
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        recorder.record(ob.build());
        d.fillOval(x, y, width, height);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        NObjectElementBuilder ob = op("drawArc");
        DumpFormat.add(ob, "x", x);
        DumpFormat.add(ob, "y", y);
        DumpFormat.add(ob, "width", width);
        DumpFormat.add(ob, "height", height);
        DumpFormat.add(ob, "start", startAngle);
        DumpFormat.add(ob, "extent", arcAngle);
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        ob.add("stroke", DumpFormat.stroke(d.getStroke()));
        recorder.record(ob.build());
        d.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        NObjectElementBuilder ob = op("fillArc");
        DumpFormat.add(ob, "x", x);
        DumpFormat.add(ob, "y", y);
        DumpFormat.add(ob, "width", width);
        DumpFormat.add(ob, "height", height);
        DumpFormat.add(ob, "start", startAngle);
        DumpFormat.add(ob, "extent", arcAngle);
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        recorder.record(ob.build());
        d.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        NObjectElementBuilder ob = op("drawPolyline");
        ob.add("points", NElement.of(DumpFormat.pointsString(xPoints, yPoints, nPoints)));
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        ob.add("stroke", DumpFormat.stroke(d.getStroke()));
        recorder.record(ob.build());
        d.drawPolyline(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        NObjectElementBuilder ob = op("drawPolygon");
        ob.add("points", NElement.of(DumpFormat.pointsString(xPoints, yPoints, nPoints)));
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        ob.add("stroke", DumpFormat.stroke(d.getStroke()));
        recorder.record(ob.build());
        d.drawPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawPolygon(Polygon p) {
        drawPolygon(p.xpoints, p.ypoints, p.npoints);
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        NObjectElementBuilder ob = op("fillPolygon");
        ob.add("points", NElement.of(DumpFormat.pointsString(xPoints, yPoints, nPoints)));
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        recorder.record(ob.build());
        d.fillPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void fillPolygon(Polygon p) {
        fillPolygon(p.xpoints, p.ypoints, p.npoints);
    }

    @Override
    public void draw3DRect(int x, int y, int width, int height, boolean raised) {
        NObjectElementBuilder ob = op("draw3DRect");
        DumpFormat.add(ob, "x", x);
        DumpFormat.add(ob, "y", y);
        DumpFormat.add(ob, "width", width);
        DumpFormat.add(ob, "height", height);
        ob.add("raised", NElement.of(raised));
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        recorder.record(ob.build());
        d.draw3DRect(x, y, width, height, raised);
    }

    @Override
    public void fill3DRect(int x, int y, int width, int height, boolean raised) {
        NObjectElementBuilder ob = op("fill3DRect");
        DumpFormat.add(ob, "x", x);
        DumpFormat.add(ob, "y", y);
        DumpFormat.add(ob, "width", width);
        DumpFormat.add(ob, "height", height);
        ob.add("raised", NElement.of(raised));
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        recorder.record(ob.build());
        d.fill3DRect(x, y, width, height, raised);
    }

    @Override
    public void drawString(String str, int x, int y) {
        recordText("text", str, x, y);
        d.drawString(str, x, y);
    }

    @Override
    public void drawString(String str, float x, float y) {
        recordText("text", str, x, y);
        d.drawString(str, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        recordAttributed(iterator, x, y);
        d.drawString(iterator, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        recordAttributed(iterator, x, y);
        d.drawString(iterator, x, y);
    }

    private void recordAttributed(AttributedCharacterIterator iterator, double x, double y) {
        NObjectElementBuilder ob = op("text");
        ob.add("at", DumpFormat.pointArray(x, y));
        ob.add("text", DumpFormat.attributedText(iterator));
        ob.add("font", DumpFormat.font(d.getFont()));
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        recorder.record(ob.build());
    }

    @Override
    public void drawChars(char[] data, int offset, int length, int x, int y) {
        String s = new String(data, offset, length);
        recordText("chars", s, x, y);
        d.drawChars(data, offset, length, x, y);
    }

    @Override
    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        String s = new String(data, offset, length, StandardCharsets.ISO_8859_1);
        recordText("bytes", s, x, y);
        d.drawBytes(data, offset, length, x, y);
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        NObjectElementBuilder ob = op("glyphs");
        ob.add("at", DumpFormat.pointArray(x, y));
        ob.add("count", NElement.of(g.getNumGlyphs()));
        ob.add("bounds", DumpFormat.shape(g.getLogicalBounds()));
        ob.add("paint", DumpFormat.paint(d.getPaint()));
        recorder.record(ob.build());
        d.drawGlyphVector(g, x, y);
    }

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return d.hit(rect, s, onStroke);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        NObjectElementBuilder ob = op("copyArea");
        DumpFormat.add(ob, "x", x);
        DumpFormat.add(ob, "y", y);
        DumpFormat.add(ob, "width", width);
        DumpFormat.add(ob, "height", height);
        DumpFormat.add(ob, "dx", dx);
        DumpFormat.add(ob, "dy", dy);
        recorder.record(ob.build());
        d.copyArea(x, y, width, height, dx, dy);
    }

    // ---------------- drawImage family ----------------

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        NObjectElementBuilder ob = op("image");
        ob.add("at", DumpFormat.pointArray(x, y));
        DumpFormat.addIf(ob, "size", DumpFormat.imageInfo(img));
        recorder.record(ob.build());
        return d.drawImage(img, x, y, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        NObjectElementBuilder ob = op("image");
        ob.add("at", DumpFormat.pointArray(x, y));
        ob.add("dest", DumpFormat.rectArray(x, y, width, height));
        DumpFormat.addIf(ob, "size", DumpFormat.imageInfo(img));
        recorder.record(ob.build());
        return d.drawImage(img, x, y, width, height, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        NObjectElementBuilder ob = op("image");
        ob.add("at", DumpFormat.pointArray(x, y));
        DumpFormat.addIf(ob, "bgcolor", DumpFormat.color(bgcolor));
        DumpFormat.addIf(ob, "size", DumpFormat.imageInfo(img));
        recorder.record(ob.build());
        return d.drawImage(img, x, y, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        NObjectElementBuilder ob = op("image");
        ob.add("at", DumpFormat.pointArray(x, y));
        ob.add("dest", DumpFormat.rectArray(x, y, width, height));
        DumpFormat.addIf(ob, "bgcolor", DumpFormat.color(bgcolor));
        DumpFormat.addIf(ob, "size", DumpFormat.imageInfo(img));
        recorder.record(ob.build());
        return d.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        NObjectElementBuilder ob = op("imagePart");
        ob.add("dest", DumpFormat.pointArray(dx1, dy1));
        ob.add("dest2", DumpFormat.pointArray(dx2, dy2));
        ob.add("src", DumpFormat.pointArray(sx1, sy1));
        ob.add("src2", DumpFormat.pointArray(sx2, sy2));
        DumpFormat.addIf(ob, "size", DumpFormat.imageInfo(img));
        recorder.record(ob.build());
        return d.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    @Override
    public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
        NObjectElementBuilder ob = op("imagePart");
        ob.add("dest", DumpFormat.pointArray(dx1, dy1));
        ob.add("dest2", DumpFormat.pointArray(dx2, dy2));
        ob.add("src", DumpFormat.pointArray(sx1, sy1));
        ob.add("src2", DumpFormat.pointArray(sx2, sy2));
        DumpFormat.addIf(ob, "bgcolor", DumpFormat.color(bgcolor));
        DumpFormat.addIf(ob, "size", DumpFormat.imageInfo(img));
        recorder.record(ob.build());
        return d.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        NObjectElementBuilder ob = op("image");
        ob.add("at", DumpFormat.matrix(xform));
        DumpFormat.addIf(ob, "size", DumpFormat.imageInfo(img));
        recorder.record(ob.build());
        return d.drawImage(img, xform, obs);
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        NObjectElementBuilder ob = op("imageOp");
        ob.add("at", DumpFormat.pointArray(x, y));
        ob.add("op", NElement.of(op == null ? "null" : op.getClass().getName()));
        recorder.record(ob.build());
        d.drawImage(img, op, x, y);
    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        NObjectElementBuilder ob = op("renderedImage");
        ob.add("at", DumpFormat.matrix(xform));
        ob.add("size", NElement.of(img.getWidth() + "x" + img.getHeight()));
        recorder.record(ob.build());
        d.drawRenderedImage(img, xform);
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        NObjectElementBuilder ob = op("renderableImage");
        ob.add("at", DumpFormat.matrix(xform));
        ob.add("width", NElement.of(DumpFormat.num(img.getWidth())));
        ob.add("height", NElement.of(DumpFormat.num(img.getHeight())));
        recorder.record(ob.build());
        d.drawRenderableImage(img, xform);
    }

    // ---------------- scope management ----------------

    @Override
    public Graphics create() {
        DumpRecorder child = recorder.branch();
        return new RecordingGraphics2D((Graphics2D) d.create(), child, canvasWidth, canvasHeight, true, sink);
    }

    @Override
    public Graphics create(int x, int y, int width, int height) {
        Graphics g = create();
        g.translate(x, y);
        g.setClip(0, 0, width, height);
        return g;
    }

    @Override
    public void dispose() {
        d.dispose();
    }
}
