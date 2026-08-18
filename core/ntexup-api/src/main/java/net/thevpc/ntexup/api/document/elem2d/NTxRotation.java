package net.thevpc.ntexup.api.document.elem2d;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NToElement;

public class NTxRotation implements NToElement {
    private NElement angle;
    private NElement x;
    private NElement y;

    public NTxRotation(NElement angle, NElement x, NElement y) {
        this.angle = angle;
        this.x = x;
        this.y = y;
    }

    public NElement getAngle() {
        return angle;
    }

    public NElement getX() {
        return x;
    }

    public NElement getY() {
        return y;
    }

    @Override
    public String toString() {
        return "(" + angle
                + ", " + x
                + ", " + y
                + ')';
    }

    @Override
    public NElement toElement() {
        return NElement.ofTuple(
                angle,
                x,
                y
        );
    }

}
