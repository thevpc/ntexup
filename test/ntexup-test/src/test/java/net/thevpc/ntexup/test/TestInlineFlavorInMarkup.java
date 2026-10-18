package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Inline text markup must span an embedded language region, and the markup (emphasis, color,
 * heading/palette styles, ...) must apply to the language region itself, as in
 * {@code hello *world [[eq: \alpha]]*}.
 * <p>
 * A real language (such as LaTeX {@code [[eq: ...]]}) is rendered as an image which a test cannot
 * inspect, so these tests install {@link ProbeTextFlavor}: a test-only flavor with the very same
 * shape (a {@code [[probe: ...]]} prefix, the {@code \(...\)} inline separators and a
 * {@code buildText} that receives the markup options). It records what the engine asked it to
 * render, which proves the flavor region survived the markup parse (and with which options).
 * <p>
 * Covered markers: {@code *italic*}, {@code **bold**}, {@code ***bold+italic***},
 * {@code ##...##} / {@code ###...###} heading styles, the {@code ##:p1 ...##} palette color,
 * the {@code ##:/ ...##} italic token, the {@code ##{...}##} color span, and their combinations.
 * {@code __...__} and {@code #...#} are treated by nuts {@code mtf} as literal text; the flavor
 * inside them must still render (it is simply not emphasized).
 * See {@code inline-flavor-markup.ntx}.
 */
public class TestInlineFlavorInMarkup {

    @BeforeAll
    static void init() {
        System.setProperty("java.awt.headless", "true");
        Nuts.require();
    }

    @Test
    public void flavorRegionIsRenderedAndInheritsItalicEmphasis() {
        renderDeck();
        ProbeTextFlavor.Call call = ProbeTextFlavor.call("X1");
        assertNotNull(call, "the flavor region inside *...* must still be rendered: " + ProbeTextFlavor.CALLS);
        assertEquals(Boolean.TRUE, call.italic(),
                "italic emphasis must be applied to the flavor region: " + call);
        assertNull(call.bold(), "no bold was requested: " + call);
    }

    @Test
    public void flavorRegionIsRenderedAndInheritsBoldEmphasis() {
        renderDeck();
        ProbeTextFlavor.Call call = ProbeTextFlavor.call("X2");
        assertNotNull(call, "the flavor region inside **...** must still be rendered: " + ProbeTextFlavor.CALLS);
        assertEquals(Boolean.TRUE, call.bold(),
                "bold emphasis must be applied to the flavor region: " + call);
        assertNull(call.italic(), "no italic was requested: " + call);
    }

    @Test
    public void inlineSeparatorsAreRenderedAndInheritEmphasis() {
        renderDeck();
        assertTrue(ProbeTextFlavor.hasCall("X3"),
                "the \\(...\\) inline separator must be rendered: " + ProbeTextFlavor.CALLS);

        ProbeTextFlavor.Call call = ProbeTextFlavor.call("X4");
        assertNotNull(call, "the \\(...\\) region inside *...* must still be rendered: " + ProbeTextFlavor.CALLS);
        assertEquals(Boolean.TRUE, call.italic(),
                "italic emphasis must be applied to the \\(...\\) region: " + call);
    }

    @Test
    public void markupColorIsAppliedToTheFlavorRegion() {
        renderDeck();
        ProbeTextFlavor.Call call = ProbeTextFlavor.call("X5");
        assertNotNull(call, "the flavor region inside a color span must still be rendered: " + ProbeTextFlavor.CALLS);
        assertNotNull(call.foregroundColor(),
                "the surrounding color must be applied to the flavor region: " + call);
    }

    @Test
    public void flavorRegionIsRenderedWithoutMarkup() {
        renderDeck();
        ProbeTextFlavor.Call call = ProbeTextFlavor.call("X6");
        assertNotNull(call, "a plain flavor region must be rendered: " + ProbeTextFlavor.CALLS);
        assertNull(call.bold(), "no bold was requested: " + call);
        assertNull(call.italic(), "no italic was requested: " + call);
    }

    @Test
    public void flavorRegionInsideACodeSpanStaysLiteral() {
        renderDeck();
        assertFalse(ProbeTextFlavor.hasCall("Z1"),
                "a flavor region inside `...` must stay literal: " + ProbeTextFlavor.CALLS);
    }

    @Test
    public void escapedFlavorRegionStaysLiteral() {
        renderDeck();
        assertFalse(ProbeTextFlavor.hasCall("Z2"),
                "a backslash escaped flavor region must stay literal: " + ProbeTextFlavor.CALLS);
    }

    @Test
    public void headingStylesApplyToTheFlavorRegion() {
        renderDeck();
        ProbeTextFlavor.Call b3 = ProbeTextFlavor.call("B3");
        assertNotNull(b3, "the flavor region inside ##...## must still be rendered: " + ProbeTextFlavor.CALLS);
        assertNotNull(b3.foregroundColor(), "the ##...## heading style must color the flavor region: " + b3);

        ProbeTextFlavor.Call b4 = ProbeTextFlavor.call("B4");
        assertNotNull(b4, "the flavor region inside ###...### must still be rendered: " + ProbeTextFlavor.CALLS);
        assertNotNull(b4.foregroundColor(), "the ###...### heading style must color the flavor region: " + b4);
    }

    @Test
    public void paletteStyleAppliesColorToTheFlavorRegion() {
        renderDeck();
        ProbeTextFlavor.Call b5 = ProbeTextFlavor.call("B5");
        assertNotNull(b5, "the flavor region inside ##:p1 ...## must still be rendered: " + ProbeTextFlavor.CALLS);
        assertNotNull(b5.foregroundColor(), "the palette color must be applied to the flavor region: " + b5);
    }

    @Test
    public void italicTokenAppliesItalicToTheFlavorRegion() {
        renderDeck();
        ProbeTextFlavor.Call b6 = ProbeTextFlavor.call("B6");
        assertNotNull(b6, "the flavor region inside ##:/ ...## must still be rendered: " + ProbeTextFlavor.CALLS);
        assertEquals(Boolean.TRUE, b6.italic(), "the italic token must apply to the flavor region: " + b6);
    }

    @Test
    public void combinedBoldAndColorApplyToTheFlavorRegion() {
        renderDeck();
        ProbeTextFlavor.Call b7 = ProbeTextFlavor.call("B7");
        assertNotNull(b7, "the flavor region must still be rendered: " + ProbeTextFlavor.CALLS);
        assertEquals(Boolean.TRUE, b7.bold(), "the surrounding bold must apply to the flavor region: " + b7);
        assertNotNull(b7.foregroundColor(), "the surrounding color must apply to the flavor region: " + b7);
    }

    @Test
    public void tripleAsteriskAppliesBoldAndItalicToTheFlavorRegion() {
        renderDeck();
        ProbeTextFlavor.Call b8 = ProbeTextFlavor.call("B8");
        assertNotNull(b8, "the flavor region inside ***...*** must still be rendered: " + ProbeTextFlavor.CALLS);
        assertEquals(Boolean.TRUE, b8.bold(), "bold must apply to the flavor region: " + b8);
        assertEquals(Boolean.TRUE, b8.italic(), "italic must apply to the flavor region: " + b8);
    }

    @Test
    public void flavorPayloadKeepsMarkupCharactersVerbatim() {
        renderDeck();
        ProbeTextFlavor.Call b9 = ProbeTextFlavor.call("B9 * _ # **");
        assertNotNull(b9, "a payload holding markup characters must reach the flavor unchanged: " + ProbeTextFlavor.CALLS);
        assertEquals(Boolean.TRUE, b9.italic(), "the emphasis around it still applies: " + b9);
    }

    @Test
    public void flavorStillRendersInsideMarkersMtfDoesNotImplement() {
        renderDeck();
        // mtf treats __...__ and #...# as literal text (no emphasis), so the flavor is just plain,
        // but it must still be resolved rather than dropped.
        assertNotNull(ProbeTextFlavor.call("B10"), "__it ...__ is literal, the flavor must still render: " + ProbeTextFlavor.CALLS);
        assertNotNull(ProbeTextFlavor.call("B11"), "#it ...# is literal, the flavor must still render: " + ProbeTextFlavor.CALLS);
    }

    private static void renderDeck() {
        ProbeTextFlavor.CALLS.clear();
        NTxEngine e = new DefaultNTxEngine();
        try (InputStream in = TestInlineFlavorInMarkup.class.getResourceAsStream("/inline-flavor-markup.ntx")) {
            assertNotNull(in, "missing test resource /inline-flavor-markup.ntx");
            NTxCompiledDocument d = e.loadDocument(in);
            assertEquals(1, d.pages().size());
            NTxCompiledPage p = d.page(0).get();
            e.renderImage(p, new NTxNodeRendererConfig(1600, 1200));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        assertFalse(ProbeTextFlavor.CALLS.isEmpty(),
                "the probe flavor was never invoked: it is probably not registered through META-INF/services");
    }
}
