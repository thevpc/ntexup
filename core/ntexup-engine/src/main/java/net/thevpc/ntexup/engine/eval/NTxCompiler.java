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
import net.thevpc.ntexup.api.parser.NTxNodeParserFactory;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.impl.NTxEngineUtils;
import net.thevpc.ntexup.engine.log.SilentNTxLogger;
import net.thevpc.ntexup.engine.parser.NTxDocumentLoadingResultImpl;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import net.thevpc.ntexup.engine.parser.ctrlnodes.*;
import net.thevpc.ntexup.engine.parser.nodeparsers.StylesSpecialParser;
import net.thevpc.nuts.concurrent.NScoredCallable;
import net.thevpc.nuts.elem.NElementReader;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.time.NChronometer;
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
        NChronometer chronometer = NChronometer.startNow();
        NTxDocument documentCopy = document0.copy();
        NTxSource source = documentCopy.root().source();
        SilentNTxLogger slog = new SilentNTxLogger();
        try {
            engine.addLog(slog);
            NTxNode root = documentCopy.root();
            List<NTxNode> rootChildren = root.children();
            root.clearChildren();
            NTxResolutionContextImpl context = new NTxResolutionContextImpl(new NTxNode[]{root}, NElement.ofNull(), null, false, engine, documentCopy, null, null, null, null, engine.itemParser());
            DispatchCompileNodeVisitor dv = new DispatchCompileNodeVisitor(new CompileNodeVisitor() {
                @Override
                public void visitNode(NTxNode node, NTxResolutionContext context) {

                }

                @Override
                public void visitRule(NTxStyleRule a, NTxResolutionContext context) {
                    root.addRule(a);
                }

                @Override
                public void visitDefinition(NTxNodeDef a, NTxResolutionContext context) {

                }

                @Override
                public void visitFunction(NTxFunction a, NTxResolutionContext context) {

                }

                @Override
                public void visitProperty(NTxProp a, NTxResolutionContext context) {

                }

                @Override
                public void visitVar(String varName, NTxVar nTxVar, NTxResolutionContext context) {

                }
            });


            try (FillDocumentCompileNodeVisitor visitor = new FillDocumentCompileNodeVisitor(root, engine)) {
                for (NTxNode rootChild : rootChildren) {
                    context.doWithChild(rootChild, cc -> {
                        compileNode(cc, visitor);
                    });
                }
            }
            return new NTxDocumentLoadingResultImpl(documentCopy, source, slog.getErrorCount() == 0);
        } finally {
            engine.removeLog(slog);
            engine.log().log(NMsg.ofC("compiled document in %s", chronometer));
        }
    }

    public void compileNode(NTxResolutionContext context, CompileNodeVisitor visitor) {
        NTxItem item = context.node();
        if (item instanceof DefaultNTxNode) {
            DefaultNTxNode c = (DefaultNTxNode) item;
            if (c.compiling) {
                return;
            }
        }
        try {
            NAssert.requireNamedNonNull(context, "context");
            boolean wasInPage = context.inPage();
            if (item instanceof DefaultNTxNode) {
                DefaultNTxNode c = (DefaultNTxNode) item;
                if (c.compiling) {
                    return;
                }
                c.compiling = true;
            }
            if (item instanceof NTxNode) {
                NTxNode node = (NTxNode) item;
                if (NTxNodeType.PAGE.equals(node.type())) {
                    //update reference!
                    context.setInPage(true);
                }
                if (!wasInPage && context.inPage()) {
                    //exit compilation here, won't process children
                    // because we are compiling document and not yet pages
                    visitor.visitNode(node, context);
                    return;
                }
                if (node instanceof CtrNTxNodelUncompiled) {
                    NElement raw = node.getRaw();
                    NElement oldElement = context.element();
                    try {
                        context.setElement(raw);
                        engine.parseNode(raw, context, o -> {
                            if (o.isPresent()) {
                                NTxItem v = o.get();
                                if (v == node) {
                                    visitor.visitNode(node, context);
                                    return;
                                }
                                new DispatchCompileNodeVisitor(visitor).visitItem(v, context);
                            }
                        });
                    } finally {
                        context.setElement(oldElement);
                    }
                    return;
                }
                node.setTemplateDefinition(context.def());
                //temporarely set parent, it will be changed later!!
                NTxUtils.setNodeParent(node, context.parent());
                context.doWithSibling(node, null, cc -> {
                    switch (node.type()) {
                        case NTxNodeType.CTRL_IF: {
                            compileNodeTree_if(cc, visitor);
                            return;
                        }
                        case NTxNodeType.CTRL_NAME: {
                            compileNodeTree_name(cc, visitor);
                            return;
                        }
                        case NTxNodeType.CTRL_INCLUDE: {
                            compileNodeTree_include(cc, visitor);
                            return;
                        }
                        case NTxNodeType.CTRL_IMPORT: {
                            compileNodeTree_import(cc, visitor);
                            return;
                        }
                        case NTxNodeType.CTRL_FOR: {
                            compileNodeTree_for(cc, visitor);
                            return;
                        }
                        case NTxNodeType.CTRL_ASSIGN: {
                            compileNodeTree_assign(cc, visitor);
                            return;
                        }
                        case NTxNodeType.CTRL_ASSIGN_DEFAULT: {
                            compileNodeTree_assignDefault(cc, visitor);
                            return;
                        }
                        case NTxNodeType.CTRL_EXPR: {
                            compileNodeTree_expr(cc, visitor);
                            return;
                        }
                        case NTxNodeType.CTRL_CALL: {
                            compileNodeTree_call(cc, visitor);
                            return;
                        }
                        case NTxNodeType.CTRL_DEFINE: {
                            compileNodeTree_define(cc, visitor);
                            return;
                        }
                        case NTxNodeType.FRAGMENT: {
                            //inlined same scope
                            compileNodeTree_fragment(cc, visitor);
                            return;
                        }
                        case NTxNodeType.BLOCK: {
                            //inlined but scoped
                            compileNodeTree_block(cc, visitor);
                            return;
                        }
                        case NTxNodeType.PAGE:
                        case NTxNodeType.PAGE_GROUP:
                        case NTxNodeType.GROUP: {
                            //new scope
                            compileNodeTree_container(cc, visitor);
                            return;
                        }
                    }
                    compileNodeTree_default(cc, visitor);
                });
            } else {
                new DispatchCompileNodeVisitor(visitor).visitItem(item, context);
            }
        } finally {
            if (item instanceof DefaultNTxNode) {
                DefaultNTxNode c = (DefaultNTxNode) item;
                if (c.compiling) {
                    c.compiling = false;
                }
            }
        }
    }

//    private void dispatchOrCompileVisit(NTxItem a, CompileNodeVisitor visitor, NTxResolutionContext context, boolean compile) {
//        if (a instanceof NTxItemList) {
//            for (NTxItem item : ((NTxItemList) a).getItems()) {
//                dispatchOrCompileVisit(item, visitor, context, compile);
//            }
//            return;
//        }
//        if (a instanceof NTxNodeDef) {
//            visitor.visitDefinition((NTxNodeDef) a, context);
//        } else if (a instanceof NTxNode) {
//            NTxNode n = (NTxNode) a;
//            switch (((NTxNode) a).type()) {
//                case NTxNodeType.FRAGMENT: {
//                    for (NTxNode child : ((NTxNode) a).children()) {
//                        dispatchOrCompileVisit(child, visitor, context, compile);
//                    }
//                    return;
//                }
//            }
//            if (compile) {
//                context.doWithSibling(n, cc -> {
//                    NTxNode nn = cc.node();
//                    NTxNodeParser p = compileNodeTree(cc,nn.type()).orNull();
//                    if (p != null) {
//                        p.compileNode(nn, context);
//                    }
//                });
//            }
//            visitor.visitNode(n, context);
//        } else if (a instanceof NTxStyleRule) {
//            visitor.visitRule((NTxStyleRule) a, context);
//        } else if (a instanceof NTxFunction) {
//            visitor.visitFunction((NTxFunction) a, context);
//        } else if (a instanceof NTxProp) {
//            visitor.visitProperty((NTxProp) a, context);
//        } else {
//            throw new NIllegalArgumentException(NMsg.ofC("unexpected"));
//        }
//    }

//    private void dispatchVisit(NTxItem a, CompileNodeVisitor visitor, NTxResolutionContext context) {
//        if (a instanceof NTxItemList) {
//            for (NTxItem child : ((NTxItemList) a).getItems()) {
//                dispatchVisit(child, visitor, context);
//            }
//        } else if (a instanceof NTxNodeDef) {
//            visitor.visitDefinition((NTxNodeDef) a, context);
//        } else if (a instanceof NTxNode) {
//            if (NTxUtils.uid(((NTxNode) a).type()).equals(NTxNodeType.FRAGMENT)) {
//                for (NTxNode child : ((NTxNode) a).children()) {
//                    dispatchVisit(child, visitor, context);
//                }
//            } else {
//                visitor.visitNode((NTxNode) a, context);
//            }
//        } else if (a instanceof NTxStyleRule) {
//            visitor.visitRule((NTxStyleRule) a, context);
//        } else if (a instanceof NTxFunction) {
//            visitor.visitFunction((NTxFunction) a, context);
//        } else if (a instanceof NTxProp) {
//            visitor.visitProperty((NTxProp) a, context);
//        } else {
//            throw new NIllegalArgumentException(NMsg.ofC("unexpected"));
//        }
//    }

    private void compileNodeTree_container(NTxResolutionContext context, CompileNodeVisitor visitor) {
        NTxNode node = context.node();
        List<NTxNode> children = node.children();
        node.clearChildren();
        for (NTxNode child : children) {
            context.doWithChild(child, ctx -> compileNode(ctx, new FillNodeCompileNodeVisitor(node)));
        }
        visitor.visitNode(node, context);
    }

    private void compileNodeTree_default(NTxResolutionContext context, CompileNodeVisitor visitor) {
        NTxNode node = context.node();
        String nodeType = node.type();
        NTxNodeParser p = context.itemParser().nodeTypeParser(nodeType).orNull();
        if (p != null) {
            p.compileNode(node, context);
        } else {
            List<NTxNode> children = node.children();
            node.clearChildren();
            for (NTxNode child : children) {
                context.doWithChild(child, new Consumer<NTxResolutionContext>() {
                    @Override
                    public void accept(NTxResolutionContext ctx) {
                        compileNode(ctx, new FillNodeCompileNodeVisitor(node));
                    }
                });
            }
        }
        visitor.visitNode(node, context);
    }

    private void compileNodeTree_fragment(NTxResolutionContext context, CompileNodeVisitor visitor) {
        NTxNode node = context.node();
        List<NTxNode> children = node.children();
        node.clearChildren();
        for (NTxNode child : children) {
            context.doWithChild(child, new Consumer<NTxResolutionContext>() {
                @Override
                public void accept(NTxResolutionContext ctx) {
                    compileNode(ctx, visitor);
                }
            });
        }
    }

    private void compileNodeTree_block(NTxResolutionContext context, CompileNodeVisitor visitor) {
        NTxResolutionContext newContext = context.pushContext();
        NTxNode node = newContext.node();
        List<NTxNode> children = node.children();
        node.clearChildren();
        for (NTxNode child : children) {
            newContext.doWithChild(child, new Consumer<NTxResolutionContext>() {
                @Override
                public void accept(NTxResolutionContext ctx) {
                    compileNode(ctx, visitor);
                }
            });
        }
    }

    private void compileNodeTree_expr(NTxResolutionContext context, CompileNodeVisitor visitor) {
        NElement varExpr = context.node().getProperty(NTxPropName.VALUE).get().getValue();
        NElement element = context.evalExpression(varExpr).get();
        engine.parseNode(element,
                context.withElement(element), a -> {
                    context.parent().mergeNode(a.get());
                }
        );
    }

    private void compileNodeTree_assign(NTxResolutionContext context, CompileNodeVisitor visitor) {
        NTxNode node = context.node();
        String varName = node.getProperty(NTxPropName.NAME).get().getValue().asStringValue().get();
        NElement varExpr = node.getProperty(NTxPropName.VALUE).get().getValue();
        NElement evaluatedExpr = context.evalExpression(varExpr).orNull();
        context.setVar(varName, NTxVar.ofEvaluatedExpression(evaluatedExpr));
        visitor.visitNode(node, context);
    }

    private void compileNodeTree_assignDefault(NTxResolutionContext context, CompileNodeVisitor visitor) {
        NTxNode node = context.node();
        String varName = node.getProperty(NTxPropName.NAME).get().getValue().asStringValue().get();
        NElement varExpr = node.getProperty(NTxPropName.VALUE).get().getValue();
        NElement evaluatedExpr = context.evalExpression(varExpr).orNull();
        NTxVar v = context.getVar(varName).orNull();
        if (v == null) {
            context.setVar(varName, NTxVar.ofEvaluatedExpression(evaluatedExpr));
            visitor.visitNode(node, context);
        }
    }

    private void compileNodeTree_name(NTxResolutionContext context, CompileNodeVisitor visitor) {
        CtrlNTxNodeName node = (CtrlNTxNodeName) context.node();
        NElement c = node.getVarName();
        String name = c.asStringValue().get();
        NOptional<NTxVar> v = context.getVar(name);
        if (v.isPresent()) {
            NTxVar nTxVar = v.get();
            NElement e0 = nTxVar.get();
            List<NElement> all = new ArrayList<>();
            if (e0.isArray()) {
                all.addAll(e0.asArray().get().children());
            } else {
                all.add(e0);
            }
            for (NElement e : all) {
                engine.parseNode(e, context, n -> {
                    if (!n.isPresent()) {
                        context.log().log(NMsg.ofC("variable '%s' as '%s' could not be evaluated as a valid node", name, e).asWarning(), NTxUtils.sourceOf(node));
                    } else {
                        new DispatchCompileNodeVisitor(visitor).visitItem(n.get(), context);
                    }
                });
            }
        } else if (NTxUtils.isComponentBody(name)) {
            //new DispatchCompileNodeVisitor(visitor).visitItem(node,context);
            visitor.visitNode(node, context);
        } else {
            NTxNodeParser p = context.itemParser().nodeTypeParser(name).orNull();
            if (p != null) {
                context.log().log(NMsg.ofC("variable '%s' not found, rendering as plain text.  If you meant a component, use '%s()' syntax", name, name).asWarning(), NTxUtils.sourceOf(node));
            } else {
                context.log().log(NMsg.ofC("variable '%s' not found, rendering as plain text", name).asWarning(), NTxUtils.sourceOf(node));
            }
            DefaultNTxNode t = DefaultNTxNode.ofText(name);
            t.setSource(node.source());
            visitor.visitNode(t, context);
        }
    }

    private void _process_call_node(NTxNodeDef d, CtrlNTxNodeCall c, final NTxResolutionContext context, CompileNodeVisitor visitor) {
        NTxNodeDefParam[] expectedParams = d.params();
        //first unset any parent COMPONENT_BODY_VAR_NAME and create a new context!
        DefaultNTxNode block = DefaultNTxNode.ofBlock();
        block.setParent(d);
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
        NTxResolutionContext newContext = context.pushContext();
        newContext.doWithChild(block, d, cc -> {
            FillNodeCompileNodeVisitor fill = new FillNodeCompileNodeVisitor(block);
            for (NTxNode assign : assigns) {
                cc.doWithChild(assign, new Consumer<NTxResolutionContext>() {
                    @Override
                    public void accept(NTxResolutionContext nTxResolutionContext) {
                        compileNode(cc, fill);
                    }
                });
            }
            for (int i = 0; i < oldBody.length; i++) {
                cc.doWithChild(oldBody[i], new Consumer<NTxResolutionContext>() {
                    @Override
                    public void accept(NTxResolutionContext nTxResolutionContext) {
                        compileNode(cc, fill);
                    }
                });
            }
        });
        visitor.visitNode(block, context);
    }

    private void compileNodeTree_define(NTxResolutionContext context, CompileNodeVisitor visitor) {
        visitor.visitNode(context.node(), context);
        context.setNamedDef((NTxNodeDef) context.node());
    }

    private void compileNodeTree_call(NTxResolutionContext context, CompileNodeVisitor visitor) {
        CtrlNTxNodeCall c = (CtrlNTxNodeCall) context.node();
        String uid = c.getCallName();
        if (!context.inPage()) {
            NOptional<NTxNodeDef> dd = context.getNamedDef(uid);
            if (dd.isPresent()) {
                _process_call_node(dd.get(), c, context, visitor);
            } else {
                NOptional<NTxFunction> t = context.getFunction(uid);
                if (t.isPresent()) {
                    _process_call_fct(t.get(), c, context, visitor);
                    return;
                }
                visitor.visitNode(c, context);
            }
            return;
        }
        NTxNodeParser p = context.itemParser().nodeTypeParser(uid).orNull();
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
                    new DispatchCompileNodeVisitor(visitor).visitItem(t, context);
                }
                return;
            }
        }
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
                            new DispatchCompileNodeVisitor(visitor).visitItem(item, context);
                        } else {
                            context.log().log(NMsg.ofC("invalid include. error loading : %s", nPath).asSevere(), NTxUtils.sourceOf(node));
                        }
                    } else {
                        context.log().log(NMsg.ofC("invalid include. error loading : %s", nPath).asWarning(), NTxUtils.sourceOf(node));
                    }
                }
            } else {
                context.log().log(NMsg.ofC("invalid include. error loading : %s", path).asWarning(), NTxUtils.sourceOf(node));
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
                    new DispatchCompileNodeVisitor(visitor).visitItem(d, context);
                }
            }
        } else {
            List<NTxNode> tb = node.getFalseBloc();
            if (tb != null) {
                for (NTxNode d : tb) {
                    d = d.copy();
                    d.setParent(context.parent());
                    new DispatchCompileNodeVisitor(visitor).visitItem(d, context);
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
                engine.parseNode(e,
                        icontext.withElement(e), node2 -> {
                            NTxUtils.setNodeParent(node2.get(), context.parent());
                            new DispatchCompileNodeVisitor(visitor).visitItem(node2.get(), context);
                        }
                );
            }
        }
    }

    private void addErr(NMsg msg, NTxResolutionContext context, CompileNodeVisitor visitor) {
        engine.log().log(msg.asError());
        visitor.visitNode(new CtrlNTxNodeError(context.source(), msg), context);
    }

    private void _process_call_fct(NTxFunction t, CtrlNTxNodeCall c, NTxResolutionContext context, CompileNodeVisitor visitor) {
        NTxSource source = NTxUtils.sourceOf(c);
        NElement result = t.invoke(new NTxFunctionArgsImpl(t.name(), c.getCallArgs(), c, context), context);
        if (result == null) {
            visitor.visitNode(new CtrlNTxNodeSimpleResult(source, NElement.ofNull()), context);
            return;
        }
        engine.parseNode(result, context.withElement(result), n -> {
            if (n.isPresent()) {
                NTxItem nn = n.get();
                new DispatchCompileNodeVisitor(visitor).visitItem(nn, context);
            } else {
                addErr(NMsg.ofC("not found node for %s : %s", result.type().id(), NTxUtils.snippet(result)), context, visitor);
            }
        });
    }

}
