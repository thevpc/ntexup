package net.thevpc.ntexup.api.document.style;

import net.thevpc.ntexup.api.document.node.NTxItem;
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
        if (items == null || items.length==0 || Arrays.stream(items).anyMatch(x->Objects.equals(x,ANY_ITEM))) {
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
            return ANY;
        }
        return new DefaultNTxNodeSelector(items2);
    }


    private DefaultNTxNodeSelector(Set<NTxStyleRuleSelectorItem> items) {
        this.items.addAll(items);
    }

    private Set<String> computeClasses(NTxItem n) {
        Set<String> all = new HashSet<>();
        while (n != null) {
            if (n instanceof NTxNode) {
                all.addAll(((NTxNode) n).styleClasses());
            }
            n = n.parent();
        }
        return all;
    }

    public Set<String> getClasses() {
        Set<String> c = new HashSet<>();
        for (NTxStyleRuleSelectorItem item : items) {
            if (item instanceof NTxStyleRuleSelectorItem.SimpleItem) {
                c.addAll(((NTxStyleRuleSelectorItem.SimpleItem) item).getClasses());
            }
        }
        return Collections.unmodifiableSet(c);
    }

    @Override
    public boolean acceptNode(NTxNode n) {
        for (NTxStyleRuleSelectorItem item : items) {
            if (item.acceptNode(n)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "(" + items.stream().map(x -> x.toString()).collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public NElement toElement() {
        return NElement.ofUplet(
                items.stream().map(x -> NElement.ofNameOrString(x.toString())).toArray(NElement[]::new)
        );
    }

    @Override
    public int compareTo(NTxStyleRuleSelector o) {
        if (o == null) return -1;
        if (!(o instanceof DefaultNTxNodeSelector)) return 1;

        DefaultNTxNodeSelector op = (DefaultNTxNodeSelector) o;

        // 1. Find the "Best" (Most Specific) Item in each collection
        NTxStyleRuleSelectorItem bestThis = findBestItem(this.items);
        NTxStyleRuleSelectorItem bestOther = findBestItem(op.items);

        // 2. Compare the two best items
        return compareSpecificItems(bestThis, bestOther);
    }

    private NTxStyleRuleSelectorItem findBestItem(Set<NTxStyleRuleSelectorItem> items) {
        return items.stream()
                .min(this::compareSpecificItems) // Minimum result = Higher specificity
                .orElse(ANY_ITEM);
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



    private int compareSpecificItems(NTxStyleRuleSelectorItem a, NTxStyleRuleSelectorItem b) {
        if (a.equals(b)) return 0;

        // Tier 1: SimpleItem (The actual logic-heavy selectors)
        if (a instanceof NTxStyleRuleSelectorItem.SimpleItem && b instanceof NTxStyleRuleSelectorItem.SimpleItem) {
            NTxStyleRuleSelectorItem.SimpleItem sa = (NTxStyleRuleSelectorItem.SimpleItem) a;
            NTxStyleRuleSelectorItem.SimpleItem sb = (NTxStyleRuleSelectorItem.SimpleItem) b;

            // Specificity: Names > Types > Classes
            int c = Integer.compare(sb.getNames().size(), sa.getNames().size());
            if (c != 0) return c;

            c = Integer.compare(sb.getTypes().size(), sa.getTypes().size());
            if (c != 0) return c;

            c = Integer.compare(sb.getClasses().size(), sa.getClasses().size());
            if (c != 0) return c;

            return sa.toString().compareTo(sb.toString());
        }

        // Tier 2: AnyItem (*) - Matches everything, so it has low specificity
        if (a instanceof NTxStyleRuleSelectorItem.AnyItem) {
            // AnyItem is more specific than NoneItem, but less than SimpleItem
            return (b instanceof NTxStyleRuleSelectorItem.NoneItem) ? -1 : 1;
        }
        if (b instanceof NTxStyleRuleSelectorItem.AnyItem) {
            return (a instanceof NTxStyleRuleSelectorItem.NoneItem) ? 1 : -1;
        }

        // Tier 3: NoneItem (!) - The "Void".
        // It never matches, so it has the lowest possible "functional" priority.
        if (a instanceof NTxStyleRuleSelectorItem.NoneItem) return 1;
        if (b instanceof NTxStyleRuleSelectorItem.NoneItem) return -1;

        return 0;
    }
}
