package net.thevpc.ntexup.api.document.node;

import java.awt.Color;

/**
 * MVC search-highlight model bound to a single node. It states <b>what</b> is
 * highlighted on that node (a color plus the normalized needles to match),
 * while node renderers decide <b>where and how</b> to paint it:
 *
 * <ul>
 *     <li>text-based renderers (subclasses of
 *     {@code net.thevpc.ntexup.engine.renderer.text.NTxTextBaseRenderer})
 *     paint one rounded rectangle behind each matching token
 *     ({@link #isWholeNode()} {@code == false});</li>
 *     <li>any other renderer paints a single rounded rectangle over the node's
 *     layout bounds ({@link #isWholeNode()} {@code == true}), which covers
 *     equations and other non-text content.</li>
 * </ul>
 *
 * Highlights are attached/detached through {@link NTxHighlights} (stored in the
 * node's user-object map, so they survive render-cache invalidation) and are
 * only painted by hosts that enable the
 * {@link net.thevpc.ntexup.api.renderer.NTxRendererContext#CAPABILITY_SEARCH_HIGHLIGHTS}
 * capability (the screen viewer); PDF/HTML/image renders stay clean.
 *
 * @see NTxHighlights
 */
public class NTxNodeHighlight {

    private final Color color;
    private final String[] needles;
    private final boolean wholeNode;

    /**
     * @param color      highlight color (typically translucent)
     * @param wholeNode  {@code true} to tint the whole node bounds,
     *                   {@code false} to tint matching text tokens
     * @param needles    normalized needles (see {@link NTxHighlights#normalize(String)})
     */
    public NTxNodeHighlight(Color color, boolean wholeNode, String... needles) {
        this.color = color;
        this.needles = needles == null ? new String[0] : needles;
        this.wholeNode = wholeNode;
    }

    public Color getColor() {
        return color;
    }

    /**
     * Returns the internal needles array (no copy: called from render hot paths).
     */
    public String[] getNeedles() {
        return needles;
    }

    public boolean isWholeNode() {
        return wholeNode;
    }
}
