package net.thevpc.ntexup.api.document.elem2d;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NToElement;

public class NTxInt2 implements NToElement {
    private Integer x;
    private Integer y;

    public NTxInt2(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    public Integer getX() {
        return x;
    }

    public Integer getY() {
        return y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ')';
    }

    @Override
    public NElement toElement() {
        return NElement.ofTuple(
                NElement.ofInt(getX()),
                NElement.ofInt(getY())
        );
    }

}
