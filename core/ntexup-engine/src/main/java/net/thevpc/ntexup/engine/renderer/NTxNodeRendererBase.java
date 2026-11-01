package net.thevpc.ntexup.engine.renderer;

import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.NTxOverflow;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxFutureUtils;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxGeometryIndex;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRenderer;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.engine.util.NTx2DUtils0;
import net.thevpc.ntexup.engine.util.NTxNodeRendererUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public abstract class NTxNodeRendererBase implements NTxNodeRenderer {

    private String[] types;

    public NTxNodeRendererBase(String... types) {
        this.types = types;
    }

    @Override
    public NTxSizeRequirements sizeRequirements(NTxRendererContext ctx) {
        if (ctx.isLayoutNone()) {
            return new NTxSizeRequirements(0, 0, 0, 0, 0, 0);
        }
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
        NTxBounds2D selfBounds = rendererContext.selfBounds2D();
        NTxGeometryIndex gi = rendererContext.session().geometry();
        NTxGraphics nv = null;
        double rotationAngle = 0;
        double rotX = 0;
        double rotY = 0;
        boolean entered = false;
        boolean dry = rendererContext.isDry();
        try {
            if (!dry) {
                NTxRotation rotation = NTxValueByName.getRotation(rendererContext);
                if (rotation != null) {
                    double angle = NTxValue.of(rotation.getAngle()).asDouble().orElse(0.0);
                    if (angle != 0) {
                        angle = angle / 180.0 * Math.PI;
                        if (angle != 0) {
                            NTxGraphics g = rendererContext.graphics();
                            nv = g.copy();
                            rotX = NTxValue.of(rotation.getX()).asDouble().get() / 100.0 * selfBounds.widthX() + selfBounds.minX();
                            rotY = NTxValue.of(rotation.getY()).asDouble().get() / 100.0 * selfBounds.widthY() + selfBounds.minY();
                            if (rendererContext.isDebug()) {
                                g.setColor(NTxValueByName.getDebugColor(rendererContext));
                                g.drawRect(selfBounds);
                                g.fillRect(rotX - 3, rotY - 3, 6, 6);
                            }
                            nv.rotate(
                                    angle,
                                    rotX,
                                    rotY
                            );
                            rendererContext = rendererContext.withGraphics(nv);
                            rotationAngle = angle;
                        }
                    }
                }

                String name = nameOf(rendererContext);

                if (!v) {
                    gi.enter(node, selfBounds, rotationAngle, rotX, rotY, name);
                    entered = true;
                    gi.markHidden(node);
                    return;
                }

                gi.enter(node, selfBounds, rotationAngle, rotX, rotY, name);
                entered = true;

                NOptional<NTxProp> pendingProp = node.getProperty(NTxPropName.PENDING);
                if (pendingProp.isPresent() && !pendingProp.get().getValue().isNull()) {
                    NElement pe = pendingProp.get().getValue();
                    if (isWaitingOnBinding(pe)) {
                        if (renderPendingBinding(rendererContext, pe)) {
                            return;
                        }
                    } else if ((pe.isBoolean() && pe.asBooleanValue().orElse(false)) || !pe.isBoolean()) {
                        renderPending(rendererContext);
                        return;
                    }
                }

                NTxRendererContext paintContext = rendererContext;
                Consumer<NTxRendererContext> paintContent = (c) -> {
                    NOptional<NTxShadow> shadowOptional = NTxValueByName.readStyleAsShadow(NTxPropName.SHADOW, c);
                    if (shadowOptional.isPresent() && !shadowOptional.get().isBlank()) {
                        NTxShadow shadow = shadowOptional.get();
                        NTx2DUtils0.drawShadowed(c.graphics(), gg -> {
                            renderMain(c.withGraphics(gg));
                        }, c.globalBounds2D(), shadow);
                    } else {
                        renderMain(c);
                    }
                    NTxNodeRendererUtils.drawDebugBox(c, c.graphics(), c.selfBounds2D());
                    // MVC search highlights: whole-node spans (equations and
                    // other non-text content) are painted by the node renderer
                    // itself; no-op unless the host enabled the
                    // CAPABILITY_SEARCH_HIGHLIGHTS capability (screen viewer)
                    NTxHighlights.paintWholeNode(node, c);
                };

                NTxOverflow overflow = NTxValueByName.readStyleAsOverflow(rendererContext);
                if (overflow.isVisible()) {
                    paintContent.accept(paintContext);
                } else if (overflow.isFade()) {
                    paintWithFade(paintContext, overflow, rotationAngle, rotX, rotY, paintContent);
                } else {
                    Shape clipShape = overflow.clipShape(selfBounds);
                    if (rotationAngle != 0) {
                        // the graphics is already rotated; bring the clip back
                        // into the rotated space so the crop stays axis-aligned
                        // with the node's own (page-space) bounds
                        clipShape = AffineTransform
                                .getRotateInstance(-rotationAngle, rotX, rotY)
                                .createTransformedShape(clipShape);
                    }
                    paintContext.withinClip(clipShape, paintContent);
                }
            }
        } catch (Exception ex){
            rendererContext.engine().log().log(NMsg.ofC("unable to render %s : %s",node.type(),ex).asFineFail(ex));
        } finally {
            if (entered) {
                gi.exit(node);
            }
            if (nv != null) {
                nv.dispose();
            }
        }
    }

    /**
     * Paints the node through an offscreen ARGB layer so a soft alpha feather
     * ({@link NTxOverflow#isFade()}) can be applied at the crop-box edge. The
     * layer replicates the current page transform, so it works regardless of the
     * page scale. A rotated node falls back to a hard crop (a rotated feather is
     * ambiguous).
     */
    private void paintWithFade(NTxRendererContext ctx, NTxOverflow overflow,
                               double rotationAngle, double rotX, double rotY,
                               Consumer<NTxRendererContext> painter) {
        Rectangle2D crop = new Rectangle2D.Double();
        Shape s = overflow.clipShape(ctx.selfBounds2D());
        if (s instanceof Rectangle2D) {
            crop.setRect((Rectangle2D) s);
        } else {
            crop.setRect(s.getBounds2D());
        }
        if (rotationAngle != 0) {
            ctx.withinClip(AffineTransform
                    .getRotateInstance(-rotationAngle, rotX, rotY)
                    .createTransformedShape(crop), painter);
            return;
        }
        Graphics2D base = ctx.graphics().graphics2D();
        AffineTransform t = base.getTransform();
        Rectangle2D dev = t.createTransformedShape(crop).getBounds2D();
        int iw = (int) Math.ceil(dev.getWidth());
        int ih = (int) Math.ceil(dev.getHeight());
        if (iw <= 0 || ih <= 0) {
            painter.accept(ctx);
            return;
        }
        BufferedImage img = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ig = img.createGraphics();
        Point2D d0 = t.transform(new Point2D.Double(crop.getMinX(), crop.getMinY()), null);
        AffineTransform off = AffineTransform.getTranslateInstance(-d0.getX(), -d0.getY());
        off.concatenate(t);
        ig.setTransform(off);
        try {
            painter.accept(ctx.withGraphics(ctx.engine().createGraphics(ig)));
        } finally {
            ig.dispose();
        }
        double sx = crop.getWidth() == 0 ? 1 : dev.getWidth() / crop.getWidth();
        double sy = crop.getHeight() == 0 ? 1 : dev.getHeight() / crop.getHeight();
        applyFeatherMask(img,
                overflow.fadeLeft() * sx, overflow.fadeRight() * sx,
                overflow.fadeTop() * sy, overflow.fadeBottom() * sy);
        Graphics2D gg = ctx.graphics().graphics2D();
        AffineTransform save = gg.getTransform();
        try {
            gg.setTransform(new AffineTransform());
            gg.drawImage(img, (int) Math.round(dev.getX()), (int) Math.round(dev.getY()), null);
        } finally {
            gg.setTransform(save);
        }
    }

    /**
     * Multiplies the alpha of {@code img} by a rectangular feather mask: fully
     * opaque in the middle, ramping to transparent over the given pixel widths
     * at each edge. Implemented by successively compositing edge gradients with
     * {@link AlphaComposite#DST_IN} (so the effect is multiplicative on the
     * alpha channel only).
     */
    private static void applyFeatherMask(BufferedImage img,
                                         double left, double right,
                                         double top, double bottom) {
        int w = img.getWidth();
        int h = img.getHeight();
        int il = (int) Math.ceil(Math.max(0, left));
        int ir = (int) Math.ceil(Math.max(0, right));
        int it = (int) Math.ceil(Math.max(0, top));
        int ib = (int) Math.ceil(Math.max(0, bottom));
        if (il == 0 && ir == 0 && it == 0 && ib == 0) {
            return;
        }
        BufferedImage mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mg = mask.createGraphics();
        mg.setComposite(AlphaComposite.Src);
        mg.setColor(new Color(0, 0, 0, 255));
        mg.fillRect(0, 0, w, h);
        mg.setComposite(AlphaComposite.DstIn);
        Color clear = new Color(0, 0, 0, 0);
        Color solid = new Color(0, 0, 0, 255);
        if (it > 0) {
            mg.setPaint(new GradientPaint(0, 0, clear, 0, it, solid));
            mg.fillRect(0, 0, w, it);
        }
        if (ib > 0) {
            mg.setPaint(new GradientPaint(0, h, clear, 0, h - ib, solid));
            mg.fillRect(0, h - ib, w, ib);
        }
        if (il > 0) {
            mg.setPaint(new GradientPaint(0, 0, clear, il, 0, solid));
            mg.fillRect(0, 0, il, h);
        }
        if (ir > 0) {
            mg.setPaint(new GradientPaint(w, 0, clear, w - ir, 0, solid));
            mg.fillRect(w - ir, 0, ir, h);
        }
        mg.dispose();
        Graphics2D cg = img.createGraphics();
        cg.setComposite(AlphaComposite.DstIn);
        cg.drawImage(mask, 0, 0, null);
        cg.dispose();
    }

    /**
     * {@code pending: someResult} names the computation the node waits on, whereas
     * a boolean, string or object value is a static placeholder.
     */
    private boolean isWaitingOnBinding(NElement pendingValue) {
        return pendingValue.isName() && !pendingValue.isBoolean();
    }

    /**
     * @return true when the node is still waiting and its content must not be painted
     */
    private boolean renderPendingBinding(NTxRendererContext rendererContext, NElement pendingValue) {
        NElement value = rendererContext.evalExpression(pendingValue).orNull();
        boolean unbound = value == null || value.isNull() || value.equals(pendingValue);
        if (!unbound && !NTxFutureUtils.isFuture(value)) {
            return false;
        }
        if (!unbound && NTxFutureUtils.isReady(value)) {
            return false;
        }
        if (!rendererContext.isAnimate()) {
            if (!unbound) {
                NTxFutureUtils.await(value);
            }
            return false;
        }
        NTxBounds2D bounds = rendererContext.selfBounds2D();
        NTxGraphics g = rendererContext.graphics();
        g.setColor(new Color(245, 247, 250, 200));
        g.fillRect(bounds);
        g.setColor(new Color(200, 210, 220));
        g.drawRect(bounds);
        rendererContext.drawPendingLabel("Computing " + pendingValue.asStringValue().orElse("") + "...");
        return true;
    }

    public void renderPending(NTxRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        NOptional<NTxProp> pendingProp = node.getProperty(NTxPropName.PENDING);
        if (pendingProp.isPresent() && !pendingProp.get().getValue().isNull()) {
            NElement pe = pendingProp.get().getValue();
            if (pe.isString()) {
                NTxBounds2D bounds = rendererContext.selfBounds2D();
                NTxGraphics g = rendererContext.graphics();
                g.setColor(new Color(245, 247, 250));
                g.fillRect(bounds);
                g.setColor(new Color(200, 210, 220));
                g.drawRect(bounds);
                g.setColor(new Color(100, 115, 130));
                g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g.drawString(pe.asStringValue().get(), bounds.centerX() - 30, bounds.centerY());
                return;
            } else if (pe.isObject()) {
                rendererContext.renderDetachedNode(pe, rendererContext.selfBounds2D());
                return;
            }
        }
        NTxBounds2D bounds = rendererContext.selfBounds2D();
        NTxGraphics g = rendererContext.graphics();
        g.setColor(new Color(245, 247, 250, 200));
        g.fillRect(bounds);
        g.setColor(new Color(200, 210, 220));
        g.drawRect(bounds);
        g.setColor(new Color(120, 130, 140));
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("Loading...", bounds.centerX() - 25, bounds.centerY());
    }

    protected String nameOf(NTxRendererContext ctx) {
        try {
            NOptional<NTxProp> p = ctx.node().getProperty(NTxPropName.NAME);
            if (!p.isPresent()) {
                return null;
            }
            NElement v = ctx.evalExpression(p.get().getValue()).orElse(p.get().getValue());
            if (v == null) {
                return null;
            }
            String s = v.asStringValue().orNull();
            if (s == null) {
                return null;
            }
            s = s.trim();
            return s.isEmpty() ? null : s;
        } catch (Exception e) {
            return null;
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
