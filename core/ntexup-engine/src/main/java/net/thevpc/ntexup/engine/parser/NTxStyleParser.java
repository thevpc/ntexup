package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.style.*;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.*;

import java.util.*;
import java.util.List;

import net.thevpc.nuts.text.NTextStyleType;

public class NTxStyleParser {

    //    static Map<String, HStyleValueParser> allStyleParsers = new HashMap<>();
    static Set<String> COMMON_STYLE_PROPS = new HashSet<>();
    static Set<String> COMMON_FLAG_STYLE_PROPS = new HashSet<>();

    static {
        COMMON_STYLE_PROPS.addAll(Arrays.asList(NTxPropName.STROKE,
                NTxPropName.SHADOW,
                NTxPropName.POSITION,
                NTxPropName.ORIGIN,
                NTxPropName.SIZE,
                NTxPropName.NAME,
                NTxPropName.COLUMNS,
                NTxPropName.ROWS,
                NTxPropName.COLSPAN,
                NTxPropName.COL_SPAN,
                NTxPropName.ROWSPAN,
                NTxPropName.ROW_SPAN,
                NTxPropName.COLWEIGHT,
                NTxPropName.COL_WEIGHT,
                NTxPropName.ROWWEIGHT,
                NTxPropName.ROW_WEIGHT,
                NTxPropName.GRID_COLOR,
                NTxPropName.LINE_COLOR,
                NTxPropName.ROTATE,
                NTxPropName.PADDING,
                NTxPropName.MARGIN,
                NTxPropName.FONT_SIZE,
                NTxPropName.DEBUG,
                NTxPropName.DEBUG_COLOR,
                NTxPropName.FONT_FAMILY,
                NTxPropName.RAISED,
                NTxPropName.FONT_BOLD,
                NTxPropName.FONT_ITALIC,
                NTxPropName.FONT_UNDERLINED,
                NTxPropName.FONT_STRIKE,
                NTxPropName.BACKGROUND_COLOR,
                NTxPropName.FOREGROUND_COLOR,
                NTxPropName.FILL_BACKGROUND,
                NTxPropName.HIDE,
                NTxPropName.DRAW_GRID,
                NTxPropName.COLUMNS_WEIGHT,
                NTxPropName.ROWS_WEIGHT,
                NTxPropName.PRESERVE_ASPECT_RATIO,
                NTxPropName.THREED,
                NTxPropName.DRAW_CONTOUR,
                NTxPropName.CLASS,
                NTxPropName.AT,
                NTxPropName.COMPONENT_NAME,
                NTxPropName.CONTENT_ORIGIN,
                NTxPropName.CONTENT_POSITION,
                NTxPropName.ALIGN,
                NTxPropName.WIDTH,
                NTxPropName.HEIGHT,
                NTxPropName.TEXT_WRAP,
                NTxPropName.WRAP,
                NTxPropName.TEXT_ALIGN,
                NTxPropName.TEXT_HALIGN,
                NTxPropName.BULLET_ALIGN
        ));

        COMMON_FLAG_STYLE_PROPS.addAll(Arrays.asList(
                NTxPropName.SHADOW,
                NTxPropName.DEBUG,
                NTxPropName.RAISED,
                NTxPropName.FONT_BOLD,
                NTxPropName.FONT_ITALIC,
                NTxPropName.FONT_UNDERLINED,
                NTxPropName.FONT_STRIKE,
                NTxPropName.FILL_BACKGROUND,
                NTxPropName.HIDE,
                NTxPropName.DRAW_GRID,
                NTxPropName.PRESERVE_ASPECT_RATIO,
                NTxPropName.THREED,
                NTxPropName.DRAW_CONTOUR
        ));

        for (NTextStyleType z : NTextStyleType.values()) {
            if (!z.basic()) {
                COMMON_STYLE_PROPS.addAll(Arrays.asList(new String[]{
                        "source-" + z.id() + "-color",
                        "source-" + z.id() + "-background",
                        "source-" + z.id() + "-font-family",
                        "source-" + z.id() + "-font-bold",
                        "source-" + z.id() + "-font-italic",
                        "source-" + z.id() + "-font-underlined",
                }));
            }
        }
        COMMON_STYLE_PROPS.addAll(
                Arrays.asList(
                        "color",
                        "background",
                        "foreground",
                        "bg",
                        "fg",
                        "show",
                        "visible",
                        "fill",
                        "contour",
                        "w",
                        "h"
                )
        );
        COMMON_FLAG_STYLE_PROPS.addAll(
                Arrays.asList(
                        "show",
                        "visible",
                        "fill",
                        "contour"
                )
        );
    }


    private static void parsePair(NPairElement pair, NTxResolutionContext context,
                                  List<String> types,
                                  List<String> names
    ) {
        NTxValue h = NTxValue.of(pair.key());
        NOptional<String> k = h.asStringOrName();
        if (k.isPresent()) {
            switch (NTxUtils.uid(k.get())) {
                case "name":
                case "names": {
                    NTxValue h2 = NTxValue.of(pair.value());
                    NOptional<String[]> cc = h2.asStringArrayOrString();
                    if (cc.isPresent()) {
                        names.addAll(Arrays.asList(cc.get()));
                    } else {
                        NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected a string or a string array.", NTxUtils.shortName(context.source()), pair).asSevere();
                        context.log(errMsg, context.source());
                    }
                    break;
                }
                case "type":
                case "types": {
                    NTxValue h2 = NTxValue.of(pair.value());
                    NOptional<String[]> cc = h2.asStringArrayOrString();
                    if (cc.isPresent()) {
                        types.addAll(Arrays.asList(cc.get()));
                    } else {
                        NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected a valid node type or array", NTxUtils.shortName(context.source()), pair).asSevere();
                        context.log(errMsg, context.source());
                    }
                    break;
                }
                default: {
                    NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected one of 'name' or 'type'", NTxUtils.shortName(context.source()), pair).asSevere();
                    context.log(errMsg, context.source());
                }
            }
        } else {
            NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected one of 'name' or 'type'", NTxUtils.shortName(context.source()), pair).asSevere();
            context.log(errMsg, context.source());
        }
    }

    private static NOptional<Integer> _asInt(NElement e, NTxResolutionContext context) {
        NOptional<Integer> r = NTxValue.of(e).asInt();
        if (r.isPresent()) {
            return r;
        }
        NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector argument %s. expected an integer", NTxUtils.shortName(context.source()), e).asSevere();
        context.log(errMsg, context.source());
        return NOptional.ofEmpty(errMsg);
    }

    /**
     * Dispatches selectors written as named elements with optional parameters:
     * <ul>
     *     <li>class-&lt;name&gt;[(base, ...)]  - class definition</li>
     *     <li>table-row(header|even|odd)</li>
     *     <li>table-column(n)</li>
     *     <li>table-cell(row: r, col: c)</li>
     *     <li>legacy: any other name is treated as a type selector, and bare
     *     params are treated as selector items (previous TUPLE behavior)</li>
     * </ul>
     */
    private static String namedSelectorName(NElement e) {
        NOptional<NNamedElement> nk = e.asNamed();
        if (nk.isPresent()) {
            String n = nk.get().name().orNull();
            if (n != null && !n.isEmpty()) {
                return n;
            }
        }
        return null;
    }

    private static List<NElement> selectorParams(NElement e) {
        List<NElement> params = new ArrayList<>();
        try {
            NOptional<NTupleElement> tu = e.asTuple();
            if (tu.isPresent()) {
                params.addAll(tu.get().children());
                return params;
            }
        } catch (Exception ex) {
            // not tuple-backed
        }
        NOptional<NParametrizedContainerElement> pc = e.asParametrizedContainer();
        if (pc.isPresent()) {
            params.addAll(pc.get().params().orElse(java.util.Collections.<NElement>emptyList()));
        }
        return params;
    }

    private static void parseNamedParamSelector(String name, List<NElement> params, NTxResolutionContext context, List<NTxStyleRuleSelectorItem> items) {
        String uid = NTxUtils.uid(name);
        if (uid.startsWith("class-")) {
            String clsName = uid.substring("class-".length());
            List<String> bases = new ArrayList<>();
            if (params != null) {
                for (NElement p : params) {
                    if (p.isNamedPair()) {
                        NPairElement pair = p.asNamedPair().get();
                        String k = NTxUtils.uid(NTxValue.of(pair.key()).asStringOrName().orElse(""));
                        if (k.equals("extends") || k.equals("bases")) {
                            NOptional<String[]> bb = NTxValue.of(pair.value()).asStringArrayOrString();
                            if (bb.isPresent()) {
                                for (String b : bb.get()) {
                                    bases.add(NTxUtils.uid(b));
                                }
                            } else {
                                NMsg errMsg = NMsg.ofC("[%s] invalid base class in class definition %s. expected a class name or an array of class names", NTxUtils.shortName(context.source()), p).asSevere();
                                context.log(errMsg, context.source());
                            }
                        } else {
                            NMsg errMsg = NMsg.ofC("[%s] invalid class definition %s. expected positional base class names or a 'extends:' named pair", NTxUtils.shortName(context.source()), p).asSevere();
                            context.log(errMsg, context.source());
                        }
                    } else {
                        NOptional<String> ps = NTxValue.of(p).asStringOrName();
                        if (ps.isPresent()) {
                            bases.add(NTxUtils.uid(ps.get()));
                        } else {
                            NMsg errMsg = NMsg.ofC("[%s] invalid base class %s in class definition %s. expected a class name", NTxUtils.shortName(context.source()), p, name).asSevere();
                            context.log(errMsg, context.source());
                        }
                    }
                }
            }
            items.add(NTxStyleRuleSelectorItem.ofClassDef(clsName, bases));
            return;
        }
        if (uid.equals("table-row")) {
            String kind = null;
            boolean dead = false;
            if (params != null) {
                for (NElement p : params) {
                    if (p.isNamedPair()) {
                        NPairElement pair = p.asNamedPair().get();
                        String k = NTxUtils.uid(NTxValue.of(pair.key()).asStringOrName().orElse(""));
                        String v = NTxUtils.uid(NTxValue.of(pair.value()).asStringOrName().orElse(""));
                        if (k.equals("row") || k.equals("r")) {
                            if (v.equals("header") || v.equals("even") || v.equals("odd")) {
                                kind = v;
                            } else {
                                NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. table-row(row:) accepts one of header, even or odd", NTxUtils.shortName(context.source()), name).asSevere();
                                context.log(errMsg, context.source());
                                dead = true;
                            }
                        } else {
                            NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. table-row accepts only row: {header, even, odd} or class-* usages", NTxUtils.shortName(context.source()), name).asSevere();
                            context.log(errMsg, context.source());
                            dead = true;
                        }
                    } else if (isClassUseWord(p)) {
                        addClassUseParam(p, items);
                    } else {
                        NOptional<String> word = NTxValue.of(p).asStringOrName();
                        if (word.isPresent()) {
                            String w = NTxUtils.uid(word.get());
                            if (w.equals("header") || w.equals("even") || w.equals("odd")) {
                                NMsg errMsg = NMsg.ofC("[%s] table-row(%s): positional form is deprecated; use table-row(row: %s)%s", NTxUtils.shortName(context.source()), w, w, w.equals("header") ? " or table-header" : "").asWarning();
                                context.log(errMsg, context.source());
                                kind = w;
                            } else {
                                NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. table-row accepts only row: {header, even, odd} or class-* usages", NTxUtils.shortName(context.source()), name).asSevere();
                                context.log(errMsg, context.source());
                                dead = true;
                            }
                        } else {
                            NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. table-row accepts only row: {header, even, odd} or class-* usages", NTxUtils.shortName(context.source()), name).asSevere();
                            context.log(errMsg, context.source());
                            dead = true;
                        }
                    }
                }
            }
            if (!dead) {
                items.add(NTxStyleRuleSelectorItem.ofTableRow(kind));
            }
            return;
        }
        if (uid.equals("table-column")) {
            Integer col = null;
            boolean dead = false;
            if (params == null || params.isEmpty()) {
                NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. table-column expects 'col:' with a 1-based column index", NTxUtils.shortName(context.source()), name).asSevere();
                context.log(errMsg, context.source());
                return;
            }
            for (NElement p : params) {
                if (p.isNamedPair()) {
                    NPairElement pair = p.asNamedPair().get();
                    String k = NTxUtils.uid(NTxValue.of(pair.key()).asStringOrName().orElse(""));
                    if (k.equals("col") || k.equals("c")) {
                        NOptional<Integer> v = _asInt(pair.value(), context);
                        if (v.isPresent()) {
                            col = v.get();
                        } else {
                            dead = true;
                        }
                    } else {
                        NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. table-column accepts only col: with a 1-based column index or class-* usages", NTxUtils.shortName(context.source()), name).asSevere();
                        context.log(errMsg, context.source());
                        dead = true;
                    }
                } else if (isClassUseWord(p)) {
                    addClassUseParam(p, items);
                } else {
                    NOptional<Integer> v = _asInt(p, context);
                    if (v.isPresent()) {
                        NMsg errMsg = NMsg.ofC("[%s] table-column(%s): positional form is deprecated; use table-column(col: %s)", NTxUtils.shortName(context.source()), v.get(), v.get()).asWarning();
                        context.log(errMsg, context.source());
                        col = v.get();
                    } else {
                        dead = true;
                    }
                }
            }
            if (!dead && col != null) {
                items.add(NTxStyleRuleSelectorItem.ofTableColumn(col));
            }
            return;
        }
        if (uid.equals("table-cell")) {
            Integer row = null;
            Integer col = null;
            boolean dead = false;
            if (params != null) {
                for (NElement p : params) {
                    if (p.isNamedPair()) {
                        NPairElement pair = p.asNamedPair().get();
                        String k = NTxUtils.uid(NTxValue.of(pair.key()).asStringOrName().orElse(""));
                        if (!(k.equals("row") || k.equals("r") || k.equals("col") || k.equals("c"))) {
                            NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. table-cell accepts only row : and col : 1-based indices", NTxUtils.shortName(context.source()), name).asSevere();
                            context.log(errMsg, context.source());
                            dead = true;
                            continue;
                        }
                        NOptional<Integer> v = _asInt(pair.value(), context);
                        if (!v.isPresent()) {
                            dead = true;
                            continue;
                        }
                        if (k.equals("row") || k.equals("r")) {
                            row = v.get();
                        } else {
                            col = v.get();
                        }
                    } else if (isClassUseWord(p)) {
                        addClassUseParam(p, items);
                    } else {
                        NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. table-cell accepts only row : and col : 1-based indices", NTxUtils.shortName(context.source()), name).asSevere();
                        context.log(errMsg, context.source());
                        dead = true;
                    }
                }
            }
            if (!dead) {
                items.add(NTxStyleRuleSelectorItem.ofTableCell(row, col));
            }
            return;
        }
        if (params == null || params.isEmpty()) {
            NTxStyleRuleSelectorItem base = ofSelectorItem(name, context).orNull();
            if (base != null && !(base instanceof NTxStyleRuleSelectorItem.NoneItem)) {
                items.add(base);
            }
        } else {
            NTxStyleRuleSelectorItem base = ofSelectorItem(name, context).orNull();
            if (base != null && !(base instanceof NTxStyleRuleSelectorItem.ClassDefItem)) {
                items.add(base);
            }
            for (NElement p : params) {
                parseStyleRuleSelectorParam(p, context, items);
            }
        }
    }

    private static boolean isClassUseWord(NElement p) {
        NOptional<String> w = NTxValue.of(p).asStringOrName();
        return w.isPresent() && NTxUtils.uid(w.get()).startsWith("class-");
    }

    private static void addClassUseParam(NElement p, List<NTxStyleRuleSelectorItem> items) {
        String w = NTxValue.of(p).asStringOrName().get();
        items.add(NTxStyleRuleSelectorItem.ofClassUse(NTxUtils.uid(w).substring("class-".length())));
    }

    /**
     * Like {@link #parseStyleRuleSelectorItem} but treats a bare
     * {@code class-<name>} as a class <b>usage</b> constraint (the node must
     * carry that class), never as a class definition.
     */
    public static void parseStyleRuleSelectorParam(NElement selector, NTxResolutionContext context, List<NTxStyleRuleSelectorItem> items) {
        NOptional<String> w = NTxValue.of(selector).asStringOrName();
        if (w.isPresent()) {
            String s = NTxUtils.uid(w.get());
            if (s.startsWith("class-")) {
                items.add(NTxStyleRuleSelectorItem.ofClassUse(s.substring("class-".length())));
                return;
            }
        }
        parseStyleRuleSelectorItem(selector, context, items);
    }

    public static void parseStyleRuleSelectorItem(NElement selector, NTxResolutionContext context, List<NTxStyleRuleSelectorItem> items) {
        switch (selector.type()) {
            case NAME:
            case BACKTICK_STRING:
            case SINGLE_QUOTED_STRING:
            case DOUBLE_QUOTED_STRING:
            case TRIPLE_BACKTICK_STRING:
            case TRIPLE_DOUBLE_QUOTED_STRING:
            case TRIPLE_SINGLE_QUOTED_STRING:
            case LINE_STRING:
            case BLOCK_STRING:
            {
                NTxStyleRuleSelectorItem n = ofSelectorItem(selector.asStringValue().get(), context).orNull();
                if (n != null) {
                    items.add(n);
                }
                return;
            }
            case FLAT_EXPR: {
                NTxStyleRuleSelectorItem n = ofSelectorItem(selector.asFlatExpression().get().toCompactString(), context).orNull();
                if (n != null) {
                    items.add(n);
                }
                return;
            }
            case OPERATOR_SYMBOL: {
                switch (selector.asOperatorSymbol().get().symbol()) {
                    case MUL: {
                        items.add(DefaultNTxNodeSelector.ANY_ITEM);
                        return;
                    }
                }
                break;
            }
            case PAIR: {
                NPairElement pair = selector.asPair().get();
                NElement key = pair.key();
                String kk = namedSelectorName(key);
                if (kk == null) {
                    kk = NTxValue.of(key).asStringOrName().orNull();
                }
                if (kk != null) {
                    parseNamedParamSelector(NTxUtils.uid(kk), selectorParams(key), context, items);
                    return;
                }
                List<String> types = new ArrayList<>();
                List<String> names = new ArrayList<>();
                parsePair(pair, context, types, names);
                items.add(NTxStyleRuleSelectorItem.of(types.toArray(new String[0]), names.toArray(new String[0])));
                return;
            }
            case NAMED_OBJECT:
            case NAMED_ARRAY:
            case NAMED_TUPLE: {
                String nm = namedSelectorName(selector);
                if (nm != null) {
                    parseNamedParamSelector(nm, selectorParams(selector), context, items);
                    return;
                }
                break;
            }
            case PARAM_OBJECT:
            case PARAM_ARRAY: {
                if (selector.isNamed()) {
                    String nm = selector.asNamed().get().name().get();
                    List<NElement> params = selector.asParametrizedContainer().get().params().orElse(java.util.Collections.emptyList());
                    parseNamedParamSelector(nm, params, context, items);
                } else {
                    // Unnamed paren selector such as '(*)' -> params carry the items.
                    NOptional<NParametrizedContainerElement> pc = selector.asParametrizedContainer();
                    boolean handled = false;
                    if (pc.isPresent()) {
                        List<NElement> params = pc.get().params().orElse(java.util.Collections.emptyList());
                        if (!params.isEmpty()) {
                            for (NElement p : params) {
                                parseStyleRuleSelectorParam(p, context, items);
                            }
                            handled = true;
                        }
                    }
                    if (!handled) {
                        for (NElement p : selector.asListContainer().get().children()) {
                            parseStyleRuleSelectorParam(p, context, items);
                        }
                    }
                }
                return;
            }
            case TUPLE: {
                NTupleElement u = selector.asTuple().get();
                if (isExactUpletPair(selector)) {
                    List<String> types = new ArrayList<>();
                    List<String> names = new ArrayList<>();
                    for (NElement child : u.children()) {
                        if (child.isNamedPair()) {
                            parsePair(child.asPair().get(), context, types, names);
                        }
                    }
                    items.add(NTxStyleRuleSelectorItem.of(types.toArray(new String[0]), names.toArray(new String[0])));
                } else {
                    for (NElement item : u.children()) {
                        parseStyleRuleSelectorParam(item, context, items);
                    }
                }
                return;
            }
        }
        NMsg msg = NMsg.ofC("unable to resolve style selector from %s", selector).asError();
        context.log(msg);
    }

    private static boolean isExactUpletPair(NElement e) {
        if (e.isTuple()) {
            NTupleElement u = e.asTuple().get();
            return (u.children().stream().allMatch(x -> x.isNamedPair(s -> {
                switch (NTxUtils.uid(s)) {
                    case "name":
                    case "names":
                    case "type":
                    case "types":
                        return true;
                }
                return false;
            })));
        }
        return false;
    }


    public static NOptional<NTxStyleRuleSelectorItem> ofSelectorItem(String item, NTxResolutionContext context) {
        return NTxStyleRuleSelectorItem.of(item, context.log());
    }


    public static NOptional<NTxStyleRuleSelector> parseStyleRuleSelector(NElement selectors, NTxResolutionContext context) {
        if (selectors.isEmpty()) {
            return NOptional.of(DefaultNTxNodeSelector.ofAny());
        }
        List<NTxStyleRuleSelectorItem> items = new ArrayList<>();
        parseStyleRuleSelectorItem(selectors, context, items);
        return NOptional.of(DefaultNTxNodeSelector.of(items.toArray(new NTxStyleRuleSelectorItem[0])));
    }

    public static NOptional<NTxStyleRule[]> parseStyleRule(NElement e, NTxDocumentFactory f, NTxResolutionContext context) {
        NMsg errMsg = NMsg.ofC("[%s] invalid style rule %s", NTxUtils.shortName(context.source()), e).asSevere();
        switch (e.type()) {
            case PAIR: {
                NElement key = e.asPair().get().key();
                NElement v = e.asPair().get().value();
                switch (v.type()) {
                    case OBJECT:
                    case FULL_OBJECT:
                    case PARAM_OBJECT:
                    case NAMED_OBJECT: {
                        return _parseStyleRule(e, key, v.toObject().get().children(), f, context, errMsg);
                    }
                    case ARRAY:
                    case FULL_ARRAY:
                    case PARAM_ARRAY:
                    case NAMED_ARRAY: {
                        return _parseStyleRule(e, key, v.toArray().get().children(), f, context, errMsg);
                    }
                }
                break;
            }
            case PARAM_OBJECT:
            case PARAM_ARRAY: {
                List<NElement> children = e.asListContainer().get().children();
                return _parseStyleRule(e, e, children, f, context, errMsg);
            }
            case NAMED_OBJECT:
            case NAMED_ARRAY: {
                List<NElement> children = e.asListContainer().get().children();
                return _parseStyleRule(e, e, children, f, context, errMsg);
            }
        }
        context.log(errMsg, context.source());
        return NOptional.ofEmpty(errMsg);
    }

    public static NOptional<NTxStyleRule[]> _parseStyleRule(NElement e, NElement selectors, List<NElement> children, NTxDocumentFactory f, NTxResolutionContext context, NMsg errMsg) {
        NOptional<NTxStyleRuleSelector> r = parseStyleRuleSelector(selectors, context);
        if (!r.isPresent()) {
            context.log(errMsg, context.source());
            return NOptional.ofEmpty(errMsg);
        }
        List<NTxProp> styles = new ArrayList<>();
        for (NElement el : children) {
            NOptional<NTxProp[]> s = parseStyle(el, context);
            if (!s.isPresent()) {
                s = parseStyle(el, context);
                return NOptional.ofEmpty(s.message());
            }
            styles.addAll(Arrays.asList(s.get()));
        }
        return NOptional.of(
                new NTxStyleRule[]{
                        DefaultNTxStyleRule.of(context.node(), context.source(), r.get(), styles.toArray(new NTxProp[0]))
                }
        );
    }


    public static NOptional<NTxProp[]> parseStyle(NElement e, NTxResolutionContext context) {
        switch (e.type()) {
            case PAIR: {
                NTxValue h = NTxValue.of(e.asPair().get().key());
                NOptional<String> u = h.asStringOrName();
                if (u.isPresent()) {
                    String uid = NTxUtils.uid(u.get());
                    return NOptional.of(new NTxProp[]{new NTxProp(uid, e.asPair().get().value(), context.node())});
                }
                break;
            }
            case NAME: {
                NTxValue h = NTxValue.of(e);
                NOptional<String> u = h.asStringOrName();
                if (u.isPresent()) {
                    String uid = NTxUtils.uid(u.get());
                    return NOptional.of(new NTxProp[]{new NTxProp(uid, NElement.ofBoolean(true), context.node())});
                }
                break;
            }
        }
        //context.messages().addMessage(HMsg.of(NMsg.ofC("[%s] invalid style %s. expected key:value format", context.source(), e),context.source());
        return NOptional.ofEmpty(NMsg.ofC("[%s] invalid style %s. expected key:value format", NTxUtils.shortName(context.source()), e));
    }

}
