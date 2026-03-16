package net.thevpc.ntexup.engine.eval;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.eval.NTxNodePath;

import java.util.ArrayList;
import java.util.List;

public class NTxNodePathImpl implements NTxNodePath {
    List<NTxNode> list = new ArrayList<>();

    public NTxNodePathImpl(List<NTxNode> list) {
        for (NTxNode a : list) {
            if(a!=null) {
                if(this.list.isEmpty() || this.list.get(this.list.size()-1)!=a) {
                    this.list.add(a);
                }
            }
        }
    }

    @Override
    public boolean isRoot() {
        return size()==0;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public NTxNodePath parent() {
        if(list.isEmpty()){
            return null;
        }
        return new NTxNodePathImpl(list.subList(0,list.size()-1));
    }

    @Override
    public NTxNode node() {
        return this.list.isEmpty()?null:this.list.get(this.list.size()-1);
    }

    @Override
    public NTxNodePath resolve(NTxNode other) {
        ArrayList<NTxNode> a = new ArrayList<>(list);
        a.add(other);
        return new  NTxNodePathImpl(a);
    }
}
