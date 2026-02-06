package net.thevpc.ntexup.api.document.node;

import net.thevpc.ntexup.api.source.NTxSource;

import java.util.ArrayList;
import java.util.List;

public class NTxItemList implements NTxItem {
    private List<NTxItem> items = new ArrayList<>();

    @Override
    public NTxItem parent() {
        return null;
    }

    @Override
    public NTxSource source() {
        return null;
    }

    public NTxItemList() {
        this.items = new ArrayList<>();
    }

    public NTxItemList add(NTxItem a) {
        if (a != null) {
            if (a instanceof NTxItemList) {
                for (NTxItem b : ((NTxItemList) a).getItems()) {
                    add(b);
                }
            } else {
                this.items.add(a);
            }
        }
        return this;
    }

    public NTxItemList addAll(List<? extends NTxItem> all) {
        if (all != null) {
            for (NTxItem i : all) {
                add(i);
            }
        }
        return this;
    }

    public List<NTxItem> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return "HItemList" + items;
    }

//    @Override
//    public NElement toElement() {
//        return NElements.obj(
//                items.stream().map(x -> x.toElement()).toArray(NElement[]::new)
//        ).build();
//    }
}
