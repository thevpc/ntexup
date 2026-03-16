package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeDef;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.engine.CompileNodeVisitor;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxVar;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.io.NClosable;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NCollections;
import net.thevpc.nuts.util.NLiteral;
import net.thevpc.nuts.util.NOptional;

import java.util.*;

public class NTxCompiledDocumentImpl implements NTxCompiledDocument {
    public static final int DEFAULT_MAX_PAGE_COUNT = 1024 * 64;
    public static final int DEFAULT_WARN_PAGE_COUNT = 1024;
    private NTxDocument rawDocument;
    private NTxDocument document;
    private NTxEngine engine;
    private List<NTxCompiledPage> compiledPages = new ArrayList<>();
    private Throwable currentThrowable;
    private Deque<NTxNodeAndContext> unparsed = new ArrayDeque<>();
    private List<NTxNodeAndContext> trailingInstrs = new ArrayList<>();
    private int warnPageCount =DEFAULT_WARN_PAGE_COUNT;
    private int maxPageCount = DEFAULT_MAX_PAGE_COUNT;
    private boolean maxExceeded;

    public NTxCompiledDocumentImpl(NTxDocument rawDocument, NTxEngine engine) {
        this.rawDocument = rawDocument;
        this.engine = engine;
        int _warnPageCount=this.engine.getEnv("warnPageCount").flatMap(x->NLiteral.of(x).asInt()).orElse(DEFAULT_WARN_PAGE_COUNT);
        int _maxPageCount=this.engine.getEnv("maxPageCount").flatMap(x->NLiteral.of(x).asInt()).orElse(DEFAULT_MAX_PAGE_COUNT);
        if(_maxPageCount<_warnPageCount){
            _maxPageCount=_warnPageCount;
        }
        if(_warnPageCount<=0){
            _warnPageCount= DEFAULT_WARN_PAGE_COUNT;
        }
        if(_maxPageCount<=0){
            _maxPageCount= DEFAULT_MAX_PAGE_COUNT;
        }
        if(_maxPageCount<_warnPageCount){
            _maxPageCount=_warnPageCount;
        }
        this.maxPageCount =_maxPageCount;
        this.warnPageCount =_warnPageCount;
    }

    @Override
    public NTxSource source() {
        return rawDocument.source();
    }

    @Override
    public NTxDocument compiledDocument() {
        if (document == null) {
            try {
                document = engine.compileDocument(rawDocument.copy()).get();
                unparsed.push(new NTxNodeAndContext(document.root(), null));
            } catch (Exception ex) {
                engine.log().log(NMsg.ofC("compile document failed %s", ex));
                this.currentThrowable = ex;
            }
        }
        if (document == null) {
            document = engine.documentFactory().ofDocument(null);
        }
        return document;
    }

    @Override
    public boolean isCompiled() {
        return document != null;
    }

    @Override
    public NTxDocument rawDocument() {
        return rawDocument;
    }

    @Override
    public String title() {
        if (rawDocument == null) {
            return "New Document";
        }
        NTxSource source = rawDocument.root().source();

        if (source == null) {
            return ("New Document");
        } else {
            return (String.valueOf(source));
        }
    }

    @Override
    public NTxEngine engine() {
        return engine;
    }

    @Override
    public List<NTxCompiledPage> pages() {
        return NCollections.list(pagesIterator());
    }

    public NOptional<NTxCompiledPage> page(int index) {
        if(index<0){
            return NOptional.ofNamedEmpty("page "+index);
        }
        // check cache first before iterating
        if (index < compiledPages.size()) {
            return NOptional.of(compiledPages.get(index));
        }
        return NClosable.callWith(pagesIterator(),it->{
            int currIndex = 0;
            while (it.hasNext()) {
                NTxCompiledPage o = it.next();
                if (index == currIndex) {
                    return NOptional.of(o);
                }
                currIndex++;
            }
            return NOptional.ofNamedEmpty("page " + index);
        });
    }

    @Override
    public Iterator<NTxCompiledPage> pagesIterator() {
        return new Iterator<NTxCompiledPage>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                while (true) {
                    if (index < compiledPages.size()) {
                        return true;
                    } else {
                        if (!readMore()) {
                            return false;
                        }
                    }
                }
            }

            @Override
            public NTxCompiledPage next() {
                NTxCompiledPage p = compiledPages.get(index);
                index++;
                return p;
            }
        };
    }

    private class PendingAutoPage {
        NTxNode newPage;
        public NTxResolutionContext context;

        public PendingAutoPage(NTxResolutionContext context) {
            this.context=context;
            newPage = engine.documentFactory().ofPage();
            newPage.setParent(context.node());
        }

        public void addChild(NTxNodeAndContext part) {
            if(newPage.source()==null) {
                newPage.setSource(part.node.source());
            }
            newPage.add(part.node);
        }
    }

    private boolean readMore() {
        if(maxExceeded){
            return false;
        }
        List<NTxNodeAndContext> pendingInstr = new ArrayList<>();
        PendingAutoPage pendingAutoPage = null;
        MyNTxPageCompileListener onCompile = new MyNTxPageCompileListener();
        while (!unparsed.isEmpty()) {
            NTxNodeAndContext part = unparsed.pop();
            if (part.node.isDisabled()) {
                continue;
            }
            switch (part.node.type()) {
                case NTxNodeType.PAGE: {
                    if (pendingAutoPage != null) {
                        safeAddPage(new NTxCompiledPageImpl(pendingAutoPage.newPage, this, compiledPages.size(), pendingAutoPage.context, pendingInstr, onCompile));
                        pendingInstr.clear();
                        pendingAutoPage = null;
                    }
                    safeAddPage(new NTxCompiledPageImpl(part.node, this, compiledPages.size(), part.context, pendingInstr, onCompile));
                    pendingInstr.clear();
                    return true;
                }
                case NTxNodeType.CTRL_ASSIGN:
                case NTxNodeType.CTRL_ASSIGN_DEFAULT:
                case NTxNodeType.CTRL_DEFINE: {
                    if (pendingAutoPage != null) {
                        safeAddPage(new NTxCompiledPageImpl(pendingAutoPage.newPage, this, compiledPages.size(), pendingAutoPage.context, pendingInstr, onCompile));
                        pendingInstr.clear();
                        pendingAutoPage = null;
                    }
                    part.run(compiledDocument(),engine,this,null,false);
                    pendingInstr.add(part);
                    break;
                }
                case NTxNodeType.PAGE_GROUP: {
                    if (pendingAutoPage != null) {
                        safeAddPage(new NTxCompiledPageImpl(pendingAutoPage.newPage, this, compiledPages.size(), pendingAutoPage.context, pendingInstr, onCompile));
                        pendingInstr.clear();
                        pendingAutoPage = null;
                    }
                    NTxResolutionContext c = engine.newContext(part.node, document, this,null,part.context);
                    List<NTxNode> children = part.node.children();
                    for (int i = children.size() - 1; i >= 0; i--) {
                        unparsed.push(new NTxNodeAndContext(children.get(i), c));
                    }
                    break;
                }
                case NTxNodeType.BLOCK: {
                    NTxResolutionContext c = engine.newContext(part.node, document,this,null, part.context);
                    List<NTxNode> children = part.node.children();
                    for (int i = children.size() - 1; i >= 0; i--) {
                        unparsed.push(new NTxNodeAndContext(children.get(i), c));
                    }
                    break;
                }
                case NTxNodeType.FRAGMENT: {
                    NTxResolutionContext c = part.context;
                    List<NTxNode> children = part.node.children();
                    for (int i = children.size() - 1; i >= 0; i--) {
                        unparsed.push(new NTxNodeAndContext(children.get(i), c));
                    }
                    break;
                }
                case NTxNodeType.CTRL_CALL: {
                    if (part.context.inPage()) {
                        if (pendingAutoPage == null) {
                            pendingAutoPage = new PendingAutoPage(part.context);
                            pendingAutoPage.addChild(part);
                        } else if (pendingAutoPage.context != part.context) {
                            safeAddPage(new NTxCompiledPageImpl(pendingAutoPage.newPage, this, compiledPages.size(), pendingAutoPage.context, pendingInstr, onCompile));
                            pendingInstr.clear();

                            pendingAutoPage = new PendingAutoPage(part.context);
                            pendingAutoPage.addChild(part);
                            return true;
                        } else {
                            pendingAutoPage.addChild(part);
                        }
                    } else {
                        NTxResolutionContext c = part.context.copy();
                        List<NTxNode> pushMe = new ArrayList<>();
                        c.doWithChild(part.node, null, cc -> {
                            engine.compileNode(cc, new CompileNodeVisitor() {
                                @Override
                                public void visitNode(NTxNode node, NTxResolutionContext context) {
                                    pushMe.add(node);
                                }

                                @Override
                                public void visitRule(NTxStyleRule a, NTxResolutionContext context) {
                                    part.node.addRule(a);
                                }

                                @Override
                                public void visitDefinition(NTxNodeDef a, NTxResolutionContext context) {
                                    pushMe.add(a);
                                }

                                @Override
                                public void visitFunction(NTxFunction a, NTxResolutionContext context) {

                                }

                                @Override
                                public void visitProperty(NTxProp a, NTxResolutionContext context) {
                                    part.node.setProperty(a);
                                }

                                @Override
                                public void visitVar(String varName, NTxVar nTxVar, NTxResolutionContext context) {

                                }
                            });
                        });
                        for (int i = pushMe.size() - 1; i >= 0; i--) {
                            NTxNodeAndContext pp = new NTxNodeAndContext(pushMe.get(i), c);
                            if(pp.node ==part.node){
                                engine.log().log(NMsg.ofC("unable to compile %s", part.node));
                                if (pendingAutoPage == null) {
                                    pendingAutoPage = new PendingAutoPage(pp.context);
                                    pendingAutoPage.addChild(pp);
                                } else if (pendingAutoPage.context != pp.context) {
                                    safeAddPage(new NTxCompiledPageImpl(pendingAutoPage.newPage, this, compiledPages.size(), pendingAutoPage.context, pendingInstr, onCompile));
                                    pendingInstr.clear();

                                    pendingAutoPage = new PendingAutoPage(pp.context);
                                    pendingAutoPage.addChild(pp);
                                    return true;
                                } else {
                                    pendingAutoPage.addChild(pp);
                                }
                            }else {
                                unparsed.push(pp);
                            }
                        }
                    }
                    break;
                }
                case NTxNodeType.GROUP:
                default: {
                    if (pendingAutoPage == null) {
                        pendingAutoPage = new PendingAutoPage(part.context);
                        pendingAutoPage.addChild(part);
                    } else if (pendingAutoPage.context != part.context) {
                        safeAddPage(new NTxCompiledPageImpl(pendingAutoPage.newPage, this, compiledPages.size(), pendingAutoPage.context, pendingInstr, onCompile));
                        pendingInstr.clear();

                        pendingAutoPage = new PendingAutoPage(part.context);
                        pendingAutoPage.addChild(part);
                        return true;
                    } else {
                        pendingAutoPage.addChild(part);
                    }
                }
            }
        }
        if (pendingAutoPage != null) {
            safeAddPage(new NTxCompiledPageImpl(pendingAutoPage.newPage, this, compiledPages.size(), pendingAutoPage.context, pendingInstr, onCompile));
            pendingInstr.clear();
            pendingAutoPage = null;
            return true;
        }
        trailingInstrs.addAll(pendingInstr);
        return false;
    }
    private boolean safeAddPage(NTxCompiledPageImpl a ){
        compiledPages.add(a);
// soft limit — warn but continue
        if (compiledPages.size() > warnPageCount) {
            this.engine.log().log(NMsg.ofC("page count %d exceeds warning threshold", compiledPages.size()).asWarning());
        }
// hard limit — stop generating
        if (compiledPages.size() > maxPageCount) {
            this.engine.log().log(NMsg.ofC("page count %d exceeds maximum, stopping", compiledPages.size()).asError());
            maxExceeded=true;
            return false; // in readMore()
        }
        return true;
    }

    public void onBeforeCompileImpl(NTxCompiledPage a) {
        for (int i = 0; i < compiledPages.size(); i++) {
            if (i < a.index()) {
                NTxCompiledPageImpl nTxCompiledPage = (NTxCompiledPageImpl) compiledPages.get(i);
                nTxCompiledPage.initialize();
            } else {
                break;
            }
        }
    }

    public void onAfterCompileImpl(NTxCompiledPage a) {
        for (int i = 0; i < compiledPages.size(); i++) {
            if (!compiledPages.get(i).isCompiled()) {
                return;
            }
        }
        if (!unparsed.isEmpty()) {
            return;
        }
        for (NTxNodeAndContext trailingInstr : trailingInstrs) {
            trailingInstr.run(document,engine,this,a,false);
        }
    }

    @Override
    public Throwable currentThrowable() {
        return currentThrowable;
    }

    private class MyNTxPageCompileListener implements NTxPageCompileListener {
        @Override
        public void onBeforeCompile(NTxCompiledPage a) {
            onBeforeCompileImpl(a);
        }

        @Override
        public void onAfterCompile(NTxCompiledPage a) {
            onAfterCompileImpl(a);
        }
    }
}
