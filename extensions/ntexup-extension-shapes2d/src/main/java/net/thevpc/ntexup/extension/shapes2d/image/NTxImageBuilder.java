/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.thevpc.ntexup.extension.shapes2d.image;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.elem2d.NTxImageOptions;
import net.thevpc.ntexup.api.document.elem2d.NTxSize;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.eval.NTxValueByType;
import net.thevpc.ntexup.api.extension.NTxNodeBuilder;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.renderer.text.NTxTextOptions;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.api.util.NtxFontInfo;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;

import java.awt.*;


/**
 * @author vpc
 */
public class NTxImageBuilder implements NTxNodeBuilder {
    NTxProperties defaultStyles = new NTxProperties();

    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
                .id(NTxNodeType.IMAGE)
                .parseParam().matchesNamedPair(NTxPropName.TRANSPARENT_COLOR).then()
                .parseParam().matchesNamedPair(NTxPropName.VALUE, NTxPropName.FILE, "content", "src").storeName(NTxPropName.VALUE).then()
                .parseParam().matchesStringOrName().storeName(NTxPropName.VALUE).ignoreDuplicates(true).then()
                .renderComponent((rendererContext, builderContext1) -> renderMain(rendererContext, builderContext1))
        ;
    }
    static class Cache {
        NPath img=null;
        NTxImageOptions options;
    }
    public void renderMain(NTxNodeRendererContext rendererContext, NTxNodeBuilderContext builderContext) {
        NTxNode node = rendererContext.node();
        rendererContext = rendererContext.withDefaultStyles(defaultStyles);
        NTxBounds2 b = rendererContext.selfBounds();
        int w = NTxUtils.intOf(b.getWidth());
        int h = NTxUtils.intOf(b.getHeight());
        if (w <= 0 || h <= 0) {
            return;
        }

        NTxNodeRendererContext finalCtx = rendererContext;
        Cache cache = node.getAndSetRenderCache(Cache.class,rendererContext.isSomeChange(),()->{
            Cache cc=new Cache();
            Color transparentColor = NTxValueByType.getColor(node, finalCtx, NTxPropName.TRANSPARENT_COLOR).orNull();
            cc.options = new NTxImageOptions();
            cc.options.setTransparentColor(transparentColor);
            cc.options.setDisableAnimation(!finalCtx.isAnimate());
            cc.options.setAsyncLoad(() -> finalCtx.repaint());
            cc.options.setImageObserver(finalCtx.imageObserver());
            cc.options.setSize(new Dimension(b.getWidth().intValue(), b.getHeight().intValue()));
            NElement eimg = node.getPropertyValue(NTxPropName.VALUE).orNull();
            if(eimg!=null){
                NElement eimg2 = finalCtx.engine().evalExpression(eimg, node, finalCtx.varProvider());
                cc.img = finalCtx.engine().resolvePath(eimg2, node);
                cc.img = resolveImagePath(cc.img);
            }
            return cc;
        }).get();



        NTxGraphics g = rendererContext.graphics();

        double x = b.getX();
        double y = b.getY();

        if (!rendererContext.isDry()) {
            if (rendererContext.applyBackgroundColor(node)) {
                g.fillRect((int) x, (int) y, NTxUtils.intOf(b.getWidth()), NTxUtils.intOf(b.getHeight()));
            }

            rendererContext.applyForeground(node, false);
            if (cache.img instanceof NPath) {
                try {
                    g.drawImage(cache.img, x, y, cache.options);
                } catch (Exception ex) {
                    NTxSource src = NTxUtils.sourceOf(node);
                    rendererContext.log().log(NMsg.ofC("[%s] [ERROR] error loading image : %s (%s)",
                            src == null ? null : src.shortName(),
                            cache.img, ex).asSevere(), src);

                }
            }else{
                int descent = g.getFontMetrics().getAscent();
                NTxTextOptions nTxTextOptions = new NTxTextOptions();
                NtxFontInfo fontInfo = NTxValueByName.getFontInfo(node, rendererContext);
                nTxTextOptions.defaultFont=fontInfo;
                nTxTextOptions.sr=rendererContext.sizeRef();
                g.drawString("Image not found "+cache.img, x, y+descent, nTxTextOptions.setForegroundColor(Color.YELLOW).setBackgroundColor(Color.RED).setFontSize(NTxSize.ofPx(8.0f)));
                NTxSource src = NTxUtils.sourceOf(node);
                rendererContext.log().log(NMsg.ofC("[%s] [ERROR] image not found : %s",
                        src == null ? null : src.shortName(),
                        cache.img).asSevere(), src);
            }
        }
        rendererContext.drawContour();
    }

    private NPath resolveImagePath(NPath p) {
        if (p.isRegularFile()) {
            return p;
        }
        for (String ext : new String[]{"png", "jpg", "gif", "jpeg","svg"}) {
            String n = p.getName();
            String nLowered = n.toLowerCase();
            if (nLowered.endsWith(".")) {
                NPath p2 = p.resolveSibling(n + ext);
                if (p2.isRegularFile()) {
                    return p2;
                }
                p2 = p.resolveSibling(n + ext.toUpperCase());
                if (p2.isRegularFile()) {
                    return p2;
                }
            } else if (nLowered.endsWith(".*")) {
                NPath p2 = p.resolveSibling(n.substring(0, n.length() - 1) + ext);
                if (p2.isRegularFile()) {
                    return p2;
                }
                p2 = p.resolveSibling(n.substring(0, n.length() - 1) + ext.toUpperCase());
                if (p2.isRegularFile()) {
                    return p2;
                }
            } else {
                NPath p2 = p.resolveSibling(n + "." + ext);
                if (p2.isRegularFile()) {
                    return p2;
                }
                p2 = p.resolveSibling(n + "." + ext.toUpperCase());
                if (p2.isRegularFile()) {
                    return p2;
                }
            }
        }
        return null;

    }


}
