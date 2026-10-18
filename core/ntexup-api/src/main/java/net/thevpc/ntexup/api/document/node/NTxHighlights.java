package net.thevpc.ntexup.api.document.node;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Access + helpers for the MVC search-highlight model ({@link NTxNodeHighlight}).
 * <p>
 * Highlights are stored in the node's user-object map (survives render-cache
 * invalidation and node cloning, never serialized with the document). All
 * matching goes through {@link #normalize(String)} so that case, diacritics,
 * markup separators ({@code \ _ [[...]]}), punctuation and whitespace never
 * prevent a hit ({@code \lambda_0} and {@code lambda} both normalize into
 * space-separated words).
 *
 * @see NTxNodeHighlight
 */
public final class NTxHighlights {

    private static final String KEY = "ntexup.highlights";

    private NTxHighlights() {
    }

    /**
     * @return the mutable list of highlights bound to the node (never null,
     * may be empty)
     */
    @SuppressWarnings("unchecked")
    public static List<NTxNodeHighlight> get(NTxNode node) {
        if (node == null) {
            return Collections.emptyList();
        }
        Object v = node.getUserObject(KEY).orNull();
        if (v instanceof List) {
            return (List<NTxNodeHighlight>) v;
        }
        return Collections.emptyList();
    }

    /**
     * Binds (or replaces) the highlight list of a node. A null or empty list
     * removes the entry entirely.
     */
    public static void set(NTxNode node, List<NTxNodeHighlight> highlights) {
        if (node == null) {
            return;
        }
        if (highlights == null || highlights.isEmpty()) {
            node.setUserObject(KEY, null);
        } else {
            node.setUserObject(KEY, highlights);
        }
    }

    /**
     * Removes every highlight bound to the node.
     */
    public static void clear(NTxNode node) {
        set(node, null);
    }

    /**
     * True when the node carries at least one highlight.
     */
    public static boolean hasHighlights(NTxNode node) {
        return node != null && node.getUserObject(KEY).isPresent();
    }

    /**
     * Canonical normalization used by search, indexing and painting:
     * <ul>
     *     <li>NFD decomposition, combining marks dropped (diacritic
     *     insensitive: {@code schéma → schema});</li>
     *     <li>letters/digits kept, lowercased;</li>
     *     <li>everything else (whitespace, punctuation, backslashes,
     *     underscores, brackets...) becomes a single space, so
     *     {@code \lambda_0 → " lambda  0"} and the word {@code lambda} still
     *     matches inside it.</li>
     * </ul>
     *
     * @return normalized text, never null
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            String d = Normalizer.normalize(String.valueOf(raw.charAt(i)), Normalizer.Form.NFD);
            for (int k = 0; k < d.length(); k++) {
                char c = d.charAt(k);
                if (Character.getType(c) == Character.NON_SPACING_MARK) {
                    continue;
                }
                if (Character.isLetterOrDigit(c)) {
                    sb.append(Character.toLowerCase(c));
                } else {
                    sb.append(' ');
                }
            }
        }
        return sb.toString();
    }

    /**
     * Needles used for a user query: the whole normalized (trimmed) query
     * first, then its whitespace-separated words. A single-word query yields
     * a single needle.
     *
     * @return normalized needles, possibly empty for blank input
     */
    public static List<String> needlesOf(String query) {
        String nq = normalize(query).trim();
        List<String> out = new ArrayList<>();
        if (nq.isEmpty()) {
            return out;
        }
        out.add(nq);
        if (nq.indexOf(' ') >= 0) {
            for (String w : nq.split(" ")) {
                if (!w.isEmpty() && !out.contains(w)) {
                    out.add(w);
                }
            }
        }
        return out;
    }

    /**
     * Finds every raw {@code [start,end)} range of {@code raw} whose text
     * matches {@code needle} under {@link #normalize(String)}.
     * Indexes map back to the raw (non-normalized) string so callers can
     * slice it directly.
     *
     * @return list of {@code int[]{start,end}} ranges (possibly empty)
     */
    public static List<int[]> findMatches(String raw, String needle) {
        List<int[]> out = new ArrayList<>();
        if (raw == null || raw.isEmpty() || needle == null || needle.isEmpty()) {
            return out;
        }
        NormText n = normalizeWithMap(raw);
        int from = 0;
        while (from <= n.text.length() - needle.length()) {
            int i = n.text.indexOf(needle, from);
            if (i < 0) {
                break;
            }
            out.add(new int[]{n.starts[i], n.ends[i + needle.length() - 1]});
            from = i + 1;
        }
        return out;
    }

    /**
     * Paints every {@link NTxNodeHighlight#isWholeNode() whole-node} span of
     * the node over its layout bounds. Called by
     * {@code net.thevpc.ntexup.engine.renderer.NTxNodeRendererBase} after the
     * node content is drawn; does nothing unless the host enabled the
     * {@link NTxRendererContext#CAPABILITY_SEARCH_HIGHLIGHTS} capability (the
     * screen viewer), so PDF/HTML/image renders stay clean.
     */
    public static void paintWholeNode(NTxNode node, NTxRendererContext ctx) {
        List<NTxNodeHighlight> hs = get(node);
        if (hs.isEmpty() || ctx == null || !ctx.isCapability(NTxRendererContext.CAPABILITY_SEARCH_HIGHLIGHTS)) {
            return;
        }
        NTxBounds2D b = ctx.selfBounds2D();
        if (b == null || b.widthX() == null || b.widthY() == null
                || b.widthX() <= 0 || b.widthY() <= 0) {
            return;
        }
        double r = Math.min(4.0, Math.min(b.widthX(), b.widthY()) / 4.0);
        NTxGraphics g = ctx.graphics();
        for (NTxNodeHighlight h : hs) {
            if (!h.isWholeNode() || h.getColor() == null) {
                continue;
            }
            g.setColor(h.getColor());
            g.fillRoundRect(b.minX(), b.minY(), b.widthX(), b.widthY(), r, r);
        }
    }

    /**
     * Normalized text plus per-normalized-char raw index mapping
     * ({@code starts[i]}/{@code ends[i]} = raw range that produced
     * normalized char {@code i}).
     */
    private static NormText normalizeWithMap(String raw) {
        int len = raw.length();
        StringBuilder sb = new StringBuilder(len);
        int[] starts = new int[len * 2 + 16];
        int[] ends = new int[len * 2 + 16];
        int n = 0;
        for (int i = 0; i < len; i++) {
            String d = Normalizer.normalize(String.valueOf(raw.charAt(i)), Normalizer.Form.NFD);
            for (int k = 0; k < d.length(); k++) {
                char c = d.charAt(k);
                if (Character.getType(c) == Character.NON_SPACING_MARK) {
                    continue;
                }
                if (n >= starts.length) {
                    starts = java.util.Arrays.copyOf(starts, starts.length * 2);
                    ends = java.util.Arrays.copyOf(ends, ends.length * 2);
                }
                sb.append(Character.isLetterOrDigit(c) ? Character.toLowerCase(c) : ' ');
                starts[n] = i;
                ends[n] = i + 1;
                n++;
            }
        }
        int[] s = java.util.Arrays.copyOf(starts, n);
        int[] e = java.util.Arrays.copyOf(ends, n);
        return new NormText(sb.toString(), s, e);
    }

    private static final class NormText {
        final String text;
        final int[] starts;
        final int[] ends;

        NormText(String text, int[] starts, int[] ends) {
            this.text = text;
            this.starts = starts;
            this.ends = ends;
        }
    }
}
