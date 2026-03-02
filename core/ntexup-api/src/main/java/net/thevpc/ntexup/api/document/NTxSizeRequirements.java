package net.thevpc.ntexup.api.document;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;

public class NTxSizeRequirements {
    public double minX;
    public double maxX;
    public double preferredX;
    public double minY;
    public double maxY;
    public double preferredY;

    public NTxSizeRequirements() {

    }

    public NTxSizeRequirements(double minX, double maxX, double preferredX, double minY, double maxY, double preferredY) {
        this.minX = minX;
        this.maxX = maxX;
        this.preferredX = preferredX;
        this.minY = minY;
        this.maxY = maxY;
        this.preferredY = preferredY;
    }

    public NTxBounds2D toBounds2() {
        return NTxBounds2D.of(
                minX, maxX,
                minY, maxY
        );
    }

    public NTxSizeRequirements(NTxBounds2D b) {
        this.minX = b.minX();
        this.maxX = b.maxX();
        this.minY = b.minY();
        this.maxY = b.maxY();
        this.preferredX = b.maxY();
        this.preferredY = b.maxY();
    }

    @Override
    public String toString() {
        return "HSizeRequirements{"
                + "min=(" + minX +","+minY+")"
                + " max=(" + maxX +","+maxY+")"
                + " pref=(" + preferredX +","+preferredY+")"
                +'}';
    }
}
