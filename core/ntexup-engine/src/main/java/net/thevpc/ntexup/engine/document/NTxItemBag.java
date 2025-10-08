package net.thevpc.ntexup.engine.document;

import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.nuts.util.NIllegalArgumentException;
import net.thevpc.nuts.text.NMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NTxItemBag {
    private List<NTxNode> nodes = new ArrayList<>();
    private List<NTxProp> props = new ArrayList<>();
    private List<NTxNodeDef> defs = new ArrayList<>();
    private List<NTxStyleRule> styleRules = new ArrayList<>();
    private List<NTxItem> list = new ArrayList<>();

    public NTxItemBag(List<NTxItem> nodes) {
        if (nodes != null) {
            for (NTxItem node : nodes) {
                add(node);
            }
        }
    }

    public List<NTxNode> nodes() {
        return nodes;
    }

    public List<NTxProp> getProps() {
        return props;
    }

    public List<NTxNodeDef> getDefs() {
        return defs;
    }

    public List<NTxStyleRule> getStyleRules() {
        return styleRules;
    }

    private void add(NTxItem a) {
        if (a != null) {
            if (a instanceof NTxItemList) {
                for (NTxItem item : ((NTxItemList) a).getItems()) {
                    add(item);
                }
            }
            if (a instanceof NTxNode) {
                nodes.add((NTxNode) a);
                list.add(a);
                return;
            }
            if (a instanceof NTxProp) {
                props.add((NTxProp) a);
                list.add(a);
                return;
            }
            if (a instanceof NTxNodeDef) {
                defs.add((NTxNodeDef) a);
                list.add(a);
                return;
            }
            if (a instanceof NTxStyleRule) {
                styleRules.add((NTxStyleRule) a);
                list.add(a);
                return;
            }
            throw new NIllegalArgumentException(NMsg.ofC("unexpected item type " + a.getClass()));
        }
    }

    public List<NTxNode> compressToNodes(boolean inPage, NTxSource source) {
        if(isNodes()){
            return new ArrayList<>(nodes());
        }
        return new ArrayList<>(Arrays.asList(compress(inPage,source)));
    }

    public NTxNode compress(boolean inPage, NTxSource source) {
        if (isEmpty()) {
            return new DefaultNTxNode(NTxNodeType.VOID, source);
        } else if (list.size() == 1 && list.get(0) instanceof NTxNode) {
            return (NTxNode) list.get(0);
        } else {
            if (inPage) {
                DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.GROUP, source);
                for (NTxItem nTxItem : list) {
                    root.mergeNode(nTxItem);
                }
                return root;
            } else {
                DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.PAGE_GROUP, source);
                for (NTxItem nTxItem : list) {
                    root.mergeNode(nTxItem);
                }
                return root;
            }
        }
    }

    public List<NTxItem> all() {
        return list;
    }
    public boolean isEmpty() {
        if (!nodes.isEmpty()) {
            return false;
        }
        if (!props.isEmpty()) {
            return false;
        }
        if (!defs.isEmpty()) {
            return false;
        }
        if (!styleRules.isEmpty()) {
            return false;
        }
        return true;
    }

    public boolean isNodes() {
        if (!props.isEmpty()) {
            return false;
        }
        if (!defs.isEmpty()) {
            return false;
        }
        if (!styleRules.isEmpty()) {
            return false;
        }
        return true;
    }
}
