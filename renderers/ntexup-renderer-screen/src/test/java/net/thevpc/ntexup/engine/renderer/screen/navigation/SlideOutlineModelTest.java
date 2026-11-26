package net.thevpc.ntexup.engine.renderer.screen.navigation;

import net.thevpc.nuts.Nuts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Section grouping used by the outline explorer: consecutive runs, deck order
 * preserved, case-insensitive section keys, flat outline for single-section
 * decks, and page → group/row lookup.
 */
class SlideOutlineModelTest {

    @BeforeAll
    static void init() {
        Nuts.require();
    }

    private static NTxSlideTitleIndex.Entry e(int pageIndex, String title, String section) {
        return new NTxSlideTitleIndex.Entry(pageIndex, title, section);
    }

    @Test
    void groupsConsecutiveSlidesBySection() {
        List<NTxSlideTitleIndex.Entry> es = Arrays.asList(
                e(0, "Cover", null),
                e(1, "Why", "intro"),
                e(2, "What", "Intro"),
                e(3, "Boxes", "elements"),
                e(4, "Text", "elements"),
                e(5, "Thanks", "intro"));
        SlideOutlineModel m = SlideOutlineModel.of(es);
        assertFalse(m.isFlat());
        assertEquals(4, m.groups().size());
        assertNull(m.groups().get(0).name);
        assertEquals("intro", m.groups().get(1).name);
        assertEquals(2, m.groups().get(1).entries.size(), "case-insensitive section key");
        assertEquals("elements", m.groups().get(2).name);
        // a section re-appearing later opens a new group (deck order kept)
        assertEquals("intro", m.groups().get(3).name);
        assertEquals(5, m.groups().get(3).firstPageIndex());
        assertEquals(6, m.slideCount());

        assertEquals(2, m.groupIndexOf(4));
        assertEquals(1, m.rowIndexOf(4));
        assertEquals(0, m.groupIndexOf(0));
        assertEquals(-1, m.groupIndexOf(42));
        assertEquals(-1, m.rowIndexOf(-1));
    }

    @Test
    void singleSectionIsFlat() {
        SlideOutlineModel m = SlideOutlineModel.of(Arrays.asList(
                e(0, "A", "only"), e(1, "B", "Only "), e(2, "C", null)));
        assertTrue(m.isFlat());
        assertEquals(1, m.groups().size());
        assertNull(m.groups().get(0).name);
        assertEquals(3, m.groups().get(0).entries.size());
    }

    @Test
    void emptyOutline() {
        SlideOutlineModel m = SlideOutlineModel.of(Collections.emptyList());
        assertTrue(m.isFlat());
        assertTrue(m.groups().isEmpty());
        assertEquals(-1, m.groupIndexOf(0));
        assertEquals(0, SlideOutlineModel.of((NTxSlideTitleIndex) null).slideCount());
    }

    @Test
    void filterKeepsGroupingAndOrder() {
        List<NTxSlideTitleIndex.Entry> es = Arrays.asList(
                new NTxSlideTitleIndex.Entry(0, "Cover", null, "0000-cover.ntx", null),
                new NTxSlideTitleIndex.Entry(1, "Antenna Basics", "Antennas", null, "Dipôle pattern"),
                new NTxSlideTitleIndex.Entry(2, "Directivity", "Antennas", null, null),
                new NTxSlideTitleIndex.Entry(3, "Thanks", "Closing", null, null),
                new NTxSlideTitleIndex.Entry(11, "Appendix", "Closing", null, null));
        SlideOutlineModel m = SlideOutlineModel.of(es);

        assertTrue(m == m.filter("  "), "blank filter is identity");

        // title words, case-insensitive, AND semantics
        SlideOutlineModel f = m.filter("antenna BAS");
        assertEquals(1, f.slideCount());
        assertEquals("Antennas", f.groups().get(0).name);

        // subtitle, diacritic-insensitive
        assertEquals(1, m.filter("dipole").slideCount());
        // source file name
        assertEquals(0, m.filter("cover.ntx").groups().get(0).firstPageIndex());
        // section name keeps the whole group
        SlideOutlineModel s = m.filter("closing");
        assertEquals(1, s.groups().size());
        assertEquals(2, s.slideCount());
        // digits: 1-based number prefix ("1" → slides 1 and 12)
        SlideOutlineModel d = m.filter("1");
        assertEquals(2, d.slideCount());
        assertEquals(0, d.groupIndexOf(0));
        assertEquals(1, d.groupIndexOf(11));
        // no match
        assertTrue(m.filter("zzz").groups().isEmpty());
    }
}
