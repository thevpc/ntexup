package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;

import java.util.List;

public interface NTxRoutedPath {
    double length();

    NTxPoint2D pointAt(double s);

    /**
     * Like {@link #pointAt(double)} but extrapolates along the first/last
     * segment when {@code s} is outside {@code [0, length()]}. Line labels may
     * legitimately sit outside the segment (negative/&gt;100 positions).
     */
    default NTxPoint2D pointAtExtended(double s) {
        double len = length();
        if (s >= 0 && s <= len) {
            return pointAt(s);
        }
        List<NTxPoint2D> v = vertices();
        if (v.size() < 2) {
            return pointAt(s);
        }
        NTxPoint2D a;
        NTxPoint2D b;
        NTxPoint2D base;
        double amount;
        if (s < 0) {
            a = v.get(0);
            b = v.get(1);
            base = a;
            amount = s;
        } else {
            a = v.get(v.size() - 2);
            b = v.get(v.size() - 1);
            base = b;
            amount = s - len;
        }
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double d = Math.hypot(dx, dy);
        if (d < 1e-9) {
            return pointAt(s);
        }
        return new NTxPoint2D(base.x + dx / d * amount, base.y + dy / d * amount);
    }

    NTxPoint2D normalAt(double s);

    List<NTxPoint2D> vertices();
}