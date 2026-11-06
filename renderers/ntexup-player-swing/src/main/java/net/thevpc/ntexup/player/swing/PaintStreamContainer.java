package net.thevpc.ntexup.player.swing;

import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NElementReader;
import net.thevpc.nuts.io.NPath;
import net.thevpc.ntexup.lib.paintstream.PaintStreamCodec;
import net.thevpc.ntexup.lib.paintstream.PaintStreamDecoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The playback archive of a deck, read as data.
 *
 * A {@code .ntxp} container written by {@code ntexup-renderer-ntxp} carries the
 * paint streams of every page plus a descriptor that says what the archive is.
 * This class is the reading half of that contract: it checks the descriptor,
 * parses the manifest once, and hands out the operations of a page already
 * decoded and ready for {@link PaintStreamDecoder}. Nothing here loads a
 * document, compiles a node tree or touches an engine — a replay needs none of
 * that, and leaving it out is what makes the player open a deck instantly
 * instead of laying it out first.
 * <p>
 * Pages are read on demand and only a few are kept: a deck of fifty slides is
 * fifty json payloads, and a player that held them all would trade its memory
 * for pages nobody scrolled to. The window mirrors the browser player, three
 * pages loaded ahead and four kept warm, for the same reason — unzipping and
 * parsing a page is far slower than the sixty-hertz replay of a frame.
 */
public class PaintStreamContainer implements AutoCloseable {

    /**
     * Container layout this reader accepts, the version
     * {@code NtxpDocumentRenderer.CONTAINER_FORMAT} writes.
     */
    public static final int SUPPORTED_FORMAT = 1;

    /**
     * First entry a player reads; it is what makes an arbitrary zip an ntexup
     * container.
     */
    private static final String DESCRIPTOR = "META-INF/ntexup.json";

    private static final int KEEP = 4;
    private static final int PREFETCH = 3;

    /**
     * One page as the manifest describes it. Frame times are implicit: samples
     * are taken every {@link #periodMillis()}, so frame {@code i} shows at
     * {@code i * periodMs}.
     */
    public record Page(int index, String file, int frames, long durationMillis,
                       long loopStartMillis, long loopEndMillis, boolean animated) {
    }

    private final ZipFile zip;
    private final String title;
    private final int width;
    private final int height;
    private final long periodMillis;
    private final boolean animated;
    private final List<Page> pages = new ArrayList<>();
    private final Set<String> declaredAssets = new LinkedHashSet<>();
    private final Map<String, BufferedImage> imageCache = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, List<List<NElement>>> frameCache =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, List<List<NElement>>> eldest) {
                    return size() > KEEP;
                }
            };

    /**
     * Opens a container. The descriptor is checked here, so a zip that is not an
     * ntexup playback archive fails before any page is touched rather than
     * showing up as a blank window.
     */
    public static PaintStreamContainer open(NPath path) {
        File file = path.toFile().orNull();
        if (file == null) {
            throw new IllegalStateException(NMsg.ofC("ntxp: %s is not a file on disk", path).toString());
        }
        try {
            return new PaintStreamContainer(file);
        } catch (IOException e) {
            throw new IllegalStateException(NMsg.ofC("ntxp: cannot open %s", path).toString(), e);
        }
    }

    private PaintStreamContainer(File file) throws IOException {
        this.zip = new ZipFile(file);
        NElement descriptor = json(DESCRIPTOR);
        int format = (int) longOf(descriptor, "format");
        if (format != SUPPORTED_FORMAT) {
            throw new IllegalStateException(NMsg.ofC("ntxp: container format %s, this player reads %s",
                    format, SUPPORTED_FORMAT).toString());
        }
        if (!"playback".equals(stringOf(descriptor, "kind"))) {
            throw new IllegalStateException(NMsg.ofC("ntxp: %s is not a playback container", file).toString());
        }
        int paintStreamFormat = (int) longOf(descriptor, "paintStreamFormat");
        if (paintStreamFormat > PaintStreamCodec.FORMAT_VERSION) {
            throw new IllegalStateException(NMsg.ofC("ntxp: paint stream format %s, this player reads %s",
                    paintStreamFormat, PaintStreamCodec.FORMAT_VERSION).toString());
        }
        NElement manifest = json(stringOf(descriptor, "manifest"));
        this.width = (int) longOf(manifest, "width");
        this.height = (int) longOf(manifest, "height");
        this.periodMillis = Math.max(1, longOf(manifest, "periodMs"));
        this.animated = booleanOf(manifest, "animated");
        this.title = stringOf(of(descriptor, "document"), "title");
        NElement entries = of(manifest, "pages");
        if (entries != null) {
            int i = 0;
            for (NElement page : entries.asArray().get()) {
                pages.add(new Page(i + 1, stringOf(page, "file"), (int) longOf(page, "frames"),
                        longOf(page, "durationMs"), longOf(page, "loopStartMs"),
                        longOf(page, "loopEndMs"), booleanOf(page, "animated")));
                i++;
            }
        }
        if (pages.isEmpty()) {
            throw new IllegalStateException(NMsg.ofC("ntxp: %s carries no page", file).toString());
        }
        NElement assets = of(manifest, "imageAssets");
        if (assets != null) {
            for (NElement sha : assets.asArray().get()) {
                declaredAssets.add(sha.asStringValue().orElse(""));
            }
        }
    }

    @Override
    public void close() {
        try {
            zip.close();
        } catch (IOException ignored) {
            //an archive the player is done with cannot fail the session
        }
    }

    public int pageCount() {
        return pages.size();
    }

    public Page page(int index) {
        return pages.get(index);
    }

    public String title() {
        return title;
    }

    /**
     * The pixel size every frame was recorded at, which is the size a replay is
     * fitted into.
     */
    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /**
     * Milliseconds between two samples of the same page.
     */
    public long periodMillis() {
        return periodMillis;
    }

    /**
     * Whether pages were sampled over time at all. A false deck ships one frame
     * per page, and a player should not start a timer for it.
     */
    public boolean animated() {
        return animated;
    }

    /**
     * The decoded operations of every frame of one page, from cache when the
     * page is warm.
     */
    public List<List<NElement>> frames(int index) {
        List<List<NElement>> cached = frameCache.get(index);
        if (cached != null) {
            return cached;
        }
        List<List<NElement>> frames = PaintStreamCodec.decode(json(pages.get(index).file()));
        frameCache.put(index, frames);
        return frames;
    }

    /**
     * Loads the pages around {@code index} so moving forward or back is a cache
     * hit rather than an unzip on the paint thread.
     */
    public void prefetch(int index) {
        for (int i = Math.max(0, index - 1); i < Math.min(pages.size(), index + 1 + PREFETCH); i++) {
            if (i != index) {
                frames(i);
            }
        }
    }

    /**
     * The frame a page shows at sample {@code sample}: the loop window wraps,
     * anything else holds its last frame, and a single-frame page has nothing to
     * schedule.
     */
    public int frameIndex(int index, int sample) {
        Page page = page(index);
        int last = Math.max(0, page.frames() - 1);
        if (last == 0) {
            return 0;
        }
        if (page.loopStartMillis() < 0) {
            return Math.min(sample, last);
        }
        int from = (int) (page.loopStartMillis() / periodMillis);
        int to = (int) (page.loopEndMillis() / periodMillis);
        if (sample < from) {
            return Math.min(sample, last);
        }
        return from + (sample - from) % Math.max(1, to - from + 1);
    }

    /**
     * Where the player finds the pixels an image operation names by SHA-256.
     * Only assets the manifest declares are looked up, and each is decoded once.
     */
    public PaintStreamDecoder.Images images() {
        return sha -> {
            if (!declaredAssets.contains(sha)) {
                return null;
            }
            BufferedImage cached = imageCache.get(sha);
            if (cached != null) {
                return cached;
            }
            byte[] png = bytes("assets/img/" + sha + ".png");
            if (png == null) {
                return null;
            }
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
                if (image != null) {
                    imageCache.put(sha, image);
                }
                return image;
            } catch (IOException e) {
                return null;
            }
        };
    }

    private NElement json(String entry) {
        byte[] bytes = bytes(entry);
        if (bytes == null) {
            throw new IllegalStateException(NMsg.ofC("ntxp: %s is missing from the archive", entry).toString());
        }
        return NElementReader.ofJson().read(new String(bytes, StandardCharsets.UTF_8));
    }

    private byte[] bytes(String name) {
        ZipEntry e = zip.getEntry(name);
        if (e == null) {
            return null;
        }
        try (InputStream in = zip.getInputStream(e)) {
            return in.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException(NMsg.ofC("ntxp: cannot read %s", name).toString(), ex);
        }
    }

    private static NElement of(NElement container, String name) {
        return container == null ? null : container.asListContainer().flatMap(c -> c.get(name)).orNull();
    }

    private static String stringOf(NElement container, String name) {
        NElement e = of(container, name);
        return e == null ? null : e.asStringValue().orNull();
    }

    private static long longOf(NElement container, String name) {
        NElement e = of(container, name);
        return e == null ? -1 : e.asLongValue().orElse(-1L);
    }

    private static boolean booleanOf(NElement container, String name) {
        NElement e = of(container, name);
        return e != null && e.asBooleanValue().orElse(false);
    }
}
