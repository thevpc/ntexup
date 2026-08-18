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
                NTxPropName.ROWSPAN,
                NTxPropName.COLWEIGHT,
                NTxPropName.ROWWEIGHT,
                NTxPropName.GRID_COLOR,
//                HPropName.LINE_COLOR,
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
                NTxPropName.THEED,
                NTxPropName.DRAW_CONTOUR,
                NTxPropName.CLASS,
                NTxPropName.AT,
                NTxPropName.COMPONENT_NAME,
                NTxPropName.CONTENT_ORIGIN,
                NTxPropName.CONTENT_POSITION,
                NTxPropName.ALIGN
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
                NTxPropName.THEED,
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
                        "contour"
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
                case "class":
                case "classes": {
                    NTxValue h2 = NTxValue.of(pair.value());
                    NOptional<String[]> cc = h2.asStringArrayOrString();
                    if (cc.isPresent()) {
                        classes.addAll(Arrays.asList(cc.get()));
                    } else {
                        NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected a string or a string array", NTxUtils.shortName(context.source()), pair).asSevere();
                        context.log(errMsg, context.source());
                    }
                    break;
                }
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
                    NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected one of 'name', 'class' or 'type'", NTxUtils.shortName(context.source()), pair).asSevere();
                    context.log(errMsg, context.source());
                }
            }
        } else {
            NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected one of 'name', 'class' or 'type'", NTxUtils.shortName(context.source()), pair).asSevere();
            context.log(errMsg, context.source());
        }
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
                List<String> classes = new ArrayList<>();
                List<String> types = new ArrayList<>();
                List<String> names = new ArrayList<>();
                NPairElement pair = selector.asPair().get();
                parsePair(pair, context, types, names, classes);
                items.add(NTxStyleRuleSelectorItem.of(types.toArray(new String[0]), names.toArray(new String[0]), classes.toArray(new String[0])));
                return;
            }
            case TUPLE: {
                NTupleElement u = selector.asTuple().get();
                if (isExactUpletPair(selector)) {
                    List<String> classes = new ArrayList<>();
                    List<String> types = new ArrayList<>();
                    List<String> names = new ArrayList<>();
                    for (NElement child : u.children()) {
                        if (child.isNamedPair()) {
                            parsePair(child.asPair().get(), context, types, names, classes);
                        }
                    }
                    items.add(NTxStyleRuleSelectorItem.of(types.toArray(new String[0]), names.toArray(new String[0]), classes.toArray(new String[0])));
                } else {
                    for (NElement item : u.children()) {
                        parseStyleRuleSelectorItem(item, context, items);
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
                    case "class":
                    case "classes":
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
                List<NElement> params = e.asParametrizedContainer().get().params().get();
                List<NElement> children = e.asListContainer().get().children();
                return _parseStyleRule(e, NElement.ofTuple(params.toArray(new NElement[0])), children, f, context, errMsg);
            }
            case NAMED_OBJECT:
            case NAMED_ARRAY: {
                String name = e.asNamed().get().name().get();
                List<NElement> children = e.asListContainer().get().children();
                return _parseStyleRule(e, NElement.ofString(name), children, f, context, errMsg);
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
                return NOptional.ofEmpty(s.getMessage());
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
