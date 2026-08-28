package net.thevpc.ntexup.engine.renderer.text;

import net.thevpc.ntexup.api.document.elem2d.NTxSize;
import net.thevpc.ntexup.api.renderer.text.*;
import net.thevpc.ntexup.api.util.NTxColors;
import net.thevpc.ntexup.engine.util.NTxNodeRendererUtils;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.nuts.text.*;
import net.thevpc.nuts.util.NStringUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NTxHighlighterMapper {
    public static void highlightNutsText(String lang, String rawText, NText parsedText, NTxRendererContext ctx, NTxTextRendererBuilder result) {
        Map<String, NTxTextPartStyle> cache = new HashMap<>();
        result.setLang(lang);
        result.setCode(rawText);
        NTxGraphics g = ctx.graphics();
        NTxNodeRendererUtils.applyFont(g, ctx);
        //String[] allLines = code.trim().split("[\n]");
        NTextTransformConfig nTextTransformConfig = new NTextTransformConfig();
        nTextTransformConfig.flatten(true);
        nTextTransformConfig.normalize(true);
        nTextTransformConfig.processTitleNumbers(true);
        processNTextRecursively(parsedText.normalize(nTextTransformConfig), result, ctx, new NTextStyle[0], cache);
        result.computeBound(ctx);
    }

    private static void applyOptions(NTxTextOptions to, NTextStyle nTextStyle, NTxRendererContext ctx, Map<String, NTxTextPartStyle> cache) {
        switch (nTextStyle.type()) {
            case BOLD: {
                to.setBold(true);
                break;
            }
            case ITALIC: {
                to.setItalic(true);
                break;
            }
            case UNDERLINED: {
                to.underlined = true;
                break;
            }
            case STRIKED: {
                to.strikeThrough = true;
                break;
            }

            case FORE_TRUE_COLOR: {
                to.foregroundColor = new Color(nTextStyle.variant());
                break;
            }
            case BACK_TRUE_COLOR: {
                to.backgroundColor = new Color(nTextStyle.variant());
                break;
            }
            case FORE_COLOR: {
                to.foregroundColor = NTxColors.resolveDefaultColorByIndex(nTextStyle.variant(),null, ctx);
                break;
            }
            case BACK_COLOR: {
                to.backgroundColor = NTxColors.resolveDefaultColorByIndex(nTextStyle.variant(),null, ctx);
                break;
            }
            case PLAIN: {
                break;
            }
            case BLINK: {
                //should we add timer??
                break;
            }
            case BOOLEAN:
            case COMMENTS:
            case CONFIG:
            case DANGER:
            case DATE:
            case ERROR:
            case FAIL:
            case INFO:
            case INPUT:
            case KEYWORD:
            case NUMBER:
            case OPERATOR:
            case OPTION:
            case PALE:
            case PATH:
            case PRIMARY:
            case REVERSED:
            case SECONDARY:
            case SEPARATOR:
            case STRING:
            case SUCCESS:
            case TITLE:
            case VAR:
            case VERSION:
            case WARN: {
                NTxTextPartStyle ss = resolveCodeStyle(nTextStyle, ctx, cache);
                if (ss.foreground != null) {
                    to.foregroundColor = ss.foreground;
                }
                if (ss.background != null) {
                    to.backgroundColor = ss.background;
                }
                if (ss.bold) {
                    to.setBold(true);
                }
                if (ss.italic) {
                    to.setItalic(true);
                }
                if (ss.fontSize!=null) {
                    to.setFontSize(ss.fontSize);
                }
                to.setFontFamily(ss.fontFamily);
                break;
            }

        }
    }

    private static void processNTextRecursively(NNormalizedText nText, NTxTextRendererBuilder result, NTxRendererContext ctx, NTextStyle[] styles, Map<String, NTxTextPartStyle> cache) {
        NTxGraphics g = ctx.graphics();
        if(styles==null){
            styles=new NTextStyle[0];
        }
        switch (nText.type()) {
            case PLAIN: {
                if (result.isEmpty()) {
                    result.nextLine();
                }
                NTextPlain np = (NTextPlain) nText;
                if (np.value().equals("\n")) {
                    result.nextLine();
                } else {
                    if (styles.length==0) {
                        result.currRow();
                        NTxRichTextToken col = new NTxRichTextToken(NTxRichTextTokenType.PLAIN, np.value());
                        col.tok = nText;
                        //g.setFont(col.textOptions.font);
                        col.bounds = g.getStringBounds(col.text);
                        result.addToken(col);
                    } else {
                        result.currRow();
                        NTxRichTextToken col = new NTxRichTextToken(NTxRichTextTokenType.STYLED, np.value());
                        col.tok = nText;
                        //g.setFont(col.textOptions.font);
                        col.bounds = g.getStringBounds(col.text);
                        for (NTextStyle nTextStyle : styles) {
                            applyOptions(col.textOptions, nTextStyle, ctx, cache);
                        }
                        result.addToken(col);
                    }
                }
                break;
            }
            case STYLED:{
                NTextStyled ss=(NTextStyled)nText;
                List<NTextStyle> newStyles=new ArrayList<>(Arrays.asList(styles));
                newStyles.addAll(ss.styles().toList());
                processNTextRecursively((NNormalizedText) ss.child(), result, ctx,newStyles.toArray(new NTextStyle[0]), cache);
                break;
            }
            case LIST:{
                NTextList list = (NTextList) nText;
                for (NText nt : list.children()) {
                    processNTextRecursively((NNormalizedText)nt, result, ctx,styles, cache);
                }
                break;
            }
            default:{
                throw new IllegalArgumentException("Unsupported text type: " + nText.type());
            }
        }
    }

    private static NTxTextPartStyle resolveCodeStyle(NTextStyle nTextStyle, NTxRendererContext ctx, Map<String, NTxTextPartStyle> cache) {
        String styleTypeId = nTextStyle.type().id();
        String prefix = "source-" + styleTypeId + "-";
        NTxTextPartStyle ss = cache.get(nTextStyle.id());
        if (ss != null) {
            return ss;
        }
        ss = new NTxTextPartStyle();
        {
            NTxValue e = NTxValue.of(ctx.computePropertyValue(prefix + "color").orNull());
            Color[] colors = e.asColorArrayOrColor().orNull();
            ss.foreground = NTxColors.resolveDefaultColorByIndex(nTextStyle.variant(), colors, ctx);
        }
        {
            NTxValue e = NTxValue.of(ctx.computePropertyValue(prefix + "background").orNull());
            Color[] colors = e.asColorArray().orNull();
            if (colors == null || colors.length == 0) {
                // od nothing
            } else {
                int i = nTextStyle.variant() % colors.length;
                ss.background = colors[i];
            }
        }
        {
            NTxValue e = NTxValue.of(
                    ctx.computePropertyValue(prefix + "font-family")
                            .orElseGetOptionalFrom(() -> ctx.computePropertyValue("font-family"))
                            .orNull()
            );
            String value = NStringUtils.stripToNull(e.asStringOrName().orNull());
            if (value == null) {
                // od nothing
            } else {
                ss.fontFamily = value;
            }
        }
        {
            NTxSize e = NTxSize.ofElement(
                    ctx.computePropertyValue(prefix + "font-family-size")
                            .orElseGetOptionalFrom(() -> ctx.computePropertyValue("font-family-size"))
                            .orNull()
            );
            if (e == null) {
                // od nothing
            } else {
                ss.fontSize = e;
            }
        }
        {
            ss.bold = NTxValue.of(ctx.computePropertyValue(prefix + "font-bold")
                    .orElseGetOptionalFrom(() -> ctx.computePropertyValue("font-bold"))
                    .orNull()).asBoolean().orElse(false);

            ss.italic = NTxValue.of(ctx.computePropertyValue(prefix + "font-italic")
                    .orElseGetOptionalFrom(() -> ctx.computePropertyValue("font-italic"))
                    .orNull()).asBoolean().orElse(false);
        }
        cache.put(nTextStyle.id(), ss);
        return ss;
    }


    protected static List<NTextPlain> toNTextPlains(NText a) {
        if (a instanceof NTextStyled) {
            return toNTextPlains(((NTextStyled) a).child());
        }
        if (a instanceof NTextPlain) {
            return Arrays.asList((NTextPlain) a);
        }
        if (a instanceof NTextList) {
            ArrayList<NTextPlain> objects = new ArrayList<>();
            NTextList list = (NTextList) a;
            for (NText nText : list) {
                objects.addAll(toNTextPlains(nText));
            }
            return objects;
        }
        return new ArrayList<>();
    }

}
