package net.thevpc.ntexup.api.document.style;

import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

/**
 * Declarative policy for the parts of a node's painting that leave its own
 * layout box. Resolved from the {@link NTxPropName#OVERFLOW} property (with the
 * {@link NTxPropName#OVERFLOW_MARGIN} and {@link NTxPropName#OVERFLOW_FADE}
 * modifiers) and applied centrally by the engine, so any node type can opt in
 * without writing custom code.
 *
 * <p>The <em>crop box</em> is the node's own bounds inflated on each side by the
 * configured margin. A positive margin lets a controlled amount of overflow be
 * shown; a negative one insets the crop inside the bounds.</p>
 *
 * <ul>
 *   <li>{@link Mode#VISIBLE} - default, nothing is cropped.</li>
 *   <li>{@link Mode#HIDDEN}/{@link Mode#CLIP} - hard crop at the crop box.</li>
 *   <li>{@link Mode#FADE} - crop at the crop box with a soft alpha feather of
 *       {@link #fadeTop()}/{@link #fadeRight()}/{@link #fadeBottom()}/
 *       {@link #fadeLeft()} pixels ending at the crop-box edge, so the content
 *       dissolves instead of ending abruptly.</li>
 * </ul>
 *
 * <p>Overflow is purely visual: like CSS {@code overflow} it never changes the
 * node's measured size or position.</p>
 */
public final class NTxOverflow {

    public enum Mode {
        VISIBLE,
        HIDDEN,
        CLIP,
        FADE;

        /**
         * Parses the mode name, accepting a few friendly aliases. Unknown values
         * fall back to {@link #VISIBLE} (fail-open, so a typo never hides
         * content).
         */
        public static Mode of(String s) {
            if (s == null) {
                return VISIBLE;
            }
            switch (s.trim().toLowerCase()) {
                case "visible":
                case "none":
                case "off":
                case "show":
                case "false":
                    return VISIBLE;
                case "hidden":
                case "hide":
                case "true":
                case "on":
                    return HIDDEN;
                case "clip":
                    return CLIP;
                case "fade":
                    return FADE;
                default:
                    return VISIBLE;
            }
        }
    }

    /** Shared no-op instance: nothing is cropped. */
    public static final NTxOverflow VISIBLE = new NTxOverflow(Mode.VISIBLE, 0, 0, 0, 0, 0, 0, 0, 0);

    private final Mode mode;
    private final double top;
    private final double right;
    private final double bottom;
    private final double left;
    private final double fadeTop;
    private final double fadeRight;
    private final double fadeBottom;
    private final double fadeLeft;

    public NTxOverflow(Mode mode, double top, double right, double bottom, double left,
                       double fadeTop, double fadeRight, double fadeBottom, double fadeLeft) {
        this.mode = mode == null ? Mode.VISIBLE : mode;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
        this.fadeTop = fadeTop;
        this.fadeRight = fadeRight;
        this.fadeBottom = fadeBottom;
        this.fadeLeft = fadeLeft;
    }

    public Mode mode() {
        return mode;
    }

    public boolean isVisible() {
        return mode == Mode.VISIBLE;
    }

    public boolean isFade() {
        return mode == Mode.FADE;
    }

    public double top() {
        return top;
    }

    public double right() {
        return right;
    }

    public double bottom() {
        return bottom;
    }

    public double left() {
        return left;
    }

    public double fadeTop() {
        return fadeTop;
    }

    public double fadeRight() {
        return fadeRight;
    }

    public double fadeBottom() {
        return fadeBottom;
    }

    public double fadeLeft() {
        return fadeLeft;
    }

    /**
     * The rectangle the node is cropped to: its bounds inflated by the margins.
     */
    public Shape clipShape(NTxBounds2D bounds) {
        return new Rectangle2D.Double(
                bounds.minX() - left,
                bounds.minY() - top,
                bounds.widthX() + left + right,
                bounds.widthY() + top + bottom);
    }

    /**
     * Same as {@link #clipShape(NTxBounds2D)} but as bounds.
     */
    public NTxBounds2D clipBounds(NTxBounds2D bounds) {
        return NTxBounds2D.ofWidth(
                bounds.minX() - left,
                bounds.minY() - top,
                bounds.widthX() + left + right,
                bounds.widthY() + top + bottom);
    }
}
