package net.thevpc.ntexup.extension.shapes3d.api;

public interface NTxRenderState3D {
    NTxVector3D lightOrientation();

    NtxElement3DPrimitive[] toPrimitives(NtxElement3D e);
}
