package net.thevpc.ntexup.api.engine;

import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class NTxTemplateFilter {
    private Supplier<NTxTemplateInfo[]> templatesSupplier;
    private NTxTemplateInfo[] templates;

    public static NTxTemplateFilter of(Supplier<NTxTemplateInfo[]> templatesSupplier) {
        return new NTxTemplateFilter(templatesSupplier);
    }

    public static NTxTemplateFilter of(NTxTemplateInfo[] items) {
        NAssert.requireNonNull(items, "items");
        return new NTxTemplateFilter(() -> items);
    }

    public static boolean isNoneTemplate(String templateUrl) {
        return "none".equalsIgnoreCase(templateUrl) || "na".equalsIgnoreCase(templateUrl) || "n/a".equalsIgnoreCase(templateUrl);
    }

    public NTxTemplateFilter(Supplier<NTxTemplateInfo[]> templatesSupplier) {
        this.templatesSupplier = NAssert.requireNonNull(templatesSupplier, "templatesSupplier");
    }

    public NOptional<NTxTemplateInfo> disambiguate(List<NTxTemplateInfo> found) {
        if (found.isEmpty()) {
            return NOptional.ofNamedEmpty("template");
        }
        if (found.size() == 1) {
            return NOptional.of(found.get(0));
        }
        class A {
            NTxTemplateInfo value;
            int score;
        }
        List<A> ordered = found.stream().map(x -> {
            A a = new A();
            a.value = x;
            if (a.value.recommended()) {
                a.score += 1000;
            }
            if (a.value.repoName().equals("dev")) {
                a.score += 100;
            }
            return a;
        }).sorted(Comparator.comparing((A x) -> x.score).reversed()).collect(Collectors.toList());
        for (Iterator<A> iterator = ordered.iterator(); iterator.hasNext(); ) {
            A a = iterator.next();
            if (a.score < ordered.get(0).score) {
                iterator.remove();
            }
        }
        if (ordered.size() > 1) {
            return NOptional.ofNamedEmpty(NMsg.ofC("ambiguous selection matches : %s", ordered.stream().map(Object::toString).collect(Collectors.joining(", "))));
        }
        return NOptional.of(ordered.get(0).value);
    }

    public String selectOneUrl(String word) {
        if (NBlankable.isBlank(word) || NTxTemplateFilter.isNoneTemplate(word)) {
            return "";
        }
        if (word.contains("/") || word.contains("\\") || word.equals(".") || word.equals("..")) {
            return word;
        } else {
            NOptional<NTxTemplateInfo> u = selectOne(word);
            return u.get().url();
        }
    }

    private NTxTemplateInfo[] getTemplates() {
        if (templates == null) {
            templates = templatesSupplier.get();
            if (templates == null) {
                templates = new NTxTemplateInfo[0];
            }
        }
        return templates;
    }

    public NOptional<NTxTemplateInfo> selectOne(String word) {
        NOptional<NTxTemplateInfo> found = null;
        found = disambiguate(Arrays.stream(getTemplates()).filter(x -> Objects.equals(x.url(), word)).collect(Collectors.toList()));
        if (found.isPresent()) {
            return found;
        }
        found = disambiguate(Arrays.stream(getTemplates()).filter(x -> Objects.equals(x.id(), word)).collect(Collectors.toList()));
        if (found.isPresent()) {
            return found;
        }
        found = disambiguate(Arrays.stream(getTemplates()).filter(x -> Objects.equals(NId.of(x.id()).getLongName(), word)).collect(Collectors.toList()));
        if (found.isPresent()) {
            return found;
        }
        found = disambiguate(Arrays.stream(getTemplates()).filter(x -> Objects.equals(NId.of(x.id()).getShortName(), word)).collect(Collectors.toList()));
        if (found.isPresent()) {
            return found;
        }
        found = disambiguate(Arrays.stream(getTemplates()).filter(x -> NStringUtils.trim(x.name()).equalsIgnoreCase(NStringUtils.trim(word))).collect(Collectors.toList()));
        if (found.isPresent()) {
            return found;
        }
        if (word.startsWith("#")) {
            NOptional<Integer> z = NLiteral.of(word.substring(1).trim()).asInt();
            if (z.isPresent()) {
                int zi = z.get();
                if (zi >= 1 && zi <= getTemplates().length) {
                    return NOptional.of(getTemplates()[zi - 1]);
                }
            }
        }
        {
            NOptional<Integer> z = NLiteral.of(word.trim()).asInt();
            if (z.isPresent()) {
                int zi = z.get();
                if (zi >= 1 && zi <= getTemplates().length) {
                    return NOptional.of(getTemplates()[zi - 1]);
                }
            }
        }
        {
            found = disambiguate(Arrays.stream(getTemplates()).filter(x -> NStringUtils.trim(x.name()).contains(NStringUtils.trim(word))).collect(Collectors.toList()));
            if (found.isPresent()) {
                return found;
            }
        }
        found = disambiguate(Arrays.stream(getTemplates()).filter(x -> Objects.equals(NId.of(x.repoName() + ":" + x.name()).getShortName(), word)).collect(Collectors.toList()));
        if (found.isPresent()) {
            return found;
        }
        found = disambiguate(Arrays.stream(getTemplates()).filter(x -> Objects.equals(NId.of(x.name()).getShortName(), word)).collect(Collectors.toList()));
        if (found.isPresent()) {
            return found;
        }
        return NOptional.ofEmpty(() -> NMsg.ofC("Invalid template url: %s", word));
    }

}
