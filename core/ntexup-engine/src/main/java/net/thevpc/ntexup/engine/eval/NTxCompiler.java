package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.extension.NTxFunction;
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
import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import net.thevpc.ntexup.engine.parser.ctrlnodes.*;
import net.thevpc.nuts.concurrent.NScoredCallable;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NIllegalArgumentException;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NPairElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.*;


import java.util.*;
import java.util.stream.Collectors;

public class NTxCompiler {
    private NTxEngine engine;

    public NTxCompiler(NTxEngine engine) {
        this.engine = engine;
    }

    public NTxDocumentLoadingResult compileDocument(NTxDocument document0) {
        NTxDocument documentCopy = document0.copy();
        NTxSource source = documentCopy.root().source();
        SilentNTxLogger slog = new SilentNTxLogger();
        try {
            engine.addLog(slog);
            NTxNode preRoot = DefaultNTxNode.ofGroup();
            NTxNode root = documentCopy.root().copy();
            root.setParent(preRoot);
            compileNodeTree(new NTxResolutionContextImpl(preRoot, root, NElement.ofNull(), false, engine, documentCopy));
            if (root.children().size() == 1) {
                root = root.children().get(0);
            }
//            checkNode(root,null);
            NTxItemBag b = new NTxItemBag((List) root.children());
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

//    public NOptional<NTxNodeDef> findDefinition(NTxItem node, String name) {
//        NTxItem currNode = node;
//        while (currNode != null) {
//            if (currNode instanceof NTxNode) {
//                for (NTxNodeDef o : ((NTxNode) currNode).definitions()) {
//                    if (NNameFormat.equalsIgnoreFormat(o.name(), name)) {
//                        return NOptional.of(o);
//                    }
//                }
//            }
//            currNode = currNode.parent();
//        }
//        return NOptional.ofNamedEmpty("definition for " + name);
//    }

    private NTxNodeDef compileNodeDef(NTxNodeDef item, NTxResolutionContext context) {
        DefaultNTxNode bodyContainer = DefaultNTxNode.ofGroup(context.parent());
        NTxResolutionContext newContext = context.withNode(bodyContainer).withDef(item); // replace itemDef
        for (NTxNode b : item.body()) {
            compileNodeTree(newContext.resolveNode(b));
        }
        List<NTxNode> newBody = new NTxItemBag(bodyContainer.children()).compressToNodes(context.inPage(), item.source());
        bodyContainer.clearChildren();
        for (NTxNode nTxNode : newBody) {
            bodyContainer.add(nTxNode);
            nTxNode.setParent(bodyContainer);
        }
        return new NTxNodeDefImpl(
                (NTxNode) item.parent(),
                item.name(),
                Arrays.copyOf(item.params(), item.params().length),
                bodyContainer,
                item.source()
        );
    }

    private void compileNodeTree(NTxResolutionContext context) {
        NTxItem item = context.node();
        NAssert.requireNamedNonNull(context, "context");
        boolean wasInPage = context.inPage();
        if (item instanceof NTxNode) {
            NTxNode node = (NTxNode) item;
            if (NTxNodeType.PAGE.equals(node.type())) {
                //update reference!
                context.setInPage(true);
            }
            if (!wasInPage && context.inPage()) {
                context.parent().add(node);
                return;
            }
            if (node instanceof CtrNTxNodelUncompiled) {
                NElement raw = node.getRaw();
                NOptional<NTxItem> o = engine.newNode(raw, context.withElement(raw));
                if (o.isPresent()) {
                    NTxNode oldNode = context.node();
                    try {
                        NTxItem v = o.get();
                        NTxItemBag ib = new NTxItemBag();
                        ib.add(v);
                        for (NTxItem a : ib.all()) {
                            if (a instanceof NTxNode) {
                                //update reference!
                                compileNodeTree(context.setNode((NTxNode) a));
                            } else {
                                context.parent().add(a);
                            }
                        }
                    } finally {
                        context.setNode(oldNode);
                    }
                    return;
                }
                return;
            }
            node = node.copy();
            node.setTemplateDefinition(context.def());
            //temporarely set parent, it will be changed later!!
            NTxUtils.setNodeParent(node, context.parent());
            context = context.withNode(node);
//            NTxUtils.setNodeParent(node, context.parent());
            switch (node.type()) {
                case NTxNodeType.CTRL_IF: {
                    compileNodeTree_if(context.withParentOnly());
                    return;
                }
                case NTxNodeType.CTRL_NAME: {
                    compileNodeTree_name(context.withParentOnly());
                    return;
                }
                case NTxNodeType.CTRL_INCLUDE: {
                    compileNodeTree_include(context.withParentOnly());
                    return;
                }
                case NTxNodeType.CTRL_IMPORT: {
                    compileNodeTree_import(context.withParentOnly());
                    return;
                }
                case NTxNodeType.CTRL_FOR: {
                    compileNodeTree_for(context.withParentOnly());
                    return;
                }
                case NTxNodeType.CTRL_ASSIGN: {
                    compileNodeTree_assign(context.withParentOnly());
                    return;
                }
                case NTxNodeType.CTRL_EXPR: {
                    compileNodeTree_expr(context.withParentOnly());
                    return;
                }
                case NTxNodeType.CTRL_CALL: {
                    compileNodeTree_call(context.withParentOnly());
                    return;
                }
                case NTxNodeType.CTRL_DEFINE: {
                    compileNodeTree_define(context.withParentOnly());
                    return;
                }
            }
            compileNodeTree_default(context);
            return;
        } else if (item instanceof NTxItemList) {
            for (NTxItem subItem : ((NTxItemList) item).getItems()) {
                if (subItem instanceof NTxProp) {
                    context.parent().setProperty((NTxProp) subItem);
                    return;
                } else if (subItem instanceof NTxStyleRule) {
                    context.parent().addRule((NTxStyleRule) subItem);
                    return;
                } else {
                    compileNodeTree(context.withNode((NTxNode) subItem));
                }
            }
            return;
        } else if (item instanceof NTxProp) {
            context.parent().setProperty((NTxProp) item);
            return;
        } else if (item instanceof NTxStyleRule) {
            context.parent().addRule((NTxStyleRule) item);
            return;
        } else {
            throw new IllegalArgumentException("what are you talking about?");
        }
    }

    private void compileNodeTree_default(NTxResolutionContext context) {
        context.parent().add(context.node());
        for (NTxNode child : context.node().children()) {
            context.pushNode(child);
            compileNodeTree(context);
            context.popNode();
        }
    }

    private void compileNodeTree_expr(NTxResolutionContext context) {
        NElement varExpr = context.node().getProperty(NTxPropName.VALUE).get().getValue();
        NElement element = context.evalExpression(varExpr).get();
        NTxItem h2 = engine.newNode(element, context.withElement(element)).get();
        context.parent().mergeNode(h2);
    }

    private void compileNodeTree_assign(NTxResolutionContext context) {
        NTxNode node = context.node();
        String varName = node.getProperty(NTxPropName.NAME).get().getValue().asStringValue().get();
        NElement varExpr = node.getProperty(NTxPropName.VALUE).get().getValue();
        boolean ifempty = NTxValue.of(node.getProperty("ifempty").map(x -> x.getValue()).orNull()).asBoolean().orElse(false);
        NElement evaluatedExpr = context.evalExpression(varExpr).orNull();
        if (ifempty) {
            NTxVar v = context.getVar(varName).orNull();
            if (v == null) {
                context.setVar(varName, NTxVar.ofEvaluatedExpression(evaluatedExpr));
            }
        } else {
            context.setVar(varName, NTxVar.ofEvaluatedExpression(evaluatedExpr));
        }
    }

    private void compileNodeTree_name(NTxResolutionContext context) {
        CtrlNTxNodeName node = (CtrlNTxNodeName) context.node();
        NElement c = node.getVarName();
        String name = c.asStringValue().get();
        NOptional<NTxVar> v = context.getVar(name);
        if (v.isPresent()) {
            DefaultNTxNode e = DefaultNTxNode.ofExpr(c, context.parent().source());
            e.setParent(context.parent());
            compileNodeTree(context.withParentOnly().withNode(e));
        } else if (NTxUtils.isComponentBody(name)) {
            context.parent().add(node);
        } else {
            NTxNodeParser p = engine.nodeTypeParser(name).orNull();
            if (p != null) {
                context.messages().log(NMsg.ofC("variable '%s' not found, rendering as plain text.  If you meant a component, use '%s()' syntax", name, name).asWarning(), NTxUtils.sourceOf(node));
            } else {
                context.messages().log(NMsg.ofC("variable '%s' not found, rendering as plain text", name).asWarning(), NTxUtils.sourceOf(node));
            }
            DefaultNTxNode t = DefaultNTxNode.ofText(name);
            t.setSource(node.source());
            context.parent().add(t);
        }
    }

    private void _process_call_node(NTxNodeDef d, CtrlNTxNodeCall c, NTxResolutionContext h) {
        NTxNodeDefParam[] expectedParams = d.params();
        //first unset any parent COMPONENT_BODY_VAR_NAME and create a new context!
        h = h.withVar(NTxUtils.COMPONENT_BODY_VAR_NAME, null);
        for (NTxNodeDefParam expectedParam : expectedParams) {
            if (expectedParam.value() != null) {
                h.setVar(NTxUtils.COMPONENT_BODY_VAR_NAME, NTxVar.ofNonEvaluatedExpression(expectedParam.value()));
            }
        }
        h.setVar(NTxUtils.COMPONENT_BODY_VAR_NAME, NTxVar.ofNonEvaluatedExpression(NElement.ofArray(c.getCallBody().toArray(new NElement[0]))));

        List<NElement> callArgs = c.getCallArgs();

        if (callArgs.stream().allMatch(NElement::isNamedPair)) {
            for (NElement e : callArgs) {
                NPairElement p = e.asPair().get();
                String n = p.key().asStringValue().get();
                h.setVar(n, NTxVar.ofNonEvaluatedExpression(NTxUtils.addCompilerDeclarationPath(p.value(), c.source())));
            }
        } else if (callArgs.stream().noneMatch(NElement::isNamedPair)) {
            for (int i = 0; i < Math.min(expectedParams.length, callArgs.size()); i++) {
                h.setVar(expectedParams[i].name(), NTxVar.ofNonEvaluatedExpression(NTxUtils.addCompilerDeclarationPath(callArgs.get(i), c.source())));
            }
        } else {
            NMsg errMsg = NMsg.ofC("cannot mix named and non named params in %s, all params ignored", callArgs.stream().map(x -> NTxUtils.snippet(x)).collect(Collectors.toList()));
            engine.log().log(errMsg, c.source());
        }
        NTxNode[] oldBody = d.body();
        NTxResolutionContext newContext = h.withDef(d);
        for (int i = 0; i < oldBody.length; i++) {
            compileNodeTree(newContext.withNode(oldBody[i]));
        }
    }

    private void compileNodeTree_define(NTxResolutionContext context) {
        context.parent().add(context.node());
        context.setNamedDef((NTxNodeDef) context.node());
    }

    private void compileNodeTree_call(NTxResolutionContext context) {
        CtrlNTxNodeCall c = (CtrlNTxNodeCall) context.node();
        String uid = c.getCallName();
        NTxNodeParser p = engine.nodeTypeParser(uid).orNull();
        if (p != null) {
            NScoredCallable<NTxItem> n = p.parseNode(
                    context.withElement(NElement.ofObjectBuilder()
                            .name(uid)
                            .addParams(c.getCallArgs())
                            .addAll(c.getCallBody())
                            .build())
            );
            NScorableContext scc = NScorableContext.of();
            if (NScorable.isValidScore(n, scc)) {
                NTxItem t = n.call();
                if (t != null) {
                    context.parent().add(t);
                }
            }
        }
//                NTxNode currNode = NTxUtils.firstNodeUp(node.parent());
        NOptional<NTxNodeDef> dd = context.getNamedDef(uid);
        if (dd.isPresent()) {
            _process_call_node(dd.get(), c, context);
            return;
        } else {
            NOptional<NTxFunction> t = context.getFunction(uid);
            if (t.isPresent()) {
                _process_call_fct(t.get(), c, context);
                return;
            }
            addErr(NMsg.ofC("undefined node %s", NMsg.ofStyledError(uid)), context);
        }
    }

    private void compileNodeTree_include(NTxResolutionContext context) {
        CtrlNTxNodeInclude node = (CtrlNTxNodeInclude) context.node();
        for (NElement callArg : node.getCallArgs()) {
            NElement r = context.evalExpression(NTxUtils.addCompilerDeclarationPath(callArg, NTxUtils.sourceOf(node))).orNull();
            NPath path = context.resolvePath(r);
            if (path.isDirectory()) {
                path = path.resolve(NTxEngineUtils.NTEXUP_EXT_STAR_STAR);
            }
            context.document().sourceMonitor().add(path);
            List<NPath> list = path.walkGlob().toList();
            list.sort(NTxEngineUtils::comparePaths);
            if (!list.isEmpty()) {
                for (NPath nPath : list) {
                    if (nPath.isRegularFile()) {
                        NOptional<NTxItem> se = engine.loadNode(node, nPath, context.document());
                        if (se.isPresent()) {
                            NTxItem item = se.get();
                            NTxUtils.setNodeParent(item, context.parent());
                            compileNodeTree(context.withParentOnly().withNode((NTxNode) item));
                        } else {
                            context.messages().log(NMsg.ofC("invalid include. error loading : %s", nPath).asSevere(), NTxUtils.sourceOf(node));
                        }
                    } else {
                        context.messages().log(NMsg.ofC("invalid include. error loading : %s", nPath).asWarning(), NTxUtils.sourceOf(node));
                    }
                }
            } else {
                context.messages().log(NMsg.ofC("invalid include. error loading : %s", path).asWarning(), NTxUtils.sourceOf(node));
            }
        }

    }

    private void compileNodeTree_import(NTxResolutionContext context) {
        CtrlNTxNodeImport node = (CtrlNTxNodeImport) context.node();
        List<String> toImport = new ArrayList<>();
        for (NElement callArg : node.getCallArgs()) {
            NElement r = context.evalExpression(NTxUtils.addCompilerDeclarationPath(callArg, NTxUtils.sourceOf(node))).orNull();
            String s = NTxValue.of(r).asString().orNull();
            toImport.add(s);
        }
        //import is global
        // TODO : add some context to "accessible imports some how!!
        // dont now how (need track each class where is came from
        // does nuts support this ? it should, its a common use
        engine.importDependencies(toImport.toArray(new String[0]));
    }

    private void compileNodeTree_if(NTxResolutionContext context) {
        CtrlNTxNodeIf node = (CtrlNTxNodeIf) context.node();
        NElement cond = node.getCond();
        NElement r = context.evalExpression(cond).orNull();
        boolean b = NTxUtils.asBoolean(r);
        if (b) {
            List<NTxNode> tb = node.getTrueBloc();
            if (tb != null) {
                for (NTxNode d : tb) {
                    d = d.copy();
                    d.setParent(context.parent());
                    compileNodeTree(context.withParentOnly().withNode(d));
                }
            }
        } else {
            List<NTxNode> tb = node.getFalseBloc();
            List<NTxItem> tb2 = new ArrayList<>();
            if (tb != null) {
                for (NTxNode d : tb) {
                    d = d.copy();
                    d.setParent(context.parent());
                    compileNodeTree(context.withParentOnly().withNode(d));
                }
            }
        }
    }

    private void compileNodeTree_for(NTxResolutionContext context) {
        CtrlNTxNodeFor node = (CtrlNTxNodeFor) context.node();
        NElement varName = node.getVarName();
        NElement varExpr = node.getVarExpr();
        String varNameStr = null;
        if (varName.isName()) {
            varNameStr = varName.asStringValue().get();
        } else {
            addErr(NMsg.ofC("expected varName in for construct : %s", node), context);
        }
        NElement anyVal = context.evalExpression(varExpr).orNull();
        List<NElement> b = new ArrayList<>();
        if (anyVal.isAnyArray()) {
            b.addAll(anyVal.asArray().get().children());
        } else {
            b.add(anyVal);
        }
        for (NElement o : b) {
            NTxResolutionContext icontext = context.withVar(varNameStr, NTxVar.ofEvaluatedExpression(o));
            for (NElement e : node.getBody()) {
                NTxNode node2 = (NTxNode) engine.newNode(e, icontext.withElement(e)).get();
                NTxUtils.setNodeParent(node2, context.parent());
                compileNodeTree(icontext.withParentOnly().withNode(node2));
            }
        }
    }

    private void addErr(NMsg msg, NTxResolutionContext context) {
        engine.log().log(msg.asError());
        context.parent().add(new CtrlNTxNodeError(context.source(), msg));
    }

    private void _process_call_fct(NTxFunction t, CtrlNTxNodeCall c, NTxResolutionContext context) {
        NTxSource source = NTxUtils.sourceOf(c);
        NElement result = t.invoke(new NTxFunctionArgsImpl(c.getCallArgs(), c, context), context);
        if (result == null) {
            context.parent().add(new CtrlNTxNodeSimpleResult(source, NElement.ofNull()));
        }
        NOptional<NTxItem> n = engine.newNode(result, context.withElement(result));
        if (n.isPresent()) {
            NTxItem nn = n.get();
            if (nn instanceof NTxNode) {
                compileNodeTree(context.withNode((NTxNode) nn));
                return;
            } else if (nn instanceof NTxItemList && ((NTxItemList) nn).getItems().stream().allMatch(x -> x instanceof NTxItem)) {
                for (NTxItem item : ((NTxItemList) nn).getItems()) {
                    compileNodeTree(context.withNode((NTxNode) item));
                }
                return;
            } else {
                addErr(NMsg.ofC("unexpected node type %s", nn.getClass()), context);
                return;
            }
        } else {
            addErr(NMsg.ofC("not found node for %s : %s", result.type().id(), NTxUtils.snippet(result)), context);
        }
    }


    public void compilePageNode(NTxResolutionContext context) {
        if (context.engine() != engine) {
            throw new NIllegalArgumentException(NMsg.ofC("invalid engine"));
        }
        context = context.withInPage(true);
        NAssert.requireNamedNonNull(context.node(), "page");
        NAssert.requireNamedEquals(NTxNodeType.PAGE, context.node().type(), "page");
        compileNodeTree(context);
    }


    private DefaultNTxNodeFactoryParseContext createParseContext(NElement element, NTxNode node, NTxSource resource, NTxResolutionContext compilePageContext) {
        return new DefaultNTxNodeFactoryParseContext(
                compilePageContext.document(),
                element,
                engine,
                NTxUtils.nodePath(node),
                resource
        );
    }
}
