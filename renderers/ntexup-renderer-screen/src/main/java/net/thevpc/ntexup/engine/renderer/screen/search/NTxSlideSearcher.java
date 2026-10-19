package net.thevpc.ntexup.engine.renderer.screen.search;

import net.thevpc.ntexup.api.document.node.NTxHighlights;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scoring + snippet builder for the Find dialog. Case/diacritic/punctuation
 * insensitive thanks to {@link NTxHighlights#normalize(String)}:
 * <ul>
 *     <li>normalized query contained as a phrase → 100;</li>
 *     <li>all query words contained → 80 (multi-word) / 60 (single word);</li>
 *     <li>fuzzy word-level match (bounded Levenshtein: ≤2 for long words,
 *     ≤1 otherwise) → 30.</li>
 * </ul>
 * A result is produced per (slide, query) pair so each entry keeps its
 * query color.
 */
public class NTxSlideSearcher {

    public static class SlideSearchResult {
        public final int pageIndex;
        public final int totalSlides;
        public final String title;
        public final String kicker;
        public final String sourceFileName;
        /**
         * Raw window (+ ellipses) around the first match; newlines collapsed.
         */
        public final String snippet;
        /**
         * Match ranges within {@link #snippet} (index space includes the
         * leading ellipsis when present).
         */
        public final List<int[]> snippetRanges;
        public final Color queryColor;
        public final String queryTerm;
        public final int score;

        public SlideSearchResult(int pageIndex, int totalSlides, String title, String kicker,
                                 String sourceFileName, String snippet, List<int[]> snippetRanges,
                                 Color queryColor, String queryTerm, int score) {
            this.pageIndex = pageIndex;
            this.totalSlides = totalSlides;
            this.title = title;
            this.kicker = kicker;
            this.sourceFileName = sourceFileName;
            this.snippet = snippet;
            this.snippetRanges = Collections.unmodifiableList(snippetRanges);
            this.queryColor = queryColor;
            this.queryTerm = queryTerm;
            this.score = score;
        }
    }

    /**
     * Searches every slide for every query. Live input searches only its own
     * (single) query; committed chips are searched as the union.
     */
    public List<SlideSearchResult> search(NTxSlideIndex index, List<SlideHighlightManager.HighlightQuery> queries) {
        List<SlideSearchResult> out = new ArrayList<>();
        if (index == null || queries == null || queries.isEmpty()) {
            return out;
        }
        int total = index.size();
        for (NTxSlideIndex.SlideEntry e : index.entries()) {
            for (SlideHighlightManager.HighlightQuery q : queries) {
                if (q == null || q.getNormalized().isEmpty()) {
                    continue;
                }
                int score = score(e, q.getNormalized());
                if (score <= 0) {
                    continue;
                }
                out.add(buildResult(e, q, score, total));
            }
        }
        out.sort((a, b) -> b.score != a.score
                ? Integer.compare(b.score, a.score)
                : Integer.compare(a.pageIndex, b.pageIndex));
        return out;
    }

    private static int score(NTxSlideIndex.SlideEntry e, String nq) {
        String nt = e.normText;
        if (nt.isEmpty()) {
            return 0;
        }
        if (nq.indexOf(' ') >= 0 && nt.contains(nq)) {
            return 100;
        }
        List<String> words = wordsOf(nq);
        if (words.isEmpty()) {
            return 0;
        }
        boolean all = true;
        for (String w : words) {
            if (!nt.contains(w)) {
                all = false;
                break;
            }
        }
        if (all) {
            return words.size() > 1 ? 80 : 60;
        }
        // fuzzy fallback: AND semantics are preserved — every query word must
        // be found (substring containment or bounded edit distance against a
        // slide word), otherwise "word1 zzzqqq" would still match any slide
        // containing word1
        for (String qw : words) {
            if (nt.contains(qw)) {
                continue;
            }
            boolean hit = false;
            if (qw.length() >= 4) {
                int max = qw.length() >= 8 ? 2 : 1;
                for (String w : e.words) {
                    if (Math.abs(w.length() - qw.length()) > max) {
                        continue;
                    }
                    if (boundedLevenshtein(w, qw, max) >= 0) {
                        hit = true;
                        break;
                    }
                }
            }
            if (!hit) {
                return 0;
            }
        }
        return 30;
    }

    private static SlideSearchResult buildResult(NTxSlideIndex.SlideEntry e,
                                                 SlideHighlightManager.HighlightQuery q,
                                                 int score, int total) {
        String raw = e.rawText;
        int matchStart = -1;
        int matchEnd = -1;
        for (String needle : q.getNeedles()) {
            for (int[] r : NTxHighlights.findMatches(raw, needle)) {
                if (matchStart < 0 || r[0] < matchStart) {
                    matchStart = r[0];
                    matchEnd = r[1];
                }
            }
        }
        String snippet;
        List<int[]> ranges = new ArrayList<>();
        if (matchStart >= 0) {
            int win = 42;
            int start = Math.max(0, matchStart - win);
            int end = Math.min(raw.length(), Math.max(matchEnd, matchStart + 1) + win);
            String snip = raw.substring(start, end)
                    .replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
            StringBuilder sb = new StringBuilder();
            if (start > 0) {
                sb.append('…');
            }
            int off = sb.length();
            sb.append(snip);
            if (end < raw.length()) {
                sb.append('…');
            }
            for (String needle : q.getNeedles()) {
                for (int[] r : NTxHighlights.findMatches(snip, needle)) {
                    ranges.add(new int[]{r[0] + off, r[1] + off});
                }
            }
            snippet = sb.toString();
        } else {
            snippet = raw.replace('\n', ' ').trim();
            if (snippet.length() > 90) {
                snippet = snippet.substring(0, 90) + "…";
            }
        }
        return new SlideSearchResult(e.pageIndex, total, e.title, e.kicker, e.sourceFileName,
                snippet, ranges, q.getColor(), q.getTerm(), score);
    }

    private static List<String> wordsOf(String nq) {
        List<String> words = new ArrayList<>();
        for (String w : nq.split(" ")) {
            if (!w.isEmpty() && !words.contains(w)) {
                words.add(w);
            }
        }
        return words;
    }

    /**
     * Levenshtein distance with early exit: returns the distance when
     * {@code <= max}, otherwise {@code -1}.
     */
    static int boundedLevenshtein(String a, String b, int max) {
        if (Math.abs(a.length() - b.length()) > max) {
            return -1;
        }
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            int best = cur[0];
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
                best = Math.min(best, cur[j]);
            }
            if (best > max) {
                return -1;
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()] <= max ? prev[b.length()] : -1;
    }
}
