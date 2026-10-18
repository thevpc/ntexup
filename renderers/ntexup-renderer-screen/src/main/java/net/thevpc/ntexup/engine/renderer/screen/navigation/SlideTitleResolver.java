package net.thevpc.ntexup.engine.renderer.screen.navigation;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.nuts.elem.NElement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Static, render-free resolution of a slide's display title.
 * <p>
 * A slide is an {@link net.thevpc.ntexup.api.engine.NTxCompiledPage}; its title
 * is resolved from the compiled node tree <b>without rendering</b>:
 * <ol>
 *     <li>an explicit {@code title} property on the page root — rarely present,
 *     themes usually interpolate it into a text node;</li>
 *     <li>the first node carrying the {@code title} style class (used by
 *     cover slides);</li>
 *     <li>the first {@code text} node (the header title of
 *     {@code content-slide}/{@code agenda-slide}/{@code thankyou-slide});</li>
 *     <li>the section name (source folder) + {@code " (untitled)"};</li>
 *     <li>{@code "Slide N"} (N = the 1-based number the viewer shows).</li>
 * </ol>
 * The fallback chain is pure and unit-testable; the tree walk is separated in
 * {@link #extract(NTxNode)} so both steps can be exercised independently.
 */
public final class SlideTitleResolver {

    /**
     * Control-flow containers whose own props are not rendered content
     * (children are still visited).
     */
    private static final Set<String> SKIP_TYPES = new HashSet<>(Arrays.asList(
            "assign", "define", "block", "group", "page-group", "fragment", "void", "then", "else"));

    /**
     * String props considered title-bearing text.
     */
    private static final Set<String> TEXT_PROPS = new HashSet<>(Arrays.asList(
            "value", "title", "kicker", "label", "text", "caption", "description",
            "notes", "subtitle", "file", "tooltip", "alt"));

    /**
     * The three title candidates statically extractable from a page node.
     */
    public static final class Candidates {
        /** explicit {@code title} prop on the page root (may be null) */
        public String explicitTitle;
        /** text of the first node with the {@code title} class (may be null) */
        public String classTitle;
        /** text of the first {@code text} node (may be null) */
        public String firstText;

        public boolean isEmpty() {
            return explicitTitle == null && classTitle == null && firstText == null;
        }
    }

    private SlideTitleResolver() {
    }

    /**
     * Walks the compiled page tree once, collecting the three title candidates.
     * Never renders, never resolves expressions beyond what has already been
     * compiled into the node props.
     */
    public static Candidates extract(NTxNode root) {
        Candidates c = new Candidates();
        if (root == null) {
            return c;
        }
        c.explicitTitle = firstStringProp(root, "title");
        collect(root, c);
        return c;
    }

    private static void collect(NTxNode node, Candidates c) {
        if (node == null) {
            return;
        }
        String type = node.type();
        boolean textNode = "text".equals(type);
        boolean titleClass = c.classTitle == null && node.hasClass("title");
        if (textNode || titleClass) {
            String t = textOf(node);
            if (t != null) {
                if (titleClass) {
                    c.classTitle = t;
                }
                if (textNode && c.firstText == null) {
                    c.firstText = t;
                }
            }
        }
        for (NTxNode child : node.children()) {
            collect(child, c);
        }
    }

    /**
     * Applies the fallback chain. All inputs may be null/blank; the result is
     * never blank.
     *
     * @param c         candidates extracted from the slide node
     * @param section   section name (source folder), may be null
     * @param userIndex 1-based slide number as shown by the viewer
     */
    public static String resolve(Candidates c, String section, int userIndex) {
        if (c != null) {
            String t = firstLine(c.explicitTitle);
            if (t != null) {
                return t;
            }
            t = firstLine(c.classTitle);
            if (t != null) {
                return t;
            }
            t = firstLine(c.firstText);
            if (t != null) {
                return t;
            }
        }
        String sec = firstLine(section);
        if (sec != null) {
            return sec + " (untitled)";
        }
        return "Slide " + userIndex;
    }

    /**
     * Extracts a human section name from a source path: the immediate parent
     * folder, stripped of any leading ordering prefix ({@code 0040-elements} →
     * {@code elements}); falls back to the file stem when there is no folder.
     *
     * @param fullPath full source path or file name (may be null)
     * @return cleaned section name, or null
     */
    public static String sectionFromPath(String fullPath) {
        if (fullPath == null) {
            return null;
        }
        String p = stripTrailingSlash(stripQuery(fullPath));
        if (p.isEmpty()) {
            return null;
        }
        String file = basename(p);
        String parent = parentPath(p);
        if (parent != null) {
            String dir = stripOrderPrefix(basename(parent));
            if (dir != null && !dir.isEmpty()) {
                return dir;
            }
        }
        String stem = stripOrderPrefix(stripExtension(file));
        return stem == null || stem.isEmpty() ? null : stem;
    }

    private static String stripOrderPrefix(String name) {
        if (name == null) {
            return null;
        }
        int i = 0;
        while (i < name.length() && Character.isDigit(name.charAt(i))) {
            i++;
        }
        if (i > 0 && i < name.length() && (name.charAt(i) == '-' || name.charAt(i) == '_')) {
            i++;
        }
        String s = name.substring(i).trim();
        return s.isEmpty() ? name.trim() : s;
    }

    private static String stripTrailingSlash(String s) {
        int end = s.length();
        while (end > 0 && (s.charAt(end - 1) == '/' || s.charAt(end - 1) == '\\')) {
            end--;
        }
        return s.substring(0, end);
    }

    private static String stripQuery(String s) {
        int i = Math.min(indexOfOrMax(s, '?'), indexOfOrMax(s, '#'));
        return s.substring(0, i);
    }

    private static int indexOfOrMax(String s, char c) {
        int i = s.indexOf(c);
        return i < 0 ? s.length() : i;
    }

    private static String basename(String path) {
        int i = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return i >= 0 ? path.substring(i + 1) : path;
    }

    private static String parentPath(String path) {
        int i = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return i > 0 ? path.substring(0, i) : null;
    }

    private static String stripExtension(String name) {
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : name;
    }

    private static String textOf(NTxNode node) {
        if (node.type() != null && SKIP_TYPES.contains(node.type())) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (NTxProp prop : node.props()) {
            if (prop == null || prop.getName() == null || !TEXT_PROPS.contains(prop.getName())) {
                continue;
            }
            NElement v = prop.getValue();
            if (v == null || v.isNull()) {
                continue;
            }
            String s = v.asStringValue().orNull();
            if (s == null || s.trim().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(s.trim());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static String firstStringProp(NTxNode node, String name) {
        if (node == null) {
            return null;
        }
        NTxProp p = node.getProperty(name).orNull();
        if (p == null) {
            return null;
        }
        NElement v = p.getValue();
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asStringValue().orNull();
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private static String firstLine(String s) {
        if (s == null) {
            return null;
        }
        int i = s.indexOf('\n');
        if (i < 0) {
            i = s.indexOf('\r');
        }
        if (i >= 0) {
            s = s.substring(0, i);
        }
        s = s.trim();
        return s.isEmpty() ? null : s;
    }
}
