package net.thevpc.ntexup.engine.renderer.screen.navigation;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.nuts.io.NPath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lightweight, render-free outline of a document's slides: one
 * {@link Entry} per {@link NTxCompiledPage}, carrying its 0-based page index,
 * resolved display {@link Entry#title} and optional {@link Entry#section}.
 * <p>
 * Built once per document load (on the viewer's async loader thread, like the
 * find index) and cached by the viewer. Only titles are resolved — no full
 * text indexing — so it stays cheap for hundreds of slides and is safe to
 * reuse by other features (Find, outline, ...).
 *
 * @see SlideTitleResolver
 */
public final class NTxSlideTitleIndex {

    /**
     * One slide of the outline. Immutable; built once.
     */
    public static final class Entry {
        /** 0-based page index (what {@code showPage} takes) */
        public final int pageIndex;
        /** resolved display title, never blank */
        public final String title;
        /** section name (source folder), may be null */
        public final String section;

        public Entry(int pageIndex, String title, String section) {
            this.pageIndex = pageIndex;
            this.title = title;
            this.section = section;
        }

        /** 1-based number as shown by the viewer's slide counter */
        public int userIndex() {
            return pageIndex + 1;
        }

        @Override
        public String toString() {
            return userIndex() + " " + title;
        }
    }

    private final List<Entry> entries;
    private final boolean grouped;

    private NTxSlideTitleIndex(List<Entry> entries, boolean grouped) {
        this.entries = Collections.unmodifiableList(entries);
        this.grouped = grouped;
    }

    /**
     * Builds the outline. Forces lazy page compilation through
     * {@link NTxCompiledDocument#pages()} (already done by the viewer at load
     * time); never renders.
     */
    public static NTxSlideTitleIndex build(NTxCompiledDocument document) {
        List<Entry> entries = new ArrayList<>();
        if (document != null) {
            for (NTxCompiledPage page : document.pages()) {
                int pageIndex = page.index();
                String section = null;
                String title;
                try {
                    section = SlideTitleResolver.sectionFromPath(sourcePath(page));
                    SlideTitleResolver.Candidates c = SlideTitleResolver.extract(page.compiledPage());
                    title = SlideTitleResolver.resolve(c, section, pageIndex + 1);
                } catch (Throwable ex) {
                    title = SlideTitleResolver.resolve(null, section, pageIndex + 1);
                }
                entries.add(new Entry(pageIndex, title, section));
            }
        }
        return new NTxSlideTitleIndex(entries, computeGrouped(entries));
    }

    /**
     * True when the outline spans more than one non-null section: only then is
     * showing the section per row useful.
     */
    private static boolean computeGrouped(List<Entry> entries) {
        Set<String> sections = new HashSet<>();
        for (Entry e : entries) {
            if (e.section != null && !e.section.isEmpty()) {
                sections.add(e.section.toLowerCase());
                if (sections.size() > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Entry> entries() {
        return entries;
    }

    public int size() {
        return entries.size();
    }

    /** True when slides are grouped in more than one section */
    public boolean isGrouped() {
        return grouped;
    }

    private static String sourcePath(NTxCompiledPage page) {
        Object s = page.source();
        if (s == null && page.rawPage() != null) {
            s = NTxUtils.sourceOf(page.rawPage());
        }
        if (s instanceof NTxSource) {
            NPath path = ((NTxSource) s).path().orNull();
            return path == null ? null : path.toString();
        }
        if (s instanceof String) {
            return (String) s;
        }
        return null;
    }
}
