package net.thevpc.ntexup.engine.util;

import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.parser.NTxNodeParser;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.util.NOptional;


import java.util.*;
import java.util.function.Predicate;

public class ToElementHelper {
    List<NElement> args = new ArrayList<>();
    List<NElement> children = new ArrayList<>();
    private final String name;
    private final NTxNode node;
    private Predicate<String> exclude;
    private final Set<String> excludeSet = new HashSet<>();
    private final Set<String> defaultExcludeSet = new HashSet<>(Collections.singletonList(NTxPropName.CLASS));
    private final Set<String> defaultExcludeSemantic = new HashSet<>(Arrays.asList(
            NTxPropName.COLORS,
            NTxPropName.BACKGROUND_COLOR,
            NTxPropName.FOREGROUND_COLOR,
            NTxPropName.DASH,
            NTxPropName.DEBUG,
            NTxPropName.DEBUG_COLOR,
            NTxPropName.TRANSPARENT_COLOR,
            NTxPropName.TOP_COLOR,
            NTxPropName.SHADOW,
            NTxPropName.MARGIN,
            NTxPropName.PADDING,
            NTxPropName.RAISED,
            NTxPropName.THEED,
            NTxPropName.ROUND_CORNER,
            NTxPropName.DRAW_CONTOUR,
            NTxPropName.FILL_BACKGROUND,
            NTxPropName.DRAW_GRID,
            NTxPropName.GRID_COLOR,
            NTxPropName.LINE_COLOR,
            NTxPropName.FONT_STRIKE,
            NTxPropName.FONT_UNDERLINED,
            NTxPropName.FONT_ITALIC,
            NTxPropName.FONT_BOLD,
            NTxPropName.STROKE,
            NTxPropName.FONT_SIZE,
            NTxPropName.FONT_FAMILY
    ));
    private final NTxEngine engine;
    private final boolean semantic;

    public static ToElementHelper of(NTxNode node, boolean semantic, NTxEngine engine) {
        return new ToElementHelper(
                NTxUtils.uid(node.type())
                , node, semantic, engine);
    }

    public ToElementHelper(String name, NTxNode node, boolean semantic, NTxEngine engine) {
        this.name = name;
        this.node = node;
        this.semantic = semantic;
        this.engine = engine;
    }

    private boolean isIncludeProp(NTxProp p) {
        String n = p.getName();

        if (exclude != null && exclude.test(n)) {
            return false;
        }

        if (excludeSet.contains(n)) {
            return false;
        }

        if (defaultExcludeSet.contains(n)) {
            return false;
        }
        if(semantic){
            if(defaultExcludeSemantic.contains(n)){
                return false;
            }
        }
        return true;
    }


    public NElement build() {
        List<NElement> args2 = new ArrayList<>();
        List<NElement> ch = new ArrayList<>();
        args2.addAll(args);
        for (NTxProp p : node.props()) {
            if (isIncludeProp(p)) {
                args2.add(p.toElement());
            }
        }
        if (node.children().size() > 0 || node.rules().length > 0) {
            if (!semantic) {
                NTxStyleRule[] rules = node.rules();
                if (rules.length > 0) {
                    ch.add(
                            NElement.ofPair("styles",
                                    NElement.ofObject(
                                            Arrays.stream(rules).map(x -> x.toElement()).toArray(NElement[]::new)
                                    )
                            )
                    );
                }
            }
            ch.addAll(children);
            for (NTxNode child : node.children()) {
                ch.add(defaultToElement(child));
            }
            NObjectElementBuilder u = NElement.ofObjectBuilder(name).addParams(args2).addAll(ch);
            applyAnnotations(u);
            return u.build();
        } else {
            NUpletElementBuilder u = NElement.ofUplet(name, args2.toArray(new NElement[0])).builder();
            applyAnnotations(u);
            return u.build();
        }
    }

    private NElement defaultToElement(NTxNode child) {
        NOptional<NTxNodeParser> p = engine.nodeTypeParser(child.type());
        if (p.isPresent()) {
            return p.get().toElement(child, semantic, engine);
        }
        NElement raw = child.getRaw();
        switch (NTxUtils.uid(child.type())) {
            case NTxNodeType.CTRL_DEFINE: {
                return raw == null ? NElement.ofNamedUplet("define") : raw;
            }
            case NTxNodeType.BLOCK: {
                return NElement.ofNamedObject("block",
                        child.children().stream().map(x -> defaultToElement(x)).toArray(NElement[]::new)
                );
            }
            case NTxNodeType.FRAGMENT: {
                return NElement.ofNamedObject("fragment",
                        child.children().stream().map(x -> defaultToElement(x)).toArray(NElement[]::new)
                );
            }
            case NTxNodeType.CTRL_UNCOMPILED: {
                return NElement.ofNamedObject("uncompiled",
                        child.getRaw()
                );
            }
        }
        return raw == null ? NElement.ofNamedUplet("unknown") : raw;
    }

    private void applyAnnotations(NElementBuilder u) {
        if (!semantic) {
            NOptional<String[]> sa = NTxValue.of(node.getPropertyValue(NTxPropName.CLASS).orNull()).asStringArrayOrString();
            if (sa.isPresent()) {
                u.addAnnotation(null,
                        Arrays.stream(sa.get()).map(x -> NElement.ofString(x)).toArray(NElement[]::new)
                );
            }
        }
    }

    public ToElementHelper addArgs(NElement... elements) {
        if (elements != null) {
            for (NElement e : elements) {
                addArg(e);
            }
        }
        return this;
    }

    public ToElementHelper addArg(NElement e) {
        if (e != null) {
            args.add(e);
        }
        return this;
    }

    public ToElementHelper addNonNullPairChild(String name, Object value) {
        if (value != null) {
            addChild(NElement.ofPair(name, NTxUtils.toElement(name)));
        }
        return this;
    }

    public ToElementHelper addChild(String name, NElement elem) {
        if (elem != null) {
            addChild(NElement.ofPair(name, elem));
        }
        return this;
    }

    public ToElementHelper addChildrenByName(String... names) {
        for (String s : names) {
            NTxProp p = node.getProperty(s).orNull();
            if (p != null) {
                addChild(NElement.ofPair(name, p.getValue()));
            }
        }
        return this;
    }

    public ToElementHelper addChild(String name, NTxProp elem) {
        if (elem != null) {
            addChild(NElement.ofPair(name, elem.getValue()));
        }
        return this;
    }

    public ToElementHelper addChildren(NElement... elements) {
        if (elements != null) {
            for (NElement e : elements) {
                addChild(e);
            }
        }
        return this;
    }

    public ToElementHelper addChild(NElement e) {
        if (e != null) {
            children.add(e);
        }
        return this;
    }

    public ToElementHelper inlineStringProp(String name) {
        String value = NTxValue.ofProp(node, name).asStringOrName().orNull();
        if (value != null) {
            boolean multiLine =
                    value.indexOf('\n') >= 0
                            || value.indexOf('\r') >= 0;
            if (!multiLine) {
                addArg(NElement.ofString(value));
            } else {
                addArg(NElement.ofString(value, NElementType.TRIPLE_DOUBLE_QUOTED_STRING));
            }
        }
        excludeProps(name);
        return this;
    }

    public ToElementHelper excludeProps(String prop) {
        if (!NBlankable.isBlank(prop)) {
            excludeSet.add(NTxUtils.uid(prop));
        }
        return this;
    }

    public ToElementHelper addChildProps(String[] propNames) {
        if (propNames != null) {
            for (String propName : propNames) {
                if (propName != null) {
                    NElement v = node.getPropertyValue(propName).orNull();
                    if (v != null) {
                        addChild(NElement.ofPair(propName, v));
                    }
                }
            }
        }
        return this;
    }
}
