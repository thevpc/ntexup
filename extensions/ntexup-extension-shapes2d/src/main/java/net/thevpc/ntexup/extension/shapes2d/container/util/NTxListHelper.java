package net.thevpc.ntexup.extension.shapes2d.container.util;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxMargin;
import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.DefaultNTxNodeSelector;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxPropName;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.document.style.NTxStyleRuleSelectorItem;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxValue;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.eval.NTxValueByType;
import net.thevpc.ntexup.api.parser.NTxArgumentReader;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxSizeRef;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NNumberElement;
import net.thevpc.nuts.elem.NPairElement;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Shared layout logic of {@code ul}/{@code ol} nodes.
 *
 * <p>Bullets are generated nodes (they are not part of the source tree) that are laid out in a
 * dedicated column to the left of the item content. The appearance of a bullet can be
 * customized per list and per nesting level:</p>
 *
 * <ul>
 *     <li>{@code bullet-shape} / {@code bullet-shape-<level>}: {@code circle}, {@code square},
 *     {@code rectangle}, {@code round-rectangle}, {@code triangle}, {@code diamond},
 *     {@code hexagon}, {@code pentagon}, {@code octagon}, {@code image} or {@code none}</li>
 *     <li>{@code bullet-image} / {@code bullet-image-<level>}: image used by the {@code image} shape</li>
 *     <li>{@code bullet-size} / {@code bullet-size-<level>}: bullet size, as a ratio of the font
 *     size of the item ({@code 0.4}, {@code 0.4em}, {@code 40%}) or with an absolute unit
 *     ({@code 2%P}, {@code 12px})</li>
 *     <li>{@code bullet-color} / {@code bullet-color-<level>}: bullet color, any color expression</li>
 *     <li>{@code bullet-align}: {@code center} (default) or {@code top}</li>
 *     <li>{@code numbering} / {@code numbering-<level>}: ordered marker modes, eg {@code ["1","a","i"]}</li>
 *     <li>{@code numbering-parent}: include the parent index, eg {@code 1.1}</li>
 *     <li>{@code distribute}: spread items over the available height</li>
 * </ul>
 *
 * <p>Whatever is not specified is defaulted by this class, so that a plain list already looks
 * good. Theme classes ({@code ul-bullet}, {@code ul-bullet-<level>}, {@code ol-bullet}, ...) and
 * item classes ({@code ul-item}, {@code ul-item-<level>}, ...) always take precedence over these
 * defaults: a default is only applied for properties the theme did not define.</p>
 *
 * @author vpc
 */
public class NTxListHelper {

    // ------------------------------------------------------------------------
    // list properties
    // ------------------------------------------------------------------------

    /** shape of the bullet marker, {@code circle} by default */
    public static final String PROP_BULLET_SHAPE = NTxPropName.BULLET_SHAPE;
    /** image used when the bullet shape is {@code image} */
    public static final String PROP_BULLET_IMAGE = NTxPropName.BULLET_IMAGE;
    /** bullet size, a ratio of the font size unless an absolute unit is used */
    public static final String PROP_BULLET_SIZE = NTxPropName.BULLET_SIZE;
    /** bullet color, any color expression */
    public static final String PROP_BULLET_COLOR = NTxPropName.BULLET_COLOR;
    /** {@code center} (default) or {@code top} */
    public static final String PROP_BULLET_ALIGN = NTxPropName.BULLET_ALIGN;
    /** ordered list marker modes, eg {@code "1.a.i"} or {@code ["1","a","i"]} */
    public static final String PROP_NUMBERING = NTxPropName.NUMBERING;
    /** prefix nested markers with the parent index, eg {@code 1.1} */
    public static final String PROP_NUMBERING_PARENT = NTxPropName.NUMBERING_PARENT;
    /** spread items over the available height */
    public static final String PROP_DISTRIBUTE = NTxPropName.DISTRIBUTE;

    /** properties accepted as parameters of a list node, whatever their level suffix */
    private static final List<String> LIST_PROPS = Arrays.asList(
            PROP_BULLET_SHAPE,
            PROP_BULLET_IMAGE,
            PROP_BULLET_SIZE,
            PROP_BULLET_COLOR,
            PROP_BULLET_ALIGN,
            PROP_NUMBERING,
            PROP_NUMBERING_PARENT,
            PROP_DISTRIBUTE);

    // ------------------------------------------------------------------------
    // defaults
    // ------------------------------------------------------------------------

    /**
     * Bullet shape used when nothing is specified. The same shape is used at every level (a
     * round dot is the sleekest and most neutral default), only the size decreases with depth.
     * Use {@code bullet-shape-<level>} to vary the shape per level.
     */
    private static final String[] DEFAULT_SHAPES = {"circle"};
    /** default bullet size per level, as a ratio of the font size */
    private static final double[] DEFAULT_SIZE_RATIOS = {0.32, 0.27, 0.24};
    /** default ordered list marker modes, per level */
    private static final String DEFAULT_NUMBERING = "V1a1";
    /** corner radius of a round-rectangle bullet, as a ratio of its size */
    private static final double ROUND_CORNER_RATIO = 0.42;
    /** a bullet is never smaller than this ratio of the font size */
    private static final double MIN_BULLET_SIZE_RATIO = 0.05;
    /** a font relative bullet size never exceeds this ratio of the font size */
    private static final double MAX_BULLET_SIZE_RATIO = 1.0;
    /** fallback color when no color can be resolved at all */
    private static final Color FALLBACK_BULLET_COLOR = new Color(0x33, 0x33, 0x33);

    private static final String ALIGN_CENTER = "center";
    private static final String ALIGN_TOP = "top";

    /** already reported warnings, to avoid flooding the log with duplicates */
    private static final Set<String> WARNED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** {@code -Dntexup.list.debug=true} dumps list metrics to the log */
    private static final boolean DEBUG = Boolean.getBoolean("ntexup.list.debug");

    private NTxListHelper() {
    }

    // ------------------------------------------------------------------------
    // parameter parsing
    // ------------------------------------------------------------------------

    /**
     * Accepts list properties such as {@code bullet-shape} and their per level variants
     * ({@code bullet-shape-2}). Unknown properties are rejected so that typos are reported.
     */
    public static boolean parseListParams(NTxArgumentReader info, NTxNodeBuilderContext buildContext) {
        NElement e = info.peek();
        if (e == null) {
            return false;
        }
        if (e.isNamedPair()) {
            NPairElement pair = e.asPair().get();
            String uid = uid(pair.key());
            if (isListProperty(uid)) {
                info.node().setProperty(uid, NTxUtils.addCompilerDeclarationPath(pair.value(), info.source()));
                info.read();
                return true;
            }
        } else if (e.isName()) {
            String uid = uid(e);
            if (isListProperty(uid)) {
                info.node().setProperty(uid, NElement.ofTrue());
                info.read();
                return true;
            }
        }
        return false;
    }

    private static boolean isListProperty(String uid) {
        if (uid == null || uid.isEmpty()) {
            return false;
        }
        int level = uid.lastIndexOf('-');
        String base = level < 0 ? uid : uid.substring(0, level);
        if (level >= 0 && !isLevelSuffix(uid.substring(level + 1))) {
            base = uid;
        }
        return LIST_PROPS.contains(base);
    }

    private static boolean isLevelSuffix(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** {@code bullet-shape-2} for level 2, {@code bullet-shape} when level is not positive */
    static String levelProp(String base, int level) {
        return level > 0 ? base + "-" + level : base;
    }

    private static String uid(NElement e) {
        if (e == null) {
            return null;
        }
        NOptional<String> s = e.asStringValue();
        if (!s.isPresent() || s.get() == null) {
            return null;
        }
        return NTxUtils.uid(s.get());
    }

    // ------------------------------------------------------------------------
    // tree flattening
    // ------------------------------------------------------------------------

    public static List<NodeWithIndent> build(NTxNode p, boolean ordered, NTxRendererContext ctx) {
        List<NodeWithIndent> all = new ArrayList<>();
        if (p == null) {
            return all;
        }
        fillList(p, all, 0, ordered, "", ctx);
        if (!all.isEmpty()) {
            layout(ordered, ctx, all);
        }
        return all;
    }

    private static boolean isList(NTxNode p) {
        if (p == null) {
            return false;
        }
        switch (p.type()) {
            case NTxNodeType.UNORDERED_LIST:
            case NTxNodeType.ORDERED_LIST: {
                return true;
            }
        }
        return false;
    }

    private static boolean isOrdered(NTxNode list) {
        return list != null && NTxNodeType.ORDERED_LIST.equals(list.type());
    }

    /**
     * A nested list starts a new level, and uses its own kind ({@code ul} inside an {@code ol}
     * gets bullets) as well as its own {@code numbering}.
     */
    private static void fillAny(NTxNode p, List<NodeWithIndent> all, int indent, boolean ordered, String parentString, int userIndex, NTxRendererContext ctx, NumberingStrategy ns) {
        if (p == null) {
            return;
        }
        if (isList(p)) {
            fillList(p, all, indent + 1, isOrdered(p), parentString, ctx);
        } else {
            fillNonList(p, all, indent, ordered, parentString, userIndex, ctx, ns);
        }
    }

    private static void fillList(NTxNode list, List<NodeWithIndent> all, int indent, boolean ordered, String parentString, NTxRendererContext ctx) {
        NumberingStrategy ns = resolveNumberingStrategy(list, ctx);
        int userIndex = 1;
        String lastParent = "";
        for (NTxNode child : list.children()) {
            if (isList(child)) {
                fillAny(child, all, indent, ordered, lastParent, userIndex, ctx, ns);
            } else {
                fillAny(child, all, indent, ordered, parentString, userIndex, ctx, ns);
                lastParent = ns.eval(parentString, userIndex, ns.includeParent, indent);
                userIndex++;
            }
        }
    }

    private static void fillNonList(NTxNode p, List<NodeWithIndent> all, int indent, boolean ordered, String parentString, int userIndex, NTxRendererContext ctx, NumberingStrategy ns) {
        NodeWithIndent z = new NodeWithIndent();
        z.indent = indent;
        z.child = p.addStyleClasses(cls("item", indent));
        z.bullet = createBullet(p, ordered, indent, parentString, userIndex, ctx, ns, z);
        all.add(z);
    }

    private static NTxNode createBullet(NTxNode item, boolean ordered, int indent, String parentString, int userIndex, NTxRendererContext ctx, NumberingStrategy ns, NodeWithIndent z) {
        NTxDocumentFactory f = ctx.documentFactory();
        int level = indent + 1;
        NTxNode bullet;
        if (ordered) {
            bullet = f.ofText(ns.eval(parentString, userIndex, ns.includeParent, indent));
        } else {
            BulletSpec spec = resolveSpec(ctx, level);
            z.spec = spec;
            if (spec.isNone()) {
                return null;
            }
            bullet = createShapeNode(f, spec, ctx, z);
        }
        if (bullet == null) {
            return null;
        }
        bullet.setSource(item.source());
        bullet.setParent(item.parent());
        String className = bulletClassName(ctx, item, indent, ordered);
        if (className != null) {
            bullet.addStyleClasses(className);
            z.bulletClassProps = declaredClassProps(ctx, item, className);
        }
        return bullet;
    }

    /**
     * Creates the node of a shape bullet. The geometry (size, placement, color) is applied later,
     * once the bullet box is known.
     */
    private static NTxNode createShapeNode(NTxDocumentFactory f, BulletSpec spec, NTxRendererContext ctx, NodeWithIndent z) {
        NTxEngine engine = ctx.engine();
        NTxNode node;
        switch (spec.shape) {
            case SHAPE_CIRCLE: {
                node = f.ofCircle();
                break;
            }
            case SHAPE_SQUARE: {
                node = f.ofSquare();
                break;
            }
            case SHAPE_RECTANGLE: {
                node = f.ofRectangle();
                break;
            }
            case SHAPE_ROUND_RECTANGLE: {
                node = f.ofRectangle();
                break;
            }
            case SHAPE_TRIANGLE: {
                node = f.ofTriangle();
                break;
            }
            case SHAPE_IMAGE: {
                node = f.ofImage();
                node.setProperty(NTxProp.of(NTxPropName.VALUE, spec.image));
                node.setProperty(NTxProp.ofBoolean(NTxPropName.PRESERVE_ASPECT_RATIO, true));
                break;
            }
            default: {
                //optional shapes, only available if the matching extension is loaded
                NOptional<net.thevpc.ntexup.api.parser.NTxNodeParser> parser = engine.nodeTypeParser(spec.shape);
                if (parser.isPresent()) {
                    node = f.of(spec.shape);
                } else {
                    warn(ctx, "unsupported bullet shape '%s', using '%s' instead. Available shapes: %s",
                            spec.shape, SHAPE_CIRCLE, availableShapes(ctx));
                    node = f.ofCircle();
                }
                break;
            }
        }
        if (node == null) {
            return null;
        }
        return node;
    }

    // ------------------------------------------------------------------------
    // bullet specification
    // ------------------------------------------------------------------------

    static final String SHAPE_CIRCLE = "circle";
    static final String SHAPE_SQUARE = "square";
    static final String SHAPE_RECTANGLE = "rectangle";
    static final String SHAPE_ROUND_RECTANGLE = "round-rectangle";
    static final String SHAPE_TRIANGLE = "triangle";
    static final String SHAPE_IMAGE = "image";
    static final String SHAPE_NONE = "none";

    /** every shape that can be used as a bullet marker */
    static final String[] SUPPORTED_SHAPES = {
            SHAPE_CIRCLE, "disc", "dot", "bullet",
            SHAPE_SQUARE,
            SHAPE_RECTANGLE, "rect", "bar",
            SHAPE_ROUND_RECTANGLE, "rounded-rectangle", "rounded", "roundrect",
            SHAPE_TRIANGLE,
            "diamond", "rhombus",
            "pentagon", "hexagon", "octagon", "decagon", "heptagon", "nonagon", "novagon",
            "trapezoid", "parallelogram", "ellipse", "arrow", "donut", "pie",
            SHAPE_IMAGE, "img", "picture",
            SHAPE_NONE, "hidden", ""};

    /**
     * Names of {@link #SUPPORTED_SHAPES} the current engine can really build. Shapes may live in
     * optional extensions and the set of aliases grows with them, so error messages only advertise
     * what is registered right now.
     */
    static String availableShapes(NTxRendererContext ctx) {
        NTxEngine engine = ctx.engine();
        List<String> found = new ArrayList<>();
        for (String s : SUPPORTED_SHAPES) {
            if (s == null || s.isEmpty() || found.contains(s)) {
                continue;
            }
            String canonical = normalizeShape(s);
            if (canonical == null) {
                continue;
            }
            if (SHAPE_NONE.equals(canonical)) {
                //always valid, needs no node type
                if (!found.contains(SHAPE_NONE)) {
                    found.add(SHAPE_NONE);
                }
                continue;
            }
            if (SHAPE_ROUND_RECTANGLE.equals(canonical)) {
                //a rectangle with rounded corners, no dedicated node type
                if (!found.contains(SHAPE_ROUND_RECTANGLE)) {
                    found.add(SHAPE_ROUND_RECTANGLE);
                }
                continue;
            }
            try {
                if (engine.nodeTypeParser(canonical).isPresent()) {
                    found.add(s);
                }
            } catch (Throwable t) {
                //not a shape name, skip it
            }
        }
        if (found.isEmpty()) {
            found.add(SHAPE_CIRCLE);
        }
        return String.join(", ", found);
    }

    static String normalizeShape(String shape) {
        if (shape == null) {
            return null;
        }
        String s = NTxUtils.uid(shape);
        switch (s) {
            case "disc":
            case "dot":
            case "bullet": {
                return SHAPE_CIRCLE;
            }
            case "rect": {
                return SHAPE_RECTANGLE;
            }
            case "bar": {
                return SHAPE_RECTANGLE;
            }
            case "rounded":
            case "rounded-rectangle":
            case "roundrect": {
                return SHAPE_ROUND_RECTANGLE;
            }
            case "rhombus": {
                return "diamond";
            }
            case "nonagon":
            case "novagon": {
                return "nonagon";
            }
            case "img": {
                return SHAPE_IMAGE;
            }
            case "picture": {
                return SHAPE_IMAGE;
            }
            case "hidden": {
                return SHAPE_NONE;
            }
            case "": {
                return null;
            }
        }
        return s;
    }

    /** appearance of a bullet marker, for one nesting level */
    static final class BulletSpec {
        /** nesting level, starting at 1 */
        int level;
        /** normalized shape, {@code null} for ordered lists */
        String shape;
        /** image of an {@code image} bullet */
        NElement image;
        /** explicit color, {@code null} to inherit the item color */
        NElement color;
        /** corner radius ratio of a round-rectangle bullet */
        double round = ROUND_CORNER_RATIO;
        /** unresolved size, {@code null} to use the default */
        NElement size;
        /** resolved size, in document units */
        double resolvedSize;

        boolean isNone() {
            return SHAPE_NONE.equals(shape);
        }

        boolean isRoundRectangle() {
            return SHAPE_ROUND_RECTANGLE.equals(shape);
        }
    }

    private static BulletSpec resolveSpec(NTxRendererContext ctx, int level) {
        BulletSpec spec = new BulletSpec();
        spec.level = level;
        String shape = firstNonBlank(
                getString(ctx, level, PROP_BULLET_SHAPE),
                getString(ctx, 0, PROP_BULLET_SHAPE));
        shape = normalizeShape(shape);
        if (shape == null) {
            shape = DEFAULT_SHAPES[(level - 1) % DEFAULT_SHAPES.length];
        }
        if (SHAPE_NONE.equals(shape)) {
            spec.shape = SHAPE_NONE;
            return spec;
        }
        spec.shape = shape;
        spec.size = getElement(ctx, level, PROP_BULLET_SIZE);
        if (spec.size == null) {
            spec.size = getElement(ctx, 0, PROP_BULLET_SIZE);
        }
        spec.color = getElement(ctx, level, PROP_BULLET_COLOR);
        if (spec.color == null) {
            spec.color = getElement(ctx, 0, PROP_BULLET_COLOR);
        }
        NElement image = getElement(ctx, level, PROP_BULLET_IMAGE);
        if (image == null) {
            image = getElement(ctx, 0, PROP_BULLET_IMAGE);
        }
        if (SHAPE_IMAGE.equals(shape)) {
            if (image == null) {
                warn(ctx, "bullet shape is 'image' but no '%s' is defined, using '%s' instead",
                        PROP_BULLET_IMAGE, SHAPE_CIRCLE);
                spec.shape = shape = SHAPE_CIRCLE;
            } else {
                spec.image = eval(ctx, image);
            }
        }
        if (SHAPE_RECTANGLE.equals(shape)) {
            //an explicit bullet-round overrides the default corner radius
            NElement round = getElement(ctx, level, "bullet-round");
            if (round == null) {
                round = getElement(ctx, 0, "bullet-round");
            }
            NOptional<Double> r = asDouble(round);
            if (r.isPresent()) {
                spec.round = Math.max(0, Math.min(0.5, r.get()));
            }
        }
        return spec;
    }

    private static String getString(NTxRendererContext ctx, int level, String base) {
        if (level > 0) {
            String s = NTxValueByType.getStringOrName(ctx, levelProp(base, level)).orNull();
            if (s != null) {
                return s;
            }
        }
        return NTxValueByType.getStringOrName(ctx, base).orNull();
    }

    private static NElement getElement(NTxRendererContext ctx, int level, String base) {
        NElement e = level > 0 ? ctx.computePropertyValue(levelProp(base, level)).orNull() : null;
        if (e == null) {
            e = ctx.computePropertyValue(base).orNull();
        }
        return e;
    }

    /** best effort evaluation of an expression, so that it can be reused in another node */
    private static NElement eval(NTxRendererContext ctx, NElement e) {
        if (e == null) {
            return null;
        }
        try {
            NElement v = ctx.evalExpression(e).orNull();
            return v == null ? e : v;
        } catch (Throwable t) {
            return e;
        }
    }

    private static NOptional<Double> asDouble(NElement e) {
        if (e == null) {
            return NOptional.ofNamedEmpty("no value");
        }
        if (e.isNumber()) {
            NNumberElement n = e.asNumber().get();
            return NOptional.of(n.numberValue().doubleValue());
        }
        return NTxValue.of(e).asDouble();
    }

    // ------------------------------------------------------------------------
    // layout
    // ------------------------------------------------------------------------

    private static void layout(boolean ordered, NTxRendererContext ctx, List<NodeWithIndent> all) {
        NTxSizeRef sizeRef = ctx.sizeRef();
        double rootWidth = sizeRef.getRootWidth();
        double rootHeight = sizeRef.getRootHeight();
        NTxBounds2D box = ctx.defaultSelfBounds2D();
        NTxMargin padding = NTxValueByName.getPadding(ctx);
        if (padding != null && !padding.isZero()) {
            box = NTxBounds2D.ofWidth(
                    box.minX() + padding.getLeft(),
                    box.minY() + padding.getTop(),
                    Math.max(0, box.widthX() - padding.getLeft() - padding.getRight()),
                    Math.max(0, box.widthY() - padding.getTop() - padding.getBottom()));
        }
        //the metrics used for the layout, as in previous versions of this helper
        double fontSize = fontSize(ctx, rootWidth);
        //the em size of the item text, in the same units as the bounds. This is the reference
        //of bullet sizes, so that "0.3em" really means 30% of the text size
        double emSize = emSize(ctx, fontSize);
        double indentFactor = Math.max(18, fontSize * 1.2);
        double marginWidth = Math.max(8, fontSize * 0.42);
        double itemGap = Math.max(4, fontSize * 0.25);
        String bulletAlign = NTxValueByType.getStringOrName(ctx, PROP_BULLET_ALIGN).orElse(ALIGN_CENTER);
        boolean bulletAlignTop = ALIGN_TOP.equalsIgnoreCase(String.valueOf(bulletAlign).trim());

        //1) resolve the appearance of every bullet, and measure the ordered markers
        double maxBulletSize = 0;
        double maxOrderedWidth = 0;
        for (NodeWithIndent item : all) {
            item.child.invalidateRenderCache();
            if (item.bullet != null) {
                item.bullet.invalidateRenderCache();
            }
            if (item.spec != null) {
                item.spec.resolvedSize = resolveSize(item.spec, emSize, rootWidth);
                maxBulletSize = Math.max(maxBulletSize, item.spec.resolvedSize);
            }
            if (ordered && item.bullet != null) {
                //the natural width of the marker, in a wide enough box
                item.bulletSelfBounds = ctx.resolveNode(item.bullet,
                        NTxBounds2D.ofWidth(box.minX(), box.minY(), 10000, fontSize * 2)).selfBounds2D();
                maxOrderedWidth = Math.max(maxOrderedWidth, item.bulletSelfBounds.widthX());
            }
        }
        //2) the bullet column
        double bulletColumn;
        if (ordered) {
            bulletColumn = Math.max(maxOrderedWidth + fontSize * 0.25, fontSize * 1.1);
        } else {
            bulletColumn = Math.max(Math.max(18, fontSize * 0.8), maxBulletSize + fontSize * 0.12);
        }
        //3) one row per item
        double y0 = box.minY();
        for (int i = 0; i < all.size(); i++) {
            NodeWithIndent item = all.get(i);
            double bulletX = box.minX() + indentFactor * item.indent;
            double childX = bulletX + bulletColumn + marginWidth;
            double childWidth = Math.max(10, box.maxX() - childX);

            item.childSelfBounds = ctx.resolveNode(item.child,
                    NTxBounds2D.ofWidth(childX, y0, childWidth, fontSize * 1.5)).selfBounds2D();
            item.height = Math.max(item.childSelfBounds.widthY(), fontSize * 1.2);

            double bulletBoxY = y0;
            double bulletBoxHeight = item.height;
            if (bulletAlignTop) {
                bulletBoxHeight = fontSize * 1.3;
                bulletBoxY = y0 + fontSize * 0.15;
            }
            NTxBounds2D bulletBox = NTxBounds2D.ofWidth(bulletX, bulletBoxY, bulletColumn, bulletBoxHeight);
            item.bulletBounds = bulletBox;
            if (item.bullet != null) {
                applyBulletDefaults(ctx, item, bulletBox, emSize, rootWidth, rootHeight);
                item.bulletSelfBounds = ctx.resolveNode(item.bullet, bulletBox).selfBounds2D();
            }
            item.childBounds = NTxBounds2D.ofWidth(childX, y0, childWidth, item.height);
            item.rowBounds = NTxBounds2D.ofWidth(bulletX, y0, box.maxX() - bulletX, item.height);

            y0 += item.height;
            if (i < all.size() - 1) {
                y0 += itemGap;
            }
        }
        //4) optional vertical distribution
        if (NTxValueByType.getBoolean(ctx, PROP_DISTRIBUTE).orElse(false)) {
            distribute(box, all);
        }
        if (DEBUG) {
            dump(ctx, ordered, box, fontSize, emSize, bulletColumn, indentFactor, all);
        }
    }

    /** opt-in layout dump, to check list metrics without rendering: {@code -Dntexup.list.debug=true} */
    private static void dump(NTxRendererContext ctx, boolean ordered, NTxBounds2D box, double fontSize, double emSize, double bulletColumn, double indentFactor, List<NodeWithIndent> all) {
        StringBuilder sb = new StringBuilder();
        sb.append("list: box=").append(fmt(box))
                .append(" fontSize=").append(f(fmt(fontSize)))
                .append(" emSize=").append(f(fmt(emSize)))
                .append(" bulletColumn=").append(f(fmt(bulletColumn)))
                .append(" indentStep=").append(f(fmt(indentFactor)))
                .append(" ordered=").append(ordered)
                .append(" items=").append(all.size())
                .append('\n');
        int i = 0;
        for (NodeWithIndent item : all) {
            sb.append("  #").append(i++)
                    .append(" level=").append(item.indent + 1)
                    .append(" bullet=").append(item.bullet == null ? "none" : item.bullet.type())
                    .append(" box=").append(fmt(item.bulletBounds))
                    .append(" ink=").append(fmt(item.bulletSelfBounds));
            if (item.spec != null) {
                sb.append(" shape=").append(item.spec.shape)
                        .append(" size=").append(f(fmt(item.spec.resolvedSize)))
                        .append(" sizeProps=").append(item.bullet.getPropertyValue(NTxPropName.SIZE).map(x -> x.toString()).orElse("-"))
                        .append(" bg=").append(item.bullet.getPropertyValue(NTxPropName.BACKGROUND_COLOR).map(x -> x.toString()).orElse("-"))
                        .append(" color=").append(item.bullet.getPropertyValue(NTxPropName.COLOR).map(x -> x.toString()).orElse("-"));
            }
            if (item.bullet != null && NTxNodeType.TEXT.equals(item.bullet.type())) {
                sb.append(" text=").append(item.bullet.getPropertyValue(NTxPropName.VALUE).map(Object::toString).orElse("-"));
            }
            sb.append(" item=").append(fmt(item.childBounds))
                    .append('\n');
        }
        try {
            ctx.log(NMsg.ofC("%s", sb), ctx.node() == null ? null : ctx.node().source());
        } catch (Throwable t) {
            //diagnostics only
        }
    }

    private static String fmt(NTxBounds2D b) {
        if (b == null) {
            return "null";
        }
        return "(" + f(fmt(b.minX())) + "," + f(fmt(b.minY())) + "," + f(fmt(b.widthX())) + "x" + f(fmt(b.widthY())) + ")";
    }

    private static String fmt(double d) {
        if (!Double.isFinite(d)) {
            return String.valueOf(d);
        }
        return String.valueOf(Math.round(d * 10000d) / 10000d);
    }

    private static String f(String s) {
        return s;
    }

    private static void distribute(NTxBounds2D box, List<NodeWithIndent> all) {
        double required = 0;
        for (NodeWithIndent item : all) {
            required += item.height;
        }
        double available = box.widthY();
        if (available <= required || all.isEmpty()) {
            return;
        }
        double extra = (available - required) / (all.size() + 1);
        double dy = extra;
        for (NodeWithIndent item : all) {
            item.bulletBounds = moved(item.bulletBounds, dy);
            item.bulletSelfBounds = moved(item.bulletSelfBounds, dy);
            item.childBounds = moved(item.childBounds, dy);
            item.childSelfBounds = moved(item.childSelfBounds, dy);
            item.rowBounds = moved(item.rowBounds, dy);
            dy += item.height + extra;
        }
    }

    private static NTxBounds2D moved(NTxBounds2D b, double dy) {
        if (b == null) {
            return null;
        }
        return NTxBounds2D.ofWidth(b.minX(), b.minY() + dy, b.widthX(), b.widthY());
    }

    private static double fontSize(NTxRendererContext ctx, double rootWidth) {
        Font font = NTxValueByName.getFont(ctx);
        if (font == null && ctx.graphics() != null) {
            font = ctx.graphics().getFont();
        }
        double fontSize = font != null ? font.getSize2D() : (rootWidth * 0.025);
        if (!(fontSize > 0)) {
            fontSize = 20;
        }
        return fontSize;
    }

    /**
     * The em size of the item text, in the same units as the bounds. The {@link Font} of the
     * renderer is expressed in points, hence the fallback on the resolved {@code font-size}.
     */
    private static double emSize(NTxRendererContext ctx, double fontSize) {
        double em = NTxValueByName.getFontSize(ctx);
        if (!(em > 0)) {
            em = fontSize;
        }
        return em;
    }

    /**
     * Resolves the bullet size. A plain number and {@code em} are a ratio of the font size of the
     * item ({@code 0.4} means 40% of the text height), {@code %} is the same ratio in percent,
     * and any absolute unit ({@code %P}, {@code px}, {@code pt}, {@code in}, {@code mm} ...) is
     * used as is. Relative values are clamped to a sane range so that a typo can not hide the
     * bullets, absolute ones are only given a lower bound.
     */
    private static double resolveSize(BulletSpec spec, double fontSize, double rootWidth) {
        double ratio = DEFAULT_SIZE_RATIOS[Math.max(0, specLevel(spec) - 1) % DEFAULT_SIZE_RATIOS.length];
        NElement e = spec.size;
        if (e == null) {
            return fontSize * ratio;
        }
        if (e.isNumber()) {
            NNumberElement n = e.asNumber().get();
            double v = n.numberValue().doubleValue();
            String suffix = n.numberSuffix() == null ? "" : n.numberSuffix().trim().toLowerCase(Locale.ROOT);
            switch (suffix) {
                case "em":
                case "": {
                    return clampRatio(v) * fontSize;
                }
                case "%": {
                    return clampRatio(v / 100) * fontSize;
                }
                case "%p": {
                    return atLeastFontSize(v / 100 * rootWidth, fontSize);
                }
                default: {
                    return atLeastFontSize(toPixels(v, suffix), fontSize);
                }
            }
        }
        NOptional<Double> d = NTxValue.of(e).asDouble();
        if (d.isPresent()) {
            return clampRatio(d.get()) * fontSize;
        }
        return fontSize * ratio;
    }

    /** clamps a font relative size ratio, {@code NaN} and non positive values use the level default */
    private static double clampRatio(double ratio) {
        if (!Double.isFinite(ratio) || ratio <= 0) {
            ratio = DEFAULT_SIZE_RATIOS[0];
        }
        return Math.max(MIN_BULLET_SIZE_RATIO, Math.min(MAX_BULLET_SIZE_RATIO, ratio));
    }

    /** an absolute size is honored, but never smaller than a barely visible bullet */
    private static double atLeastFontSize(double size, double fontSize) {
        if (!Double.isFinite(size) || size <= 0) {
            size = fontSize * DEFAULT_SIZE_RATIOS[0];
        }
        return Math.max(size, fontSize * MIN_BULLET_SIZE_RATIO);
    }

    /** converts an absolute css like length to pixels, unknown units are taken as pixels */
    private static double toPixels(double value, String suffix) {
        switch (suffix) {
            case "pt": {
                return value * 96 / 72;
            }
            case "pc": {
                return value * 16;
            }
            case "in": {
                return value * 96;
            }
            case "cm": {
                return value * 96 / 2.54;
            }
            case "mm": {
                return value * 96 / 25.4;
            }
            default: {
                return value;
            }
        }
    }

    private static int specLevel(BulletSpec spec) {
        //default ratios are indexed by level, the level is stored as a field of the item
        return spec.level <= 0 ? 1 : spec.level;
    }

    /**
     * Applies the default appearance of a bullet, for every property that the theme did not
     * define. Theme classes always win, so that templates keep full control.
     */
    private static void applyBulletDefaults(NTxRendererContext ctx, NodeWithIndent item, NTxBounds2D box, double emSize, double rootWidth, double rootHeight) {
        NTxNode b = item.bullet;
        if (b == null) {
            return;
        }
        Set<String> declared = item.bulletClassProps == null ? Collections.emptySet() : item.bulletClassProps;
        boolean isText = NTxNodeType.TEXT.equals(b.type());
        BulletSpec spec = item.spec;

        //placement
        if (!declares(declared, NTxPropName.AT, NTxPropName.ORIGIN, NTxPropName.POSITION)) {
            b.setProperty(NTxProp.ofString(NTxPropName.ORIGIN, ALIGN_CENTER));
            b.setProperty(NTxProp.ofDouble2(NTxPropName.POSITION, 50, 50));
        }
        //size (text markers are sized by their content, or by the theme)
        if (!isText && !declares(declared, NTxPropName.SIZE, NTxPropName.WIDTH, NTxPropName.HEIGHT)) {
            double size = spec == null ? emSize * DEFAULT_SIZE_RATIOS[0] : spec.resolvedSize;
            double w = box.widthX() > 0 ? size / box.widthX() * 100 : 100;
            double h = box.widthY() > 0 ? size / box.widthY() * 100 : 100;
            b.setProperty(NTxProp.ofDouble2(NTxPropName.SIZE, w, h));
        }
        //color
        NElement color = spec == null ? null : spec.color;
        if (color != null) {
            color = eval(ctx, color);
        }
        if (color == null && !isText) {
            color = listColor(ctx);
        }
        if (color != null && !isColorDeclared(declared, isText)) {
            b.setProperty(NTxProp.of(isText ? NTxPropName.COLOR : NTxPropName.BACKGROUND_COLOR, color));
        }
        //round corner of round-rectangles
        if (spec != null && spec.isRoundRectangle() && !declares(declared, NTxPropName.ROUND_CORNER)) {
            double size = spec.resolvedSize;
            double radius = Math.max(0, Math.min(0.5, spec.round)) * size;
            double rx = rootWidth > 0 ? radius / rootWidth * 100 : 0;
            double ry = rootHeight > 0 ? radius / rootHeight * 100 : 0;
            b.setProperty(NTxProp.ofDouble2(NTxPropName.ROUND_CORNER, rx, ry));
        }
    }

    private static boolean isColorDeclared(Set<String> declared, boolean isText) {
        if (isText) {
            return declares(declared, NTxPropName.COLOR);
        }
        return declares(declared, NTxPropName.BACKGROUND_COLOR, "background", NTxPropName.FILL_BACKGROUND);
    }

    /**
     * Default bullet color: the text color of the list, so that markers look like part of the
     * text instead of a foreign accent. The color of the items themselves is not used, because
     * non text items (a shape used as an item, for instance) would give arbitrary colors.
     */
    private static NElement listColor(NTxRendererContext ctx) {
        NElement color = null;
        try {
            color = ctx.<NElement>computePropertyValue(
                    NTxPropName.COLOR, NTxPropName.FOREGROUND_COLOR).orNull();
        } catch (Throwable t) {
            color = null;
        }
        if (color != null) {
            return color;
        }
        Paint paint = NTxValueByName.getForegroundColor(ctx, true);
        return paint == null ? NElement.ofCustom(FALLBACK_BULLET_COLOR) : NElement.ofCustom(paint);
    }

    private static boolean declares(Set<String> declared, String... names) {
        if (declared == null || declared.isEmpty()) {
            return false;
        }
        for (String n : names) {
            if (declared.contains(n)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // style classes
    // ------------------------------------------------------------------------

    static String cls(String base, int indent) {
        return indent > 0 ? base + "-" + (indent + 1) : base;
    }

    /**
     * The theme class of a bullet: {@code ul-bullet-<level>} (or {@code ol-bullet-<level>}) then
     * {@code ul-bullet} (or {@code ol-bullet}), as long as the class is defined by the theme.
     * {@code null} when the theme does not define any.
     */
    private static String bulletClassName(NTxRendererContext ctx, NTxNode item, int indent, boolean ordered) {
        Set<String> allClasses = ctx.engine().computeDeclaredStylesClasses(item);
        String prefix = ordered ? "ol" : "ul";
        String levelName = prefix + "-bullet-" + (indent + 1);
        if (allClasses.contains(levelName)) {
            return levelName;
        }
        String name = prefix + "-bullet";
        if (allClasses.contains(name)) {
            return name;
        }
        return null;
    }

    /**
     * Property names defined by a class definition and its bases, used to know what the theme
     * already provides and what must be defaulted.
     */
    private static Set<String> declaredClassProps(NTxRendererContext ctx, NTxNode item, String className) {
        Set<String> out = new LinkedHashSet<>();
        collectClassProps(ctx, item, className, out, new HashSet<>());
        return out;
    }

    private static void collectClassProps(NTxRendererContext ctx, NTxNode item, String className, Set<String> out, Set<String> visited) {
        if (className == null || !visited.add(className)) {
            return;
        }
        for (NTxStyleRule rule : ctx.engine().computeDeclaredStyles(item)) {
            if (!(rule.selector() instanceof DefaultNTxNodeSelector)) {
                continue;
            }
            DefaultNTxNodeSelector selector = (DefaultNTxNodeSelector) rule.selector();
            for (String name : selector.getClassDefNames()) {
                if (!name.equals(className)) {
                    continue;
                }
                for (NTxProp prop : rule.styles().toList()) {
                    String pn = prop.getName();
                    if (pn != null) {
                        out.add(NTxUtils.uid(pn));
                    }
                }
                NTxStyleRuleSelectorItem.ClassDefItem def = selector.getClassDef(name);
                if (def != null) {
                    for (String base : def.getBases()) {
                        collectClassProps(ctx, item, base, out, visited);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // numbering
    // ------------------------------------------------------------------------

    /**
     * Resolves the {@code numbering} of a list node, so that a nested list can use its own
     * style ({@code ol(numbering: "1"){ ... }}). A nested list without its own style continues
     * the one of the closest ancestor list, which is what makes a mode string such as
     * {@code numbering: "1.a.i"} describe a whole tree of nested lists.
     */
    private static NumberingStrategy resolveNumberingStrategy(NTxNode list, NTxRendererContext ctx) {
        List<String> modes = new ArrayList<>();
        NElement e = numberingProperty(list, ctx, PROP_NUMBERING);
        if (e != null) {
            NOptional<String[]> arr = NTxValue.of(e).asStringArrayOrString();
            if (arr.isPresent()) {
                for (String s : arr.get()) {
                    if (s != null && !s.trim().isEmpty()) {
                        //case is meaningful here: 'a' and 'A' are not the same mode
                        modes.add(s.trim());
                    }
                }
            }
        }
        if (modes.isEmpty()) {
            modes.add(DEFAULT_NUMBERING);
        }
        boolean includeParent = isTrue(numberingProperty(list, ctx, PROP_NUMBERING_PARENT));
        return new NumberingStrategy(modes, includeParent);
    }

    /**
     * Reads a numbering property on the given list, or on the closest ancestor list that declares
     * it. {@code null} is returned when neither the list nor any ancestor list declares it.
     */
    private static NElement numberingProperty(NTxNode list, NTxRendererContext ctx, String name) {
        for (NTxItem item = list; item != null; item = item.parent()) {
            if (!(item instanceof NTxNode)) {
                continue;
            }
            NElement v = ctx.engine().<NElement>computePropertyValue((NTxNode) item, name).orNull();
            if (v != null) {
                return v;
            }
        }
        return list == null
                ? ctx.computePropertyValue(name).orNull()
                : null;
    }

    /** evaluates a raw property element as a boolean flag */
    private static boolean isTrue(NElement value) {
        if (value == null) {
            return false;
        }
        NOptional<Boolean> b = NTxValue.of(value).asBoolean();
        return b.isPresent() && b.get();
    }

    /**
     * Ordered list markers. The default mode is {@code V1a1}: roman, arabic and alphabetic for
     * the first three levels. Use the {@code numbering} property to change it.
     */
    public static class NumberingStrategy {
        private final List<String> modes;
        final boolean includeParent;

        public NumberingStrategy(List<String> modes, boolean includeParent) {
            this.modes = new ArrayList<>(modes);
            this.includeParent = includeParent;
        }

        public String eval(String parent, int index, boolean includeP, int depth) {
            int level = Math.max(0, depth);
            String mode = modes.get(Math.min(level, modes.size() - 1));
            char m = modeAt(mode, level);
            String value;
            switch (m) {
                case 'A': {
                    value = aa(index, true);
                    break;
                }
                case 'a': {
                    value = aa(index, false);
                    break;
                }
                case 'I':
                case 'V': {
                    //'V' is the historical ntexup spelling of upper roman
                    value = intToRoman(index);
                    break;
                }
                case 'i':
                case 'v': {
                    //'v' is accepted as the lower case spelling of upper roman
                    value = intToRoman(index).toLowerCase(Locale.ROOT);
                    break;
                }
                case '1':
                default: {
                    value = String.valueOf(Math.max(0, index));
                    break;
                }
            }
            if (parent != null && !parent.isEmpty() && (includeP || includeParent)) {
                return parent + "." + value;
            }
            return value;
        }

        /**
         * Mode character of the given nesting level. A mode string lists one character per level
         * ({@code "a1i"}), and separators such as {@code .} or a space are ignored, so both
         * {@code "1.a.i"} and {@code "1ai"} are understood. Levels beyond the end keep the last
         * mode, and an empty or unusable string means arabic numbers.
         */
        private static char modeAt(String mode, int level) {
            if (mode == null) {
                return '1';
            }
            int pos = 0;
            char last = 0;
            for (int i = 0; i < mode.length(); i++) {
                char c = mode.charAt(i);
                if (!isModeChar(c)) {
                    continue;
                }
                if (pos == level) {
                    return c;
                }
                pos++;
                last = c;
            }
            return last == 0 ? '1' : last;
        }

        private static boolean isModeChar(char c) {
            switch (c) {
                case '1':
                case 'a':
                case 'A':
                case 'i':
                case 'I':
                case 'v':
                case 'V': {
                    return true;
                }
                default: {
                    return false;
                }
            }
        }

        private static String aa(int index, boolean upper) {
            if (index <= 0) {
                return String.valueOf(index);
            }
            char base = upper ? 'A' : 'a';
            StringBuilder sb = new StringBuilder();
            int a = index - 1;
            do {
                sb.insert(0, (char) ((a % 26) + base));
                a = a / 26 - 1;
            } while (a >= 0);
            return sb.toString();
        }

        private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        private static final String[] ROMAN_LETTERS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

        private static String intToRoman(int index) {
            if (index <= 0 || index > 3999) {
                return String.valueOf(index);
            }
            StringBuilder sb = new StringBuilder();
            int num = index;
            for (int i = 0; i < ROMAN_VALUES.length; i++) {
                while (num >= ROMAN_VALUES[i]) {
                    sb.append(ROMAN_LETTERS[i]);
                    num -= ROMAN_VALUES[i];
                }
            }
            return sb.toString();
        }
    }

    // ------------------------------------------------------------------------
    // misc
    // ------------------------------------------------------------------------

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v;
            }
        }
        return null;
    }

    private static void warn(NTxRendererContext ctx, String format, Object... args) {
        NMsg msg = NMsg.ofC(format, args).asWarning();
        //bullets are resolved several times per item (self bounds, dry runs, render), warn once
        if (!WARNED.add(String.valueOf(msg))) {
            return;
        }
        try {
            ctx.log(msg, ctx.node() == null ? null : ctx.node().source());
        } catch (Throwable t) {
            //never fail the rendering because of a warning
        }
    }

    // ------------------------------------------------------------------------
    // items
    // ------------------------------------------------------------------------

    public static class NodeWithIndent {
        public NTxNode bullet;
        public NTxNode child;
        public int indent;
        public NTxBounds2D bulletBounds;
        public NTxBounds2D bulletSelfBounds;
        public NTxBounds2D childBounds;
        public NTxBounds2D childSelfBounds;
        public NTxBounds2D rowBounds;
        public double height;
        /**
         * appearance of this bullet, {@code null} for ordered lists (their marker is a text
         * that carries its own style)
         */
        BulletSpec spec;
        /** properties defined by the theme class of this bullet */
        Set<String> bulletClassProps;
    }
}
