package net.thevpc.ntexup.api.document.elem3d;

import net.thevpc.ntexup.api.document.NtxElement;

import java.awt.*;

public interface NtxElement3D extends NtxElement {

    Stroke getLineStroke();

    NtxElement3D setLineStroke(Stroke lineStroke);

    Stroke getLineStroke(String edge);

    NtxElement3D setLineStroke(Stroke stroke, String edge);

    Paint getBackgroundPaint();
    NtxElement3D setBackgroundPaint(Paint color);

    Paint getBackgroundPaint(String face);
    NtxElement3D setBackgroundPaint(Paint color, String face);

    Paint getForegroundPaint();
    NtxElement3D setForegroundPaint(Paint color);

    NTxMatrix3D getTransform();

    NtxElement3D setTransform(NTxMatrix3D transform);

    Paint getLinePaint();

    NtxElement3D setLinePaint(Paint contourPaint);

    Composite getComposite();

    NtxElement3D setComposite(Composite composite);

    Paint getContourPaint();

    NtxElement3D setContourPaint(Paint contourPaint) ;

    Stroke getContourStroke();

    NtxElement3D setContourStroke(Stroke contourPaint) ;

}
