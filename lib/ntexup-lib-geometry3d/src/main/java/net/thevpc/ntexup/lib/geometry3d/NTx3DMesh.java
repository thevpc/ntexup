package net.thevpc.ntexup.lib.geometry3d;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DPolygon;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DTriangle;

import java.util.List;

public interface NTx3DMesh {
    void triangulatePolygon(NtxElement3DPolygon polygon, List<? super NtxElement3DTriangle> out);

    boolean refineTriangle(NtxElement3DTriangle triangle, List<? super NtxElement3DTriangle> out);

    NTx3DMesh configureScene(NTxNode polygon, NTxNodeRendererContext rendererContext);

    NTx3DMesh configureElement(NtxElement3D polygon);

}
