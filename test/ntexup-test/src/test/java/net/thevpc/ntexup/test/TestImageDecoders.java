package net.thevpc.ntexup.test;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestImageDecoders {

    @Test
    void decodesWebPShowcaseAsset() throws IOException {
        assertDecoded("image.webp", 530, 539);
    }

    @Test
    void decodesAvifShowcaseAsset() throws IOException {
        assertDecoded("image.avif", 720, 540);
    }

    private static void assertDecoded(String name, int width, int height) throws IOException {
        Path path = Path.of("..", "..", "..", "ntexup-doc-slides", "images", name);
        BufferedImage image;
        try (InputStream input = Files.newInputStream(path)) {
            image = ImageIO.read(input);
        }
        assertNotNull(image, name);
        assertEquals(width, image.getWidth(), name);
        assertEquals(height, image.getHeight(), name);
    }
}
