package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.time.NChronometer;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.util.List;

public class NTxCompiledPageImpl implements NTxCompiledPage {
    private NTxCompiledDocument document;
    private NTxNode rawPage;
    private NTxNode compiledPage;
    private int index;


    public NTxCompiledPageImpl(NTxNode rawPage, NTxCompiledDocument document, int index) {
        this.rawPage = rawPage;
        this.document = document;
        this.index = index;
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
        if(compiledPage == null) {
                NChronometer c = NChronometer.startNow();
                List<NTxNode> all = document.engine().compilePageNode(this.rawPage, document.compiledDocument());
                this.compiledPage = NOptional.ofSingleton(all).get();
                c.stop();
            document.engine().log().log(NMsg.ofC("page %s compiled in %s", (index + 1), c), NTxUtils.sourceOf(this.rawPage));
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
