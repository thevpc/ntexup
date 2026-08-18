package net.thevpc.ntexup.lib.geometry3d;

import net.thevpc.ntexup.api.util.NTxNumberUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NNumberElement;
import net.thevpc.nuts.elem.NToElement;

import java.util.Objects;

public class NTxNumberElement2 implements NToElement {
    public NNumberElement x, y;

    public static NTxNumberElement2 denull(NTxNumberElement2 other) {
        if (other == null) {
            return NTxNumberElement2.ofZero();
        }
        return other;
    }

    public static NTxNumberElement2 ofZero() {
        return new NTxNumberElement2(NTxNumberUtils.ofNumber(0.0), NTxNumberUtils.ofNumber(0.0));
    }

    public static NTxNumberElement2 ofHundred() {
        return new NTxNumberElement2(NTxNumberUtils.ofNumber(100.0), NTxNumberUtils.ofNumber(100.0));
    }

    public static NTxNumberElement2 ofOne() {
        return new NTxNumberElement2(NTxNumberUtils.ofNumber(1.0), NTxNumberUtils.ofNumber(1.0));
    }

    public NTxNumberElement2(NNumberElement x, NNumberElement y) {
        this.x = NTxNumberUtils.ofNumber(x);
        this.y = NTxNumberUtils.ofNumber(y);
    }

    public NNumberElement getX() {
        return x;
    }

    public NNumberElement getY() {
        return y;
    }

//    public double length() {
//        return Math.sqrt(dot(this));
//    }
//
//    public NTxElementNumber3 plus(NTxElementNumber3 b) {
//        return new NTxElementNumber3(
//                x + b.x,
//                y + b.y,
//                z + b.z
//        );
//    }
//
//    public NTxElementNumber3 plus(double b) {
//        return new NTxElementNumber3(
//                x + b,
//                y + b,
//                z + b
//        );
//    }
//
//    public NTxElementNumber3 minus(double b) {
//        return new NTxElementNumber3(
//                x - b,
//                y - b,
//                z - b
//        );
//    }
//
//    public NTxElementNumber3 mul(double b) {
//        return new NTxElementNumber3(
//                x * b,
//                y * b,
//                z * b
//        );
//    }
//
//    public double dot(NTxElementNumber3 b) {
//        return (
//                x * b.x +
//                        y * b.y +
//                        z * b.z
//        );
//    }
//
//    // Apply a transformation matrix to the point
//    public NTxElementNumber3 transform(double[][] matrix) {
//        double newX = matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] * z + matrix[0][3];
//        double newY = matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] * z + matrix[1][3];
//        double newZ = matrix[2][0] * x + matrix[2][1] * y + matrix[2][2] * z + matrix[2][3];
//        return new NTxElementNumber3(newX, newY, newZ);
//    }
//
//    // Apply a transformation matrix to the point
//    public NTxElementNumber3 transform(NTxMatrix3D transform) {
//        double[][] m = transform.m;
//        double newX = m[0][0] * x + m[0][1] * y + m[0][2] * z + m[0][3];
//        double newY = m[1][0] * x + m[1][1] * y + m[1][2] * z + m[1][3];
//        double newZ = m[2][0] * x + m[2][1] * y + m[2][2] * z + m[2][3];
//        return new NTxElementNumber3(newX, newY, newZ);
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NTxNumberElement2 point3D = (NTxNumberElement2) o;
        return Objects.equals(x, point3D.x) && Objects.equals(y, point3D.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Point3D{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    @Override
    public NElement toElement() {
        return NElement.ofTuple(
                getX(),
                getY()
        );
    }

//    public double distance(NTxElementNumber3 other) {
//        double dx = this.x - other.x;
//        double dy = this.y - other.y;
//        double dz = this.z - other.z;
//        return Math.sqrt(dx * dx + dy * dy + dz * dz);
//    }

}
