package net.thevpc.ntexup.lib.geometry2d;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem2d.NTxVector2D;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NToElement;

import java.util.Objects;

public class NTxSize2D implements NToElement {
    public final double x, y;

    public NTxSize2D(double x, double y) {
        this.x = Math.max(x, 0);
        this.y = Math.max(y, 0);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public NTxVector2D asVector() {
        return new NTxVector2D(x, y);
    }

    public NTxPoint2D asPoint() {
        return new NTxPoint2D(x, y);
    }

    public NTxSize2D minus(NTxSize2D b) {
        return new NTxSize2D(
                x - b.x,
                y - b.y
        );
    }


    public double length() {
        return Math.sqrt(x * x +
                y * y);
    }

    public NTxSize2D plus(NTxSize2D b) {
        return new NTxSize2D(
                x + b.x,
                y + b.y
        );
    }

    public NTxSize2D plus(double b) {
        return new NTxSize2D(
                x + b,
                y + b
        );
    }

    public NTxSize2D minus(double b) {
        return new NTxSize2D(
                x - b,
                y - b
        );
    }

    public NTxSize2D mul(double b) {
        return new NTxSize2D(
                x * b,
                y * b
        );
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NTxSize2D point2D = (NTxSize2D) o;
        return Double.compare(x, point2D.x) == 0 && Double.compare(y, point2D.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "NTxSize2D{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    @Override
    public NElement toElement() {
        return NElement.ofTuple(
                NElement.ofDouble(getX()),
                NElement.ofDouble(getY())
        );
    }

    public NTxSize2D add(double x, double y) {
        return new NTxSize2D(this.x + x, this.y + y);
    }
}
