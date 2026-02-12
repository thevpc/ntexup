package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.time.NChronometer;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.util.ArrayList;
import java.util.List;

public class NTxCompiledPageImpl implements NTxCompiledPage {
    private NTxCompiledDocument document;
    private NTxNode rawPage;
    private NTxNode compiledPage;
    private NTxResolutionContext parentContext;
    private List<NTxNodeAndContext> prefixInstructions;
    private int index;
    private NTxPageCompileListener onCompile;


    public NTxCompiledPageImpl(NTxNode rawPage, NTxCompiledDocument document, int index, NTxResolutionContext parentContext, List<NTxNodeAndContext> prefixInstructions,NTxPageCompileListener onCompile) {
        this.rawPage = rawPage;
        this.document = document;
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
        return document;
    }

    @Override
    public NTxNode compiledPage() {
        if (compiledPage == null) {
            onCompile.onBeforeCompile(this);
            NChronometer c = NChronometer.startNow();
            for (NTxNodeAndContext outerInstruction : prefixInstructions) {
                document.engine().runNode(outerInstruction.node, document.compiledDocument(), outerInstruction.context, new CompileNodeVisitorRunner());
            }
            List<NTxNode> all = document.engine().compilePageNode(this.rawPage, document.compiledDocument(), parentContext);
            this.compiledPage = NOptional.ofSingleton(all).get();
            c.stop();
            document.engine().log().log(NMsg.ofC("page %s compiled in %s", (index + 1), c), NTxUtils.sourceOf(this.rawPage));
            onCompile.onAfterCompile(this);
        }
        return compiledPage;
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
