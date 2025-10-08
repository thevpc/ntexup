package net.thevpc.ntexup.engine.document;

import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.*;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.NTxAlign;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NIllegalArgumentException;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.util.*;


import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DefaultNTxNode implements NTxNode {
    private String uuid;
    private String nodeType;
    private NTxSource source;
    protected NTxItem parent;
    private NTxProperties properties;
    private Map<String, NElement> vars = new LinkedHashMap<>();
    private Map<String, Object> userObjects;
    private Map<String, Object> renderCache;
    private List<NTxNode> children = new ArrayList<>();
    private List<NTxStyleRule> styleRules = new ArrayList<>();
    private List<NTxNodeDef> definitions = new ArrayList<>();
    private List<NTxFunction> functions = new ArrayList<>();
    private List<NTxNode> hierarchy = new ArrayList<>();
    private NTxNodeDef templateDefinition;

    public static DefaultNTxNode ofText(String message) {
        DefaultNTxNode t = new DefaultNTxNode(NTxNodeType.TEXT);
        t.setProperty(NTxPropName.VALUE, NElement.ofString(message));
        return t;
    }

    public static DefaultNTxNode ofAssign(String name, NElement value, NTxSource source) {
        DefaultNTxNode n = new DefaultNTxNode(NTxNodeType.CTRL_ASSIGN, source);
        n.setProperty(NTxPropName.NAME, NTxUtils.addCompilerDeclarationPath(NElement.ofString(name), source));
        n.setProperty(NTxPropName.VALUE, NTxUtils.addCompilerDeclarationPath(value, source));
        return n;
    }

    public static DefaultNTxNode ofAssignIfEmpty(String name, NElement value, NTxSource source) {
        DefaultNTxNode n = new DefaultNTxNode(NTxNodeType.CTRL_ASSIGN, source);
        n.setProperty(NTxPropName.NAME, NTxUtils.addCompilerDeclarationPath(NElement.ofString(name), source));
        n.setProperty(NTxPropName.VALUE, NTxUtils.addCompilerDeclarationPath(value, source));
        n.setProperty("ifempty", NElement.ofBoolean(true));
        return n;
    }

    public static DefaultNTxNode ofExpr(NElement value, NTxSource source) {
        DefaultNTxNode n = new DefaultNTxNode(NTxNodeType.CTRL_EXPR, source);
        n.setProperty(NTxPropName.VALUE, NTxUtils.addCompilerDeclarationPath(value, source));
        return n;
    }

    public DefaultNTxNode(String nodeType) {
        this(nodeType, null);
    }

    public DefaultNTxNode(String nodeType, NTxSource source) {
        this.nodeType = nodeType;
        this.uuid = UUID.randomUUID().toString();
        this.source = source;
        properties = new NTxProperties(this);
    }

    @Override
    public String uuid() {
        return uuid;
    }

    public NTxNode setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    @Override
    public String type() {
        return this.nodeType;
    }


    @Override
    public String[] getStyleClasses() {
        NOptional<NElement> style = getPropertyValue(NTxPropName.CLASS);
        if (style.isEmpty()) {
            return new String[0];
        }
        String[] v = NTxValue.of(style.get()).asStringArrayOrString().orNull();
        if (v == null) {
            return new String[0];
        }
        return Arrays.stream(v).filter(x -> !NBlankable.isBlank(x))
                .map(NTxUtils::uid)
                .distinct()
                .toArray(String[]::new)
                ;
    }

    public NTxSource source() {
        return source;
    }


    @Override
    public List<NTxProp> props() {
        return new ArrayList<>(properties.toSet());
    }

    public NTxNode setSource(NTxSource source) {
        this.source = source;
        return this;
    }

    @Override
    public boolean isDisabled() {
        NOptional<NElement> style = getPropertyValue(NTxPropName.HIDE);
        if (style.isEmpty()) {
            return false;
        }
        NElement v = style.get();
        if (v == null) {
            return false;
        }
        return v.asBooleanValue().get();
    }

    @Override
    public NTxNode setDisabled(boolean disabled) {
        setProperty(NTxProps.disabled(disabled));
        return this;
    }

    public String name() {
        NOptional<NElement> style = getPropertyValue(NTxPropName.NAME);
        if (style.isEmpty()) {
            return null;
        }
        NElement v = style.get();
        if (v == null) {
            return null;
        }
        return v.asStringValue().get();
    }

    @Override
    public NTxNode setName(String name) {
        setProperty(NTxProps.name(name));
        return this;
    }

    @Override
    public NOptional<NElement> getPropertyValue(String... propertyNames) {
        return getProperty(propertyNames).map(x -> x.getValue());
    }

    @Override
    public NOptional<NElement> getVar(String property) {
        NElement u = vars.get(property);
        if (u != null) {
            NTxSource source = NTxUtils.sourceOf(this);
            if (source != null) {
                u = NTxUtils.addCompilerDeclarationPath(u, source);
            }
        }
        return NOptional.ofNamed(u, property);
    }

    @Override
    public Map<String, NElement> getVars() {
        return new LinkedHashMap<>(vars);
    }


    @Override
    public NTxNode setUserObject(String name, Object value) {
        if (value == null) {
            if(userObjects!=null) {
                userObjects.remove(name);
            }
        } else {
            if(userObjects==null) {
                userObjects=new HashMap<>();
            }
            userObjects.put(name, value);
        }
        return this;
    }

    @Override
    public NOptional<Object> getUserObject(String property) {
        if(userObjects!=null) {
            Object u = userObjects.get(property);
            if(u!=null) {
                return NOptional.of(u);
            }
        }
        return NOptional.ofNamedEmpty(property);
    }

    @Override
    public <T> NOptional<T> getAndSetUserObject(String property, boolean force,Supplier<T> defaultValue) {
        if(!force){
            NOptional<T> ol = (NOptional) this.getUserObject(property);
            if(ol.isPresent()){
               return ol;
            }
        }
        T nv = defaultValue.get();
        if(nv==null){
            setUserObject(property,null);
        }else{
            setUserObject(property,nv);
        }
        return NOptional.ofNamed(nv,property);
    }

    @Override
    public <T> NOptional<T> getAndSetUserObject(Class<T> property, boolean force,Supplier<T> defaultValue) {
        return getAndSetUserObject(property.getName(), force, defaultValue);
    }

    public NOptional<NTxProp> getProperty(String... propertyNames) {
        return properties.get(propertyNames);
    }

    public NTxNode setProperty(NTxProp prop) {
        if (false) {
            String s = NTxUtils.findCompilerDeclarationPath(prop.getValue()).orNull();
            if (s == null) {
                throw new NIllegalArgumentException(NMsg.ofC("var value %s=%s is missing CompilerDeclarationPath", prop.getName(), prop.getValue()));
            }
        }
        properties.set(prop);
        return this;
    }

    public NTxNode unsetProperty(String s) {
        properties.unset(s);
        return this;
    }

    @Override
    public NTxNode setProperty(String name, NElement value) {
        if (value != null) {
            setProperty(NTxProp.of(name, value));
        } else {
            unsetProperty(name);
        }
        return this;
    }

    @Override
    public NTxNode setProperty(String name, NToElement value) {
        if (value != null) {
            setProperty(NTxProp.of(name, value));
        } else {
            unsetProperty(name);
        }
        return this;
    }


    @Override
    public NTxNode setVar(String name, NElement value) {
        if (value == null) {
            vars.remove(name);
        } else {
            if (false) {
                String s = NTxUtils.findCompilerDeclarationPath(value).orNull();
                if (s == null) {
                    throw new NIllegalArgumentException(NMsg.ofC("var value %s=%s is missing CompilerDeclarationPath", name, value));
                }
            }
            vars.put(name, value);
        }
        return this;
    }


    @Override
    public NTxItem parent() {
        return parent;
    }


    private Set<String> _oldClassNames() {
        NOptional<NTxProp> y = properties.get(NTxPropName.CLASS);
        if (y.isPresent()) {
            return new LinkedHashSet<>(Arrays.asList(NTxValue.of(y.get().getValue()).asStringArray().orElse(new String[0])).stream()
                    .filter(x -> x != null && x.trim().length() > 0)
                    .map(String::trim)
                    .collect(Collectors.toList()));
        }
        return new HashSet<>();
    }

    @Override
    public NTxNode addStyleClass(String className) {
        className = validateClassName(className);
        if (className != null) {
            Set<String> s = _oldClassNames();
            s.add(className);
            setProperty(NTxProps.styleClasses(s.toArray(new String[0])));
        }
        return this;
    }

    @Override
    public NTxItem setStyleClasses(String[] classNames) {
        Set<String> s = new HashSet<>();
        if (classNames != null) {
            for (String c : classNames) {
                c = validateClassName(c);
                if (c != null) {
                    s.add(c);
                }
            }
        }
        setProperty(NTxProps.styleClasses(s.toArray(new String[0])));
        return this;
    }

    @Override
    public NTxNode addStyleClasses(String... classNames) {
        if (classNames != null) {
            Set<String> s = _oldClassNames();
            for (String c : classNames) {
                c = validateClassName(c);
                if (c != null) {
                    s.add(c);
                }
            }
            setProperty(NTxProps.styleClasses(s.toArray(new String[0])));
        }
        return this;
    }

    private static String validateClassName(String className) {
        className = NStringUtils.trimToNull(className);
        if (className != null) {
            className = NNameFormat.LOWER_SNAKE_CASE.format(className);
        }
        return className;
    }

    @Override
    public NTxNode removeClass(String className) {
        className = validateClassName(className);
        if (className != null) {
            Set<String> s = _oldClassNames();
            s.remove(className);
            setProperty(NTxProps.styleClasses(s.toArray(new String[0])));
        }
        return this;
    }

    @Override
    public boolean hasClass(String className) {
        className = validateClassName(className);
        if (className != null) {
            return _oldClassNames().contains(className);
        }
        return false;
    }

    @Override
    public Set<String> styleClasses() {
        return _oldClassNames();
    }

    @Override
    public NTxNode setParent(NTxItem parent) {
        this.parent = parent;
        return this;
    }

    @Override
    public NTxNode mergeNodes(NTxItem... other) {
        if (other != null) {
            for (NTxItem o : other) {
                mergeNode(o);
            }
        }
        return this;
    }

    @Override
    public NTxNode addAll(NTxItem... other) {
        if (other != null) {
            for (NTxItem o : other) {
                add(o);
            }
        }
        return this;
    }

    @Override
    public NTxNode mergeNode(NTxItem other) {
        if (other != null) {
            if (other instanceof NTxItemList) {
                for (NTxItem item : ((NTxItemList) other).getItems()) {
                    mergeNode(item);
                }
            } else if (other instanceof NTxProp) {
                setProperty((NTxProp) other);
            } else if (other instanceof NTxNodeDef) {
                addDefinition((NTxNodeDef) other);
            } else if (other instanceof NTxNode) {
                NTxNode hn = (NTxNode) other;
                if (this.source == null) {
                    this.source = hn.source();
                }
                this.properties.set(hn.props());
                addRules(hn.rules());
                for (NTxNode child : hn.children()) {
                    addChild(child);
                }
            }
        }
        return this;
    }

    @Override
    public NTxNode add(NTxItem other) {
        if (other != null) {
            if (other instanceof NTxItemList) {
                for (NTxItem item : ((NTxItemList) other).getItems()) {
                    add(item);
                }
            } else if (other instanceof NTxProp) {
                setProperty((NTxProp) other);
            } else if (other instanceof NTxNodeDef) {
                addDefinition((NTxNodeDef) other);
            } else if (other instanceof NTxNode) {
                addChild((NTxNode) other);
            } else if (other instanceof NTxStyleRule) {
                addRule((NTxStyleRule) other);
            }
        }
        return this;
    }

    @Override
    public boolean append(NTxItem a) {
        if (a != null) {
            if (a instanceof NTxItemList) {
                boolean b = false;
                for (NTxItem item : ((NTxItemList) a).getItems()) {
                    b |= append(item);
                }
                return b;
            } else if (a instanceof NTxProp) {
                setProperty((NTxProp) a);
                return true;
            } else if (a instanceof NTxStyleRule) {
                addRule((NTxStyleRule) a);
                return true;
            } else if (a instanceof NTxNode) {
                addChild((NTxNode) a);
                return true;
            } else if (a instanceof NTxNodeDef) {
                addDefinition((NTxNodeDef) a);
                return true;
            }
        }
        return false;
    }

    @Override
    public NTxNode setPosition(NTxAlign align) {
        setProperty(NTxPropName.POSITION, align);
        return this;
    }

    @Override
    public NTxNode setPosition(Number x, Number y) {
        setPosition(new NTxDouble2(x, y));
        return this;
    }

    @Override
    public NTxNode setPosition(NTxDouble2 d) {
        setProperty(NTxPropName.POSITION, d);
        return this;
    }

    @Override
    public NTxNode setOrigin(NTxAlign align) {
        setProperty(NTxPropName.ORIGIN, align);
        return this;
    }

    @Override
    public NTxNode setOrigin(Number x, Number y) {
        setOrigin(new NTxDouble2(x, y));
        return this;
    }

    @Override
    public NTxNode setOrigin(NTxDouble2 d) {
        setProperty(NTxPropName.ORIGIN, d);
        return this;
    }

    @Override
    public NTxNode at(NTxAlign align) {
        setPosition(align);
        setOrigin(align);
        return this;
    }

    @Override
    public NTxNode at(Number x, Number y) {
        setPosition(x, y);
        setOrigin(x, y);
        return this;
    }

    @Override
    public NTxNode at(NTxDouble2 d) {
        setPosition(d);
        setOrigin(d);
        return this;
    }

    @Override
    public NTxNode setSize(Number size) {
        return setSize(new NTxDouble2(size, size));
    }

    @Override
    public NTxNode setSize(NTxDouble2 size) {
        NElement old = getPropertyValue(NTxPropName.SIZE).orNull();
        NTxDouble2 oo = old == null ? null : NTxValue.of(old).asDouble2().orNull();
        if (oo == null) {
            oo = new NTxDouble2(null, null);
        }
        Number w = size == null ? null : size.getX();
        Number h = size == null ? null : size.getY();
        setProperty(NTxPropName.SIZE, new NTxDouble2(
                NUtils.firstNonNull(w, oo.getX()),
                NUtils.firstNonNull(h, oo.getY())
        ));
        return this;
    }

    @Override
    public NTxNode setSize(Number w, Number h) {
        setSize(new NTxDouble2(w, h));
        return this;
    }

    @Override
    public NTxNode setFontSize(Number w) {
        setProperty(NTxPropName.FONT_SIZE, NElement.ofNumber(w));
        return this;
    }

    @Override
    public NTxNode setFontFamily(String w) {
        setProperty(NTxPropName.FONT_FAMILY, NElement.ofString(w));
        return this;
    }

    @Override
    public NTxNode setFontBold(Boolean w) {
        setProperty(NTxPropName.FONT_BOLD, NElement.ofBoolean(w));
        return this;
    }

    @Override
    public NTxNode setFontItalic(Boolean w) {
        setProperty(NTxPropName.FONT_ITALIC, NElement.ofBoolean(w));
        return this;
    }

    @Override
    public NTxNode setFontUnderlined(Boolean w) {
        setProperty(NTxPropName.FONT_UNDERLINED, NElement.ofBoolean(w));
        return this;
    }

    @Override
    public NTxNode setFontStrike(Boolean w) {
        setProperty(NTxPropName.FONT_STRIKE, NElement.ofBoolean(w));
        return this;
    }

    @Override
    public NTxNode setForegroundColor(String w) {
        setProperty(NTxPropName.FOREGROUND_COLOR, NElement.ofString(w));
        return this;
    }

    @Override
    public NTxNode setBackgroundColor(String w) {
        setProperty(NTxPropName.BACKGROUND_COLOR, NElement.ofString(w));
        return this;
    }

//    @Override
//    public NTxNode setLineColor(String w) {
//        setProperty(NTxPropName.LINE_COLOR, w);
//        return this;
//    }

    @Override
    public NTxNode setGridColor(String w) {
        setProperty(NTxPropName.GRID_COLOR, NElement.ofString(w));
        return this;
    }

    @Override
    public List<NTxProp> getProperties() {
        return new ArrayList<>(properties.toSet());
    }

    @Override
    public NTxNode setProperties(NTxProp... props) {
        if (props != null) {
            for (NTxProp prop : props) {
                setProperty(prop);
            }
        }
        return this;
    }

    @Override
    public NTxNode setChildren(NTxNode... children) {
        this.children.clear();
        addChildren(children);
        return this;
    }

    @Override
    public NTxNode addChildren(NTxNode... a) {
        if (a != null) {
            for (NTxNode n : a) {
                addChild(n);
            }
        }
        return this;
    }

    @Override
    public NTxNode addChild(NTxNode a) {
        if (a != null) {
            children.add(a);
            a.setParent(this);
            NTxUtils.checkNode(a, true);
        }
        return this;
    }

    @Override
    public List<NTxNode> children() {
        return children;
    }

    @Override
    public NTxNode addRule(NTxStyleRule s) {
        if (s != null) {
            if (!styleRules.contains(s)) {
                styleRules.add(s);
            }
        }
        return this;
    }

    @Override
    public NTxNode setRules(NTxStyleRule[] rules) {
        styleRules.clear();
        addRules(rules);
        return this;
    }

    @Override
    public NTxNode addRules(NTxStyleRule... rules) {
        if (rules != null) {
            for (NTxStyleRule rule : rules) {
                addRule(rule);
            }
        }
        return this;
    }

    @Override
    public NTxNode removeRule(NTxStyleRule s) {
        styleRules.remove(s);
        return this;
    }

    @Override
    public NTxNode clearRules() {
        for (NTxStyleRule rule : rules()) {
            removeRule(rule);
        }
        return this;
    }

    @Override
    public NTxNode clearChildren() {
        children.clear();
        return this;
    }

    @Override
    public NTxNode clearDefinitions() {
        definitions.clear();
        return this;
    }

    public NTxNodeDef[] definitions() {
        return definitions.toArray(new NTxNodeDef[0]);
    }

    @Override
    public NTxNode addDefinition(NTxNodeDef s) {
        if (s != null) {
            definitions.add(s);
        }
        return this;
    }

    @Override
    public NTxNode removeNodeDefinition(NTxNodeDef s) {
        if (s != null) {
            definitions.remove(s);
        }
        return this;
    }

    @Override
    public NTxStyleRule[] rules() {
        return styleRules.toArray(new NTxStyleRule[0]);
    }

    public NTxNode copy() {
        DefaultNTxNode o = new DefaultNTxNode(nodeType, source());
        copyTo(o);
        return o;
    }

    public NTxNode copyTo(NTxNode other) {
        other.setUuid(UUID.randomUUID().toString());
        other.setSource(source());
        other.setProperties(properties.toArray());
        other.addChildren(children().stream().map(NTxNode::copy).toArray(NTxNode[]::new));
        other.addRules(Arrays.stream(rules()).toArray(NTxStyleRule[]::new));
        for (Map.Entry<String, NElement> e : vars.entrySet()) {
            other.setVar(e.getKey(), e.getValue());
        }
        if(userObjects!=null) {
            for (Map.Entry<String, Object> e : userObjects.entrySet()) {
                other.setUserObject(e.getKey(), e.getValue());
            }
        }
        for (NTxNodeDef v : definitions) {
            other.addDefinitions(v);
        }
        for (NTxFunction v : functions) {
            other.addNodeFunction(v);
        }
        for (NTxNode h : hierarchy) {
            other.addHierarchy(h);
        }
        other.setTemplateDefinition(templateDefinition);
        return this;
    }

    public NTxNode reset() {
        uuid = null;
        source = null;
        parent = null;
        if (properties != null) {
            properties.clear();
        }
        vars.clear();
        children.clear();
        styleRules.clear();
        definitions.clear();
        functions.clear();
        hierarchy.clear();
        if(userObjects!=null){
            userObjects.clear();
        }
        templateDefinition = null;
        return this;
    }

    public NTxNode copyFrom(NTxNode other) {
        other.copyTo(this);
        return this;
    }

    @Override
    public NTxNode addDefinitions(NTxNodeDef... definitions) {
        for (NTxNodeDef definition : definitions) {
            addDefinition(definition);
        }
        return this;
    }

    //    @Override
//    public NTxNode addRule(HProp... s) {
//        if (s != null) {
//            Set<NTxProp> ss = Arrays.stream(s).filter(x -> x != null).collect(Collectors.toSet());
//            if (!ss.isEmpty()) {
//                addRule(DefaultHStyleRule.ofAny(s));
//            }
//        }
//        return this;
//    }
//
    private NElement toElem0() {
        if (NTxNodeType.CTRL_ASSIGN.equals(type())) {
            return NElement.ofPair("$" + getName(), getPropertyValue(NTxPropName.VALUE).orNull());
        }
        String componentName=getPropertyValue(NTxPropName.VALUE).flatMap(x->x.asStringValue()).orNull();
        String[] styleClasses = getStyleClasses();
        if (!styleRules.isEmpty() || !children.isEmpty()) {
            NObjectElementBuilder o = NElement.ofObjectBuilder(nodeType);
            if (styleClasses.length > 0) {
                o.addAnnotation(null, Arrays.stream(styleClasses).map(x -> NElement.ofString(x)).toArray(NElement[]::new));
            }
            if(!NBlankable.isBlank(componentName)){
                o.add(NElement.ofPair("componentName", NElement.ofString(componentName)));
            }
            if (source != null) {
                o.add(NElement.ofPair("source", NElement.ofString(source.shortName())));
            }
            for (NTxProp p : properties.toList()) {
                switch (p.getName()) {
                    case NTxPropName.CLASS: {
                        break;
                    }
                    default: {
                        o.addParam(p.toElement());
                    }
                }
            }
            if (!styleRules.isEmpty()) {
                o.add("rules", NElement.ofObject(styleRules.stream().map(x -> x.toElement()).toArray(NElement[]::new)));
            }
            for (NTxNode child : children()) {
                if (child instanceof DefaultNTxNode) {
                    o.add(((DefaultNTxNode) child).toElem0());
                } else {
                    o.add(NElement.ofString(child.toString()));
                }
            }
            return o.build();
        } else {
            NUpletElementBuilder o = NElement.ofUpletBuilder().name(nodeType);
            if (styleClasses.length > 0) {
                o.addAnnotation(null, Arrays.stream(styleClasses).map(x -> NElement.ofString(x)).toArray(NElement[]::new));
            }
            if(!NBlankable.isBlank(componentName)){
                o.add(NElement.ofPair("componentName", NElement.ofString(componentName)));
            }
            if (source != null) {
                o.add(NElement.ofPair("source", NElement.ofString(String.valueOf(source))));
            }
            for (NTxProp p : properties.toList()) {
                switch (p.getName()) {
                    case NTxPropName.CLASS: {
                        break;
                    }
                    default: {
                        o.add(p.toElement());
                    }
                }
            }
            return o.build();
        }
    }

    @Override
    public String toString() {
        return toElem0().toString();
    }

    @Override
    public String getName() {
        String n = NTxValue.of(getPropertyValue(NTxPropName.NAME).orNull()).asStringOrName().orNull();
        if (n != null) {
            return n;
        }
        return null;
    }

    @Override
    public String getComponentName() {
        String n = NTxValue.of(getPropertyValue(NTxPropName.COMPONENT_NAME).orNull()).asStringOrName().orNull();
        if (n != null) {
            return n;
        }
        return null;
    }

    @Override
    public void setChildAt(int i, NTxNode c) {
        NAssert.requireNonNull(c, "node");
        children.set(i, c);
    }

    @Override
    public NTxFunction[] nodeFunctions() {
        return functions.toArray(new NTxFunction[0]);
    }

    @Override
    public NTxNode addNodeFunction(NTxFunction s) {
        if (s != null) {
            functions.add(s);
        }
        return this;
    }

    @Override
    public NTxNode addNodeFunctions(NTxFunction... definitions) {
        if (definitions != null) {
            for (NTxFunction s : definitions) {
                addNodeFunction(s);
            }
        }
        return this;
    }

    @Override
    public NTxNode removeNodeFunction(String s) {
        functions.removeIf(f -> NNameFormat.equalsIgnoreFormat(f.name(), s));
        return this;
    }

    public NTxNode addHierarchy(NTxNode n) {
        if (n != null) {
            hierarchy.add(n);
        }
        return this;
    }

    public NTxNode removeHierarchy(NTxNode n) {
        if (n != null) {
            hierarchy.remove(n);
        }
        return this;
    }

    public List<NTxNode> hierarchy() {
        return hierarchy;
    }

    public NTxNode setTemplateDefinition(NTxNodeDef n) {
        this.templateDefinition = n;
        return this;
    }

    public NTxNodeDef templateDefinition() {
        return templateDefinition;
    }

    // -------------------------------------------


    @Override
    public NTxNode setRenderCache(String name, Object value) {
        if (value == null) {
            if(renderCache!=null) {
                renderCache.remove(name);
            }
        } else {
            if(renderCache==null) {
                renderCache=new HashMap<>();
            }
            renderCache.put(name, value);
        }
        return this;
    }

    @Override
    public NOptional<Object> getRenderCache(String property) {
        if(renderCache!=null) {
            Object u = renderCache.get(property);
            if(u!=null) {
                return NOptional.of(u);
            }
        }
        return NOptional.ofNamedEmpty(property);
    }

    @Override
    public <T> NOptional<T> getAndSetRenderCache(String property, boolean force,Supplier<T> defaultValue) {
        if(!force){
            NOptional<T> ol = (NOptional) this.getRenderCache(property);
            if(ol.isPresent()){
                return ol;
            }
        }
        T nv = defaultValue.get();
        if(nv==null){
            setRenderCache(property,null);
        }else{
            setRenderCache(property,nv);
        }
        return NOptional.ofNamed(nv,property);
    }

    @Override
    public <T> NOptional<T> getAndSetRenderCache(Class<T> property, boolean force,Supplier<T> defaultValue) {
        return getAndSetRenderCache(property.getName(), force, defaultValue);
    }

    public void invalidateRenderCache(){
        renderCache=null;
        if(children!=null){
            for (NTxNode child : children) {
                child.invalidateRenderCache();
            }
        }
    }

}
