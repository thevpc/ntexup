package net.thevpc.ntexup.engine.renderer.text;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.*;
import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererBuilder;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.engine.util.NTxNodeRendererUtils;
import net.thevpc.ntexup.engine.renderer.NTxNodeRendererBase;
import net.thevpc.nuts.elem.NElement;

import java.util.*;
import java.util.stream.Collectors;

public abstract class NTxTextBaseRenderer extends NTxNodeRendererBase {


    protected NTxProperties defaultStyles = new NTxProperties(null);

    public NTxTextBaseRenderer(String... types) {
        super(types);
    }

    @Override
    public NTxSizeRequirements sizeRequirements(NTxRendererContext ctx) {
        NTxBounds2D s = ctx.selfBounds2D();
        NTxBounds2D bb = ctx.parentBounds2D();
        return new NTxSizeRequirements(
                s.widthX(),
                Math.max(bb.widthX(), s.widthX()),
                s.widthX(),
                s.widthY(),
                Math.max(bb.widthY(), s.widthY()),
                s.widthY()
        );
    }

    public NTxBounds2D bgBounds(NTxNode p, NTxRendererContext ctx) {
        return ctx.selfBounds2D();
    }

    public NTxBounds2D selfBounds2D(NTxRendererContext ctx) {
        Cache0 renderInfo = renderInfo0(ctx);
        return NTxValueByName.selfBounds2D(new NTxDouble2(renderInfo.computedBound.widthX(), renderInfo.computedBound.widthY()), null, ctx);
    }

    protected abstract NTxTextRendererBuilder createRichTextHelper(NTxRendererContext ctx);

    static class Cache0 {
        NTxTextRendererBuilder helper;
        NTxBounds2D computedBound;
    }
    static class Cache {
        NTxBounds2D bgBounds0;
        NTxBounds2D bgBounds;
        NTxBounds2D selfBounds;
    }
    private Cache0 renderInfo0(NTxRendererContext ctx){
        return ctx.node().getAndSetRenderCache(Cache0.class,ctx.isSomeChange(),()->{
            Cache0 ri = new Cache0();
            ri.helper = createRichTextHelper(ctx);
            ri.computedBound=ri.helper.computeBound(ctx);
            return ri;
        }).get();
    }
    private Cache renderInfo(NTxNode p, NTxRendererContext ctx, NTxBounds2D selfBounds0){
        return p.getAndSetRenderCache(Cache.class,ctx.isSomeChange(),()->{
            Cache ri = new Cache();
            ri.bgBounds0 = bgBounds(p, ctx);
            ri.bgBounds = ri.bgBounds0;
            ri.selfBounds = selfBounds0;
            ri.bgBounds = ri.bgBounds.expand(ri.selfBounds);
            return ri;
        }).get();
    }

    public void renderMain(NTxRendererContext ctx) {
        ctx = ctx.withDefaultStyles(defaultStyles);
        NTxNode node = ctx.node();
        NTxGraphics g = ctx.graphics();
        NTxNodeRendererUtils.applyFont(g, ctx);

        NTxRendererContext finalCtx = ctx;
        Cache0 renderInfo0 = renderInfo0(ctx);
        Cache renderInfo = renderInfo(node,ctx,ctx.selfBounds2D());
        if (NTxValueByName.getDebugLevel(ctx) >= 10) {
            g.debugString(
                    "Plain:\n"
                            + "expected=" + renderInfo.bgBounds0 + "\n"
                            + "fullSize=" + renderInfo.selfBounds + "\n"
                            + "newExpectedBounds=" + renderInfo.bgBounds + "\n"
                            + "curr: "
                            + Arrays.asList(
                                    NTxPropName.SIZE,
                                    NTxPropName.ORIGIN,
                                    NTxPropName.POSITION
                            )
                            .stream().map(x
                                    -> node.getProperty(x).orNull()
                            ).filter(x -> x != null).collect(Collectors.toList())
                            + "\n"
                            + "eff: "
                            + Arrays.asList(
                                    NTxPropName.SIZE,
                                    NTxPropName.ORIGIN,
                                    NTxPropName.POSITION
                            )
                            .stream().map(x
                                            -> {
                                        NElement n = finalCtx.computePropertyValue(x).orNull();
                                        if (n == null) {
                                            return n;
                                        }
                                        return new NTxProp(x, n, node);
                                    }
                            ).filter(x -> x != null).collect(Collectors.toList())
                            + "\n",
                    30, 100
            );
        }

        renderInfo0.helper.render(node, ctx, renderInfo.bgBounds, renderInfo.selfBounds);
        NTxNodeRendererUtils.drawDebugBox(ctx, g, renderInfo.selfBounds);
    }
}
