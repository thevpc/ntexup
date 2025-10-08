package net.thevpc.ntexup.engine.renderer;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.elem2d.*;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxProperties;
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxCompiledPage;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.api.eval.NTxValueByName;
import net.thevpc.ntexup.api.eval.NTxVar;
import net.thevpc.ntexup.api.eval.NTxVarProvider;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.renderer.NTxNodeRenderer;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererBuilder;
import net.thevpc.ntexup.api.source.NTxSourceMonitor;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.api.renderer.NTxGraphics;
import net.thevpc.ntexup.api.renderer.NTxNodeRendererContext;
import net.thevpc.ntexup.api.util.NTxSizeRef;
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


public class DefaultNTxNodeRendererContext implements NTxNodeRendererContext {
    private NTxBounds2 selfBounds;
    private NTxVarProvider nTxVarProvider = new NTxVarProvider() {
        @Override
        public NOptional<NTxVar> findVar(String varName, NTxNode node) {
            switch (varName) {
                case "selfBounds": {
                    return NOptional.of(new NTxVar() {
                        @Override
                        public NElement get() {
                            NTxBounds2 nTxBounds2 = DefaultNTxNodeRendererContext.this.selfBounds();
                            return NTxUtils.toElement(nTxBounds2);
                        }
                    });
                }
            }
            return NOptional.ofNamedEmpty("var " + varName);
        }
    };
    private final ImageObserver imageObserver;
    private final NTxGraphics g3;
    private final NTxEngine engine;
    private final NTxBounds2 globalBound;
    private final NTxBounds2 parentBounds;
    private final Map<String, Object> capabilities;
    private final long pageStartTime;
    private final boolean someChange;
    private final NTxCompiledPage compiledPage;
    private final NTxNode node;
    private final Runnable repainter;
    private final NTxProperties defaultStyles;
    private final boolean dry;


    public DefaultNTxNodeRendererContext(NTxNode node, NTxEngine engine, NTxGraphics g, NTxBounds2 selfBounds, NTxBounds2 parentBounds, NTxBounds2 globalBound, NTxCompiledPage compiledPage,
                                         boolean someChange, long pageStartTime, Map<String, Object> capabilities, ImageObserver imageObserver, Runnable repainter, NTxProperties defaultStyles, boolean dry) {
        this.node = node;
        this.engine = engine;
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
    }

//    public DefaultNTxNodeRendererContext(NTxNode node, NTxEngine engine, NTxGraphics g, NTxBounds2 selfBounds, Dimension parentBounds, NTxCompiledPage compiledPage, boolean someChange,
//                                      long pageStartTime, Map<String, Object> capabilities, ImageObserver imageObserver, Runnable repainter) {
//        this(node, engine, g, selfBounds, parentBounds, new NTxBounds2(0, 0, parentBounds.getWidth(), parentBounds.getHeight()), compiledPage, someChange, pageStartTime, capabilities,imageObserver,repainter);
//    }

    public NTxNode node() {
        return node;
    }

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
    public NTxDocumentFactory documentFactory() {
        return engine().documentFactory();
    }

    @Override
    public NTxBounds2 selfBounds() {
        if (selfBounds == null) {
            selfBounds = engine().getRenderer(node().type()).get().selfBounds(this);
        }
        return selfBounds;
    }

    @Override
    public NTxBounds2 defaultSelfBounds() {
        return NTxValueByName.selfBounds(node(), null, null, this);
    }

    @Override
    public NTxNodeRendererContext withDefaultStyles(NTxProperties defaultStyles) {
        return new DefaultNTxNodeRendererContext(node, engine, g3, selfBounds, parentBounds, globalBound, compiledPage, someChange, pageStartTime, capabilities, imageObserver, repainter, defaultStyles, dry);
    }

    @Override
    public NTxNodeRendererContext withChild(NTxNode node,NTxBounds2 parentBounds) {
        return new DefaultNTxNodeRendererContext(node, engine, g3, null, parentBounds==null?selfBounds():parentBounds, globalBound, compiledPage, someChange, pageStartTime, capabilities, imageObserver, repainter, defaultStyles, dry);
    }

    @Override
    public NTxNodeRendererContext withChild(NTxNode node) {
        return new DefaultNTxNodeRendererContext(node, engine, g3, null, selfBounds(), globalBound, compiledPage, someChange, pageStartTime, capabilities, imageObserver, repainter, defaultStyles, dry);
    }

    @Override
    public NTxNodeRendererContext withNode(NTxNode node) {
        return new DefaultNTxNodeRendererContext(node, engine, g3, null, parentBounds, globalBound, compiledPage, someChange, pageStartTime, capabilities, imageObserver, repainter, defaultStyles, dry);
    }

    @Override
    public NTxNodeRendererContext withParentBounds(NTxBounds2 parentBounds) {
        if(parentBounds==null || Objects.equals(parentBounds, this.parentBounds)) {
            return this;
        }
        return new DefaultNTxNodeRendererContext(node, engine, g3, selfBounds, parentBounds, globalBound, compiledPage, someChange, pageStartTime, capabilities, imageObserver, repainter, defaultStyles, dry);
    }

    @Override
    public NTxNodeRendererContext dryMode() {
        if(dry) {
            return this;
        }
        return new DefaultNTxNodeRendererContext(node, engine, g3, selfBounds, parentBounds, globalBound, compiledPage, someChange, pageStartTime, capabilities, imageObserver, repainter, defaultStyles, true);
    }

    @Override
    public NTxNodeRendererContext withGraphics(NTxGraphics graphics) {
        if(graphics == null || graphics==g3) {
            return this;
        }
        return new DefaultNTxNodeRendererContext(node, engine, graphics, selfBounds, parentBounds, globalBound, compiledPage, someChange, pageStartTime, capabilities, imageObserver, repainter, defaultStyles, dry);
    }

    @Override
    public NTxVarProvider varProvider() {
        return nTxVarProvider;
    }

    @Override
    public NTxSizeRef sizeRef() {
        NTxBounds2 b = parentBounds();
        NTxBounds2 gb = getGlobalBounds();
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
    public NOptional<Paint> getColorProperty(String propName, NTxNode t) {
        return NTxValueByName.getColorProperty(propName, t, this);
    }

    @Override
    public NTxDouble2 getOrigin(NTxNode t, NTxDouble2 a) {
        return NTxValueByName.getOrigin(t, this, a);
    }

    @Override
    public NTxDouble2 getPosition(NTxNode t, NTxDouble2 a) {
        return NTxValueByName.getPosition(t, this, a);
    }

    @Override
    public NElement getStroke(NTxNode t) {
        return NTxValueByName.getStroke(t, this);
    }

    @Override
    public NTxBounds2 selfBounds(NTxNode t, NTxDouble2 selfSize, NTxDouble2 minSize) {
        return NTxValueByName.selfBounds(t, selfSize, minSize, this);
    }

    @Override
    public boolean isVisible(NTxNode t) {
        return NTxValueByName.isVisible(t, this);
    }

    @Override
    public double getFontSize(NTxNode t) {
        return NTxValueByName.getFontSize(t, this);
    }

    @Override
    public String getFontFamily(NTxNode t) {
        return NTxValueByName.getFontFamily(t, this);
    }

    @Override
    public boolean isFontUnderlined(NTxNode t) {
        return NTxValueByName.isFontUnderlined(t, this);
    }

    @Override
    public boolean isFontStrike(NTxNode t) {
        return NTxValueByName.isFontStrike(t, this);
    }

    @Override
    public boolean isFontBold(NTxNode t) {
        return NTxValueByName.isFontBold(t, this);
    }

    @Override
    public boolean isFontItalic(NTxNode t) {
        return NTxValueByName.isFontItalic(t, this);
    }

    @Override
    public Font getFont(NTxNode t) {
        return NTxValueByName.getFont(t, this);
    }

    @Override
    public NTxDouble2 getRoundCornerArcs(NTxNode t) {
        return NTxValueByName.getRoundCornerArcs(t, this);
    }

    @Override
    public int getColSpan(NTxNode t) {
        return NTxValueByName.getColSpan(t, this);
    }

    @Override
    public int getRowSpan(NTxNode t) {
        return NTxValueByName.getRowSpan(t, this);
    }

    @Override
    public Boolean get3D(NTxNode t) {
        return NTxValueByName.get3D(t, this);
    }

    @Override
    public Boolean getRaised(NTxNode t) {
        return NTxValueByName.getRaised(t, this);
    }

    @Override
    public NOptional<NTxShadow> readStyleAsShadow(NTxNode p, String s) {
        return NTxValueByName.readStyleAsShadow(p, s, this);
    }

    @Override
    public Paint getForegroundColor(NTxNode p, boolean force) {
        return NTxValueByName.getForegroundColor(p, this, force);
    }

    @Override
    public Paint resolveGridColor(NTxNode p) {
        return NTxValueByName.resolveGridColor(p, this);
    }

    @Override
    public Paint resolveBackgroundColor(NTxNode p) {
        return NTxValueByName.resolveBackgroundColor(p, this);
    }

    @Override
    public boolean isDrawContour(NTxNode p) {
        return NTxValueByName.isDrawContour(p, this);
    }

    @Override
    public boolean requireDrawGrid(NTxNode p) {
        return NTxValueByName.requireDrawGrid(p, this);
    }

    @Override
    public boolean requireFillBackground(NTxNode p) {
        return NTxValueByName.requireFillBackground(p, this);
    }


    @Override
    public int getColumns(NTxNode p) {
        return NTxValueByName.getColumns(p, this);
    }

    @Override
    public int getRows(NTxNode p) {
        return NTxValueByName.getRows(p, this);
    }

    public boolean isDebug(NTxNode p) {
        return NTxValueByName.isDebug(p, this);
    }

    @Override
    public int getDebugLevel(NTxNode p) {
        return NTxValueByName.getDebugLevel(p, this);
    }

    @Override
    public Color getDebugColor(NTxNode p) {
        return NTxValueByName.getDebugColor(p, this);
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
    public Stroke resolveStroke(NTxNode t) {
        return NTxNodeRendererUtils.resolveStroke(t, graphics(), this);

    }

    @Override
    public boolean withStroke(NTxNode t, Runnable r) {
        return NTxNodeRendererUtils.withStroke(t, graphics(), this, r);

    }

    @Override
    public boolean applyStroke(NTxNode t) {
        return NTxNodeRendererUtils.applyStroke(t, graphics(), this);

    }

    @Override
    public void applyFont(NTxNode t) {
        NTxNodeRendererUtils.applyFont(t, graphics(), this);

    }

    @Override
    public NTxSizeD mapDim(double w, double h) {
        return NTxNodeRendererUtils.mapDim(w, h, this);

    }

    @Override
    public NTxBounds2 bounds(NTxNode t, NTxNodeRendererContext ctx) {
        return NTxNodeRendererUtils.bounds(t, this);

    }

    @Override
    public boolean applyForeground(NTxNode t, boolean force) {
        return NTxNodeRendererUtils.applyForeground(t, graphics(), this, force);

    }

    @Override
    public boolean applyBackgroundColor(NTxNode t) {
        return NTxNodeRendererUtils.applyBackgroundColor(t, graphics(), this);

    }

    @Override
    public boolean applyGridColor(NTxNode t, boolean force) {
        return NTxNodeRendererUtils.applyGridColor(t, graphics(), this, force);

    }

    @Override
    public NOptional<Color> colorFromPaint(Paint p) {
        return NTxNodeRendererUtils.colorFromPaint(p);
    }

    @Override
    public void paintBorderLine(NTxNode t, NTxBounds2 a) {
        NTxNodeRendererUtils.drawBorderLine(t, this, graphics(), a);
    }

    @Override
    public void paintBackground(NTxNode t, NTxBounds2 a) {
        NTxNodeRendererUtils.paintBackground(t, this, graphics(), a);
    }

    @Override
    public void drawContour() {
        if (NTxValueByName.isDrawContour(node(), this)) {
            NTxNodeRendererUtils.drawBorderLine(node(), this, graphics(), selfBounds());
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
    public void highlightNutsText(String lang, String rawText, NText parsedText, NTxNode p, NTxTextRendererBuilder result) {
        NTxHighlighterMapper.highlightNutsText(lang, rawText, parsedText, p, this, result);
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

    public NTxBounds2 getGlobalBounds() {
        return globalBound;
    }

    public NTxBounds2 parentBounds() {
        return parentBounds;
    }

    @Override
    public NOptional<NElement> computePropertyValue(NTxNode t, String s, String... others) {
        return computePropertyValue(t, s, others, null);
    }


//    @Override
//    public List<NTxProp> computeProperties(NTxNode t) {
//        return engine().computeProperties(t);
//    }

    @Override
    public List<NTxProp> computeProperties(NTxNode t) {
        List<NTxProp> inherited = engine().computeInheritedProperties(t);
        NTxProperties hp = new NTxProperties(t);
        if (this.defaultStyles != null) {
            hp.set(this.defaultStyles.toArray());
        }
        hp.set(t.getProperties());
        for (NTxProp h : inherited) {
            if (!hp.containsKey(h.getName())) {
                hp.set(h);
            }
        }
        return hp.toList();
    }

    @Override
    public NOptional<NElement> computePropertyValue(NTxNode t, String s, String[] others, NTxVarProvider varProvider) {
        NAssert.requireNonBlank(s, "property name");
        NOptional<NElement> r = computePropertyValueImpl(t, NTxUtils.uids(new String[]{s}, others));
        if (r.isPresent()) {
            NElement y = r.get();
            y = engine().evalExpression(y, t, varProvider);
            if (y != null) {
                return NOptional.of(y);
            }
        }
        return r;
    }

    private NOptional<NElement> computePropertyValueImpl(NTxNode node, String... all) {
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
        return NOptional.ofNamedEmpty("value for prop "+String.join(",", all));
    }

}
