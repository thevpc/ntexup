package net.thevpc.ntexup.api.document.elem2d;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NToElement;

public class NTxDouble4 implements NToElement {
    private Double x1;
    private Double x2;
    private Double x3;
    private Double x4;

    public NTxDouble4(Number x1, Number x2, Number x3, Number x4) {
        this.x1 = x1 == null ? null : x1.doubleValue();
        this.x2 = x2 == null ? null : x2.doubleValue();
        this.x3 = x3 == null ? null : x3.doubleValue();
        this.x4 = x4 == null ? null : x4.doubleValue();
    }

    public Double getX1() {
        return x1;
    }

    public Double getX2() {
        return x2;
    }

    public Double getX3() {
        return x3;
    }

    public Double getX4() {
        return x4;
    }

    @Override
    public String toString() {
        return "(" + x1
                + ", " + x2
                + ", " + x3
                + ", " + x4
                + ')';
    }

    @Override
    public NElement toElement() {
        return NElement.ofTuple(
                NElement.ofDouble(getX1()),
                NElement.ofDouble(getX2()),
                NElement.ofDouble(getX3()),
                NElement.ofDouble(getX4())
        );
    }

}
