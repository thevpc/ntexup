package net.thevpc.ntexup.api.document.elem2d;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NToElement;

import java.util.Objects;

public class NTxPoint2D implements NToElement {
    public double x, y;

    public NTxPoint2D(double x, double y) {
        this.x = x;
        this.y = y;
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

    public NTxPoint2D minus(NTxPoint2D b) {
        return new NTxPoint2D(
                x - b.x,
                y - b.y
        );
    }


    public double length() {
        return Math.sqrt(dot(this));
    }

    public NTxPoint2D plus(NTxPoint2D b) {
        return new NTxPoint2D(
                x + b.x,
                y + b.y
        );
    }

    public NTxPoint2D plus(double b) {
        return new NTxPoint2D(
                x + b,
                y + b
        );
    }

    public NTxPoint2D minus(double b) {
        return new NTxPoint2D(
                x - b,
                y - b
        );
    }

    public NTxPoint2D mul(double b) {
        return new NTxPoint2D(
                x * b,
                y * b
        );
    }

    public double dot(NTxPoint2D b) {
        return (
                x * b.x +
                        y * b.y
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NTxPoint2D point2D = (NTxPoint2D) o;
        return Double.compare(x, point2D.x) == 0 && Double.compare(y, point2D.y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Point2D{" +
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

    public NTxPoint2D add(double x, double y) {
        return new NTxPoint2D(x+x, y+y);
    }
}
