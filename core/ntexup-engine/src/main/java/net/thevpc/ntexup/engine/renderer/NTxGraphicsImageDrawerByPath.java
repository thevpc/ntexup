package net.thevpc.ntexup.engine.renderer;

import net.thevpc.ntexup.api.document.elem2d.NTxImageOptions;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxGraphicsImageDrawer;
import net.thevpc.ntexup.api.util.NTxUtilsImages;
import net.thevpc.nuts.io.NPath;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

public class NTxGraphicsImageDrawerByPath implements NTxGraphicsImageDrawer {
    private final NPath ic;

    public NTxGraphicsImageDrawerByPath(NPath ic) {
        this.ic = ic;
    }


    private BufferedImage loadBufferedImage(NPath p) {
        URL url = p.toURL().orNull();
        if(url!=null) {
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

    @Override
    public void drawImage(double x, double y, NTxImageOptions options, NTxGraphics g) {
        BufferedImage image = loadBufferedImage(ic);
        if (image == null) {
            return;
        }
        if (options != null && options.isPreserveAspectRatio() && options.getSize() != null) {
            java.awt.Dimension targetSize = options.getSize();
            int origW = image.getWidth();
            int origH = image.getHeight();
            if (origW > 0 && origH > 0 && targetSize.width > 0 && targetSize.height > 0) {
                double scale = Math.min((double) targetSize.width / origW, (double) targetSize.height / origH);
                int drawW = Math.max(1, (int) Math.round(origW * scale));
                int drawH = Math.max(1, (int) Math.round(origH * scale));
                double drawX = x + (targetSize.width - drawW) / 2.0;
                double drawY = y + (targetSize.height - drawH) / 2.0;
                BufferedImage resized = NTxUtilsImages.resizeImage(image, drawW, drawH);
                g.drawImage(resized, drawX, drawY, options.getImageObserver());
                return;
            }
        }
        image = NTxUtilsImages.resizeImage(image, options.getSize());
        g.drawImage(image, x, y, options.getImageObserver());
    }
}
