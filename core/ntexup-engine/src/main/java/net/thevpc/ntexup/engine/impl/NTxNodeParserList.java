package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.parser.NTxNodeParser;

import java.util.*;
import net.thevpc.nuts.artifact.NId;

public class NTxNodeParserList extends NtxServiceListImpl2<NTxNodeParser> {
    public NTxNodeParserList(DefaultNTxEngine engine) {
        super("node parser", NTxNodeParser.class, engine);
    }

    @Override
    protected List<String> aliasesOf(NTxNodeParser nTxNodeParser) {
        return Arrays.asList(nTxNodeParser.aliases());
    }

    @Override
    protected String idOf(NTxNodeParser nTxNodeParser) {
        return nTxNodeParser.id();
    }

    @Override
    protected void onAfterNewService(NTxNodeParser renderer, boolean custom, NId[] dependencies, NId preferredDependency) {
        renderer.init(engine);
    }
}
