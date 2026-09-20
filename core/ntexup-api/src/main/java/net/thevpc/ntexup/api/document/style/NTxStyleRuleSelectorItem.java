package net.thevpc.ntexup.api.document.style;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NMsg;
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

        public SimpleItem(Set<String> types, Set<String> names) {
            this.types = types;
            this.names = names;
        }

        @Override
        public boolean acceptNode(NTxNode n) {
            if (!types.isEmpty() && !types.contains(n.type())) return false;
            if (!names.isEmpty() && !names.contains(n.name())) return false;
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            SimpleItem item = (SimpleItem) o;
            return Objects.equals(types, item.types) && Objects.equals(names, item.names);
        }

        @Override
        public int hashCode() {
            return Objects.hash(types, names);
        }

        @Override
        public String toString() {
            String nc = names.stream().map(x -> ":" + x).collect(Collectors.joining());
            if (types.isEmpty()) {
                return nc;
            } else if (types.size() == 1) {
                return types.toArray()[0].toString() + nc;
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
    }

    /**
     * Class definition: {@code class-<name>(<base>, <base>, ...)}.
     * This is a <b>definition</b>, not a matcher: {@link #acceptNode} always
     * returns false. Resolution folds the flattened definition into nodes that
     * reference the class by name (usage-site {@code @(name)} classes).
     */
    public static class ClassDefItem extends NTxStyleRuleSelectorItem {
        private final String name;
        private final List<String> bases;

        public ClassDefItem(String name, List<String> bases) {
            this.name = name == null ? "" : name;
            this.bases = bases == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(bases));
        }

        public String getName() {
            return name;
        }

        public List<String> getBases() {
            return bases;
        }

        @Override
        public boolean acceptNode(NTxNode n) {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ClassDefItem item = (ClassDefItem) o;
            return Objects.equals(name, item.name) && Objects.equals(bases, item.bases);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, bases);
        }

        @Override
        public String toString() {
            if (bases.isEmpty()) {
                return "class-" + name;
            }
            return "class-" + name + "(" + String.join(", ", bases) + ")";
        }
    }

    /**
     * Class <b>usage</b>: {@code class-<name>} referenced inside another
     * selector, e.g. {@code table(class-important)}. Matches nodes that carry
     * the class in their style-class list. This is a pure constraint, never a
     * definition: it does not create nor modify {@code class-<name>}.
     */
    public static class ClassUseItem extends NTxStyleRuleSelectorItem {
        private final String name;

        public ClassUseItem(String name) {
            this.name = name == null ? "" : name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean acceptNode(NTxNode n) {
            for (String c : n.getStyleClasses()) {
                if (c != null && c.equals(name)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            ClassUseItem item = (ClassUseItem) o;
            return Objects.equals(name, item.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash("ClassUseItem", name);
        }

        @Override
        public String toString() {
            return "@" + name;
        }
    }

    /**
     * Structural selector on table rows: {@code table-row(row: n)} (nth data
     * row, 1-based over the body), {@code table-row(row: header|even|odd)} or
     * bare {@code table-row}. Matches the nearest enclosing {@code table-row}
     * (the row itself or any node rendered inside it). This is what lets cell
     * content inherit the row-level styles (e.g. {@code table-header} foreground
     * color flowing into the text of every header cell).
     */
    public static class TableRowItem extends NTxStyleRuleSelectorItem {
        private final String kind;  // header | even | odd | null
        private final Integer row;  // 1-based data (body) row, null when kind-based

        public TableRowItem(String kind, Integer row) {
            this.kind = kind == null ? null : kind.trim();
            this.row = row;
        }

        public String getKind() {
            return kind;
        }

        public Integer getRow() {
            return row;
        }

        @Override
        public boolean acceptNode(NTxNode n) {
            NTxNode ctx = firstNodeUpOfType(n, NTxNodeType.TABLE_ROW);
            if (ctx == null) {
                return false;
            }
            String section = NTxStringProp.of(ctx, NTxPropName.SECTION).orElse("");
            int sectionRow = NTxIntProp.of(ctx, NTxPropName.SECTION_ROW).orElse(-1);
            if (row != null) {
                return "body".equals(section) && sectionRow == row;
            }
            if (kind == null || kind.isEmpty()) {
                return true;
            }
            int bodyRow = NTxIntProp.of(ctx, NTxPropName.BODY_ROW).orElse(-1);
            switch (kind) {
                case "header": {
                    return "header".equals(section);
                }
                case "even": {
                    return "body".equals(section) && bodyRow > 0 && (bodyRow % 2) == 0;
                }
                case "odd": {
                    return "body".equals(section) && bodyRow > 0 && (bodyRow % 2) == 1;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TableRowItem item = (TableRowItem) o;
            return Objects.equals(kind, item.kind) && Objects.equals(row, item.row);
        }

        @Override
        public int hashCode() {
            return Objects.hash("TableRowItem", kind, row);
        }

        @Override
        public String toString() {
            if (row != null) {
                return "table-row(row: " + row + ")";
            }
            return kind == null || kind.isEmpty() ? "table-row" : "table-row(" + kind + ")";
        }
    }

    /**
     * Structural selector on table cells: {@code table-cell(row: r, col: c)}.
     * Either coordinate may be omitted (matches any row/column index).
     * {@code row:} is 1-based over the <b>body</b> (data) rows only — so
     * cell 1,1 is the first data cell and header/footer rows are never
     * counted. When any coordinate is given the selector restricts to data
     * (body) cells; header/footer cells are addressed with
     * {@code table-header(row: n, col: m)} / {@code table-footer(row: n, col: m)}
     * instead. Matches the nearest enclosing {@code table-cell} (the cell
     * itself or any node rendered inside it) so cell styles cascade into the
     * cell content.
     */
    public static class TableCellItem extends NTxStyleRuleSelectorItem {
        private final Integer row;
        private final Integer col;

        public TableCellItem(Integer row, Integer col) {
            this.row = row;
            this.col = col;
        }

        public Integer getRow() {
            return row;
        }

        public Integer getCol() {
            return col;
        }

        @Override
        public boolean acceptNode(NTxNode n) {
            NTxNode ctx = firstNodeUpOfType(n, NTxNodeType.TABLE_CELL);
            if (ctx == null) {
                return false;
            }
            if (row != null || col != null) {
                // coordinate-constrained table-cell targets the body (data) only
                if (!"body".equals(NTxStringProp.of(ctx, NTxPropName.SECTION).orElse(""))) {
                    return false;
                }
            }
            if (row != null) {
                if (row != NTxIntProp.of(ctx, NTxPropName.SECTION_ROW).orElse(-1)) {
                    return false;
                }
            }
            if (col != null) {
                if (col != NTxIntProp.of(ctx, NTxPropName.COL_INDEX).orElse(-1)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TableCellItem item = (TableCellItem) o;
            return Objects.equals(row, item.row) && Objects.equals(col, item.col);
        }

        @Override
        public int hashCode() {
            return Objects.hash("TableCellItem", row, col);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("table-cell(");
            boolean first = true;
            if (row != null) {
                sb.append("row: ").append(row);
                first = false;
            }
            if (col != null) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append("col: ").append(col);
            }
            sb.append(')');
            return sb.toString();
        }
    }

    /**
     * Structural selector on header rows/cells: {@code table-header},
     * {@code table-header(row: n)}, {@code table-header(col: m)} or
     * {@code table-header(row: n, col: m)}. {@code row:} counts header rows
     * only (1-based within the header section); {@code col:} counts columns.
     * Without a {@code col:} it matches the nearest enclosing
     * {@code table-row} (the header row itself or any node rendered inside
     * it), so row-level styles/weights flow into the header cells. With a
     * {@code col:} it matches the nearest enclosing {@code table-cell}, which
     * is how a header cell gets its own weight (e.g.
     * {@code table-header(row: 1, col: 3): { column-weight: 2 }}).
     */
    public static class TableHeaderItem extends NTxStyleRuleSelectorItem {
        private final Integer row;  // 1-based within the header section
        private final Integer col;  // 1-based column index

        public TableHeaderItem(Integer row, Integer col) {
            this.row = row;
            this.col = col;
        }

        public Integer getRow() {
            return row;
        }

        public Integer getCol() {
            return col;
        }

        @Override
        public boolean acceptNode(NTxNode n) {
            if (col != null) {
                NTxNode ctx = firstNodeUpOfType(n, NTxNodeType.TABLE_CELL);
                if (ctx == null) {
                    return false;
                }
                if (!"header".equals(NTxStringProp.of(ctx, NTxPropName.SECTION).orElse(""))) {
                    return false;
                }
                if (row != null && row != NTxIntProp.of(ctx, NTxPropName.SECTION_ROW).orElse(-1)) {
                    return false;
                }
                return col == NTxIntProp.of(ctx, NTxPropName.COL_INDEX).orElse(-1);
            }
            NTxNode ctx = firstNodeUpOfType(n, NTxNodeType.TABLE_ROW);
            if (ctx == null) {
                return false;
            }
            if (!"header".equals(NTxStringProp.of(ctx, NTxPropName.SECTION).orElse(""))) {
                return false;
            }
            return row == null || row == NTxIntProp.of(ctx, NTxPropName.SECTION_ROW).orElse(-1);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TableHeaderItem item = (TableHeaderItem) o;
            return Objects.equals(row, item.row) && Objects.equals(col, item.col);
        }

        @Override
        public int hashCode() {
            return Objects.hash("TableHeaderItem", row, col);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("table-header(");
            String inner = rowColInner(row, col);
            sb.append(inner).append(')');
            return inner.isEmpty() ? "table-header" : sb.toString();
        }
    }

    /**
     * Structural selector on footer rows/cells: {@code table-footer},
     * {@code table-footer(row: n)}, {@code table-footer(col: m)} or
     * {@code table-footer(row: n, col: m)}, mirroring {@code table-header}
     * for the footer section.
     */
    public static class TableFooterItem extends NTxStyleRuleSelectorItem {
        private final Integer row;  // 1-based within the footer section
        private final Integer col;  // 1-based column index

        public TableFooterItem(Integer row, Integer col) {
            this.row = row;
            this.col = col;
        }

        public Integer getRow() {
            return row;
        }

        public Integer getCol() {
            return col;
        }

        @Override
        public boolean acceptNode(NTxNode n) {
            if (col != null) {
                NTxNode ctx = firstNodeUpOfType(n, NTxNodeType.TABLE_CELL);
                if (ctx == null) {
                    return false;
                }
                if (!"footer".equals(NTxStringProp.of(ctx, NTxPropName.SECTION).orElse(""))) {
                    return false;
                }
                if (row != null && row != NTxIntProp.of(ctx, NTxPropName.SECTION_ROW).orElse(-1)) {
                    return false;
                }
                return col == NTxIntProp.of(ctx, NTxPropName.COL_INDEX).orElse(-1);
            }
            NTxNode ctx = firstNodeUpOfType(n, NTxNodeType.TABLE_ROW);
            if (ctx == null) {
                return false;
            }
            if (!"footer".equals(NTxStringProp.of(ctx, NTxPropName.SECTION).orElse(""))) {
                return false;
            }
            return row == null || row == NTxIntProp.of(ctx, NTxPropName.SECTION_ROW).orElse(-1);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TableFooterItem item = (TableFooterItem) o;
            return Objects.equals(row, item.row) && Objects.equals(col, item.col);
        }

        @Override
        public int hashCode() {
            return Objects.hash("TableFooterItem", row, col);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("table-footer(");
            String inner = rowColInner(row, col);
            sb.append(inner).append(')');
            return inner.isEmpty() ? "table-footer" : sb.toString();
        }
    }

    private static String rowColInner(Integer row, Integer col) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if (row != null) {
            sb.append("row: ").append(row);
            first = false;
        }
        if (col != null) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("col: ").append(col);
        }
        return sb.toString();
    }

    /**
     * Structural selector on table columns: {@code table-column(n)}, 1-based.
     * Matches nodes whose nearest enclosing {@code table-cell} has column
     * index n (the cell itself, or any node rendered inside it).
     */
    public static class TableColumnItem extends NTxStyleRuleSelectorItem {
        private final int col;

        public TableColumnItem(int col) {
            this.col = col;
        }

        public int getCol() {
            return col;
        }

        @Override
        public boolean acceptNode(NTxNode n) {
            NTxNode ctx = firstNodeUpOfType(n, NTxNodeType.TABLE_CELL);
            return ctx != null && col == NTxIntProp.of(ctx, NTxPropName.COL_INDEX).orElse(-1);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TableColumnItem item = (TableColumnItem) o;
            return col == item.col;
        }

        @Override
        public int hashCode() {
            return Objects.hash("TableColumnItem", col);
        }

        @Override
        public String toString() {
            return "table-column(" + col + ")";
        }
    }

    public static NTxStyleRuleSelectorItem of(String[] types, String[] names) {
        Set<String> stypes = types == null ? Collections.emptySet() : Arrays.stream(types).map(x -> NStringUtils.strip(x)).filter(x -> x.length() > 0).collect(Collectors.toSet());
        Set<String> snames = names == null ? Collections.emptySet() : Arrays.stream(names).map(x -> NStringUtils.strip(x)).filter(x -> x.length() > 0).collect(Collectors.toSet());
        if (stypes.isEmpty() && snames.isEmpty()) {
            return DefaultNTxNodeSelector.ANY_ITEM;
        }
        return new SimpleItem(stypes, snames);
    }

    /**
     * Returns the nearest node up the tree (starting at {@code n} itself)
     * whose type equals {@code targetType}, or {@code null} when the node is
     * outside any table structure. The search stops at a {@code table}
     * boundary, which makes nested tables shadow their ancestors: content of a
     * table living inside a cell of another table only ever sees the innermost
     * table's row/cell context.
     */
    private static NTxNode firstNodeUpOfType(NTxNode n, String targetType) {
        NTxNode cur = n;
        while (cur != null) {
            if (NTxNodeType.TABLE.equals(cur.type())) {
                return null;
            }
            if (targetType.equals(cur.type())) {
                return cur;
            }
            cur = NTxUtils.firstNodeUp(cur.parent());
        }
        return null;
    }

    public static NTxStyleRuleSelectorItem ofClassDef(String name, List<String> bases) {
        return new ClassDefItem(name, bases);
    }

    public static NTxStyleRuleSelectorItem ofClassUse(String name) {
        return new ClassUseItem(name);
    }

    public static NTxStyleRuleSelectorItem ofTableRow(String kind) {
        return new TableRowItem(kind);
    }

    public static NTxStyleRuleSelectorItem ofTableCell(Integer row, Integer col) {
        return new TableCellItem(row, col);
    }

    public static NTxStyleRuleSelectorItem ofTableWeight(Integer row, Integer col) {
        return new TableWeightItem(row, col);
    }

    public static NTxStyleRuleSelectorItem ofTableColumn(int col) {
        return new TableColumnItem(col);
    }

    public static NOptional<NTxStyleRuleSelectorItem> of(String item, NTxLogger log) {
        item = NStringUtils.strip(item);
        if (item.isEmpty() || item.equals("*") || item.equals("all")) {
            return NOptional.of(DefaultNTxNodeSelector.ANY_ITEM);
        }
        if (item.equals("!")) {
            return NOptional.of(DefaultNTxNodeSelector.NONE_ITEM);
        }

        // Legacy '.name' class selectors are rejected outright by design.
        if (item.charAt(0) == '.') {
            return rejectLegacyDotSelector(item, log);
        }

        // Special forms with parens / class- prefix
        if (isSpecialSelectorPrefix(item)) {
            return parseSpecialSelector(item, log);
        }

        Set<String> types = new HashSet<>();
        Set<String> names = new TreeSet<>();

        // We use a regex-free scan for maximum performance (Skippy style)
        int length = item.length();
        int start = 0;

        char firstChar = item.charAt(0);
        if (firstChar != ':' && firstChar != '$') {
            int end = findNextDelimiter(item, 1);
            types.add(item.substring(0, end));
            start = end;
        }
        // 2. Parse the rest (Names)
        while (start < length) {
            char prefix = item.charAt(start);
            int end = findNextDelimiter(item, start + 1);
            String value = item.substring(start + 1, end);

            if (prefix == '.') {
                return rejectLegacyDotSelector(item, log);
            }
            if (!value.isEmpty()) {
                if (prefix == ':') {
                    names.add(value);
                } else if (prefix == '$') {
                    types.add(value);
                }
            }
            start = end;
        }
        if (types.isEmpty() && names.isEmpty()) {
            return NOptional.of(DefaultNTxNodeSelector.ANY_ITEM);
        }
        return NOptional.of(new SimpleItem(types, names));
    }

    private static NOptional<NTxStyleRuleSelectorItem> rejectLegacyDotSelector(String item, NTxLogger log) {
        if (log != null) {
            int dot = item.indexOf('.');
            String name = dot >= 0 ? item.substring(dot + 1) : item;
            log.log(NMsg.ofC("invalid style rule selector '%s': legacy class selector '.%s' is no longer supported; declare class-%s in a styles{} block and use @(%s) at the usage site", item, name, name, name));
        }
        return NOptional.ofEmpty(NMsg.ofC("invalid style rule selector '%s'", item));
    }

    private static boolean isSpecialSelectorPrefix(String item) {
        return item.startsWith("class-")
                || item.equals("table-header")
                || item.startsWith("table-row")
                || item.startsWith("table-column")
                || item.startsWith("table-cell")
                || item.startsWith("table-weight");
    }

    private static void _warn(NTxLogger log, String msg, Object... args) {
        if (log != null) {
            log.log(NMsg.ofC(msg, args));
        }
    }

    private static NOptional<NTxStyleRuleSelectorItem> parseSpecialSelector(String item, NTxLogger log) {
        if (item.startsWith("class-")) {
            // At rule top (or in a text context) a bare class-* is a definition.
            String rest = item.substring("class-".length());
            String name;
            List<String> bases = Collections.emptyList();
            int open = rest.indexOf('(');
            if (open >= 0) {
                if (!rest.endsWith(")")) {
                    return _err(log, "invalid class selector '%s'", item);
                }
                name = rest.substring(0, open);
                String inner = rest.substring(open + 1, rest.length() - 1);
                bases = new ArrayList<>();
                for (String b : inner.split(",")) {
                    String bb = NStringUtils.strip(b);
                    if (!bb.isEmpty()) {
                        bases.add(bb);
                    }
                }
            } else {
                name = rest;
            }
            if (name.isEmpty()) {
                return _err(log, "invalid class selector '%s'", item);
            }
            return NOptional.of(ofClassDef(name, bases));
        }
        if (item.equals("table-header")) {
            return NOptional.of(ofTableRow("header"));
        }
        if (item.startsWith("table-row")) {
            String rest = item.substring("table-row".length());
            if (rest.isEmpty()) {
                return NOptional.of(ofTableRow(null));
            }
            if (rest.startsWith("(") && rest.endsWith(")")) {
                String inner = NStringUtils.strip(rest.substring(1, rest.length() - 1));
                int ci = inner.indexOf(':');
                if (ci > 0 && !inner.contains(",")) {
                    String k = NStringUtils.strip(inner.substring(0, ci));
                    String v = NStringUtils.strip(inner.substring(ci + 1));
                    if (k.equalsIgnoreCase("row") || k.equalsIgnoreCase("r")) {
                        if (v.isEmpty() || v.matches("header|even|odd")) {
                            return NOptional.of(ofTableRow(v.isEmpty() ? null : v));
                        }
                    }
                    return _err(log, "invalid table-row selector '%s', expected table-row(row: header|even|odd) or table-header", item);
                }
                // legacy positional form, kept as a best-effort migration
                _warn(log, "table-row(%s): positional form is deprecated; use table-row(row: %s)%s",
                        inner, inner.isEmpty() ? "header" : inner, inner.equals("header") ? " or table-header" : "");
                if (inner.isEmpty() || inner.matches("header|even|odd")) {
                    return NOptional.of(ofTableRow(inner.isEmpty() ? null : inner));
                }
            }
            return _err(log, "invalid table-row selector '%s', expected header, even or odd", item);
        }
        if (item.startsWith("table-column")) {
            String rest = item.substring("table-column".length());
            if (rest.startsWith("(") && rest.endsWith(")")) {
                String inner = NStringUtils.strip(rest.substring(1, rest.length() - 1));
                int ci = inner.indexOf(':');
                if (ci > 0 && !inner.contains(",")) {
                    String k = NStringUtils.strip(inner.substring(0, ci));
                    String v = NStringUtils.strip(inner.substring(ci + 1));
                    if (k.equalsIgnoreCase("col") || k.equalsIgnoreCase("c")) {
                        try {
                            return NOptional.of(ofTableColumn(Integer.parseInt(v)));
                        } catch (NumberFormatException e) {
                            // fallthrough
                        }
                    }
                    return _err(log, "invalid table-column selector '%s', expected table-column(col: n)", item);
                }
                // legacy positional form, kept as a best-effort migration
                _warn(log, "table-column(%s): positional form is deprecated; use table-column(col: %s)", inner, inner);
                try {
                    return NOptional.of(ofTableColumn(Integer.parseInt(inner)));
                } catch (NumberFormatException e) {
                    // fallthrough
                }
            }
            return _err(log, "invalid table-column selector '%s', expected a 1-based column index", item);
        }
        if (item.startsWith("table-cell")) {
            String rest = item.substring("table-cell".length());
            if (rest.isEmpty()) {
                return NOptional.of(ofTableCell(null, null));
            }
            if (rest.startsWith("(") && rest.endsWith(")")) {
                String inner = rest.substring(1, rest.length() - 1);
                Integer row = null;
                Integer col = null;
                for (String kv : inner.split(",")) {
                    int ci = kv.indexOf(':');
                    if (ci < 0) {
                        return _err(log, "invalid table-cell selector '%s'", item);
                    }
                    String k = NStringUtils.strip(kv.substring(0, ci));
                    String v = NStringUtils.strip(kv.substring(ci + 1));
                    int iv;
                    try {
                        iv = Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        return _err(log, "invalid table-cell selector '%s'", item);
                    }
                    if (k.equalsIgnoreCase("row") || k.equalsIgnoreCase("r")) {
                        row = iv;
                    } else if (k.equalsIgnoreCase("col") || k.equalsIgnoreCase("c")) {
                        col = iv;
                    } else {
                        return _err(log, "invalid table-cell selector '%s'", item);
                    }
                }
                return NOptional.of(ofTableCell(row, col));
            }
            return _err(log, "invalid table-cell selector '%s'", item);
        }
        if (item.startsWith("table-weight")) {
            String rest = item.substring("table-weight".length());
            if (rest.isEmpty()) {
                return NOptional.of(ofTableWeight(null, null));
            }
            if (rest.startsWith("(") && rest.endsWith(")")) {
                String inner = rest.substring(1, rest.length() - 1);
                Integer row = null;
                Integer col = null;
                for (String kv : inner.split(",")) {
                    int ci = kv.indexOf(':');
                    if (ci < 0) {
                        return _err(log, "invalid table-weight selector '%s'", item);
                    }
                    String k = NStringUtils.strip(kv.substring(0, ci));
                    String v = NStringUtils.strip(kv.substring(ci + 1));
                    int iv;
                    try {
                        iv = Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        return _err(log, "invalid table-weight selector '%s'", item);
                    }
                    if (k.equalsIgnoreCase("row") || k.equalsIgnoreCase("r")) {
                        row = iv;
                    } else if (k.equalsIgnoreCase("col") || k.equalsIgnoreCase("c")) {
                        col = iv;
                    } else {
                        return _err(log, "invalid table-weight selector '%s'", item);
                    }
                }
                return NOptional.of(ofTableWeight(row, col));
            }
            return _err(log, "invalid table-weight selector '%s'", item);
        }
        return _err(log, "invalid special selector '%s'", item);
    }

    private static NOptional<NTxStyleRuleSelectorItem> _err(NTxLogger log, String msg, String item) {
        if (log != null) {
            log.log(NMsg.ofC(msg, item));
        }
        return NOptional.ofEmpty(NMsg.ofC(msg, item));
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

    public static final class NTxStringProp {
        public static NOptional<String> of(NTxNode n, String prop) {
            return n.getPropertyValue(prop).map(x -> x.asStringValue().orNull());
        }
    }

    public static final class NTxIntProp {
        public static NOptional<Integer> of(NTxNode n, String prop) {
            return n.getPropertyValue(prop).map(x -> asIntSafe(x));
        }

        private static Integer asIntSafe(NElement x) {
            try {
                return x.asIntValue().orNull();
            } catch (Throwable t) {
                return null;
            }
        }
    }
}