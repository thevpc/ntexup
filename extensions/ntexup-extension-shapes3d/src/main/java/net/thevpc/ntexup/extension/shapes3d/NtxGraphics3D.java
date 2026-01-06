package net.thevpc.ntexup.extension.shapes3d;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem3d.NTxLight3D;
import net.thevpc.ntexup.api.document.elem3d.NTxMatrix3D;
import net.thevpc.ntexup.api.document.elem3d.NtxElement3D;

public interface NtxGraphics3D {
    void transform3D(NTxMatrix3D transform3D);

    void draw3D(NtxElement3D element3D, NTxPoint2D origin);

    NTxLight3D getLight3D();

    NTxLight3D setLight3D(NTxLight3D light3D);

}
