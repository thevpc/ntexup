package net.thevpc.ntexup.test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Deterministic screenshot comparison used by the rendering regression tests.
 *
 * <p>Baselines live in {@code src/test/resources/golden}. A missing baseline is
 * an error unless {@code -Dntexup.golden.update=true} (or the
 * {@code NTEXUP_GOLDEN_UPDATE} environment variable) is set, which writes the
 * current render as the new baseline. Mismatches write the actual image under
 * {@code target/golden-actual} so it can be inspected.
 *
 * <p>The comparison is pixel based with a small channel tolerance and a tiny
 * differing-pixel budget, because anti-aliasing and font hinting may vary
 * slightly between JDK builds while a real layout regression moves far more
 * pixels.
 */
final class GoldenImages {

    private static final int CHANNEL_TOLERANCE = 12;
    private static final double DIFF_PIXEL_BUDGET = 0.005;

    private GoldenImages() {
    }

    static boolean updateMode() {
        return Boolean.getBoolean("ntexup.golden.update")
                || "true".equalsIgnoreCase(System.getenv("NTEXUP_GOLDEN_UPDATE"));
    }

    static void assertMatches(String name, BufferedImage actual) throws IOException {
        Path goldenDir = goldenDir();
        Path golden = goldenDir.resolve(name + ".png");
        Path actualDir = moduleDir().resolve("target/golden-actual");

        if (!Files.exists(golden)) {
            if (updateMode()) {
                write(actual, golden);
                return;
            }
            write(actual, actualDir.resolve(name + ".png"));
            fail("Missing golden baseline " + golden
                    + ". Run with -Dntexup.golden.update=true to create it.");
        }

        BufferedImage expected = ImageIO.read(golden.toFile());
        if (expected == null) {
            fail("Unable to read golden baseline " + golden);
        }
        if (expected.getWidth() != actual.getWidth() || expected.getHeight() != actual.getHeight()) {
            write(actual, actualDir.resolve(name + ".png"));
            fail("Golden size mismatch for " + name + ": expected "
                    + expected.getWidth() + "x" + expected.getHeight()
                    + " but was " + actual.getWidth() + "x" + actual.getHeight());
        }

        long differing = 0;
        long total = (long) actual.getWidth() * actual.getHeight();
        for (int y = 0; y < actual.getHeight(); y++) {
            for (int x = 0; x < actual.getWidth(); x++) {
                if (!pixelEquals(expected.getRGB(x, y), actual.getRGB(x, y))) {
                    differing++;
                }
            }
        }
        double ratio = total == 0 ? 0 : (double) differing / total;
        if (ratio > DIFF_PIXEL_BUDGET) {
            write(actual, actualDir.resolve(name + ".png"));
            fail("Golden mismatch for " + name + ": " + differing + "/" + total
                    + " pixels differ (" + String.format("%.3f%%", ratio * 100)
                    + "). Actual image written to " + actualDir.resolve(name + ".png"));
        }
    }

    private static boolean pixelEquals(int a, int b) {
        int aa = (a >>> 24) & 0xff;
        int ar = (a >>> 16) & 0xff;
        int ag = (a >>> 8) & 0xff;
        int ab = a & 0xff;
        int ba = (b >>> 24) & 0xff;
        int br = (b >>> 16) & 0xff;
        int bg = (b >>> 8) & 0xff;
        int bb = b & 0xff;
        return Math.abs(aa - ba) <= CHANNEL_TOLERANCE
                && Math.abs(ar - br) <= CHANNEL_TOLERANCE
                && Math.abs(ag - bg) <= CHANNEL_TOLERANCE
                && Math.abs(ab - bb) <= CHANNEL_TOLERANCE;
    }

    private static void write(BufferedImage image, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        ImageIO.write(image, "png", path.toFile());
    }

    private static Path goldenDir() {
        return moduleDir().resolve("src/test/resources/golden");
    }

    /**
     * Resolves the test module directory both when surefire runs in the module
     * and when the build is launched from the repository root.
     */
    private static Path moduleDir() {
        Path cwd = Path.of("").toAbsolutePath();
        if (Files.isDirectory(cwd.resolve("src/test/java"))) {
            return cwd;
        }
        Path fromRoot = cwd.resolve("test/ntexup-test");
        if (Files.isDirectory(fromRoot.resolve("src/test/java"))) {
            return fromRoot;
        }
        return cwd;
    }
}
