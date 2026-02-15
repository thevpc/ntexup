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
import java.awt.image.ImageObserver;
import java.util.List;

public interface NTxRendererContext extends NTxResolutionContext {
    String CAPABILITY_PRINT = "print";
    String CAPABILITY_ANIMATE = "animate";

    default NTxSizeRequirements sizeRequirementsOf() {
        return engine().getRenderer(node().type()).get().sizeRequirements(this);
    }

    NTxNodeBuilderContext buildContext();

    boolean isSomeChange();

    NTxBounds2D selfBounds();

    NTxBounds2D defaultSelfBounds();

    NTxRendererContext dryMode();

    long pageStartTime();

    ImageObserver imageObserver();

    boolean isDry();

    NTxBounds2D getGlobalBounds();

    NTxGraphics graphics();

    NTxBounds2D parentBounds();

    void render();


    NOptional<NElement> computePropertyValue(String s, String... synonyms);

    List<NTxProp> computeProperties();

    NTxRendererContext withBuilderContext(NTxNodeBuilderContext builderContext);

    NTxRendererContext resolveNode(NTxNode node, NTxBounds2D parentBounds);

    NTxRendererContext withDefaultStyles(NTxProperties defaultStyles);

    NTxRendererContext withParentBounds(NTxBounds2D bounds2);

    NTxRendererContext withGraphics(NTxGraphics graphics);

    NTxSizeRef sizeRef();

    boolean isPrint();

    boolean isAnimate();

    void repaint();

    Object getCapability(String name);

    boolean hasCapability(String name);

    public boolean isCapability(String name);

    void highlightNutsText(String lang, String rawText, NText parsedText, NTxTextRendererBuilder result);

    NOptional<Paint> getColorProperty(String propName);

    NTxDouble2 getOrigin(NTxDouble2 a);

    NTxDouble2 getPosition(NTxDouble2 a);

    NElement getStroke();

    NTxBounds2D selfBounds(NTxDouble2 selfSize, NTxDouble2 minSize);

    boolean isVisible();

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

    Paint resolveGridColor();

    Paint resolveBackgroundColor();

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
