package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.nuts.util.NOptional;

import java.awt.geom.AffineTransform;
import java.util.Collections;
import java.util.List;

public class NTxGeometryIndexNoOp implements NTxGeometryIndex {
    public static final NTxGeometryIndexNoOp INSTANCE = new NTxGeometryIndexNoOp();

    @Override
    public void enter(NTxNode node, NTxBounds2D selfBounds, double angleRad, double pivotX, double pivotY, String nameOrNull) {
    }

    @Override
    public void exit(NTxNode node) {
    }

    @Override
    public void markHidden(NTxNode node) {
    }

    @Override
    public NOptional<NTxPlaced> resolve(NTxNode from, String ref) {
        return NOptional.ofNamedEmpty("geometry");
    }

    @Override
    public List<NTxPlaced> placed() {
        return Collections.emptyList();
    }

    @Override
    public AffineTransform currentToRoot() {
        return new AffineTransform();
    }
}