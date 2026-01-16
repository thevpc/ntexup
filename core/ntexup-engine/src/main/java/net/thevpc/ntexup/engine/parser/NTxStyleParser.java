package net.thevpc.ntexup.engine.parser;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.style.*;
import net.thevpc.ntexup.api.parser.NTxNodeFactoryParseContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.*;

import net.thevpc.nuts.elem.NElement;

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


    public static NOptional<NTxStyleRuleSelector> parseStyleRuleSelector(NElement e, NTxNodeFactoryParseContext context) {
        switch (e.type()) {
            case DOUBLE_QUOTED_STRING:
            case SINGLE_QUOTED_STRING:
            case BACKTICK_STRING:
            case TRIPLE_DOUBLE_QUOTED_STRING:
            case TRIPLE_SINGLE_QUOTED_STRING:
            case TRIPLE_BACKTICK_STRING:
            case LINE_STRING:
            {
                String s = e.asStringValue().get();
                if (s.isEmpty() || s.equals("*")) {
                    return NOptional.of(DefaultNTxNodeSelector.ofAny());
                }
                if (s.startsWith(".")) {
                    return NOptional.of(DefaultNTxNodeSelector.ofClasses(s.substring(1)));
                }
                return NOptional.of(DefaultNTxNodeSelector.ofName(s));
            }
            case NAME: {
                String n = e.asStringValue().get();
                if (n.startsWith(".")) {
                    return NOptional.of(DefaultNTxNodeSelector.ofClasses(n.substring(1)));
                }
                return NOptional.of(DefaultNTxNodeSelector.ofType(n));
            }
            case UPLET:
            case NAMED_UPLET:
            {
                List<String> names = new ArrayList<>();
                List<String> classes = new ArrayList<>();
                List<String> types = new ArrayList<>();
                for (NElement child : e.asUplet().get().params()) {
                    switch (child.type()) {
                        case PAIR: {
                            NTxValue h = NTxValue.of(e.asPair().get().key());
                            NOptional<String> k = h.asStringOrName();
                            if (k.isPresent()) {
                                switch (NTxUtils.uid(k.get())) {
                                    case "class": {
                                        NTxValue h2 = NTxValue.of(e.asPair().get().value());
                                        NOptional<String[]> cc = h2.asStringArrayOrString();
                                        if (cc.isPresent()) {
                                            classes.addAll(Arrays.asList(cc.get()));
                                        } else {
                                            context.messages().log(NMsg.ofC("[%s] invalid style rule selector %s. expected a string or a string array", NTxUtils.shortName(context.source()), e).asSevere(), context.source());
                                            return NOptional.ofEmpty(NMsg.ofC("[%s] invalid style rule selector %s. expected a string or a string array", NTxUtils.shortName(context.source()), e));
                                        }
                                    }
                                    case "name": {
                                        NTxValue h2 = NTxValue.of(e.asPair().get().value());
                                        NOptional<String[]> cc = h2.asStringArrayOrString();
                                        if (cc.isPresent()) {
                                            names.addAll(Arrays.asList(cc.get()));
                                        } else {
                                            context.messages().log(NMsg.ofC("[%s] invalid style rule selector %s. expected a string or a string array.", NTxUtils.shortName(context.source()), e).asSevere(), context.source());
                                            return NOptional.ofEmpty(
                                                    NMsg.ofC("[%s] invalid style rule selector %s. expected a string or a string array.", NTxUtils.shortName(context.source()), e)
                                            );
                                        }
                                    }
                                    case "type": {
                                        NTxValue h2 = NTxValue.of(e.asPair().get().value());
                                        NOptional<String[]> cc = h2.asStringArrayOrString();
                                        if (cc.isPresent()) {
                                            types.addAll(Arrays.asList(cc.get()));
                                        } else {
                                            context.messages().log(NMsg.ofC("[%s] invalid style rule selector %s. expected a valid node type or array", NTxUtils.shortName(context.source()), e).asSevere(), context.source());
                                            return NOptional.ofEmpty(NMsg.ofC("[%s] invalid style rule selector %s. expected a valid node type or array", NTxUtils.shortName(context.source()), e));
                                        }
                                    }
                                    default: {
                                        context.messages().log(NMsg.ofC("[%s] invalid style rule selector %s. expected one of 'name', 'class' or 'type'", NTxUtils.shortName(context.source()), e).asSevere(), context.source());
                                        return NOptional.ofEmpty(NMsg.ofC("[%s] invalid style rule selector %s. expected one of 'name', 'class' or 'type'", NTxUtils.shortName(context.source()), e));
                                    }
                                }
                            } else {
                                context.messages().log(NMsg.ofC("[%s] invalid style rule selector %s. expected one of 'name', 'class' or 'type'", NTxUtils.shortName(context.source()), e).asSevere(), context.source());
                                return NOptional.ofEmpty(NMsg.ofC("[%s] invalid style rule selector %s. expected one of 'name', 'class' or 'type'", NTxUtils.shortName(context.source()), e));
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
                        case DOUBLE_QUOTED_STRING:
                        case SINGLE_QUOTED_STRING:
                        case BACKTICK_STRING:
                        case TRIPLE_DOUBLE_QUOTED_STRING:
                        case TRIPLE_SINGLE_QUOTED_STRING:
                        case TRIPLE_BACKTICK_STRING:
                        case LINE_STRING:
                        {
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
                            context.messages().log(NMsg.ofC("[%s] invalid style rule selector %s", context.source(), e).asSevere(), context.source());
                            return NOptional.ofEmpty(NMsg.ofC("[%s] invalid style rule selector %s", context.source(), e));
                        }
                    }

                }
                return NOptional.of(DefaultNTxNodeSelector.of(
                        names.toArray(new String[0]),
                        types.toArray(new String[0]),
                        classes.toArray(new String[0])
                ));
            }
            default: {
                context.messages().log(NMsg.ofC("[%s] invalid style rule selector %s", NTxUtils.shortName(context.source()), e).asSevere(), context.source());
                return NOptional.ofEmpty(NMsg.ofC("[%s] invalid style rule selector %s", NTxUtils.shortName(context.source()), e));
            }
        }
    }

    public static NOptional<NTxStyleRule[]> parseStyleRule(NElement e, NTxDocumentFactory f, NTxNodeFactoryParseContext context) {
        switch (e.type()) {
            case PAIR: {
                NOptional<NTxStyleRuleSelector> r = parseStyleRuleSelector(e.asPair().get().key(), context);
                if (!r.isPresent()) {
                    return NOptional.ofEmpty(r.getMessage());
                }
                NElement v = e.asPair().get().value();
                switch (v.type()) {
                    case OBJECT:
                    case NAMED_PARAMETRIZED_OBJECT:
                    case PARAMETRIZED_OBJECT:
                    case NAMED_OBJECT:
                    {
                        List<NTxProp> styles = new ArrayList<>();
                        for (NElement el : v.toObject().get().children()) {
                            NOptional<NTxProp[]> s = parseStyle(el, context);
                            if (!s.isPresent()) {
                                s = parseStyle(el, context);
                                return NOptional.ofEmpty(s.getMessage());
                            }
                            styles.addAll(Arrays.asList(s.get()));
                        }
                        return NOptional.of(
                                new NTxStyleRule[]{
                                        DefaultNTxStyleRule.of(context.node(),context.source(),r.get(), styles.toArray(new NTxProp[0]))
                                }
                        );
                    }
                }
                break;
            }
        }
        context.messages().log(NMsg.ofC("[%s] invalid style rule %s", NTxUtils.shortName(context.source()), e).asSevere(), context.source());
        return NOptional.ofEmpty(NMsg.ofC("[%s] invalid style rule %s", NTxUtils.shortName(context.source()), e));
    }


    public static NOptional<NTxProp[]> parseStyle(NElement e, NTxNodeFactoryParseContext context) {
        switch (e.type()) {
            case PAIR: {
                NTxValue h = NTxValue.of(e.asPair().get().key());
                NOptional<String> u = h.asStringOrName();
                if (u.isPresent()) {
                    String uid = NTxUtils.uid(u.get());
                    return NOptional.of(new NTxProp[]{new NTxProp(uid, e.asPair().get().value(),context.node())});
                }
                break;
            }
            case NAME: {
                NTxValue h = NTxValue.of(e);
                NOptional<String> u = h.asStringOrName();
                if (u.isPresent()) {
                    String uid = NTxUtils.uid(u.get());
                    return NOptional.of(new NTxProp[]{new NTxProp(uid, NElement.ofBoolean(true),context.node())});
                }
                break;
            }
        }
        //context.messages().addMessage(HMsg.of(NMsg.ofC("[%s] invalid style %s. expected key:value format", context.source(), e),context.source());
        return NOptional.ofEmpty(NMsg.ofC("[%s] invalid style %s. expected key:value format", NTxUtils.shortName(context.source()), e));
    }

}
