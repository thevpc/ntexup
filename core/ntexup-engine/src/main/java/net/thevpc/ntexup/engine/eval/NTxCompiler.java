package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.engine.CompileNodeVisitor;
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
import java.util.function.Consumer;
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
            NTxNode root = documentCopy.root();
            List<NTxNode> rootChildren = root.children();
            root.clearChildren();
            try (FillDocumentCompileNodeVisitor visitor = new FillDocumentCompileNodeVisitor(root, engine)) {
                for (NTxNode rootChild : rootChildren) {
                    compileNodeTree(new NTxResolutionContextImpl(root, rootChild, NElement.ofNull(), false, engine, documentCopy), visitor);
                }
            }
            return new NTxDocumentLoadingResultImpl(documentCopy, source, slog.getErrorCount() == 0);
        } finally {
            engine.removeLog(slog);
        }
    }

    private NTxNodeDef compileNodeDef(NTxNodeDef item, NTxResolutionContext context, CompileNodeVisitor visitor) {
        DefaultNTxNode bodyContainer = DefaultNTxNode.ofGroup(context.parent());
        NTxResolutionContext newContext = context.setNode(bodyContainer).withDef(item); // replace itemDef
        for (NTxNode b : item.body()) {
            newContext.doWithPush(b, nTxResolutionContext -> compileNodeTree(nTxResolutionContext, visitor));
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

    public void compileNodeTree(NTxResolutionContext context, CompileNodeVisitor visitor) {
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
                visitor.visitNode(node, context);
                return;
            }
            if (node instanceof CtrNTxNodelUncompiled) {
                NElement raw = node.getRaw();
                NElement oldElement = context.element();
                try {
                    context.setElement(raw);
                    NTxResolutionContext finalContext = context;
                    engine.processNewNode(raw, o -> {
                        if (o.isPresent()) {
                            NTxNode oldNode = finalContext.node();
                            try {
                                NTxItem v = o.get();
                                if(v==node){
                                    visitor.visitNode(node, finalContext);
                                    return;
                                }
                                NTxItemBag ib = new NTxItemBag();
                                ib.add(v);
                                for (NTxItem a : ib.all()) {
                                    if (a instanceof NTxNode) {
                                        //update reference!
                                        NTxResolutionContext cc = finalContext.setNode((NTxNode) a);
                                        compileNodeTree(cc, new CompileNodeVisitorWithContext(finalContext, visitor));
                                    } else {
                                        dispatchVisit(a, visitor, finalContext);
                                    }
                                }
                            } finally {
                                finalContext.setNode(oldNode);
                            }
                            return;
                        }
                    }, context);
                } finally {
                    context.setElement(oldElement);
                }
                return;
            }
//            node = node.copy();
            node.setTemplateDefinition(context.def());
            //temporarely set parent, it will be changed later!!
            NTxUtils.setNodeParent(node, context.parent());

            NTxNode[] oldPath = context.path();
            NTxNodeDef oldDef = context.getDef();
            context = context.setNode(node);
            try {
                context.setDef(null);
                switch (node.type()) {
                    case NTxNodeType.CTRL_IF: {
                        compileNodeTree_if(context, visitor);
                        return;
                    }
                    case NTxNodeType.CTRL_NAME: {
                        compileNodeTree_name(context, visitor);
                        return;
                    }
                    case NTxNodeType.CTRL_INCLUDE: {
                        compileNodeTree_include(context, visitor);
                        return;
                    }
                    case NTxNodeType.CTRL_IMPORT: {
                        compileNodeTree_import(context, visitor);
                        return;
                    }
                    case NTxNodeType.CTRL_FOR: {
                        compileNodeTree_for(context, visitor);
                        return;
                    }
                    case NTxNodeType.CTRL_ASSIGN: {
                        compileNodeTree_assign(context, visitor);
                        return;
                    }
                    case NTxNodeType.CTRL_EXPR: {
                        compileNodeTree_expr(context, visitor);
                        return;
                    }
                    case NTxNodeType.CTRL_CALL: {
                        compileNodeTree_call(context, visitor);
                        return;
                    }
                    case NTxNodeType.CTRL_DEFINE: {
                        compileNodeTree_define(context, visitor);
                        return;
                    }
                }
                compileNodeTree_default(context, visitor);
            } finally {
                context.setPath(oldPath);
                context.setDef(oldDef);
            }
        } else if (item instanceof NTxItemList) {
            NTxNode[] oldPath = context.path();
            for (NTxItem subItem : ((NTxItemList) item).getItems()) {
                if (subItem instanceof NTxNode) {
                    compileNodeTree(context.setNode((NTxNode) subItem), new CompileNodeVisitorWithContext(context, visitor));
                } else {
                    dispatchVisit(subItem, visitor, context);
                }
            }
            context.setPath(oldPath);
        } else if (item instanceof NTxProp) {
            dispatchVisit(item, visitor, context);
        } else if (item instanceof NTxStyleRule) {
            dispatchVisit(item, visitor, context);
        } else {
            throw new IllegalArgumentException("what are you talking about?");
        }
    }

    private void dispatchVisit(NTxItem a, CompileNodeVisitor visitor, NTxResolutionContext context) {
        if (a instanceof NTxNodeDef) {
            visitor.visitDefinition((NTxNodeDef) a, context);
        } else if (a instanceof NTxNode) {
            visitor.visitNode((NTxNode) a, context);
        } else if (a instanceof NTxStyleRule) {
            visitor.visitRule((NTxStyleRule) a, context);
        } else if (a instanceof NTxFunction) {
            visitor.visitFunction((NTxFunction) a, context);
        } else if (a instanceof NTxProp) {
            visitor.visitProperty((NTxProp) a, context);
        } else {
            throw new NIllegalArgumentException(NMsg.ofC("unexpected"));
        }
    }

    private void compileNodeTree_default(NTxResolutionContext context, CompileNodeVisitor visitor) {
        NTxNode node = context.node();
        List<NTxNode> children = node.children();
        node.clearChildren();
        for (NTxNode child : children) {
            context.doWithPush(child, new Consumer<NTxResolutionContext>() {
                @Override
                public void accept(NTxResolutionContext ctx) {
                    compileNodeTree(ctx, new FillNodeCompileNodeVisitor(node));
                }
            });
        }
        visitor.visitNode(node, context);
    }

    private void compileNodeTree_expr(NTxResolutionContext context, CompileNodeVisitor visitor) {
        NElement varExpr = context.node().getProperty(NTxPropName.VALUE).get().getValue();
        NElement element = context.evalExpression(varExpr).get();
        engine.processNewNode(element,
                a -> {
                    context.parent().mergeNode(a.get());
                },
                context.withElement(element)
        );
    }

    private void compileNodeTree_assign(NTxResolutionContext context, CompileNodeVisitor visitor) {
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

    private void compileNodeTree_name(NTxResolutionContext context, CompileNodeVisitor visitor) {
        CtrlNTxNodeName node = (CtrlNTxNodeName) context.node();
        NElement c = node.getVarName();
        String name = c.asStringValue().get();
        NOptional<NTxVar> v = context.getVar(name);
        if (v.isPresent()) {
            DefaultNTxNode e = DefaultNTxNode.ofExpr(c, context.parent().source());
            e.setParent(context.parent());
            compileNodeTree(context.setDef(null).setNode(e), visitor);
        } else if (NTxUtils.isComponentBody(name)) {
            visitor.visitNode(node, context);
        } else {
            NTxNodeParser p = engine.nodeTypeParser(name).orNull();
            if (p != null) {
                context.messages().log(NMsg.ofC("variable '%s' not found, rendering as plain text.  If you meant a component, use '%s()' syntax", name, name).asWarning(), NTxUtils.sourceOf(node));
            } else {
                context.messages().log(NMsg.ofC("variable '%s' not found, rendering as plain text", name).asWarning(), NTxUtils.sourceOf(node));
            }
            DefaultNTxNode t = DefaultNTxNode.ofText(name);
            t.setSource(node.source());
            visitor.visitNode(t, context);
        }
    }

    private void _process_call_node(NTxNodeDef d, CtrlNTxNodeCall c, final NTxResolutionContext context, CompileNodeVisitor visitor) {
        NTxNodeDefParam[] expectedParams = d.params();
        //first unset any parent COMPONENT_BODY_VAR_NAME and create a new context!
        DefaultNTxNode fragment = DefaultNTxNode.ofFragment();

        List<NTxNode> assigns = new ArrayList<>();
        for (NTxNodeDefParam expectedParam : expectedParams) {
            if (expectedParam.value() != null) {
                assigns.add(DefaultNTxNode.ofAssign(expectedParam.name(), expectedParam.value(), context.source()));
            }
        }
        assigns.add(DefaultNTxNode.ofAssign(NTxUtils.COMPONENT_BODY_VAR_NAME, NElement.ofArray(c.getCallBody().toArray(new NElement[0])), context.source()));

        List<NElement> callArgs = c.getCallArgs();

        if (callArgs.stream().allMatch(NElement::isNamedPair)) {
            for (NElement e : callArgs) {
                NPairElement p = e.asPair().get();
                String n = p.key().asStringValue().get();
                assigns.add(DefaultNTxNode.ofAssign(n, NTxUtils.addCompilerDeclarationPath(p.value(), c.source()), context.source()));
            }
        } else if (callArgs.stream().noneMatch(NElement::isNamedPair)) {
            for (int i = 0; i < Math.min(expectedParams.length, callArgs.size()); i++) {
                assigns.add(DefaultNTxNode.ofAssign(expectedParams[i].name(), NTxUtils.addCompilerDeclarationPath(callArgs.get(i), c.source()), context.source()));
            }
        } else {
            NMsg errMsg = NMsg.ofC("cannot mix named and non named params in %s, all params ignored", callArgs.stream().map(x -> NTxUtils.snippet(x)).collect(Collectors.toList()));
            engine.log().log(errMsg, c.source());
        }
        NTxNode[] oldBody = d.body();
        NTxResolutionContext newContext = context.withDef(d);
        FillNodeCompileNodeVisitor fill = new FillNodeCompileNodeVisitor(fragment);
        newContext.doWithPush(fragment, cc -> {
            for (NTxNode assign : assigns) {
                cc.doWithPush(assign, new Consumer<NTxResolutionContext>() {
                    @Override
                    public void accept(NTxResolutionContext nTxResolutionContext) {
                        compileNodeTree(cc, fill);
                    }
                });
            }
            for (int i = 0; i < oldBody.length; i++) {
                cc.doWithPush(oldBody[i], new Consumer<NTxResolutionContext>() {
                    @Override
                    public void accept(NTxResolutionContext nTxResolutionContext) {
                        compileNodeTree(cc, fill);
                    }
                });
            }
        });
        visitor.visitNode(fragment, context);
    }

    private void compileNodeTree_define(NTxResolutionContext context, CompileNodeVisitor visitor) {
        visitor.visitNode(context.node(), context);
        context.setNamedDef((NTxNodeDef) context.node());
    }

    private void compileNodeTree_call(NTxResolutionContext context, CompileNodeVisitor visitor) {
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
                    dispatchVisit(t, visitor, context);
                }
            }
        }
//                NTxNode currNode = NTxUtils.firstNodeUp(node.parent());
        NOptional<NTxNodeDef> dd = context.getNamedDef(uid);
        if (dd.isPresent()) {
            _process_call_node(dd.get(), c, context, visitor);
            return;
        } else {
            NOptional<NTxFunction> t = context.getFunction(uid);
            if (t.isPresent()) {
                _process_call_fct(t.get(), c, context, visitor);
                return;
            }
            addErr(NMsg.ofC("undefined node %s", NMsg.ofStyledError(uid)), context, visitor);
        }
    }

    private void compileNodeTree_include(NTxResolutionContext context, CompileNodeVisitor visitor) {
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
                            compileNodeTree(context.setDef(null).setNode((NTxNode) item), visitor);
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

    private void compileNodeTree_import(NTxResolutionContext context, CompileNodeVisitor visitor) {
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

    private void compileNodeTree_if(NTxResolutionContext context, CompileNodeVisitor visitor) {
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
                    compileNodeTree(context.setDef(null).setNode(d), visitor);
                }
            }
        } else {
            List<NTxNode> tb = node.getFalseBloc();
            List<NTxItem> tb2 = new ArrayList<>();
            if (tb != null) {
                for (NTxNode d : tb) {
                    d = d.copy();
                    d.setParent(context.parent());
                    compileNodeTree(context.setDef(null).setNode(d), visitor);
                }
            }
        }
    }

    private void compileNodeTree_for(NTxResolutionContext context, CompileNodeVisitor visitor) {
        CtrlNTxNodeFor node = (CtrlNTxNodeFor) context.node();
        NElement varName = node.getVarName();
        NElement varExpr = node.getVarExpr();
        String varNameStr = null;
        if (varName.isName()) {
            varNameStr = varName.asStringValue().get();
        } else {
            addErr(NMsg.ofC("expected varName in for construct : %s", node), context, visitor);
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
                engine.processNewNode(e,
                        node2 -> {
                            NTxUtils.setNodeParent(node2.get(), context.parent());
                            compileNodeTree(icontext.setDef(null).setNode((NTxNode) node2.get()), visitor);
                        },
                        icontext.withElement(e));
            }
        }
    }

    private void addErr(NMsg msg, NTxResolutionContext context, CompileNodeVisitor visitor) {
        engine.log().log(msg.asError());
        visitor.visitNode(new CtrlNTxNodeError(context.source(), msg), context);
    }

    private void _process_call_fct(NTxFunction t, CtrlNTxNodeCall c, NTxResolutionContext context, CompileNodeVisitor visitor) {
        NTxSource source = NTxUtils.sourceOf(c);
        NElement result = t.invoke(new NTxFunctionArgsImpl(c.getCallArgs(), c, context), context);
        if (result == null) {
            visitor.visitNode(new CtrlNTxNodeSimpleResult(source, NElement.ofNull()), context);
            return;
        }
        engine.processNewNode(result, n -> {
            if (n.isPresent()) {
                NTxItem nn = n.get();
                if (nn instanceof NTxNode) {
                    compileNodeTree(context.setNode((NTxNode) nn), visitor);
                } else if (nn instanceof NTxItemList && ((NTxItemList) nn).getItems().stream().allMatch(x -> x instanceof NTxItem)) {
                    for (NTxItem item : ((NTxItemList) nn).getItems()) {
                        compileNodeTree(context.setNode((NTxNode) item), visitor);
                    }
                } else {
                    addErr(NMsg.ofC("unexpected node type %s", nn.getClass()), context, visitor);
                }
            } else {
                addErr(NMsg.ofC("not found node for %s : %s", result.type().id(), NTxUtils.snippet(result)), context, visitor);
            }
        }, context.withElement(result));
    }


    public void compilePageNode(NTxResolutionContext context, CompileNodeVisitor visitor) {
        if (context.engine() != engine) {
            throw new NIllegalArgumentException(NMsg.ofC("invalid engine"));
        }
        context = context.setInPage(true);
        NAssert.requireNamedNonNull(context.node(), "page");
        NAssert.requireNamedEquals(NTxNodeType.PAGE, context.node().type(), "page");
        compileNodeTree(context, visitor);
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
