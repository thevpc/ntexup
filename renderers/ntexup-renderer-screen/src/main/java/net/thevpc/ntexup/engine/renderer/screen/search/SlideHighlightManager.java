package net.thevpc.ntexup.engine.renderer.screen.search;

import net.thevpc.ntexup.api.document.node.NTxHighlights;
import net.thevpc.ntexup.api.document.node.NTxNodeHighlight;
import net.thevpc.ntexup.engine.renderer.screen.DocumentView;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MVC controller of the Find feature: owns the query palette (committed chips
 * + the live input query) and is the only writer of the per-node highlight
 * model ({@link NTxHighlights}). After any query change it re-applies the
 * spans to every indexed node and asks the viewer to repaint the current page;
 * off-screen pages pick the model up on their next render (navigation forces
 * one).
 */
public class SlideHighlightManager {

    /**
     * Chip palette, alpha 102 (~40%). A query keeps its slot until removed;
     * slots are handed out by a modulo counter so removing a chip never
     * reshuffles the others.
     */
    public static final Color[] PALETTE = {
            new Color(245, 158, 11, 102),   // Amber
            new Color(16, 185, 129, 102),   // Emerald
            new Color(6, 182, 212, 102),    // Cyan
            new Color(244, 63, 94, 102),    // Rose
            new Color(139, 92, 246, 102)    // Purple
    };

    /**
     * One search term + its stable palette color, with precomputed normalized
     * needles for painting and scoring.
     */
    public static class HighlightQuery {
        private final String term;
        private final String normalized;
        private final List<String> needles;
        private final Color color;

        public HighlightQuery(String term, Color color) {
            this.term = term;
            this.color = color;
            this.normalized = NTxHighlights.normalize(term).trim();
            this.needles = Collections.unmodifiableList(NTxHighlights.needlesOf(term));
        }

        /** raw user input (trimmed) */
        public String getTerm() {
            return term;
        }

        /** normalized + trimmed query (dedup key, scoring needle) */
        public String getNormalized() {
            return normalized;
        }

        /** normalized phrase + word needles */
        public List<String> getNeedles() {
            return needles;
        }

        public Color getColor() {
            return color;
        }
    }

    private final DocumentView documentView;
    private final List<HighlightQuery> queries = new ArrayList<>();
    private HighlightQuery liveQuery;
    private int colorCounter;
    private NTxSlideIndex index;

    public SlideHighlightManager(DocumentView documentView) {
        this.documentView = documentView;
    }

    public NTxSlideIndex getIndex() {
        return index;
    }

    public void setIndex(NTxSlideIndex index) {
        this.index = index;
    }

    /** committed queries (chips), in creation order */
    public List<HighlightQuery> getQueries() {
        return new ArrayList<>(queries);
    }

    public HighlightQuery getLiveQuery() {
        return liveQuery;
    }

    /**
     * Updates the live query (the dialog input). It keeps its palette slot
     * while the user keeps typing; blank input drops it.
     */
    public void setLiveQuery(String term) {
        String t = term == null ? "" : term.trim();
        if (t.isEmpty()) {
            liveQuery = null;
            return;
        }
        if (liveQuery != null && liveQuery.getTerm().equals(t)) {
            return;
        }
        Color c = liveQuery != null ? liveQuery.getColor() : nextColor();
        liveQuery = new HighlightQuery(t, c);
    }

    /**
     * Converts the live query into a committed chip (unless an identical chip
     * already exists, in which case it is dropped to avoid duplicates).
     */
    public void commitLiveQuery() {
        if (liveQuery == null) {
            return;
        }
        for (HighlightQuery q : queries) {
            if (q.getNormalized().equals(liveQuery.getNormalized())) {
                liveQuery = null;
                return;
            }
        }
        queries.add(liveQuery);
        liveQuery = null;
    }

    public void removeQuery(HighlightQuery q) {
        queries.remove(q);
    }

    /** committed chips + live query (what is currently highlighted) */
    public List<HighlightQuery> activeQueries() {
        List<HighlightQuery> all = new ArrayList<>(queries);
        if (liveQuery != null) {
            all.add(liveQuery);
        }
        return all;
    }

    public boolean hasActiveQueries() {
        return !queries.isEmpty() || liveQuery != null;
    }

    /**
     * Clears every query and unbinds all node highlights (ESC reset).
     */
    public void clearAll() {
        queries.clear();
        liveQuery = null;
        colorCounter = 0;
        NTxSlideIndex idx = index;
        if (idx != null) {
            for (NTxSlideIndex.SlideEntry e : idx.entries()) {
                for (NTxSlideIndex.NodeText nt : e.nodeTexts) {
                    NTxHighlights.clear(nt.node);
                }
            }
        }
        documentView.repaintCurrentPage();
    }

    /**
     * Rebinds the highlight model on every indexed node: a node gets one span
     * per matching query (with that query's color + needles), only when its
     * own extracted text contains at least one of the query's needles.
     * Then repaints the current page.
     */
    public void applyToDocument() {
        NTxSlideIndex idx = index;
        if (idx == null) {
            return;
        }
        List<HighlightQuery> active = activeQueries();
        for (NTxSlideIndex.SlideEntry e : idx.entries()) {
            for (NTxSlideIndex.NodeText nt : e.nodeTexts) {
                List<NTxNodeHighlight> spans = null;
                if (!active.isEmpty() && !nt.normText.isEmpty()) {
                    for (HighlightQuery q : active) {
                        List<String> matching = new ArrayList<>();
                        for (String needle : q.getNeedles()) {
                            if (!needle.isEmpty() && nt.normText.contains(needle)) {
                                matching.add(needle);
                            }
                        }
                        if (!matching.isEmpty()) {
                            if (spans == null) {
                                spans = new ArrayList<>();
                            }
                            spans.add(new NTxNodeHighlight(q.getColor(), nt.wholeNode,
                                    matching.toArray(new String[0])));
                        }
                    }
                }
                NTxHighlights.set(nt.node, spans);
            }
        }
        documentView.repaintCurrentPage();
    }

    private Color nextColor() {
        Color c = PALETTE[colorCounter % PALETTE.length];
        colorCounter++;
        return c;
    }
}
