package net.thevpc.ntexup.engine.base.nodes.text;

import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.renderer.text.*;
import net.thevpc.nuts.internal.util.NReservedSimpleCharQueue;
import net.thevpc.nuts.util.NStringUtils;

import java.util.*;
import java.util.function.Consumer;

class NTxTextTokenParseHelper {
    private List<NTxTextRendererFlavor> flavors;
    private NTxNodeBuilderContext builderContext;
    private NTxTextRendererFlavorParseContext parseContext;
    private Set<String> parsePrefixes;
    private int parsePrefixesMaxLength;

    public NTxTextTokenParseHelper(NTxNodeRendererContext rendererContext, NReservedSimpleCharQueue cq, NTxNodeBuilderContext builderContext) {
        this.flavors = rendererContext.engine().textRendererFlavors();
        this.builderContext = builderContext;
        this.parseContext = new MyNTxTextRendererFlavorParseContext(rendererContext, cq);
        init();
    }

    public NTxTextTokenParseHelper(NTxTextRendererFlavorParseContext parseContext, NTxNodeBuilderContext builderContext) {
        this.flavors = parseContext.rendererContext().engine().textRendererFlavors();
        this.builderContext = builderContext;
        this.parseContext = parseContext;
        init();
    }

    private void init() {
        parsePrefixes = new HashSet<>();
        for (NTxTextRendererFlavor flavor : flavors) {
            for (String p : flavor.getParsePrefixes()) {
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

    List<NTxTextToken> readSpecial() {
        for (NTxTextRendererFlavor flavor : flavors) {
            if (!Objects.equals(flavor.type(), builderContext.id())) {
                List<NTxTextToken> image = flavor.parseTokens(parseContext);
                if (image != null) {
                    return image;
                }
            }
        }
        return null;
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
                    if(parseContext.peek(1).equals("#")){
                        return readPlain(parseContext.read(1));
                    }
                    break;
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
            if (o.foregroundColorIndex == null) {
                o.foregroundColorIndex = col-1;
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


    public List<NTxTextToken> parse() {
        List<NTxTextToken> all = new ArrayList<>();
        while (parseContext.hasNext()) {
            List<NTxTextToken> a = readAny();
            all.addAll(a);
        }
        return all;
    }

}
