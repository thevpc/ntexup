package net.thevpc.ntexup.engine.util;

import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxItemList;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.parser.ctrlnodes.CtrlNTxNodeName;

import java.util.ArrayList;
import java.util.List;

public class NTxNodeUtils {
    public static boolean isComponentBody(NTxNode child) {
        return (child instanceof CtrlNTxNodeName && NTxUtils.isComponentBody(((CtrlNTxNodeName)child).getVarName().asStringValue().get()));
    }
    public static NTxItem ofNTxItem(List<? extends NTxItem> any) {
        if (any == null) {
            return null;
        }
        if (any.size() == 0) {
            return new NTxItemList();
        }
        if (any.size() == 1) {
            return any.get(0);
        }
        return new NTxItemList().addAll(any);
    }

    public static List<NTxNode> toNodes(NTxItem item) {
        List<NTxNode> a = new ArrayList<>();
        if (item instanceof NTxItemList) {
            for (NTxItem nTxItem : ((NTxItemList) item).getItems()) {
                a.addAll(toNodes(nTxItem));
            }
        } else if (item instanceof NTxNode) {
            a.add((NTxNode) item);
        }
        return a;
    }
}
