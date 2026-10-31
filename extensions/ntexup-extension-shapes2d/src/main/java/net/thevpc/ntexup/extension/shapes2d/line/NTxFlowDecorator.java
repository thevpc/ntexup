package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;

import java.awt.Color;
import java.awt.Paint;

public class NTxFlowDecorator {
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
        double elapsed = computeElapsedSeconds(ctx);
        double len = path.length();
        int count = Math.max(1, flow.count);
        double size = flow.size;
        if (size <= 0) {
            java.awt.Stroke st = ctx.resolveStroke();
            if (st instanceof java.awt.BasicStroke) {
                size = Math.max(2.0, ((java.awt.BasicStroke) st).getLineWidth() * 1.5);
            } else {
                size = 2.0;
            }
        }
        Paint paint = flow.color != null ? flow.color : ctx.getLineColor(true);
        Color oldColor = g.getColor();
        g.setColor(paint instanceof Color ? (Color) paint : Color.BLACK);
        for (int i = 0; i < count; i++) {
            double u = phase(elapsed, flow.period, i, count, flow.reverse);
            NTxPoint2D p = path.pointAt(u * len);
            g.fillOval(p.x - size, p.y - size, size * 2, size * 2);
        }
        g.setColor(oldColor);
        if (anim) {
            ctx.requestAnimationFrame(true);
        }
    }
}
