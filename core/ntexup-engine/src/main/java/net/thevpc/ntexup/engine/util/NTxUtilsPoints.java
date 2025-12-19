package net.thevpc.ntexup.engine.util;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.elem3d.NTxPoint3D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.util.NTxElementUtils;
import net.thevpc.nuts.util.NOptional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NTxUtilsPoints {
    static
    public boolean addPoints(NTxNode line, NTxPoint2D[] points) {
        boolean result = false;
        for (NTxPoint2D point : points) {
            result |= addPoint(line, point);
        }
        return result;
    }

    static
    public boolean addPoints(NTxNode line, NTxPoint3D[] points) {
        boolean result = false;
        for (NTxPoint3D point : points) {
            result |= addPoint(line, point);
        }
        return result;
    }

    static
    public boolean addPoint(NTxNode line, NTxPoint2D point) {
        if (point != null) {
            NTxValue o = NTxValue.of(line.getPropertyValue(NTxPropName.POINTS).orNull());
            NOptional<NTxPoint2D[]> hPoint2DArray = o.asPoint2DArray();
            java.util.List<NTxPoint2D> v = new ArrayList<>();
            if (hPoint2DArray.isPresent()) {
                v.addAll(Arrays.asList(hPoint2DArray.get()));
            }
            v.add(point);
            line.setProperty(NTxPropName.POINTS, NTxElementUtils.toElement(v.toArray(new NTxPoint2D[0])));
            return true;
        }
        return false;
    }

    static
    public boolean addPoint(NTxNode line, NTxPoint3D point) {
        if (point != null) {
            NTxValue o = NTxValue.of(line.getPropertyValue(NTxPropName.POINTS).orNull());
            NOptional<NTxPoint3D[]> hPoint2DArray = o.asPoint3DArray();
            List<NTxPoint3D> v = new ArrayList<>();
            if (hPoint2DArray.isPresent()) {
                v.addAll(Arrays.asList(hPoint2DArray.get()));
            }
            v.add(point);
            line.setProperty(NTxPropName.POINTS, NTxElementUtils.toElement(v.toArray(new NTxPoint3D[0])));
            return true;
        }
        return false;
    }
}
