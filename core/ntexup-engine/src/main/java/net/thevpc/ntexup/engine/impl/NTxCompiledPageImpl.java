package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import net.thevpc.ntexup.engine.eval.FillNodeCompileNodeVisitor;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.time.NChronometer;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.util.ArrayList;
import java.util.List;

public class NTxCompiledPageImpl implements NTxCompiledPage {

    private final List<NTxNodeAndContext> prefixInstructions;
    private final NTxCompiledDocument compiledDocument;
    private final NTxNode rawPage;
    private volatile NTxNode compiledPage;
    private final NTxResolutionContext parentContext;
    private NTxResolutionContext pageContext;
    private final int index;
    private final NTxPageCompileListener onCompile;
    private boolean prefixExecuted;

    public NTxCompiledPageImpl(NTxNode rawPage, NTxCompiledDocument compiledDocument, int index, NTxResolutionContext parentContext, List<NTxNodeAndContext> prefixInstructions, NTxPageCompileListener onCompile) {
        this.rawPage = rawPage;
        this.compiledDocument = compiledDocument;
        this.index = index;
        this.parentContext = parentContext;
        this.prefixInstructions = new ArrayList<>(prefixInstructions);
        this.onCompile = onCompile;
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public NTxCompiledDocument document() {
        return compiledDocument;
    }

    public void initialize() {
        if (!prefixExecuted) {
            NChronometer c = NChronometer.of();
            for (NTxNodeAndContext outerInstruction : prefixInstructions) {
                outerInstruction.run(compiledDocument.document(), document().engine(), compiledDocument, this, true);
            }
            c.stop();
            prefixExecuted = true;
            compiledDocument.engine().log().log(NMsg.ofC("page %s initialized in %s", (index + 1), c), NTxUtils.sourceOf(this.rawPage));
        }
    }

    @Override
    public NTxNode compiledPage() {
        if (compiledPage == null) {
            synchronized (this) {
                if (compiledPage == null) {
                    onCompile.onBeforeCompile(this);
                    initialize();
                    NChronometer c = NChronometer.of();
                    pageContext = compiledDocument.engine().newContext(this.rawPage, compiledDocument.document(), compiledDocument, this, parentContext).setInPage(true);

                    NTxNode o = DefaultNTxNode.ofBlock();
                    o.setParent(this.rawPage.parent());
                    NTxNode node = this.rawPage.copy();
                    node.setParent(o);
                    pageContext.doWithChild(node, cc -> {
                        pageContext.engine().compileNode(cc, new FillNodeCompileNodeVisitor(o));
                    });
                    this.compiledPage = NOptional.ofSingleton(o.children()).get();
                    c.stop();
                    compiledDocument.engine().log().log(NMsg.ofC("page %s compiled in %s", (index + 1), c), NTxUtils.sourceOf(this.rawPage));
                    onCompile.onAfterCompile(this);
                }
            }
        }
        return compiledPage;
    }

    @Override
    public NElement toElement(boolean semantic) {
        NTxNode p = compiledPage();
        return compiledDocument.engine().nodeTypeParser(p.type()).get()
                .toElement(p, semantic, compiledDocument.engine());
    }

    public NTxResolutionContext pageContext() {
        compiledPage();
        return pageContext;
    }

    @Override
    public Object source() {
        NTxItem p = rawPage;
        Object s = null;
        while (p != null && s == null) {
            s = p.source();
            p = p.parent();
        }
        return s;
    }

    @Override
    public boolean isCompiled() {
        return compiledPage != null;
    }

    @Override
    public NTxNode rawPage() {
        return rawPage;
    }

}
