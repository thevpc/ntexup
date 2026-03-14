package net.thevpc.ntexup.lib.geometry3d.impl.composite;

import net.thevpc.ntexup.lib.geometry3d.NtxFace;

import java.awt.*;

public class NtxFaceImpl implements NtxFace {
    private boolean visible=true;
    private boolean drawContour=true;
    private boolean fillBackground=true;
    private Paint background;
    private Paint lineColor;
    private Stroke stroke;

    public Paint getLineColor() {
        return lineColor;
    }

    public NtxFaceImpl setLineColor(Paint lineColor) {
        this.lineColor = lineColor;
        return this;
    }

    @Override
    public boolean isDrawContour() {
        return drawContour;
    }

    public NtxFaceImpl setDrawContour(boolean drawContour) {
        this.drawContour = drawContour;
        return this;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    public NtxFace setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    @Override
    public Paint getBackground() {
        return background;
    }

    public NtxFace setBackground(Paint background) {
        this.background = background;
        return this;
    }

    @Override
    public Stroke getStroke() {
        return stroke;
    }

    public NtxFace setStroke(Stroke stroke) {
        this.stroke = stroke;
        return this;
    }

    @Override
    public boolean isFillBackground() {
        return fillBackground;
    }

    public NtxFace setFillBackground(boolean fillBackground) {
        this.fillBackground = fillBackground;
        return this;
    }
}
