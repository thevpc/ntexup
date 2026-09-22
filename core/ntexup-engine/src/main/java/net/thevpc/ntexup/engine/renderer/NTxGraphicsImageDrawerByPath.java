package net.thevpc.ntexup.engine.renderer;

import net.thevpc.ntexup.api.document.elem2d.NTxImageOptions;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxGraphicsImageDrawer;
import net.thevpc.ntexup.api.util.NTxUtilsImages;
import net.thevpc.nuts.io.NPath;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class NTxGraphicsImageDrawerByPath implements NTxGraphicsImageDrawer {
    private final NPath ic;

    private static final long MAX_CACHED_PIXELS = 96L * 1024 * 1024;
    private static final ImageLRU IMAGE_CACHE = new ImageLRU();

    public NTxGraphicsImageDrawerByPath(NPath ic) {
        this.ic = ic;
    }

    private static BufferedImage loadBufferedImage(NPath p) {
        URL url = p.toURL().orNull();
        if (url != null) {
            try {
                BufferedImage i = ImageIO.read(url);
                return i;
            } catch (IOException e) {
                //
            }
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(p.readBytes()));
        } catch (IOException e) {
            //
        }
        return null;
    }

    private static String cacheKey(NPath p, int w, int h) {
        return p.toString() + "@" + w + "x" + h;
    }

    private static BufferedImage cachedImage(NPath p, int w, int h) {
        String key = cacheKey(p, w, h);
        BufferedImage img = IMAGE_CACHE.get(key);
        if (img != null) {
            return img;
        }
        BufferedImage raw = loadBufferedImage(p);
        if (raw == null) {
            return null;
        }
        int rw = raw.getWidth();
        int rh = raw.getHeight();
        if (w <= 0 || h <= 0 || (w == rw && h == rh)) {
            img = raw;
        } else {
            img = NTxUtilsImages.resizeImage(raw, w, h);
        }
        IMAGE_CACHE.put(key, img);
        return img;
    }

    @Override
    public void drawImage(double x, double y, NTxImageOptions options, NTxGraphics g) {
        int tw = options == null ? -1 : (options.getSize() == null ? -1 : options.getSize().width);
        int th = options == null ? -1 : (options.getSize() == null ? -1 : options.getSize().height);
        if (options != null && options.isPreserveAspectRatio() && options.getSize() != null) {
            Dimension targetSize = options.getSize();
            if (targetSize.width > 0 && targetSize.height > 0) {
                BufferedImage raw = cachedImage(ic, -1, -1);
                if (raw == null) {
                    return;
                }
                int origW = raw.getWidth();
                int origH = raw.getHeight();
                if (origW > 0 && origH > 0) {
                    double scale = Math.min((double) targetSize.width / origW, (double) targetSize.height / origH);
                    int drawW = Math.max(1, (int) Math.round(origW * scale));
                    int drawH = Math.max(1, (int) Math.round(origH * scale));
                    double drawX = x + (targetSize.width - drawW) / 2.0;
                    double drawY = y + (targetSize.height - drawH) / 2.0;
                    BufferedImage resized = cachedImage(ic, drawW, drawH);
                    if (resized != null) {
                        g.drawImage(resized, drawX, drawY, options.getImageObserver());
                    }
                    return;
                }
            }
        }
        BufferedImage image = cachedImage(ic, tw, th);
        if (image != null) {
            g.drawImage(image, x, y, options == null ? null : options.getImageObserver());
        }
    }

    private static long pixelsOf(BufferedImage i) {
        if (i == null) {
            return 0;
        }
        return (long) i.getWidth() * i.getHeight();
    }

    private static class ImageLRU {
        private final LinkedHashMap<String, BufferedImage> map;
        private long pixels;

        ImageLRU() {
            map = new LinkedHashMap<String, BufferedImage>(256, 0.75f, true);
        }

        synchronized BufferedImage get(String k) {
            return map.get(k);
        }

        synchronized void put(String k, BufferedImage v) {
            if (v == null) {
                return;
            }
            long vp = pixelsOf(v);
            if (vp > MAX_CACHED_PIXELS) {
                return;
            }
            BufferedImage prev = map.put(k, v);
            if (prev != null) {
                pixels -= pixelsOf(prev);
            }
            pixels += vp;
            if (pixels > MAX_CACHED_PIXELS) {
                Iterator<Map.Entry<String, BufferedImage>> it = map.entrySet().iterator();
                while (it.hasNext() && pixels > MAX_CACHED_PIXELS) {
                    Map.Entry<String, BufferedImage> e = it.next();
                    pixels -= pixelsOf(e.getValue());
                    it.remove();
                }
            }
        }
    }
}