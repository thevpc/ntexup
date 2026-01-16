package net.thevpc.ntexup.lib.geometry2d;

import net.thevpc.ntexup.api.document.elem2d.NTxDouble2;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.nuts.elem.NArrayElement;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.util.NOptional;

import java.util.Arrays;
import java.util.List;

public class NTx2DUtils {
    public static NOptional<NTxPoint2D> asPoint2D(Object element) {
        if (element instanceof NTxPoint2D) {
            return NOptional.of((NTxPoint2D) element);
        }
        NOptional<double[]> d = NTxValue.of(element).asDoubleArray();
        if (d.isPresent()) {
            double[] dd = d.get();
            if (dd.length == 2) {
                return NOptional.of(new NTxPoint2D(dd[0], dd[1]));
            }
        }
        return NOptional.ofNamedEmpty("Point2D from " + element);
    }

    public static NOptional<NTxSize2D> asSize2D(Object element) {
        if (element instanceof NTxSize2D) {
            return NOptional.of((NTxSize2D) element);
        }
        NOptional<double[]> d = NTxValue.of(element).asDoubleArray();
        if (d.isPresent()) {
            double[] dd = d.get();
            if (dd.length == 2) {
                return NOptional.of(new NTxSize2D(dd[0], dd[1]));
            }
        }
        return NOptional.ofNamedEmpty("NTxSize2D from " + element);
    }

    public static NOptional<NTxPoint2D[]> asPoint2DArray(Object element) {
        if (element instanceof NTxPoint2D[]) {
            return NOptional.of((NTxPoint2D[]) element);
        }
        NOptional<NTxDouble2[]> u = NTxValue.of(element).asDouble2Array();
        if (u.isPresent()) {
            return NOptional.of(
                    Arrays.stream(u.get()).map(x -> new NTxPoint2D(x.getX(), x.getY())).toArray(NTxPoint2D[]::new)
            );
        } else {
            return NOptional.ofNamedEmpty("Point2D[] from " + element);
        }
    }

    public static NOptional<NTxPoint2D[][]> asPoint2DArray2Or1(Object element) {
        if (element instanceof NTxPoint2D[][]) {
            return NOptional.of((NTxPoint2D[][]) element);
        }
        if (element instanceof NTxPoint2D[]) {
            NTxPoint2D[][] v = new NTxPoint2D[1][];
            v[0] = (NTxPoint2D[]) element;
            return NOptional.of(v);
        }
        if (element instanceof NElement) {
            NElement te = (NElement) element;
            if (te.isListContainer()) {
                NArrayElement array = te.toArray().get();
                if (array.isEmpty()) {
                    return NOptional.of(new NTxPoint2D[0][0]);
                }
                if (array.get(0).get().isListContainer()) {
                    NTxPoint2D[][] all = new NTxPoint2D[array.size()][];
                    List<NElement> children = array.children();
                    for (int i = 0; i < children.size(); i++) {
                        NElement child = children.get(i);
                        NOptional<NTxPoint2D[]> u = asPoint2DArray(child);
                        if (u.isPresent()) {
                            all[i] = u.get();
                        } else {
                            return NOptional.ofNamedEmpty("Point2DArray from " + child);
                        }
                    }
                    return NOptional.of(all);
                }
                NOptional<NTxPoint2D[]> u = asPoint2DArray(element);
                if (u.isPresent()) {
                    NTxPoint2D[][] v = new NTxPoint2D[1][];
                    v[0] = u.get();
                    return NOptional.of(v);
                }
                return NOptional.ofNamedEmpty("Point2DArray from " + element);
            }
        }
        return NOptional.ofNamedEmpty("Point2DArray from " + element);
    }

}
