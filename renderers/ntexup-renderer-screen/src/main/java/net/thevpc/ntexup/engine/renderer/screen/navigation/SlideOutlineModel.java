package net.thevpc.ntexup.engine.renderer.screen.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Pure (Swing-free) grouping of a {@link NTxSlideTitleIndex} into the
 * chapters/sections shown by the outline explorer.
 * <p>
 * Slides keep deck order. Consecutive slides sharing the same
 * {@link NTxSlideTitleIndex.Entry#section} (case-insensitive; the index already
 * falls back from chapter metadata to the source folder) form one
 * {@link Group}. A section that re-appears later in the deck opens a new group
 * rather than pulling slides out of order. Slides without a section form an
 * unnamed group ({@link Group#name} {@code null}), rendered as top-level rows.
 * When the deck has at most one distinct section the outline is
 * {@link #isFlat() flat}: a single unnamed group holding every slide.
 */
public final class SlideOutlineModel {

    /**
     * One chapter/section of the outline: a run of consecutive slides.
     */
    public static final class Group {
        /** display name, or null for slides outside any section */
        public final String name;
        public final List<NTxSlideTitleIndex.Entry> entries;

        Group(String name, List<NTxSlideTitleIndex.Entry> entries) {
            this.name = name;
            this.entries = Collections.unmodifiableList(entries);
        }

        public int firstPageIndex() {
            return entries.isEmpty() ? -1 : entries.get(0).pageIndex;
        }

        @Override
        public String toString() {
            return name == null ? "" : name;
        }
    }

    private final List<Group> groups;
    private final boolean flat;
    private final int[] groupOfPage;
    private final int[] rowOfPage;

    private SlideOutlineModel(List<Group> groups, boolean flat) {
        this.groups = Collections.unmodifiableList(groups);
        this.flat = flat;
        int max = -1;
        for (Group g : groups) {
            for (NTxSlideTitleIndex.Entry e : g.entries) {
                max = Math.max(max, e.pageIndex);
            }
        }
        groupOfPage = new int[max + 1];
        rowOfPage = new int[max + 1];
        java.util.Arrays.fill(groupOfPage, -1);
        java.util.Arrays.fill(rowOfPage, -1);
        for (int gi = 0; gi < groups.size(); gi++) {
            List<NTxSlideTitleIndex.Entry> es = groups.get(gi).entries;
            for (int ri = 0; ri < es.size(); ri++) {
                int p = es.get(ri).pageIndex;
                if (p >= 0) {
                    groupOfPage[p] = gi;
                    rowOfPage[p] = ri;
                }
            }
        }
    }

    public static SlideOutlineModel of(NTxSlideTitleIndex index) {
        return of(index == null ? Collections.emptyList() : index.entries());
    }

    public static SlideOutlineModel of(List<NTxSlideTitleIndex.Entry> entries) {
        List<Group> groups = new ArrayList<>();
        if (entries == null || entries.isEmpty()) {
            return new SlideOutlineModel(groups, true);
        }
        java.util.Set<String> distinct = new java.util.HashSet<>();
        for (NTxSlideTitleIndex.Entry e : entries) {
            String k = key(e.section);
            if (k != null) {
                distinct.add(k);
            }
        }
        if (distinct.size() <= 1) {
            groups.add(new Group(null, new ArrayList<>(entries)));
            return new SlideOutlineModel(groups, true);
        }
        String currentKey = null;
        String currentName = null;
        List<NTxSlideTitleIndex.Entry> current = null;
        for (NTxSlideTitleIndex.Entry e : entries) {
            String k = key(e.section);
            if (current == null || !java.util.Objects.equals(k, currentKey)) {
                if (current != null) {
                    groups.add(new Group(currentName, current));
                }
                current = new ArrayList<>();
                currentKey = k;
                currentName = k == null ? null : e.section.trim();
            }
            current.add(e);
        }
        groups.add(new Group(currentName, current));
        return new SlideOutlineModel(groups, false);
    }

    /**
     * Returns the outline restricted to {@code query}, keeping the grouping and
     * deck order of this model (empty groups are dropped). Blank query → this.
     * <ul>
     *     <li>digits only ({@code "12"}): slides whose 1-based number starts
     *     with it, or whose text contains it;</li>
     *     <li>otherwise every whitespace-separated word must occur
     *     (case/diacritic-insensitive) in the slide's title, subtitle, section
     *     or source file name; a group whose name matches every word keeps all
     *     its slides.</li>
     * </ul>
     */
    public SlideOutlineModel filter(String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return this;
        }
        String[] words = SlideGotoFilter.norm(q).split(" ");
        boolean digits = q.chars().allMatch(Character::isDigit);
        List<Group> out = new ArrayList<>();
        for (Group g : groups) {
            boolean groupMatch = g.name != null && matchesAll(SlideGotoFilter.norm(g.name), words);
            List<NTxSlideTitleIndex.Entry> kept = new ArrayList<>();
            for (NTxSlideTitleIndex.Entry e : g.entries) {
                if (groupMatch
                        || (digits && Integer.toString(e.userIndex()).startsWith(q))
                        || matchesAll(haystack(e), words)) {
                    kept.add(e);
                }
            }
            if (!kept.isEmpty()) {
                out.add(new Group(g.name, kept));
            }
        }
        return new SlideOutlineModel(out, flat);
    }

    private static String haystack(NTxSlideTitleIndex.Entry e) {
        StringBuilder sb = new StringBuilder(SlideGotoFilter.norm(e.title));
        if (e.subtitle != null) {
            sb.append(' ').append(SlideGotoFilter.norm(e.subtitle));
        }
        if (e.section != null) {
            sb.append(' ').append(SlideGotoFilter.norm(e.section));
        }
        if (e.sourceFileName != null) {
            sb.append(' ').append(SlideGotoFilter.norm(e.sourceFileName));
        }
        return sb.toString();
    }

    private static boolean matchesAll(String haystack, String[] words) {
        for (String w : words) {
            if (!w.isEmpty() && !haystack.contains(w)) {
                return false;
            }
        }
        return true;
    }

    private static String key(String section) {
        if (section == null) {
            return null;
        }
        String s = section.trim();
        return s.isEmpty() ? null : s.toLowerCase(Locale.ROOT);
    }

    public List<Group> groups() {
        return groups;
    }

    /** true when there is a single unnamed group (no section headers) */
    public boolean isFlat() {
        return flat;
    }

    /** index in {@link #groups()} of the group holding {@code pageIndex}, or -1 */
    public int groupIndexOf(int pageIndex) {
        return pageIndex >= 0 && pageIndex < groupOfPage.length ? groupOfPage[pageIndex] : -1;
    }

    /** row of {@code pageIndex} inside its group, or -1 */
    public int rowIndexOf(int pageIndex) {
        return pageIndex >= 0 && pageIndex < rowOfPage.length ? rowOfPage[pageIndex] : -1;
    }

    public int slideCount() {
        int n = 0;
        for (Group g : groups) {
            n += g.entries.size();
        }
        return n;
    }
}
