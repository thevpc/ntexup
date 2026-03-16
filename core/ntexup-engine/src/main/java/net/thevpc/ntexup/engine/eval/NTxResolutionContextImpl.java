package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeDef;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxVar;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.parser.NTxItemParser;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.util.NTxElementUtils;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class NTxResolutionContextImpl implements NTxResolutionContext {
    protected static AtomicInteger ID = new AtomicInteger();
    protected String uid;
    protected NElement element;
    protected NTxNode node;
    protected NTxResolutionContext parentContext;
    protected NTxNode parent;
    protected NTxNode[] path;
    protected NTxNodeDef def;
    //    NTxNode hierarchy;
    protected boolean isInPage;
    protected final NTxEngine engine;
    protected final NTxDocument document;
    protected Map<String, NTxVar> vars = new LinkedHashMap<>();
    protected Map<String, NTxNodeDef> definitions = new LinkedHashMap<>();
    protected Map<String, NTxFunction> functions = new LinkedHashMap<>();
    protected NTxItemParser itemParser;
    protected NTxCompiledDocument compiledDocument;
    protected NTxCompiledPage compiledPage;

    public NTxResolutionContextImpl(NTxNode[] path, NElement element, NTxNodeDef def, boolean isInPage, NTxEngine engine, NTxDocument document, Map<String, NTxVar> vars, Map<String, NTxNodeDef> definitions, Map<String, NTxFunction> functions,
                                    NTxCompiledDocument compiledDocument,
                                    NTxCompiledPage compiledPage,
                                    NTxResolutionContext parentContext, NTxItemParser itemParser) {
        this.uid = ((parentContext != null) ? (parentContext.uid() + "/") : "") + ID.incrementAndGet();
        this.path = Arrays.copyOf(path, path.length);
        this.node = path[path.length - 1];
        this.parent = (path.length - 2 >= 0) ? path[path.length - 2] : null;
        this.def = def;
        this.element = element;
        this.isInPage = isInPage;
        this.engine = engine;
        this.document = document;
        this.compiledPage = compiledPage;
        this.compiledDocument = compiledDocument;

        if (parentContext != null) {
            this.vars.putAll(((NTxResolutionContextImpl) parentContext).getVars());
        }
        if (vars != null) {
            this.vars.putAll(vars);
        }

        if (parentContext != null) {
            this.definitions.putAll(((NTxResolutionContextImpl) parentContext).getDefinitions());
        }
        if (definitions != null) {
            this.definitions.putAll(definitions);
        }

        if (parentContext != null) {
            this.functions.putAll(((NTxResolutionContextImpl) parentContext).getFunctions());
        }
        if (functions != null) {
            this.functions.putAll(functions);
        }

        this.parentContext = parentContext;
        this.itemParser = itemParser;
    }

    @Override
    public NTxCompiledPage compiledPage() {
        return compiledPage;
    }

    @Override
    public NTxCompiledDocument compiledDocument() {
        return compiledDocument;
    }


    public String uid() {
        return uid;
    }

    protected NTxResolutionContext copyAs(NTxNode[] path, NElement element, NTxNodeDef def, boolean isInPage, NTxEngine engine, NTxDocument document, Map<String, NTxVar> vars, Map<String, NTxNodeDef> definitions, Map<String, NTxFunction> functions,
                                          NTxCompiledDocument compiledDocument,
                                          NTxCompiledPage compiledPage,
                                          NTxResolutionContext parentContext, NTxItemParser nodeParserFactory) {
        return new NTxResolutionContextImpl(path, element, def, isInPage, engine, document, vars, definitions, functions, compiledDocument, compiledPage, parentContext, nodeParserFactory);
    }

    public NTxItemParser itemParser() {
        if (itemParser != null) {
            return itemParser;
        }
        if (parentContext != null) {
            return parentContext.itemParser();
        }
        return null;
    }

    @Override
    public NTxResolutionContext pushNode(NTxNode node) {
        NAssert.requireNamedNonNull(node, "node");
        List<NTxNode> all = new ArrayList<>(Arrays.asList(path));
        all.add(NAssert.requireNamedNonNull(node, "parent"));
        setPath(all.toArray(new NTxNode[0]));
        return this;
    }

    @Override
    public NTxResolutionContext popNode() {
        List<NTxNode> all = new ArrayList<>(Arrays.asList(path));
        all.remove(all.size() - 1);
        setPath(all.toArray(new NTxNode[0]));
        return this;
    }

    public NTxResolutionContext setPath(NTxNode[] path) {
        this.path = Arrays.copyOf(path, path.length);
        this.node = path[path.length - 1];
        this.parent = (path.length - 2 >= 0) ? path[path.length - 2] : null;
        return this;
    }

    @Override
    public NElement element() {
        return element;
    }

    @Override
    public NTxNode parent() {
        return parent;
    }

    @Override
    public NTxNode node() {
        return node;
    }

    public NTxNode[] path() {
        return Arrays.copyOf(path, path.length);
    }

    @Override
    public NTxSource source() {
        return node.source();
    }

    @Override
    public NTxNodeDef def() {
        return def;
    }

    @Override
    public boolean inPage() {
        return isInPage;
    }

    public NTxDocument document() {
        return document;
    }

    public NTxEngine engine() {
        return engine;
    }

    @Override
    public NTxResolutionContext copy() {
        return copyAs(path, element, def, isInPage, engine, document, vars, definitions, functions, compiledDocument,compiledPage, parentContext, itemParser);
    }

    @Override
    public NTxResolutionContext doWithChild(NTxNode newPush, Consumer<NTxResolutionContext> me) {
        try {
            pushNode(newPush);
            me.accept(this);
        } finally {
            popNode();
        }
        return this;
    }

    @Override
    public NTxResolutionContext doWithElement(NElement element, Consumer<NTxResolutionContext> me) {
        NElement old = element();
        try {
            setElement(element);
            me.accept(this);
        } finally {
            setElement(old);
        }
        return this;
    }

    @Override
    public NTxResolutionContext doWithChild(NTxNode newPush, NTxNodeDef d, Consumer<NTxResolutionContext> me) {
        NTxNodeDef d0 = def();
        try {
            pushNode(newPush);
            me.accept(this);
        } finally {
            popNode();
            setDef(d0);
        }
        return this;
    }

    @Override
    public NTxResolutionContext doWithSibling(NTxNode node, Consumer<NTxResolutionContext> me) {
        NTxNode[] p = path();
        try {
            setNode(node);
            me.accept(this);
        } finally {
            setPath(p);
        }
        return this;
    }

    @Override
    public NTxResolutionContext doWithSibling(NTxNode node, NTxNodeDef d, Consumer<NTxResolutionContext> me) {
        NTxNode[] p = path();
        NTxNodeDef d0 = def();
        try {
            setNode(node);
            me.accept(this);
        } finally {
            setPath(p);
            setDef(d0);
        }
        return this;
    }


    @Override
    public NTxResolutionContext resolveNode(NTxNode next) {
        return copy().pushNode(next);
    }

    @Override
    public NTxResolutionContext setNode(NTxNode node) {
        NAssert.requireNamedNonNull(node, "node");
        NTxNode[] path = Arrays.copyOf(this.path, this.path.length);
        path[path.length - 1] = node;
        setPath(path);
        return this;
    }

    @Override
    public NTxResolutionContext setElement(NElement element) {
        this.element = element;
        return this;
    }

    @Override
    public NTxResolutionContext withNode(NTxNode next) {
        NTxNode n = NAssert.requireNamedNonNull(next, "parent");
        return copy().setNode(n);
    }

    @Override
    public NTxResolutionContext withElement(NElement element) {
        if (Objects.equals(element, this.element)) {
            return this;
        }
        return copy().setElement(element);
    }

    @Override
    public NTxResolutionContext setInPage(boolean isInPage) {
        this.isInPage = isInPage;
        return this;
    }

    public NTxResolutionContext withInPage(boolean isInPage) {
        if (this.isInPage == isInPage) {
            return this;
        }
        return copyAs(path, element, def, isInPage, engine, document, vars, definitions, functions, compiledDocument,compiledPage, parentContext, itemParser);
    }

    @Override
    public NTxResolutionContext withParentOnly(NTxNode parent) {
        if (parent == this.node) {
            return this;
        }
        List<NTxNode> all = new ArrayList<>(Arrays.asList(path));
        all.add(NAssert.requireNamedNonNull(parent, "parent"));
        return copyAs(all.toArray(new NTxNode[0]), element, null, isInPage, engine, document, vars, definitions, functions, compiledDocument,compiledPage, parentContext, itemParser);
    }

    @Override
    public NTxResolutionContext withParentOnly() {
        if (def == null) {
            return this;
        }
        return copyAs(path, element, null, isInPage, engine, document, vars, definitions, functions, compiledDocument,compiledPage, parentContext, itemParser);
    }

    @Override
    public NTxResolutionContext setDef(NTxNodeDef def) {
        this.def = def;
        return this;
    }

    @Override
    public NTxResolutionContext withDef(NTxNodeDef def) {
        if (def == this.def) {
            return this;
        }
        return copyAs(path, element, def, isInPage, engine, document, vars, definitions, functions, compiledDocument,compiledPage, parentContext, itemParser);
    }

    @Override
    public NTxResolutionContext setVar(String name, NTxVar value) {
        if (name == null) {
            return this;
        }
        NTxVar old = this.vars.get(name);
        if (!Objects.equals(value, old)) {
            if (value == null) {
                vars.remove(name);
            } else {
                vars.put(name, value);
            }
        }
        return this;
    }

    @Override
    public NTxResolutionContext withVar(String name, NTxVar value) {
        if (name == null) {
            return this;
        }
        Object old = this.vars.get(name);
        if (Objects.equals(value, old)) {
            return this;
        }
        Map<String, NTxVar> vars2 = new LinkedHashMap<>(vars);
        if (value == null) {
            vars2.remove(name);
        } else {
            vars2.put(name, value);
        }
        return copyAs(path, element, def, isInPage, engine, document, vars2, definitions, functions, compiledDocument,compiledPage, parentContext, itemParser);
    }

    @Override
    public NOptional<NElement> getVarValue(String name) {
        return getVar(name).map(NTxVar::get);
    }

    public NOptional<NTxVar> getVar(String varName) {
        NTxVar value = vars.get(varName);
        if (value != null) {
            return NOptional.ofNamed(value, varName);
        }
        switch (NStringUtils.trim(varName)) {
            case "HOME": {
                return NTxVarImpl.ofOptional(varName, () -> NElement.ofString(System.getProperty("user.home")));
            }
            case "USERNAME": {
                return NTxVarImpl.ofOptional(varName, () -> NElement.ofString(System.getProperty("user.name")));
            }
        }
        String v = System.getProperty(varName);
        if (v != null) {
            return NTxVarImpl.ofOptional(varName, () -> NElement.ofString(System.getProperty(varName)));
        }
        return NOptional.ofNamedEmpty("var " + varName);
    }


    @Override
    public NTxResolutionContext withRemoveNamedDef(String name) {
        if (!this.definitions.containsKey(name)) {
            return this;
        }
        Map<String, NTxNodeDef> def2 = new LinkedHashMap<>(definitions);
        def2.remove(name);
        return copyAs(path, element, def, isInPage, engine, document, vars, def2, functions, compiledDocument,compiledPage, parentContext, itemParser);
    }

    @Override
    public NTxResolutionContext removeNamedDef(String name) {
        if (this.definitions.containsKey(name)) {
            definitions.remove(name);
        }
        return this;
    }

    public NOptional<NTxNodeDef> getNamedDef(String name) {
        return NOptional.ofNamed(definitions.get(name), name);
    }

    @Override
    public NTxResolutionContext setNamedDef(NTxNodeDef value) {
        if (value == null) {
            return this;
        }
        Object old = this.definitions.get(value.name());
        if (!Objects.equals(value, old)) {
            if (value == null) {
                definitions.remove(value.name());
            } else {
                definitions.put(value.name(), value);
            }
        }
        return this;
    }

    @Override
    public NTxResolutionContext withNamedDef(NTxNodeDef value) {
        if (value == null) {
            return this;
        }
        Object old = this.definitions.get(value.name());
        if (Objects.equals(value, old)) {
            return this;
        }
        Map<String, NTxNodeDef> def2 = new LinkedHashMap<>(definitions);
        if (value == null) {
            def2.remove(value.name());
        } else {
            def2.put(value.name(), value);
        }
        return copyAs(path, element, def, isInPage, engine, document, vars, def2, functions, compiledDocument,compiledPage, parentContext, itemParser);
    }

    @Override
    public NTxResolutionContext withRemoveFunction(String name) {
        if (!this.functions.containsKey(name)) {
            return this;
        }
        Map<String, NTxFunction> def2 = new LinkedHashMap<>(functions);
        def2.remove(name);
        return copyAs(path, element, def, isInPage, engine, document, vars, definitions, def2, compiledDocument,compiledPage, parentContext, itemParser);
    }


    @Override
    public NTxResolutionContext removeFunction(String name) {
        if (this.functions.containsKey(name)) {
            functions.remove(name);
        }
        return this;
    }

    @Override
    public NOptional<NTxFunction> getFunction(String name) {
        NTxFunction f = functions.get(name);
        if (f != null) {
            return NOptional.of(f);
        }
        if (parentContext != null) {
            f = parentContext.getFunction(name).orNull();
            if (f != null) {
                return NOptional.of(f);
            }
        }
        return engine.findFunction(name);
    }

    @Override
    public NTxResolutionContext setFunction(NTxFunction value) {
        if (value == null) {
            return this;
        }
        Object old = this.functions.get(value.name());
        if (!Objects.equals(value, old)) {
            if (value == null) {
                functions.remove(value.name());
            } else {
                functions.put(value.name(), value);
            }
        }
        return this;
    }

    @Override
    public NTxResolutionContext withFunction(NTxFunction value) {
        if (value == null) {
            return this;
        }
        Object old = this.functions.get(value.name());
        if (Objects.equals(value, old)) {
            return this;
        }
        Map<String, NTxFunction> def2 = new LinkedHashMap<>(functions);
        if (value == null) {
            def2.remove(value.name());
        } else {
            def2.put(value.name(), value);
        }
        return copyAs(path, element, def, isInPage, engine, document, vars, definitions, def2, compiledDocument,compiledPage, parentContext, itemParser);
    }

    @Override
    public boolean isDef() {
        return def != null;
    }

    @Override
    public NOptional<NElement> evalExpression(NElement expression) {
        if (expression == null) {
            return NOptional.ofNamedEmpty(String.valueOf(expression));
        }
        String baseSrc = NTxUtils.findCompilerDeclarationPath(expression).orNull();
        NTxNodeEval ne = new NTxNodeEval(this);
        NElement u = ne.eval(expression);
        if (u == null) {
            u = NElement.ofNull();
        }
        if (baseSrc != null) {
            u = NTxUtils.addCompilerDeclarationPath(u, baseSrc);
        }
        return NOptional.ofNamed(u, "expression " + expression);
    }

    @Override
    public NTxLogger log() {
        return engine.log();
    }

    @Override
    public NPath resolvePath(NElement path) {
        if (NBlankable.isBlank(path)) {
            return null;
        }
        if (path.isAnyString()) {
            String pathStr = path.asStringValue().get();
            if (NTxGitHelper.isGithubFolder(pathStr)) {
                return NTxGitHelper.resolveGithubPath(pathStr, log());
            }
            NTxSource source = source();
            return NTxUtils.resolvePath(path, source);
        }
        throw new NIllegalArgumentException(NMsg.ofC("unsupported path type : %s", path));
    }

    @Override
    public NTxResolutionContext pushContext() {
        return copyAs(path, element, def, isInPage, engine, document, vars, definitions, functions, compiledDocument,compiledPage,this, itemParser);
    }

    @Override
    public NTxResolutionContext withItemParser(NTxItemParser itemParser) {
        return copyAs(path, element, def, isInPage, engine, document, vars, definitions, functions, compiledDocument,compiledPage, this, itemParser);
    }

    @Override
    public NTxResolutionContext popContext() {
        return parentContext;
    }

    @Override
    public NOptional<NTxNode> findNodeByProperty(String propertyName, Predicate<NElement> propertyValueFilter) {
        return engine().findNodeByProperty(propertyName, propertyValueFilter, this);
    }

    public NTxResolutionContext parentContext() {
        return parentContext;
    }

    @Override
    public NTxDocumentFactory documentFactory() {
        return engine().documentFactory();
    }

    public Map<String, ? extends NTxVar> getVars() {
        return new LinkedHashMap<>(vars);
    }

    public Map<String, ? extends NTxNodeDef> getDefinitions() {
        return new LinkedHashMap<>(definitions);
    }

    public Map<String, ? extends NTxFunction> getFunctions() {
        return new LinkedHashMap<>(functions);
    }

    @Override
    public String toString() {
        return "NTxResolutionContextImpl{" +
                "uid=" + this.uid +
                ", element=" + element +
                ", path=" + Arrays.toString(path) +
                ", def=" + def +
                ", isInPage=" + isInPage +
                ", vars=" + vars +
                ", definitions=" + definitions +
                ", functions=" + functions +
                '}';
    }
}
