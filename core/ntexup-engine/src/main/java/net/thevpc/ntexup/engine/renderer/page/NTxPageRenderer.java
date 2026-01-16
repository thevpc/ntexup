package net.thevpc.ntexup.engine.renderer.page;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.engine.renderer.NTxNodeRendererBase;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxSizeRef;

import net.thevpc.ntexup.engine.util.NTxNodeRendererUtils;
import net.thevpc.nuts.elem.NElement;

import java.awt.*;

public class NTxPageRenderer extends NTxNodeRendererBase {
    public NTxPageRenderer() {
        super(NTxNodeType.PAGE);
    }

    @Override
    public void renderMain(NTxNodeRendererContext ctx) {
        NTxBounds2D b = ctx.selfBounds(ctx.node(), null, null);

        drawBackground(ctx.node(), ctx.graphics(), ctx, b);
//        drawGrid(ctx.graphics(), b);

        for (NTxNode child : ctx.node().children()) {
            ctx.withChild(child, b)
                    .render();
        }
        //perhaps add page sum ??
    }

    private void drawGrid(NTxGraphics g, NTxBounds2D b) {
        int width = b.getWidth().intValue();
        int height = b.getHeight().intValue();
        Color color = Color.gray;
        g.setColor(color);
        NElement rowsSize = NElement.ofDouble(10, "%");
        NElement columnsSize = NElement.ofDouble(10, "%");

        Stroke os = g.getStroke();
        Stroke dashed = new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                0, new float[]{9}, 0);

        g.setStroke(dashed);

        NTxSizeRef sizeRef = new NTxSizeRef(b.getWidth(), b.getHeight(), b.getWidth(), b.getHeight());
        double rowsSizeEff = sizeRef.x(rowsSize).get();
        double columnsSizeEff = sizeRef.y(columnsSize).get();

        int rowsCount = (int) (height * 100 / rowsSizeEff + 0.5);
        int columnsCount = (int) (width * 100 / columnsSizeEff + 0.5);

        for (int i = 0; i < rowsCount; i++) {
            g.drawLine(0, i * rowsSizeEff,
                    width, i * rowsSizeEff
            );
        }
        for (int i = 0; i < columnsCount; i++) {
            g.drawLine(i * columnsSizeEff, 0,
                    i * columnsSizeEff, height
            );
        }
        g.setStroke(os);
    }

    private void drawBackground(NTxNode p, NTxGraphics g, NTxNodeRendererContext rendererContext, NTxBounds2D b) {
        NTxNodeRendererUtils.paintBackground(p, rendererContext, g, b);

//        int width = b.getWidth().intValue();
//        int height = b.getHeight().intValue();

//        g.setBackground(Color.white);
//        g.clearRect(0, 0, size.width, size.height);

//        g.setColor(Color.white);
//        g.fillRect(0, 0, width, height);

//        Color startColor = Color.white;
//        Color endColor = new Color(0xa3a3a3);
//        GradientPaint gradient = new GradientPaint(0, 0, startColor, width, height, endColor);
//        g.setPaint(gradient);
//        g.fill(new Rectangle(0, 0, width, height));
    }
}
