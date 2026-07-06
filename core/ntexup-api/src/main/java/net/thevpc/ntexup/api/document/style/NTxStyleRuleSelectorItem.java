package net.thevpc.ntexup.api.document.style;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.nuts.util.NOptional;
import net.thevpc.nuts.util.NStringUtils;

import java.util.*;
import java.util.stream.Collectors;

public abstract class NTxStyleRuleSelectorItem {

    public abstract boolean acceptNode(NTxNode n);


    public static class AnyItem extends NTxStyleRuleSelectorItem {
        @Override
        public boolean acceptNode(NTxNode n) {
            return true;
        }

        @Override
        public boolean equals(Object o) {
            return o != null && getClass() == o.getClass();
        }

        @Override
        public int hashCode() {
            return "AnyItem".hashCode();
        }

        @Override
        public String toString() {
            return "*";
        }
    }

    public static class NoneItem extends NTxStyleRuleSelectorItem {
        @Override
        public boolean acceptNode(NTxNode n) {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            return o != null && getClass() == o.getClass();
        }

        @Override
        public int hashCode() {
            return "NoneItem".hashCode();
        }

        @Override
        public String toString() {
            return "!";
        }
    }

    public static class SimpleItem extends NTxStyleRuleSelectorItem {
        private Set<String> types;
        private Set<String> names;
        private Set<String> classes;

        public SimpleItem(Set<String> types, Set<String> names, Set<String> classes) {
            this.types = types;
            this.names = names;
            this.classes = classes;
        }

        @Override
        public boolean acceptNode(NTxNode n) {
            if (!types.isEmpty() && !types.contains(n.type())) return false;
            if (!names.isEmpty() && !names.contains(n.name())) return false;
            if (!classes.isEmpty()) {
                Set<String> nodeClasses = n.styleClasses();
                if (!nodeClasses.containsAll(this.classes)) return false;
            }
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            SimpleItem item = (SimpleItem) o;
            return Objects.equals(types, item.types) && Objects.equals(names, item.names) && Objects.equals(classes, item.classes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(types, names, classes);
        }

        @Override
        public String toString() {
            String nc = names.stream().map(x -> ":" + x).collect(Collectors.joining()) + classes.stream().map(x -> "." + x).collect(Collectors.joining());
            if (types.isEmpty()) {
                return nc;
            } else if (types.size() == 1) {
                return types.toArray()[0].toString() +nc;
            } else {
                return types.stream().map(x -> "$" + x).collect(Collectors.joining()) + nc;
            }
        }

        public Set<String> getNames() {
            return names;
        }

        public Set<String> getTypes() {
            return types;
        }

        public Set<String> getClasses() {
            return classes;
        }
    }

    public static NTxStyleRuleSelectorItem ofClasses(String... classes) {
        return of(null,null,classes);
    }

    public static NTxStyleRuleSelectorItem of(String[] types, String[] names, String[] classes) {
        Set<String> stypes = types == null ? Collections.emptySet() : Arrays.stream(types).map(x -> NStringUtils.strip(x)).filter(x -> x.length() > 0).collect(Collectors.toSet());
        Set<String> snames = names == null ? Collections.emptySet() : Arrays.stream(names).map(x -> NStringUtils.strip(x)).filter(x -> x.length() > 0).collect(Collectors.toSet());
        Set<String> sclasses = classes == null ? Collections.emptySet() : Arrays.stream(classes).map(x -> NStringUtils.strip(x)).filter(x -> x.length() > 0).collect(Collectors.toSet());
        if (stypes.isEmpty() && snames.isEmpty() && sclasses.isEmpty()) {
            return DefaultNTxNodeSelector.ANY_ITEM;
        }
        return new SimpleItem(stypes, snames, sclasses);
    }

    public static NOptional<NTxStyleRuleSelectorItem> of(String item, NTxLogger log) {
        item = NStringUtils.strip(item);
        if (item.isEmpty() || item.equals("*")) {
            return NOptional.of(DefaultNTxNodeSelector.ANY_ITEM);
        }
        Set<String> types = new HashSet<>();
        Set<String> names = new TreeSet<>();
        Set<String> classes = new TreeSet<>();

        // We use a regex-free scan for maximum performance (Skippy style)
        int length = item.length();
        int start = 0;

        char firstChar = item.charAt(0);
        if (firstChar != '.' && firstChar != ':' && firstChar != '$') {
            int end = findNextDelimiter(item, 1);
            types.add(item.substring(0, end));
            start = end;
        }
        // 2. Parse the rest (Names and Classes)
        while (start < length) {
            char prefix = item.charAt(start);
            int end = findNextDelimiter(item, start + 1);
            String value = item.substring(start + 1, end);

            if (!value.isEmpty()) {
                if (prefix == ':') {
                    names.add(value);
                } else if (prefix == '.') {
                    classes.add(value);
                } else if (prefix == '$') {
                    types.add(value);
                }
            }
            start = end;
        }
        if (types.isEmpty() && names.isEmpty() && classes.isEmpty()) {
            return NOptional.of(DefaultNTxNodeSelector.ANY_ITEM);
        }
        return NOptional.of(new SimpleItem(types, names, classes));
    }

    private static int findNextDelimiter(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.' || c == ':' || c == '$') {
                return i;
            }
        }
        return s.length();
    }
}
