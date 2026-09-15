package net.thevpc.ntexup.extension.animatedgif;

import net.thevpc.ntexup.api.document.elem2d.NTxImageOptions;
import net.thevpc.ntexup.api.renderer.NTxGraphicsImageDrawer;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.nuts.io.NPath;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

class GifNTxImageDrawer implements NTxGraphicsImageDrawer {
    private final byte[] ic;
    private final Map<String, FutureTask<NPath>> pendingCache;

    public GifNTxImageDrawer(byte[] ic, Map<String, FutureTask<NPath>> pendingCache) {
        this.ic = ic;
        this.pendingCache = pendingCache;
    }

    @Override
    public void drawImage(double x, double y, NTxImageOptions options, NTxGraphics g) {
        Color transparentColor = options.getTransparentColor();
        Dimension size = options.getSize();
        if (options != null && options.isPreserveAspectRatio() && size != null) {
            try {
                BufferedImage tmp = ImageIO.read(new ByteArrayInputStream(ic));
                if (tmp != null && tmp.getWidth() > 0 && tmp.getHeight() > 0 && size.width > 0 && size.height > 0) {
                    double scale = Math.min((double) size.width / tmp.getWidth(), (double) size.height / tmp.getHeight());
                    int drawW = Math.max(1, (int) Math.round(tmp.getWidth() * scale));
                    int drawH = Math.max(1, (int) Math.round(tmp.getHeight() * scale));
                    x = x + (size.width - drawW) / 2.0;
                    y = y + (size.height - drawH) / 2.0;
                    size = new Dimension(drawW, drawH);
                }
            } catch (Exception ignored) {
            }
        }
        Dimension finalSize = size;
        FutureTask<NPath> location = GifResizer.transformWithCache(ic, null,
                new GifResizer.GifFrameTransformer() {
                    @Override
                    public BufferedImage frame(BufferedImage image, int index) {
                        return null;
                    }

                    @Override
                    public Color transparentColor() {
                        return transparentColor;
                    }

                    @Override
                    public Dimension size() {
                        return finalSize;
                    }
                }, pendingCache, g.engine());
        if (location.isDone() || options.isDisableAnimation()) {
            ImageIcon ii = null;
            try {
                ii = new ImageIcon(location.get().toURL().get());
                g.drawImage(ii.getImage(), x, y, options.getImageObserver());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        } else {
            BufferedImage image = null;
            try {
                image = ImageIO.read(new ByteArrayInputStream(ic));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (image != null) {
                image = g.engine().tools().resizeBufferedImage(image, options.getSize());
                g.drawImage(image, x, y, options.getImageObserver());
            }
            new Thread() {
                @Override
                public void run() {
                    if (options.getAsyncLoad() != null) {
                        options.getAsyncLoad().run();
                    }
                }
            }.start();
        }
    }
}
