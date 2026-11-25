package net.thevpc.ntexup.api.engine;

import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Conventional per-slide metadata (<b>chapter</b>, <b>title</b>,
 * <b>subtitle</b>) captured while the document is compiled.
 * <p>
 * Themes usually render the slide title through a component call such as
 * {@code content-slide(title:"Introduction")}. Once compiled, the value is
 * only a plain {@code text} node: the original {@code title} argument is gone,
 * and tools (search results, the go-to-slide outline, the HUD) cannot tell it
 * apart from the document title or the author printed in the header.
 * <p>
 * To keep that information, the compiler records the slide-level arguments of
 * the <b>outermost</b> component call of each page:
 * <pre>{@code
 * content-slide(chapter:"Part I", title:"Introduction", subtitle:"Antennas"){ ... }
 * }</pre>
 * Values are attached as user objects on the page node and stay available
 * through {@link #read(NTxNode)}. They never take part in layout or rendering.
 * <p>
 * Supported names (case/spelling normalised by the compiler):
 * {@link #TITLE}, {@link #SUBTITLE} (alias {@link #KICKER}), {@link #CHAPTER}
 * (alias {@link #SECTION}).
 */
public final class NTxSlideMeta {

    public static final String TITLE = "title";
    public static final String SUBTITLE = "subtitle";
    public static final String KICKER = "kicker";
    public static final String CHAPTER = "chapter";
    public static final String SECTION = "section";

    private static final String PREFIX = "ntexup.slide.";
    private static final String DEPTH_SUFFIX = ".depth";

    private NTxSlideMeta() {
    }

    /**
     * Attaches the arguments to every page node found in {@code root}'s
     * subtree (and to {@code root} itself when it is a page).
     * <p>
     * {@code depth} is the nesting level of the component call (0 = outermost).
     * A value coming from a less nested call always wins, so a title argument
     * of a card inside a slide can never replace the slide title.
     */
    public static void attach(NTxNode root, Map<String, String> values, int depth) {
        if (root == null || values == null || values.isEmpty()) {
            return;
        }
        if (NTxNodeType.PAGE.equals(root.type())) {
            for (Map.Entry<String, String> e : values.entrySet()) {
                setIfBetter(root, e.getKey(), e.getValue(), depth);
            }
        }
        for (NTxNode child : root.children()) {
            attach(child, values, depth);
        }
    }

    /**
     * Reads the metadata of a page node. Never null; absent values are
     * {@code null} inside the returned map.
     */
    public static Map<String, String> read(NTxNode page) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put(TITLE, get(page, TITLE));
        String subtitle = get(page, SUBTITLE);
        if (subtitle == null) {
            subtitle = get(page, KICKER);
        }
        m.put(SUBTITLE, subtitle);
        String chapter = get(page, CHAPTER);
        if (chapter == null) {
            chapter = get(page, SECTION);
        }
        m.put(CHAPTER, chapter);
        return m;
    }

    public static String get(NTxNode page, String name) {
        if (page == null || name == null) {
            return null;
        }
        Object v = page.getUserObject(PREFIX + name).orNull();
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    /**
     * Stores {@code value} unless a value from a less nested call (smaller
     * depth) is already present.
     */
    public static void setIfBetter(NTxNode page, String name, String value, int depth) {
        if (page == null || name == null || value == null) {
            return;
        }
        String s = value.trim();
        if (s.isEmpty()) {
            return;
        }
        Object oldDepth = page.getUserObject(PREFIX + name + DEPTH_SUFFIX).orNull();
        if (oldDepth instanceof Number && get(page, name) != null
                && ((Number) oldDepth).intValue() <= depth) {
            return;
        }
        page.setUserObject(PREFIX + name, s);
        page.setUserObject(PREFIX + name + DEPTH_SUFFIX, depth);
    }
}
