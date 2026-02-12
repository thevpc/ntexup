package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.engine.parser.resources.NTxSourceNew;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NCollections;
import net.thevpc.nuts.util.NIllegalArgumentException;
import net.thevpc.nuts.util.NUtils;

import java.util.*;

public class NTxCompiledDocumentImpl implements NTxCompiledDocument {
    private NTxDocument rawDocument;
    private NTxDocument document;
    private NTxEngine engine;
    private List<NTxCompiledPage> compiledPages=new ArrayList<>();
    private Throwable currentThrowable;
    private Deque<NTxNodeAndContext> unparsed=new ArrayDeque<>();
    private List<NTxNodeAndContext> trailingInstrs=new ArrayList<>();
    public NTxCompiledDocumentImpl(NTxDocument rawDocument, NTxEngine engine) {
        this.rawDocument = rawDocument;
        this.engine = engine;
    }

    @Override
    public NTxSource source(){
        return rawDocument.source();
    }

    @Override
    public NTxDocument compiledDocument() {
        if (document == null) {
            try {
                document = engine.compileDocument(rawDocument.copy()).get();
                unparsed.push(new NTxNodeAndContext(document.root(),null));
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

    @Override
    public Iterator<NTxCompiledPage> pagesIterator() {
        return new Iterator<NTxCompiledPage>() {
            int index=0;
            @Override
            public boolean hasNext() {
                while(true) {
                    if (index < compiledPages.size()) {
                        return true;
                    } else {
                        if(!readMore()){
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

    private boolean readMore() {
        List<NTxNodeAndContext> pendingInstr=new ArrayList<>();
        while(!unparsed.isEmpty()){
            NTxNodeAndContext part = unparsed.pop();
            switch (part.node.type()) {
                case NTxNodeType.PAGE: {
                    NTxNode p = part.node;
                    if (!p.isDisabled()) {
                        NTxResolutionContext c = engine.newContext(part.node, document, part.context);
                        compiledPages.add(new NTxCompiledPageImpl(p, this, compiledPages.size(), c, pendingInstr, new NTxPageCompileListener() {
                            @Override
                            public void onBeforeCompile(NTxCompiledPage a) {
                                onBeforeCompileImpl(a);
                            }

                            @Override
                            public void onAfterCompile(NTxCompiledPage a) {
                                onAfterCompileImpl(a);
                            }
                        }));
                        pendingInstr.clear();
                        return true;
                    }
                    break;
                }
                case NTxNodeType.CTRL_ASSIGN:
                case NTxNodeType.CTRL_DEFINE:
                {
                    pendingInstr.add(part);
                    break;
                }
                case NTxNodeType.PAGE_GROUP:
                case NTxNodeType.GROUP: {
                    if (!part.node.isDisabled()) {
                        NTxResolutionContext c = engine.newContext(part.node, document, part.context);
                        List<NTxNode> children = part.node.children();
                        for (int i = children.size() - 1; i >= 0; i--) {
                            unparsed.push(new NTxNodeAndContext(children.get(i),c));
                        }
                    }
                    break;
                }
                default:{
                    throw new NIllegalArgumentException(NMsg.ofC("unexpected"));
                }
            }
        }
        trailingInstrs.addAll(pendingInstr);
        return false;
    }

    public void onBeforeCompileImpl(NTxCompiledPage a){
        for (int i = 0; i < compiledPages.size(); i++) {
            if(i<a.index()){
                compiledPages.get(i).compiledPage();
            }
        }
    }

    public void onAfterCompileImpl(NTxCompiledPage a){
        for (int i = 0; i < compiledPages.size(); i++) {
            if(!compiledPages.get(i).isCompiled()){
                return;
            }
        }
        if(!unparsed.isEmpty()){
            return;
        }
        for (NTxNodeAndContext trailingInstr : trailingInstrs) {
            engine.runNode(trailingInstr.node, document, trailingInstr.context, new CompileNodeVisitorRunner());
        }
    }

    @Override
    public Throwable currentThrowable() {
        return currentThrowable;
    }

}
