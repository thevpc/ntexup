package net.thevpc.ntexup.api.document.elem2d;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.util.Objects;

public class NTxLinearGradientPaint implements Paint {
    private final Point2D start; // Relative 0..100
    private final Point2D end;   // Relative 0..100
    private final float[] fractions;
    private final Color[] colors;
    private final MultipleGradientPaint.CycleMethod cycleMethod;

    public NTxLinearGradientPaint(Point2D start, Point2D end, float[] fractions, Color[] colors, MultipleGradientPaint.CycleMethod cycleMethod) {
        this.start = start != null ? start : new Point2D.Double(0, 50);
        this.end = end != null ? end : new Point2D.Double(100, 50);
        this.colors = Objects.requireNonNull(colors, "colors cannot be null");
        if (colors.length < 2) {
            throw new IllegalArgumentException("At least two colors are required for a gradient.");
        }
        this.fractions = fractions != null ? fractions : defaultFractions(colors.length);
        this.cycleMethod = cycleMethod != null ? cycleMethod : MultipleGradientPaint.CycleMethod.NO_CYCLE;
    }

    public static float[] defaultFractions(int length) {
        float[] fractions = new float[length];
        for (int i = 0; i < length; i++) {
            fractions[i] = (float) i / (float) (length - 1);
        }
        return fractions;
    }

    @Override
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
        Rectangle2D b = userBounds;
        if (b == null && deviceBounds != null) {
            b = deviceBounds;
        }
        Point2D s;
        Point2D e;
        if (b != null && b.getWidth() > 0 && b.getHeight() > 0) {
            double ux = b.getX();
            double uy = b.getY();
            double uw = b.getWidth();
            double uh = b.getHeight();
            s = new Point2D.Double(ux + (start.getX() / 100.0) * uw, uy + (start.getY() / 100.0) * uh);
            e = new Point2D.Double(ux + (end.getX() / 100.0) * uw, uy + (end.getY() / 100.0) * uh);
        } else {
            s = new Point2D.Double(start.getX(), start.getY());
            e = new Point2D.Double(end.getX(), end.getY());
        }
        if (s.distance(e) < 1e-4) {
            e = new Point2D.Double(s.getX() + 1, s.getY());
        }
        LinearGradientPaint lgp = new LinearGradientPaint(s, e, fractions, colors, cycleMethod);
        return lgp.createContext(cm, deviceBounds, userBounds, xform, hints);
    }

    @Override
    public int getTransparency() {
        for (Color c : colors) {
            if (c != null && c.getAlpha() < 255) {
                return Transparency.TRANSLUCENT;
            }
        }
        return Transparency.OPAQUE;
    }

    public Point2D getStart() { return start; }
    public Point2D getEnd() { return end; }
    public float[] getFractions() { return fractions; }
    public Color[] getColors() { return colors; }
    public MultipleGradientPaint.CycleMethod getCycleMethod() { return cycleMethod; }
}
