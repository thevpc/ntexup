package net.thevpc.ntexup.engine.renderer;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.document.node.NTxItem;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeDef;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.engine.NTxNodeBuilderContext;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.eval.NTxVar;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.renderer.NTxNodeRenderer;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererBuilder;
import net.thevpc.ntexup.api.source.NTxSourceMonitor;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxRendererContext;
import net.thevpc.ntexup.api.util.NTxSizeRef;
import net.thevpc.ntexup.engine.eval.NTxResolutionContextImpl;
import net.thevpc.ntexup.engine.parser.DefaultNTxNodeFactoryParseContext;
import net.thevpc.ntexup.engine.renderer.text.NTxHighlighterMapper;
import net.thevpc.ntexup.engine.util.NTxNodeRendererUtils;
import net.thevpc.nuts.elem.NElement;
import net.thevpc.nuts.text.NText;
import net.thevpc.nuts.util.NAssert;
import net.thevpc.nuts.util.NLiteral;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.NOptional;

import java.awt.*;
import java.awt.image.ImageObserver;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;


public class DefaultNTxRendererContext extends NTxResolutionContextImpl implements NTxRendererContext {
    private NTxBounds2D selfBounds;
    private final NTxNodeBuilderContext buildContext;
    private final ImageObserver imageObserver;
    private final NTxGraphics g3;
    private final NTxBounds2D globalBound;
    private final NTxBounds2D parentBounds;
    private final Map<String, Object> capabilities;
    private final long pageStartTime;
    private final boolean someChange;
    private final NTxCompiledPage compiledPage;
    private final Runnable repainter;
    private final NTxProperties defaultStyles;
    private final boolean dry;
    private final NTxCompiledPage page;

    public DefaultNTxRendererContext(NTxCompiledPage page, NTxNode[] path, NTxEngine engine, NTxGraphics g, NTxBounds2D selfBounds, NTxBounds2D parentBounds, NTxBounds2D globalBound, NTxCompiledPage compiledPage,
                                     boolean someChange, long pageStartTime, Map<String, Object> capabilities, ImageObserver imageObserver, Runnable repainter,
                                     NTxDocument document

    ) {
        this(page, path, engine, g, selfBounds, parentBounds, globalBound, compiledPage,
                someChange, pageStartTime, capabilities, imageObserver, repainter,
                null, false,
                null,
                null, null, document,
                null,
                null,
                null);
    }

    public DefaultNTxRendererContext(NTxCompiledPage page, NTxNode[] path, NTxEngine engine, NTxGraphics g, NTxBounds2D selfBounds, NTxBounds2D parentBounds, NTxBounds2D globalBound, NTxCompiledPage compiledPage,
                                     boolean someChange, long pageStartTime, Map<String, Object> capabilities, ImageObserver imageObserver, Runnable repainter,
                                     NTxProperties defaultStyles, boolean dry,
                                     NTxNodeBuilderContext buildContext,
                                     NElement element, NTxNodeDef def, NTxDocument document,
                                     Map<String, NTxVar> vars,
                                     Map<String, NTxNodeDef> definitions,
                                     Map<String, NTxFunction> functions
    ) {
        super(path, element, def, true, engine, document, vars, definitions, functions);
        setVar("selfBounds", () -> {
            NTxBounds2D nTxBounds2 = DefaultNTxRendererContext.this.selfBounds();
            return NTxUtils.toElement(nTxBounds2);
        });
        this.page = page;
        this.selfBounds = selfBounds;
        this.parentBounds = parentBounds;
//        this.parentBounds = new NTxBounds2(0, 0, parentBounds.getWidth(), parentBounds.getHeight());
        this.globalBound = globalBound;
        this.g3 = g;
        this.compiledPage = compiledPage;
        this.someChange = someChange;
        this.pageStartTime = pageStartTime;
        this.capabilities = capabilities == null ? new HashMap<>() : new HashMap<>(capabilities);
        this.imageObserver = imageObserver;
        this.repainter = repainter;
        this.defaultStyles = defaultStyles;
        this.dry = dry;
        this.buildContext = buildContext;
    }

    @Override
    public NTxNodeBuilderContext buildContext() {
        return buildContext;
    }

    //    public DefaultNTxNodeRendererContext(NTxNode node, NTxEngine engine, NTxGraphics g, NTxBounds2 selfBounds, Dimension parentBounds, NTxCompiledPage compiledPage, boolean someChange,
//                                      long pageStartTime, Map<String, Object> capabilities, ImageObserver imageObserver, Runnable repainter) {
//        this(node, engine, g, selfBounds, parentBounds, new NTxBounds2(0, 0, parentBounds.getWidth(), parentBounds.getHeight()), compiledPage, someChange, pageStartTime, capabilities,imageObserver,repainter);
//    }

    public boolean isSomeChange() {
        return someChange;
    }


    public long pageStartTime() {
        return pageStartTime;
    }


    public void setCapability(String name, Object value) {
        if (value == null) {
            capabilities.remove(name);
        } else {
            capabilities.put(name, value);
        }
    }

    public NTxCompiledPage compiledPage() {
        return compiledPage;
    }

    public NTxCompiledDocument compiledDocument() {
        return compiledPage.document();
    }

    @Override
    public NTxSourceMonitor sourceMonitor() {
        return compiledPage.document().rawDocument().sourceMonitor();
    }


    @Override
    public NTxBounds2D selfBounds() {
        if (selfBounds == null) {
            selfBounds = engine().getRenderer(node().type()).get().selfBounds(this);
        }
        return selfBounds;
    }

    @Override
    public NTxBounds2D defaultSelfBounds() {
        return NTxValueByName.selfBounds(null, null, this);
    }

    @Override
    public NTxRendererContext withDefaultStyles(NTxProperties defaultStyles) {
        return copyAsRenderer(page, path, engine, g3, selfBounds, parentBounds, globalBound, compiledPage, someChange, pageStartTime, capabilities, imageObserver, repainter, defaultStyles, dry, buildContext, element, def, document, vars, definitions, functions);
    }

    @Override
    public NTxRendererContext withBuilderContext(NTxNodeBuilderContext builderContext) {
        return copyAsRenderer(page, path, engine, g3, null, parentBounds == null ? selfBounds() : parentBounds, globalBound, compiledPage, someChange, pageStartTime, capabilities, imageObserver, repainter, defaultStyles, dry, builderContext, element, def, document, vars, definitions, functions);
    }

    @Override
    public NTxRendererContext resolveNode(NTxNode node, NTxBounds2D parentBounds) {
        List<NTxNode> all = new ArrayList<>(Arrays.asList(path));
        all.add(NAssert.requireNamedNonNull(node, "parent"));
        return copyAsRenderer(page, all.toArray(new NTxNode[0]), engine, g3, null, parentBounds == null ? selfBounds() : parentBounds, globalBound, compiledPage, someChange, pageStartTime, capabilities, imageObserver, repainter, defaultStyles, dry, buildContext, element, def, document, vars, definitions, functions);
    }


    @Override
    public NTxRendererContext resolveNode(NTxNode node) {
        return (NTxRendererContext) super.resolveNode(node);
    }

    @Override
    public NTxRendererContext popNode() {
        return (NTxRendererContext) super.popNode();
    }

    @Override
    public NTxRendererContext pushNode(NTxNode node) {
        return (NTxRendererContext) super.pushNode(node);
    }

    @Override
    public NTxRendererContext withNode(NTxNode node) {
        return (NTxRendererContext) super.withNode(node);
    }

    @Override
    public NTxRendererContext withElement(NElement node) {
        return (NTxRendererContext) super.withElement(node);
    }

    @Override
    public NTxRendererContext setNode(NTxNode node) {
        return (NTxRendererContext) super.setNode(node);
    }

    @Override
    public NTxRendererContext withParentBounds(NTxBounds2D parentBounds) {
        if (parentBounds == null || Objects.equals(parentBounds, this.parentBounds)) {
            return this;
        }
        return copyAsRenderer(page, path, engine, g3, selfBounds, parentBounds, globalBound, compiledPage, someChange, pageStartTime, capabilities, imageObserver, repainter, defaultStyles, dry, buildContext, element, def, document, vars, definitions, functions);
    }

    @Override
    protected NTxResolutionContext copyAs(NTxNode[] path, NElement element, NTxNodeDef def, boolean isInPage, NTxEngine engine, NTxDocument document, Map<String, NTxVar> vars, Map<String, NTxNodeDef> definitions, Map<String, NTxFunction> functions) {
        return copyAsRenderer(page, path, engine, g3, selfBounds, parentBounds, globalBound, compiledPage, someChange, pageStartTime, capabilities, imageObserver, repainter, defaultStyles, dry, buildContext, element, def, document, vars, definitions, functions);
    }

    @Override
    public NTxRendererContext dryMode() {
        if (dry) {
            return this;
        }
        return copyAsRenderer(page, path, engine, g3, selfBounds, parentBounds, globalBound, compiledPage, someChange, pageStartTime, capabilities, imageObserver, repainter, defaultStyles, true, buildContext, element, def, document, vars, definitions, functions);
    }

    @Override
    public NTxRendererContext withGraphics(NTxGraphics graphics) {
        if (graphics == null || graphics == g3) {
            return this;
        }
        return copyAsRenderer(page, path, engine, graphics, selfBounds, parentBounds, globalBound, compiledPage, someChange, pageStartTime, capabilities, imageObserver, repainter, defaultStyles, dry, buildContext, element, def, document, vars, definitions, functions);
    }

    @Override
    public NTxSizeRef sizeRef() {
        NTxBounds2D b = parentBounds();
        NTxBounds2D gb = getGlobalBounds();
        return new NTxSizeRef(
                b.getWidth(), b.getHeight(),
                gb.getWidth(), gb.getHeight()
        );
    }

    @Override
    public void render() {
        NTxNode p = node();
        NOptional<NTxNodeRenderer> renderer = engine().getRenderer(p.type());
        if (renderer.isPresent()) {
            renderer.get().render(this);
        } else {
            engine().log().log(NMsg.ofC("%s for %s", renderer.getMessage().get(), NTxUtils.snippet(p)).asError(), NTxUtils.sourceOf(p));
        }
    }

    @Override
    public void renderDetachedNode(NElement childNode, NTxBounds2D relativeBounds) {
        renderDetachedNode((NTxNode) newDetachedNode(childNode).get(), relativeBounds);
    }

    @Override
    public void renderDetachedNode(NTxNode childNode, NTxBounds2D relativeBounds) {
        //childNode=buildNode(childNode);
        NOptional<NTxNodeRenderer> renderer = engine().getRenderer(childNode.type());
        if (renderer.isPresent()) {
            NTxBounds2D pb = selfBounds();
            NTxBounds2D sb = new NTxBounds2D(
                    relativeBounds.getX() / 100 * pb.getWidth() + pb.getX(),
                    relativeBounds.getY() / 100 * pb.getHeight() + pb.getY(),
                    relativeBounds.getWidth() / 100 * pb.getWidth(),
                    relativeBounds.getHeight() / 100 * pb.getHeight()
            );
            List<NTxNode> all = new ArrayList<>(Arrays.asList(path));
            all.add(NAssert.requireNamedNonNull(childNode, "parent"));
            NTxRendererContext d2 = copyAsRenderer(page, all.toArray(new NTxNode[0]), engine(), graphics(), sb, pb, getGlobalBounds(), compiledPage(),
                    isSomeChange(), pageStartTime(), capabilities, imageObserver, repainter, null, false, buildContext, element, def, document, vars, definitions, functions);
            renderer.get().render(d2);
        } else {
            engine().log().log(NMsg.ofC("%s for %s", renderer.getMessage().get(), NTxUtils.snippet(childNode)).asError(), NTxUtils.sourceOf(node()));
        }
    }

    public NOptional<NTxItem> newDetachedNode(NElement element) {
        DefaultNTxNodeFactoryParseContext r = new DefaultNTxNodeFactoryParseContext(
                page.document().compiledDocument(),
                null,
                engine(),
                new ArrayList<>(Arrays.asList(node())),
                node().source()
        );
        return engine().newNode(element, this.withElement(element));
    }

    @Override
    public NOptional<Paint> getColorProperty(String propName) {
        return NTxValueByName.getColorProperty(propName, this);
    }

    @Override
    public NTxDouble2 getOrigin(NTxDouble2 a) {
        return NTxValueByName.getOrigin(this, a);
    }

    @Override
    public NTxDouble2 getPosition(NTxDouble2 a) {
        return NTxValueByName.getPosition(this, a);
    }

    @Override
    public NElement getStroke() {
        return NTxValueByName.getStroke(this);
    }

    @Override
    public NTxBounds2D selfBounds(NTxDouble2 selfSize, NTxDouble2 minSize) {
        return NTxValueByName.selfBounds(selfSize, minSize, this);
    }

    @Override
    public boolean isVisible() {
        return NTxValueByName.isVisible(this);
    }

    @Override
    public double getFontSize() {
        return NTxValueByName.getFontSize(this);
    }

    @Override
    public String getFontFamily() {
        return NTxValueByName.getFontFamily(this);
    }

    @Override
    public boolean isFontUnderlined() {
        return NTxValueByName.isFontUnderlined(this);
    }

    @Override
    public boolean isFontStrike() {
        return NTxValueByName.isFontStrike(this);
    }

    @Override
    public boolean isFontBold() {
        return NTxValueByName.isFontBold(this);
    }

    @Override
    public boolean isFontItalic() {
        return NTxValueByName.isFontItalic(this);
    }

    @Override
    public Font getFont() {
        return NTxValueByName.getFont(this);
    }

    @Override
    public NTxDouble2 getRoundCornerArcs() {
        return NTxValueByName.getRoundCornerArcs(this);
    }

    @Override
    public int getColSpan() {
        return NTxValueByName.getColSpan(this);
    }

    @Override
    public int getRowSpan() {
        return NTxValueByName.getRowSpan(this);
    }

    @Override
    public Boolean get3D() {
        return NTxValueByName.get3D(this);
    }

    @Override
    public Boolean getRaised() {
        return NTxValueByName.getRaised(this);
    }

    @Override
    public NOptional<NTxShadow> readStyleAsShadow(String s) {
        return NTxValueByName.readStyleAsShadow(s, this);
    }

    @Override
    public Paint getForegroundColor(boolean force) {
        return NTxValueByName.getForegroundColor(this, force);
    }

    @Override
    public Paint resolveGridColor() {
        return NTxValueByName.resolveGridColor(this);
    }

    @Override
    public Paint resolveBackgroundColor() {
        return NTxValueByName.resolveBackgroundColor(this);
    }

    @Override
    public boolean isDrawContour() {
        return NTxValueByName.isDrawContour(this);
    }

    @Override
    public boolean requireDrawGrid() {
        return NTxValueByName.requireDrawGrid(this);
    }

    @Override
    public boolean requireFillBackground() {
        return NTxValueByName.requireFillBackground(this);
    }


    @Override
    public int getColumns() {
        return NTxValueByName.getColumns(this);
    }

    @Override
    public int getRows() {
        return NTxValueByName.getRows(this);
    }

    public boolean isDebug() {
        return NTxValueByName.isDebug(this);
    }

    @Override
    public int getDebugLevel() {
        return NTxValueByName.getDebugLevel(this);
    }

    @Override
    public Color getDebugColor() {
        return NTxValueByName.getDebugColor(this);
    }

    @Override
    public NOptional<NTxPoint2D> getStyleAsShadowDistance(Object sv) {
        return NTxValueByName.getStyleAsShadowDistance(sv, this);
    }


    @Override
    public NTxSizeD mapDim(NTxSizeD d, NTxSizeD base) {
        return NTxNodeRendererUtils.mapDim(d, base);

    }

    @Override
    public Stroke resolveStroke() {
        return NTxNodeRendererUtils.resolveStroke(graphics(), this);

    }

    @Override
    public boolean withStroke(Runnable r) {
        return NTxNodeRendererUtils.withStroke(graphics(), this, r);

    }

    @Override
    public boolean applyStroke() {
        return NTxNodeRendererUtils.applyStroke(graphics(), this);

    }

    @Override
    public void applyFont() {
        NTxNodeRendererUtils.applyFont(graphics(), this);

    }

    @Override
    public NTxSizeD mapDim(double w, double h) {
        return NTxNodeRendererUtils.mapDim(w, h, this);

    }

    @Override
    public NTxBounds2D bounds(NTxRendererContext ctx) {
        return NTxNodeRendererUtils.bounds(this);

    }

    @Override
    public boolean applyForeground(boolean force) {
        return NTxNodeRendererUtils.applyForeground(graphics(), this, force);

    }

    @Override
    public boolean applyBackgroundColor() {
        return NTxNodeRendererUtils.applyBackgroundColor(graphics(), this);

    }

    @Override
    public boolean applyGridColor(boolean force) {
        return NTxNodeRendererUtils.applyGridColor(graphics(), this, force);

    }

    @Override
    public NOptional<Color> colorFromPaint(Paint p) {
        return NTxNodeRendererUtils.colorFromPaint(p);
    }

    @Override
    public void paintBorderLine(NTxBounds2D a) {
        NTxNodeRendererUtils.drawBorderLine(this, graphics(), a);
    }

    @Override
    public void paintBackground(NTxBounds2D a) {
        NTxNodeRendererUtils.paintBackground(this, graphics(), a);
    }

    @Override
    public void drawContour() {
        if (NTxValueByName.isDrawContour(this)) {
            NTxNodeRendererUtils.drawBorderLine(this, graphics(), selfBounds());
        }
    }

    @Override
    public ImageObserver imageObserver() {
        return imageObserver;
    }

    @Override
    public void repaint() {
        if (repainter != null) {
            repainter.run();
        }
    }

    @Override
    public NTxEngine engine() {
        return engine;
    }

//    @Override
//    public NOptional<NElement> resolveVarValue(String varName, NTxNode node) {
//        return engine().resolveVarValue(varName, node, varProvider());
//    }
//
//    @Override
//    public NOptional<NElement> evalExpression(NElement expression, NTxNode node) {
//        return engine().evalExpression(expression, node, varProvider());
//    }
//
//    @Override
//    public NOptional<NTxVar> findVar(String varName, NTxNode node) {
//        return engine().findVar(varName, node, varProvider());
//    }

//    @Override
//    public NOptional<NTxNode> findNodeByProperty(String varName, String varValue, NTxNode node) {
//        return engine().findNodeByProperty(varName, varValue, node, varProvider());
//    }

    @Override
    public boolean isAnimate() {
        return isCapability(CAPABILITY_ANIMATE);
    }

    @Override
    public boolean isPrint() {
        return isCapability(CAPABILITY_PRINT);
    }

    public Object getCapability(String name) {
        return capabilities.get(name);
    }

    public boolean hasCapability(String name) {
        return capabilities.get(name) != null;
    }

    public boolean isCapability(String name) {
        return NLiteral.of(getCapability(name))
                .asBoolean().orElse(false);
    }

    @Override
    public void highlightNutsText(String lang, String rawText, NText parsedText, NTxTextRendererBuilder result) {
        NTxHighlighterMapper.highlightNutsText(lang, rawText, parsedText, this, result);
    }

    @Override
    public NTxLogger log() {
        return engine().log();
    }

    @Override
    public boolean isDry() {
        return dry;
    }

    @Override
    public NTxGraphics graphics() {
        return g3;
    }

    public NTxBounds2D getGlobalBounds() {
        return globalBound;
    }

    public NTxBounds2D parentBounds() {
        return parentBounds;
    }

    @Override
    public List<NTxProp> computeProperties() {
        NTxNode node = node();
        List<NTxProp> inherited = engine().computeInheritedProperties(node);
        NTxProperties hp = new NTxProperties(node);
        if (this.defaultStyles != null) {
            hp.set(this.defaultStyles.toArray());
        }
        hp.set(node.getProperties());
        for (NTxProp h : inherited) {
            if (!hp.containsKey(h.getName())) {
                hp.set(h);
            }
        }
        return hp.toList();
    }

    @Override
    public NOptional<NElement> computePropertyValue(String s, String[] others) {
        NAssert.requireNamedNonBlank(s, "property name");
        NOptional<NElement> r = computePropertyValueImpl(NTxUtils.uids(new String[]{s}, others));
        if (r.isPresent()) {
            NElement y = r.get();
            y = evalExpression(y).orNull();
            if (y != null) {
                return NOptional.of(y);
            }
        }
        return r;
    }

    private NOptional<NElement> computePropertyValueImpl(String... all) {
        NOptional<NElement> y = null;
        if (node != null) {
            y = engine().computeProperty(node, all).map(NTxProp::getValue).filter(x -> x != null);
            if (y.isPresent()) {
                return y;
            }
        }
        if (this.defaultStyles != null) {
            y = this.defaultStyles.get(all).map(NTxProp::getValue).filter(x -> x != null);
            if (y.isPresent()) {
                return y;
            }
        }
//        if (this.node != null) {
//            for (String s : all) {
//                y = this.node.getProperty(s).map(NTxProp::getValue).filter(x -> x != null);
//                if (y.isPresent()) {
//                    return y;
//                }
//            }
//        }
//        return computePropertyValue(null, all[0], Arrays.copyOfRange(all, 1, all.length));
        return NOptional.ofNamedEmpty("value for prop " + String.join(",", all));
    }

    protected NTxRendererContext copyAsRenderer(NTxCompiledPage page, NTxNode[] path, NTxEngine engine, NTxGraphics g, NTxBounds2D selfBounds, NTxBounds2D parentBounds, NTxBounds2D globalBound, NTxCompiledPage compiledPage,
                                                boolean someChange, long pageStartTime, Map<String, Object> capabilities, ImageObserver imageObserver, Runnable repainter,
                                                NTxProperties defaultStyles, boolean dry,
                                                NTxNodeBuilderContext buildContext,
                                                NElement element, NTxNodeDef def, NTxDocument document,
                                                Map<String, NTxVar> vars,
                                                Map<String, NTxNodeDef> definitions,
                                                Map<String, NTxFunction> functions
    ) {
        return new DefaultNTxRendererContext(
                page, path, engine, g, selfBounds, parentBounds, globalBound, compiledPage,
                someChange, pageStartTime, capabilities, imageObserver, repainter,
                defaultStyles, dry,
                buildContext,
                element, def, document, vars, definitions, functions
        );
    }
}
