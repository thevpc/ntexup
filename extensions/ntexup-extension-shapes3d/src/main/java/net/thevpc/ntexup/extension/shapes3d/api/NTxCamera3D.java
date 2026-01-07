package net.thevpc.ntexup.extension.shapes3d.api;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;

public interface NTxCamera3D {

    NTxPoint3D getPosition();

    NTxMatrix3D getViewMatrix();

    NTxPoint2D[] projectFromWorldToScreen(NTxPoint3D[] worldPoint, NTxPoint2D screenOrigin);

    NTxPoint2D projectFromWorldToScreen(NTxPoint3D worldPoint, NTxPoint2D screenOrigin);
}
