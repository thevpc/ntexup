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
        image = NTxUtilsImages.resizeImage(image, options.getSize());
        g.drawImage(image, x, y, options.getImageObserver());
    }
}
