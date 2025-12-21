package net.thevpc.ntexup.engine.renderer.text;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererFlavor;
import net.thevpc.ntexup.api.renderer.text.NTxTextOptions;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererBuilder;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.api.util.NtxFontInfo;
import net.thevpc.ntexup.engine.util.NTxUtilsText;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.text.NMsg;

import java.awt.*;

public class NTxTextRendererBase extends NTxTextBaseRenderer {
    private String flavor;

    public NTxTextRendererBase(String type, String flavor) {
        super(type);
        this.flavor = flavor;
    }

    public NTxTextRendererBuilder createRichTextHelper(NTxNodeRendererContext ctx) {
        NTxTextRendererFlavor f = ctx.engine().textRendererFlavor(flavor).orNull();
        if (f == null) {
            ctx.log().log(NMsg.ofC("TextRendererFlavor not found %s", flavor));
            f = ctx.engine().textRendererFlavor("text").get();
        }
        NTxNode node = ctx.node();
        ctx = ctx.withDefaultStyles(defaultStyles);
        String text = resolveStringOrFileOr(node, node.getPropertyValue(NTxPropName.VALUE).orNull(), node.getPropertyValue(NTxPropName.FILE).orNull(), "", ctx);

        Paint fg = NTxValueByName.getForegroundColor(node, ctx, true);
        NtxFontInfo font = NTxValueByName.getFontInfo(node, ctx);
        if (font.baseFont == null && NBlankable.isBlank(font.family)) {
            font.baseFont = ctx.graphics().getFont();
        }
        NTxTextRendererBuilderImpl builder = new NTxTextRendererBuilderImpl(ctx.engine(), fg, font);
        f.buildText(text, new NTxTextOptions(), node, ctx, builder);
        return builder;
    }

    public String resolveStringOrFileOr(NTxNode node, NElement str, NElement file, String defaultValue, NTxNodeRendererContext ctx) {
        if (str != null) {
            NElement vElemValue = ctx.evalExpression(str, node).orNull();
            return NTxUtilsText.trimBloc(NTxValue.of(vElemValue).asStringOrName().orElse(""));
        } else {
            NElement vElemValue = ctx.evalExpression(file, node).orNull();
            NPath nPath = ctx.engine().resolvePath(vElemValue, node);
            if (nPath != null) {
                ctx.sourceMonitor().add(nPath);
                if (nPath.isRegularFile()) {
                    try {
                        return nPath.readString().trim();
                    } catch (Exception e) {
                        ctx.log().log(NMsg.ofC("unable to read path %s : %s", nPath, e).asError(), NTxUtils.sourceOf(node));
                    }
                }
            }
            return defaultValue;
        }
    }

}
