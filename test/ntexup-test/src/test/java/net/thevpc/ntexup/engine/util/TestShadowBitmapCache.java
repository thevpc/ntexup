package net.thevpc.ntexup.engine.util;

import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class TestShadowBitmapCache {

    @Test
    void reusesBlurOnlyForIdenticalSourceAndShadowStyle() {
        DefaultNTxNode node = new DefaultNTxNode(NTxNodeType.TEXT);
        BufferedImage source = sourceImage(0xff336699);
        BufferedImage first = NTx2DUtils0.cachedBlurredImage(node, source, Color.BLACK, 2);
        BufferedImage expected = NTx2DUtils0.applyGaussianBlur(
                NTx2DUtils0.tint(source, Color.BLACK), 2);
        assertArrayEquals(pixels(expected), pixels(first));

        BufferedImage samePixels = sourceImage(0xff336699);
        assertSame(first, NTx2DUtils0.cachedBlurredImage(node, samePixels, Color.BLACK, 2));
        assertNotSame(first,
                NTx2DUtils0.cachedBlurredImage(node, samePixels, Color.WHITE, 2));

        BufferedImage changedPixels = sourceImage(0xff336699);
        changedPixels.setRGB(16, 16, 0xffabcdef);
        assertNotSame(first,
                NTx2DUtils0.cachedBlurredImage(node, changedPixels, Color.BLACK, 2));
    }

    private static BufferedImage sourceImage(int color) {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(16, 16, color);
        return image;
    }

    private static int[] pixels(BufferedImage image) {
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }
}
