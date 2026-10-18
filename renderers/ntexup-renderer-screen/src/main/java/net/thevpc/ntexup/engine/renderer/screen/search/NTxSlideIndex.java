package net.thevpc.ntexup.engine.renderer.screen.search;

import net.thevpc.ntexup.api.document.node.NTxHighlights;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxNodeRenderer;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.renderer.text.NTxTextBaseRenderer;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.NOptional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Full-text index of the slides (one {@link SlideEntry} per page), built
 * lazily the first time the Find dialog is opened (on the viewer's async
 * loader thread) and rebuilt after a document reload.
 * <p>
 * For each page it extracts:
 * <ul>
 *     <li>the title (page {@code title} prop → node with class {@code title}
 *     → first {@code text} node),</li>
 *     <li>a kicker (page {@code kicker} prop → second single-line text
 *     node ≤ 60 chars),</li>
 *     <li>the source file basename,</li>
 *     <li>every text-bearing node with its raw + normalized text, so the
 *     highlight manager can bind {@link NTxHighlights} spans to the matching
 *     nodes only.</li>
 * </ul>
 */
public class NTxSlideIndex {

    /**
     * Control-flow containers whose own props are not rendered content
     * (children are still visited).
     */
    private static final Set<String> SKIP_TYPES = new HashSet<>(Arrays.asList(
            "assign", "define", "block", "group", "page-group", "fragment", "void", "then", "else"));

    /**
     * String props considered searchable text.
     */
    private static final Set<String> TEXT_PROPS = new HashSet<>(Arrays.asList(
            "value", "title", "kicker", "label", "text", "caption", "description",
            "notes", "subtitle", "file", "tooltip", "alt"));

    /**
     * One text-bearing node of a slide: the model side of the MVC design
     * (what can be highlighted on this node).
     */
    public static class NodeText {
        public final NTxNode node;
        public final String text;
        public final String normText;
        /**
         * True when the node's renderer is not a text renderer, i.e. it paints
         * as one block (equations, images, components): its highlight is a
         * single tint over the node bounds instead of per-token rectangles.
         */
        public final boolean wholeNode;

        public NodeText(NTxNode node, String text, String normText, boolean wholeNode) {
            this.node = node;
            this.text = text;
            this.normText = normText;
            this.wholeNode = wholeNode;
        }
    }

    public static class SlideEntry {
        public final int pageIndex;
        public final String title;
        public final String kicker;
        public final String sourceFileName;
        public final String rawText;
        public final String normText;
        public final List<String> words;
        public final List<NodeText> nodeTexts;

        public SlideEntry(int pageIndex, String title, String kicker, String sourceFileName,
                          String rawText, String normText, List<String> words, List<NodeText> nodeTexts) {
            this.pageIndex = pageIndex;
            this.title = title;
            this.kicker = kicker;
            this.sourceFileName = sourceFileName;
            this.rawText = rawText;
            this.normText = normText;
            this.words = Collections.unmodifiableList(words);
            this.nodeTexts = Collections.unmodifiableList(nodeTexts);
        }
    }

    private final List<SlideEntry> entries;

    private NTxSlideIndex(List<SlideEntry> entries) {
        this.entries = Collections.unmodifiableList(entries);
    }

    /**
     * Builds the index; forces the lazy compilation of every page (fast:
     * a few ms per page).
     */
    public static NTxSlideIndex build(NTxEngine engine, NTxCompiledDocument document) {
        List<SlideEntry> entries = new ArrayList<>();
        if (document != null) {
            for (NTxCompiledPage page : document.pages()) {
                try {
                    entries.add(buildEntry(engine, page));
                } catch (Throwable ex) {
                    entries.add(new SlideEntry(page.index(), null, null, null,
                            "", "", new ArrayList<>(), new ArrayList<>()));
                }
            }
        }
        return new NTxSlideIndex(entries);
    }

    public List<SlideEntry> entries() {
        return entries;
    }

    public int size() {
        return entries.size();
    }

    private static SlideEntry buildEntry(NTxEngine engine, NTxCompiledPage page) {
        List<NodeText> nodeTexts = new ArrayList<>();
        String[] firstTwo = new String[2];
        String[] classTitle = new String[1];
        NTxNode root = page.compiledPage();
        String title = null;
        String kicker = null;
        if (root != null) {
            title = firstStringProp(root, "title");
            kicker = firstStringProp(root, "kicker");
            collect(engine, root, nodeTexts, firstTwo, classTitle);
            if (title == null) {
                title = classTitle[0];
            }
            if (title == null) {
                title = firstTwo[0];
            }
            if (kicker == null && firstTwo[1] != null
                    && firstTwo[1].length() <= 60 && firstTwo[1].indexOf('\n') < 0
                    && !firstTwo[1].equals(title)) {
                kicker = firstTwo[1];
            }
        }
        title = firstLine(title);
        kicker = firstLine(kicker);

        StringBuilder raw = new StringBuilder();
        appendLine(raw, title);
        appendLine(raw, kicker);
        for (NodeText nt : nodeTexts) {
            appendLine(raw, nt.text);
        }
        String rawText = raw.toString();
        String normText = NTxHighlights.normalize(rawText);
        List<String> words = new ArrayList<>();
        if (!normText.isEmpty()) {
            for (String w : normText.split(" ")) {
                if (!w.isEmpty()) {
                    words.add(w);
                }
            }
        }
        return new SlideEntry(page.index(), title, kicker, sourceName(page),
                rawText, normText, words, nodeTexts);
    }

    private static void collect(NTxEngine engine, NTxNode node, List<NodeText> out,
                                String[] firstTwo, String[] classTitle) {
        String type = node.type();
        if (type != null && !SKIP_TYPES.contains(type)) {
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
            if (sb.length() > 0) {
                String text = sb.toString();
                out.add(new NodeText(node, text, NTxHighlights.normalize(text), isWholeNode(engine, node)));
                if (classTitle[0] == null && node.hasClass("title")) {
                    classTitle[0] = text;
                }
                if ("text".equals(type)) {
                    if (firstTwo[0] == null) {
                        firstTwo[0] = text;
                    } else if (firstTwo[1] == null) {
                        firstTwo[1] = text;
                    }
                }
            }
        }
        for (NTxNode child : node.children()) {
            collect(engine, child, out, firstTwo, classTitle);
        }
    }

    /**
     * Text renderers paint matching tokens (TOKENS mode); everything else
     * (equations, images, custom components) is tinted over its whole bounds
     * (BOUNDS mode). Decided once at index time by the same rule the paint
     * paths use, so both modes stay disjoint.
     */
    private static boolean isWholeNode(NTxEngine engine, NTxNode node) {
        if (engine == null) {
            return true;
        }
        NTxNodeRenderer r = engine.getRenderer(node.type()).orNull();
        return !(r instanceof NTxTextBaseRenderer);
    }

    private static String firstStringProp(NTxNode node, String name) {
        NOptional<NTxProp> p = node.getProperty(name);
        if (p.isPresent()) {
            NTxProp prop = p.get();
            NElement v = prop.getValue();
            if (v != null && !v.isNull()) {
                String s = v.asStringValue().orNull();
                if (s != null && !s.trim().isEmpty()) {
                    return s.trim();
                }
            }
        }
        return null;
    }

    private static String firstLine(String s) {
        if (s == null) {
            return null;
        }
        int i = s.indexOf('\n');
        if (i >= 0) {
            s = s.substring(0, i);
        }
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    private static void appendLine(StringBuilder sb, String s) {
        if (s == null || s.trim().isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(s.trim());
    }

    private static String sourceName(NTxCompiledPage page) {
        Object s = page.source();
        if (s == null && page.rawPage() != null) {
            s = NTxUtils.sourceOf(page.rawPage());
        }
        if (s instanceof NTxSource) {
            NTxSource src = (NTxSource) s;
            NPath path = src.path().orNull();
            if (path != null) {
                return path.name();
            }
            return basename(src.shortName());
        }
        if (s instanceof String) {
            return basename((String) s);
        }
        return null;
    }

    private static String basename(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        int i = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
        return i >= 0 ? s.substring(i + 1) : s;
    }
}
