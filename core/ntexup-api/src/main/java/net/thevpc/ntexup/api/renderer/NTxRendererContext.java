package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.*;

import net.thevpc.ntexup.api.document.NTxSizeRequirements;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererBuilder;
import net.thevpc.ntexup.api.source.NTxSourceMonitor;
import net.thevpc.ntexup.api.util.NTxSizeRef;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NText;
import net.thevpc.nuts.util.NOptional;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;
import java.util.List;
import java.util.function.Consumer;

public interface NTxRendererContext extends NTxResolutionContext {
    String CAPABILITY_PRINT = "print";
    String CAPABILITY_ANIMATE = "animate";
    /**
     * Host paints on-canvas search highlights (screen viewer); absent in
     * PDF/HTML/image exports so they never appear there.
     */
    String CAPABILITY_SEARCH_HIGHLIGHTS = "search-highlights";

    default NTxSizeRequirements sizeRequirementsOf() {
        return engine().getRenderer(node().type()).get().sizeRequirements(this);
    }

    /**
     * Live lifecycle of the page currently being rendered, when the hosting
     * renderer knows about page visibility (typically the screen renderer).
     * Static renderers (PDF/image/HTML) return an empty optional.
     *
     * @see NtxPageLifecycle
     */
    default NOptional<NtxPageLifecycle> pageLifecycle() {
        Object v = getCapability(NtxPageLifecycle.CAPABILITY_PAGE_LIFECYCLE);
        if (v instanceof NtxPageLifecycle) {
            return NOptional.of((NtxPageLifecycle) v);
        }
        return NOptional.ofNamedEmpty("pageLifecycle");
    }

    /**
     * Whether the page being rendered is currently shown to the user. When the
     * host does not track page visibility (PDF/image/HTML or an ad-hoc render)
     * this returns {@code true} so components keep their usual behaviour.
     *
     * @see NtxPageLifecycle#isShowing()
     */
    default boolean isPageShowing() {
        NOptional<NtxPageLifecycle> l = pageLifecycle();
        return !l.isPresent() || l.get().isShowing();
    }

    /**
     * Requests one more animation frame from the hosting view (no-op when there
     * is none). Prefer this over {@link #repaint()} for continuous animation so
     * a single page-level frame driver can throttle all components together.
     *
     * @see NtxPageLifecycle#requestFrame()
     */
    default void requestFrame() {
        NtxPageLifecycle l = pageLifecycle().orNull();
        if (l != null) {
            l.requestFrame();
        }
    }

    /**
     * Seconds elapsed since the page being rendered became visible; {@code 0}
     * while the page is hidden or when the host does not track page visibility
     * (static PDF/image/HTML renders).
     *
     * <p>
     * This is the <em>shared animation clock</em>. A component's animation state
     * should be a pure function of this value and the component's own rate
     * (speed / period property), so any number of components can animate at
     * different speeds while sharing a single page-level frame driver (FPS).
     * Do not use {@link System#currentTimeMillis()} directly in a component.
     *
     * @see NtxPageLifecycle#showElapsedSeconds()
     */
    default double pageElapsedSeconds() {
        NtxPageLifecycle l = pageLifecycle().orNull();
        return l == null ? 0.0 : l.showElapsedSeconds();
    }

    /**
     * Convenience for animated components: requests one animation frame only when
     * the component itself wants to animate ({@code wantsAnimation}), the host is
     * actually animating ({@link #isAnimate()}) and the page is currently showing
     * ({@link #isPageShowing()}). Otherwise it is a no-op, so it is always safe to
     * call once at the end of a render method.
     *
     * <p>
     * This requests the page's <em>frame rate (FPS)</em>; it does not define an
     * animation speed. Speed is the component's own rate applied to
     * {@link #pageElapsedSeconds()}. The frame <em>period</em> is owned by the
     * hosting view, so several animated components on the same page share a
     * single throttled frame driver.
     *
     * @param wantsAnimation whether this node instance wants animation (its own flag)
     * @see #requestFrame()
     */
    default void requestAnimationFrame(boolean wantsAnimation) {
        if (wantsAnimation && isAnimate() && isPageShowing()) {
            requestFrame();
        }
    }

    /**
     * Registers a page-owned periodic animation tick for the current node; see
     * {@link NtxPageLifecycle#newTimer(Object, long, Runnable)}. The tick runs on
     * the UI thread every {@code periodMs} while the page is showing, and is
     * disposed with the page. Ticks with the same period share one underlying
     * timer. Returns {@link NtxAnimationTimer#NONE} for static renders
     * (PDF/image/HTML), so it is always safe to call.
     *
     * <p>
     * Prefer {@link #pageElapsedSeconds()} + {@link #requestFrame()} for
     * animations whose state is a pure function of time; use a timer only for
     * genuinely stateful stepping that must happen independently of repaint.
     *
     * @param periodMs desired period between ticks in milliseconds (&gt; 0)
     * @param tick     callback invoked on the UI thread while the page is showing
     */
    default NtxAnimationTimer newTimer(long periodMs, Runnable tick) {
        NtxPageLifecycle l = pageLifecycle().orNull();
        return l == null ? NtxAnimationTimer.NONE : l.newTimer(node(), periodMs, tick);
    }

    /**
     * Returns a disposable copy of the current graphics whose clip is
     * intersected with {@code bounds}. The caller is responsible for calling
     * {@link NTxGraphics#dispose()} on the result. Prefer {@link #withinClip}
     * which does it automatically.
     *
     * <p>This is the reusable low-level primitive behind the
     * {@link net.thevpc.ntexup.api.document.style.NTxPropName#OVERFLOW} policy
     * and the manual crop used by components (for example the antenna panels)
     * that must never paint outside their own box.</p>
     */
    default NTxGraphics clippedGraphics(NTxBounds2D bounds) {
        NTxGraphics g = graphics().copy();
        g.graphics2D().clip(new Rectangle2D.Double(
                bounds.minX(), bounds.minY(), bounds.widthX(), bounds.widthY()));
        return g;
    }

    /**
     * Returns a disposable copy of the current graphics whose clip is
     * intersected with {@code shape}; see {@link #clippedGraphics(NTxBounds2D)}.
     */
    default NTxGraphics clippedGraphics(Shape shape) {
        NTxGraphics g = graphics().copy();
        g.graphics2D().clip(shape);
        return g;
    }

    /**
     * Runs {@code body} with a renderer context whose graphics is clipped to
     * {@code bounds}, then restores (disposes) the clip. The current context is
     * left untouched, so sibling nodes are never affected by the crop.
     *
     * <p>The clip is applied to a private copy of the graphics; anything the
     * component paints outside {@code bounds} is discarded, exactly like a Swing
     * {@code JPanel} painting into its own rectangle.</p>
     */
    default void withinClip(NTxBounds2D bounds, Consumer<NTxRendererContext> body) {
        NTxGraphics g = clippedGraphics(bounds);
        try {
            body.accept(withGraphics(g));
        } finally {
            g.dispose();
        }
    }

    /**
     * Same as {@link #withinClip(NTxBounds2D, Consumer)} but clips to an
     * arbitrary shape.
     */
    default void withinClip(Shape shape, Consumer<NTxRendererContext> body) {
        NTxGraphics g = clippedGraphics(shape);
        try {
            body.accept(withGraphics(g));
        } finally {
            g.dispose();
        }
    }

    NTxNodeBuilderContext buildContext();

    boolean isSomeChange();

    NTxBounds2D selfBounds2D();

    NTxBounds2D globalBounds2D();

    NTxBounds2D parentBounds2D();

    NTxBounds2D realBounds2D();

    NTxBounds2D realGlobalBounds2D();

    NTxBounds2D defaultSelfBounds2D();

    NTxBounds3D selfBounds3D();

    NTxBounds3D globalBounds3D();

    NTxBounds3D parentBounds3D();

    NTxBounds3D realBounds3D();

    NTxBounds3D realGlobalBounds3D();

    NTxBounds3D defaultSelfBounds3D();

    NTxRendererContext dryMode();

    long pageStartTime();

    ImageObserver imageObserver();

    boolean isDry();

    default NTxRenderSession session() {
        return NTxRenderSessionNoOp.INSTANCE;
    }


    NTxGraphics graphics();


    void render();


    NOptional<NElement> computePropertyValue(String s, String... synonyms);

    List<NTxProp> computeProperties();

    NTxRendererContext withBuilderContext(NTxNodeBuilderContext builderContext);

    NTxRendererContext resolveNode(NTxNode node, NTxBounds2D parentBounds);

    NTxRendererContext withDefaultStyles(NTxProperties defaultStyles);

    NTxRendererContext withParentBounds(NTxBounds2D bounds2);

    NTxRendererContext withGraphics(NTxGraphics graphics);

    NTxRendererContext withRealBounds3D(NTxBounds3D realBounds3D);

    NTxRendererContext withRealGlobalBounds3D(NTxBounds3D realGlobalBounds3D);

    NTxSizeRef sizeRef();

    boolean isPrint();

    boolean isAnimate();

    void repaint();

    Object getCapability(String name);

    boolean hasCapability(String name);

    boolean isCapability(String name);

    void highlightNutsText(String lang, String rawText, NText parsedText, NTxTextRendererBuilder result);

    NOptional<Paint> getColorProperty(String propName);

    NTxDouble2 getOrigin(NTxDouble2 a);

    NTxDouble2 getPosition(NTxDouble2 a);

    NElement getStroke();

    NTxBounds2D selfBounds2D(NTxDouble2 selfSize, NTxDouble2 minSize);

    boolean isVisible();

    boolean isLayoutNone();

    double getFontSize();

    String getFontFamily();

    boolean isFontUnderlined();

    boolean isFontStrike();

    boolean isFontBold();

    boolean isFontItalic();

    Font getFont();

    NTxDouble2 getRoundCornerArcs();

    int getColSpan();

    int getRowSpan();

    Boolean get3D();

    Boolean getRaised();

    NOptional<NTxShadow> readStyleAsShadow(String s);

    Paint getForegroundColor(boolean force);
    Paint getLineColor(boolean force);

    Paint resolveGridColor();

    Paint resolveBackgroundColor();

    Paint resolveLineColor(boolean force);

    boolean isDrawContour();

    boolean requireDrawGrid();

    boolean requireFillBackground();

    int getColumns();

    int getRows();

    boolean isDebug();

    int getDebugLevel();

    Color getDebugColor();

    NOptional<NTxPoint2D> getStyleAsShadowDistance(Object sv);

    NTxSizeD mapDim(NTxSizeD d, NTxSizeD base);

    Stroke resolveStroke();

    boolean withStroke(Runnable r);

    boolean applyStroke();

    void applyFont();

    NTxSizeD mapDim(double w, double h);

    NTxBounds2D bounds(NTxRendererContext ctx);

    boolean applyForeground(boolean force);

    boolean applyBackgroundColor();

    boolean applyGridColor(boolean force);

    boolean applyLineColor(boolean force);

    boolean applyContourColor(boolean force);

    NOptional<Color> colorFromPaint(Paint p);

    void paintBorderLine(NTxBounds2D a);

    void paintBackground(NTxBounds2D a);

    NTxCompiledPage compiledPage();

    NTxCompiledDocument compiledDocument();

    NTxSourceMonitor sourceMonitor();

    void drawContour();

    void renderDetachedNode(NElement childNode, NTxBounds2D relativeBounds);

    void renderDetachedNode(NTxNode childNode, NTxBounds2D relativeBounds);

    /// ///////
    NTxRendererContext resolveNode(NTxNode node);

    NTxRendererContext withElement(NElement element);

    NTxRendererContext withNode(NTxNode parent);

    NTxRendererContext pushNode(NTxNode node);

    NTxRendererContext popNode();

}
