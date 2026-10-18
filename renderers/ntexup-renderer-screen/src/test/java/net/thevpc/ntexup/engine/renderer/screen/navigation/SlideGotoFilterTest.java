package net.thevpc.ntexup.engine.renderer.screen.navigation;

import net.thevpc.nuts.Nuts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure filter/rank used by the "Go to slide" navigator:
 * digits map to slide numbers (exact first), text filters titles with
 * case/diacritic insensitivity, and a mixed query matches on index or title.
 */
class SlideGotoFilterTest {

    @BeforeAll
    static void init() {
        Nuts.require();
    }

    private static NTxSlideTitleIndex.Entry e(int pageIndex, String title) {
        return new NTxSlideTitleIndex.Entry(pageIndex, title, null);
    }

    /** 0-based indexes 0.. -> 1-based numbers 1.. */
    private static List<NTxSlideTitleIndex.Entry> sample() {
        return new ArrayList<>(Arrays.asList(
                e(0, "Intro"),
                e(1, "Rationale"),
                e(2, "Hello World"),
                e(11, "Chapter 2"),
                e(19, "Text"),
                e(20, "Text Wrap"),
                e(30, "Schéma")
        ));
    }

    private static List<String> titles(List<NTxSlideTitleIndex.Entry> list) {
        List<String> out = new ArrayList<>();
        for (NTxSlideTitleIndex.Entry x : list) {
            out.add(x.userIndex() + ":" + x.title);
        }
        return out;
    }

    @Test
    void blankQueryShowsEverythingInOrder() {
        SlideGotoFilter.Result r = SlideGotoFilter.filter(sample(), "  ");
        assertFalse(r.digitsOnly);
        assertFalse(r.outOfRange);
        assertEquals(Arrays.asList(
                "1:Intro", "2:Rationale", "3:Hello World", "12:Chapter 2",
                "20:Text", "21:Text Wrap", "31:Schéma"), titles(r.entries));
    }

    @Test
    void digitsExactMatchComesFirst() {
        SlideGotoFilter.Result r = SlideGotoFilter.filter(sample(), "2");
        assertTrue(r.digitsOnly);
        assertFalse(r.outOfRange);
        // exact index 2 first, then index-prefix matches (20,21), then the
        // title match "Chapter 2" (index 12)
        assertEquals(Arrays.asList(
                "2:Rationale", "20:Text", "21:Text Wrap", "12:Chapter 2"), titles(r.entries));
    }

    @Test
    void digitsExactNarrowsToSingleRow() {
        SlideGotoFilter.Result r = SlideGotoFilter.filter(sample(), "12");
        assertFalse(r.outOfRange);
        assertEquals(Arrays.asList("12:Chapter 2"), titles(r.entries));
    }

    @Test
    void digitsOutOfRangeIsFlaggedAndEmpty() {
        SlideGotoFilter.Result r = SlideGotoFilter.filter(sample(), "99");
        assertTrue(r.digitsOnly);
        assertTrue(r.outOfRange);
        assertTrue(r.isEmpty());
    }

    @Test
    void zeroIsOutOfRangeBecauseSlidesAreOneBased() {
        SlideGotoFilter.Result r = SlideGotoFilter.filter(sample(), "0");
        assertTrue(r.outOfRange);
    }

    @Test
    void textIsASubstringMatchOnTitle() {
        SlideGotoFilter.Result r = SlideGotoFilter.filter(sample(), "text");
        assertFalse(r.digitsOnly);
        assertFalse(r.outOfRange);
        assertEquals(Arrays.asList("20:Text", "21:Text Wrap"), titles(r.entries));
    }

    @Test
    void textMatchIsCaseInsensitive() {
        SlideGotoFilter.Result r = SlideGotoFilter.filter(sample(), "CHAPTER 2");
        assertEquals(Arrays.asList("12:Chapter 2"), titles(r.entries));
    }

    @Test
    void textMatchIsDiacriticInsensitive() {
        SlideGotoFilter.Result r = SlideGotoFilter.filter(sample(), "schema");
        assertEquals(Arrays.asList("31:Schéma"), titles(r.entries));
    }

    @Test
    void noMatchYieldsEmptyResult() {
        SlideGotoFilter.Result r = SlideGotoFilter.filter(sample(), "zzz");
        assertFalse(r.digitsOnly);
        assertFalse(r.outOfRange);
        assertTrue(r.isEmpty());
    }

    @Test
    void nullQueryIsTreatedAsBlank() {
        SlideGotoFilter.Result r = SlideGotoFilter.filter(sample(), null);
        assertEquals(7, r.entries.size());
    }
}
