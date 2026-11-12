package net.thevpc.ntexup.api.document.style;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.util.*;

public class NTxProperties {
    private NTxNode parent;
    private final Map<String, NTxProp> map = new HashMap<>();
    private final Collection<NTxProp> values = Collections.unmodifiableCollection(map.values());
    private Runnable mutationListener;

    public NTxProperties(NTxNode parent) {
        this.parent = parent;
    }

    public NTxProperties() {
    }

    void setMutationListener(Runnable mutationListener) {
        this.mutationListener = mutationListener;
    }

    private void changed() {
        if (mutationListener != null) {
            mutationListener.run();
        }
    }

    public boolean containsKey(String k) {
        return map.containsKey(k);
    }

    public void set(String s, NElement value) {
        map.put(s, new NTxProp(s, value, parent));
        changed();
    }

    public void set(NTxProp... c) {
        if (c != null) {
            for (NTxProp cc : c) {
                set(cc);
            }
        }
    }

    public void set(Collection<NTxProp> c) {
        if (c != null) {
            for (NTxProp cc : c) {
                set(cc);
            }
        }
    }

    public void unset(NTxProp s) {
        if (s != null) {
            if (map.remove(s.getName()) != null) {
                changed();
            }
        }
    }

    public void unset(String s) {
        if (s != null) {
            if (map.remove(s) != null) {
                changed();
            }
        }
    }

    public void set(NTxProp s) {
        if (s != null) {
            map.put(s.getName(), new NTxProp(s.getName(), s.getValue(), parent));
            changed();
        }
    }

    public NOptional<NTxProp> get(String... names) {
        NTxProp last = null;
        for (String name : names) {
            NTxProp hProp = map.get(name);
            if (hProp != null) {
                last = hProp;
            }
        }
        return NOptional.of(last, () -> NMsg.ofC("style " +
                (names.length == 1 ? names[0] : Arrays.asList(names).toString())));
    }

    public NOptional<NTxProp> get(String s) {
        return NOptional.of(map.get(s), () -> NMsg.ofC("style " + s));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NTxProperties hStyleMap = (NTxProperties) o;
        return Objects.equals(map, hStyleMap.map);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(map);
    }

    @Override
    public String toString() {
        return "Props" + map.values().toString();
    }

    public int size() {
        return map.size();
    }

    public Set<NTxProp> toSet() {
        return new HashSet<>(map.values());
    }

    public NTxProp[] toArray() {
        return values.toArray(new NTxProp[0]);
    }

    public List<NTxProp> toList() {
        return new ArrayList<>(values);
    }

    /** Returns an unmodifiable, live view of this property's values. */
    public Collection<NTxProp> values() {
        return values;
    }

    public NElement toElement() {
        return NElement.ofObject(
                map.values().stream().map(x -> x.toElement()).toArray(NElement[]::new)
        );
    }

    public void clear() {
        if (!map.isEmpty()) {
            map.clear();
            changed();
        }
    }
}
