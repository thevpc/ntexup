package net.thevpc.ntexup.extension.shapes3d.api.composite;

import net.thevpc.ntexup.extension.shapes3d.api.AbstractNTxElement3D;
import net.thevpc.ntexup.extension.shapes3d.api.NTxPoint3D;

public class NtxElement3DSurface extends AbstractNTxElement3D {
    private NTxPoint3D[] points;

    public NtxElement3DSurface(NTxPoint3D... points) {
        this.points = points;
    }

    public NTxPoint3D[] getPoints() {
        return points;
    }

}
