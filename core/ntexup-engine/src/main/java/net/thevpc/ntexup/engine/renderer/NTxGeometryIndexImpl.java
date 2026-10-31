package net.thevpc.ntexup.engine.renderer;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.renderer.NTxPlaced;

import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NTxGeometryIndexImpl implements net.thevpc.ntexup.api.renderer.NTxGeometryIndex {
    private static class Frame {
        final NTxNode node;
        final AffineTransform transform;
        final String name;
        NTxPlaced placed;
        Frame(NTxNode node, AffineTransform transform, String name) {
            this.node = node;
            this.transform = transform;
            this.name = name;
        }
    }

    private final List<Frame> stack = new ArrayList<>();
    private final List<NTxPlaced> placedList = new ArrayList<>();
    private final Map<NTxNode, NTxPlaced> byNode = new IdentityHashMap<>();
    private final Map<NTxNode, NTxNode[]> nodeAncestors = new IdentityHashMap<>();
    private final Map<NTxNode, String> nodeNames = new IdentityHashMap<>();

    private static final java.util.Set<String> NON_OBSTACLE_TYPES = new java.util.HashSet<>(java.util.Arrays.asList(
            "line", "polyline", "quad-curve", "cubic-curve", "arc", "connector"
    ));

    @Override
    public void enter(NTxNode node, NTxBounds2D selfBounds, double angleRad, double pivotX, double pivotY, String nameOrNull) {
        AffineTransform parent = stack.isEmpty() ? new AffineTransform() : stack.get(stack.size() - 1).transform;
        AffineTransform m;
        if (angleRad == 0) {
            m = parent;
        } else {
            m = new AffineTransform(parent);
            m.rotate(angleRad, pivotX, pivotY);
        }

        // record ancestors (root..parent) for this node
        NTxNode[] ancestors = new NTxNode[stack.size()];
        for (int i = 0; i < stack.size(); i++) {
            ancestors[i] = stack.get(i).node;
        }
        nodeAncestors.put(node, ancestors);
        if (nameOrNull != null) {
            nodeNames.put(node, nameOrNull);
        }

        Frame f = new Frame(node, m, nameOrNull);
        stack.add(f);

        if (nameOrNull != null) {
            NTxPlaced existing = byNode.get(node);
            if (existing == null || !samePlacement(existing, selfBounds, m)) {
                boolean container = isContainer(node);
                boolean layoutNone = NTxValueByName.isLayoutNone(node, null);
                boolean nonObs = NON_OBSTACLE_TYPES.contains(node.type());
                boolean obstacle = !(nonObs || container || layoutNone);
                NTxPlaced p = new NTxPlaced(node, nameOrNull, selfBounds, m, ancestors, false, container, obstacle);
                byNode.put(node, p);
                placedList.add(p);
                f.placed = p;
            } else {
                f.placed = existing;
            }
        }
    }

    private boolean samePlacement(NTxPlaced p, NTxBounds2D bounds, AffineTransform transform) {
        return Objects.equals(p.bounds.minX(), bounds.minX())
                && Objects.equals(p.bounds.minY(), bounds.minY())
                && Objects.equals(p.bounds.widthX(), bounds.widthX())
                && Objects.equals(p.bounds.widthY(), bounds.widthY())
                && p.toRoot.equals(transform);
    }

    @Override
    public void exit(NTxNode node) {
        if (!stack.isEmpty() && stack.get(stack.size() - 1).node == node) {
            stack.remove(stack.size() - 1);
        } else {
            // search
            for (int i = stack.size() - 1; i >= 0; i--) {
                if (stack.get(i).node == node) {
                    stack.remove(i);
                    break;
                }
            }
        }
    }

    @Override
    public void markHidden(NTxNode node) {
        for (NTxPlaced p : placedList) {
            if (p.node == node) {
                p.hidden = true;
            }
        }
    }

    @Override
    public net.thevpc.nuts.util.NOptional<NTxPlaced> resolve(NTxNode from, String ref) {
        if (ref == null) {
            return net.thevpc.nuts.util.NOptional.ofNamedEmpty("ref");
        }
        String[] parts = ref.split("\\.", -1);
        String targetName = parts[parts.length - 1].trim();
        if (targetName.isEmpty()) {
            return net.thevpc.nuts.util.NOptional.ofNamedEmpty("ref");
        }

        List<NTxPlaced> candidates = new ArrayList<>();
        for (NTxPlaced p : placedList) {
            if (targetName.equals(p.name)) {
                candidates.add(p);
            }
        }
        if (parts.length > 1) {
            // require preceding segments as ancestor names
            List<String> segs = new ArrayList<>();
            for (int i = 0; i < parts.length - 1; i++) {
                if (!parts[i].trim().isEmpty()) segs.add(parts[i].trim());
            }
            List<NTxPlaced> filtered = new ArrayList<>();
            for (NTxPlaced p : candidates) {
                if (hasAncestorSegments(p, segs)) {
                    filtered.add(p);
                }
            }
            candidates = filtered;
        }

        if (candidates.isEmpty()) {
            return net.thevpc.nuts.util.NOptional.ofNamedEmpty("geometry");
        }

        NTxNode[] fromAncestors = nodeAncestors.get(from);
        if (fromAncestors == null) {
            fromAncestors = new NTxNode[0];
        }

        NTxPlaced best = null;
        int bestScore = -1;
        for (NTxPlaced c : candidates) {
            // candidates already in placedList order? candidates built by iterating placedList, yes.
            int score = commonPrefixLength(c.ancestors, fromAncestors);
            if (score >= bestScore) {
                bestScore = score;
                best = c;
            }
        }
        return net.thevpc.nuts.util.NOptional.of(best);
    }

    private boolean hasAncestorSegments(NTxPlaced p, List<String> segs) {
        // require all segments appear in ancestors in order
        int si = 0;
        for (NTxNode a : p.ancestors) {
            if (si < segs.size()) {
                String n = nodeNames.get(a);
                if (n == null) {
                    n = simpleName(a);
                }
                if (n != null && n.equals(segs.get(si))) {
                    si++;
                }
            }
        }
        return si == segs.size();
    }

    private String simpleName(NTxNode node) {
        try {
            net.thevpc.nuts.util.NOptional<net.thevpc.ntexup.api.document.style.NTxProp> p = node.getProperty(NTxPropName.NAME);
            if (p.isPresent()) {
                net.thevpc.nuts.elem.NElement v = p.get().getValue();
                if (v != null) {
                    String s = v.asStringValue().orNull();
                    if (s != null) return s.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private int commonPrefixLength(NTxNode[] a, NTxNode[] b) {
        int n = Math.min(a.length, b.length);
        int i = 0;
        while (i < n) {
            if (a[i] == b[i]) {
                i++;
            } else if (a[i] != null && b[i] != null && a[i].uuid() != null && a[i].uuid().equals(b[i].uuid())) {
                i++;
            } else {
                break;
            }
        }
        return i;
    }

    @Override
    public List<NTxPlaced> placed() {
        return new ArrayList<>(placedList);
    }

    @Override
    public AffineTransform currentToRoot() {
        if (stack.size() <= 1) {
            return new AffineTransform();
        }
        return new AffineTransform(stack.get(stack.size() - 2).transform);
    }

    private boolean isContainer(NTxNode node) {
        String t = node.type();
        return t.equals("group") || t.equals("block") || t.equals("fragment") || t.equals("page") || t.equals("page-group") || t.equals("container") || t.equals("ul") || t.equals("grid") || t.equals("row") || t.equals("column");
    }
}