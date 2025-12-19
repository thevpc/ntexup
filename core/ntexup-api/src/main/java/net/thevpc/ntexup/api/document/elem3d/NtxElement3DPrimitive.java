package net.thevpc.ntexup.api.document.elem3d;

public interface NtxElement3DPrimitive extends NtxElement3D {
    NTxElement3DPrimitiveType type();
    NTxPoint3D[] points();

}
