package net.thevpc.ntexup.extension.shapes3d;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.elem3d.NTxPoint3D;
import net.thevpc.ntexup.api.document.elem3d.NtxElement3D;

public class Shape3DHelper {
    public static void copyProps(NtxElement3D from, NtxElement3D to, String name) {
        to.setTransform(from.getTransform());

        to.setLineStroke(from.getLineStroke());
        to.setLinePaint(from.getLinePaint());

        to.setBackgroundPaint(from.getBackgroundPaint());
        to.setForegroundPaint(from.getForegroundPaint());

        to.setContourStroke(from.getContourStroke());
        to.setContourPaint(from.getContourPaint());

        to.setComposite(from.getComposite());
    }

    public static NTxPoint3D convertPoint(NTxPoint3D p, NTxBounds2 b) {
        double h = Math.max(b.getWidth(),b.getHeight());
        return new NTxPoint3D(
                p.x / 100 * b.getWidth()/* + b.getMinX()*/,
                p.y / 100 * b.getHeight()/* + b.getMinY()*/,
                p.z / 100 * h
        );
    }

    public static void copyPropsIfEmptyTarget(NtxElement3D from, NtxElement3D to, String name) {
        if (to.getTransform() == null) {
            to.setTransform(from.getTransform());
        }
        if (to.getLineStroke() == null) {
            to.setLineStroke(from.getLineStroke());
        }
        if (to.getLinePaint() == null) {
            to.setLinePaint(from.getLinePaint());
        }
        if (to.getBackgroundPaint() == null) {
            to.setBackgroundPaint(from.getBackgroundPaint());
        }
        if (to.getForegroundPaint() == null) {
            to.setForegroundPaint(from.getForegroundPaint());
        }

        if (to.getContourStroke() == null) {
            to.setContourStroke(from.getContourStroke());
        }
        if (to.getContourPaint() == null) {
            to.setContourPaint(from.getContourPaint());
        }
        if (to.getComposite() == null) {
            to.setComposite(from.getComposite());
        }
    }
}
