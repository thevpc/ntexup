package net.thevpc.ntexup.extension.shapes3d.api;

public interface NtxElement3DPrimitive extends NtxElement3D {
    NTxElement3DPrimitiveType type();
    NTxPoint3D[] points();

}
