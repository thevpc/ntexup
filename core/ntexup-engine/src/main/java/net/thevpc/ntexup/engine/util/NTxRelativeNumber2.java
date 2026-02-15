package net.thevpc.ntexup.engine.util;

import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;

public class NTxRelativeNumber2 {
    private NTxRelativeNumber x;
    private NTxRelativeNumber y;

    public NTxRelativeNumber2 of(NTxRelativeNumber x, NTxRelativeNumber y) {
        if(x!=null && y!=null){
            return new NTxRelativeNumber2(x,y);
        }
        if(x==null && y==null){
            return new NTxRelativeNumber2(NTxRelativeNumber.ofParent(100), NTxRelativeNumber.ofParent(100));
        }
        if(x==null){
            return new NTxRelativeNumber2(y,y);
        }
        return new NTxRelativeNumber2(x,x);
    }

    public NTxRelativeNumber2(NTxRelativeNumber x, NTxRelativeNumber y) {
        this.x=x;
        this.y=y;
    }

    public NTxDouble2 compute(NTxDouble2 parentSize, NTxDouble2 pageSize) {
        return new NTxDouble2(
                x.compute(parentSize.getX(),pageSize.getX()),
                y.compute(parentSize.getY(),pageSize.getY())
        );
    }
}
