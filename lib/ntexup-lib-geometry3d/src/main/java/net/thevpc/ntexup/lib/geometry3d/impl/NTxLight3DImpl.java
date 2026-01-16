package net.thevpc.ntexup.lib.geometry3d.impl;

import net.thevpc.ntexup.lib.geometry3d.NTxLight3D;
import net.thevpc.ntexup.lib.geometry3d.NTxVector3D;

public class NTxLight3DImpl implements NTxLight3D, Cloneable {
    private NTxVector3D orientation = new NTxVector3D(0, 0, 1);

    public NTxVector3D orientation() {
        return orientation;
    }

    public NTxLight3DImpl setOrientation(NTxVector3D orientation) {
        NTxLight3DImpl n = (NTxLight3DImpl) copy();
        n.orientation = orientation == null ? new NTxVector3D(0, -1, 0) : orientation;
        return n;
    }

    public NTxLight3D copy() {
        try {
            return (NTxLight3D) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
