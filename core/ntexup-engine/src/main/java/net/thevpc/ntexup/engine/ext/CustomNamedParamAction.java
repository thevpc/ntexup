package net.thevpc.ntexup.engine.ext;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.parser.NTxArgumentReader;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.parser.NTxParserUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NElementType;
import net.thevpc.nuts.elem.NElementTypeGroup;
import net.thevpc.nuts.elem.NPairElement;
import net.thevpc.nuts.util.NBlankable;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.util.*;
import java.util.function.Predicate;

class CustomNamedParamAction implements NTxNodeBuilderContext.NamedParamAction, NTxNodeBuilderContext.ProcessParamAction {
    NTxNodeBuilderContextImpl base;
    Set<String> expectedNamedPairNames = new HashSet<>();
    List<Predicate<NTxArgumentReader>> matches=new ArrayList<>();
    NTxNodeBuilderContext.ToNameResolver toNameResolver;
    boolean flags;
    boolean ignoreCommonFlags;
    NTxNodeBuilderContext.PropResolver propResolver;
    boolean ignoreDuplicates;
    boolean matchesLeading;
    NTxNodeBuilderContext.StoreAction storeAction;

    public CustomNamedParamAction(NTxNodeBuilderContextImpl base) {
        if (base.customNamedParamAction != null) {
            throw new RuntimeException("must call end");
        }
        this.base = base;
        base.customNamedParamAction = this;
    }


    @Override
    public NTxNodeBuilderContext.NamedParamAction asFlags() {
        this.flags = true;
        return this;
    }

    public boolean isIgnoreDuplicates() {
        return ignoreDuplicates;
    }

    public NTxNodeBuilderContext.NamedParamAction ignoreDuplicates() {
        this.ignoreDuplicates = true;
        return this;
    }

    public NTxNodeBuilderContext.NamedParamAction ignoreDuplicates(boolean ignoreDuplicates) {
        this.ignoreDuplicates = ignoreDuplicates;
        return this;
    }

    public NTxNodeBuilderContext.NamedParamAction matchesLeading() {
        this.matchesLeading = true;
        return this;
    }

    public NTxNodeBuilderContext.NamedParamAction matchesLeading(boolean matchesLeading) {
        this.matchesLeading = matchesLeading;
        return this;
    }

    @Override
    public NTxNodeBuilderContext.NamedParamAction matchesNonCommonFlags() {
        this.ignoreCommonFlags = true;
        return this;
    }

    @Override
    public NTxNodeBuilderContext.NamedParamAction matchesString() {
        return matches(new Predicate<NTxArgumentReader>() {
            @Override
            public boolean test(NTxArgumentReader info) {
                NElement x = info.peek();
                return x.type().typeGroup() == NElementTypeGroup.STRING;
            }

            @Override
            public String toString() {
                return "(type().typeGroup() == STRING)";
            }
        });
    }

    @Override
    public NTxNodeBuilderContext.NamedParamAction matchesStringOrName() {
        return matches(new Predicate<NTxArgumentReader>() {
            @Override
            public boolean test(NTxArgumentReader info) {
                NElement x = info.peek();
                return x.type().typeGroup() == NElementTypeGroup.STRING || x.type().typeGroup() == NElementTypeGroup.NAME;
            }

            @Override
            public String toString() {
                return "(type().typeGroup() == STRING|NAME)";
            }
        });
    }

    @Override
    public NTxNodeBuilderContext.NamedParamAction matchesAny() {
        return matches(new Predicate<NTxArgumentReader>() {
            @Override
            public boolean test(NTxArgumentReader info) {
                return true;
            }

            @Override
            public String toString() {
                return "any";
            }
        });
    }

    @Override
    public NTxNodeBuilderContext.NamedParamAction matchesAnyNonPair() {
        return matches(new Predicate<NTxArgumentReader>() {
            @Override
            public boolean test(NTxArgumentReader info) {
                NElement x = info.peek();
                return x.type() != NElementType.PAIR;
            }

            @Override
            public String toString() {
                return "any";
            }
        });
    }

    @Override
    public NTxNodeBuilderContext.NamedParamAction matchesName() {
        return matches(new Predicate<NTxArgumentReader>() {
            @Override
            public boolean test(NTxArgumentReader info) {
                NElement x = info.peek();
                return x.type().typeGroup() == NElementTypeGroup.NAME;
            }

            @Override
            public String toString() {
                return "(type().typeGroup() == NAME)";
            }
        });
    }

    @Override
    public NTxNodeBuilderContext.NamedParamAction matches(Predicate<NTxArgumentReader> predicate) {
        if(predicate!=null){
            this.matches.add(predicate);
        }
        return this;
    }


    @Override
    public NTxNodeBuilderContext.NamedParamAction matchesNamedPair(String... names) {
        expectedNamedPairNames.addAll(Arrays.asList(names));
        return this;
    }

    @Override
    public NTxNodeBuilderContext.NamedParamAction store(NTxNodeBuilderContext.StoreAction storeAction) {
        this.storeAction=storeAction;
        if(storeAction!=null) {
            this.toNameResolver = null;
        }
        return this;
    }

    @Override
    public NTxNodeBuilderContext.NamedParamAction storeName(String newName) {
        this.toNameResolver = newName == null ? null : (visitedName, info, buildContext) -> newName;
        if (newName != null) {
            base.addParamName(newName);
        }
        if(newName!=null) {
            this.storeAction = null;
        }
        return this;
    }

    @Override
    public NTxNodeBuilderContext.NamedParamAction storeName(NTxNodeBuilderContext.ToNameResolver newName) {
        this.toNameResolver = newName;
        if(newName!=null) {
            this.storeAction = null;
        }
        return this;
    }

    @Override
    public NTxNodeBuilderContext.NamedParamAction storeFirstMissingName(String... names) {
        if(names!=null && names.length>0){
            matchesNonCommonFlags();
            matchesMissingProperties(names);
            return storeName(new NTxNodeBuilderContext.ToNameResolver() {
                @Override
                public String resolveName(String visitedName, NTxArgumentReader info, NTxNodeBuilderContext buildContext) {
                    NTxNode node = info.node();
                    for (String name : names) {
                        if(!node.getProperty(name).isPresent()){
                            return name;
                        }
                    }
                    return names[names.length-1];
                }
            });
        }
        return this;
    }

    @Override
    public NTxNodeBuilderContext.NamedParamAction matchesMissingProperties(String... names) {
        if(names!=null && names.length>0){
            return
                    matches(new Predicate<NTxArgumentReader>() {
                @Override
                public boolean test(NTxArgumentReader nTxArgumentReader) {
                    for (String name : names) {
                        if(!nTxArgumentReader.node().getProperty(name).isPresent()){
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
        return this;
    }

    @Override
    public NTxNodeBuilderContext.NamedParamAction resolvedAs(NTxNodeBuilderContext.PropResolver a) {
        this.propResolver = a;
        return this;
    }

    @Override
    public NTxNodeBuilderContext then() {
        return end();
    }

    @Override
    public NTxNodeBuilderContext end() {
        for (String n : expectedNamedPairNames) {
            base.addParamName(n);
        }
        if (base.processSingleArgumentList == null) {
            base.processSingleArgumentList = new ArrayList<>();
        }
        base.processSingleArgumentList.add(this);
        this.base.customNamedParamAction = null;
        return base;
    }

    public boolean processParam(NTxArgumentReader info, NTxNodeBuilderContext buildContext) {
        if (matchesLeading) {
            if (info.currentIndex() != 0) {
                return false;
            }
        }
        NElement n = info.peek();
        switch (n.type()) {
            case PAIR: {
                NPairElement thePair = n.asPair().get();
                if (!expectedNamedPairNames.isEmpty()) {
                    if (n.isNamedPair()) {
                        String uid = NTxUtils.uid(thePair.key().asStringValue().get());
                        boolean u = expectedNamedPairNames.contains(uid);
                        if (u) {
                            info.read();
                            setProp(uid, thePair.value(), info, buildContext);
                            return true;
                        }
                    }
                }
                if (dmatches(null, info)) {
                    info.read();
                    String uid = null;
                    if (n.isNamedPair()) {
                        uid = NTxUtils.uid(thePair.key().asStringValue().get());
                    }
                    setProp(uid, thePair.value(), info, buildContext);
                    return true;
                }
                break;
            }
            case NAME: {
                String uid = NTxUtils.uid(n.asNameValue().get());
                if (flags && !expectedNamedPairNames.isEmpty()) {
                    boolean u = expectedNamedPairNames.contains(uid);
                    if (u) {
                        info.read();
                        setProp(uid, NElement.ofTrue(), info, buildContext);
                        return true;
                    }
                }
                if (dmatches(uid, info)) {
                    info.read();
                    setProp(uid, n, info, buildContext);
                    return true;
                }
                break;
            }
            case NAMED_ARRAY:
            case NAMED_PARAMETRIZED_ARRAY: {
                String uid = NTxUtils.uid(n.asArray().get().name().orNull());
                if (dmatches(null, info)) {
                    info.read();
                    setProp(uid, n, info, buildContext);
                    return true;
                }
                break;
            }
            case NAMED_OBJECT:
            case NAMED_PARAMETRIZED_OBJECT: {
                String uid = NTxUtils.uid(n.asObject().get().name().orNull());
                if (dmatches(null, info)) {
                    info.read();
                    setProp(uid, n, info, buildContext);
                    return true;
                }
                break;
            }
            case NAMED_UPLET: {
                String uid = NTxUtils.uid(n.asUplet().get().name().orNull());
                if (dmatches(null, info)) {
                    info.read();
                    setProp(uid, n, info, buildContext);
                    return true;
                }
                break;
            }
            default: {
                if (dmatches(null, info)) {
                    info.read();
                    setProp(null, n, info, buildContext);
                    return true;
                }
                break;
            }
        }
        return false;
    }

    private boolean dmatches(String name, NTxArgumentReader info) {
        boolean someTest=false;
        if (!NBlankable.isBlank(name)) {
            if (ignoreCommonFlags) {
                if (NTxParserUtils.isCommonStyleFlagProperty(name)) {
                    return false;
                }
                someTest=true;
            }
        }
        if(expectedNamedPairNames!=null && !expectedNamedPairNames.isEmpty()){
            if(name==null){
                return false;
            }
            String uid = NTxUtils.uid(name);
            if(!expectedNamedPairNames.contains(uid)){
                return false;
            }
            someTest=true;
        }
        if (matches != null && !matches.isEmpty()) {
            for (Predicate<NTxArgumentReader> a : matches) {
                if(!a.test(info)){
                    return false;
                }
                someTest=true;
            }
        }
        return someTest;
    }

    private void setProp(String uid, NElement p, NTxArgumentReader info, NTxNodeBuilderContext buildContext) {
        try {
            if(storeAction!=null){
                storeAction.store(info,  buildContext);
            }else {
                String okName;
                if (toNameResolver == null) {
                    okName = uid == null ? null : NTxUtils.uid(uid);
                } else {
                    okName = NTxUtils.uid(toNameResolver.resolveName(uid, info, buildContext));
                }
                if (NBlankable.isBlank(okName)) {
                    return;
                }
                NTxProp prp = propResolver == null ?
                        NTxProp.of(okName, p)
                        : propResolver.resolve(okName, p, info, buildContext);
                String oldCDP = NTxUtils.getCompilerDeclarationPath(p);
                String newCDP = NTxUtils.getCompilerDeclarationPath(prp.getValue());
                if (newCDP == null) {
                    prp = NTxProp.of(prp.getName(), NTxUtils.addCompilerDeclarationPath(prp.getValue(), oldCDP));
                }
                if (ignoreDuplicates) {
                    NOptional<NTxProp> o = info.node().getProperty(prp.getName());
                    if (!o.isPresent()) {
                        info.node().setProperty(prp);
                    }
                } else {
                    info.node().setProperty(prp);
                }
            }
        } catch (Exception ex) {
            info.parseContext().messages().log(
                    NMsg.ofC("unable to set %s : %s", NTxUtils.snippet(p), ex).asSevere()
            );
        }
    }

}
