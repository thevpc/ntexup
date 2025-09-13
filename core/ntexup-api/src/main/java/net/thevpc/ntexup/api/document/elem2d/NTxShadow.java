package net.thevpc.ntexup.api.document.elem2d;

import net.thevpc.nuts.util.NBlankable;

import java.awt.Paint;
import java.util.Objects;

public class NTxShadow implements Cloneable, NBlankable {

    private NTxPoint2D translation;
    private Paint color;
    private NTxPoint2D shear;
    private NTxPoint2D zoom;
    private double radius;
    private double alpha;

    public double getAlpha() {
        return alpha;
    }

    public NTxShadow setAlpha(double alpha) {
        this.alpha = alpha;
        return this;
    }

    public void setColor(Paint color) {
        this.color = color;
    }

    public Paint getColor() {
        return color;
    }

    public NTxPoint2D getZoom() {
        return zoom;
    }

    public NTxShadow setZoom(NTxPoint2D zoom) {
        this.zoom = zoom;
        return this;
    }

    public NTxPoint2D getTranslation() {
        return translation;
    }

    public double getRadius() {
        return radius;
    }

    public NTxShadow setRadius(double radius) {
        this.radius = radius;
        return this;
    }

    @Override
    public boolean isBlank() {
        if (translation != null) {
            if (translation.x != 0) {
                return false;
            }
            if (translation.y != 0) {
                return false;
            }
        }
        if (zoom != null) {
            if (zoom.x != 0) {
                return false;
            }
            if (zoom.y != 0) {
                return false;
            }
        }
        if (shear != null) {
            if (shear.x != 0) {
                return false;
            }
            if (shear.y != 0) {
                return false;
            }
        }
        if (!Double.isNaN(radius) && radius > 0) {
            return false;
        }
        if (!Double.isNaN(alpha)) {
            return false;
        }
        return false;
    }

    public void setTranslation(NTxPoint2D translation) {
        this.translation = translation;
    }

    public NTxPoint2D getShear() {
        return shear;
    }

    public void setShear(NTxPoint2D shear) {
        this.shear = shear;
    }

    @Override
    public String toString() {
        return "Shadow{" + "translation=" + translation + ", color=" + color + ", shear=" + shear + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.translation);
        hash = 29 * hash + Objects.hashCode(this.color);
        hash = 29 * hash + Objects.hashCode(this.shear);
        hash = 29 * hash + Objects.hashCode(this.zoom);
        hash = 29 * hash + Objects.hashCode(this.radius);
        hash = 29 * hash + Objects.hashCode(this.alpha);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NTxShadow other = (NTxShadow) obj;
        if (!Objects.equals(this.translation, other.translation)) {
            return false;
        }
        if (!Objects.equals(this.color, other.color)) {
            return false;
        }
        if (!Objects.equals(this.shear, other.shear)) {
            return false;
        }
        if (!Objects.equals(this.zoom, other.zoom)) {
            return false;
        }
        if (!Objects.equals(this.radius, other.radius)) {
            return false;
        }
        if (!Objects.equals(this.alpha, other.alpha)) {
            return false;
        }
        return true;
    }

    public NTxShadow copy() {
        try {
            return (NTxShadow) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
