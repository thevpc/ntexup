package net.thevpc.ntexup.api.engine;

import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NLiteral;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NStringUtils;
import net.thevpc.nuts.util.NValidationException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NTxTemplateFilter {
    private NTxTemplateInfo[] templates;
    public static NTxTemplateFilter of(NTxTemplateInfo[] items){
        return new NTxTemplateFilter(items);
    }

    public NTxTemplateFilter(NTxTemplateInfo[] templates) {
        this.templates = templates;
    }

    public NOptional<NTxTemplateInfo> selectOne(String word){
        List<NTxTemplateInfo> found = null;
        found=Arrays.stream(templates).filter(x -> Objects.equals(x.url(),word)).collect(Collectors.toList());
        if(!found.isEmpty()){
            return NOptional.of(found.get(0));
        }
        found=Arrays.stream(templates).filter(x -> Objects.equals(x.id(),word)).collect(Collectors.toList());
        if(!found.isEmpty()){
            return NOptional.of(found.get(0));
        }
        found=Arrays.stream(templates).filter(x -> Objects.equals(NId.of(x.id()).getLongName(),word)).collect(Collectors.toList());
        if(!found.isEmpty()){
            return NOptional.of(found.get(0));
        }
        found=Arrays.stream(templates).filter(x -> Objects.equals(NId.of(x.id()).getShortName(),word)).collect(Collectors.toList());
        if(!found.isEmpty()){
            return NOptional.of(found.get(0));
        }
        found = Arrays.stream(templates).filter(x -> NStringUtils.trim(x.name()).equalsIgnoreCase(NStringUtils.trim(word))).collect(Collectors.toList());
        if(!found.isEmpty()) {
            return NOptional.of(found.get(0));
        }
        if(word.startsWith("#")){
            NOptional<Integer> z = NLiteral.of(word.substring(1).trim()).asInt();
            if(z.isPresent()){
                int zi = z.get();
                if(zi>=1 && zi<=templates.length){
                    return NOptional.of(templates[zi-1]);
                }
            }
        }
        {
            NOptional<Integer> z = NLiteral.of(word.trim()).asInt();
            if(z.isPresent()){
                int zi = z.get();
                if(zi>=1 && zi<=templates.length){
                    return NOptional.of(templates[zi-1]);
                }
            }
        }
        {
            found = Arrays.stream(templates).filter(x -> NStringUtils.trim(x.name()).contains(NStringUtils.trim(word))).collect(Collectors.toList());
            if(found.size()==1){
                return NOptional.of(found.get(0));
            }else if(found.size()>1){
                return NOptional.ofError(NMsg.ofC("ambiguous selection matches : %s", found.stream().map(Object::toString).collect(Collectors.joining(", "))));
            }
        }
        return NOptional.ofEmpty(()->NMsg.ofC("Invalid template url: %s", word));
    }

}
