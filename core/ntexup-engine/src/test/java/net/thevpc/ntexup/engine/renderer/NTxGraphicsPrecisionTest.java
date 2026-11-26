package net.thevpc.ntexup.engine.renderer;

import net.thevpc.ntexup.api.document.NTxArrow;
import net.thevpc.ntexup.api.document.NTxArrowType;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.NTxVector2D;
import net.thevpc.ntexup.api.document.elem2d.primitives.NtxElement2DCubicCurve;
import net.thevpc.ntexup.api.document.elem2d.primitives.NtxElement2DLine;
import net.thevpc.ntexup.api.document.elem2d.primitives.NtxElement2DPolyline;
import net.thevpc.ntexup.api.document.elem2d.primitives.NtxElement2DQuadCurve;
import net.thevpc.ntexup.lib.geometry2d.NTxElement2DFactory;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NTxGraphicsPrecisionTest {
    @Test
    void arrowheadPreservesFractionalCoordinates() {
        RecordingGraphics2D graphics = new RecordingGraphics2D();
        NTxGraphicsImpl renderer = new NTxGraphicsImpl(graphics, null);
        renderer.drawArrayHead(new NTxPoint2D(20.25, 30.75), new NTxVector2D(1, 0),
                new NTxArrow(NTxArrowType.TRIANGLE, 8.4, 5.6));

        assertEquals(1, graphics.shapes.size());
        assertTrue(graphics.shapes.get(0) instanceof Path2D);
        assertEquals(8.4, graphics.shapes.get(0).getBounds2D().getWidth(), 1e-9);
        assertEquals(new AffineTransform(), graphics.transform);
    }

    @Test
    void polylineKeepsFractionalVertices() {
        RecordingGraphics2D graphics = new RecordingGraphics2D();
        NTxGraphicsImpl renderer = new NTxGraphicsImpl(graphics, null);
        NtxElement2DPolyline line = (NtxElement2DPolyline) NTxElement2DFactory.polyline(
                new NTxPoint2D(1.25, 2.75), new NTxPoint2D(10.5, 20.125), new NTxPoint2D(30.75, 25.5));
        renderer.draw2D(line);

        assertEquals(1, graphics.shapes.size());
        assertEquals(1.25, graphics.shapes.get(0).getBounds2D().getMinX(), 1e-9);
        assertEquals(2.75, graphics.shapes.get(0).getBounds2D().getMinY(), 1e-9);
    }

    @Test
    void lineShaftsStopAtArrowheadBases() {
        assertEndSetback(NTxArrowType.SIMPLE, 100);
        assertEndSetback(NTxArrowType.TRIANGLE, 80);
        assertEndSetback(NTxArrowType.TRIANGLE_FULL, 80);
        assertEndSetback(NTxArrowType.DIAMOND, 80);
        assertEndSetback(NTxArrowType.OVAL, 80);
        assertEndSetback(NTxArrowType.OVAL_FULL, 80);
        assertEndSetback(NTxArrowType.RECTANGLE, 80);
        assertEndSetback(NTxArrowType.RECTANGLE_FULL, 80);
    }

    @Test
    void polylineShaftStopsAtFinalArrowheadBase() {
        RecordingGraphics2D graphics = new RecordingGraphics2D();
        NTxGraphicsImpl renderer = new NTxGraphicsImpl(graphics, null);
        NtxElement2DPolyline line = (NtxElement2DPolyline) NTxElement2DFactory.polyline(
                new NTxPoint2D(0, 0), new NTxPoint2D(50, 0), new NTxPoint2D(100, 0));
        line.setEndArrow(new NTxArrow(NTxArrowType.OVAL, 20, 10));
        renderer.draw2D(line);

        assertEquals(80, graphics.shapes.get(0).getBounds2D().getMaxX(), 1e-9);
        assertEquals(0, graphics.shapes.get(1).getBounds2D().getMaxX(), 1e-9);
    }

    @Test
    void quadraticShaftStopsAtOvalArrowheadBase() {
        RecordingGraphics2D graphics = new RecordingGraphics2D();
        NTxGraphicsImpl renderer = new NTxGraphicsImpl(graphics, null);
        NtxElement2DQuadCurve curve = NTxElement2DFactory.quad(
                new NTxPoint2D(0, 0), new NTxPoint2D(50, 0), new NTxPoint2D(100, 0));
        curve.setEndArrow(new NTxArrow(NTxArrowType.OVAL, 20, 10));
        renderer.draw2D(curve);

        assertEquals(80, ((QuadCurve2D) graphics.shapes.get(0)).getX2(), 0.01);
    }

    @Test
    void cubicShaftStopsAtTriangleArrowheadBase() {
        RecordingGraphics2D graphics = new RecordingGraphics2D();
        NTxGraphicsImpl renderer = new NTxGraphicsImpl(graphics, null);
        NtxElement2DCubicCurve curve = NTxElement2DFactory.cubic(
                new NTxPoint2D(0, 0), new NTxPoint2D(30, 0),
                new NTxPoint2D(70, 0), new NTxPoint2D(100, 0));
        curve.setEndArrow(new NTxArrow(NTxArrowType.TRIANGLE, 20, 10));
        renderer.draw2D(curve);

        assertEquals(80, ((CubicCurve2D) graphics.shapes.get(0)).getX2(), 0.01);
    }

    @Test
    void opposingCurveArrowsLeaveOnlyTheMiddleShaft() {
        RecordingGraphics2D graphics = new RecordingGraphics2D();
        NTxGraphicsImpl renderer = new NTxGraphicsImpl(graphics, null);
        NtxElement2DCubicCurve curve = NTxElement2DFactory.cubic(
                new NTxPoint2D(0, 0), new NTxPoint2D(30, 0),
                new NTxPoint2D(70, 0), new NTxPoint2D(100, 0));
        curve.setStartArrow(new NTxArrow(NTxArrowType.TRIANGLE, 20, 10))
                .setEndArrow(new NTxArrow(NTxArrowType.TRIANGLE, 20, 10));
        renderer.draw2D(curve);

        CubicCurve2D shaft = (CubicCurve2D) graphics.shapes.get(0);
        assertEquals(20, shaft.getX1(), 0.01);
        assertEquals(80, shaft.getX2(), 0.01);
    }

    @Test
    void quadraticArrowheadsFollowEndpointTangents() {
        RecordingGraphics2D graphics = new RecordingGraphics2D();
        NTxGraphicsImpl renderer = new NTxGraphicsImpl(graphics, null);
        NTxArrow arrow = new NTxArrow(NTxArrowType.TRIANGLE, 4, 4);
        NtxElement2DQuadCurve curve = NTxElement2DFactory.quad(
                new NTxPoint2D(10, 10), new NTxPoint2D(30, 10), new NTxPoint2D(30, 30));
        curve.setStartArrow(arrow).setEndArrow(arrow);
        renderer.draw2D(curve);

        assertArrowTransform(graphics, 1, 10, 10, Math.PI);
        assertArrowTransform(graphics, 2, 30, 30, Math.PI / 2);
    }

    @Test
    void cubicArrowheadsFollowEndpointTangents() {
        RecordingGraphics2D graphics = new RecordingGraphics2D();
        NTxGraphicsImpl renderer = new NTxGraphicsImpl(graphics, null);
        NTxArrow arrow = new NTxArrow(NTxArrowType.TRIANGLE, 4, 4);
        NtxElement2DCubicCurve curve = NTxElement2DFactory.cubic(
                new NTxPoint2D(10, 10), new NTxPoint2D(30, 10),
                new NTxPoint2D(30, 30), new NTxPoint2D(50, 30));
        curve.setStartArrow(arrow).setEndArrow(arrow);
        renderer.draw2D(curve);

        assertArrowTransform(graphics, 1, 10, 10, Math.PI);
        assertArrowTransform(graphics, 2, 50, 30, 0);
    }

    private static void assertEndSetback(NTxArrowType type, double expectedShaftEnd) {
        RecordingGraphics2D graphics = new RecordingGraphics2D();
        NTxGraphicsImpl renderer = new NTxGraphicsImpl(graphics, null);
        NtxElement2DLine line = NTxElement2DFactory.line(new NTxPoint2D(0, 0), new NTxPoint2D(100, 0));
        line.setEndArrow(new NTxArrow(type, 20, 10));
        renderer.draw2D(line);

        assertEquals(2, graphics.shapes.size(), type.toString());
        Line2D shaft = (Line2D) graphics.shapes.get(0);
        assertEquals(expectedShaftEnd, shaft.getX2(), 1e-9, type.toString());
        assertEquals(0, graphics.shapes.get(1).getBounds2D().getMaxX(), 1e-9, type.toString());
    }

    private static void assertArrowTransform(RecordingGraphics2D graphics, int index,
                                             double x, double y, double angle) {
        AffineTransform transform = graphics.shapeTransforms.get(index);
        assertEquals(x, transform.getTranslateX(), 1e-9);
        assertEquals(y, transform.getTranslateY(), 1e-9);
        assertEquals(angle, Math.atan2(transform.getShearY(), transform.getScaleX()), 1e-9);
    }

    private static class RecordingGraphics2D extends Graphics2D {
        private final List<java.awt.Shape> shapes = new ArrayList<>();
        private final List<AffineTransform> shapeTransforms = new ArrayList<>();
        private AffineTransform transform = new AffineTransform();

        @Override
        public void draw(java.awt.Shape shape) {
            shapes.add(shape);
            shapeTransforms.add(new AffineTransform(transform));
        }

        @Override
        public AffineTransform getTransform() {
            return new AffineTransform(transform);
        }

        @Override
        public void setTransform(AffineTransform tx) {
            transform = new AffineTransform(tx);
        }

        @Override
        public void translate(int x, int y) {
            transform.concatenate(AffineTransform.getTranslateInstance(x, y));
        }

        @Override
        public void translate(double x, double y) {
            transform.concatenate(AffineTransform.getTranslateInstance(x, y));
        }

        @Override
        public void rotate(double theta) {
            transform.concatenate(AffineTransform.getRotateInstance(theta));
        }

        @Override
        public void rotate(double theta, double x, double y) {
            transform.concatenate(AffineTransform.getRotateInstance(theta, x, y));
        }

        @Override public void fill(java.awt.Shape shape) {
            shapes.add(shape);
            shapeTransforms.add(new AffineTransform(transform));
        }
        @Override public void drawString(String str, int x, int y) { }
        @Override public void drawString(java.text.AttributedCharacterIterator iterator, int x, int y) { }
        @Override public void drawString(String str, float x, float y) { }
        @Override public void drawString(java.text.AttributedCharacterIterator iterator, float x, float y) { }
        @Override public void drawGlyphVector(java.awt.font.GlyphVector glyphs, float x, float y) { }
        @Override public boolean drawImage(java.awt.Image img, AffineTransform xform, java.awt.image.ImageObserver observer) { return true; }
        @Override public boolean drawImage(java.awt.Image img, int x, int y, java.awt.image.ImageObserver observer) { return true; }
        @Override public boolean drawImage(java.awt.Image img, int x, int y, int width, int height, java.awt.image.ImageObserver observer) { return true; }
        @Override public boolean drawImage(java.awt.Image img, int x, int y, Color bgcolor, java.awt.image.ImageObserver observer) { return true; }
        @Override public boolean drawImage(java.awt.Image img, int x, int y, int width, int height, Color bgcolor, java.awt.image.ImageObserver observer) { return true; }
        @Override public boolean drawImage(java.awt.Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, java.awt.image.ImageObserver observer) { return true; }
        @Override public boolean drawImage(java.awt.Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, java.awt.image.ImageObserver observer) { return true; }
        @Override public void copyArea(int x, int y, int width, int height, int dx, int dy) { }
        @Override public Graphics2D create() { return this; }
        @Override public java.awt.Rectangle getClipBounds() { return null; }
        @Override public void clipRect(int x, int y, int width, int height) { }
        @Override public void setClip(int x, int y, int width, int height) { }
        @Override public java.awt.Shape getClip() { return null; }
        @Override public void setClip(java.awt.Shape clip) { }
        @Override public void drawLine(int x1, int y1, int x2, int y2) { }
        @Override public void fillRect(int x, int y, int width, int height) { }
        @Override public void clearRect(int x, int y, int width, int height) { }
        @Override public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) { }
        @Override public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) { }
        @Override public void drawOval(int x, int y, int width, int height) { }
        @Override public void fillOval(int x, int y, int width, int height) { }
        @Override public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) { }
        @Override public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) { }
        @Override public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) { }
        @Override public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) { }
        @Override public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) { }
        @Override public java.awt.FontMetrics getFontMetrics(java.awt.Font font) { return null; }
        @Override public void setXORMode(Color c1) { }
        @Override public void setPaintMode() { }
        @Override public java.awt.Color getColor() { return Color.BLACK; }
        @Override public void setColor(Color c) { }
        @Override public void setPaint(java.awt.Paint paint) { }
        @Override public java.awt.Paint getPaint() { return Color.BLACK; }
        @Override public void setStroke(java.awt.Stroke s) { }
        @Override public java.awt.Stroke getStroke() { return null; }
        @Override public void scale(double sx, double sy) { transform.concatenate(AffineTransform.getScaleInstance(sx, sy)); }
        @Override public void shear(double shx, double shy) { transform.concatenate(AffineTransform.getShearInstance(shx, shy)); }
        @Override public void transform(AffineTransform tx) { transform.concatenate(tx); }
        @Override public java.awt.font.FontRenderContext getFontRenderContext() { return null; }
        @Override public java.awt.Font getFont() { return null; }
        @Override public void setFont(java.awt.Font font) { }
        @Override public java.awt.FontMetrics getFontMetrics() { return null; }
        @Override public java.awt.RenderingHints getRenderingHints() { return null; }
        @Override public void setRenderingHints(java.util.Map<?, ?> hints) { }
        @Override public void addRenderingHints(java.util.Map<?, ?> hints) { }
        @Override public void clip(java.awt.Shape s) { }
        @Override public void drawRenderedImage(java.awt.image.RenderedImage img, AffineTransform xform) { }
        @Override public void drawRenderableImage(java.awt.image.renderable.RenderableImage img, AffineTransform xform) { }
        @Override public void drawImage(BufferedImage image, java.awt.image.BufferedImageOp op, int x, int y) { }
        @Override public boolean hit(java.awt.Rectangle rect, java.awt.Shape shape, boolean onStroke) { return false; }
        @Override public java.awt.GraphicsConfiguration getDeviceConfiguration() { return null; }
        @Override public void dispose() { }
        @Override public void setRenderingHint(java.awt.RenderingHints.Key key, Object value) { }
        @Override public Object getRenderingHint(java.awt.RenderingHints.Key key) { return null; }
        @Override public void setComposite(java.awt.Composite comp) { }
        @Override public java.awt.Composite getComposite() { return null; }
        @Override public void setBackground(Color color) { }
        @Override public Color getBackground() { return Color.BLACK; }
    }
}
