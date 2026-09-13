package net.thevpc.ntexup.extension.shapes2d.container.util;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxMargin;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.style.DefaultNTxStyleRule;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxSizeRef;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.elem.NElement;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class NTxListHelper {
    private static NumberingStrategy resolveNumberingStrategy(NTxNode p, boolean ordered, NTxRendererContext ctx) {
        return new NumberingStrategy();
    }

    public static List<NodeWithIndent> build(NTxNode p, boolean ordered, NTxRendererContext ctx) {
        List<NodeWithIndent> all = new ArrayList<>();
        List<NTxNode> children = p.children();
        NTxDocumentFactory f = ctx.documentFactory();
        NumberingStrategy ns = resolveNumberingStrategy(p, ordered, ctx);
        int userIndex = 1;
        String parentString = "";
        String lastParent = "";
        for (int i = 0; i < children.size(); i++) {
            NTxNode child = children.get(i);
            if (isList(child)) {
                fillAny(child, all, 0, ordered, lastParent, userIndex, f, ctx, ns);
            } else {
                fillAny(child, all, 0, ordered, parentString, userIndex, f, ctx, ns);
                lastParent = ns.eval("", userIndex, false, 0);
                userIndex++;
            }
        }
        NTxSizeRef nTxSizeRef = ctx.sizeRef();
        double rw = nTxSizeRef.getRootWidth();
        NTxBounds2D sb = ctx.defaultSelfBounds2D();
        NTxMargin padding = NTxValueByName.getPadding(ctx);
        if (padding != null && !padding.isZero()) {
            sb = NTxBounds2D.ofWidth(
                    sb.minX() + padding.getLeft(),
                    sb.minY() + padding.getTop(),
                    Math.max(0, sb.widthX() - padding.getLeft() - padding.getRight()),
                    Math.max(0, sb.widthY() - padding.getTop() - padding.getBottom())
            );
        }

        Font font = NTxValueByName.getFont(ctx);
        if (font == null && ctx.graphics() != null) {
            font = ctx.graphics().getFont();
        }
        double fontSize = font != null ? font.getSize2D() : (rw * 0.025);
        if (fontSize <= 0) {
            fontSize = 20;
        }

        double bulletWidth;
        double marginWidth = Math.max(8, fontSize * 0.4);
        double indentFactor = Math.max(18, fontSize * 1.2);

        if (ordered) {
            double maxBulletW = 0;
            for (NodeWithIndent child : all) {
                child.bulletSelfBounds = ctx.resolveNode(child.bullet, NTxBounds2D.ofWidth(sb.minX(), sb.minY(), 200, fontSize * 1.5)).selfBounds2D();
                maxBulletW = Math.max(maxBulletW, child.bulletSelfBounds.widthX());
            }
            bulletWidth = Math.max(maxBulletW + 4, fontSize * 1.2);
        } else {
            bulletWidth = Math.max(18, fontSize * 0.8);
        }

        double itemGap = Math.max(4, fontSize * 0.25);
        // Read bullet-align: "top" aligns bullet to first line, "center" is the default
        String bulletAlign = net.thevpc.ntexup.api.eval.NTxValueByType.getStringOrName(ctx, "bullet-align").orElse("center");
        boolean bulletAlignTop = "top".equalsIgnoreCase(bulletAlign);

        double y0 = sb.minY();
        for (int i = 0; i < all.size(); i++) {
            NodeWithIndent child = all.get(i);
            double indentWidth = indentFactor * child.indent;
            double bulletX = sb.minX() + indentWidth;
            double childX = bulletX + bulletWidth + marginWidth;
            double childW = Math.max(10, sb.maxX() - childX);

            child.bullet.invalidateRenderCache();
            child.child.invalidateRenderCache();

            child.bulletSelfBounds = ctx.resolveNode(child.bullet, NTxBounds2D.ofWidth(bulletX, y0, bulletWidth, fontSize * 1.3)).selfBounds2D();
            child.childSelfBounds = ctx.resolveNode(child.child, NTxBounds2D.ofWidth(childX, y0, childW, fontSize * 1.5)).selfBounds2D();

            double naturalChildH = child.childSelfBounds.widthY();
            double lineH = Math.max(naturalChildH, fontSize * 1.2);
            child.height = lineH;

            double bulletH;
            double bulletY;
            if (bulletAlignTop) {
                // Align bullet to first-line level with a small top padding
                bulletY = y0 + fontSize * 0.15;
                bulletH = child.bulletSelfBounds.widthY();
            } else {
                // Center bullet vertically within the row (default)
                bulletH = child.height;
                bulletY = y0;
            }
            child.bulletBounds = NTxBounds2D.ofWidth(bulletX, bulletY, bulletWidth, bulletH);
            child.childBounds = NTxBounds2D.ofWidth(childX, y0, childW, child.height);
            child.rowBounds = NTxBounds2D.ofWidth(bulletX, y0, sb.maxX() - bulletX, child.height);

            y0 += child.height;

            if (i < all.size() - 1) {
                y0 += itemGap;
            }
        }

        boolean distribute = net.thevpc.ntexup.api.eval.NTxValueByType.getBoolean(ctx, "distribute").orElse(false);
        if (distribute && !all.isEmpty()) {
            double totalRequired = 0;
            for (NodeWithIndent c : all) {
                totalRequired += c.height;
            }
            double availableH = sb.widthY();
            if (availableH > totalRequired) {
                double extra = (availableH - totalRequired) / (all.size() + 1);
                y0 = sb.minY() + extra;
                for (NodeWithIndent child : all) {
                    double indentWidth = indentFactor * child.indent;
                    double bulletX = sb.minX() + indentWidth;
                    double childX = bulletX + bulletWidth + marginWidth;
                    double childW = Math.max(10, sb.maxX() - childX);
                    double bulletH;
                    double bulletY;
                    if (bulletAlignTop) {
                        bulletY = y0 + fontSize * 0.15;
                        bulletH = child.bulletSelfBounds.widthY();
                    } else {
                        bulletH = child.height;
                        bulletY = y0;
                    }
                    child.bulletBounds = NTxBounds2D.ofWidth(bulletX, bulletY, bulletWidth, bulletH);
                    child.childBounds = NTxBounds2D.ofWidth(childX, y0, childW, child.height);
                    child.rowBounds = NTxBounds2D.ofWidth(bulletX, y0, sb.maxX() - bulletX, child.height);
                    y0 += child.height + extra;
                }
            }
        }
        return all;

    }

    private static boolean isList(NTxNode p) {
        switch (p.type()) {
            case NTxNodeType.UNORDERED_LIST:
            case NTxNodeType.ORDERED_LIST: {
                return true;
            }
        }
        return false;
    }

    private static void fillList(List<NTxNode> children, List<NodeWithIndent> all, int indent, boolean ordered, String parentString, NTxDocumentFactory f, NTxRendererContext ctx, NumberingStrategy ns) {
        int userIndex = 1;
        String lastParent = "";
        for (int i = 0; i < children.size(); i++) {
            NTxNode child = children.get(i);
            if (isList(child)) {
                fillAny(child, all, indent, ordered, lastParent, userIndex, f, ctx, ns);
            } else {
                fillAny(child, all, indent, ordered, parentString, userIndex, f, ctx, ns);
                lastParent = ns.eval("", userIndex, false, indent);
                userIndex++;
            }
        }
    }

    private static boolean fillNonList(NTxNode p, List<NodeWithIndent> all, int indent, boolean ordered, String parentString, int index, NTxDocumentFactory f, NTxRendererContext ctx, NumberingStrategy ns) {
        switch (p.type()) {
            case NTxNodeType.UNORDERED_LIST:
            case NTxNodeType.ORDERED_LIST: {
                return false;
            }
            default: {
                NodeWithIndent g = new NodeWithIndent();
                Set<String> allClasses = ctx.engine().computeDeclaredStylesClasses(p);
                String specialStyle = null;
                String clsPrefix = ordered ? "ol" : "ul";
                if (allClasses.contains(clsPrefix + "-bullet-" + (indent + 1))) {
                    specialStyle = clsPrefix + "-bullet-" + (indent + 1);
                } else {
                    if (!allClasses.contains(clsPrefix + "-bullet")) {
                        NTxNode r = NTxUtils.findRootNode(p.parent());
                        List<NTxProp> styles = new ArrayList<>();
                        styles.addAll(
                                Arrays.asList(
                                        NTxProp.of("origin", NElement.ofString("center"))
                                        , NTxProp.of("position", NElement.ofDoubleArray(50, 50))
//                                        , NTxProp.of("margin", NElement.ofDoubleArray(10))
                                )
                        );
                        if (ordered) {
                            styles.add(NTxProp.of("size", NElement.ofDouble(5, "%P")));
                        } else {
                            styles.add(NTxProp.of("size", NElement.ofDouble(2.5, "%P")));
                            styles.add(NTxProp.of("background", NElement.ofString("blue")));
                        }
                        r.addRule(DefaultNTxStyleRule.ofClass(r, r.source(), clsPrefix + "-bullet", styles.toArray(new NTxProp[0])));
                    }
                    specialStyle = clsPrefix + "-bullet";
                }
                if (ordered) {
                    g.bullet = f.ofText(ns.eval(parentString, index, true, indent))
                            .addStyleClasses(specialStyle)
                            .setSource(p.source());
                } else {
//                    g.bullet =f.ofGroup()
//                                    .setSource(p.source())
//                                        .setProperty("background",NElement.ofName("yellow"))
//                                        .setProperty("color",NElement.ofName("blue"))
//                                            .add(f.ofCircle()
//                                                    .addStyleClasses(specialStyle)
//                                                    .setProperty("at",NElement.ofName("center"))
//                                                    .setProperty("align",NElement.ofName("center"))
//                                                    .setSource(p.source()))
//                            ;
                    g.bullet = f.ofCircle()
                            .addStyleClasses(specialStyle)
                            .setSource(p.source())
                    ;
                }
                g.bullet.setParent(p.parent());
                if (allClasses.contains(clsPrefix + "-item-" + (indent + 1))) {
                    specialStyle = clsPrefix + "-item-" + (indent + 1);
                } else {
                    specialStyle = clsPrefix + "-item";
                }
                g.child = p.addStyleClasses(specialStyle);

                g.indent = indent;
                all.add(g);
                return true;
            }
        }
    }

    private static void fillAny(NTxNode p, List<NodeWithIndent> all, int indent, boolean ordered, String parentString, int index, NTxDocumentFactory f, NTxRendererContext ctx, NumberingStrategy ns) {
        switch (p.type()) {
            case NTxNodeType.UNORDERED_LIST: {
                List<NTxNode> children = p.children();
                fillList(children, all, indent + 1, false, parentString, f, ctx, ns);
                break;
            }
            case NTxNodeType.ORDERED_LIST: {
                ns = resolveNumberingStrategy(p, ordered, ctx);
                List<NTxNode> children = p.children();
                fillList(children, all, indent + 1, true, parentString, f, ctx, ns);
                break;
            }
            default: {
                fillNonList(p, all, indent, ordered, parentString, index, f, ctx, ns);
                break;
            }
        }
    }

    public static class NumberingStrategy {
        boolean includeParent = false;
        String mode = "V1a1";

        public String eval(String parentString, int index, boolean format, int depth) {
            StringBuilder sb = new StringBuilder();
            if (includeParent) {
                if (parentString.isEmpty()) {
                    sb.append(parentString);
                    sb.append(".");
                }
            }
            String mode = this.mode;
            if (mode == null || mode.length() == 0) {
                mode = "V1a1";
            }
            char m;
            if (depth <= 0) {
                m = mode.charAt(0);
            } else if (depth >= mode.length()) {
                m = mode.charAt(mode.length() - 1);
            } else {
                m = mode.charAt(depth);
            }
            sb.append(aa(m, index));
            if (format) {
                sb.append(".");
            }
            return sb.toString();
        }

        private String aa(char mode, int index) {
            StringBuilder sb = new StringBuilder();
            switch (mode) {
                case '1': {
                    sb.append(index);
                    break;
                }
                case 'a': {
                    StringBuilder sb2 = new StringBuilder();
                    int a = index - 1;
                    while (true) {
                        int u = a / 26;
                        int v = a % 26;
                        sb2.insert(0, (char) (v + 'a'));
                        if (u == 0) {
                            break;
                        }
                        a = u;
                    }
                    sb.append(sb2);
                    break;
                }
                case 'A': {
                    StringBuilder sb2 = new StringBuilder();
                    int a = index - 1;
                    while (true) {
                        int u = a / 26;
                        int v = a % 26;
                        sb2.insert(0, (char) (v + 'A'));
                        if (u == 0) {
                            break;
                        }
                        a = u;
                    }
                    sb.append(sb2);
                    break;
                }
                case 'V': {
                    if (index > 3999) {
                        sb.append(index);
                    } else {
                        sb.append(intToRoman(index));
                    }
                    break;
                }
                default: {
                    sb.append(index);
                }
            }
            return sb.toString();
        }
    }

    public static String intToRoman(int num) {
        if (num <= 0 || num > 3999) {
            throw new IllegalArgumentException("Number out of range (must be 1..3999)");
        }

        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < values.length; i++) {
            while (num >= values[i]) {
                num -= values[i];
                result.append(symbols[i]);
            }
        }

        return result.toString();
    }

    public static class NodeWithIndent {
        public int indent;
        public NTxNode bullet;
        public NTxNode child;
        public NTxBounds2D rowBounds;
        public NTxBounds2D childBounds;
        public NTxBounds2D childSelfBounds;
        public NTxBounds2D bulletBounds;
        public NTxBounds2D bulletSelfBounds;
        public double height;
    }
}
