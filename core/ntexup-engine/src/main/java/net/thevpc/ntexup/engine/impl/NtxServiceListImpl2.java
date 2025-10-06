package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.engine.NTxDependencyLoadedListener;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.util.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class NtxServiceListImpl2<T> implements NTxDependencyLoadedListener {
    protected DefaultNTxEngine engine;
    private Class<T> serviceType;
    private Map<String, T> map = new LinkedHashMap<>();
    private Map<String, T> map2 = new LinkedHashMap<>();
    private Map<String, String> aliases = new LinkedHashMap<>();
    private String name;

    public NtxServiceListImpl2(String name, Class<T> serviceType, DefaultNTxEngine engine) {
        this.engine = engine;
        this.serviceType = serviceType;
        this.name = name;
        engine.addNTxDependencyLoadedListener(this);
    }

    public void build() {
        List<T> newServices = engine.loadServices(serviceType);
        for (T newService : newServices) {
            onNewService(newService, false);
        }
    }

    @Override
    public void onLoadDependencyLoaded() {
        build();
    }

    protected List<String> aliasesOf(T t) {
        return new ArrayList<>();
    }

    protected String idOf(T t) {
        return t.getClass().getName();
    }

    public void dump(Consumer<NMsg> out) {
        out.accept(NMsg.ofC("%s services : %s base, %s user, %s aliases", NMsg.ofStyledPrimary1(name), map.size(), map2.size(), aliases.size()));
        for (Map.Entry<String, T> e : map.entrySet()) {
            Set<String> a = aliasesFor(e.getKey());
            if (a.isEmpty()) {
                out.accept(NMsg.ofC("\t [BASE] %s : %s", NMsg.ofStyledPrimary1(e.getKey()), e.getValue().getClass()));
            } else {
                out.accept(NMsg.ofC("\t [BASE] %s (%s) : %s", NMsg.ofStyledPrimary1(e.getKey()), String.join(",", a), e.getValue().getClass()));
            }
        }
        for (Map.Entry<String, T> e : map2.entrySet()) {
            Set<String> a = aliasesFor(e.getKey());
            if (a.isEmpty()) {
                out.accept(NMsg.ofC("\t [USER] %s : %s", NMsg.ofStyledPrimary1(e.getKey()), e.getValue().getClass()));
            } else {
                out.accept(NMsg.ofC("\t [USER] %s (%s) : %s", NMsg.ofStyledPrimary1(e.getKey()), String.join(",", a), e.getValue().getClass()));
            }
        }
    }
    private NMsg formattedClass(Class a){
        return NMsg.ofC("%s.%s", NMsg.ofStyledComments(a.getPackage().getName()), NMsg.ofStyledConfig(a.getSimpleName()));
    }

    private Set<String> aliasesFor(String a) {
        a = NTxUtils.uid(a);
        String finalA = a;
        return aliases.keySet().stream().filter(x -> Objects.equals(finalA, aliases.get(x))).collect(Collectors.toSet());
    }

    protected void onNewService(T h, boolean custom) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        String id = NTxUtils.uid(idOf(h));
        set.add(id);
        set.addAll(aliasesOf(h));
        try {
            for (String s : set) {
                requireNotExists(s, h, false);
            }
        } catch (IllegalArgumentException ex) {
            return;
        }
        if (!custom) {
            map.put(id, h);
        } else {
            map2.put(id, h);
        }
        for (String s : set) {
            if (!s.equals(id)) {
                aliases.put(NTxUtils.uid(s), id);
            }
        }
        onAfterNewService(h, false);
        update();
    }


    public void addCustom(T h) {
        onNewService(h, true);
    }

    public void addBase(T h) {
        onNewService(h, false);
    }

    protected void onAfterNewService(T h, boolean custom) {

    }

    public void update() {

    }

    public void requireNotExists(String id, T h, boolean custom) {
        id = NTxUtils.uid(id);
        String customString = " (as custom) ";
        if (map.containsKey(id)) {
            T old = map.get(id);
            throw new IllegalArgumentException("already registered " + name + " " + id + " from " + old.getClass().getSimpleName() + " and you want to add it " + customString + "via " + h.getClass().getSimpleName());
        }
        if (map2.containsKey(id)) {
            T old = map2.get(id);
            throw new IllegalArgumentException("already registered custom " + name + " " + id + " from " + old.getClass().getSimpleName() + " and you want to add it " + customString + "via " + h.getClass().getSimpleName());
        }
        if (aliases.containsKey(id)) {
            String to = aliases.get(NTxUtils.uid(id));
            T old = map.get(to);
            if (old == null) {
                old = map2.get(to);
            }
            throw new IllegalArgumentException("already registered " + name + " " + id + " as alias to" + to + " from " + old.getClass().getSimpleName() + " and you want to add it " + customString + "via " + h.getClass().getSimpleName());
        }
    }

    public boolean exists(String id) {
        id = NTxUtils.uid(id);
        return map.containsKey(id) || map2.containsKey(id) || aliases.containsKey(id);
    }

    public List<T> list() {
        ArrayList<T> a = new ArrayList<>(map.values());
        a.addAll(map2.values());
        return a;
    }

    public NOptional<T> get(String id) {
        String id2 = NTxUtils.uid(id);
        T v = map.get(id2);
        if (v != null) {
            return NOptional.of(v);
        }
        v = map2.get(id2);
        if (v != null) {
            return NOptional.of(v);
        }
        String alias = aliases.get(id);
        if (alias != null) {
            v = map.get(alias);
            if (v != null) {
                return NOptional.of(v);
            }
            v = map2.get(alias);
            if (v != null) {
                return NOptional.of(v);
            }
        }
        return NOptional.ofNamedEmpty(name + " " + id);
    }

    public Map<String, T> map() {
        Map<String, T> a = new LinkedHashMap<>(map);
        a.putAll(map2);
        return a;
    }


}
