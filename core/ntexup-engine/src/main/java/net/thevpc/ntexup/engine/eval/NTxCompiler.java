package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.eval.*;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.document.*;
import net.thevpc.ntexup.api.parser.NTxNodeParser;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.engine.document.NTxItemBag;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.impl.NTxEngineUtils;
import net.thevpc.ntexup.engine.log.SilentNTxLogger;
import net.thevpc.ntexup.engine.parser.DefaultNTxNodeFactoryParseContext;
import net.thevpc.ntexup.engine.parser.NTxDocumentLoadingResultImpl;
import net.thevpc.ntexup.engine.parser.NTxNodeDefImpl;
import net.thevpc.ntexup.engine.parser.NTxNodeDefParamImpl;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import net.thevpc.ntexup.engine.parser.ctrlnodes.*;
import net.thevpc.nuts.util.NIllegalArgumentException;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NPairElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.*;


import java.util.*;
import java.util.stream.Collectors;

public class NTxCompiler {
    NTxVarProvider varProvider=null;
    private NTxEngine engine;

    public NTxCompiler(NTxEngine engine) {
        this.engine = engine;
    }

    public NTxDocumentLoadingResult compile(NTxDocument document0) {
        NTxDocument documentCopy = document0.copy();
        NTxSource source = documentCopy.root().source();
        SilentNTxLogger slog = new SilentNTxLogger();
        try {
            engine.addLog(slog);
            NTxNode root = documentCopy.root();
            List<NTxItem> all = compileNodeTree(root, new NodeHierarchy(null, false, false, new NTxCompilePageContextImpl(engine, documentCopy)));
//            checkNode(root,null);
            NTxItemBag b = new NTxItemBag(all);
            root = b.compress(false, source);
            processRootPages(root);
            if (documentCopy.root() != root) {
                documentCopy.root().reset();
                documentCopy.root().copyFrom(root);
            }
            return new NTxDocumentLoadingResultImpl(documentCopy, source, slog.getErrorCount() == 0);
        } finally {
            engine.removeLog(slog);
        }
    }

    public boolean processRootPages(NTxNode node) {
        switch (node.type()) {
            case NTxNodeType.PAGE_GROUP: {
                boolean someChanges = false;
//                for (NTxNode child : node.children()) {
//                    NTxUtils.checkNode(child);
//                }
                List<NTxNode> children = new ArrayList<>(node.children());
                List<NTxNode> newChildren = new ArrayList<>();
                List<NTxNode> pending = null;
                for (NTxNode c : children) {
                    if (c == null) {
                        someChanges = true;
                    } else {
                        someChanges |= processRootPages(c);
                        if (Objects.equals(c.type(), NTxNodeType.PAGE_GROUP)
                                || Objects.equals(c.type(), NTxNodeType.PAGE)
                                //assign are retained in the same level!
                                || Objects.equals(c.type(), NTxNodeType.CTRL_ASSIGN)
                        ) {
                            if (pending != null && !pending.isEmpty()) {
                                NTxNode newPage = engine.documentFactory().of(NTxNodeType.PAGE);
                                newPage.setSource(node.source());
                                newChildren.add(newPage);
                                newPage.addChildren(pending.toArray(new NTxNode[0]));
                                pending = null;
                            }
                            newChildren.add(c);
                        } else {
                            someChanges = true;
                            if (pending == null) {
                                pending = new ArrayList<>();
                            }
                            pending.add(c);
                        }
                    }
                }
                if (pending != null && pending.size() > 0) {
                    NTxNode newPage = engine.documentFactory().of(NTxNodeType.PAGE);
                    newPage.setSource(node.source());
                    newPage.addChildren(pending.toArray(new NTxNode[0]));
                    newChildren.add(newPage);
                }
                if (someChanges) {
                    node.children().clear();
                    node.addChildren(newChildren.toArray(new NTxNode[0]));
                }
                return someChanges;
            }
            default: {
                return false;
            }
        }
    }

    public NOptional<NTxNodeDef> findDefinition(NTxItem node, String name) {
        NTxItem currNode = node;
        while (currNode != null) {
            if (currNode instanceof NTxNode) {
                for (NTxNodeDef o : ((NTxNode) currNode).definitions()) {
                    if (NNameFormat.equalsIgnoreFormat(o.name(), name)) {
                        return NOptional.of(o);
                    }
                }
            }
            currNode = currNode.parent();
        }
        return NOptional.ofNamedEmpty("definition for " + name);
    }

    private NTxNodeDef compileNodeDef(NTxNodeDef item, NodeHierarchy h) {
        List<NTxItem> body = new ArrayList<>();
        for (NTxNode b : item.body()) {
            body.addAll(compileNodeTree(b, h.withParent(h.parent).withDef(item).withHierarchy(h.parent)));
        }
        return new NTxNodeDefImpl(
                (NTxNode) item.parent(),
                item.name(),
                Arrays.copyOf(item.params(), item.params().length),
                new NTxItemBag(body).compressToNodes(h.isInPage, item.source()).toArray(new NTxNode[0]),
                item.source()
        );
    }

    private static class NodeHierarchy {
        NTxNode parent;
        NTxNodeDef def;
        NTxNode hierarchy;
        boolean processPages;
        boolean isInPage;
        NTxCompilePageContext context;

        public NodeHierarchy(NTxNode parent, boolean processPages, boolean isInPage, NTxCompilePageContext context) {
            this.parent = parent;
            this.isInPage = isInPage;
            this.processPages = processPages;
            this.context = context;
        }

        public NodeHierarchy(NTxNode parent, NTxNodeDef def, NTxNode hierarchy, boolean processPages, boolean isInPage, NTxCompilePageContext context) {
            this.parent = parent;
            this.def = def;
            this.hierarchy = hierarchy;
            this.isInPage = isInPage;
            this.processPages = processPages;
            this.context = context;
        }
        public NTxDocument document(){
            return context.document();
        }
        NTxLogger messages(){
            return context.messages();
        }
        NTxEngine engine(){
            return context.engine();
        }

        public NodeHierarchy copy() {
            return new NodeHierarchy(parent, def, hierarchy , processPages, isInPage, context);
        }

        public NodeHierarchy withParent(NTxNode parent) {
            return new NodeHierarchy(parent, def, hierarchy , processPages, isInPage, context);
        }

        public NodeHierarchy withInPage(boolean isInPage) {
            return new NodeHierarchy(parent, def, hierarchy , processPages, isInPage, context);
        }

        public NodeHierarchy withParentOnly(NTxNode parent) {
            return new NodeHierarchy(parent, null, null, processPages, isInPage, context);
        }

        public NodeHierarchy withParentOnly() {
            return new NodeHierarchy(parent, null, null, processPages, isInPage, context);
        }

        public NodeHierarchy withDef(NTxNodeDef def) {
            return new NodeHierarchy(parent, def, hierarchy , processPages, isInPage, context);
        }

        public NodeHierarchy withHierarchy(NTxNode hierarchy) {
            return new NodeHierarchy(parent, def, hierarchy , processPages, isInPage, context);
        }

        public boolean isDef() {
            return def != null;
        }
    }

    private List<NTxItem> compileNodeTree(NTxItem item, NodeHierarchy h0) {
        NodeHierarchy h=h0;
        NAssert.requireNonNull(h, "h");
        if (item instanceof NTxNode) {
            NTxNode node = (NTxNode) item;
            NTxItem tparent = node.parent();
            if (!h.isInPage && NTxNodeType.PAGE.equals(node.type())) {
                h=h.withInPage(true);
            }
            if (!h.processPages && h.isInPage) {
                return Arrays.asList(node);
            }
            node = node.copy();
            node.addHierarchy(h.hierarchy);
            node.setTemplateDefinition(h.def);
            //temporarely set parent, it will be changed later!!
            NTxUtils.setNodeParent(node, h.parent);
            // compile properties
            List<NTxProp> properties = node.getProperties();
//            for (NTxProp property : properties) {
//                NElement value0 = property.getValue();
//                NElement value = engine.evalExpression(value0, node);
//                node.setProperty(property.getName(), value);
//            }
            // compile definitions
            NTxNodeDef[] defs = node.definitions();
            for (int i = 0; i < defs.length; i++) {
                defs[i] = compileNodeDef(defs[i], h.withParentOnly(node));
            }
            node.clearDefinitions();
            node.addDefinitions(defs);
            NTxUtils.setNodeParent(node, h.parent);
            switch (node.type()) {
                case NTxNodeType.CTRL_IF:
                    return compileNodeTree_if(node, h.withParentOnly());
                case NTxNodeType.CTRL_NAME:
                    return compileNodeTree_name(node, h.withParentOnly());
                case NTxNodeType.CTRL_INCLUDE:
                    return compileNodeTree_include(node, h.withParentOnly());
                case NTxNodeType.CTRL_IMPORT:
                    return compileNodeTree_import(node, h.withParentOnly());
                case NTxNodeType.CTRL_FOR:
                    return compileNodeTree_for(node, h.withParentOnly());
                case NTxNodeType.CTRL_ASSIGN:
                    return compileNodeTree_assign(node, h.withParentOnly());
                case NTxNodeType.CTRL_EXPR:
                    return compileNodeTree_expr(node, h.withParentOnly());
                case NTxNodeType.CTRL_CALL:
                    return compileNodeTree_call(node, h.withParentOnly());
            }
            return compileNodeTree_default(node, h);
        } else if (item instanceof NTxItemList) {
            List<NTxItem> all = new ArrayList<>();
            for (NTxItem subItem : ((NTxItemList) item).getItems()) {
                all.addAll(compileNodeTree(subItem, h));
            }
            return all;
        } else if (item instanceof NTxProp) {
            return Arrays.asList(item);
        } else if (item instanceof NTxNodeDef) {
            // should i compile
            return Arrays.asList(item);
        } else if (item instanceof NTxStyleRule) {
            return Arrays.asList(item);
        } else {
            throw new IllegalArgumentException("what are you talking about?");
        }
    }

    private List<NTxItem> compileNodeTree_default(NTxNode node, NodeHierarchy h) {
        List<NTxItem> newChildren = new ArrayList<>();
        List<NTxNode> initialChildren = new ArrayList<>(node.children());
        //enforce forward definition dependencies
        node.clearChildren();
        for (NTxNode child : initialChildren) {
            newChildren.addAll(compileNodeTree(child, h.withParent(node)));
            // should update for each child, because second child my depend on
            // some include of the previous child
            node.clearChildren();
            node.addAll(newChildren.toArray(new NTxItem[0]));
        }
        return Arrays.asList(node);
    }

    private List<NTxItem> compileNodeTree_expr(NTxNode node, NodeHierarchy h) {
        NElement varExpr = node.getProperty(NTxPropName.VALUE).get().getValue();
        NElement element = engine.evalExpression(varExpr, node, varProvider);
        NTxItem h2 = engine.newNode(element, createParseContext(
                varExpr,
                node,
                node.source(),
                h)).get();
        return new ArrayList<>(compileNodeTree(h2, h));
    }

    private List<NTxItem> compileNodeTree_assign(NTxNode node, NodeHierarchy h) {
        NTxItem p = node.parent();
        NTxNode n = (NTxNode) p;
        String varName = node.getProperty(NTxPropName.NAME).get().getValue().asStringValue().get();
        NElement varExpr = node.getProperty(NTxPropName.VALUE).get().getValue();
        boolean ifempty = NTxValue.of(node.getProperty("ifempty").map(x->x.getValue()).orNull()).asBoolean().orElse(false);
        if(ifempty) {
            NElement old = n.getVar(varName).orNull();
            if(old==null || old.isNull()) {
                n.setVar(varName, engine.evalExpression(varExpr, node, varProvider));
            }
        }else{
            n.setVar(varName, engine.evalExpression(varExpr, node, varProvider));
        }
        return new ArrayList<>();
    }

    private DefaultNTxNodeFactoryParseContext createParseContext(NElement element, NTxNode node, NTxSource resource, NodeHierarchy compilePageContext) {
        return new DefaultNTxNodeFactoryParseContext(
                compilePageContext.document(),
                element,
                engine,
                NTxUtils.nodePath(node),
                resource
        );
    }

    private List<NTxItem> compileNodeTree_name(NTxNode node, NodeHierarchy h) {
        CtrlNTxNodeName nnode = (CtrlNTxNodeName) node;
        NElement c = nnode.getVarName();
        String name = c.asStringValue().get();
        NOptional<NTxVar> v = engine.findVar(name, h.parent, varProvider);
        if (v.isPresent()) {
            DefaultNTxNode e = DefaultNTxNode.ofExpr(c, h.parent.source());
            e.setParent(h.parent);
            return compileNodeTree(e, h.withParentOnly());
        }else if(NTxUtils.isAnyDefVarName(name)) {
            return Arrays.asList(node);
        }
        NTxNodeParser p = engine.nodeTypeParser(name).orNull();
        if (p != null) {
            h.messages().log(NMsg.ofC("variable '%s' not found, rendering as plain text.  If you meant a component, use '%s()' syntax",name,name).asWarning(), NTxUtils.sourceOf(node));
        }else {
            h.messages().log(NMsg.ofC("variable '%s' not found, rendering as plain text", name).asWarning(), NTxUtils.sourceOf(node));
        }
        DefaultNTxNode t = DefaultNTxNode.ofText(name);
        t.setSource(node.source());
        return Arrays.asList(t);
    }

    private List<NTxItem> _process_call_node(NTxNodeDef d, CtrlNTxNodeCall c, NodeHierarchy h) {
        String uid = c.getCallName();
        NElement callDeclaration = c.getCallExpr();
        NTxNodeDefParam[] expectedParams = d.params();
        NTxNodeDefParam[] allExpectedParams = Arrays.copyOf(expectedParams, expectedParams.length + 1);
        allExpectedParams[expectedParams.length] = new NTxNodeDefParamImpl(NTxUtils.COMPONENT_BODY_VAR_NAME, NElement.ofArray(c.getCallBody().toArray(new NElement[0])));
        NTxNodeDefParam[] effectiveParams = new NTxNodeDefParam[allExpectedParams.length];
        List<NElement> callArgs = c.getCallArgs();
        List<NTxProp> extraParams = new ArrayList<>();

        if (callArgs.stream().allMatch(x -> x.isNamedPair())) {
            for (NElement e : callArgs) {
                NPairElement p = e.asPair().get();
                String n = p.key().asStringValue().get();
                int pos = NArrays.indexOfByMatcher(allExpectedParams, a -> NNameFormat.equalsIgnoreFormat(a.name(), n));
                NElement newValue = NTxUtils.addCompilerDeclarationPath(p.value(), c.source());
                if (pos >= 0) {
                    effectiveParams[pos] = new NTxNodeDefParamImpl(allExpectedParams[pos].name(), newValue);
                } else {
                    extraParams.add(new NTxProp(n, newValue, c));
                }
            }
            for (Map.Entry<String, NElement> e : c.getBodyVars().entrySet()) {
                String n = e.getKey();
                int pos = NArrays.indexOfByMatcher(allExpectedParams, a -> NNameFormat.equalsIgnoreFormat(a.name(), n));
                if (pos >= 0) {
                    effectiveParams[pos] = new NTxNodeDefParamImpl(allExpectedParams[pos].name(), e.getValue());
                } else {
                    NMsg errMsg = NMsg.ofC("undefined param %s for %s in %s. ignored", n, uid, NTxUtils.snippet(callDeclaration));
                    engine.log().log(errMsg, c.source());
                }
            }
            for (int i = 0; i < allExpectedParams.length; i++) {
                if (effectiveParams[i] == null) {
                    NElement v = allExpectedParams[i].value();
                    if (v != null) {
                        effectiveParams[i] = allExpectedParams[i];
                    } else {
                        NMsg errMsg = NMsg.ofC("missing param %s for %s in %s", allExpectedParams[i].name(), uid, NTxUtils.snippet(callDeclaration));
                        engine.log().log(errMsg, c.source());
                        //throw new NIllegalArgumentException(errMsg);
                    }
                }
            }
        } else if (callArgs.stream().noneMatch(x -> x.isNamedPair())) {
            if (expectedParams.length == callArgs.size()) {
                for (int i = 0; i < expectedParams.length; i++) {
                    effectiveParams[i] = new NTxNodeDefParamImpl(allExpectedParams[i].name(), callArgs.get(i));
                }
            }
        } else {
            NMsg errMsg = NMsg.ofC("cannot mix named and non named params in %s", callArgs.stream().map(x-> NTxUtils.snippet(x)).collect(Collectors.toList()));
            engine.log().log(errMsg, c.source());
            List<NTxItem> result = new ArrayList<>();
            return result;
        }

        NTxNode[] body = Arrays.copyOf(d.body(), d.body().length);
        for (int i = 0; i < body.length; i++) {
            body[i] = body[i].copy();
            body[i].setParent(c);
            for (NTxProp extraParam : extraParams) {
                body[i].setProperty(extraParam);
            }
            Map<String, NElement> ee = NTxUtils.inheritedVarsMap(c);
            for (Map.Entry<String, NElement> e : ee.entrySet()) {
                body[i].setVar(e.getKey(), e.getValue());
            }
            for (int j = effectiveParams.length - 1; j >= 0; j--) {
                if (effectiveParams[j] != null) {
                    body[i].setVar(effectiveParams[j].name(), NTxUtils.addCompilerDeclarationPath(effectiveParams[j].value(), NTxUtils.sourceOf(d)));
                }
            }
        }
        List<NTxItem> result = new ArrayList<>();
        for (NTxNode i : body) {
            result.addAll(compileNodeTree(i, h.withDef(d).withHierarchy((NTxNode) d.parent())));
        }
        return result;
    }

    private List<NTxItem> compileNodeTree_call(NTxNode node, NodeHierarchy h) {
        if (NTxNodeType.CTRL_CALL.equals(node.type())) {
            CtrlNTxNodeCall c = (CtrlNTxNodeCall) node;
            if (!h.isInPage || (h.isInPage && h.processPages)) {
                String uid = c.getCallName();
//                NTxNode currNode = NTxUtils.firstNodeUp(node.parent());
                NOptional<NTxNodeDef> dd = findDefinition(h.parent, uid);
                if (dd.isPresent()) {
                    return _process_call_node(dd.get(), c, h);
                } else {
                    NOptional<NTxFunction> t = engine.findFunction(h.parent, uid);
                    if (t.isPresent()) {
                        return _process_call_fct(t.get(), c, h);
                    }
                    h.messages().log(NMsg.ofC("undefined node %s", uid).asError(), c.source());
                    NTxNode node2 = DefaultNTxNode.ofText((NMsg.ofC("undefined node %s", uid).toString()));
                    node2.setSource(node.source());
                    return new ArrayList<>(Arrays.asList(node2));
                }
            }
            return new ArrayList<>(Collections.singleton(node));
        }
        throw new NIllegalArgumentException(NMsg.ofC("unexpected node type %s", node.type()));
    }

    private List<NTxItem> compileNodeTree_include(NTxNode node, NodeHierarchy h) {
        if (NTxNodeType.CTRL_INCLUDE.equals(node.type())) {
            CtrlNTxNodeInclude c = (CtrlNTxNodeInclude) node;
            List<NTxItem> loaded = new ArrayList<>();
            for (NElement callArg : c.getCallArgs()) {
                NElement r = engine.evalExpression(NTxUtils.addCompilerDeclarationPath(callArg, NTxUtils.sourceOf(c)), c, varProvider);
                NPath path = engine.resolvePath(r, c);
                if (path.isDirectory()) {
                    path = path.resolve(NTxEngineUtils.NTEXUP_EXT_STAR_STAR);
                }
                h.document().sourceMonitor().add(path);
                List<NPath> list = path.walkGlob().toList();
                list.sort(NTxEngineUtils::comparePaths);
                if (!list.isEmpty()) {
                    for (NPath nPath : list) {
                        if (nPath.isRegularFile()) {
                            NOptional<NTxItem> se = engine.loadNode(c, nPath, h.document());
                            if (se.isPresent()) {
                                NTxItem item = se.get();
                                NTxUtils.setNodeParent(item, h.parent);
                                List<NTxItem> compiled = compileNodeTree(item, h.withParentOnly());
                                loaded.addAll(compiled);
                            } else {
                                h.messages().log(NMsg.ofC("invalid include. error loading : %s", nPath).asSevere(), NTxUtils.sourceOf(c));
                            }
                        } else {
                            h.messages().log(NMsg.ofC("invalid include. error loading : %s", nPath).asWarning(), NTxUtils.sourceOf(c));
                        }
                    }
                } else {
                    h.messages().log(NMsg.ofC("invalid include. error loading : %s", path).asWarning(), NTxUtils.sourceOf(c));
                }
            }
            return loaded;
        }
        throw new NIllegalArgumentException(NMsg.ofC("expected 'include' node, got %s", node.type()));
    }
    private List<NTxItem> compileNodeTree_import(NTxNode node, NodeHierarchy h) {
        if (NTxNodeType.CTRL_IMPORT.equals(node.type())) {
            CtrlNTxNodeImport c = (CtrlNTxNodeImport) node;
            List<String> toImport = new ArrayList<>();
            for (NElement callArg : c.getCallArgs()) {
                NElement r = engine.evalExpression(NTxUtils.addCompilerDeclarationPath(callArg, NTxUtils.sourceOf(c)), c, varProvider);
                String s = NTxValue.of(r).asString().orNull();
                toImport.add(s);
            }
            engine.importDependencies(toImport.toArray(new String[0]));
            return new  ArrayList<>();
        }
        throw new NIllegalArgumentException(NMsg.ofC("expected 'include' node, got %s", node.type()));
    }

    private List<NTxItem> compileNodeTree_if(NTxNode node, NodeHierarchy h) {
        if (NTxNodeType.CTRL_IF.equals(node.type())) {
            CtrlNTxNodeIf c = (CtrlNTxNodeIf) node;
            if (!h.isInPage || (h.isInPage && h.processPages)) {
                NElement cond = c.getCond();
                NElement r = engine.evalExpression(cond, c, varProvider);
                boolean b = NTxUtils.asBoolean(r);
                if (b) {
                    List<NTxNode> tb = c.getTrueBloc();
                    List<NTxItem> tb2 = new ArrayList<>();
                    if (tb != null) {
                        for (NTxNode d : tb) {
                            d = d.copy();
                            d.setParent(h.parent);
                            tb2.addAll(compileNodeTree(d, h.withParentOnly()));
                        }
                    }
                    return tb2;
                } else {
                    List<NTxNode> tb = c.getFalseBloc();
                    List<NTxItem> tb2 = new ArrayList<>();
                    if (tb != null) {
                        for (NTxNode d : tb) {
                            d = d.copy();
                            d.setParent(h.parent);
                            tb2.addAll(compileNodeTree(d, h.withParentOnly()));
                        }
                    }
                    return tb2;
                }
            }
            return new ArrayList<>(Collections.singleton(node));
        }
        throw new NIllegalArgumentException(NMsg.ofC("unexpected node type %s", node.type()));
    }

    private List<NTxItem> compileNodeTree_for(NTxNode node, NodeHierarchy h) {
        if (NTxNodeType.CTRL_FOR.equals(node.type())) {
            CtrlNTxNodeFor c = (CtrlNTxNodeFor) node;
            if (!h.isInPage || (h.isInPage && h.processPages)) {
                NElement varName = c.getVarName();
                NElement varExpr = c.getVarExpr();
                String varNameStr = null;
                if (varName.isName()) {
                    varNameStr = varName.asStringValue().get();
                } else {
                    throw new NIllegalArgumentException(NMsg.ofC("expected varName in for contruct : %s", node));
                }
                NElement anyVal = engine.evalExpression(varExpr, node, varProvider);
                List<NElement> b = new ArrayList<>();
                if (anyVal.isAnyArray()) {
                    b.addAll(anyVal.asArray().get().children());
                } else {
                    b.add(anyVal);
                }
                List<NTxItem> result = new ArrayList<>();
                for (NElement o : b) {
                    for (NElement e : c.getBody()) {
                        NTxNode node2 = (NTxNode) engine.newNode(e, createParseContext(e, node, node.source(), h)).get();
                        node2.setVar(varNameStr, o);
                        NTxUtils.setNodeParent(node2, h.parent);
                        result.addAll(compileNodeTree(node2, h.withParentOnly()));
                    }
                }
                return result;
            }
            return new ArrayList<>(Collections.singleton(node));
        }
        throw new NIllegalArgumentException(NMsg.ofC("unexpected node type %s", node.type()));
    }

    private List<NTxItem> _process_call_fct(NTxFunction t, CtrlNTxNodeCall c, NodeHierarchy h) {
        NTxSource source = NTxUtils.sourceOf(c);
        NTxFunctionContext ctx = new DefaultNTxFunctionContext(engine,c,varProvider);
        NElement result = t.invoke(new NTxFunctionArgsImpl(c.getCallArgs(),c,engine,varProvider), ctx);
        if (result == null) {
            return new ArrayList<>();
        }
        DefaultNTxNodeFactoryParseContext ctx2 = createParseContext(result, c, source, h);
        NOptional<NTxItem> n = engine.newNode(result, ctx2);
        if (n.isPresent()) {
            NTxItem nn = n.get();
            if (nn instanceof NTxNode) {
                return compileNodeTree(nn, h);
            } else if (nn instanceof NTxItemList && ((NTxItemList) nn).getItems().stream().allMatch(x -> x instanceof NTxItem)) {
                List<NTxItem> r = new ArrayList<>();
                for (NTxItem item : ((NTxItemList) nn).getItems()) {
                    r.addAll(compileNodeTree((NTxNode) item, h));
                }
                return r;
            } else {
                engine.newNode(result, ctx2).get();
                NMsg msg = NMsg.ofC("unexpected node type %s", nn.getClass());
                engine.log().log(msg.asError());
                return Arrays.asList(DefaultNTxNode.ofText(msg.toString()));
            }
        } else {
            NMsg msg = NMsg.ofC("not found node for %s : %s", result.type().id(), NTxUtils.snippet(result));
            engine.log().log(msg.asError());
            return Arrays.asList(DefaultNTxNode.ofText(msg.toString()));
        }
    }


    public List<NTxNode> compilePageNode(NTxNode node, NTxCompilePageContext context) {
        NTxNode p = (NTxNode) node.parent();
        NTxUtils.checkNode(node, p);
        ArrayList<NTxNode> y = new ArrayList<>();
        for (NTxItem d : compileNodeTree(node, new NodeHierarchy(p,true, true, context))) {
            y.add((NTxNode) d);
        }
        return y;
    }

}
