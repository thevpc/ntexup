package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public final class NTxPlaced {
    public final NTxNode node;
    public final String name;
    public final NTxBounds2D bounds;
    public final AffineTransform toRoot;
    public final NTxNode[] ancestors;
    public boolean hidden;
    public boolean container;
    public boolean obstacle;

    public NTxPlaced(NTxNode node, String name, NTxBounds2D bounds, AffineTransform toRoot,
                     NTxNode[] ancestors, boolean hidden, boolean container, boolean obstacle) {
        this.node = node;
        this.name = name;
        this.bounds = bounds;
        this.toRoot = toRoot;
        this.ancestors = ancestors;
        this.hidden = hidden;
        this.container = container;
        this.obstacle = obstacle;
    }

    public Shape outline() {
        Rectangle2D r = new Rectangle2D.Double(bounds.minX(), bounds.minY(), bounds.widthX(), bounds.widthY());
        return toRoot.createTransformedShape(r);
    }
}