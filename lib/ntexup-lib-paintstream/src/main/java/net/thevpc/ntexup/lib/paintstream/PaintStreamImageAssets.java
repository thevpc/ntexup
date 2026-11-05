package net.thevpc.ntexup.lib.paintstream;

import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.elem.NObjectElementBuilder;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Image identity registry for a paint pass.
 *
 * A recorded {@code image} op is only replayable in a browser if it names the
 * pixels it drew, so every distinct {@link Image} painted during the pass is
 * encoded once, content-addressed by its PNG SHA-256, and referenced from the
 * op as {@code ref:{sha256,width,height}}. The bytes collected here are written
 * by the host renderer as assets and deduplicated by the same hash.
 * <p>
 * Not wired by default: {@link PaintStreamRecorder#setImages(PaintStreamImageAssets)} enables
 * it, which keeps non-asset consumers (the dump renderer) byte-for-byte as they
 * were.
 */
public class PaintStreamImageAssets {

    /**
     * Content address and intrinsic size of one painted image.
     */
    public record Ref(String sha256, int width, int height) {
    }

    private final Map<Object, Ref> byImage = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<String, byte[]> pngBySha = new LinkedHashMap<>();

    /**
     * @return the reference for {@code image}, or {@code null} when the image
     * cannot be encoded (unknown size, no PNG writer).
     */
    public Ref ref(Object image) {
        if (image == null) {
            return null;
        }
        Ref cached = byImage.get(image);
        if (cached != null) {
            return cached;
        }
        BufferedImage bi = toBufferedImage(image);
        if (bi == null || bi.getWidth() <= 0 || bi.getHeight() <= 0) {
            return null;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(bi, "png", bos)) {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
        byte[] png = bos.toByteArray();
        String sha = sha256(png);
        pngBySha.putIfAbsent(sha, png);
        Ref r = new Ref(sha, bi.getWidth(), bi.getHeight());
        byImage.put(image, r);
        return r;
    }

    /**
     * @return the {@code ref:{...}} element for {@code image}, or {@code null}.
     */
    public NElement element(Object image) {
        Ref r = ref(image);
        if (r == null) {
            return null;
        }
        NObjectElementBuilder ob = NElement.ofObjectBuilder("png");
        ob.add("sha256", NElement.of(r.sha256()));
        ob.add("width", NElement.of(r.width()));
        ob.add("height", NElement.of(r.height()));
        return ob.build();
    }

    /**
     * All encoded images of this pass, keyed by SHA-256.
     */
    public Map<String, byte[]> pngs() {
        return pngBySha;
    }

    private static BufferedImage toBufferedImage(Object image) {
        if (image instanceof BufferedImage bi) {
            return bi;
        }
        int w;
        int h;
        if (image instanceof RenderedImage ri) {
            w = ri.getWidth();
            h = ri.getHeight();
        } else if (image instanceof Image im) {
            w = im.getWidth(null);
            h = im.getHeight(null);
        } else {
            return null;
        }
        if (w <= 0 || h <= 0) {
            return null;
        }
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        try {
            if (image instanceof RenderedImage ri) {
                g.drawRenderedImage(ri, new AffineTransform());
            } else {
                g.drawImage((Image) image, 0, 0, null);
            }
        } finally {
            g.dispose();
        }
        return bi;
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest(bytes)) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
