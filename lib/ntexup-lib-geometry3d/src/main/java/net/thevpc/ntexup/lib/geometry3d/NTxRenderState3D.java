package net.thevpc.ntexup.lib.geometry3d;

public interface NTxRenderState3D {
    NTxVector3D lightOrientation();

    NtxElement3DPrimitive[] toPrimitives(NtxElement3D e);
}
