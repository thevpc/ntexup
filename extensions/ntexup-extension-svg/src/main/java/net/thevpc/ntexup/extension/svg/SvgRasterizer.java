package net.thevpc.ntexup.extension.svg;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import net.thevpc.nuts.collections.NMaps;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static java.awt.RenderingHints.*;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * Responsible for converting SVG images into rasterized PNG images.
 */
public class SvgRasterizer {
    public final static Map<Object, Object> RENDERING_HINTS = NMaps.of(
            KEY_ANTIALIASING,
            VALUE_ANTIALIAS_ON,
            KEY_ALPHA_INTERPOLATION,
            VALUE_ALPHA_INTERPOLATION_QUALITY,
            KEY_COLOR_RENDERING,
            VALUE_COLOR_RENDER_QUALITY,
            KEY_DITHERING,
            VALUE_DITHER_DISABLE,
            KEY_FRACTIONALMETRICS,
            VALUE_FRACTIONALMETRICS_ON,
            KEY_INTERPOLATION,
            VALUE_INTERPOLATION_BICUBIC,
            KEY_RENDERING,
            VALUE_RENDER_QUALITY,
            KEY_STROKE_CONTROL,
            VALUE_STROKE_PURE,
            KEY_TEXT_ANTIALIASING,
            VALUE_TEXT_ANTIALIAS_ON
    );

    private final static SVGUniverse sRenderer = new SVGUniverse();

    /**
     * Rasterizes a vector graphic to a given size using a {@link BufferedImage}.
     * The rendering hints are set to produce high quality output.
     *
     * @param path   Fully qualified path to the image resource to rasterize.
     * @param dstDim The output image dimensions.
     * @return The rasterized {@link Image}.
     * @throws SVGException Could not open, read, parse, or render SVG data.
     */
    public Image rasterize(final String path, final Dimension dstDim) {
        final SVGDiagram diagram = loadDiagram(path);
        final float wDiagram = diagram.getWidth();
        final float hDiagram = diagram.getHeight();
        final Dimension srcDim = new Dimension((int) wDiagram, (int) hDiagram);

        final Dimension scaled = fit(srcDim, dstDim);
        final int wScaled = (int) scaled.getWidth();
        final int hScaled = (int) scaled.getHeight();

        final BufferedImage image = new BufferedImage(wScaled, hScaled, TYPE_INT_ARGB);

        final Graphics2D g = image.createGraphics();
        g.setRenderingHints(RENDERING_HINTS);

        final AffineTransform transform = g.getTransform();
        transform.setToScale(wScaled / wDiagram, hScaled / hDiagram);

        g.setTransform(transform);
        try {
            diagram.render(g);
        } catch (SVGException e) {
            throw new RuntimeException(e);
        }
        g.dispose();

        return image;
    }

    public Image rasterize(final byte[] path, final Dimension dstDim)
            throws SVGException {
        final SVGDiagram diagram = loadDiagram(path);
        final float wDiagram = diagram.getWidth();
        final float hDiagram = diagram.getHeight();
        final Dimension srcDim = new Dimension((int) wDiagram, (int) hDiagram);

        final Dimension scaled = fit(srcDim, dstDim);
        final int wScaled = (int) scaled.getWidth();
        final int hScaled = (int) scaled.getHeight();

        final BufferedImage image = new BufferedImage(wScaled, hScaled, TYPE_INT_ARGB);

        final Graphics2D g = image.createGraphics();
        g.setRenderingHints(RENDERING_HINTS);

        final AffineTransform transform = g.getTransform();
        transform.setToScale(wScaled / wDiagram, hScaled / hDiagram);

        g.setTransform(transform);
        diagram.render(g);
        g.dispose();

        return image;
    }

    /**
     * Gets an instance of {@link URL} that references a file in the
     * application's resources.
     *
     * @param path The full path (starting at the root), relative to the
     *             application or JAR file's resources directory.
     * @return A {@link URL} to the file or {@code null} if the path does not
     * point to a resource.
     */
    private URL getResourceUrl(final String path) {
        return SvgRasterizer.class.getResource(path);
    }

    /**
     * Loads the resource specified by the given path into an instance of
     * {@link SVGDiagram} that can be rasterized into a bitmap format. The
     * {@link SVGUniverse} class will
     *
     * @param path The full path (starting at the root), relative to the
     *             application or JAR file's resources directory.
     * @return An {@link SVGDiagram} that can be rasterized onto a
     * {@link BufferedImage}.
     */
    private SVGDiagram loadDiagram(final String path) {
        final URL url = getResourceUrl(path);
        final URI uri = sRenderer.loadSVG(url);
        final SVGDiagram diagram = sRenderer.getDiagram(uri);
        return applySettings(diagram);
    }

    private SVGDiagram loadDiagram(final byte[] bytes) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(bytes);
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            Path path = Paths.get(hashtext + ".svg");
            Files.write(path, bytes, StandardOpenOption.WRITE);
            final URL url = path.toUri().toURL();
            final URI uri = sRenderer.loadSVG(url);
            final SVGDiagram diagram = sRenderer.getDiagram(uri);
            return applySettings(diagram);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Instructs the SVG renderer to rasterize the image even if it would be
     * clipped.
     *
     * @param diagram The {@link SVGDiagram} to render.
     * @return The same instance with ignore clip heuristics set to {@code true}.
     */
    private SVGDiagram applySettings(final SVGDiagram diagram) {
        diagram.setIgnoringClipHeuristic(true);
        return diagram;
    }

    /**
     * Scales the given source {@link Dimension} to the destination
     * {@link Dimension}, maintaining the aspect ratio with respect to
     * the best fit.
     *
     * @param src The original vector graphic dimensions to change.
     * @param dst The desired image dimensions to scale.
     * @return The given source dimensions scaled to the destination dimensions,
     * maintaining the aspect ratio.
     */
    private Dimension fit(final Dimension src, final Dimension dst) {
        final double srcWidth = src.getWidth();
        final double srcHeight = src.getHeight();

        // Determine the ratio that will have the best fit.
        final double ratio = Math.min(
                dst.getWidth() / srcWidth, dst.getHeight() / srcHeight
        );

        // Scale both dimensions with respect to the best fit ratio.
        return new Dimension((int) (srcWidth * ratio), (int) (srcHeight * ratio));
    }
}
