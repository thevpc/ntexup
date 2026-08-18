package net.thevpc.ntexup.api.document.elem2d;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NToElement;

public class NTxDouble2 implements NToElement {
    private Double x;
    private Double y;

    public NTxDouble2(Number x, Number y) {
        this.x = x == null ? null : x.doubleValue();
        this.y = y == null ? null : y.doubleValue();
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ')';
    }

    public NTxDouble2 mul(double w, double h) {
        return new NTxDouble2(
                x * w,
                y * h
        );
    }

    @Override
    public NElement toElement() {
        return NElement.ofTuple(
                NElement.ofDouble(getX()),
                NElement.ofDouble(getY())
        );
    }
}
