package net.thevpc.ntexup.engine.renderer;

import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRenderer;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.engine.util.NTx2DUtils0;
import net.thevpc.ntexup.engine.util.NTxNodeRendererUtils;
import net.thevpc.nuts.util.NOptional;

public abstract class NTxNodeRendererBase implements NTxNodeRenderer {

    private String[] types;

    public NTxNodeRendererBase(String... types) {
        this.types = types;
    }

    @Override
    public NTxSizeRequirements sizeRequirements(NTxNodeRendererContext ctx) {
        NTxBounds2D bounds = ctx.selfBounds();
        return new NTxSizeRequirements(
                0,
                bounds.getWidth(),
                bounds.getWidth(),
                0,
                bounds.getHeight(),
                bounds.getHeight()
        );
    }

    @Override
    public String[] types() {
        return types;
    }

    public void render(NTxNodeRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        boolean v = NTxValueByName.isVisible(node, rendererContext);
        if (!v) {
            return;
        }

        NTxBounds2D selfBounds = rendererContext.selfBounds();
        NTxGraphics nv = null;
        try {
            if (!rendererContext.isDry()) {
                NTxRotation rotation = NTxValueByName.getRotation(node, rendererContext);
                if (rotation != null) {
                    double angle = NTxValue.of(rotation.getAngle()).asDouble().orElse(0.0);
                    if (angle != 0) {
                        angle = angle / 180.0 * Math.PI;
                        if (angle != 0) {
                            NTxGraphics g = rendererContext.graphics();
                            nv = g.copy();
                            ///HSizeRef sr=new HSizeRef();
                            double rotX = NTxValue.of(rotation.getX()).asDouble().get() / 100.0 * selfBounds.getWidth() + selfBounds.getX();
                            double rotY = NTxValue.of(rotation.getY()).asDouble().get() / 100.0 * selfBounds.getHeight() + selfBounds.getY();
                            if (rendererContext.isDebug(node)) {
                                g.setColor(NTxValueByName.getDebugColor(node, rendererContext));
                                g.drawRect(selfBounds);
                                g.fillRect(rotX - 3, rotY - 3, 6, 6);
//                            g.drawString(rotX + "," + rotY+" : "+p, 100, 100);
                            }
                            nv.rotate(
                                    angle,
                                    rotX,
                                    rotY
                            );
                            rendererContext = rendererContext.withGraphics(nv);
                        }
                    }
                }

                NOptional<NTxShadow> shadowOptional = NTxValueByName.readStyleAsShadow(node, NTxPropName.SHADOW, rendererContext);
                if (shadowOptional.isPresent() && !shadowOptional.get().isBlank()) {
                    NTxShadow shadow = shadowOptional.get();
                    NTxNodeRendererContext finalRendererContext = rendererContext;
                    NTx2DUtils0.drawShadowed(rendererContext.graphics(), gg -> {
                        renderMain(finalRendererContext.withGraphics(gg));
                    }, finalRendererContext.getGlobalBounds(), shadow);
                } else {
                    renderMain(rendererContext);
                }
                NTxNodeRendererUtils.drawDebugBox(rendererContext.node(), rendererContext, rendererContext.graphics(), rendererContext.selfBounds());

            }
        } finally {
            if (nv != null) {
                nv.dispose();
            }
        }
    }

    public abstract void renderMain(NTxNodeRendererContext ctx);

    public NTxBounds2D bgBounds(NTxNode p, NTxNodeRendererContext ctx) {
        return ctx.selfBounds(p, null, null);
    }

    public NTxBounds2D selfBounds(NTxNodeRendererContext ctx) {
        return NTxValueByName.selfBounds(ctx.node(), null, null, ctx);
    }

    public NTxBounds2D defaultSelfBounds(NTxNodeRendererContext ctx) {
        return NTxValueByName.selfBounds(ctx.node(), null, null, ctx);
    }

}
