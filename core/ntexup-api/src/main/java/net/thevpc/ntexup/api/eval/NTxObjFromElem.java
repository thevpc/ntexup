package net.thevpc.ntexup.api.eval;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElementBuilder;
import net.thevpc.nuts.util.NNameFormat;
import net.thevpc.nuts.util.NOptional;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class NTxObjFromElem implements NTxObj {
    private final NElement element;

    public NTxObjFromElem(NElement element) {
        this.element=element;
    }

    @Override
    public Set<String> properties() {
        Set<String> found=new LinkedHashSet<>();
        if(element.isListContainer()){
            for (NElement child : element.asListContainer().get().children()) {
                if(child.isNamedPair()){
                    found.add(child.asPair().get().asStringValue().get());
                }
            }
        }
        if(element.isParametrized()){
            for (NElement child : element.asParametrizedContainer().get().params().get()) {
                if(child.isNamedPair()){
                    found.add(child.asPair().get().asStringValue().get());
                }
            }
        }
        return found;
    }

    @Override
    public NOptional<NTxObj> get(String property) {
        if(element.isListContainer()){
            NOptional<NElement> p = element.asListContainer().get().get(property);
            if(p.isPresent()) {
               return NOptional.of(NTxObjs.elem(p.get()));
            }
        }
        if(element.isParametrized()){
            NOptional<NElement> p = element.asParametrizedContainer().get().param(property);
            if(p.isPresent()) {
                return NOptional.of(NTxObjs.elem(p.get()));
            }
        }
        return NOptional.ofNamedEmpty(property);
    }

    @Override
    public NElement toElement() {
        return this.element;
    }
}
