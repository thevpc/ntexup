package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;

import net.thevpc.nuts.util.NOptional;

import java.awt.geom.AffineTransform;
import java.util.List;

public interface NTxGeometryIndex {
    void enter(NTxNode node, NTxBounds2D selfBounds, double angleRad, double pivotX, double pivotY, String nameOrNull);

    void exit(NTxNode node);

    void markHidden(NTxNode node);

    NOptional<NTxPlaced> resolve(NTxNode from, String ref);

    List<NTxPlaced> placed();

    AffineTransform currentToRoot();
}