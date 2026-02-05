package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.style.*;
import net.thevpc.ntexup.api.parser.NTxNodeFactoryParseContext;
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
                NTxPropName.COMPONENT_NAME
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


    public static NOptional<NTxStyleRuleSelector> parseStyleRuleSelector(List<NElement> selectors, NTxNodeFactoryParseContext context) {
        if (selectors.isEmpty()) {
            return NOptional.of(DefaultNTxNodeSelector.ofAny());
        }
        List<String> names = new ArrayList<>();
        List<String> classes = new ArrayList<>();
        List<String> types = new ArrayList<>();
        List<NElement> selectors2 = new ArrayList<>(selectors);
        while (!selectors2.isEmpty()) {
            NElement child = selectors2.remove(0);
            switch (child.type()) {
                case OPERATOR_SYMBOL: {
                    switch (child.asOperatorSymbol().get().symbol()){
                        case MUL:
                        {
                            return NOptional.of(DefaultNTxNodeSelector.ofAny());
                        }
                    }
                    NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected *", NTxUtils.shortName(context.source()), child).asSevere();
                    context.messages().log(errMsg, context.source());
                    return NOptional.ofEmpty(errMsg);
                }
                case PAIR: {
                    NPairElement pair = child.asPair().get();
                    NTxValue h = NTxValue.of(pair.key());
                    NOptional<String> k = h.asStringOrName();
                    if (k.isPresent()) {
                        switch (NTxUtils.uid(k.get())) {
                            case "class": {
                                NTxValue h2 = NTxValue.of(pair.value());
                                NOptional<String[]> cc = h2.asStringArrayOrString();
                                if (cc.isPresent()) {
                                    classes.addAll(Arrays.asList(cc.get()));
                                } else {
                                    NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected a string or a string array", NTxUtils.shortName(context.source()), child).asSevere();
                                    context.messages().log(errMsg, context.source());
                                    return NOptional.ofEmpty(errMsg);
                                }
                            }
                            case "name": {
                                NTxValue h2 = NTxValue.of(pair.value());
                                NOptional<String[]> cc = h2.asStringArrayOrString();
                                if (cc.isPresent()) {
                                    names.addAll(Arrays.asList(cc.get()));
                                } else {
                                    NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected a string or a string array.", NTxUtils.shortName(context.source()), child).asSevere();
                                    context.messages().log(errMsg, context.source());
                                    return NOptional.ofEmpty(errMsg);
                                }
                            }
                            case "type": {
                                NTxValue h2 = NTxValue.of(pair.value());
                                NOptional<String[]> cc = h2.asStringArrayOrString();
                                if (cc.isPresent()) {
                                    types.addAll(Arrays.asList(cc.get()));
                                } else {
                                    NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected a valid node type or array", NTxUtils.shortName(context.source()), child).asSevere();
                                    context.messages().log(errMsg, context.source());
                                    return NOptional.ofEmpty(errMsg);
                                }
                            }
                            default: {
                                NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected one of 'name', 'class' or 'type'", NTxUtils.shortName(context.source()), child).asSevere();
                                context.messages().log(errMsg, context.source());
                                return NOptional.ofEmpty(errMsg);
                            }
                        }
                    } else {
                        NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. expected one of 'name', 'class' or 'type'", NTxUtils.shortName(context.source()), child).asSevere();
                        context.messages().log(errMsg, context.source());
                        return NOptional.ofEmpty(errMsg);
                    }
                }
                case NAME: {
                    String s = child.asStringValue().get().trim();
                    if (s.isEmpty() || s.equals("*")) {
                        //
                    } else if (s.startsWith(".")) {
                        classes.add(s.substring(1));
                    } else {
                        types.add(s);
                    }
                    break;
                }
                case FLAT_EXPR: {
                    List<NElement> fes = new ArrayList<>(child.asFlatExpression().get().children());
                    while (!fes.isEmpty()) {
                        NElement c = fes.remove(0);
                        if (c.isOperatorSymbol(NOperatorSymbol.DOT) && !fes.isEmpty() && (fes.get(0).isAnyString() || fes.get(0).isName())) {
                            NElement c2 = fes.remove(0);
                            String s = c2.asStringValue().get();
                            if (s.isEmpty() || s.equals("*")) {
                                return NOptional.of(DefaultNTxNodeSelector.ofAny());
                            } else if (s.startsWith(".")) {
                                classes.add(s.substring(1));
                            } else {
                                classes.add(s);
                            }
                        }else if (c.isOperatorSymbol(NOperatorSymbol.MUL)) {
                            return NOptional.of(DefaultNTxNodeSelector.ofAny());
                        } else if (c.isAnyString() || c.isName()) {
                            String s = c.asStringValue().get();
                            if (s.isEmpty() || s.equals("*")) {
                                return NOptional.of(DefaultNTxNodeSelector.ofAny());
                            } else if (s.startsWith(".")) {
                                classes.add(s.substring(1));
                            } else {
                                types.add(s);
                            }
                        } else {
                            NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s. error at %s", NTxUtils.shortName(context.source()), child, c).asSevere();
                            context.messages().log(errMsg, context.source());
                            return NOptional.ofEmpty(errMsg);
                        }
                    }
                    break;
                }
                case DOUBLE_QUOTED_STRING:
                case SINGLE_QUOTED_STRING:
                case BACKTICK_STRING:
                case TRIPLE_DOUBLE_QUOTED_STRING:
                case TRIPLE_SINGLE_QUOTED_STRING:
                case TRIPLE_BACKTICK_STRING:
                case LINE_STRING: {
                    String s = child.asStringValue().get().trim();
                    if (s.isEmpty() || s.equals("*")) {
                        //
                    } else if (s.startsWith(".")) {
                        classes.add(s.substring(1));
                    } else {
                        names.add(s);
                    }
                    break;
                }
                default: {
                    NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s", context.source(), child).asSevere();
                    context.messages().log(errMsg, context.source());
                    return NOptional.ofEmpty(errMsg);
                }
            }
        }
        return NOptional.of(DefaultNTxNodeSelector.of(
                names.toArray(new String[0]),
                types.toArray(new String[0]),
                classes.toArray(new String[0])
        ));
//        switch (e.type()) {
//            case DOUBLE_QUOTED_STRING:
//            case SINGLE_QUOTED_STRING:
//            case BACKTICK_STRING:
//            case TRIPLE_DOUBLE_QUOTED_STRING:
//            case TRIPLE_SINGLE_QUOTED_STRING:
//            case TRIPLE_BACKTICK_STRING:
//            case LINE_STRING: {
//                String s = e.asStringValue().get();
//                if (s.isEmpty() || s.equals("*")) {
//                    return NOptional.of(DefaultNTxNodeSelector.ofAny());
//                }
//                if (s.startsWith(".")) {
//                    return NOptional.of(DefaultNTxNodeSelector.ofClasses(s.substring(1)));
//                }
//                return NOptional.of(DefaultNTxNodeSelector.ofName(s));
//            }
//            case NAME: {
//                String n = e.asStringValue().get();
//                if (n.startsWith(".")) {
//                    return NOptional.of(DefaultNTxNodeSelector.ofClasses(n.substring(1)));
//                }
//                return NOptional.of(DefaultNTxNodeSelector.ofType(n));
//            }
//            case UPLET:
//            case NAMED_UPLET: {
//
//            }
//            default: {
//                NMsg errMsg = NMsg.ofC("[%s] invalid style rule selector %s", NTxUtils.shortName(context.source()), e).asSevere();
//                context.messages().log(errMsg, context.source());
//                return NOptional.ofEmpty(errMsg);
//            }
//        }
    }

    public static NOptional<NTxStyleRule[]> parseStyleRule(NElement e, NTxDocumentFactory f, NTxNodeFactoryParseContext context) {
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
                        return _parseStyleRule(e, Arrays.asList(key), v.toObject().get().children(), f, context, errMsg);
                    }
                    case ARRAY:
                    case FULL_ARRAY:
                    case PARAM_ARRAY:
                    case NAMED_ARRAY: {
                        return _parseStyleRule(e, Arrays.asList(key), v.toArray().get().children(), f, context, errMsg);
                    }
                }
                break;
            }
            case PARAM_OBJECT:
            case PARAM_ARRAY: {
                List<NElement> params = e.asParametrizedContainer().get().params().get();
                List<NElement> children = e.asListContainer().get().children();
                return _parseStyleRule(e, params, children, f, context, errMsg);
            }
            case NAMED_OBJECT:
            case NAMED_ARRAY:
            {
                String name = e.asNamed().get().name().get();
                List<NElement> children = e.asListContainer().get().children();
                return _parseStyleRule(e, Arrays.asList(NElement.ofString(name)), children, f, context, errMsg);
            }
        }
        context.messages().log(errMsg, context.source());
        return NOptional.ofEmpty(errMsg);
    }

    public static NOptional<NTxStyleRule[]> _parseStyleRule(NElement e, List<NElement> selectors, List<NElement> children, NTxDocumentFactory f, NTxNodeFactoryParseContext context, NMsg errMsg) {
        NOptional<NTxStyleRuleSelector> r = parseStyleRuleSelector(selectors, context);
        if (!r.isPresent()) {
            context.messages().log(errMsg, context.source());
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


    public static NOptional<NTxProp[]> parseStyle(NElement e, NTxNodeFactoryParseContext context) {
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
