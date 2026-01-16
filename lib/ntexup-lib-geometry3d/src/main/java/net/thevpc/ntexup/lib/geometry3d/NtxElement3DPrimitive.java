package net.thevpc.ntexup.lib.geometry3d;

public interface NtxElement3DPrimitive extends NtxElement3D {
    NTxElement3DPrimitiveType type();
    NTxPoint3D[] points();
}
