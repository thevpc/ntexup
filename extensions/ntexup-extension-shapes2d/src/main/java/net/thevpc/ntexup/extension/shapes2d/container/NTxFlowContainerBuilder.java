/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.extension.shapes2d.container;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author vpc
 */
public class NTxFlowContainerBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext.id(NTxNodeType.FLOW)
                .renderComponent(this::renderMain)
                .sizeRequirements(this::sizeRequirements);
    }


    private static class Elems {
        Elem[] elems;
        NTxDouble2 size;
        NTxDouble2 fullSize;
    }

    private static class Elem {
        NTxNode node;
        NTxSizeRequirements sizeRequirements;
        NTxBounds2 bounds;
    }

    private Elems compute(NTxNode p, NTxBounds2 expectedBounds, NTxNodeRendererContext ctx) {
        List<NTxNode> texts = p.children()
                .stream().filter(x -> ctx.isVisible(x)).collect(Collectors.toList());
        Elems e = new Elems();
        e.elems = new Elem[texts.size()];
        double allWidth = 0;
        double allHeight = 0;

        Double expectedWidth = expectedBounds.getWidth();
        Double expectedHeight = expectedBounds.getHeight();
        double xRef = expectedBounds.getX();
        double yRef = expectedBounds.getY();
        NTxNodeRendererContext ctx2 = ctx.withParentBounds(new NTxBounds2(0, 0, expectedWidth, expectedHeight));
        for (int i = 0; i < texts.size(); i++) {
            NTxNode text = texts.get(i);
            NTxSizeRequirements ee = ctx2.sizeRequirementsOf(text);
            double w = ee.minX;
            if (w <= 0) {
                w = 10;
            }
            double h = ee.minY;
            if (h <= 0) {
                h = 10;
            }
            Elem zz = new Elem();
            e.elems[i] = zz;
            zz.node = text;
            zz.bounds = new NTxBounds2(xRef, yRef, w, h);
            if (e.size == null) {
                allWidth = zz.bounds.getWidth();
                allHeight = zz.bounds.getHeight();
                e.size = new NTxDouble2(allWidth, allHeight);
            } else {
                allWidth += zz.bounds.getWidth();
                allHeight = Math.max(zz.bounds.getHeight(), allHeight);
                e.size = new NTxDouble2(allWidth, allHeight);
            }
            xRef += w;
        }
        double w = Math.max(expectedWidth, e.size == null ? 0 : e.size.getX());
        double h = Math.max(expectedHeight, e.size == null ? 0 : e.size.getY());
        e.fullSize = new NTxDouble2(w, h);
        return e;
    }

    public NTxSizeRequirements sizeRequirements(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        NTxNode node = rendererContext.node();
        NTxBounds2 bg = rendererContext.selfBounds();
        Elems ee = compute(node, bg, rendererContext);
        return new NTxSizeRequirements(
                ee.size.getX(),
                ee.fullSize.getX(),
                ee.fullSize.getX(),
                ee.size.getY(),
                ee.fullSize.getY(),
                ee.fullSize.getY()
        );
    }

    public void renderMain(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxGraphics g = rendererContext.graphics();
        NTxNode node = rendererContext.node();

        NTxBounds2 bg = rendererContext.selfBounds();
        Elems ee = compute(node, bg, rendererContext);
        NTxBounds2 newExpectedBounds = rendererContext.selfBounds(node, ee.size, null);

//        g.setColor(Color.BLUE);
//        g.drawRect(newExpectedBounds);
        if (rendererContext.getDebugLevel(node) >= 10) {
            g.debugString(
                    "Flow:\n"
                            + "expected=" + bg + "\n"
                            + "fullSize=" + ee.fullSize.toString() + "\n"
                            + "newExpectedBounds=" + newExpectedBounds.toString(),
                    30, 30
            );
        }
        NTxNodeRendererContext ctx2 = rendererContext.withParentBounds(newExpectedBounds);
        ee = compute(node, newExpectedBounds, ctx2);

        bg = bg.expand(newExpectedBounds);
        if (!rendererContext.isDry()) {
            rendererContext.paintBackground(node, bg);
        }

        for (Elem elem : ee.elems) {
            NTxNodeRendererContext ctx3 = rendererContext.withChild(elem.node, elem.bounds);
            ctx3.render();
        }


        rendererContext.paintBorderLine(node, bg);
    }
}
