package net.thevpc.ntexup.engine.renderer.text;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererFlavor;
import net.thevpc.ntexup.api.renderer.text.NTxTextOptions;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererBuilder;
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

    public NTxTextRendererBuilder createRichTextHelper(NTxRendererContext context) {
        NTxTextRendererFlavor f = context.engine().textRendererFlavor(flavor).orNull();
        if (f == null) {
            context.log(NMsg.ofC("TextRendererFlavor not found %s", flavor));
            f = context.engine().textRendererFlavor("text").get();
        }
        NTxNode node = context.node();
        context = context.withDefaultStyles(defaultStyles);
        String text = resolveStringOrFileOr(node.getPropertyValue(NTxPropName.VALUE).orNull(), node.getPropertyValue(NTxPropName.FILE).orNull(), "", context);

        Paint fg = NTxValueByName.getForegroundColor(context, true);
        NtxFontInfo font = NTxValueByName.getFontInfo(context);
        if (font.baseFont == null && NBlankable.isBlank(font.family)) {
            font.baseFont = context.graphics().getFont();
        }
        NTxTextRendererBuilderImpl builder = new NTxTextRendererBuilderImpl(context.engine(), fg, font);
        f.buildText(text, new NTxTextOptions(), context, builder);
        return builder;
    }

    public String resolveStringOrFileOr(NElement str, NElement file, String defaultValue, NTxRendererContext ctx) {
        if (str != null) {
            NElement vElemValue = ctx.evalExpression(str).orNull();
            return NTxUtilsText.trimBloc(NTxValue.of(vElemValue).asStringOrName().orElse(""));
        } else {
            NElement vElemValue = ctx.evalExpression(file).orNull();
            NPath nPath = ctx.resolvePath(vElemValue);
            if (nPath != null) {
                ctx.sourceMonitor().add(nPath);
                if (nPath.isRegularFile()) {
                    try {
                        return nPath.readString().trim();
                    } catch (Exception e) {
                        ctx.log(NMsg.ofC("unable to read path %s : %s", nPath, e).asError(), ctx.source());
                    }
                }
            }
            return defaultValue;
        }
    }

}
