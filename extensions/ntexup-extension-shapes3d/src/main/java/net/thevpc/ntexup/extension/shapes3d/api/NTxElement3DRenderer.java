package net.thevpc.ntexup.extension.shapes3d.api;


public interface NTxElement3DRenderer {
    Class<? extends NtxElement3D> forType();
    NtxElement3DPrimitive[] toPrimitives(NtxElement3D e, NTxRenderState3D renderState);
}
