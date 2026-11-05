package net.thevpc.ntexup.engine.renderer.htmlstatic;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.api.renderer.NtxPageLifecycle;
import net.thevpc.ntexup.lib.paintstream.PaintStreamImageAssets;
import net.thevpc.ntexup.lib.paintstream.PaintStreamRecorder;
import net.thevpc.ntexup.lib.paintstream.RecordingGraphics2D;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns one compiled page into the list of paint streams that a player has to
 * replay.
 * <p>
 * Every sample is a real java2d render of the page, painted at a virtual
 * elapsed time into a throw-away surface while {@link RecordingGraphics2D}
 * captures the primitive stream. The surface itself is discarded: only the
 * operation list ships. This is what makes the export equivalent to the screen
 * renderer rather than an approximation of it — the same node renderers, the
 * same layout and the same text shaping produce the stream, java decides the
 * geometry, and the browser only replays it.
 * <p>
 * Sampling stops at the horizon, or as soon as the stream is proven periodic:
 * a page that settles shows up as a one-frame period and ships only its last
 * frame, while a repeating animation ships exactly one cycle plus the frames
 * that lead into it, with the cycle bounds reported so the player can wrap.
 */
class HtmlStaticBaker {

    /**
     * Reported in {@link Result} when the player has to stop on the last frame
     * instead of wrapping.
     */
    static final long NO_LOOP = -1;

    /**
     * One sampled animation frame.
     */
    record Frame(long millis, List<NElement> ops) {
    }

    /**
     * The frames to ship, plus the animation window the player loops over.
     */
    record Result(List<Frame> frames, long loopStartMillis, long loopEndMillis) {
    }

    /**
     * Bake parameters of a single page.
     */
    record Options(int pixelWidth, int pixelHeight, boolean animate, long periodMillis,
                   long maxMillis, int settleFrames) {
    }

    private final NTxEngine engine;
    private final PaintStreamImageAssets assets;

    HtmlStaticBaker(NTxEngine engine, PaintStreamImageAssets assets) {
        this.engine = engine;
        this.assets = assets;
    }

    Result bake(NTxCompiledPage page, int pageIndex, Options options) {
        List<Frame> frames = new ArrayList<>();
        List<String> hashes = new ArrayList<>();
        Map<String, Integer> firstSeen = new HashMap<>();
        NTxCompiledDocument document = page.document();
        settleFutures(document, page);
        long millis = 0;
        int sample = 0;
        Cycle candidate = null;
        while (true) {
            List<NElement> ops = paint(page, pageIndex, options, millis, sample);
            int index = frames.size();
            frames.add(new Frame(millis, ops));
            String hash = opsHash(ops);
            hashes.add(hash);
            if (!options.animate()) {
                return new Result(frames, NO_LOOP, NO_LOOP);
            }
            if (candidate != null) {
                if (hash.equals(hashes.get(index - candidate.period))) {
                    candidate.confirmed++;
                    if (candidate.confirmed == candidate.needed) {
                        int kept = candidate.start + candidate.period;
                        List<Frame> shipped = new ArrayList<>(frames.subList(0, kept));
                        long loopStart = shipped.get(candidate.start).millis();
                        long loopEnd = shipped.get(kept - 1).millis();
                        if (candidate.period == 1) {
                            //a one-frame cycle is a settled page, not a loop
                            return new Result(shipped, NO_LOOP, NO_LOOP);
                        }
                        return new Result(shipped, loopStart, loopEnd);
                    }
                } else {
                    candidate = null;
                }
            }
            if (candidate == null) {
                Integer earlier = firstSeen.get(hash);
                if (earlier != null) {
                    int period = index - earlier;
                    if (period > 0) {
                        int settleConfirm = period == 1 ? Math.max(1, options.settleFrames() - 1) : 0;
                        candidate = new Cycle(earlier, period, period - 1 + settleConfirm, 0);
                    }
                }
                firstSeen.putIfAbsent(hash, index);
            }
            sample++;
            millis += options.periodMillis();
            if (millis > options.maxMillis()) {
                return new Result(frames, NO_LOOP, NO_LOOP);
            }
            settleFutures(document, page);
        }
    }

    /**
     * A cycle is only accepted once every frame of a full second repetition
     * matches the frame one period earlier, so two samples that happen to
     * coincide in the middle of an animation cannot truncate it. A one-frame
     * period is a page that settled rather than a loop, and additionally needs
     * {@code settleFrames} samples to agree.
     */
    private static final class Cycle {
        final int start;
        final int period;
        final int needed;
        int confirmed;

        Cycle(int start, int period, int needed, int confirmed) {
            this.start = start;
            this.period = period;
            this.needed = needed;
            this.confirmed = confirmed;
        }
    }

    /**
     * Paints one frame. The first sample of a page is rendered with the node
     * caches closed, exactly as the screen renderer paints a page it has never
     * seen, so async content cannot be hidden behind a stale cached image.
     */
    private List<NElement> paint(NTxCompiledPage page, int pageIndex, Options options, long millis, int sample) {
        NTxNodeRendererConfig config = new NTxNodeRendererConfig(options.pixelWidth(), options.pixelHeight());
        config.withAnimate(options.animate());
        config.setUseCache(sample > 0);
        VirtualPageLifecycle lifecycle = new VirtualPageLifecycle(pageIndex);
        lifecycle.setElapsedMillis(millis);
        if (options.animate()) {
            config.setCapability(NtxPageLifecycle.CAPABILITY_PAGE_LIFECYCLE, lifecycle);
        }
        BufferedImage surface = new BufferedImage(options.pixelWidth(), options.pixelHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D base = surface.createGraphics();
        PaintStreamRecorder recorder = new PaintStreamRecorder();
        recorder.setTextOutlines(true);
        recorder.setImages(assets);
        try {
            engine.renderPage(page, config,
                    new RecordingGraphics2D(base, recorder, options.pixelWidth(), options.pixelHeight(), false, null),
                    null, null);
        } finally {
            base.dispose();
        }
        List<NElement> ops = new ArrayList<>();
        flatten(recorder, ops);
        return ops;
    }

    /**
     * Mirrors DefaultNTxEngine.renderImage(): content produced by a pending
     * future only shows up after a second pass, and the node render cache has
     * to be dropped for that second pass to repaint instead of reusing the
     * placeholder frame.
     */
    private void settleFutures(NTxCompiledDocument document, NTxCompiledPage page) {
        if (document.hasPendingFutures()) {
            document.awaitFutures();
            page.compiledPage().invalidateRenderCache();
        }
    }

    /**
     * Group boundaries come from {@code Graphics2D.create()} and carry no
     * rendering state of their own — every leaf operation already holds its
     * absolute transform, clip and paint. Flattening in paint order is
     * therefore lossless and gives the codec a linear stream to diff.
     */
    private static void flatten(PaintStreamRecorder recorder, List<NElement> sink) {
        for (Object entry : recorder.getEntries()) {
            if (entry instanceof PaintStreamRecorder child) {
                flatten(child, sink);
            } else {
                sink.add((NElement) entry);
            }
        }
    }

    private static String opsHash(List<NElement> ops) {
        StringBuilder sb = new StringBuilder();
        sb.append(ops.size()).append('/');
        for (NElement op : ops) {
            sb.append(op.toString()).append('\n');
        }
        return sb.toString();
    }
}
