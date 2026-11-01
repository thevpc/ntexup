package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

public class NTxFlowDecorator {

    /**
     * Fraction of the path over which a particle ramps in and out at each end.
     */
    static final double FADE_BAND = 0.12;
    /**
     * Trail length behind a particle, as a multiple of its radius.
     */
    static final double TAIL_RATIO = 4.0;
    /**
     * Half-width at the far end of the trail, as a multiple of the particle radius.
     * Tapering all the way to zero leaves a needle; a blunt end reads as a comet.
     */
    static final double TAIL_END_RATIO = 0.15;
    /**
     * How the trail narrows along its length. Above one it thins fast beside the head and
     * then runs on as a streak; a linear taper leaves a triangle that reads as an arrowhead.
     */
    static final double TAIL_CURVE = 1.7;
    /**
     * Samples along the trail. Enough to curve with the route, few enough to stay cheap.
     */
    static final int TAIL_STEPS = 10;

    /**
     * Position of the {@code index}-th flow dot along the path, as a ratio in {@code [0,1]}.
     * Static rendering uses {@code elapsedSeconds == 0}, which spaces the dots evenly.
     */
    static double phase(double elapsedSeconds, double period, int index, int count, boolean reverse) {
        double u = (elapsedSeconds / Math.max(0.001, period) + (double) index / Math.max(1, count)) % 1.0;
        if (u < 0) {
            u += 1.0;
        }
        return reverse ? 1.0 - u : u;
    }

    /**
     * Seconds for one particle to cross the path. {@code speed} is a page-unit rate and so
     * makes the crossing time grow with the connector, which is what keeps two connectors
     * of different length moving at the same apparent rate; {@code period} is the older
     * fixed-crossing-time form and wins when no speed is given.
     */
    static double traversalPeriod(double period, double speed, double lengthUnits) {
        if (speed <= 0 || lengthUnits <= 0) {
            return period;
        }
        return Math.max(0.05, lengthUnits / speed);
    }

    /**
     * Path length expressed in page units. The unit space is 100x100 stretched over the
     * box, so the two axes carry different rates and a diagonal cannot be rescaled from
     * the pixel length alone; each segment is converted on its own axes first.
     */
    static double lengthInPageUnits(NTxRoutedPath path, double pixelsPerUnitX, double pixelsPerUnitY) {
        java.util.List<NTxPoint2D> v = path.vertices();
        double acc = 0;
        for (int i = 1; i < v.size(); i++) {
            double dx = (v.get(i).x - v.get(i - 1).x) / pixelsPerUnitX;
            double dy = (v.get(i).y - v.get(i - 1).y) / pixelsPerUnitY;
            acc += Math.hypot(dx, dy);
        }
        return acc;
    }

    /**
     * Device pixels covered by one page unit along each axis of the node's parent box.
     * A box too small to measure falls back to one pixel per unit rather than dividing
     * the crossing time by zero.
     */
    static double pixelsPerUnitX(NTxRendererContext ctx) {
        double w = ctx.sizeRef().getParentWidth();
        return w > 0 ? w / 100 : 1;
    }

    static double pixelsPerUnitY(NTxRendererContext ctx) {
        double h = ctx.sizeRef().getParentHeight();
        return h > 0 ? h / 100 : 1;
    }

    /**
     * Opacity of a particle at ratio {@code u} along the path: zero at the two anchors,
     * full in the middle. Without it, a dot appears and vanishes abruptly at the ends.
     */
    static double fadeAlpha(double u, double band) {
        if (band <= 0) {
            return 1.0;
        }
        double edge = Math.min(u, 1.0 - u);
        if (edge <= 0) {
            return 0.0;
        }
        return Math.min(1.0, edge / band);
    }

    public static double computeElapsedSeconds(NTxRendererContext ctx) {
        if (ctx == null || !ctx.isAnimate()) {
            return 0.0;
        }
        return ctx.pageElapsedSeconds();
    }

    public static void draw(NTxRendererContext ctx, NTxGraphics g, NTxRoutedPath path, NTxFlow flow) {
        if (flow == null) {
            return;
        }
        boolean anim = ctx.isAnimate();
        double len = path.length();
        if (len <= 0) {
            return;
        }
        double period = traversalPeriod(flow.period, flow.speed,
                lengthInPageUnits(path, pixelsPerUnitX(ctx), pixelsPerUnitY(ctx)));
        double elapsed = computeElapsedSeconds(ctx);
        int count = Math.max(1, flow.count);
        Paint paint = flow.color != null ? flow.color : ctx.getLineColor(true);
        Color baseColor = paint instanceof Color ? (Color) paint : Color.BLACK;
        double lineWidth = 1.0;
        Stroke st = ctx.resolveStroke();
        if (st instanceof java.awt.BasicStroke) {
            lineWidth = ((java.awt.BasicStroke) st).getLineWidth();
        }
        //A dot must read as a dot: a bead on the wire it rides on, never a couple of
        //device pixels (anti-aliasing would make a sub-pixel oval vanish at small zooms).
        double minSize = Math.max(3.0, lineWidth * 2.0);
        double size = flow.size > 0 ? Math.max(flow.size, minSize) : minSize;
        //Keep the particles off the source border and out of the arrowhead: the first
        //and last dot of the cycle used to sit exactly on the anchors and read as an
        //artefact glued to the box.
        double inset = Math.min(size, len * 0.15);
        double span = Math.max(0.001, len - 2 * inset);
        double travel = flow.reverse ? -1 : 1;
        Color oldColor = g.getColor();
        for (int i = 0; i < count; i++) {
            double u = phase(elapsed, period, i, count, flow.reverse);
            if (!anim) {
                //A static frame shows the train mid-flight rather than one dot parked
                //at each anchor, so a PDF export keeps all `count` particles visible.
                u = (u + 0.5 / count) % 1.0;
            }
            double alpha = fadeAlpha(u, FADE_BAND);
            if (alpha <= 0.02) {
                continue;
            }
            int a = (int) Math.max(0, Math.min(255, Math.round(baseColor.getAlpha() * alpha)));
            drawParticle(g, path, len, inset + u * span, travel, size,
                    new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), a));
        }
        g.setColor(oldColor);
        if (anim) {
            ctx.requestAnimationFrame(true);
        }
    }

    /**
     * Half-width of the trail at fraction {@code t} of its length behind the head.
     */
    static double trailHalfWidth(double size, double t) {
        double fall = Math.pow(1 - t, TAIL_CURVE);
        return size * (TAIL_END_RATIO + (1 - TAIL_END_RATIO) * fall);
    }

    /**
     * One particle: a round head on the path plus a trail that tapers behind it, drawn as
     * a single fill so the overlapping shapes cannot band where their alpha stacks.
     */
    private static void drawParticle(NTxGraphics g, NTxRoutedPath path, double len,
                                     double head, double travel, double size, Color color) {
        g.setColor(color);
        NTxPoint2D p = path.pointAt(head);
        //fill(Shape) rather than fillOval: the oval primitive truncates to whole pixels,
        //so a moving dot snapped between integer positions and sat up to a pixel off the
        //line it rides on.
        g.fill(new Ellipse2D.Double(p.x - size, p.y - size, size * 2, size * 2));
        //The trail cannot reach past the anchor the particle came from, or it would
        //bunch up on the clamped endpoint and smear the anchor.
        double room = travel > 0 ? head : len - head;
        double tail = Math.min(size * TAIL_RATIO, Math.max(0, room));
        if (tail <= size * 0.5) {
            return;
        }
        int steps = TAIL_STEPS;
        double[] cx = new double[steps + 1];
        double[] cy = new double[steps + 1];
        double[] nx = new double[steps + 1];
        double[] ny = new double[steps + 1];
        double[] hw = new double[steps + 1];
        for (int k = 0; k <= steps; k++) {
            double s = head - travel * tail * ((double) k / steps);
            NTxPoint2D c = path.pointAt(s);
            NTxPoint2D n = path.normalAt(s);
            cx[k] = c.x;
            cy[k] = c.y;
            nx[k] = n.x;
            ny[k] = n.y;
            hw[k] = trailHalfWidth(size, (double) k / steps);
        }
        Path2D.Double ribbon = new Path2D.Double();
        ribbon.moveTo(cx[0] + nx[0] * hw[0], cy[0] + ny[0] * hw[0]);
        for (int k = 1; k <= steps; k++) {
            ribbon.lineTo(cx[k] + nx[k] * hw[k], cy[k] + ny[k] * hw[k]);
        }
        for (int k = steps; k >= 0; k--) {
            ribbon.lineTo(cx[k] - nx[k] * hw[k], cy[k] - ny[k] * hw[k]);
        }
        ribbon.closePath();
        g.fill(ribbon);
    }
}
