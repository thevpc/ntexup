package net.thevpc.ntexup.api.document.elem2d;

import net.thevpc.ntexup.api.util.NTxUtils;

public class NTxBounds3D {
    private Double x1;
    private Double x2;
    private Double y1;
    private Double y2;
    private Double z1;
    private Double z2;

    public static NTxBounds3D ofFull() {
        return new NTxBounds3D(0.0, 100.0, 0.0, 100.0, 0.0, 100.0);
    }
    public static NTxBounds3D ofUnit() {
        return ofWidth(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    }

    public static NTxBounds3D of(Number x1, Number x2, Number y1, Number y2, Number z1, Number z2) {
        return new NTxBounds3D(x1, x2, y1, y2, z1, z2);
    }


    public static NTxBounds3D ofWidth(Number x1, Number y1, Number z1, Number wx, Number wy, Number wz) {
        return new NTxBounds3D(x1, NTxUtils.addDouble(x1,wx), y1, NTxUtils.addDouble(y1,wy), z1,NTxUtils.addDouble(z1,wz));
    }

    public NTxBounds3D(Number x1, Number x2, Number y1, Number y2, Number z1, Number z2) {
        this.x1 = x1 == null ? null : x1.doubleValue();
        this.x2 = x2 == null ? null : x2.doubleValue();
        this.y1 = y1 == null ? null : y1.doubleValue();
        this.y2 = y2 == null ? null : y2.doubleValue();
        this.z1 = z1 == null ? null : z1.doubleValue();
        this.z2 = z2 == null ? null : z2.doubleValue();
    }

    public NTxDouble3 size() {
        return new NTxDouble3(widthX(), widthY(), widthZ());
    }

    public Double minX() {
        return NTxUtils.min(x1, x2);
    }

    public Double maxX() {
        return NTxUtils.max(x1, x2);
    }

    public Double widthX() {
        return NTxUtils.distance(x1, x2);
    }

    public Double centerX() {
        return NTxUtils.center(x1, x2);
    }

    public Double minY() {
        return NTxUtils.min(y1, y2);
    }

    public Double maxY() {
        return NTxUtils.max(y1, y2);
    }

    public Double widthY() {
        return NTxUtils.distance(y1, y2);
    }

    public Double centerY() {
        return NTxUtils.center(y1, y2);
    }

    public Double minZ() {
        return NTxUtils.min(z1, z2);
    }

    public Double maxZ() {
        return NTxUtils.max(z1, z2);
    }

    public Double widthZ() {
        return NTxUtils.distance(z1, z2);
    }

    public Double centerZ() {
        return NTxUtils.center(z1, z2);
    }

    @Override
    public String toString() {
        return "(" + x1 + ", " + x2 + ", " + y1 + ", " + y2 + ", " + z1 + ", " + z2 + ')';
    }

    public NTxBounds3D expand(NTxBounds3D s) {
        if (s == null) {
            return new NTxBounds3D(x1, x2, y1, y2, z1, z2);
        }

        Double xx1 = NTxUtils.min(minX(), s.minX());
        Double xx2 = NTxUtils.max(maxX(), s.maxX());
        Double yy1 = NTxUtils.min(minY(), s.minY());
        Double yy2 = NTxUtils.max(maxY(), s.maxY());
        Double zz1 = NTxUtils.min(minZ(), s.minZ());
        Double zz2 = NTxUtils.max(maxZ(), s.maxZ());
        return new NTxBounds3D(
                xx1,
                xx2,
                yy1,
                yy2,
                zz1,
                zz2
        );
    }

}
