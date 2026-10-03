package net.thevpc.ntexup.engine.base.nodes.text;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.renderer.text.*;
import net.thevpc.ntexup.api.util.NTxColors;
import net.thevpc.nuts.collections.NCharQueue;
import net.thevpc.nuts.spi.NCodeHighlighter;
import net.thevpc.nuts.text.*;
import net.thevpc.nuts.util.NStringUtils;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Parses an ntexup rich text into a flat list of {@link NTxTextToken}.
 *
 * <p>Text markup ({@code **bold**}, {@code *italic*}, {@code ##:style:text##},
 * {@code ###text###}, {@code ```verbatim```}, {@code \escapes}, ...) is delegated to the
 * {@code MTF} parser of nuts through {@link NCodeHighlighter#of(String)}. Languages are
 * <b>not</b> delegated: ntexup resolves {@code [[lang: source]]} and the flavor specific
 * inline separators (such as {@code \(}...{@code \)}) <b>before</b> handing the remaining
 * text over to nuts, because a language may be rendered as an image (as done by the
 * {@code eq}/{@code latex-equation} flavor) which nuts knows nothing about.
 *
 * <p>A language region is replaced in the text given to nuts by an inert private use
 * placeholder, so that markup may span a language (as in {@code *italic [[eq: x]] italic*}).
 * Placeholders are restored while converting the nuts {@link NText} tree back to tokens.
 * A language that no ntexup flavor supports is kept as plain text, brackets included.
 *
 * <p>When the {@code mtf} code highlighter is not available (nuts-runtime is resolved at
 * runtime by the nuts boot class loader and may be missing), the legacy hand written
 * tokenizer is used as a fallback.
 */
class NTxTextTokenParseHelper {

    private static final String MTF_FORMAT = "mtf";

    private List<NTxTextRendererFlavor> flavors;
    private NTxNodeBuilderContext builderContext;
    private NTxTextRendererFlavorParseContext parseContext;
    private Set<String> parsePrefixes;
    private int parsePrefixesMaxLength;
    private NTxRendererContext rendererContext;
    private boolean warnedMissingMtf;
    private NTextTransformConfig nconfig = new NTextTransformConfig().applyTheme(true).normalize(true).basicTrueStyles(true);

    public NTxTextTokenParseHelper(NTxRendererContext rendererContext, NCharQueue cq, NTxNodeBuilderContext builderContext) {
        this.rendererContext = rendererContext;
        this.flavors = rendererContext.engine().textRendererFlavors();
        this.builderContext = builderContext;
        this.parseContext = new MyNTxTextRendererFlavorParseContext(rendererContext, cq);
        init();
    }

    public NTxTextTokenParseHelper(NTxTextRendererFlavorParseContext parseContext, NTxNodeBuilderContext builderContext) {
        rendererContext = parseContext.rendererContext();
        this.flavors = parseContext.rendererContext().engine().textRendererFlavors();
        this.builderContext = builderContext;
        this.parseContext = parseContext;
        init();
    }

    private void init() {
        parsePrefixes = new HashSet<>();
        for (NTxTextRendererFlavor flavor : flavors) {
            for (String p : flavor.parsePrefixes()) {
                int ln = p.length();
                if (ln > 0) {
                    parsePrefixes.add(p);
                    if (ln > parsePrefixesMaxLength) {
                        parsePrefixesMaxLength = ln;
                    }
                }
            }
        }
    }

    public NTxTextRendererFlavor resolveFlavor(String s) {
        for (NTxTextRendererFlavor flavor : flavors) {
            for (String p : flavor.parsePrefixes()) {
                int ln = p.length();
                if (ln > 0) {
                    if (p.equals(s)) {
                        return flavor;
                    }
                    if(("[["+s+":").equals(p)){
                        return flavor;
                    }
                }
            }
        }
        return null;
    }

    public List<NTxTextToken> parse() {
        NCodeHighlighter mtf = mtfHighlighter();
        if (mtf == null) {
            return parseLegacy();
        }
        StringBuilder sb = new StringBuilder();
        while (parseContext.hasNext()) {
            String r = parseContext.read(1024);
            if (r != null) {
                sb.append(r);
            }
        }
        List<NTxTextToken> tokens = new ArrayList<>();
        NText nt = mtf.stringToText(sb.toString());
        collect(nt, new NTxTextOptions(), tokens);
        return tokens;
    }

    /**
     * The nuts {@code mtf} highlighter, or {@code null} if nuts-runtime is unavailable.
     */
    private NCodeHighlighter mtfHighlighter() {
        try {
            return NCodeHighlighter.of(MTF_FORMAT);
        } catch (Throwable e) {
            if (!warnedMissingMtf) {
                warnedMissingMtf = true;
                if (rendererContext != null) {
                    rendererContext.log(NMsg.ofC("mtf code highlighter is not available, using the legacy ntexup text parser"), rendererContext.source());
                }
            }
            return null;
        }
    }


    /**
     * Converts the nuts {@link NText} tree into tokens, restoring language placeholders.
     */
    private void collect(NText node, NTxTextOptions options, List<NTxTextToken> tokens) {
        if (node == null) {
            return;
        }
        if (node instanceof NTextList) {
            for (NText child : ((NTextList) node).children()) {
                collect(child, options, tokens);
            }
            return;
        }
        if (node instanceof NTextStyled) {
            NNormalizedText normalized = node.normalize(nconfig);
            if(normalized instanceof NTextStyled) {
                NTextStyled styled = (NTextStyled) normalized;
                NTxTextOptions o = options.copy();
                applyStyles(styled.styles(), o);
                collect(styled.child(), o, tokens);
            }else{
                collect(normalized, options, tokens);
            }
            return;
        }
        if (node instanceof NTextTitle) {
            options=options.copy();
            options.foregroundColor = NTxColors.resolveDefaultColorByIndex(((NTextTitle) node).level(), null, rendererContext);
            options.fontSizeMultiplier = new NTxPoint(1.2,1.2,true);
            collect(((NTextTitle) node).child(), options, tokens);
            return;
        }
        if (node instanceof NTextAnchor) {
            collect(node.normalize(nconfig), options, tokens);
            return;
        }
        if (node instanceof NTextBuilder) {
            collect(((NTextBuilder) node).build(), options, tokens);
            return;
        }
        if (node instanceof NTextCmd) {
            collect(node.normalize(nconfig), options, tokens);
            return;
        }
        if (node instanceof NTextCode) {
            // keep the fenced form so that no content is lost
            NTextCode nt = (NTextCode) node;
            String value = nt.value();
            if (nt.start().equals("`")) {
                NTxTextTokenText t = new NTxTextTokenText(value);
                t.options().foregroundColor = NTxColors.resolveDefaultColorByIndex(3, null, rendererContext);
                tokens.add(t);
                return;
            }
            if (nt.start().length() >= 3 && isRepeated(nt.start())) {
                String q = nt.qualifier();
                NTxTextRendererFlavor f = resolveFlavor(q);
                if (f != null) {
                    tokens.addAll(f.parseTokens(parseContext.withText(
                            f.parsePrefixes().get(0)
                            +value
                            +f.parseSuffix(f.parsePrefixes().get(0))

                    )));
                    return;
                }
            }
            collect(nt.highlight(), options, tokens);
            return;
        }
        if (node instanceof NTextPlain) {
            NTxTextTokenText e = new NTxTextTokenText(((NTextPlain) node).value());
            e.options().copyNonNullFrom(options);
            tokens.add(e);
            return;
        }
        tokens.add(new NTxTextTokenText(node.filteredText()));
    }

    private boolean isRepeated(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != s.charAt(0)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Maps the nuts text styles ntexup can render. Styles that have no ntexup counterpart
     * are ignored, their text is still kept.
     */
    private void applyStyles(NTextStyles styles, NTxTextOptions options) {
        for (NTextStyle style : styles) {
            switch (style.type()) {
                case BOLD:
                    options.bold = true;
                    break;
                case ITALIC:
                    options.italic = true;
                    break;
                case UNDERLINED:
                    options.underlined = true;
                    break;
                case STRIKED:
                    options.strikeThrough = true;
                    break;
                case FORE_COLOR:
                    options.foregroundColor = NTxColors.resolveDefaultColorByIndex(style.variant(), null, rendererContext);
                    break;
                case FORE_TRUE_COLOR:
                    options.foregroundColor = new Color(style.variant());
                    break;
                case BACK_COLOR:
                    options.backgroundColor = NTxColors.resolveDefaultColorByIndex(style.variant(), null, rendererContext);
                    break;
                case BACK_TRUE_COLOR:
                    options.backgroundColor = new Color(style.variant());
                    break;
                default:
                    break;
            }
        }
    }


    List<NTxTextToken> readSpecial() {
        String bcId = builderContext == null ? null : builderContext.id();
        for (NTxTextRendererFlavor flavor : flavors) {
            if (!Objects.equals(flavor.type(), bcId)) {
                List<NTxTextToken> image = flavor.parseTokens(parseContext);
                if (image != null) {
                    return image;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // legacy tokenizer, used when the mtf code highlighter is not available
    // ------------------------------------------------------------------

    private List<NTxTextToken> parseLegacy() {
        List<NTxTextToken> all = new ArrayList<>();
        while (parseContext.hasNext()) {
            List<NTxTextToken> a = readAny();
            all.addAll(a);
        }
        return all;
    }

    private List<NTxTextToken> readAny() {
        if (parseContext.hasNext()) {
            List<NTxTextToken> s = readSpecial();
            if (s != null) {
                return s;
            }

            //this is not special now
            // so skip flavor prefixes
            if (parsePrefixesMaxLength > 0) {
                String p2 = parseContext.peek(parsePrefixesMaxLength);
                if (p2 != null) {
                    for (String p : parsePrefixes) {
                        if (p2.startsWith(p)) {
                            parseContext.read(p.length());
                            return readPlain(p);
                        }
                    }
                }
            }
            if (parseContext.peek(2).equals("[[")) {
                return readPlain(parseContext.read(2));
            }
            switch (parseContext.peek()) {
                case '*': {
                    if (parseContext.peek(3).equals("***")) {
                        return readBoldItalic();
                    } else if (parseContext.peek(2).equals("**")) {
                        return readBold();
                    } else {
                        return readPlain("");
                    }
                }
                case '#': {
                    for (int i = 10; i >= 2; i--) {
                        String r = NStringUtils.repeat('#', i);
                        if (parseContext.peek(i).equals(r)) {
                            return readColor(i);
                        }
                    }
                    if (parseContext.peek(1).equals("#")) {
                        return readPlain(parseContext.read(1));
                    }
                    break;
                }
                case '`': {
                    return readBackTick();
                }
                case '_': {
                    if (parseContext.peek(2).equals("__")) {
                        return readItalic();
                    } else {
                        return readPlain("");
                    }
                }
                default: {
                    return readPlain("");
                }
            }
        }
        return Collections.emptyList();
    }

    private List<NTxTextToken> readBold() {
        return readBounded("**", new String[]{"***"}, tt -> {
            NTxTextOptions o = tt.options();
            if (o.bold == null) {
                o.bold = true;
            }
        });
    }

    private List<NTxTextToken> readItalic() {
        return readBounded("__", tt -> {
            NTxTextOptions o = tt.options();
            if (o.italic == null) {
                o.italic = true;
            }
        });
    }


    private List<NTxTextToken> readBoldItalic() {
        return readBounded("***", tt -> {
            NTxTextOptions o = tt.options();
            if (o.bold == null) {
                o.bold = true;
            }
            if (o.italic == null) {
                o.italic = true;
            }
        });
    }

    private List<NTxTextToken> readColor(int col) {
        String bounds = NStringUtils.repeat('#', col);
        return readBounded(bounds, new String[]{bounds + "#"}, tt -> {
            NTxTextOptions o = tt.options();
            if (o.foregroundColor == null) {
                o.foregroundColor = NTxColors.resolveDefaultColorByIndex(col, null, rendererContext);
            }
        });
    }

    private List<NTxTextToken> readBackTick() {
        return readBounded("`", tt -> {
            NTxTextOptions o = tt.options();
            if (o.foregroundColor == null) {
                o.foregroundColor = NTxColors.resolveDefaultColorByIndex(3, null, rendererContext);
            }
        });
    }

    private List<NTxTextToken> readBounded(String bounds, Consumer<NTxTextToken> a) {
        return readBounded(bounds, new String[0], a);
    }

    private List<NTxTextToken> readBounded(String bounds, String[] subs, Consumer<NTxTextToken> a) {
        if (!parseContext.read(bounds.length()).equals(bounds)) {
            throw new IllegalArgumentException("expected " + bounds.length());
        }
        List<NTxTextToken> sbs = new ArrayList<>();
        while (parseContext.hasNext()) {
            boolean subbed = false;
            for (String sb : subs) {
                if (parseContext.peek(sb.length()).equals(sb)) {
                    List<NTxTextToken> u = readAny();
                    for (NTxTextToken tt : u) {
                        a.accept(tt);
                        sbs.add(tt);
                    }
                    subbed = true;
                    break;
                }
            }
            if (!subbed) {
                if (parseContext.peek(bounds.length()).equals(bounds)) {
                    parseContext.skip(bounds.length());
                    break;
                } else {
                    List<NTxTextToken> u = readAny();
                    for (NTxTextToken tt : u) {
                        a.accept(tt);
                        sbs.add(tt);
                    }
                }
            }
        }
        return sbs;
    }


    private List<NTxTextToken> readPlain(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        boolean stop = false;
        while (!stop && parseContext.hasNext()) {
            String p3 = parseContext.peek(2);
            if (p3.equals("\\[[") || p3.equals("\\\\(") || p3.equals("\\##") || p3.equals("\\**") || p3.equals("\\__")) {
                sb.append(p3.substring(1));
                parseContext.read(3);
            } else {
                String p2 = parseContext.peek(2);
                if (p2.equals("[[") || p2.equals("\\(") || p2.equals("##") || p2.equals("**") || p2.equals("__")) {
                    stop = true;
                } else if (p2.startsWith("`")) {
                    stop = true;
                } else {
                    sb.append(parseContext.read());
                }
            }
        }
        if (sb.length() > 0) {
            return Arrays.asList(new NTxTextTokenText(sb.toString()));
        }
        return Collections.emptyList();
    }

}
