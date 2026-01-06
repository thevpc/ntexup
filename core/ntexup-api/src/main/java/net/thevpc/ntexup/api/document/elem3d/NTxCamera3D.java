package net.thevpc.ntexup.api.document.elem3d;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;

public interface NTxCamera3D {

    NTxMatrix3D getViewMatrix();

    NTxPoint2D projectFromWorldToScreen(NTxPoint3D worldPoint, NTxPoint2D screenOrigin) ;
    NTxPoint2D projectFromLocalToScreen(NTxPoint3D localPoint, NTxMatrix3D worldTransform, NTxPoint2D screenOrigin) ;
}
