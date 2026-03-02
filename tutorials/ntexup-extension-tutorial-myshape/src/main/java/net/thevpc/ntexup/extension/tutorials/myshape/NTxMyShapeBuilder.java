package net.thevpc.ntexup.extension.tutorials.myshape;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;

import java.awt.*;
import java.awt.geom.GeneralPath;


/**
 *
 */
public class NTxMyShapeBuilder implements NTxNodeBuilder {
    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id("my-shape")
                .parseParam().matchesNamedPair(NTxPropName.WIDTH, NTxPropName.HEIGHT,"base","hat").end()
                .renderComponent(this::render)
        ;
    }

    public void render(NTxRendererContext rendererContext) {
        NTxNode node=rendererContext.node();
        NTxBounds2D b = rendererContext.selfBounds2D();
        double x = b.minX();
        double y = b.minY();
        double width = b.widthX();
        double height = b.widthY();

        Paint color = rendererContext.getForegroundColor(true);
        NTxPoint2D base = NTxValue.of(node.getPropertyValue("base")).asPoint2DOrDouble().orNull();
        if (base == null) {
            base = new NTxPoint2D(80, 20);
        }
        NTxPoint2D hat = NTxValue.of(node.getPropertyValue("hat")).asPoint2DOrDouble().orNull();
        if (hat == null) {
            hat = new NTxPoint2D(-1, -1);
        }
        if (hat.y < 0) {
            hat.y = 15;
        }
        if (hat.x < 0) {
            hat.x = 0;
        }
        double baseH = base.y * height / 100.0;
        double baseW = base.x * width / 100.0;
        double hatH = hat.y * height / 100.0;
        double hatW = hat.x * width / 100.0;

        NTxGraphics g = rendererContext.graphics();
        g.setPaint(color);
        GeneralPath arrow = new GeneralPath();
        arrow.moveTo(x, y + height / 2);
        arrow.lineTo(x, y + height / 2 - baseH / 2);
        arrow.lineTo(x + baseW, y + height / 2 - baseH / 2);
        arrow.lineTo(x + baseW - hatW, y + height / 2 - baseH / 2 - hatH);
        arrow.lineTo(x + width, y + height / 2);
        arrow.lineTo(x + baseW - hatW, y + height / 2 + baseH / 2 + hatH);
        arrow.lineTo(x + baseW, y + height / 2 + baseH / 2);
        arrow.lineTo(x, y + height / 2 + baseH / 2);
        arrow.closePath();
        g.draw(arrow);
        g.fill(arrow);
        rendererContext.drawContour();

    }
}
