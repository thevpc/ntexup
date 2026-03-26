package net.thevpc.ntexup.api.eval;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElementBuilder;
import net.thevpc.nuts.util.NOptional;

import java.util.*;

public class NTxObjFromMap implements NTxObj {
    private final Map<String, NTxObj> all;

    public NTxObjFromMap() {
        this.all = new HashMap<>();
    }

    @Override
    public Set<String> properties() {
        return new LinkedHashSet<>(all.keySet());
    }

    @Override
    public NOptional<NTxObj> get(String property) {
        return NOptional.ofNamed(all.get(property), property);
    }

    @Override
    public NElement toElement() {
        NObjectElementBuilder o = NElement.ofObjectBuilder();
        for (Map.Entry<String, NTxObj> e : all.entrySet()) {
            o.add(e.getKey(), e.getValue().toElement());
        }
        return o.build();
    }

    public NTxObj set(String name, NElement e) {
        all.put(name,e);
        return this;
    }
}
