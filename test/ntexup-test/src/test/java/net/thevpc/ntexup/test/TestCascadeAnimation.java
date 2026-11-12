package net.thevpc.ntexup.test;

import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.api.renderer.NtxAnimationTimer;
import net.thevpc.ntexup.api.renderer.NtxPageLifecycle;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.nuts.Nuts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestCascadeAnimation {

    private static final String DECK = "page{\n"
            + "  rectangle(name:\"a\", at:(10,40), size:(20,20), background-color:white, draw-contour)\n"
            + "  rectangle(name:\"b\", at:(70,40), size:(20,20), background-color:white, draw-contour)\n"
            + "  connector(\"a\", \"b\", flow:dots(count:3,size:2,period:5), stroke:2)\n"
            + "}\n";

    @BeforeAll
    static void initializeNuts() {
        System.setProperty("java.awt.headless", "true");
        Nuts.require();
    }

    @Test
    void animatedFramesTrackVirtualTimeAndRemainDeterministicAtTheSameTime() {
        NTxEngine engine = new DefaultNTxEngine();
        NTxCompiledPage page = engine.loadDocument(new ByteArrayInputStream(
                DECK.getBytes(StandardCharsets.UTF_8))).page(0).get();
        TestLifecycle lifecycle = new TestLifecycle();
        NTxNodeRendererConfig config = new NTxNodeRendererConfig(600, 400)
                .withAnimate(true)
                .withPrint(true)
                .setCapability(NtxPageLifecycle.CAPABILITY_PAGE_LIFECYCLE, lifecycle);

        lifecycle.elapsedMillis = 0;
        int[] frame0 = pixels(engine.renderImage(page, config));
        int framesRequestedAt0 = lifecycle.requestedFrames;
        int[] repeatedFrame0 = pixels(engine.renderImage(page, config));
        assertArrayEquals(frame0, repeatedFrame0, "same animation time must render identically");
        assertTrue(framesRequestedAt0 > 0, "animated connector should request another frame");

        lifecycle.elapsedMillis = 1_000;
        int[] frame1 = pixels(engine.renderImage(page, config));
        lifecycle.elapsedMillis = 2_000;
        int[] frame2 = pixels(engine.renderImage(page, config));
        assertFalse(Arrays.equals(frame0, frame1), "animation frame at t=1 should differ from t=0");
        assertFalse(Arrays.equals(frame1, frame2), "animation frame at t=2 should differ from t=1");
    }

    @Test
    void staticRenderingDoesNotDependOnVirtualAnimationTime() {
        NTxEngine engine = new DefaultNTxEngine();
        NTxCompiledPage page = engine.loadDocument(new ByteArrayInputStream(
                DECK.getBytes(StandardCharsets.UTF_8))).page(0).get();
        TestLifecycle lifecycle = new TestLifecycle();
        NTxNodeRendererConfig config = new NTxNodeRendererConfig(600, 400)
                .withAnimate(false)
                .withPrint(true)
                .setCapability(NtxPageLifecycle.CAPABILITY_PAGE_LIFECYCLE, lifecycle);

        lifecycle.elapsedMillis = 0;
        int[] first = pixels(engine.renderImage(page, config));
        lifecycle.elapsedMillis = 2_000;
        int[] later = pixels(engine.renderImage(page, config));
        assertArrayEquals(first, later, "static output must use a deterministic frame");
        assertEquals(0, lifecycle.requestedFrames);
    }

    @Test
    void pageRelativeSizesAreRecomputedForEachRenderSize() {
        NTxEngine engine = new DefaultNTxEngine();
        NTxCompiledPage page = engine.loadDocument(new ByteArrayInputStream(
                ("page{rectangle(at:(10,10), size:(25%P,25%P), background-color:red)}")
                        .getBytes(StandardCharsets.UTF_8))).page(0).get();

        int[] small = renderPixels(engine, page, 600, 400, false);
        int[] large = renderPixels(engine, page, 1200, 800, false);

        int smallRedPixels = countRedPixels(small);
        int largeRedPixels = countRedPixels(large);
        assertTrue(smallRedPixels > 0, "page-relative rectangle must render");
        assertTrue(largeRedPixels > smallRedPixels * 3,
                "doubling render dimensions should grow page-relative area; got "
                        + smallRedPixels + " -> " + largeRedPixels);

        int[] legacyLarge;
        String oldLegacy = System.getProperty("ntexup.style.legacy");
        try {
            System.setProperty("ntexup.style.legacy", "true");
            legacyLarge = renderPixels(engine, page, 1200, 800, false);
        } finally {
            if (oldLegacy == null) {
                System.clearProperty("ntexup.style.legacy");
            } else {
                System.setProperty("ntexup.style.legacy", oldLegacy);
            }
        }
        assertArrayEquals(legacyLarge, large,
                "resized page-relative rendering must match the legacy resolver");
    }

    private static int[] renderPixels(NTxEngine engine, NTxCompiledPage page,
                                     int width, int height, boolean animate) {
        return pixels(engine.renderImage(page, new NTxNodeRendererConfig(width, height)
                .withAnimate(animate).withPrint(true)));
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static int countRedPixels(int[] pixels) {
        int count = 0;
        for (int pixel : pixels) {
            if ((pixel & 0x00ffffff) == 0x00ff0000) {
                count++;
            }
        }
        return count;
    }

    private static final class TestLifecycle implements NtxPageLifecycle {
        private long elapsedMillis;
        private int requestedFrames;

        @Override
        public int pageIndex() {
            return 0;
        }

        @Override
        public boolean isShowing() {
            return true;
        }

        @Override
        public long showStartTime() {
            return System.currentTimeMillis() - elapsedMillis;
        }

        @Override
        public long showElapsedMillis() {
            return elapsedMillis;
        }

        @Override
        public void requestFrame() {
            requestedFrames++;
        }

        @Override
        public NtxAnimationTimer newTimer(Object owner, long periodMs, Runnable tick) {
            return NtxAnimationTimer.NONE;
        }
    }
}
