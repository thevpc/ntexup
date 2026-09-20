package net.thevpc.ntexup.api.document.style;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.nuts.elem.NElement;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultNTxNodeSelector implements NTxStyleRuleSelector {

    public static NTxStyleRuleSelectorItem NONE_ITEM = new NTxStyleRuleSelectorItem.NoneItem();
    public static NTxStyleRuleSelectorItem ANY_ITEM = new NTxStyleRuleSelectorItem.AnyItem();
    private static DefaultNTxNodeSelector ANY = new DefaultNTxNodeSelector(new HashSet<>(Arrays.asList(ANY_ITEM)));
    private static DefaultNTxNodeSelector NONE = new DefaultNTxNodeSelector(new HashSet<>(Arrays.asList(NONE_ITEM)));
    private final Set<NTxStyleRuleSelectorItem> items = new HashSet<>();

    public static DefaultNTxNodeSelector ofAny() {
        return ANY;
    }

    public static DefaultNTxNodeSelector of(NTxStyleRuleSelectorItem... items) {
        if (items == null || items.length==0) {
            // An empty selector must never match everything; it is an inert
            // (dead) selector, otherwise a leftover unresolvable item such as a
            // legacy '.name' would silently style every node in the document.
            return NONE;
        }
        if (Arrays.stream(items).anyMatch(x->Objects.equals(x,ANY_ITEM))) {
            return ANY;
        }
        boolean none=false;
        HashSet<NTxStyleRuleSelectorItem> items2 = new HashSet<>();
        for (NTxStyleRuleSelectorItem item : items) {
            if(item!=null){
                if(item.equals(ANY_ITEM)) {
                    return ANY;
                }else if(item.equals(NONE_ITEM)){
                    none=true;
                }else{
                    items2.add(item);
                }
            }
        }
        if(none){
            return NONE;
        }
        if (items2.isEmpty()) {
            // Only null/unresolvable items remained: treat the selector as dead
            // (NONE), never as everything-matching.
            return NONE;
        }
        return new DefaultNTxNodeSelector(items2);
    }


    private DefaultNTxNodeSelector(Set<NTxStyleRuleSelectorItem> items) {
        this.items.addAll(items);
    }

    /**
     * Returns the names of any class definitions ({@code class-<name>}) held in
     * this selector.
     */
    public Set<String> getClassDefNames() {
        Set<String> all = new HashSet<>();
        for (NTxStyleRuleSelectorItem item : items) {
            if (item instanceof NTxStyleRuleSelectorItem.ClassDefItem) {
                all.add(((NTxStyleRuleSelectorItem.ClassDefItem) item).getName());
            }
        }
        return all;
    }

    /**
     * Returns the class definition item with the given name if present.
     */
    public NTxStyleRuleSelectorItem.ClassDefItem getClassDef(String name) {
        for (NTxStyleRuleSelectorItem item : items) {
            if (item instanceof NTxStyleRuleSelectorItem.ClassDefItem) {
                NTxStyleRuleSelectorItem.ClassDefItem cd = (NTxStyleRuleSelectorItem.ClassDefItem) item;
                if (Objects.equals(name, cd.getName())) {
                    return cd;
                }
            }
        }
        return null;
    }

    @Override
    public boolean acceptNode(NTxNode n) {
        // Conjunction: every item must accept the node. A selector such as
        // table(class-important) therefore means "a table node that also
        // carries the class important".
        for (NTxStyleRuleSelectorItem item : items) {
            if (!item.acceptNode(n)) {
                return false;
            }
        }
        return !items.isEmpty();
    }

    @Override
    public String toString() {
        return "(" + items.stream().map(x -> x.toString()).collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public NElement toElement() {
        return NElement.ofTuple(
                items.stream().map(x -> NElement.ofNameOrString(x.toString())).toArray(NElement[]::new)
        );
    }

    @Override
    public int compareTo(NTxStyleRuleSelector o) {
        if (o == null) return -1;
        if (!(o instanceof DefaultNTxNodeSelector)) return 1;

        DefaultNTxNodeSelector op = (DefaultNTxNodeSelector) o;

        // 1. More constraints (explicit AND terms) = more specific.
        int cThis = constraintCount();
        int cOther = op.constraintCount();
        if (cThis != cOther) {
            // smaller result = higher specificity
            return Integer.compare(cOther, cThis);
        }

        // 2. Same count: compare rank signatures from strongest to weakest term.
        List<Integer> thisRanks = sortedRanks(this.items);
        List<Integer> otherRanks = sortedRanks(op.items);
        int n = Math.min(thisRanks.size(), otherRanks.size());
        for (int i = 0; i < n; i++) {
            int a = thisRanks.get(i);
            int b = otherRanks.get(i);
            if (a != b) {
                return Integer.compare(b, a);
            }
        }
        if (thisRanks.size() != otherRanks.size()) {
            return Integer.compare(otherRanks.size(), thisRanks.size());
        }

        // 3. Fallback: stable lexical order.
        return this.toString().compareTo(o.toString());
    }

    private int constraintCount() {
        int c = 0;
        for (NTxStyleRuleSelectorItem item : items) {
            if (!(item instanceof NTxStyleRuleSelectorItem.AnyItem)
                    && !(item instanceof NTxStyleRuleSelectorItem.NoneItem)) {
                c++;
            }
        }
        return c;
    }

    private static List<Integer> sortedRanks(Set<NTxStyleRuleSelectorItem> items) {
        List<Integer> ranks = new ArrayList<>();
        for (NTxStyleRuleSelectorItem item : items) {
            ranks.add(specificityRank(item));
        }
        ranks.sort(Collections.reverseOrder());
        return ranks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultNTxNodeSelector that = (DefaultNTxNodeSelector) o;
        return Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }



    private static int specificityRank(NTxStyleRuleSelectorItem it) {
        if (it instanceof NTxStyleRuleSelectorItem.AnyItem) {
            return 1;
        }
        if (it instanceof NTxStyleRuleSelectorItem.NoneItem) {
            return 0;
        }
        if (it instanceof NTxStyleRuleSelectorItem.ClassUseItem) {
            return 8;
        }
        if (it instanceof NTxStyleRuleSelectorItem.ClassDefItem) {
            return 0;
        }
        if (it instanceof NTxStyleRuleSelectorItem.TableRowItem) {
            NTxStyleRuleSelectorItem.TableRowItem t = (NTxStyleRuleSelectorItem.TableRowItem) it;
            return (t.getKind() == null || t.getKind().isEmpty()) ? 2 : 3;
        }
        if (it instanceof NTxStyleRuleSelectorItem.TableColumnItem) {
            return 4;
        }
        if (it instanceof NTxStyleRuleSelectorItem.TableCellItem) {
            NTxStyleRuleSelectorItem.TableCellItem t = (NTxStyleRuleSelectorItem.TableCellItem) it;
            if (t.getRow() != null && t.getCol() != null) {
                return 6;
            }
            if (t.getRow() != null || t.getCol() != null) {
                return 5;
            }
            return 4;
        }
        if (it instanceof NTxStyleRuleSelectorItem.SimpleItem) {
            return 7;
        }
        return 0;
    }

    private int compareSpecificItems(NTxStyleRuleSelectorItem a, NTxStyleRuleSelectorItem b) {
        if (a.equals(b)) return 0;

        int ra = specificityRank(a);
        int rb = specificityRank(b);
        if (ra != rb) {
            // smaller result = higher specificity
            return Integer.compare(rb, ra);
        }

        // Tier 1: SimpleItem (The actual logic-heavy selectors)
        if (a instanceof NTxStyleRuleSelectorItem.SimpleItem && b instanceof NTxStyleRuleSelectorItem.SimpleItem) {
            NTxStyleRuleSelectorItem.SimpleItem sa = (NTxStyleRuleSelectorItem.SimpleItem) a;
            NTxStyleRuleSelectorItem.SimpleItem sb = (NTxStyleRuleSelectorItem.SimpleItem) b;

            // Specificity: Names > Types
            int c = Integer.compare(sb.getNames().size(), sa.getNames().size());
            if (c != 0) return c;

            c = Integer.compare(sb.getTypes().size(), sa.getTypes().size());
            if (c != 0) return c;
        }

        return a.toString().compareTo(b.toString());
    }
}
