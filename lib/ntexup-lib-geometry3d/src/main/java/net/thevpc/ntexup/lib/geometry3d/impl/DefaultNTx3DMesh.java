package net.thevpc.ntexup.lib.geometry3d.impl;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.eval.NTxValueByType;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.lib.geometry3d.NTx3DMesh;
import net.thevpc.ntexup.lib.geometry3d.NtxElement3D;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DPolygon;
import net.thevpc.ntexup.lib.geometry3d.impl.primitives.NtxElement3DTriangle;

import java.awt.*;
import java.util.List;
import java.util.Objects;

public class DefaultNTx3DMesh implements NTx3DMesh {
    int maxEdge = 100;
    boolean showMesh = false;
    Stroke meshStroke;
    Paint meshPaint;

    public DefaultNTx3DMesh() {
    }

    public DefaultNTx3DMesh(int maxEdge, boolean showMesh, Stroke meshStroke, Paint meshPaint) {
        if (maxEdge <= 0) {
            maxEdge = 100;
        }
        this.maxEdge = maxEdge;
        this.showMesh = showMesh;
        this.meshStroke = meshStroke;
        this.meshPaint = meshPaint;
    }

    public NTx3DMesh configureScene(NTxNode node, NTxRendererContext rendererContext) {
        int _maxEdge = NTxValue.ofProp(node, "mesh-precision").asInt().orElse(maxEdge);
        if (_maxEdge <= 0) {
            _maxEdge = maxEdge;
        }
        boolean _showMesh = NTxValue.ofProp(node, "mesh-visible").asBoolean().orElse(showMesh);
        Paint _meshPaint = NTxValueByType.getPaint(rendererContext, "mesh-color").orElse(null);
        Stroke _meshStroke = (rendererContext.graphics().createStroke(NTxValueByType.getElement(rendererContext, "mesh-stroke").orNull()));
        if (_meshStroke == null) {
            _meshStroke = meshStroke;
        }
        if (_meshPaint == null) {
            _meshPaint = meshPaint;
        }
        if (
                _maxEdge != maxEdge
                        || _showMesh != showMesh
                        || !Objects.equals(_meshStroke, meshStroke)
                        || !Objects.equals(_meshPaint, meshPaint)
        ) {
            return new DefaultNTx3DMesh(_maxEdge, _showMesh, _meshStroke, _meshPaint);
        }
        return this;
    }

    @Override
    public NTx3DMesh configureElement(NtxElement3D polygon) {
        Double _maxEdge = polygon.getMeshPrecision();
        if (_maxEdge!=null) {
            int iv = _maxEdge.intValue();
            if(iv<=0) {
                _maxEdge = null;
            }
        }
        if (_maxEdge==null) {
            _maxEdge=(double)maxEdge;
        }
        Boolean _showMesh = polygon.getMeshVisible();
        if (_showMesh == null) {
            _showMesh = showMesh;
        }
        Paint _meshPaint = polygon.getMeshPaint();
        Stroke _meshStroke = polygon.getMeshStroke();
        if (_meshStroke == null) {
            _meshStroke = meshStroke;
        }
        if (_meshPaint == null) {
            _meshPaint = meshPaint;
        }
        if (
                _maxEdge.intValue() != maxEdge
                        || _showMesh != showMesh
                        || !Objects.equals(_meshStroke, meshStroke)
                        || !Objects.equals(_meshPaint, meshPaint)
        ) {
            return new DefaultNTx3DMesh(_maxEdge.intValue(), _showMesh, meshStroke, meshPaint);
        }
        return this;
    }

    @Override
    public void triangulatePolygon(NtxElement3DPolygon polygon, List<? super NtxElement3DTriangle> out) {
        MeshHelper.triangulatePolygon(polygon, out, maxEdge);
    }

    @Override
    public boolean refineTriangle(NtxElement3DTriangle triangle, List<? super NtxElement3DTriangle> out) {
        return MeshHelper.refineTriangle(triangle, out, maxEdge, showMesh);
    }
}
