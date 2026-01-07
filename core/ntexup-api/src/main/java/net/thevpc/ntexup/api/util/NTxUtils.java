package net.thevpc.ntexup.api.util;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.elem2d.NTxPoint2D;
import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxItemList;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.renderer.text.NTxTextOptions;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class NTxUtils {

    public static final String COMPONENT_BODY_VAR_NAME = "componentBody";
    public static final String COMPILER_DECLARATION_PATH = "CompilerDeclarationPath";

    public static boolean hasCompilerDeclarationPath(NElement element) {
        return element.annotations().stream().anyMatch(x -> x.name().equals(COMPILER_DECLARATION_PATH));
    }

    public static NOptional<String> findCompilerDeclarationPath(NElement element) {
        Optional<String> e = element.annotations().stream().filter(x -> COMPILER_DECLARATION_PATH.equals(x.name())).flatMap(x -> x.params().get(0).asStringValue().stream().stream()).findFirst();
        return NOptional.ofOptional(e, NMsg.ofC("Missing CompilerDeclarationPath in %s", element));
    }

    public static NTxPoint2D point2DasRelative(NTxPoint2D a, NTxSizeRef sr) {
        return a == null ? null : new NTxPoint2D(
                a.getX() / 100.0 * sr.getParentWidth(),
                a.getY() / 100 * sr.getParentHeight()
        );
    }
    /**
     * Generates points along an elliptical arc.
     *
     * @param minX      bounding box left
     * @param minY      bounding box top
     * @param maxX      bounding box right
     * @param maxY      bounding box bottom
     * @param startDeg  start angle in degrees (0° = right, increasing counterclockwise)
     * @param endDeg    end angle in degrees
     * @param steps     number of points along the curve
     * @return          list of points along the arc
     */
    public static List<NTxPoint2D> createArc(double minX, double minY, double maxX, double maxY,
                                          double startDeg, double endDeg, int steps) {
        List<NTxPoint2D> points = new ArrayList<>();
        double cx = (minX + maxX) / 2.0;
        double cy = (minY + maxY) / 2.0;
        double rx = (maxX - minX) / 2.0;
        double ry = (maxY - minY) / 2.0;

        double startRad = Math.toRadians(startDeg);
        double endRad = Math.toRadians(endDeg);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps; // 0 → 1
            double angle = startRad + t * (endRad - startRad);
            double x = cx + rx * Math.cos(angle);
            double y = cy - ry * Math.sin(angle); // Y down
            points.add(new NTxPoint2D(x, y));
        }

        return points;
    }
    public static List<NTxPoint2D> createArc2(double cx, double cy, double rx, double ry,
                                          double startDeg, double endDeg, int steps) {
        List<NTxPoint2D> points = new ArrayList<>();
        double startRad = Math.toRadians(startDeg);
        double endRad = Math.toRadians(endDeg);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps; // 0 → 1
            double angle = startRad + t * (endRad - startRad);
            double x = cx + rx * Math.cos(angle);
            double y = cy - ry * Math.sin(angle); // Y down
            points.add(new NTxPoint2D(x, y));
        }

        return points;
    }
    public static NElement addCompilerDeclarationPath(NElement elem, NTxSource resource) {
        if (resource != null) {
            NPath nPath = resource.path().orNull();
            if (nPath != null) {
                NOptional<String> o = findCompilerDeclarationPath(elem);
                if (o.isPresent()) {
                    return elem;
                }
                return addCompilerDeclarationPath(elem, nPath.toString());
            }
        }
        return elem;
    }

    public static NElement addCompilerDeclarationPathDummy(NElement element) {
        NOptional<String> o = findCompilerDeclarationPath(element);
        if (o.isPresent()) {
            return element;
        }
        return element.builder().addAnnotation(COMPILER_DECLARATION_PATH, NElement.ofString("")).build();
    }

    public static NElement addCompilerDeclarationPath(NElement element, String path) {
        NOptional<String> o = findCompilerDeclarationPath(element);
        if (o.isPresent()) {
            return element;
        }
        //if (element.isString()) {
            return element.builder().addAnnotation(COMPILER_DECLARATION_PATH, NElement.ofString(path)).build();
        //}
        //return element;
    }

    public static List<NTxNode> nodePath(NTxNode node) {
        List<NTxNode> all = new ArrayList<>();
        NTxItem i = node;
        while (i != null) {
            if (i instanceof NTxItem) {
                all.add(0, (NTxNode) i);
            }
            i = i.parent();
        }
        return all;
    }

    public static NTxNode firstNodeUp(NTxItem item) {
        while (item != null) {
            if (item instanceof NTxNode) {
                return (NTxNode) item;
            }
            item = item.parent();
        }
        return null;
    }

    public static final double[] dtimes(double min, double max, int times) {
        double[] d = new double[times];
        if (times == 1) {
            d[0] = min;
        } else {
            double step = (max - min) / (times - 1);
            for (int i = 0; i < d.length; i++) {
                d[i] = min + i * step;
            }
        }
        return d;
    }

    public static String getCompilerDeclarationPath(NElement element) {
        return element.annotations().stream().filter(a -> a.name().equals(COMPILER_DECLARATION_PATH)).findFirst().map(x -> x.param(0).asStringValue().get()).orElse(null);

    }


    public static NPath resolvePath(NElement path, Object source) {
        if (NBlankable.isBlank(path)) {
            return null;
        }
        String pathString = path.asStringValue().get();
        if (NBlankable.isBlank(pathString)) {
            return null;
        }
        String compilerDeclarationPath = getCompilerDeclarationPath(path);
        NPath referencePath = null;
        if (compilerDeclarationPath != null) {
            referencePath = NPath.of(compilerDeclarationPath);
        } else {
            if (source != null) {
                if (source instanceof NPath) {
                    referencePath = (NPath) source;
                } else if (source instanceof NTxSource) {
                    referencePath = ((NTxSource) source).path().orNull();
                }
            }
        }
        NPath base;
        if (referencePath != null) {
            if (referencePath.isRegularFile()) {
                referencePath = referencePath.getParent();
            }
            if (referencePath != null) {
                base = referencePath.resolve(pathString);
            } else {
                base = NPath.of(pathString);
            }
        } else {
            base = NPath.of(pathString);
        }
        return base;
    }

    public static String shortName(NTxSource a) {
        if (a == null) {
            return null;
        }
        return a.shortName();
    }

    public static String strSnapshot(Object a) {
        if (a == null) {
            return "null";
        }
        ;
        for (String s : a.toString().split("\n")) {
            s = s.trim();
            if (!s.isEmpty()) {
                return s + "...";
            }
        }
        return "";
    }

    public static Double min(Double a, Double b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (a < b) {
            return a;
        }
        return b;
    }

    public static Double max(Double a, Double b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (a < b) {
            return b;
        }
        return a;
    }

    public static double doubleOf(Number n) {
        if (n == null) {
            return 0;
        }
        return n.doubleValue();
    }

    public static int intOf(Number n) {
        if (n == null) {
            return 0;
        }
        return n.intValue();
    }

    public static NElement toElement(
            Object o
    ) {
        if (o == null) {
            return NElement.ofNull();
        }
        if (o instanceof String) {
            return NElement.ofString((String) o);
        }
        if (o instanceof Double) {
            return NElement.ofDouble((Double) o);
        }
        if (o instanceof Integer) {
            return NElement.ofInt((Integer) o);
        }
        if (o instanceof Boolean) {
            return NElement.ofBoolean((Boolean) o);
        }
        if (o instanceof NElement) {
            return (NElement) o;
        }
        if (o instanceof Point2D.Double) {
            return NElement.ofUplet(
                    toElement(((Point2D.Double) o).getX()),
                    toElement(((Point2D.Double) o).getY())
            );
        }
        if (o instanceof NTxBounds2) {
            NTxBounds2 oo = (NTxBounds2) o;
            return NElement.ofUplet(
                    NElement.ofDouble(oo.getX()),
                    NElement.ofDouble(oo.getY()),
                    NElement.ofDouble(oo.getWidth()),
                    NElement.ofDouble(oo.getHeight())
            );
        }
        if (o instanceof NToElement) {
            return ((NToElement) o).toElement();
        }
        if (o instanceof Enum) {
            return NElement.ofName(
                    NNameFormat.LOWER_KEBAB_CASE.format(((Enum<?>) o).name())
            );
        }
        if (o instanceof Color) {
            Color c = (Color) o;
            return NElement.ofString(String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()));
        }
        if (o.getClass().isArray()
        ) {
            if (o.getClass().getComponentType().isPrimitive()) {
                List<NElement> a = new ArrayList<>();
                int max = Array.getLength(o);
                for (int i = 0; i < max; i++) {
                    a.add(toElement(Array.get(o, i)));
                }
                return NElement.ofArray(a.toArray(new NElement[0]));
            } else {
                return
                        NElement.ofArray(
                                Arrays.stream((Object[]) o)
                                        .map(x -> toElement(x))
                                        .toArray(NElement[]::new)
                        );
            }
        }
        throw new IllegalArgumentException("Unsupported toElement(" + o.getClass().getName() + ")");
    }

    public static Object fromElement(NElement v) {
        if (v == null) {
            return null;
        }
        switch (v.type()) {
            case INT:
                return v.asIntValue().get();
            case DOUBLE_QUOTED_STRING:
            case SINGLE_QUOTED_STRING:
            case ANTI_QUOTED_STRING:
            case TRIPLE_DOUBLE_QUOTED_STRING:
            case TRIPLE_SINGLE_QUOTED_STRING:
            case TRIPLE_ANTI_QUOTED_STRING:
            case LINE_STRING:
                return v.asStringValue().get();
        }
        throw new IllegalArgumentException("unsupported yet : fromElement(" + v.type() + ")");
    }

    public static String[] uids(String[]... ids) {
        LinkedHashSet<String> all = new LinkedHashSet<>();
        if (ids != null) {
            for (String[] ids1 : ids) {
                if (ids1 != null) {
                    for (String s : ids1) {
                        s = NStringUtils.trimToNull(s);
                        if (s != null) {
                            all.add(uid(s));
                        }
                    }
                }
            }
        }
        return all.toArray(new String[0]);
    }

    public static String uid(String id) {
        return NNameFormat.LOWER_KEBAB_CASE.format(NStringUtils.trim(id));
    }

    public static Color paintAsColor(Paint paint) {
        if (paint instanceof Color) {
            return (Color) paint;
        }
        return null;
    }

    public static Paint resolveForegroundColor(NTxTextOptions options, NTxNode node, NTxNodeRendererContext ctx) {
        if (options.foregroundColorIndex != null) {
            return NTxColors.resolveDefaultColorByIndex(options.foregroundColorIndex, null, node, ctx);
        } else if (options.foregroundColor instanceof Color) {
            return options.foregroundColor;
        }
        return null;
    }

    public static boolean asBoolean(NElement e) {
        switch (e.type()) {
            case BOOLEAN:
                return e.asBooleanValue().get();
            default: {
                if (e.isNumber()) {
                    return e.asDoubleValue().get().doubleValue() != 0.0;
                }
                return !e.isNull();
            }
        }
    }

    public static List<Object> asListOfObjects(Object anyVal) {
        if (anyVal instanceof Collection) {
            return new ArrayList<>((Collection) anyVal);
        }
        if (anyVal instanceof Object[]) {
            return new ArrayList<>(Arrays.asList((Object[]) anyVal));
        }
        if (anyVal == null) {
            return new ArrayList<>();
        }
        if (anyVal.getClass().isArray()) {
            List<Object> a = new ArrayList<>();
            int max = Array.getLength(anyVal);
            for (int i = 0; i < max; i++) {
                a.add(Array.get(anyVal, i));
            }
            return a;
        }
        return Arrays.asList(anyVal);
    }

    public static Map<String, NElement> inheritedVarsMap(NTxItem c) {
        Map<String, NElement> r = new HashMap<>();
        NTxItem i = c;
        while (i != null) {
            if (i instanceof NTxNode) {
                for (Map.Entry<String, NElement> e : ((NTxNode) i).getVars().entrySet()) {
                    String k = e.getKey();
                    if (!r.containsKey(k)) {
                        r.put(k, e.getValue());
                    }
                }
            }
            i = i.parent();
        }
        return r;
    }

    public static NTxSource sourceOf(NTxItem node) {
        while (node != null) {
            NTxSource s = node.source();
            if (s != null) {
                return s;
            }
            node = node.parent();
        }
        return null;
    }

    public static NElement removeCompilerDeclarationPathAnnotations(NElement yy) {
        return yy.transform(new NElementTransform() {
            @Override
            public NElement[] preTransform(NElement element) {
                List<NElementAnnotation> oldAnn = element.annotations();
                List<NElementAnnotation> a = oldAnn.stream().filter(x -> !COMPILER_DECLARATION_PATH.equals(x.name())).collect(Collectors.toList());
                if (a.size() != oldAnn.size()) {
                    NElementBuilder b = element.builder().clearAnnotations().addAnnotations(a);
                    return new NElement[]{b.build()};
                }
                return new NElement[]{element};
            }
        })[0];
    }

    public static NElement addCompilerDeclarationPathAnnotations(NElement yy, String source) {
        if (NBlankable.isBlank(source)) {
            return yy;
        }
        return yy.transform(new NElementTransform() {
            @Override
            public NElement[] postTransform(NElement element) {
                if (element.isString()) {
                    List<NElementAnnotation> oldAnn = element.annotations();
                    List<NElementAnnotation> a = oldAnn.stream().filter(x -> !COMPILER_DECLARATION_PATH.equals(x.name())).collect(Collectors.toList());
                    if (a.size() == oldAnn.size()) {
                        NElementBuilder b = element.builder().addAnnotation(COMPILER_DECLARATION_PATH, NElement.ofString(source)).addAnnotations(a);
                        return new NElement[]{b.build()};
                    }
                }
                return new NElement[]{element};
            }
        })[0];
    }

    public static String snippet(NElement yy) {
        if (yy == null) {
            return "null";
        }
        yy = removeCompilerDeclarationPathAnnotations(yy);
        return yy.snippet();
    }


    public static void setNodeParent(NTxItem item, NTxNode parent) {
        if (item instanceof NTxNode) {
            ((NTxNode) item).setParent(parent);
        } else if (item instanceof NTxItemList) {
            for (NTxItem nTxItem : ((NTxItemList) item).getItems()) {
                setNodeParent(nTxItem, parent);
            }
        }
    }

    public static String snippet(NTxNode node) {
        if (node == null) {
            return "null";
        }
        int size = 100;
        String s = node.toString();
        int u = s.indexOf("\n");
        boolean truncated = false;
        if (u >= 0) {
            s = s.substring(0, u);
            truncated = true;
        }
        if (s.length() > size) {
            s = s.substring(0, size);
            truncated = true;
        }
        if (truncated) {
            return s + "...";
        }
        return s;
    }

    public static void checkNode(NTxNode node) {
        checkNode(node, true);
    }

    public static void checkNode(NTxNode node, boolean recursive) {
        checkNode(node, (NTxNode) (node.parent()), recursive);

    }

    public static void checkNode(NTxNode node, NTxNode expectedRoot) {
        checkNode(node, expectedRoot, true);
    }

    public static void checkNode(NTxNode node, NTxNode expectedRoot, boolean recursive) {
//        NAssert.requireEquals(node.parent(), expectedRoot, "root for " + snippet(node));
//        NAssert.requireNonNull(node.type(), "type for " + snippet(node));
//        NAssert.requireNonNull(node.source(), "source for " + snippet(node));
//        NAssert.requireNonNull(node.uuid(), "uuid for " + snippet(node));
//        NAssert.requireNonNull(node.uuid(), "uuid for " + snippet(node));
//        for (NTxNode child : node.children()) {
//            checkNode(child, node, recursive);
//        }
    }

    public static boolean isAnyDefVarName(String name) {
        return NNameFormat.equalsIgnoreFormat(COMPONENT_BODY_VAR_NAME, name)
                ;
    }

    public static NTxNode findRootNode(NTxItem bullet) {
        NTxItem a = bullet;
        NTxNode r = null;
        while (a != null) {
            if (a instanceof NTxNode) {
                r = (NTxNode) a;
            }
            a = a.parent();
        }
        return r;
    }

}
