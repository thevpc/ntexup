package net.thevpc.ntexup.engine.renderer;

import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRenderer;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.engine.util.NTx2DUtils0;
import net.thevpc.ntexup.engine.util.NTxNodeRendererUtils;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

public abstract class NTxNodeRendererBase implements NTxNodeRenderer {

    private String[] types;

    public NTxNodeRendererBase(String... types) {
        this.types = types;
    }

    @Override
    public NTxSizeRequirements sizeRequirements(NTxRendererContext ctx) {
        NTxBounds2D bounds = ctx.selfBounds2D();
        return new NTxSizeRequirements(
                0,
                bounds.widthX(),
                bounds.widthX(),
                0,
                bounds.widthY(),
                bounds.widthY()
        );
    }

    @Override
    public String[] types() {
        return types;
    }

    public void render(NTxRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        boolean v = NTxValueByName.isVisible(rendererContext);
        if (!v) {
            return;
        }

        NTxBounds2D selfBounds = rendererContext.selfBounds2D();
        NTxGraphics nv = null;
        try {
            if (!rendererContext.isDry()) {
                NTxRotation rotation = NTxValueByName.getRotation(rendererContext);
                if (rotation != null) {
                    double angle = NTxValue.of(rotation.getAngle()).asDouble().orElse(0.0);
                    if (angle != 0) {
                        angle = angle / 180.0 * Math.PI;
                        if (angle != 0) {
                            NTxGraphics g = rendererContext.graphics();
                            nv = g.copy();
                            ///HSizeRef sr=new HSizeRef();
                            double rotX = NTxValue.of(rotation.getX()).asDouble().get() / 100.0 * selfBounds.widthX() + selfBounds.minX();
                            double rotY = NTxValue.of(rotation.getY()).asDouble().get() / 100.0 * selfBounds.widthY() + selfBounds.minY();
                            if (rendererContext.isDebug()) {
                                g.setColor(NTxValueByName.getDebugColor(rendererContext));
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

                NOptional<NTxShadow> shadowOptional = NTxValueByName.readStyleAsShadow(NTxPropName.SHADOW, rendererContext);
                if (shadowOptional.isPresent() && !shadowOptional.get().isBlank()) {
                    NTxShadow shadow = shadowOptional.get();
                    NTxRendererContext finalRendererContext = rendererContext;
                    NTx2DUtils0.drawShadowed(rendererContext.graphics(), gg -> {
                        renderMain(finalRendererContext.withGraphics(gg));
                    }, finalRendererContext.globalBounds2D(), shadow);
                } else {
                    renderMain(rendererContext);
                }
                NTxNodeRendererUtils.drawDebugBox(rendererContext, rendererContext.graphics(), rendererContext.selfBounds2D());

            }
        } catch (Exception ex){
            rendererContext.engine().log().log(NMsg.ofC("unable to render %s : %s",node.type(),ex).asFineFail(ex));
        } finally {
            if (nv != null) {
                nv.dispose();
            }
        }
    }

    public abstract void renderMain(NTxRendererContext ctx);

    public NTxBounds2D bgBounds(NTxNode p, NTxRendererContext ctx) {
        return selfBounds2D(ctx);
    }

    public NTxBounds2D selfBounds2D(NTxRendererContext ctx) {
        NTxDouble2 size = defaultSelfBounds(ctx).size();
        return NTxValueByName.selfBounds2D(size, null, ctx);
    }

    public NTxBounds3D selfBounds3D(NTxRendererContext ctx) {
        NTxDouble3 size = defaultSelfBounds3D(ctx).size();
        return NTxValueByName.selfBounds3D(size, null, ctx);
    }

    public NTxBounds2D defaultSelfBounds(NTxRendererContext ctx) {
        return NTxValueByName.defaultSelfBounds2D(ctx);
    }

    public NTxBounds3D defaultSelfBounds3D(NTxRendererContext ctx) {
        return NTxValueByName.defaultSelfBounds3D(ctx);
    }

}
