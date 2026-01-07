package net.thevpc.ntexup.extension.shapes3d.api.primitives;

import net.thevpc.ntexup.extension.shapes3d.api.AbstractNTxElement3DPrimitive;
import net.thevpc.ntexup.extension.shapes3d.api.NTxElement3DPrimitiveType;
import net.thevpc.ntexup.extension.shapes3d.api.NTxPoint3D;

import java.util.Arrays;

public class NtxElement3DPolygon extends AbstractNTxElement3DPrimitive {
    private NTxPoint3D[] nodes;
    private boolean fill;
    private boolean contour;

    public NtxElement3DPolygon(NTxPoint3D[] nodes, boolean fill, boolean contour) {
        this.nodes = nodes;
        this.fill = fill;
        this.contour = contour;
    }

    public boolean isFill() {
        return fill;
    }

    public boolean isContour() {
        return contour;
    }

    public NTxPoint3D[] getNodes() {
        return nodes;
    }

    @Override
    public NTxPoint3D[] points() {
        return nodes;
    }

    @Override
    public NTxElement3DPrimitiveType type() {
        return NTxElement3DPrimitiveType.POLYGON;
    }

    @Override
    public String toString() {
        return "NtxElement3DPolygon{" +
                "nodes=" + Arrays.toString(nodes) +
                ", fill=" + fill +
                ", contour=" + contour +
                '}';
    }
}
