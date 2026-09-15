package net.thevpc.ntexup.api.document.elem2d;

import java.awt.*;
import java.awt.image.ImageObserver;

public class NTxImageOptions {
    private Color transparentColor;
    private boolean disableAnimation;
    private ImageObserver imageObserver;
    private Runnable asyncLoad;
    private Dimension size;
    private boolean preserveAspectRatio;

    public boolean isPreserveAspectRatio() {
        return preserveAspectRatio;
    }

    public NTxImageOptions setPreserveAspectRatio(boolean preserveAspectRatio) {
        this.preserveAspectRatio = preserveAspectRatio;
        return this;
    }

    public Dimension getSize() {
        return size;
    }

    public NTxImageOptions setSize(Dimension size) {
        this.size = size;
        return this;
    }

    public Runnable getAsyncLoad() {
        return asyncLoad;
    }

    public NTxImageOptions setAsyncLoad(Runnable asyncLoad) {
        this.asyncLoad = asyncLoad;
        return this;
    }

    public ImageObserver getImageObserver() {
        return imageObserver;
    }

    public NTxImageOptions setImageObserver(ImageObserver imageObserver) {
        this.imageObserver = imageObserver;
        return this;
    }

    public Color getTransparentColor() {
        return transparentColor;
    }

    public NTxImageOptions setTransparentColor(Color transparentColor) {
        this.transparentColor = transparentColor;
        return this;
    }

    public boolean isDisableAnimation() {
        return disableAnimation;
    }

    public NTxImageOptions setDisableAnimation(boolean disableAnimation) {
        this.disableAnimation = disableAnimation;
        return this;
    }
}
