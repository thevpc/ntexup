package net.thevpc.ntexup.lib.geometry3d;


import net.thevpc.nuts.spi.NComponent;

public interface NTxElement3DRenderer extends NComponent {
    Class<? extends NtxElement3D> forType();
    NtxElement3DPrimitive[] toPrimitives(NtxElement3D e, NTxRenderState3D renderState);
}
