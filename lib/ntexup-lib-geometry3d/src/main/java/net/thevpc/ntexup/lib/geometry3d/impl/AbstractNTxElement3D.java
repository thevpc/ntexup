package net.thevpc.ntexup.lib.geometry3d.impl;

import net.thevpc.ntexup.lib.geometry3d.NTxMatrix3D;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractNTxElement3D implements NtxElement3D {
    private NTxMatrix3D transform = NTxMatrix3D.identity();
    private Paint backgroundColor;
    private Paint foregroundColor;
    private Stroke lineStroke;
    private Stroke contourStroke;
    private Map<String, Paint> backgroundColors;
    private Map<String, Stroke> strokes;
    private Paint linePaint;
    private Paint contourPaint;
    private Composite composite;
    private Double meshPrecision;
    private Boolean meshVisible;
    private Paint meshPaint;
    private Stroke meshStroke;

    @Override
    public Boolean getMeshVisible() {
        return meshVisible;
    }

    @Override
    public AbstractNTxElement3D setMeshVisible(Boolean meshVisible) {
        this.meshVisible = meshVisible;
        return this;
    }

    @Override
    public Stroke getMeshStroke() {
        return meshStroke;
    }

    @Override
    public AbstractNTxElement3D setMeshStroke(Stroke meshStroke) {
        this.meshStroke = meshStroke;
        return this;
    }

    @Override
    public Paint getMeshPaint() {
        return meshPaint;
    }

    @Override
    public AbstractNTxElement3D setMeshPaint(Paint meshPaint) {
        this.meshPaint = meshPaint;
        return this;
    }

    public Double getMeshPrecision() {
        return meshPrecision;
    }

    public AbstractNTxElement3D setMeshPrecision(Double meshPrecision) {
        this.meshPrecision = meshPrecision;
        return this;
    }

    @Override
    public void copyStyle(NtxElement3D other) {
        if (other != null) {
            this.setBackgroundPaint(other.getBackgroundPaint());
            this.setLinePaint(other.getLinePaint());
            this.setLineStroke(other.getLineStroke());
            this.setComposite(other.getComposite());
            this.setContourPaint(other.getContourPaint());
            this.setContourStroke(other.getContourStroke());
            this.setTransform(other.getTransform());
            this.setBackgroundPaint(other.getBackgroundPaint());
            this.setComposite(other.getComposite());
            this.setMeshPrecision(other.getMeshPrecision());
            this.setMeshPaint(other.getMeshPaint());
            this.setMeshStroke(other.getMeshStroke());
            this.setMeshVisible(other.getMeshVisible());
        }
    }

    public Stroke getLineStroke() {
        return lineStroke;
    }

    public NtxElement3D setLineStroke(Stroke lineStroke) {
        this.lineStroke = lineStroke;
        return this;
    }

    @Override
    public Stroke getLineStroke(String edge) {
        if (strokes != null) {
            return strokes.get(edge);
        }
        return null;
    }

    @Override
    public NtxElement3D setLineStroke(Stroke stroke, String edge) {
        if (stroke == null) {
            if (strokes != null) {
                strokes.remove(edge);
            }
        } else {
            if (strokes == null) {
                strokes = new HashMap<>();
            }
            strokes.put(edge, stroke);
        }
        return this;
    }

    @Override
    public Paint getBackgroundPaint() {
        return backgroundColor;
    }

    @Override
    public NtxElement3D setBackgroundPaint(Paint color) {
        this.backgroundColor = color;
        return this;
    }

    @Override
    public Paint getBackgroundPaint(String face) {
        if (backgroundColors != null) {
            return backgroundColors.get(face);
        }
        return null;
    }

    @Override
    public NtxElement3D setBackgroundPaint(Paint color, String face) {
        if (color == null) {
            if (backgroundColors != null) {
                backgroundColors.remove(face);
            }
        } else {
            if (backgroundColors == null) {
                backgroundColors = new HashMap<>();
            }
            backgroundColors.put(face, color);
        }
        return this;
    }

    @Override
    public Paint getForegroundPaint() {
        return foregroundColor;
    }

    @Override
    public NtxElement3D setForegroundPaint(Paint color) {
        this.foregroundColor = color;
        return this;
    }

    @Override
    public NTxMatrix3D getTransform() {
        return transform;
    }

    @Override
    public NtxElement3D setTransform(NTxMatrix3D transform) {
        this.transform = transform == null ? NTxMatrix3D.identity() : transform;
        return this;
    }

    public Composite getComposite() {
        return composite;
    }

    public NtxElement3D setComposite(Composite composite) {
        this.composite = composite;
        return this;
    }

    public Paint getLinePaint() {
        return linePaint;
    }

    public NtxElement3D setLinePaint(Paint contourPaint) {
        this.linePaint = contourPaint;
        return this;
    }

    public Paint getContourPaint() {
        return contourPaint;
    }

    public NtxElement3D setContourPaint(Paint contourPaint) {
        this.contourPaint = contourPaint;
        return this;
    }

    public Stroke getContourStroke() {
        return contourStroke;
    }

    public NtxElement3D setContourStroke(Stroke contourStroke) {
        this.contourStroke = contourStroke;
        return this;
    }
}
