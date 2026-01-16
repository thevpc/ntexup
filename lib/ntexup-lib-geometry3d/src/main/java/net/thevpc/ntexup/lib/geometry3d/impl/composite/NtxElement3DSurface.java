package net.thevpc.ntexup.lib.geometry3d.impl.composite;

import net.thevpc.ntexup.lib.geometry3d.impl.AbstractNTxElement3D;
import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;

public class NtxElement3DSurface extends AbstractNTxElement3D {
    private NTxPoint3D[] points;

    public NtxElement3DSurface(NTxPoint3D... points) {
        this.points = points;
    }

    public NTxPoint3D[] getPoints() {
        return points;
    }

}
