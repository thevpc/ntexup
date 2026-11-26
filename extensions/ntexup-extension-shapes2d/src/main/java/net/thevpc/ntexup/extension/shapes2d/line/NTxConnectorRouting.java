package net.thevpc.ntexup.extension.shapes2d.line;

import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class NTxConnectorRouting {
    public static final double DEFAULT_BEND_RADIUS = 12.0;
    private NTxConnectorRouting() {    }

    public static List<NTxPoint2D> route(String route, List<NTxPoint2D> points, double bendRadius) {
        if (points == null || points.size() < 3) {
            return points == null ? Collections.emptyList() : new ArrayList<>(points);
        }
        String value = route == null ? "polyline" : route.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty() || isStraight(value)) {
            return new ArrayList<>(points);
        }
        List<NTxPoint2D> unique = new ArrayList<>();
        for (NTxPoint2D point : points) {
            if (unique.isEmpty() || distance(unique.get(unique.size() - 1), point) > 1e-9) {
                unique.add(point);
            }
        }
        if (unique.size() < 3) {
            return unique;
        }
        double radius = Double.isFinite(bendRadius) && bendRadius > 0 ? bendRadius : DEFAULT_BEND_RADIUS;
        List<NTxPoint2D> orthogonal = isOrthogonal(value)
                ? orthogonalize(unique)
                : new ArrayList<>(unique);
        return isOrthogonal(value) ? orthogonal : isRounded(value) ? round(orthogonal, radius) : orthogonal;
    }

    private static boolean isRounded(String route) {
        return "rounded".equals(route) || "curve".equals(route) || "curved".equals(route);
    }

    public static boolean isSupported(String route) {
        if (route == null || route.trim().isEmpty()) {
            return true;
        }
        String value = route.trim().toLowerCase(Locale.ROOT);
        return isStraight(value) || isOrthogonal(value) || isRounded(value);
    }

    public static boolean isStraight(String route) {
        return "straight".equals(route) || "direct".equals(route)
                || "line".equals(route) || "none".equals(route) || "auto".equals(route)
                || "polyline".equals(route) || "sharp".equals(route);
    }

    private static boolean isOrthogonal(String route) {
        return "orthogonal".equals(route) || "ortho".equals(route)
                || "right-angle".equals(route) || "elbow".equals(route);
    }

    private static List<NTxPoint2D> orthogonalize(List<NTxPoint2D> source) {
        List<NTxPoint2D> result = new ArrayList<>();
        result.add(source.get(0));
        for (int i = 1; i < source.size(); i++) {
            NTxPoint2D current = result.get(result.size() - 1);
            NTxPoint2D target = source.get(i);
            if (Math.abs(current.x - target.x) > 1e-9) {
                addUnique(result, new NTxPoint2D(target.x, current.y));
            }
            if (Math.abs(current.y - target.y) > 1e-9) {
                addUnique(result, target);
            }
        }
        return result;
    }

    private static List<NTxPoint2D> round(List<NTxPoint2D> source, double radius) {
        List<Integer> corners = new ArrayList<>();
        for (int i = 1; i < source.size() - 1; i++) {
            NTxPoint2D previous = source.get(i - 1);
            NTxPoint2D current = source.get(i);
            NTxPoint2D next = source.get(i + 1);
            NTxPoint2D inDirection = unit(current.x - previous.x, current.y - previous.y);
            NTxPoint2D outDirection = unit(next.x - current.x, next.y - current.y);
            double dot = clamp(inDirection.x * outDirection.x + inDirection.y * outDirection.y, -1, 1);
            if (dot < 1 - 1e-9) {
                corners.add(i);
            }
        }
        List<NTxPoint2D> result = new ArrayList<>();
        result.add(source.get(0));
        for (int i : corners) {
            NTxPoint2D previous = source.get(i - 1);
            NTxPoint2D current = source.get(i);
            NTxPoint2D next = source.get(i + 1);
            double incoming = distance(previous, current);
            double outgoing = distance(current, next);
            NTxPoint2D inDirection = unit(current.x - previous.x, current.y - previous.y);
            NTxPoint2D outDirection = unit(next.x - current.x, next.y - current.y);
            double dot = clamp(inDirection.x * outDirection.x + inDirection.y * outDirection.y, -1, 1);
            double trim = Math.min(radius * Math.tan(Math.acos(dot) / 2),
                    Math.min(incoming, outgoing) / 2);
            NTxPoint2D entry = new NTxPoint2D(current.x - inDirection.x * trim, current.y - inDirection.y * trim);
            NTxPoint2D exit = new NTxPoint2D(current.x + outDirection.x * trim, current.y + outDirection.y * trim);
            if (distance(result.get(result.size() - 1), entry) > 1e-9) {
                result.add(entry);
            }
            NTxPoint2D center = circleCenter(entry, current, exit);
            if (center == null) {
                addUnique(result, current);
            } else {
                double startAngle = Math.atan2(entry.y - center.y, entry.x - center.x);
                double endAngle = Math.atan2(exit.y - center.y, exit.x - center.x);
                double sweep = endAngle - startAngle;
                if (sweep <= 0) {
                    sweep += Math.PI * 2;
                }
                if (sweep > Math.PI) {
                    sweep -= Math.PI * 2;
                }
                appendArc(result, center, distance(center, entry), startAngle, sweep);
            }
        }
        addUnique(result, source.get(source.size() - 1));
        return result;
    }

    private static NTxPoint2D circleCenter(NTxPoint2D entry, NTxPoint2D corner, NTxPoint2D exit) {
        double ax = corner.x - entry.x;
        double ay = corner.y - entry.y;
        double bx = exit.x - corner.x;
        double by = exit.y - corner.y;
        double cross = ax * by - ay * bx;
        if (Math.abs(cross) <= 1e-12) {
            return null;
        }
        double factor = (ax * bx + ay * by + bx * bx + by * by) / cross;
        return new NTxPoint2D(entry.x - ay * factor, entry.y + ax * factor);
    }

    private static void appendArc(List<NTxPoint2D> result, NTxPoint2D center, double radius,
                                  double startAngle, double sweep) {
        int segments = Math.max(4, (int) Math.ceil(Math.abs(sweep) * 12));
        for (int i = 1; i <= segments; i++) {
            double angle = startAngle + sweep * i / segments;
            result.add(new NTxPoint2D(center.x + radius * Math.cos(angle), center.y + radius * Math.sin(angle)));
        }
    }

    private static NTxPoint2D unit(double x, double y) {
        double length = Math.hypot(x, y);
        return new NTxPoint2D(x / length, y / length);
    }

    private static double distance(NTxPoint2D a, NTxPoint2D b) {
        return Math.hypot(b.x - a.x, b.y - a.y);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void addUnique(List<NTxPoint2D> points, NTxPoint2D point) {
        if (points.isEmpty() || distance(points.get(points.size() - 1), point) > 1e-9) {
            points.add(point);
        }
    }
}
