package net.thevpc.ntexup.engine.renderer.text;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.*;
import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererBuilder;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
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
    public NTxSizeRequirements sizeRequirements(NTxNodeRendererContext ctx) {
        NTxBounds2D s = ctx.selfBounds();
        NTxBounds2D bb = ctx.parentBounds();
        return new NTxSizeRequirements(
                s.getWidth(),
                Math.max(bb.getWidth(), s.getWidth()),
                s.getWidth(),
                s.getHeight(),
                Math.max(bb.getHeight(), s.getHeight()),
                s.getHeight()
        );
    }

    public NTxBounds2D bgBounds(NTxNode p, NTxNodeRendererContext ctx) {
        return ctx.selfBounds(p, null, null);
    }

    public NTxBounds2D selfBounds(NTxNodeRendererContext ctx) {
        return defaultSelfBounds(ctx);
    }

    public NTxBounds2D defaultSelfBounds(NTxNodeRendererContext ctx) {
        Cache0 renderInfo = renderInfo0(ctx);
        return NTxValueByName.selfBounds(ctx.node(), new NTxDouble2(renderInfo.computedBound.getWidth(), renderInfo.computedBound.getHeight()), null, ctx);
    }

    protected abstract NTxTextRendererBuilder createRichTextHelper(NTxNodeRendererContext ctx);

    static class Cache0 {
        NTxTextRendererBuilder helper;
        NTxBounds2D computedBound;
    }
    static class Cache {
        NTxBounds2D bgBounds0;
        NTxBounds2D bgBounds;
        NTxBounds2D selfBounds;
    }
    private Cache0 renderInfo0(NTxNodeRendererContext ctx){
        return ctx.node().getAndSetRenderCache(Cache0.class,ctx.isSomeChange(),()->{
            Cache0 ri = new Cache0();
            ri.helper = createRichTextHelper(ctx);
            ri.computedBound=ri.helper.computeBound(ctx);
            return ri;
        }).get();
    }
    private Cache renderInfo(NTxNode p, NTxNodeRendererContext ctx, NTxBounds2D selfBounds0){
        return p.getAndSetRenderCache(Cache.class,ctx.isSomeChange(),()->{
            Cache ri = new Cache();
            ri.bgBounds0 = bgBounds(p, ctx);
            ri.bgBounds = ri.bgBounds0;
            ri.selfBounds = selfBounds0;
            ri.bgBounds = ri.bgBounds.expand(ri.selfBounds);
            return ri;
        }).get();
    }

    public void renderMain(NTxNodeRendererContext ctx) {
        ctx = ctx.withDefaultStyles(defaultStyles);
        NTxNode node = ctx.node();
        NTxGraphics g = ctx.graphics();
        NTxNodeRendererUtils.applyFont(node, g, ctx);

        NTxNodeRendererContext finalCtx = ctx;
        Cache0 renderInfo0 = renderInfo0(ctx);
        Cache renderInfo = renderInfo(node,ctx,ctx.selfBounds());
        if (NTxValueByName.getDebugLevel(node, ctx) >= 10) {
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
                                        NElement n = finalCtx.computePropertyValue(node, x).orNull();
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
        NTxNodeRendererUtils.drawDebugBox(node, ctx, g, renderInfo.selfBounds);
    }
}
