package net.thevpc.ntexup.test;

import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NElementReader;
import net.thevpc.nuts.io.NPath;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRendererConfig;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererConfig;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.engine.renderer.htmlstatic.HtmlStaticDocumentRenderer;
import net.thevpc.ntexup.lib.paintstream.PaintStreamCodec;
import net.thevpc.ntexup.lib.paintstream.PaintStreamDecoder;
import net.thevpc.ntexup.lib.paintstream.PaintStreamImageAssets;
import net.thevpc.ntexup.lib.paintstream.PaintStreamJson;
import net.thevpc.ntexup.lib.paintstream.PaintStreamRecorder;
import net.thevpc.ntexup.lib.paintstream.RecordingGraphics2D;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the java replay of a paint stream paints what java painted.
 *
 * A recorded stream is only worth shipping if it can be turned back into
 * pixels, and the browser is not the only consumer: {@link PaintStreamDecoder}
 * replays the same operations on a real {@code Graphics2D}. What is tested here
 * is the whole round trip, record, encode, write json, parse, decode, replay,
 * and the comparison is between rasters rather than between element trees.
 *
 * <p>
 * The comparison is deliberately low frequency: both rasters are box-averaged
 * down by four before blocks are matched. A 1:1 diff would be dominated by
 * glyph antialiasing, because the stream carries a glyph as an outline that has
 * to be filled while the engine drew it with text hinting, and every text page
 * would then read as a failure for a reason that has nothing to do with layout,
 * geometry or color.
 */
public class TestPaintStreamDecoder {

    /**
     * Two pages of what the engine really paints, so the shipped stream of a real
     * deck is compared against the real raster and not against a script.
     */
    private static final String DECK = "page{\n"
            + "  rectangle(name:\"a\", at:(10,10), size:(20,10), background-color:#2266aa, draw-contour){\n"
            + "    text(\"Alpha\", at:center)\n"
            + "  }\n"
            + "  rectangle(name:\"b\", at:(70,40), size:(20,10), background-color:white, draw-contour){\n"
            + "    text(\"Beta\", at:center)\n"
            + "  }\n"
            + "  connector(\"a\", \"b\", end-arrow:simple(), label:\"edge\")\n"
            + "  line(from:(10,70), to:(90,70), stroke:basic(dashed), width:0.5)\n"
            + "}\n"
            + "page{\n"
            + "  text(\"a page that never moves\", at:center)\n"
            + "}\n";

    private static final int W = 800;
    private static final int H = 600;
    private static final int DOWNSCALE = 4;
    /**
     * Channel tolerance of a downsampled block, the share of blocks allowed to
     * exceed it and the mean error budget. Measured, not guessed: a real deck
     * against the engine raster lands at a mean of 0.05 and two blocks, the
     * scripted scene at 0.14 and 0.12 % of blocks, and all of it is glyph
     * antialiasing, which is the one thing this comparison agrees to ignore.
     */
    private static final int BLOCK_TOLERANCE = 24;
    private static final double BLOCK_BUDGET = 0.005;
    private static final double MAX_MEAN_ERROR = 1.0;

    @BeforeAll
    static void init() {
        System.setProperty("java.awt.headless", "true");
        Nuts.require();
    }

    // ---------------------------------------------------------------- scripted

    /**
     * Records a scene that covers every operation kind, ships it through the wire
     * format, replays the decoded result and asks whether the pixels came back.
     */
    @Test
    public void recordedShippedThenReplayedPaintsTheSameScene() {
        BufferedImage logo = logo();
        PaintStreamImageAssets assets = new PaintStreamImageAssets();
        BufferedImage expected = drawDirect(logo);
        List<NElement> ops = record(logo, assets);
        assertEquals(ops.size(), shipAndDecode(ops).size(), "the round trip must not lose operations");

        BufferedImage actual = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        PaintStreamDecoder decoder = new PaintStreamDecoder(sha -> image(assets, sha));
        Graphics2D g = actual.createGraphics();
        try {
            decoder.replay(g, shipAndDecode(ops));
        } finally {
            g.dispose();
        }
        assertEquals(List.of(), List.copyOf(decoder.warnings()),
                "every operation of the scene must be replayable");
        assertLowFrequencyEqual(expected, actual, "scripted-scene");
    }

    /**
     * The stream is flat by contract: every operation carries its own absolute
     * transform, clip and paint, so a frame needs no other frame. Replaying one
     * after the frames before it must land on the same pixels as replaying it
     * alone, which is what lets a player seek and prefetch in any order.
     */
    @Test
    public void framesReplayIndependentlyOfTheFramesBeforeThem() {
        List<List<NElement>> frames = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            PaintStreamRecorder recorder = new PaintStreamRecorder();
            BufferedImage surface = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = surface.createGraphics();
            try {
                Graphics2D c = new RecordingGraphics2D(g, recorder, W, H, false, null);
                frame(c, i);
            } finally {
                g.dispose();
            }
            frames.add(flatten(recorder, new ArrayList<>()));
        }
        PaintStreamImageAssets none = new PaintStreamImageAssets();
        BufferedImage alone = replay(frames.get(2), none, 1.0);
        BufferedImage last = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        PaintStreamDecoder decoder = new PaintStreamDecoder(sha -> image(none, sha));
        Graphics2D g = last.createGraphics();
        try {
            for (int i = 0; i < frames.size(); i++) {
                decoder.replay(g, shipAndDecode(frames.get(i)));
            }
        } finally {
            g.dispose();
        }
        assertLowFrequencyEqual(alone, last, "frame-order");
    }

    /**
     * Scaling a replay multiplies the recorded affine on the left, so a frame at
     * double size is the same scene drawn sharper rather than the one-pixel raster
     * blown up; averaging it back down returns the original.
     */
    @Test
    public void scaledReplayIsTheSameGeometryNotAResampledBitmap() {
        BufferedImage logo = logo();
        PaintStreamImageAssets assets = new PaintStreamImageAssets();
        List<NElement> ops = record(logo, assets);
        BufferedImage once = replay(ops, assets, 1.0);
        BufferedImage twice = replay(ops, assets, 2.0);
        assertEquals(2 * W, twice.getWidth(), "the scaled surface is twice as wide");
        assertEquals(2 * H, twice.getHeight(), "and twice as high");
        assertLowFrequencyEqual(once, boxDownsample(twice, 2), "scaled-replay");
    }

    // ------------------------------------------------------------------ engine

    /**
     * The acceptance test of the java player: bake a deck with the real exporter,
     * read the shipped page back, replay it, and compare against the raster the
     * engine painted for the same page at the same pixel size.
     */
    @Test
    public void shippedDeckReplaysWhatTheEnginePainted() {
        NTxEngine engine = new DefaultNTxEngine();
        NTxCompiledDocument document = load(engine);
        NPath folder = tempFolder("equivalence");
        HtmlStaticDocumentRenderer renderer = htmlStatic(engine, folder);
        renderer.setProperty(HtmlStaticDocumentRenderer.PROP_ANIMATE, Boolean.FALSE);
        NElement manifest = renderer.writeDataBundle(document, folder);

        int width = (int) longField(manifest, "width");
        int height = (int) longField(manifest, "height");
        assertEquals(600, width, "the replay must match the geometry of the export");
        assertEquals(450, height);

        PaintStreamDecoder.Images pixels = folderImages(folder);
        for (int i = 0; i < document.pages().size(); i++) {
            NElement page = NElementReader.ofJson().read(new String(
                    folder.resolve("pages/page-" + String.format("%03d", i + 1) + ".json").readBytes(),
                    StandardCharsets.UTF_8));
            List<List<NElement>> frames = PaintStreamCodec.decode(page);
            assertEquals(1, frames.size(), "animate=false ships one frame per page");

            //the reference is the engine itself, at the exporter's pixel size
            NTxNodeRendererConfig config = new NTxNodeRendererConfig(width, height);
            config.withAnimate(false);
            config.setUseCache(false);
            BufferedImage expected = engine.renderImage(document.pages().get(i), config);

            BufferedImage actual = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            PaintStreamDecoder decoder = new PaintStreamDecoder(pixels);
            Graphics2D g = actual.createGraphics();
            try {
                decoder.replay(g, frames.get(0));
            } finally {
                g.dispose();
            }
            assertEquals(List.of(), List.copyOf(decoder.warnings()),
                    "page " + (i + 1) + " shipped an operation the java player cannot replay");
            assertLowFrequencyEqual(expected, actual, "deck-page-" + (i + 1));
        }
    }

    // ------------------------------------------------------------------- scene

    /**
     * One frame of the order-independence scene, moved and clipped by its index.
     */
    private static void frame(Graphics2D c, int i) {
        c.setColor(new Color(0xEFEFEF));
        c.fillRect(0, 0, W / 2, H);
        c.translate(40 * i, 25 * i);
        c.setColor(new Color(30 + 60 * i, 90, 160));
        c.fill(new Ellipse2D.Double(100, 100, 120, 80));
        Graphics2D g = (Graphics2D) c.create();
        g.setClip(new Rectangle2D.Double(60, 60, 300, 200));
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(6f));
        g.drawLine(40, 150, 400, 150);
        g.dispose();
    }

    /**
     * Paints every primitive family the recorder knows about through plain java2d
     * calls, so a decoder gap shows up as a wrong pixel rather than as an
     * operation kind nobody exercised.
     */
    private static void scene(Graphics2D g, BufferedImage logo) {
        g.setColor(new Color(0xF7F7F2));
        g.fillRect(0, 0, W, H);
        g.setColor(Color.BLACK);
        g.drawRect(8, 8, 150, 100);
        g.clearRect(200, 8, 60, 40);

        g.setColor(new Color(0x2266AA));
        g.fillRoundRect(8, 130, 160, 90, 40, 24);
        g.setColor(Color.DARK_GRAY);
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(190, 130, 160, 90, 30, 30);

        g.setColor(new Color(200, 60, 60));
        g.fillOval(370, 130, 140, 90);
        g.setColor(Color.BLACK);
        g.drawOval(8, 250, 140, 90);

        g.setColor(new Color(40, 140, 80));
        g.fillArc(170, 250, 120, 120, 20, 130);
        g.setStroke(new BasicStroke(4f));
        g.drawArc(310, 250, 120, 120, 200, 90);

        g.setColor(new Color(90, 40, 150));
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 11f,
                new float[]{12f, 6f}, 3f));
        g.drawLine(460, 250, 700, 300);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        g.drawLine(460, 320, 600, 360);

        g.setColor(new Color(220, 150, 40));
        g.fillPolygon(new int[]{8, 90, 150, 60}, new int[]{400, 380, 470, 520}, 4);
        g.setColor(new Color(20, 100, 140));
        g.setStroke(new BasicStroke(4f));
        g.drawPolygon(new int[]{200, 300, 340, 420}, new int[]{420, 400, 480, 540}, 4);
        g.setColor(new Color(140, 60, 160));
        g.drawPolyline(new int[]{460, 520, 560, 640}, new int[]{420, 460, 400, 460}, 4);

        g.setPaint(new LinearGradientPaint(new Point2D.Double(8, 540), new Point2D.Double(220, 590),
                new float[]{0f, 0.4f, 1f}, new Color[]{Color.RED, Color.GREEN, Color.BLUE},
                MultipleGradientPaint.CycleMethod.REPEAT));
        g.fillRect(8, 540, 220, 50);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(360, 565), 60f,
                new Point2D.Double(345, 555), new float[]{0f, 1f},
                new Color[]{Color.WHITE, new Color(30, 30, 120)}, MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fillOval(300, 505, 120, 90);
        g.setColor(Color.BLACK);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        g.setColor(Color.RED);
        g.fillRect(480, 480, 200, 100);
        g.setComposite(AlphaComposite.SrcOver);

        Graphics2D oval = (Graphics2D) g.create();
        oval.setClip(new Ellipse2D.Double(600, 20, 200, 200));
        oval.setColor(new Color(0x303030));
        oval.fillRect(600, 20, 200, 200);
        oval.dispose();

        Graphics2D turned = (Graphics2D) g.create();
        turned.translate(560, 120);
        turned.rotate(Math.toRadians(15));
        turned.setColor(new Color(0, 130, 90));
        turned.fillRect(-60, -30, 120, 60);
        turned.setColor(new Color(160, 30, 30));
        turned.fillRoundRect(-40, 40, 80, 30, 16, 16);
        turned.setStroke(new BasicStroke(2f));
        turned.setColor(Color.WHITE);
        turned.setClip(new Rectangle2D.Double(-30, -20, 60, 40));
        turned.drawRoundRect(-60, -30, 120, 60, 20, 20);
        turned.dispose();

        g.setColor(new Color(0x1A1A1A));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 34));
        g.drawString("Glyph shapes 0123", 240, 60);
        g.setFont(new Font(Font.SERIF, Font.BOLD | Font.ITALIC, 22));
        g.setColor(new Color(0x00457A));
        g.drawString("Beta text, hinted", 240, 100);

        g.setColor(new Color(0x1A1A1A));
        g.drawImage(logo, 620, 380, null);
        g.drawImage(logo, 680, 420, 120, 90, null);
        AffineTransform at = AffineTransform.getTranslateInstance(700, 200);
        at.scale(0.8, 0.8);
        g.drawImage(logo, at, null);
    }

    private static BufferedImage logo() {
        BufferedImage img = new BufferedImage(90, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(new Color(240, 240, 240));
            g.fillRect(0, 0, 90, 64);
            g.setColor(new Color(200, 40, 40));
            g.fillOval(8, 8, 40, 40);
            g.setColor(new Color(40, 80, 200));
            g.fillRect(44, 30, 40, 30);
        } finally {
            g.dispose();
        }
        return img;
    }

    // ----------------------------------------------------------------- helpers

    private static BufferedImage drawDirect(BufferedImage logo) {
        BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            engineHints(g);
            scene(g, logo);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static List<NElement> record(BufferedImage logo, PaintStreamImageAssets assets) {
        PaintStreamRecorder recorder = new PaintStreamRecorder();
        recorder.setTextOutlines(true);
        recorder.setImages(assets);
        BufferedImage surface = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = surface.createGraphics();
        try {
            engineHints(g);
            scene(new RecordingGraphics2D(g, recorder, W, H, false, null), logo);
        } finally {
            g.dispose();
        }
        return flatten(recorder, new ArrayList<>());
    }

    /**
     * Group boundaries come from {@code Graphics2D.create()} and carry no state of
     * their own, so flattening in paint order is lossless.
     */
    private static List<NElement> flatten(PaintStreamRecorder recorder, List<NElement> sink) {
        for (Object entry : recorder.getEntries()) {
            if (entry instanceof PaintStreamRecorder child) {
                flatten(child, sink);
            } else {
                sink.add((NElement) entry);
            }
        }
        return sink;
    }

    /**
     * Replays through the ship-and-parse route the exporter uses, so what gets
     * compared is the wire format rather than an in-memory shortcut.
     */
    private static BufferedImage replay(List<NElement> ops, PaintStreamImageAssets assets, double scale) {
        BufferedImage out = new BufferedImage((int) (W * scale), (int) (H * scale),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            new PaintStreamDecoder(sha -> image(assets, sha)).replay(g, shipAndDecode(ops), scale);
        } finally {
            g.dispose();
        }
        return out;
    }

    private static List<NElement> shipAndDecode(List<NElement> ops) {
        NElement shipped = new PaintStreamCodec()
                .encodePage(List.of(new PaintStreamCodec.Frame(0, ops)));
        NElement reparsed = NElementReader.ofJson()
                .read(new String(PaintStreamJson.write(shipped, false), StandardCharsets.UTF_8));
        return PaintStreamCodec.decode(reparsed).get(0);
    }

    /**
     * The pixels an image operation names, read back from what the encoder
     * collected for the same pass.
     */
    private static BufferedImage image(PaintStreamImageAssets assets, String sha256) {
        byte[] bytes = assets.pngs().get(sha256);
        return bytes == null ? null : readPng(bytes);
    }

    /**
     * The other route to the same pixels: the {@code assets/img} directory the
     * exporter wrote beside the pages, which is what a container carries.
     */
    private static PaintStreamDecoder.Images folderImages(NPath folder) {
        Map<String, BufferedImage> cache = new HashMap<>();
        return sha -> {
            NPath file = folder.resolve("assets/img/" + sha + ".png");
            if (!file.exists()) {
                return null;
            }
            return cache.computeIfAbsent(sha, s -> readPng(file.readBytes()));
        };
    }

    private static BufferedImage readPng(byte[] bytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The hint set {@code DefaultNTxEngine} installs for a page. A replay that
     * left them out would differ from the export for a reason that has nothing to
     * do with the stream.
     */
    private static void engineHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private static void assertLowFrequencyEqual(BufferedImage expected, BufferedImage actual, String tag) {
        assertEquals(expected.getWidth(), actual.getWidth(), tag + ": width");
        assertEquals(expected.getHeight(), actual.getHeight(), tag + ": height");
        BufferedImage a = boxDownsample(expected, DOWNSCALE);
        BufferedImage b = boxDownsample(actual, DOWNSCALE);
        long bad = 0;
        long total = (long) a.getWidth() * a.getHeight();
        double sum = 0;
        int worstBlock = 0;
        List<String> first = new ArrayList<>();
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                int p = a.getRGB(x, y);
                int q = b.getRGB(x, y);
                int worst = Math.max(
                        Math.abs(((p >>> 24) & 0xff) - ((q >>> 24) & 0xff)),
                        Math.max(Math.abs(((p >>> 16) & 0xff) - ((q >>> 16) & 0xff)),
                                Math.max(Math.abs(((p >>> 8) & 0xff) - ((q >>> 8) & 0xff)),
                                        Math.abs((p & 0xff) - (q & 0xff)))));
                sum += worst;
                worstBlock = Math.max(worstBlock, worst);
                if (worst > BLOCK_TOLERANCE) {
                    bad++;
                    if (first.size() < 8) {
                        first.add(x * DOWNSCALE + "," + y * DOWNSCALE + " off by " + worst);
                    }
                }
            }
        }
        double ratio = total == 0 ? 0 : (double) bad / total;
        double mean = total == 0 ? 0 : sum / total;
        String stats = tag + ": mean " + String.format("%.3f", mean) + ", worst block " + worstBlock
                + ", " + bad + "/" + total + " blocks (" + String.format("%.2f%%", ratio * 100) + ")"
                + (first.isEmpty() ? "" : ", at " + first);
        assertTrue(mean <= MAX_MEAN_ERROR, stats);
        assertTrue(ratio <= BLOCK_BUDGET, stats + ", first at " + first);
    }

    private static BufferedImage boxDownsample(BufferedImage src, int factor) {
        int w = src.getWidth() / factor;
        int h = src.getHeight() / factor;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = 0, r = 0, g0 = 0, b = 0;
                for (int dy = 0; dy < factor; dy++) {
                    for (int dx = 0; dx < factor; dx++) {
                        int p = src.getRGB(x * factor + dx, y * factor + dy);
                        a += (p >>> 24) & 0xff;
                        r += (p >>> 16) & 0xff;
                        g0 += (p >>> 8) & 0xff;
                        b += p & 0xff;
                    }
                }
                int n = factor * factor;
                out.setRGB(x, y, ((a / n) << 24) | ((r / n) << 16) | ((g0 / n) << 8) | (b / n));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ fixture

    private static HtmlStaticDocumentRenderer htmlStatic(NTxEngine engine, NPath folder) {
        HtmlStaticDocumentRenderer renderer =
                (HtmlStaticDocumentRenderer) engine.newStreamRenderer("html-static").get();
        renderer.setStreamRendererConfig(new NTxDocumentStreamRendererConfig()
                .setPageWidth(400)
                .setPageHeight(300)
                .setDpi(72)
                .setShowDate(false)
                .setShowFileName(false)
                .setShowPageNumber(false));
        renderer.setProperty(HtmlStaticDocumentRenderer.PROP_WIDTH, 600);
        renderer.setOutput(folder);
        return renderer;
    }

    private static NTxCompiledDocument load(NTxEngine engine) {
        return engine.loadDocument(new ByteArrayInputStream(DECK.getBytes(StandardCharsets.UTF_8)));
    }

    private static NPath tempFolder(String tag) {
        try {
            return NPath.of(Files.createTempDirectory("ntexup-decoder-" + tag).toString());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static NElement field(NElement container, String name) {
        return container.asListContainer().flatMap(c -> c.get(name)).orNull();
    }

    private static long longField(NElement container, String name) {
        NElement e = field(container, name);
        return e == null ? -1 : e.asLongValue().orElse(-1L);
    }
}
