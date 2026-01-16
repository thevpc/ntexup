package net.thevpc.ntexup.extension.shapes3d.impl.builders;

import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.extension.shapes3d.impl.NtxElement3DNodeParser;
import net.thevpc.nuts.io.NServiceLoader;
import net.thevpc.nuts.util.NOptional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NtxElement3DNodeParserFactory {
    private Map<String,NtxElement3DNodeParser> map;

    public NtxElement3DNodeParserFactory(NTxEngine engine) {
        this(createMap(engine));
    }
    public NtxElement3DNodeParserFactory(Map<String, NtxElement3DNodeParser> map) {
        this.map = map;
    }

    public static Map<String,NtxElement3DNodeParser> createMap(NTxEngine engine) {
        Map<String,NtxElement3DNodeParser> nodeParsersMap=new HashMap<>();
        for (NtxElement3DNodeParser e : createList(engine)) {
            for (String k : e.getId3d()) {
                nodeParsersMap.put(k,e);
            }
        }
        return nodeParsersMap;
    }

    public static List<NtxElement3DNodeParser> createList(NTxEngine engine) {
        return NServiceLoader.of(NtxElement3DNodeParser.class,null,engine.getEngineClassLoader().asClassLoader()).loadAll(null);
    }

    public NOptional<NtxElement3DNodeParser> resolve(String id){
        return NOptional.ofNamed(map.get(NTxUtils.uid(id)),id);
    }
}
