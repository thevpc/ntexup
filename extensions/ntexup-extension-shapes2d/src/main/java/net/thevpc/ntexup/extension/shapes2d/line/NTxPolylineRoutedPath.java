package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;

import java.util.ArrayList;
import java.util.List;

public class NTxPolylineRoutedPath implements NTxRoutedPath {
    private final List<NTxPoint2D> vertices;
    private final double[] cumLen;
    private final double total;

    public NTxPolylineRoutedPath(List<NTxPoint2D> vertices) {
        this.vertices = new ArrayList<>(vertices);
        cumLen = new double[this.vertices.size()];
        if (!this.vertices.isEmpty()) {
            cumLen[0] = 0;
        }
        double acc = 0;
        for (int i = 1; i < this.vertices.size(); i++) {
            double dx = this.vertices.get(i).x - this.vertices.get(i - 1).x;
            double dy = this.vertices.get(i).y - this.vertices.get(i - 1).y;
            acc += Math.hypot(dx, dy);
            cumLen[i] = acc;
        }
        total = acc;
    }

    @Override
    public double length() {
        return total;
    }

    @Override
    public NTxPoint2D pointAt(double s) {
        if (vertices.isEmpty()) {
            return new NTxPoint2D(0, 0);
        }
        if (vertices.size() == 1) {
            return vertices.get(0);
        }
        if (s <= 0) {
            return vertices.get(0);
        }
        if (s >= total) {
            return vertices.get(vertices.size() - 1);
        }
        // find segment
        int i = 1;
        while (i < cumLen.length && cumLen[i] < s) {
            i++;
        }
        if (i >= cumLen.length) {
            i = cumLen.length - 1;
        }
        int seg = Math.max(1, i);
        double segStart = cumLen[seg - 1];
        double segLen = cumLen[seg] - segStart;
        double t = segLen == 0 ? 0 : (s - segStart) / segLen;
        NTxPoint2D a = vertices.get(seg - 1);
        NTxPoint2D b = vertices.get(seg);
        return new NTxPoint2D(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t);
    }

    @Override
    public NTxPoint2D normalAt(double s) {
        // determine segment
        if (vertices.size() < 2) {
            return new NTxPoint2D(0, -1);
        }
        double clamped = Math.max(0, Math.min(total, s));
        int seg = 0;
        if (clamped >= total) {
            seg = vertices.size() - 2;
        } else {
            int i = 1;
            while (i < cumLen.length && cumLen[i] < clamped) {
                i++;
            }
            seg = Math.max(0, Math.min(vertices.size() - 2, i - 1));
            if (i >= cumLen.length) seg = vertices.size() - 2;
        }
        NTxPoint2D a = vertices.get(seg);
        NTxPoint2D b = vertices.get(seg + 1);
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double len = Math.hypot(dx, dy);
        if (len < 1e-12) {
            // degenerate; try next
            return new NTxPoint2D(0, -1);
        }
        if (Math.abs(dx) < 1e-6) {
            return new NTxPoint2D(-1.0, 0.0);
        }
        if (Math.abs(dy) < 1e-6) {
            return new NTxPoint2D(0.0, -1.0);
        }
        // perpendicular, flipped so ny <= 0 (same convention as the old line label)
        double nx = -dy / len;
        double ny = dx / len;
        if (ny > 0) {
            nx = -nx;
            ny = -ny;
        }
        // normalize
        double nlen = Math.hypot(nx, ny);
        if (nlen > 0) {
            nx /= nlen;
            ny /= nlen;
        }
        return new NTxPoint2D(nx, ny);
    }

    @Override
    public List<NTxPoint2D> vertices() {
        return new ArrayList<>(vertices);
    }
}