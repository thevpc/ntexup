package net.thevpc.ntexup.engine.renderer.screen.navigation;

import net.thevpc.ntexup.api.document.node.NTxHighlights;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure filter + ranking for the "Go to slide" navigator.
 * <p>
 * One field, no mode switch. The query is interpreted uniformly:
 * <ul>
 *     <li><b>digits only</b> ({@code "12"}): a row matches when its 1-based
 *     index starts with the typed number; the <b>exact</b> index match is
 *     ranked first, then the other index matches, then title matches.
 *     {@link Result#outOfRange} is set when the typed number matches no index
 *     at all (beyond the deck) — the dialog then shows a subtle invalid state
 *     instead of navigating;</li>
 *     <li><b>anything else</b> ({@code "chap"}): case/diacritic-insensitive
 *     substring match on the resolved display {@link NTxSlideTitleIndex.Entry#title};</li>
 *     <li><b>mixed</b> ({@code "2"}, a title like {@code "Chapter 2"}): a row
 *     matches on index <b>or</b> title, with index matches ranked first.</li>
 * </ul>
 * Filtering is O(n) string work per keystroke over pre-resolved titles — cheap
 * for hundreds of slides, no title re-resolution.
 */
public final class SlideGotoFilter {

    /**
     * Filter outcome: the ordered matching entries plus the query
     * interpretation, for the dialog's rendering.
     */
    public static final class Result {
        public final List<NTxSlideTitleIndex.Entry> entries;
        /** the trimmed query was only digits */
        public final boolean digitsOnly;
        /** digits-only query matching no slide index (no exact, no prefix) */
        public final boolean outOfRange;

        Result(List<NTxSlideTitleIndex.Entry> entries, boolean digitsOnly, boolean outOfRange) {
            this.entries = Collections.unmodifiableList(entries);
            this.digitsOnly = digitsOnly;
            this.outOfRange = outOfRange;
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }
    }

    private SlideGotoFilter() {
    }

    /**
     * Filters and ranks {@code all} against {@code query}. A blank query
     * returns every entry in document order.
     */
    public static Result filter(List<NTxSlideTitleIndex.Entry> all, String query) {
        String q = query == null ? "" : query.trim();
        if (all == null) {
            all = Collections.emptyList();
        }
        if (q.isEmpty()) {
            return new Result(new ArrayList<>(all), false, false);
        }
        boolean digitsOnly = isDigits(q);
        String nq = norm(q);

        List<Ranked> ranked = new ArrayList<>();
        boolean anyExact = false;
        boolean anyPrefix = false;
        for (NTxSlideTitleIndex.Entry e : all) {
            String idx = Integer.toString(e.userIndex());
            boolean indexExact = digitsOnly && idx.equals(q);
            boolean indexPrefix = digitsOnly && idx.startsWith(q);
            boolean titleMatch = !nq.isEmpty() && norm(e.title).contains(nq);
            if (!indexExact && !indexPrefix && !titleMatch) {
                continue;
            }
            if (indexExact) {
                anyExact = true;
            }
            if (indexPrefix) {
                anyPrefix = true;
            }
            int rank = indexExact ? 0 : (indexPrefix ? 1 : 2);
            ranked.add(new Ranked(e, rank));
        }
        ranked.sort((a, b) -> a.rank != b.rank
                ? Integer.compare(a.rank, b.rank)
                : Integer.compare(a.entry.pageIndex, b.entry.pageIndex));

        List<NTxSlideTitleIndex.Entry> out = new ArrayList<>(ranked.size());
        for (Ranked r : ranked) {
            out.add(r.entry);
        }
        return new Result(out, digitsOnly, digitsOnly && !anyExact && !anyPrefix);
    }

    private static final class Ranked {
        final NTxSlideTitleIndex.Entry entry;
        final int rank;

        Ranked(NTxSlideTitleIndex.Entry entry, int rank) {
            this.entry = entry;
            this.rank = rank;
        }
    }

    private static boolean isDigits(String s) {
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

    /**
     * Case + diacritic insensitive normalization with whitespace runs
     * collapsed, built on the shared {@link NTxHighlights#normalize(String)} so
     * it stays consistent with the find feature.
     */
    static String norm(String s) {
        if (s == null) {
            return "";
        }
        String n = NTxHighlights.normalize(s).trim();
        StringBuilder sb = new StringBuilder(n.length());
        boolean prevSpace = false;
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (c == ' ') {
                if (!prevSpace && sb.length() > 0) {
                    sb.append(' ');
                }
                prevSpace = true;
            } else {
                sb.append(c);
                prevSpace = false;
            }
        }
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ' ') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}
