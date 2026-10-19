package net.thevpc.ntexup.engine.renderer.screen.navigation;

import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.elem.NElement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for the render-free slide title resolution:
 * the extraction from the compiled node tree, the pure fallback chain and the
 * section-name derivation from a source path.
 */
class SlideTitleResolverTest {

    @BeforeAll
    static void init() {
        Nuts.require();
    }

    // -- extraction ----------------------------------------------------

    @Test
    void explicitTitlePropIsExtracted() {
        DefaultNTxNode page = new DefaultNTxNode("page");
        page.setProperty("title", NElement.ofString("My Title"));

        SlideTitleResolver.Candidates c = SlideTitleResolver.extract(page);
        assertEquals("My Title", c.explicitTitle);
    }

    @Test
    void firstTextNodeIsExtracted() {
        DefaultNTxNode page = new DefaultNTxNode("page");
        DefaultNTxNode header = new DefaultNTxNode("group");
        header.addChild(DefaultNTxNode.ofText("Rationale"));
        header.addChild(DefaultNTxNode.ofText("second"));
        page.addChild(header);
        page.addChild(DefaultNTxNode.ofText("body"));

        SlideTitleResolver.Candidates c = SlideTitleResolver.extract(page);
        assertEquals("Rationale", c.firstText);
    }

    @Test
    void classTitleNodeIsExtracted() {
        DefaultNTxNode page = new DefaultNTxNode("page");
        DefaultNTxNode title = DefaultNTxNode.ofText("Cover");
        title.addStyleClass("title");
        page.addChild(DefaultNTxNode.ofText("something"));
        page.addChild(title);

        SlideTitleResolver.Candidates c = SlideTitleResolver.extract(page);
        assertEquals("Cover", c.classTitle);
    }

    // -- fallback chain ------------------------------------------------

    @Test
    void explicitTitleWins() {
        SlideTitleResolver.Candidates c = new SlideTitleResolver.Candidates();
        c.explicitTitle = "Explicit";
        c.classTitle = "Class";
        c.firstText = "Text";
        assertEquals("Explicit", SlideTitleResolver.resolve(c, "sec", 3));
    }

    @Test
    void classTitleBeatsFirstText() {
        SlideTitleResolver.Candidates c = new SlideTitleResolver.Candidates();
        c.classTitle = "Class";
        c.firstText = "Text";
        assertEquals("Class", SlideTitleResolver.resolve(c, "sec", 3));
    }

    @Test
    void firstTextBeatsSection() {
        SlideTitleResolver.Candidates c = new SlideTitleResolver.Candidates();
        c.firstText = "Text";
        assertEquals("Text", SlideTitleResolver.resolve(c, "sec", 3));
    }

    @Test
    void sectionUsedWhenUntitled() {
        SlideTitleResolver.Candidates c = new SlideTitleResolver.Candidates();
        assertEquals("sec (untitled)", SlideTitleResolver.resolve(c, "sec", 3));
    }

    @Test
    void slideNumberIsTheLastResort() {
        SlideTitleResolver.Candidates c = new SlideTitleResolver.Candidates();
        assertEquals("Slide 7", SlideTitleResolver.resolve(c, null, 7));
        assertEquals("Slide 1", SlideTitleResolver.resolve(null, "  ", 1));
    }

    @Test
    void onlyFirstLineOfATitleIsKept() {
        SlideTitleResolver.Candidates c = new SlideTitleResolver.Candidates();
        c.firstText = "Line one\nLine two";
        assertEquals("Line one", SlideTitleResolver.resolve(c, null, 1));
    }

    // -- section derivation -------------------------------------------

    @Test
    void sectionIsTheCleanedParentFolder() {
        assertEquals("text", SlideTitleResolver.sectionFromPath(
                "/a/b/02-pages/0040-elements/0010-text/0110-plain.ntx"));
        assertEquals("elements", SlideTitleResolver.sectionFromPath(
                "/a/b/0040-elements/0120-shapes.ntx"));
        assertEquals("cover", SlideTitleResolver.sectionFromPath(
                "/a/b/0001-cover/0001-page-cover.ntx"));
    }

    @Test
    void sectionFallsBackToFileStemWithoutFolder() {
        assertEquals("plain", SlideTitleResolver.sectionFromPath("0110-plain.ntx"));
        assertEquals("README", SlideTitleResolver.sectionFromPath("README"));
    }

    @Test
    void sectionIgnoresQuery() {
        assertEquals("cover", SlideTitleResolver.sectionFromPath(
                "/a/b/0001-cover/0001-page-cover.ntx?x=1"));
    }

    @Test
    void nullPathHasNoSection() {
        assertNull(SlideTitleResolver.sectionFromPath(null));
        assertNull(SlideTitleResolver.sectionFromPath("   "));
    }
}
