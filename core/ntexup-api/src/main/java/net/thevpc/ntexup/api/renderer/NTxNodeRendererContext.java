package net.thevpc.ntexup.api.renderer;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxVar;
import net.thevpc.ntexup.api.eval.NTxVarProvider;
import net.thevpc.ntexup.api.log.NTxLogger;
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

public interface NTxNodeRendererContext {
    String CAPABILITY_PRINT = "print";
    String CAPABILITY_ANIMATE = "animate";

    default NTxSizeRequirements sizeRequirementsOf(NTxNode p) {
        return engine().getRenderer(p.type()).get().sizeRequirements(this);
    }

    NTxNodeBuilderContext buildContext();
    NTxNode node();

    boolean isSomeChange();

    NTxBounds2D selfBounds();

    NTxBounds2D defaultSelfBounds();

    NTxLogger log();

    NTxNodeRendererContext dryMode();

    NTxVarProvider varProvider();

    long pageStartTime();

    ImageObserver imageObserver();

    boolean isDry();

    NTxBounds2D getGlobalBounds();

    NTxGraphics graphics();

    NTxBounds2D parentBounds();

    void render();

    NOptional<NTxVar> findVar(String varName, NTxNode node);

    NOptional<NElement> resolveVarValue(String varName, NTxNode node);

    NOptional<NElement> evalExpression(NElement expression, NTxNode node);


    NOptional<NTxNode> findNodeByProperty(String varName, String varValue, NTxNode node);

    NTxEngine engine();

    NTxDocumentFactory documentFactory();

    NOptional<NElement> computePropertyValue(NTxNode t, String s, String[] synonyms, NTxVarProvider varProvider);

    NOptional<NElement> computePropertyValue(NTxNode t, String s, String... synonyms);

    List<NTxProp> computeProperties(NTxNode t);

    NTxNodeRendererContext withBuilderContext(NTxNodeBuilderContext builderContext);
    NTxNodeRendererContext withChild(NTxNode node, NTxBounds2D parentBounds);

    NTxNodeRendererContext withChild(NTxNode node);

    NTxNodeRendererContext withNode(NTxNode node);

    NTxNodeRendererContext withDefaultStyles(NTxProperties defaultStyles);

    NTxNodeRendererContext withParentBounds(NTxBounds2D bounds2);

    NTxNodeRendererContext withGraphics(NTxGraphics graphics);

    NTxSizeRef sizeRef();

    boolean isPrint();

    boolean isAnimate();

    void repaint();

    Object getCapability(String name);

    boolean hasCapability(String name);

    public boolean isCapability(String name);

    void highlightNutsText(String lang, String rawText, NText parsedText, NTxNode p, NTxTextRendererBuilder result);

    NOptional<Paint> getColorProperty(String propName, NTxNode t);

    NTxDouble2 getOrigin(NTxNode t, NTxDouble2 a);

    NTxDouble2 getPosition(NTxNode t, NTxDouble2 a);

    NElement getStroke(NTxNode t);

    NTxBounds2D selfBounds(NTxNode t, NTxDouble2 selfSize, NTxDouble2 minSize);

    boolean isVisible(NTxNode t);

    double getFontSize(NTxNode t);

    String getFontFamily(NTxNode t);

    boolean isFontUnderlined(NTxNode t);

    boolean isFontStrike(NTxNode t);

    boolean isFontBold(NTxNode t);

    boolean isFontItalic(NTxNode t);

    Font getFont(NTxNode t);

    NTxDouble2 getRoundCornerArcs(NTxNode t);

    int getColSpan(NTxNode t);

    int getRowSpan(NTxNode t);

    Boolean get3D(NTxNode t);

    Boolean getRaised(NTxNode t);

    NOptional<NTxShadow> readStyleAsShadow(NTxNode p, String s);

    Paint getForegroundColor(NTxNode p, boolean force);

    Paint resolveGridColor(NTxNode p);

    Paint resolveBackgroundColor(NTxNode p);

    boolean isDrawContour(NTxNode p);

    boolean requireDrawGrid(NTxNode p);

    boolean requireFillBackground(NTxNode p);

    int getColumns(NTxNode p);

    int getRows(NTxNode p);

    boolean isDebug(NTxNode p);

    int getDebugLevel(NTxNode p);

    Color getDebugColor(NTxNode p);

    NOptional<NTxPoint2D> getStyleAsShadowDistance(Object sv);

    NTxSizeD mapDim(NTxSizeD d, NTxSizeD base);

    Stroke resolveStroke(NTxNode t);

    boolean withStroke(NTxNode t, Runnable r);

    boolean applyStroke(NTxNode t);

    void applyFont(NTxNode t);

    NTxSizeD mapDim(double w, double h);

    NTxBounds2D bounds(NTxNode t, NTxNodeRendererContext ctx);

    boolean applyForeground(NTxNode t, boolean force);

    boolean applyBackgroundColor(NTxNode t);

    boolean applyGridColor(NTxNode t, boolean force);

    NOptional<Color> colorFromPaint(Paint p);

    void paintBorderLine(NTxNode t, NTxBounds2D a);

    void paintBackground(NTxNode t, NTxBounds2D a);

    NTxCompiledPage compiledPage();

    NTxCompiledDocument compiledDocument();

    NTxSourceMonitor sourceMonitor();

    void drawContour();

    void renderDetachedNode(NElement childNode, NTxBounds2D relativeBounds);

    void renderDetachedNode(NTxNode childNode, NTxBounds2D relativeBounds);
}
