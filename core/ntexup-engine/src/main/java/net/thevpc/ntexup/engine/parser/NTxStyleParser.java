package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
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
                NTxPropName.GRID_WEIGHT,
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
                NTxPropName.BULLET_ALIGN,
                NTxPropName.OVERFLOW,
                NTxPropName.OVERFLOW_MARGIN,
                NTxPropName.OVERFLOW_FADE,
                NTxPropName.PENDING,
                NTxPropName.PENDING_TEXT,
                NTxPropName.PENDING_COLOR,
                NTxPropName.PENDING_BACKGROUND
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
                NTxPropName.DRAW_CONTOUR,
                NTxPropName.OVERFLOW
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
                                  List<String> names,
                                  List<String> classes
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
                case "class": {
                    NTxValue h2 = NTxValue.of(pair.value());
                    NOptional<String[]> cc = h2.asStringArrayOrString();
                    if (cc.isPresent()) {
                        classes.addAll(Arrays.asList(cc.get()));
                    } else {
                        NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected a class name or an array of class names", NTxUtils.shortName(context.source()), pair).asSevere();
                        context.log(errMsg, context.source());
                    }
                    break;
                }
                default: {
                    NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected one of 'name', 'type' or 'class'", NTxUtils.shortName(context.source()), pair).asSevere();
                    context.log(errMsg, context.source());
                }
            }
        } else {
            NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected one of 'name', 'type' or 'class'", NTxUtils.shortName(context.source()), pair).asSevere();
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
     *     <li>@&lt;name&gt;[(base, ...)]  - class definition</li>
     *     <li>table-row(row: header|even|odd) or table-row(row: n)</li>
     *     <li>table-column(col: n)</li>
     *     <li>table-cell(row: r, col: c) — row: is 1-based over data rows</li>
     *     <li>table-header(row: n[, col: m]) / table-footer(row: n[, col: m])</li>
     *     <li>table-weight(...) — removed selector; dropped with a warning</li>
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
            context.log(NMsg.ofC("[%s] class- definitions are no longer supported; declare @%s in a styles{} block", NTxUtils.shortName(context.source()), uid.substring("class-".length())).asSevere(), context.source());
            return;
        }
        if (uid.equals("table-row")) {
            String kind = null;
            Integer row = null;
            boolean dead = false;
            if (params != null) {
                for (NElement p : params) {
                    if (addClassConstraint(p, context, items)) {
                        continue;
                    }
                    if (p.isNamedPair()) {
                        NPairElement pair = p.asNamedPair().get();
                        String k = NTxUtils.uid(NTxValue.of(pair.key()).asStringOrName().orElse(""));
                        if (k.equals("row") || k.equals("r")) {
                            String v = NTxUtils.uid(NTxValue.of(pair.value()).asStringOrName().orElse(""));
                            if (v.equals("header") || v.equals("even") || v.equals("odd")) {
                                kind = v;
                            } else {
                                NOptional<Integer> vi = NTxValue.of(pair.value()).asInt();
                                if (vi.isPresent() && vi.get() > 0) {
                                    row = vi.get();
                                } else {
                                    NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. table-row(row:) accepts header, even, odd or a 1-based data-row index", NTxUtils.shortName(context.source()), name).asSevere();
                                    context.log(errMsg, context.source());
                                    dead = true;
                                }
                            }
                        } else {
                            NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. table-row accepts only row: {header, even, odd, n} or class: name usages", NTxUtils.shortName(context.source()), name).asSevere();
                            context.log(errMsg, context.source());
                            dead = true;
                        }
                    } else {
                        NOptional<String> word = NTxValue.of(p).asStringOrName();
                        if (word.isPresent()) {
                            String w = NTxUtils.uid(word.get());
                            if (w.equals("header") || w.equals("even") || w.equals("odd")) {
                                NMsg errMsg = NMsg.ofC("[%s] table-row(%s): positional form is deprecated; use table-row(row: %s)%s", NTxUtils.shortName(context.source()), w, w, w.equals("header") ? " or table-header" : "").asWarning();
                                context.log(errMsg, context.source());
                                kind = w;
                            } else {
                                NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. table-row accepts only row: {header, even, odd, n} or class: name usages", NTxUtils.shortName(context.source()), name).asSevere();
                                context.log(errMsg, context.source());
                                dead = true;
                            }
                        } else {
                            NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. table-row accepts only row: {header, even, odd, n} or class: name usages", NTxUtils.shortName(context.source()), name).asSevere();
                            context.log(errMsg, context.source());
                            dead = true;
                        }
                    }
                }
            }
            if (!dead) {
                items.add(NTxStyleRuleSelectorItem.ofTableRow(kind, row));
            }
            return;
        }
        if (uid.equals("table-header") || uid.equals("table-footer")) {
            Integer row = null;
            Integer col = null;
            boolean dead = false;
            if (params != null) {
                for (NElement p : params) {
                    if (addClassConstraint(p, context, items)) {
                        continue;
                    }
                    if (p.isNamedPair()) {
                        NPairElement pair = p.asNamedPair().get();
                        String k = NTxUtils.uid(NTxValue.of(pair.key()).asStringOrName().orElse(""));
                        if (!(k.equals("row") || k.equals("r") || k.equals("col") || k.equals("c"))) {
                            NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. %s accepts only row: and col: 1-based indices", NTxUtils.shortName(context.source()), name, uid).asSevere();
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
                    } else {
                        NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. %s accepts only row: and col: 1-based indices", NTxUtils.shortName(context.source()), name, uid).asSevere();
                        context.log(errMsg, context.source());
                        dead = true;
                    }
                }
            }
            if (!dead) {
                items.add(uid.equals("table-header")
                        ? NTxStyleRuleSelectorItem.ofTableHeader(row, col)
                        : NTxStyleRuleSelectorItem.ofTableFooter(row, col));
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
                if (addClassConstraint(p, context, items)) {
                    continue;
                }
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
                        NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. table-column accepts only col: with a 1-based column index or class: name usages", NTxUtils.shortName(context.source()), name).asSevere();
                        context.log(errMsg, context.source());
                        dead = true;
                    }
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
                    if (addClassConstraint(p, context, items)) {
                        continue;
                    }
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
        if (uid.equals("table-weight")) {
            // table-weight(row:/col:) was removed: weights are plain row-weight /
            // column-weight props set through table-row / table-header /
            // table-footer / table-column / table-cell selectors instead.
            // Never-fail: the rule is dropped with a warning.
            NMsg errMsg = NMsg.ofC("[%s] table-weight(...) selector is removed; set row-weight / column-weight through table-row(row: n), table-header(row: n[, col: m]), table-footer(row: n[, col: m]), table-column(col: n) or table-cell(row: r, col: c) instead", NTxUtils.shortName(context.source())).asWarning();
            context.log(errMsg, context.source());
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

    private static boolean addClassConstraint(NElement selector, NTxResolutionContext context, List<NTxStyleRuleSelectorItem> items) {
        if (!selector.isNamedPair()) {
            return false;
        }
        NPairElement pair = selector.asPair().get();
        String key = NTxUtils.uid(NTxValue.of(pair.key()).asStringOrName().orElse(""));
        if (!key.equals("class")) {
            return false;
        }
        NOptional<String[]> classes = NTxValue.of(pair.value()).asStringArrayOrString();
        if (classes.isPresent()) {
            for (String value : classes.get()) {
                String name = NTxUtils.uid(value);
                if (!NBlankable.isBlank(name)) {
                    items.add(NTxStyleRuleSelectorItem.ofClassUse(name));
                }
            }
        } else {
            context.log(NMsg.ofC("[%s] invalid class constraint %s. expected a class name or an array of class names", NTxUtils.shortName(context.source()), pair).asSevere(), context.source());
        }
        return true;
    }

    /**
     * Parses a selector parameter, where {@code class: name} constrains usage
     * instead of declaring a class.
     */
    public static void parseStyleRuleSelectorParam(NElement selector, NTxResolutionContext context, List<NTxStyleRuleSelectorItem> items) {
        if (addClassConstraint(selector, context, items)) {
            return;
        }
        NOptional<String> w = NTxValue.of(selector).asStringOrName();
        if (w.isPresent()) {
            String s = NTxUtils.uid(w.get());
            if (s.startsWith("class-")) {
                context.log(NMsg.ofC("[%s] class- selectors are no longer supported; use class: %s", NTxUtils.shortName(context.source()), s.substring("class-".length())).asSevere(), context.source());
                return;
            }
            if (s.startsWith("@") && s.length() > 1) {
                items.add(NTxStyleRuleSelectorItem.ofClassUse(s.substring(1)));
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
                if (addClassConstraint(selector, context, items)) {
                    return;
                }
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
                List<String> classes = new ArrayList<>();
                parsePair(pair, context, types, names, classes);
                if (!types.isEmpty() || !names.isEmpty()) {
                    items.add(NTxStyleRuleSelectorItem.of(types.toArray(new String[0]), names.toArray(new String[0])));
                }
                for (String className : classes) {
                    items.add(NTxStyleRuleSelectorItem.ofClassUse(NTxUtils.uid(className)));
                }
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
                    List<String> classes = new ArrayList<>();
                    for (NElement child : u.children()) {
                        if (child.isNamedPair()) {
                            parsePair(child.asPair().get(), context, types, names, classes);
                        }
                    }
                    if (!types.isEmpty() || !names.isEmpty()) {
                        items.add(NTxStyleRuleSelectorItem.of(types.toArray(new String[0]), names.toArray(new String[0])));
                    }
                    for (String className : classes) {
                        items.add(NTxStyleRuleSelectorItem.ofClassUse(NTxUtils.uid(className)));
                    }
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
                    case "class":
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

    private static List<String> parseClassBases(NElementAnnotation annotation, NTxResolutionContext context) {
        List<String> bases = new ArrayList<>();
        List<NElement> params = annotation.params().orElse(Collections.emptyList());
        for (NElement param : params) {
            if (param.isNamedPair()) {
                NPairElement pair = param.asPair().get();
                String key = NTxUtils.uid(NTxValue.of(pair.key()).asStringOrName().orElse(""));
                if (key.equals("extends") || key.equals("bases")) {
                    NOptional<String[]> values = NTxValue.of(pair.value()).asStringArrayOrString();
                    if (values.isPresent()) {
                        for (String value : values.get()) {
                            bases.add(NTxUtils.uid(value));
                        }
                    } else {
                        context.log(NMsg.ofC("[%s] invalid base class in @%s. expected a class name or an array of class names", NTxUtils.shortName(context.source()), pair, annotation.name()).asSevere(), context.source());
                    }
                } else {
                    context.log(NMsg.ofC("[%s] invalid class definition @%s. expected positional base class names or an 'extends:' named pair", NTxUtils.shortName(context.source()), param, annotation.name()).asSevere(), context.source());
                }
            } else {
                NOptional<String> value = NTxValue.of(param).asStringOrName();
                if (value.isPresent()) {
                    bases.add(NTxUtils.uid(value.get()));
                } else {
                    context.log(NMsg.ofC("[%s] invalid base class %s in @%s. expected a class name", NTxUtils.shortName(context.source()), param, annotation.name()).asSevere(), context.source());
                }
            }
        }
        return bases;
    }

    private static String tableSectionName(NElement selector) {
        String raw = namedSelectorName(selector);
        if (raw == null) {
            raw = NTxValue.of(selector).asStringOrName().orNull();
        }
        if (raw == null) {
            return null;
        }
        switch (NTxUtils.uid(raw)) {
            case "table":
            case "table-row":
            case "table-header":
            case "table-footer":
            case "table-column":
            case "table-cell":
                return NTxUtils.uid(raw);
            case "row":
                return "table-row";
            case "header":
                return "table-header";
            case "footer":
                return "table-footer";
            case "column":
                return "table-column";
            case "cell":
                return "table-cell";
            default:
                return null;
        }
    }

    private static String nestedTableSelectorName(NElement child) {
        if (child.isNamedPair()) {
            NPairElement pair = child.asPair().get();
            if (pair.value().isAnyObject() || pair.value().isAnyArray()) {
                return tableSectionName(pair.key());
            }
        } else if (child.isNamed() && (child.isAnyObject() || child.isAnyArray())) {
            return tableSectionName(child);
        }
        return null;
    }

    private static NOptional<NTxStyleRule> parseNestedTableStyleRule(NElement child, String className, NTxResolutionContext context) {
        NElement selectorElement;
        List<NElement> children;
        if (child.isNamedPair()) {
            NPairElement pair = child.asPair().get();
            selectorElement = pair.key();
            children = pair.value().asListContainer().get().children();
        } else {
            selectorElement = child;
            children = child.asListContainer().get().children();
        }
        String sectionName = tableSectionName(selectorElement);
        List<NElement> sectionParams = selectorParams(selectorElement);
        NOptional<NTxStyleRuleSelector> selector;
        if (sectionParams.isEmpty()) {
            NTxStyleRuleSelectorItem item;
            switch (sectionName) {
                case "table":
                    item = NTxStyleRuleSelectorItem.of(new String[]{"table"}, new String[0]);
                    break;
                case "table-row":
                    item = NTxStyleRuleSelectorItem.ofTableRow(null);
                    break;
                case "table-header":
                    item = NTxStyleRuleSelectorItem.ofTableHeader(null, null);
                    break;
                case "table-footer":
                    item = NTxStyleRuleSelectorItem.ofTableFooter(null, null);
                    break;
                case "table-cell":
                    item = NTxStyleRuleSelectorItem.ofTableCell(null, null);
                    break;
                default:
                    return NOptional.ofEmpty(NMsg.ofC("invalid table style selector %s. table-column expects col: with a 1-based column index", child));
            }
            selector = NOptional.of(DefaultNTxNodeSelector.of(item));
        } else {
            NElement normalizedSelector = NElement.ofNamedTuple(sectionName, sectionParams.toArray(new NElement[0]));
            selector = parseStyleRuleSelector(normalizedSelector, context);
        }
        if (!selector.isPresent()) {
            return NOptional.ofEmpty(NMsg.ofC("invalid table style selector %s", child));
        }
        DefaultNTxNodeSelector scoped = ((DefaultNTxNodeSelector) selector.get())
                .and(NTxStyleRuleSelectorItem.ofTableClassUse(className));
        List<NTxProp> styles = new ArrayList<>();
        for (NElement style : children) {
            NOptional<NTxProp[]> parsed = parseStyle(style, context);
            if (!parsed.isPresent()) {
                return NOptional.ofEmpty(parsed.message());
            }
            styles.addAll(Arrays.asList(parsed.get()));
        }
        return NOptional.of(DefaultNTxStyleRule.of(context.node(), context.source(), scoped, styles.toArray(new NTxProp[0])));
    }

    private static NOptional<NTxStyleRule[]> parseAnnotatedClassRule(NElement e, NTxResolutionContext context, NMsg errMsg) {
        List<NElementAnnotation> annotations = e.annotations();
        if (annotations.size() != 1) {
            context.log(NMsg.ofC("[%s] a class declaration must have exactly one @name annotation", NTxUtils.shortName(context.source())).asSevere(), context.source());
            return NOptional.ofEmpty(errMsg);
        }
        NElementAnnotation annotation = annotations.get(0);
        String className = NTxUtils.uid(annotation.name());
        if (NBlankable.isBlank(className) || className.equals(NTxNodeType.CTRL_DEFINE) || className.startsWith("class-")) {
            context.log(NMsg.ofC("[%s] invalid class declaration %s. expected @name", NTxUtils.shortName(context.source()), e).asSevere(), context.source());
            return NOptional.ofEmpty(errMsg);
        }
        List<String> bases = parseClassBases(annotation, context);
        List<NTxProp> styles = new ArrayList<>();
        List<NTxStyleRule> rules = new ArrayList<>();
        rules.add(DefaultNTxStyleRule.ofClassDef(context.node(), context.source(), className, bases));
        for (NElement child : e.asListContainer().get().children()) {
            if (nestedTableSelectorName(child) != null) {
                NOptional<NTxStyleRule> nested = parseNestedTableStyleRule(child, className, context);
                if (!nested.isPresent()) {
                    return NOptional.ofEmpty(nested.message());
                }
                rules.add(nested.get());
            } else {
                NOptional<NTxProp[]> parsed = parseStyle(child, context);
                if (!parsed.isPresent()) {
                    return NOptional.ofEmpty(parsed.message());
                }
                styles.addAll(Arrays.asList(parsed.get()));
            }
        }
        rules.set(0, DefaultNTxStyleRule.ofClassDef(context.node(), context.source(), className, bases, styles.toArray(new NTxProp[0])));
        return NOptional.of(rules.toArray(new NTxStyleRule[0]));
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
            case OBJECT:
            case FULL_OBJECT:
            case ARRAY:
            case FULL_ARRAY: {
                if (!e.isNamed() && !e.annotations().isEmpty()) {
                    return parseAnnotatedClassRule(e, context, errMsg);
                }
                break;
            }
            case PARAM_OBJECT:
            case PARAM_ARRAY: {
                if (!e.isNamed() && !e.annotations().isEmpty()) {
                    return parseAnnotatedClassRule(e, context, errMsg);
                }
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
