package net.thevpc.ntexup.extension.shapes3d.api;

public class NTxVector3D {
    public double x, y, z;

    public NTxVector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public NTxVector3D minus(NTxVector3D b) {
        return new NTxVector3D(
                x - b.x,
                y - b.y,
                z - b.z
        );
    }


    public double norm() {
        return Math.sqrt(dot(this));
    }

    public NTxVector3D normalize() {
        double lrcp = 1.0 / Math.sqrt(dot(this));
        return new NTxVector3D(this.x * lrcp, this.y * lrcp, this.z * lrcp);
    }

    public NTxVector3D cross(NTxVector3D o) {
        return new NTxVector3D(
                this.y * o.z - this.z * o.y
                , this.z * o.x - this.x * o.z
                , this.x * o.y - this.y * o.x
        );
    }

    public NTxVector3D plus(NTxVector3D b) {
        return new NTxVector3D(
                x + b.x,
                y + b.y,
                z + b.z
        );
    }

    public NTxVector3D plus(double b) {
        return new NTxVector3D(
                x + b,
                y + b,
                z + b
        );
    }

    public NTxVector3D minus(double b) {
        return new NTxVector3D(
                x - b,
                y - b,
                z - b
        );
    }

    public NTxVector3D mul(double b) {
        return new NTxVector3D(
                x * b,
                y * b,
                z * b
        );
    }

    public double dot(NTxVector3D b) {
        return (
                x * b.x +
                        y * b.y +
                        z * b.z
        );
    }
}
