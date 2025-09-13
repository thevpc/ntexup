package net.thevpc.ntexup.engine.util;


import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.NTxShadow;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.text.NTxTextOptions;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NTx2DUtils {
    private static float[] createGaussianKernel(float radius) {
        int r = (int) Math.ceil(radius * 3); // cover ~99% of Gaussian
        int size = r * 2 + 1;
        float[] data = new float[size];
        float sigma = radius;
        float sigma22 = 2 * sigma * sigma;
        float sigmaRoot = (float) Math.sqrt(2 * Math.PI) * sigma;
        float sum = 0;
        for (int i = -r; i <= r; i++) {
            float val = (float) Math.exp(-(i * i) / sigma22) / sigmaRoot;
            data[i + r] = val;
            sum += val;
        }
        // normalize so total = 1
        for (int i = 0; i < data.length; i++) {
            data[i] /= sum;
        }
        return data;
    }

    public static BufferedImage tint(BufferedImage src, Color tintColor) {
        BufferedImage tinted = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float[] scales = {
                tintColor.getRed() / 255f,   // scale red
                tintColor.getGreen() / 255f, // scale green
                tintColor.getBlue() / 255f,  // scale blue
                tintColor.getAlpha() / 255f  // scale alpha
        };
        float[] offsets = {0f, 0f, 0f, 0f};

        RescaleOp op = new RescaleOp(scales, offsets, null);
        op.filter(src, tinted);
        return tinted;
    }

    public static java.util.List<Point2D> interpolatePoints(Point2D[] points, double spacing) {
        List<Point2D> result = new ArrayList<>();
        for (int i = 0; i < points.length - 1; i++) {
            Point2D p0 = points[i];
            Point2D p1 = points[i + 1];
            double dist = p0.distance(p1);
            int steps = Math.max(1, (int) (dist / spacing));
            for (int j = 0; j < steps; j++) {
                double t = (double) j / steps;
                double x = p0.getX() * (1 - t) + p1.getX() * t;
                double y = p0.getY() * (1 - t) + p1.getY() * t;
                result.add(new Point2D.Double(x, y));
            }
        }
        result.add(points[points.length - 1]);
        return result;
    }

    public static List<Double> computeSegmentLengths(List<Point2D> pts) {
        List<Double> lengths = new ArrayList<>();
        for (int i = 1; i < pts.size(); i++) {
            lengths.add(pts.get(i).distance(pts.get(i - 1)));
        }
        return lengths;
    }

    public static Point2D getPointAtLength(List<Point2D> pts, List<Double> segLengths, double targetLength) {
        double s = 0;
        for (int i = 0; i < segLengths.size(); i++) {
            if (s + segLengths.get(i) >= targetLength) {
                double t = (targetLength - s) / segLengths.get(i);
                Point2D p0 = pts.get(i);
                Point2D p1 = pts.get(i + 1);
                double x = p0.getX() * (1 - t) + p1.getX() * t;
                double y = p0.getY() * (1 - t) + p1.getY() * t;
                return new Point2D.Double(x, y);
            }
            s += segLengths.get(i);
        }
        return pts.get(pts.size() - 1);
    }

    public static double getTangentAngle(List<Point2D> pts, List<Double> segLengths, double targetLength) {
        double s = 0;
        for (int i = 0; i < segLengths.size(); i++) {
            if (s + segLengths.get(i) >= targetLength) {
                Point2D p0 = pts.get(i);
                Point2D p1 = pts.get(i + 1);
                return Math.atan2(p1.getY() - p0.getY(), p1.getX() - p0.getX());
            }
            s += segLengths.get(i);
        }
        Point2D p0 = pts.get(pts.size() - 2);
        Point2D p1 = pts.get(pts.size() - 1);
        return Math.atan2(p1.getY() - p0.getY(), p1.getX() - p0.getX());
    }

    public static void drawStringAlongCurve(NTxGraphics g,String str, Point2D[] points, NTxTextOptions options) {
        if (str == null || str.isEmpty() || points == null || points.length < 2) return;

        Font font = options.getComputedFont();
        GlyphVector gv = g.getFont().createGlyphVector(g.getFontRenderContext(), str);

        // Step 1: build a dense sample of points along the curve
        List<Point2D> curvePoints = interpolatePoints(points, 2.0); // spacing ~2px
        List<Double> segmentLengths = computeSegmentLengths(curvePoints);

        // Step 2: cumulative lengths
        double totalLength = segmentLengths.stream().mapToDouble(Double::doubleValue).sum();
        double s = 0; // distance along curve

        for (int i = 0; i < gv.getNumGlyphs(); i++) {
            Shape glyph = gv.getGlyphOutline(i);
            double advance = gv.getGlyphMetrics(i).getAdvance();

            // Step 3: find curve point for current glyph
            double posAlongCurve = s + advance / 2.0; // center of glyph
            Point2D pos = getPointAtLength(curvePoints, segmentLengths, posAlongCurve);

            // Step 4: compute tangent
            double angle = getTangentAngle(curvePoints, segmentLengths, posAlongCurve);

            // Step 5: transform and draw glyph
            AffineTransform at = new AffineTransform();
            at.translate(pos.getX(), pos.getY());
            at.rotate(angle);
            at.translate(-advance / 2.0, 0); // center horizontally
            g.fill(at.createTransformedShape(glyph));

            s += advance;
        }
    }

    public static void drawShadowed(NTxGraphics graphics, Consumer<NTxGraphics> consumer, NTxBounds2 selfBounds, NTxShadow shadow) {
        NTxGraphics g2d = graphics.copy();
        // Render text to offscreen image (solid white for shadow)
        BufferedImage textImage = new BufferedImage(selfBounds.getWidth().intValue(), selfBounds.getHeight().intValue(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D tg = textImage.createGraphics();
        consumer.accept(graphics.engine().createGraphics(tg));
        tg.dispose();
        if(shadow.getColor() instanceof Color) {
            textImage=tint(textImage,(Color) shadow.getColor());
        }
        BufferedImage blurred = applyGaussianBlur(textImage, 2);

        // Draw blurred image as shadow, offset by (0,2)
        double alpha = shadow.getAlpha();
        if(Double.isNaN(alpha) || alpha<=0 || alpha>1){
            alpha = 0.4f;
        }
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
        NTxPoint2D translation = shadow.getTranslation();


        AffineTransform at = new AffineTransform();
        at.translate(translation==null?0:translation.getX(), translation==null?0:translation.getY());
        if(shadow.getZoom()!=null && (!Double.isNaN(shadow.getZoom().x) && shadow.getZoom().x>0 && shadow.getZoom().x!=1) && (!Double.isNaN(shadow.getZoom().y) && shadow.getZoom().y>0 && shadow.getZoom().y!=1)) {
            AffineTransform zoom = AffineTransform.getScaleInstance(shadow.getZoom().x, shadow.getZoom().y);
            at.concatenate(zoom);
        }
        if(shadow.getShear()!=null && ((!Double.isNaN(shadow.getShear().x) && shadow.getShear().x!=0) || (!Double.isNaN(shadow.getShear().y) && shadow.getShear().y!=0))) {
            double xx=shadow.getShear().x;
            double yy=shadow.getShear().y;
            AffineTransform shear = AffineTransform.getShearInstance(xx, yy); // shearX = 0.5
            at.concatenate(shear);
        }
        g2d.graphics2D().drawImage(blurred, at, null);

//        g2d.drawImage(blurred, translation==null?0:translation.getX(), translation==null?0:translation.getY(), null);

        // Draw actual text
        g2d.setComposite(AlphaComposite.SrcOver);
        consumer.accept(g2d);
        g2d.dispose();
    }

    public static BufferedImage applyGaussianBlur(BufferedImage src, float radius) {
        float[] kernel = createGaussianKernel(radius);

        // horizontal blur
        ConvolveOp op1 = new ConvolveOp(new Kernel(kernel.length, 1, kernel),
                ConvolveOp.EDGE_NO_OP, null);
        BufferedImage tmp = op1.filter(src, null);

        // vertical blur
        ConvolveOp op2 = new ConvolveOp(new Kernel(1, kernel.length, kernel),
                ConvolveOp.EDGE_NO_OP, null);
        return op2.filter(tmp, null);
    }

}
