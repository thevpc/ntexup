/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.extension.shapes2d.container;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;

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
                .sizeRequirements(this::sizeRequirements)
                .selfBounds2D(this::selfBounds)
        ;
    }

    public NTxBounds2D selfBounds(NTxRendererContext rendererContext) {
        return preComputed(rendererContext).selfBounds;
    }

    private static class Elems {
        Elem[] elems;
        NTxDouble2 size;
        NTxDouble2 fullSize;
    }

    private static class Elem {
        NTxNode node;
        NTxSizeRequirements sizeRequirements;
        NTxBounds2D bounds;
    }

    private Elems compute(NTxNode p, NTxBounds2D expectedBounds, NTxRendererContext ctx) {
        List<NTxNode> texts = p.children()
                .stream().filter(x -> ctx.isVisible()).collect(Collectors.toList());
        Elems e = new Elems();
        e.elems = new Elem[texts.size()];
        double allWidth = 0;
        double allHeight = 0;

        Double expectedWidth = expectedBounds.widthX();
        Double expectedHeight = expectedBounds.widthY();
        double xRef = expectedBounds.minX();
        double yRef = expectedBounds.minY();
        NTxRendererContext ctx2 = ctx.withParentBounds(NTxBounds2D.ofWidth(0, 0, expectedWidth, expectedHeight));
        for (int i = 0; i < texts.size(); i++) {
            NTxNode text = texts.get(i);
            NTxSizeRequirements ee = ctx2.sizeRequirementsOf();
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
            zz.bounds = NTxBounds2D.ofWidth(xRef, yRef, w, h);
            if (e.size == null) {
                allWidth = zz.bounds.widthX();
                allHeight = zz.bounds.widthY();
                e.size = new NTxDouble2(allWidth, allHeight);
            } else {
                allWidth += zz.bounds.widthX();
                allHeight = Math.max(zz.bounds.widthY(), allHeight);
                e.size = new NTxDouble2(allWidth, allHeight);
            }
            xRef += w;
        }
        double w = Math.max(expectedWidth, e.size == null ? 0 : e.size.getX());
        double h = Math.max(expectedHeight, e.size == null ? 0 : e.size.getY());
        e.fullSize = new NTxDouble2(w, h);
        return e;
    }

    public NTxSizeRequirements sizeRequirements(NTxRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        NTxBounds2D bg = rendererContext.selfBounds2D();
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

    private static class PreComputed {
        NTxBounds2D defaultSelfBounds;
        NTxBounds2D selfBounds;
        Elems ee;
    }

    public PreComputed preComputed(NTxRendererContext rendererContext) {
        PreComputed u = (PreComputed) rendererContext.node().getRenderCache(PreComputed.class.getName()).orNull();
        if (u == null) {
            u = new PreComputed();
            rendererContext = rendererContext.withDefaultStyles(defaultStyles);
            NTxGraphics g = rendererContext.graphics();
            NTxNode node = rendererContext.node();

            NTxBounds2D defaultSelfBounds = rendererContext.defaultSelfBounds2D();
            NTxBounds2D selfBounds = defaultSelfBounds;
            Elems ee = compute(node, selfBounds, rendererContext);
            NTxBounds2D newExpectedBounds = rendererContext.selfBounds2D(ee.size, null);

//        g.setColor(Color.BLUE);
//        g.drawRect(newExpectedBounds);
            NTxRendererContext ctx2 = rendererContext.withParentBounds(newExpectedBounds);
            u.ee = compute(node, newExpectedBounds, ctx2);

            selfBounds = selfBounds.expand(newExpectedBounds);
            u.defaultSelfBounds = defaultSelfBounds;
            u.selfBounds = selfBounds;
            rendererContext.node().setRenderCache(PreComputed.class.getName(), u);
        }
        return u;
    }

    public void renderMain(NTxRendererContext rendererContext) {
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxGraphics g = rendererContext.graphics();
        NTxNode node = rendererContext.node();
        PreComputed preComputed = preComputed(rendererContext);


//        g.setColor(Color.BLUE);
//        g.drawRect(newExpectedBounds);
        if (rendererContext.getDebugLevel() >= 10) {
            g.debugString(
                    "Flow:\n"
                            + "expected=" + preComputed.defaultSelfBounds + "\n"
                            + "fullSize=" + preComputed.ee.fullSize.toString() + "\n"
                            + "newExpectedBounds=" + preComputed.selfBounds.toString(),
                    30, 30
            );
        }
        if (!rendererContext.isDry()) {
            rendererContext.paintBackground(preComputed.defaultSelfBounds);
        }

        for (Elem elem : preComputed.ee.elems) {
            NTxRendererContext ctx3 = rendererContext.resolveNode(elem.node, elem.bounds);
            ctx3.render();
        }


        rendererContext.paintBorderLine(preComputed.defaultSelfBounds);
    }
}
