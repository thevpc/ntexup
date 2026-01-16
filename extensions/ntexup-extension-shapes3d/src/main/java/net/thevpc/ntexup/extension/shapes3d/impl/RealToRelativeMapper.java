package net.thevpc.ntexup.extension.shapes3d.impl;

import net.thevpc.ntexup.lib.geometry3d.NTxPoint3D;

public interface RealToRelativeMapper {
    NTxPoint3D mapPoint(NTxPoint3D other);
}
