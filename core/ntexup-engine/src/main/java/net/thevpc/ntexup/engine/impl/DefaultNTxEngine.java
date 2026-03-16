package net.thevpc.ntexup.engine.impl;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2D;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds3D;
import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.DefaultNTxNodeSelector;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.document.style.NTxStyleRuleSelector;
import net.thevpc.ntexup.api.engine.*;
import net.thevpc.ntexup.api.engine.CompileNodeVisitor;
import net.thevpc.ntexup.api.eval.NTxFunctionCallContext;
import net.thevpc.ntexup.api.eval.NTxNodePath;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererFlavor;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.engine.document.*;
import net.thevpc.ntexup.engine.eval.*;
import net.thevpc.ntexup.engine.log.DefaultNTxLogger;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.document.*;
import net.thevpc.ntexup.api.parser.*;
import net.thevpc.ntexup.api.renderer.*;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.parser.ctrlnodes.CtrNTxNodelUncompiled;
import net.thevpc.ntexup.engine.parser.nodeparsers.StylesSpecialParser;
import net.thevpc.ntexup.engine.renderer.DefaultNTxRendererContext;
import net.thevpc.ntexup.engine.ext.NTxNodeBuilderContextImpl;
import net.thevpc.ntexup.engine.log.NTxMessageList;
import net.thevpc.ntexup.engine.log.SilentNTxLogger;
import net.thevpc.ntexup.engine.parser.*;
import net.thevpc.ntexup.engine.parser.resources.NTxSourceFactory;
import net.thevpc.ntexup.engine.renderer.NTxDocumentRendererFactoryContextImpl;
import net.thevpc.ntexup.engine.renderer.NTxGraphicsImpl;
import net.thevpc.nuts.app.NApp;
import net.thevpc.nuts.artifact.NDefinition;
import net.thevpc.nuts.artifact.NDependency;
import net.thevpc.nuts.artifact.NDependencyBuilder;
import net.thevpc.nuts.concurrent.NScoredCallable;
import net.thevpc.nuts.core.NMutableClassLoader;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.ext.NExtensions;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.io.NServiceLoader;
import net.thevpc.nuts.platform.NStoreType;
import net.thevpc.nuts.util.NScorable;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.*;

import javax.imageio.ImageIO;

import net.thevpc.nuts.artifact.NId;
import net.thevpc.nuts.text.NTextBuilder;

/**
 * @author vpc
 */
public class DefaultNTxEngine implements NTxEngine {

    private NTxNodeBuilderList builderContexts;
    private NTxDocumentRendererFactoryList documentRendererFactories;
    private NTxNodeParserFactoryList nodeParserFactories;

    NTxNodeParserList nodeTypeFactories;
    private NTxEngineTools tools;
    //    private List<NTxNodeBuilderContextImpl> customBuilderContexts;
    private NTxDocumentFactory factory;
    private NTxPropCalculator propCalculator;
    private NTxFunctionList functions;
    private final NTxMessageList log = new NTxMessageList();
    NtxTextFlavorList textFlavors;
    private NMutableClassLoader classLoader;
    private final List<NTxDependencyLoadedListener> dependencyLoadedListeners = new ArrayList<>();
    NTxNodeRendererList renderers;
    private NTxImageTypeRendererFactoryList imageTypeRendererFactoryList;
    private final Map<String, Object> env = new HashMap<>();
    private final Set<String> dependenciesLoadingPerformed = new HashSet<>();
    private volatile List<NTxStyleRule> defaultStyles;
    private final AtomicBoolean componentsInitialized = new AtomicBoolean(false);
    private final NTxItemParser itemParser = new NTxItemParser() {
        public NOptional<NTxNodeParser> nodeTypeParser(String id) {
            id = NStringUtils.trim(id);
            if (!id.isEmpty()) {
                initializeComponents();
                NOptional<NTxNodeParser> a = nodeTypeFactories.get(id);
                if (a.isPresent()) {
                    return a;
                }
            }
            return NOptional.ofNamedEmpty("node type parser for NodeType " + id);
        }
    };

    public DefaultNTxEngine() {
        init();
    }

    public DefaultNTxEngine(NMutableClassLoader classLoader) {
        if (classLoader != null) {
            this.classLoader = classLoader;
        }
        init();
    }


    private NOptional<NTxItem> parseItem0(NTxResolutionContext context) {
        NElement element = context.element();
        return NScorable.<NScoredCallable<NTxItem>>query()
                .withName(() -> NMsg.ofC("support for node from type '%s' value '%s'", element.type().id(), NTxUtils.snippet(element)))
                .fromStream(
                        nodeParserFactories.list().stream()
                                .map(x -> x.parseNode(context))
                )
                .getBest().map(x -> x.call())
                .withMessage(() -> NMsg.ofC("support for node from type '%s' value '%s'", element.type().id(), NTxUtils.snippet(element)))
                ;
    }

    public DefaultNTxEngine(ClassLoader classLoader) {
        if (classLoader != null) {
            this.classLoader = NExtensions.of().createMutableClassLoader(classLoader);
        }
        init();
    }

    protected void init() {
        propCalculator = new NTxPropCalculator(this);
        addLog(new DefaultNTxLogger());
        if (classLoader == null) {
            classLoader = NExtensions.of().createMutableClassLoader(Thread.currentThread().getContextClassLoader());
        }
        log().log(NMsg.ofC("starting %s engine...", NMsg.ofStyledPrimary1("NTexUp")).asFineAlert());
        tools = new MyNTxEngineTools(this);
        textFlavors = new NtxTextFlavorList(this);
        functions = new NTxFunctionList(this);
        documentRendererFactories = new NTxDocumentRendererFactoryList(this);
        builderContexts = new NTxNodeBuilderList(this);
        nodeTypeFactories = new NTxNodeParserList(this);
        nodeParserFactories = new NTxNodeParserFactoryList(this);
        renderers = new NTxNodeRendererList(this);
        imageTypeRendererFactoryList = new NTxImageTypeRendererFactoryList(this);


    }

    private void initializeComponents() {
        if (componentsInitialized.compareAndSet(false, true)) {
            log().log(NMsg.ofC("bootstrap base components...").asFineAlert());
            textFlavors.build(new NId[0], false);
            functions.build(new NId[0], false);
            documentRendererFactories.build(new NId[0], false);
            builderContexts.build(new NId[0], false);
            nodeTypeFactories.build(new NId[0], false);
            nodeParserFactories.addBase(new DefaultNTxDocumentItemParserFactory());
            nodeParserFactories.build(new NId[0], false);
            renderers.build(new NId[0], false);
            imageTypeRendererFactoryList.build(new NId[0], false);
            log().log(NMsg.ofC("%s engine ready!", NMsg.ofStyledPrimary1("NTexUp")).asFineAlert());
        }
    }

    public <T> NOptional<T> getEnv(String name) {
        return NOptional.ofNamed((T) env.get(name), name);
    }

    public <T> NOptional<T> computeIfAbsent(String name, Function<String, T> fct) {
        return NOptional.ofNamed((T) env.computeIfAbsent(name, fct == null ? s -> null : fct), name);
    }

    @Override
    public void defaultCompileNodeProperties(NTxNode node, NTxResolutionContext context) {
        for (NTxProp property : node.getProperties()) {
            String n = property.getName();
            NElement v = property.getValue();
            node.setProperty(n, context.evalExpression(v).orElse(NElement.ofNull()));
        }
    }

    @Override
    public void defaultCompileNodeChildren(NTxNode node, NTxResolutionContext context) {
        List<NTxNode> children = node.children();
        node.clearChildren();
        for (NTxNode child : children) {
            context.doWithChild(child, new Consumer<NTxResolutionContext>() {
                @Override
                public void accept(NTxResolutionContext ctx) {
                    context.engine().compileNode(ctx, new FillNodeCompileNodeVisitor(node));
                }
            });
        }
    }

    @Override
    public NTxFunctionCallContext createFunctionArgs(String functionName, NElement[] callArgs, NTxResolutionContext context) {
        return new NTxFunctionCallContextImpl(functionName, callArgs, context);
    }

    public NOptional<NTxNode> findNodeByProperty(String propertyName, Predicate<NElement> propertyValueFilter, NTxResolutionContext context) {
        Set<NTxNode> visited = new HashSet<>();

        // PHASE 1: Walk up the spine to root — check every ancestor including root
        NTxNodePath current = new NTxNodePathImpl(Arrays.asList(context.path()));
        while (current != null && !current.isRoot()) {
            visited.add(current.node());
            if (matches(current.node(), propertyName, propertyValueFilter))
                return NOptional.of(current.node());
            if (current.isRoot()) break;
            current = current.parent();
        }

        // PHASE 2: For each spine level, BFS into siblings radially
        NTxNodePath spineChild = new NTxNodePathImpl(Arrays.asList(context.path()));
        NTxNodePath spineNode = spineChild.parent();

        while (spineNode != null) {
            NTxNodeProvider children;
            int pivot;
            NTxNodePath finalSpineNode = spineNode;

            if (spineNode.isRoot()) {
                // at document root — use compiled pages, pivot is current page index
                pivot = ((NTxNodePage) spineChild.node()).index();
                if (context.compiledDocument() == null) {
                    children = i -> null;
                } else {
                    children = index -> {
                        NOptional<NTxCompiledPage> i = context.compiledDocument().page(index);
                        if (!i.isPresent()) {
                            return null;
                        }
                        return finalSpineNode.resolve(i.get().compiledPage());
                    };
                }
            } else {
                // within a page — use snapshot tree children
                List<NTxNode> children0 = spineNode.node().children();
                pivot = -1;
                int _i = 0;
                for (NTxNode x : children0) {
                    if (x == spineChild.node() || x.uuid().equals(spineChild.node().uuid())) {
                        pivot = _i;
                    }
                    _i++;
                }
                if (pivot == -1) {
                    throw new NIllegalArgumentException(NMsg.ofC("invalid hierarchy"));
                }
                children = index -> {
                    if (index >= 0 && index < children0.size()) {
                        return finalSpineNode.resolve(children0.get(index));
                    }
                    return null;
                };
            }

            Queue<NTxNodePath> queue = new LinkedList<>();
            Iterator<NTxNodePath> it = oscillatingOrder(children, pivot);
            while (it.hasNext()) {
                NTxNodePath n = it.next();
                if (visited.add(n.node())) {
                    queue.add(n);
                }
            }

            // BFS into each sibling's subtree
            while (!queue.isEmpty()) {
                NTxNodePath node = queue.poll();
                if (matches(node.node(), propertyName, propertyValueFilter))
                    return NOptional.of(node.node());
                for (NTxNode child : node.node().children()) {
                    if (visited.add(child)) {
                        queue.add(node.resolve(child));
                    }
                }
            }

            spineChild = spineNode;
            spineNode = spineNode.parent();
        }

        return NOptional.ofNamedEmpty(propertyName);
    }

    private interface NTxNodeProvider {
        NTxNodePath get(int index);
    }



    private Iterator<NTxNodePath> oscillatingOrder(NTxNodeProvider nodes, int pivot) {
        return new Iterator<NTxNodePath>() {
            int d = 1;
            int step = 0; // 0 = try backward (pivot-d), 1 = try forward (pivot+d)
            boolean moreBackward = true;
            boolean moreForward = true;
            NTxNodePath nextItem = null;
            boolean needsCompute = true;

            private void computeNext() {
                if (!needsCompute) return;
                needsCompute = false;
                nextItem = null;
                while (moreForward || moreBackward) {
                    if (step == 0) {
                        // try backward
                        step = 1;
                        if (moreBackward) {
                            NTxNodePath u = nodes.get(pivot - d);
                            if (u == null) {
                                moreBackward = false;
                            } else {
                                nextItem = u;
                                return;
                            }
                        }
                    }
                    if (step == 1) {
                        // try forward
                        step = 0;
                        int fd = d;
                        d++;
                        if (moreForward) {
                            NTxNodePath u = nodes.get(pivot + fd);
                            if (u == null) {
                                moreForward = false;
                            } else {
                                nextItem = u;
                                return;
                            }
                        }
                    }
                }
            }

            @Override
            public boolean hasNext() {
                computeNext();
                return nextItem != null;
            }

            @Override
            public NTxNodePath next() {
                computeNext();
                needsCompute = true;
                return nextItem;
            }
        };
    }

    private boolean matches(NTxNode node, String prop, Predicate<NElement> filter) {
        NElement element = node.getPropertyValue(prop).orNull();
        return element != null && filter.test(element);
    }

    public List<NTxStyleRule> getDefaultStyles() {
        if (defaultStyles == null) {
            synchronized (this) {
                if (defaultStyles == null) {
                    NElement stylesNode = NElementReader.ofTson().read(NPath.of("classpath:/net/thevpc/ntexup/default-style.ntx", Thread.currentThread().getContextClassLoader()).readString());
                    DefaultNTxNode root = new DefaultNTxNode(NTxNodeType.PAGE_GROUP);
                    NTxResolutionContextImpl context = new NTxResolutionContextImpl(new NTxNode[]{root}, NElement.ofNull(), null, false, this, new DefaultNTxDocument(null), null, null, null,
                            null, null,
                            null, itemParser());
                    List<NTxStyleRule> styles = new ArrayList<>();
                    context.doWithElement(stylesNode, cc -> {
                        NTxItem sc = new StylesSpecialParser().parseNode(cc).call();
                        if (sc instanceof NTxStyleRule) {
                            styles.add((NTxStyleRule) sc);
                        } else if (sc instanceof NTxItemList) {
                            for (NTxItem item : ((NTxItemList) sc).getItems()) {
                                styles.add((NTxStyleRule) item);
                            }
                        }
                    });
                    defaultStyles = Collections.unmodifiableList(styles);
                }
            }
        }
        return defaultStyles;
    }


    public DefaultNTxEngine setEnv(String env, Object value) {
        if (value != null) {
            this.env.put(env, value);
        } else {
            this.env.remove(env);
        }
        return this;
    }

    @Override
    public void dump() {
        dump(x -> log().log(x));
    }

    @Override
    public void dump(Consumer<NMsg> out) {
        out.accept(NMsg.ofC("NTexup Engine"));
        initializeComponents();
        dump_classloader(out);
        textFlavors.dump(out);
        functions.dump(out);
        documentRendererFactories.dump(out);
        builderContexts.dump(out);
        nodeTypeFactories.dump(out);
        nodeParserFactories.dump(out);
        renderers.dump(out);
        imageTypeRendererFactoryList.dump(out);
    }

    private void dump_classloader(Consumer<NMsg> out) {
        List<NDefinition> dependencies = classLoader.getLoadedDependencies();
        out.accept(NMsg.ofC("Dependencies : %s", dependencies.size()));
        for (NDefinition dependency : dependencies) {
            out.accept(NMsg.ofC("\t %s", dependency.getId()));
        }
    }

    public List<NTxImageTypeRendererFactory> imageTypeRendererFactories() {
        return imageTypeRendererFactoryList.list();
    }

    @Override
    public void addDependencyLoadedListener(NTxDependencyLoadedListener listener) {
        this.dependencyLoadedListeners.add(listener);
    }

    public <S> List<S> loadServices(Class<S> serviceClass) {
        return NCollections.list(NServiceLoader.of(serviceClass, null, classLoader.asClassLoader()).loadAll(null));
    }

    public NMutableClassLoader getEngineClassLoader() {
        return classLoader;
    }

    public boolean importDependencies(String... deps) {
        NDependency[] okDeps = Arrays.stream(deps)
                .filter(x -> !NBlankable.isBlank(x))
                .map(x -> NDependency.get(x).orNull())
                .map(x -> {
                    if (x == null) {
                        return null;
                    }
                    NDependencyBuilder b = x.builder();
                    if (NBlankable.isBlank(b.getArtifactId())) {
                        return null;
                    }
                    if (NBlankable.isBlank(x.getGroupId())) {
                        b.setGroupId("net.thevpc.ntexup");
                        String r = b.getArtifactId();
                        if (!r.startsWith("ntexup-extension-")) {
                            b.setArtifactId("ntexup-extension-" + r);
                        }
                    }
                    if (NBlankable.isBlank(x.getVersion())) {
                        b.setVersion(NTxEngine.CURRENT_VERSION);
                    }
                    return b.build();
                })
                .filter(dep -> dep != null && !dependenciesLoadingPerformed.contains(dep.toString()))
                .toArray(NDependency[]::new);

        if (okDeps.length > 0) {
            dependenciesLoadingPerformed.addAll(Arrays.stream(deps).map(x -> x).collect(Collectors.toList()));
            log().log(NMsg.ofC("importing dependencies %s",
                    NTextBuilder.of()
                            .appendJoined(",", Arrays.asList(okDeps))
                            .build()
            ));
            NId[] u = classLoader.loadDependencies(okDeps);
            if (u.length > 0) {
                log().log(NMsg.ofC("new dependencies %s",
                        NTextBuilder.of()
                                .appendJoined(",", Arrays.asList(u))
                                .build()
                ));
                for (NTxDependencyLoadedListener d : dependencyLoadedListeners) {
                    d.onLoadDependencyLoaded(u);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean importDefaultDependencies() {
        return importDependencies(
                "net.thevpc.ntexup:ntexup-extension-plantuml:" + NTxEngine.CURRENT_VERSION,
                "net.thevpc.ntexup:ntexup-extension-animated-gif:" + NTxEngine.CURRENT_VERSION,
                "net.thevpc.ntexup:ntexup-extension-svg:" + NTxEngine.CURRENT_VERSION,
                "net.thevpc.ntexup:ntexup-extension-shapes2d:" + NTxEngine.CURRENT_VERSION,
                "net.thevpc.ntexup:ntexup-extension-shapes3d:" + NTxEngine.CURRENT_VERSION,
                "net.thevpc.ntexup:ntexup-extension-plot2d:" + NTxEngine.CURRENT_VERSION,
                "net.thevpc.ntexup:ntexup-extension-presenters:" + NTxEngine.CURRENT_VERSION,
                "net.thevpc.ntexup:ntexup-extension-latex:" + NTxEngine.CURRENT_VERSION
        );
    }

    @Override
    public NTxEngineTools tools() {
        return tools;
    }

    public NOptional<NTxTextRendererFlavor> textRendererFlavor(String id) {
        initializeComponents();
        return textFlavors.get(id);
    }

    public List<NTxTextRendererFlavor> textRendererFlavors() {
        initializeComponents();
        return textFlavors.list();
    }

    @Override
    public NTxLogger log() {
        return log;
    }

    @Override
    public NTxLogger addLog(NTxLogger messages) {
        log.add(messages);
        return log;
    }

    @Override
    public NTxLogger removeLog(NTxLogger messages) {
        log.remove(messages);
        return log;
    }

    @Override
    public NOptional<NTxFunction> findFunction(String name) {
        initializeComponents();
        return functions.get(name);
    }

    @Override
    public NTxDocumentFactory documentFactory() {
        if (factory == null) {
            factory = new NTxDocumentFactoryImpl(this);
        }
        return factory;
    }

    @Override
    public NTxEngine parseNode(NElement element, NTxResolutionContext ctx, Consumer<NOptional<NTxItem>> consumer) {
        initializeComponents();
        NAssert.requireNamedNonNull(ctx, "context");
        NAssert.requireNamedNonNull(element, "element");
        if (ctx.source() == null) {
            throw new IllegalArgumentException("unexpected source null");
        }
        if (element.isFragment()) {
            for (NElement child : element.asFragment().get().children()) {
                parseNode(child, ctx, consumer);
            }
            return this;
        }
        NTxResolutionContext finalCtx = ctx.withElement(element);

        NOptional<NTxItem> optional = parseItem0(finalCtx);
        consumer.accept(optional);
        return this;
    }

    @Override
    public NOptional<NTxDocumentStreamRenderer> newPdfRenderer() {
        return newStreamRenderer("pdf");
    }

    @Override
    public NOptional<NTxDocumentStreamRenderer> newHtmlRenderer() {
        return newStreamRenderer("html");
    }

    @Override
    public NOptional<NTxDocumentScreenRenderer> newScreenRenderer() {
        NOptional<NTxDocumentRenderer> u = newRenderer("screen");
        if (u.isPresent()) {
            if (u.get() instanceof NTxDocumentScreenRenderer) {
                return NOptional.of((NTxDocumentScreenRenderer) u.get());
            }
            return NOptional.ofEmpty(NMsg.ofC("support for stream renderer is not of HDocumentScreenRenderer type"));
        }
        return NOptional.ofNamedEmpty("screen renderer");
    }

    @Override
    public NOptional<NTxDocumentStreamRenderer> newStreamRenderer(String type) {
        initializeComponents();
        NOptional<NTxDocumentRenderer> u = newRenderer(type);
        if (u.isPresent()) {
            if (u.get() instanceof NTxDocumentStreamRenderer) {
                return NOptional.of((NTxDocumentStreamRenderer) u.get());
            }
            return NOptional.ofEmpty(NMsg.ofC("support for stream renderer '%s' is not of HDocumentStreamRenderer type", type));
        }

        return NOptional.ofNamedEmpty(type + " renderer");
    }

    @Override
    public NOptional<NTxDocumentRenderer> newRenderer(String type) {
        initializeComponents();
        NTxDocumentRendererFactoryContext ctx = new NTxDocumentRendererFactoryContextImpl(this, type);
        return NScorable.<NScoredCallable<NTxDocumentRenderer>>query()
                .withName(NMsg.ofC("StreamRenderer %s", type))
                .fromStream(
                        documentRendererFactories().stream()
                                .map(x -> x.createDocumentRenderer(ctx))
                )
                .getBest().map(x -> x.call());
    }

    private List<NTxDocumentRendererFactory> documentRendererFactories() {
        initializeComponents();
        return documentRendererFactories.list();
    }

    @Override
    public NTxItemParser itemParser() {
        return itemParser;
    }

    @Override
    public List<NTxNodeParser> nodeTypeFactories() {
        return new ArrayList<>(nodeTypeFactories0().values());
    }

    public List<NTxNodeBuilderContextImpl> builderContexts() {
        initializeComponents();
        return builderContexts.builderContexts();
    }

    private Map<String, NTxNodeParser> nodeTypeFactories0() {
        initializeComponents();
        return nodeTypeFactories.map();
    }

    public NTxNode newDefaultNode(String id) {
        if (NTxNodeType.PAGE.equals(id)) {
            return new DefaultNTxNodePage();
        }
        return new DefaultNTxNode(id);
    }

    public NOptional<NTxNodeParser> nodeTypeParser(String id) {
        return itemParser().nodeTypeParser(id);
    }

    public NTxDocumentLoadingResult compileDocument(NTxDocument document) {
        initializeComponents();
        return new NTxCompiler(this).compileDocument(document);
    }

    @Override
    public void compileNode(NTxResolutionContext ctx, CompileNodeVisitor visitor) {
        initializeComponents();
        new NTxCompiler(this).compileNode(ctx, visitor);
    }

    public void compileNode(NTxNode node, NTxDocument document, NTxCompiledDocument compiledDocument, NTxCompiledPage compiledPage, NTxResolutionContext context, CompileNodeVisitor visitor) {
        initializeComponents();
        node = node.copy();
        if (context == null) {
            context = newContext(node, document, compiledDocument, compiledPage, null);
        }
//        context.setInPage(true);
        compileNode(context, visitor);
    }

    public NTxResolutionContext newContext(NTxNode node, NTxDocument document, NTxCompiledDocument compiledDocument, NTxCompiledPage compiledPage, NTxResolutionContext parentContext) {
        List<NTxNode> p = new ArrayList<>();
        if (node != null) {
            if (node.parent() != null) {
                p.add((NTxNode) node.parent());
            }
        }
        p.add(node);
        return new NTxResolutionContextImpl(p.toArray(new NTxNode[0]), NElement.ofNull(), null, parentContext != null && parentContext.inPage(), this, document, null, null, null,
                compiledDocument,
                compiledPage,
                parentContext,
                parentContext != null ? parentContext.itemParser() : itemParser()
        );
    }

    public boolean validateNode(NTxNode node) {
        return nodeTypeParser(node.type()).get().validateNode(node);
    }

    @Override
    public NTxCompiledDocument loadCompiledDocument(NPath path) {
        NTxDocumentLoadingResult d = loadDocument(path);
        return asCompiledDocument(d.get());
    }

    @Override
    public NTxCompiledDocument asCompiledDocument(NTxDocument document) {
        return new NTxCompiledDocumentImpl(document, this);
    }

    @Override
    public NTxDocumentLoadingResult loadDocument(NPath path) {
        initializeComponents();
        NAssert.requireNamedNonNull(path, "path");
        synchronized (this) {
            if (NTxGitHelper.isGithubFolder(path.toString())) {
                log().log(NMsg.ofC("loading document : loading github repository for %s", path));
                path = NTxGitHelper.resolveGithubPath(path.toString(), log());
            } else {
                log().log(NMsg.ofC("loading document : local file %s", path.toAbsolute()));
            }
            path = path.normalize().toAbsolute();
            NTxSource source = NTxSourceFactory.of(path);
            if (path.exists()) {
                if (path.isRegularFile()) {
                    NTxSource nPathResource = NTxSourceFactory.of(path);
                    NOptional<NElement> f = new NTxDocStreamParser(this).parsePath(path, nPathResource);
                    if (!f.isPresent()) {
                        log().log(f.getMessage().get().asSevere(), nPathResource);
                    }
                    NElement d = f.get();
                    SilentNTxLogger slog = new SilentNTxLogger();
                    try {
                        this.addLog(slog);
                        NOptional<NTxDocument> dd = convertDocument(d, source);
                        if (dd.isPresent()) {
                            return new NTxDocumentLoadingResultImpl(dd.get(), nPathResource, slog.isSuccessful());
                        } else {
                            log().log(dd.getMessage().get().asSevere(), nPathResource);
                        }
                    } finally {
                        this.removeLog(slog);
                    }

                } else if (path.isDirectory()) {
                    NTxDocument document = documentFactory().ofDocument(source);
                    document.sourceMonitor().add(path.resolve(NTxEngineUtils.NTEXUP_EXT_STAR));
                    List<NPath> all = path.stream().filter(x -> x.isRegularFile() && NTxEngineUtils.isNTexupFile(x)).toList();
                    if (all.isEmpty()) {
                        log().log(
                                NMsg.ofC("invalid folder (no valid enclosed files) %s", path.normalize().toAbsolute()).asSevere()
                        );
                        return new NTxDocumentLoadingResultImpl(document, source, false);
                    }
                    NPath main = null;
                    for (String mainFiled : new String[]{
                            "main." + FILE_EXT,}) {
                        NPath m = all.stream().filter(x -> mainFiled.equals(x.getName())).findFirst().orElse(null);
                        if (m != null) {
                            main = m;
                            all.remove(m);
                            break;
                        }
                    }
                    all.sort((a, b) -> a.getName().compareTo(b.getName()));
                    if (main != null) {
                        all.add(0, main);
                    }
                    for (NPath nPath : all) {
                        // document.resources().add(nPath);
                        NOptional<NTxItem> d = null;
                        NTxSource nPathResource = NTxSourceFactory.of(nPath);
                        try {
                            d = loadNode(document.root(), nPath, document);
                        } catch (Exception ex) {
                            log().log(NMsg.ofC("unable to load %s : %s", nPath, ex).asSevere(), nPathResource);
                        }
                        if (d != null) {
                            if (!d.isPresent()) {
                                log().log(NMsg.ofC("invalid file %s", nPath).asSevere(), nPathResource);
                            }
                            updateSource(d.get(), nPathResource);
                            document.root().append(d.get());
                        }
                    }
                    if (document.root().source() == null) {
                        document.root().setSource(NTxSourceFactory.of(path));
                    }
                    return new NTxDocumentLoadingResultImpl(document, source, true);
                } else {
                    log().log(NMsg.ofC("invalid file %s", path).asSevere());
                    return new NTxDocumentLoadingResultImpl(null, source, false);
                }
            }
            log().log(NMsg.ofC("file does not exist %s", path).asSevere());
            return new NTxDocumentLoadingResultImpl(null, source, false);
        }
    }

    private void updateSource(NTxItem item, NTxSource source) {
        if (item != null) {
            if (item instanceof NTxItemList) {
                for (NTxItem hItem : ((NTxItemList) item).getItems()) {
                    updateSource(hItem, source);
                }
            } else if (item instanceof NTxNode) {
                NTxNode i = (NTxNode) item;
                if (i.source() == null) {
                    i.setSource(source);
                }
            }
        }
    }

    @Override
    public NOptional<NTxItem> loadNode(NTxNode into, NPath path, NTxDocument document) {
        initializeComponents();
        if (path.exists()) {
            if (path.isRegularFile()) {
                NTxSource source = NTxSourceFactory.of(path);
                NOptional<NTxItem> d = loadNode0(into, path, document);
                if (d.isPresent()) {
                    updateSource(d.get(), source);
                }
                return d;
            } else if (path.isDirectory()) {
                List<NPath> all = path.stream().filter(x -> x.isRegularFile() && NTxEngineUtils.isNTexupFile(x.getName())).toList();
                all.sort(NTxEngineUtils::comparePaths);
                NTxItem node = null;
                for (NPath nPath : all) {
                    NOptional<NTxItem> d = loadNode0((node instanceof NTxNode) ? (NTxNode) node : null, nPath, document);
                    if (!d.isPresent()) {
                        log().log(NMsg.ofC("invalid file %s", nPath));
                        return NOptional.ofError(() -> NMsg.ofC("invalid file %s", nPath));
                    }
                    updateSource(d.get(), NTxSourceFactory.of(path));
                    if (node == null) {
                        node = d.get();
                    } else {
                        if (node instanceof NTxNode) {
                            ((NTxNode) node).mergeNode(d.get());
                        } else if (node instanceof NTxItemList) {
                            node = new NTxItemList().addAll(((NTxItemList) node).getItems()).add(node);
                        } else {
                            node = new NTxItemList().add(node).add(d.get());
                        }
                    }
                }
                return NOptional.of(node);
            } else {
                log().log(NMsg.ofC("invalid file %s", path));
            }
        }
        log().log(NMsg.ofC("file does not exist %s", path));
        return NOptional.ofError(() -> NMsg.ofC("file does not exist %s", path));
    }

    public NTxDocumentLoadingResult loadDocument(InputStream is) {
        initializeComponents();
        NTxSource source = NTxSourceFactory.of(is);

        SilentNTxLogger slog = new SilentNTxLogger();
        try {
            this.addLog(slog);

            NOptional<NElement> f = new NTxDocStreamParser(this).parseInputStream(is, source);
            if (!f.isPresent()) {
                log().log(f.getMessage().get().asSevere());
            }
            NElement d = f.get();
            NOptional<NTxDocument> dd = convertDocument(d, source);
            if (dd.isPresent()) {
                return new NTxDocumentLoadingResultImpl(dd.get(), source, slog.isSuccessful());
            } else {
                log().log(dd.getMessage().get().asSevere());
                return new NTxDocumentLoadingResultImpl(null, source, false);
            }
        } finally {
            this.removeLog(slog);
        }
    }

    private NOptional<NTxDocument> convertDocument(NElement doc, NTxSource source) {
        if (doc == null) {
            log().log(NMsg.ofPlain("missing document").asError());
            return NOptional.ofNamedEmpty("document");
        }
        initializeComponents();
        NTxDocument docd = documentFactory().ofDocument(source);
        docd.sourceMonitor().add(source);
        docd.root().setSource(source);
        docd.root().append(new CtrNTxNodelUncompiled(doc, source));
        return NOptional.of(docd);
    }

    private NOptional<NTxItem> loadNode0(NTxNode into, NPath path, NTxDocument document) {
        initializeComponents();
        NTxSource source = NTxSourceFactory.of(path);
        document.sourceMonitor().add(source);
        NOptional<NElement> u = new NTxDocStreamParser(this).parsePath(path, source);
        if (!u.isPresent()) {
            return NOptional.ofEmpty(u.getMessage());
        }
        NElement c = u.get();
        ArrayList<NTxNode> parents = new ArrayList<>();
        if (into != null) {
            parents.add(into);
        }
        //document.root().setRaw(c);
        return NOptional.of(new CtrNTxNodelUncompiled(c, source));
//        return newNode(c, new DefaultNTxNodeFactoryParseContext(
//                document,
//                null,
//                this,
//                parents,
//                source
//        ));
    }

    @Override
    public NElement toElement(NTxDocument doc) {
        NTxNode r = doc.root();
        return nodeTypeParser(r.type()).get().toElem(r, this);
    }

    @Override
    public NElement toElement(NTxNode node) {
        return nodeTypeParser(node.type()).get().toElem(node, this);
    }

    @Override
    public NOptional<NTxProp> computeProperty(NTxNode node, String... propertyNames) {
        return propCalculator.computeProperty(node, propertyNames);
    }

    @Override
    public List<NTxProp> computeProperties(NTxNode node) {
        return propCalculator.computeProperties(node);
    }

    @Override
    public List<NTxProp> computeInheritedProperties(NTxNode node) {
        return propCalculator.computeInheritedProperties(node);
    }

    @Override
    public List<NTxStyleRule> computeStyles(NTxNode node) {
        return propCalculator.computeStyles(node);
    }

    @Override
    public List<NTxStyleRule> computeDeclaredStyles(NTxNode node) {
        return propCalculator.computeDeclaredStyles(node);
    }

    @Override
    public Set<String> computeDeclaredStylesClasses(NTxNode node) {
        return computeDeclaredStyles(node).stream().flatMap(x -> {
            NTxStyleRuleSelector s = x.selector();
            if (s instanceof DefaultNTxNodeSelector) {
                DefaultNTxNodeSelector y = (DefaultNTxNodeSelector) s;
                return y.getClasses().stream();
            }
            return Stream.empty();
        }).collect(Collectors.toSet());
    }

    @Override
    public <T> NOptional<T> computePropertyValue(NTxNode node, String... propertyNames) {
        return propCalculator.computePropertyValue(node, propertyNames);
    }

    @Override
    public NTxGraphics createGraphics(Graphics2D g2d) {
        return new NTxGraphicsImpl(g2d, this);
    }

    public boolean isNtxProject(NPath path) {
        try (NStream<NPath> stream = path.stream()) {
            return stream.anyMatch(x -> x.getName().endsWith(".ntx"));
        }
    }

    @Override
    public void createProject(NPath path, NPath templateUrl, Function<String, String> vars) {
        NAssert.requireNamedNonNull(path, "path");
        if (NBlankable.isBlank(templateUrl)) {
            if (path.isDirectory()) {
                NPath main = path.resolve("main.ntx");
                if (!main.exists()) {
                    log().log(NMsg.ofC("create no-template one-file ntexup project : %s", main.normalize().toAbsolute()));
                    main.mkParentDirs().writeString(resolveEmptyNtxContent());
                } else {
                    NMsg msg = NMsg.ofC("unable to create no-template one-file ntexup project. file already exists %s", main).asSevere();
                    log().log(msg);
                    throw new NIllegalArgumentException(msg);
                }
            } else if (path.isRegularFile()) {
                if (!path.exists()) {
                    log().log(NMsg.ofC("create no-template one-file ntexup project : ", path.normalize().toAbsolute()));
                    path.writeString(resolveEmptyNtxContent());
                } else {
                    NMsg msg = NMsg.ofC("unable to create no-template one-file ntexup project. file already exists %s", path).asSevere();
                    log().log(msg);
                    throw new NIllegalArgumentException(msg);
                }
            } else if (path.exists()) {
                NMsg msg = NMsg.ofC("unable to create no-template one-file ntexup project. file already exists %s", path).asSevere();
                log().log(msg);
                throw new NIllegalArgumentException(msg);
            } else {
                if (path.getName().endsWith(".ntx")) {
                    path.mkParentDirs().writeString(resolveEmptyNtxContent());
                } else {
                    NPath main = path.resolve("main.ntx");
                    if (!main.exists()) {
                        log().log(NMsg.ofC("create no-template one-file ntexup project : ", main.normalize().toAbsolute()));
                        main.mkParentDirs().writeString(resolveEmptyNtxContent());
                    } else {
                        NMsg msg = NMsg.ofC("unable to create no-template one-file ntexup project. file already exists %s", main).asSevere();
                        log().log(msg);
                        throw new NIllegalArgumentException(msg);
                    }
                }
            }
            return;
        }
        NAssert.requireNamedNonNull(templateUrl, "projectUrl");
        NPath localTemplatePath = templateUrl;
        if (NTxGitHelper.isGithubFolder(templateUrl.toString())) {
            try {
                localTemplatePath = NTxGitHelper.resolveGithubPath(templateUrl.toString(), log());
            } catch (Exception ex) {
                NMsg msg = NMsg.ofC("unable to create project from template. invalid location %s", templateUrl).asSevere();
                log().log(msg);
                throw new NIllegalArgumentException(msg);
            }
        }
        if (!localTemplatePath.exists()) {
            NMsg msg = NMsg.ofC("unable to create project from template. invalid location %s", templateUrl).asSevere();
            log().log(msg);
            throw new NIllegalArgumentException(msg);
        }
        log().log(NMsg.ofC("create project %s from template %s", path.normalize().toAbsolute(), templateUrl));
//        NPath finalProjectUrl = templateUrl;
        Function<String, String> vars2 = m -> {
            switch (m) {
                case "template.templateBootUrl":
                    return templateUrl.toString();
                case "template.templateUrl": {
                    try {
                        NPath bp = templateUrl;
                        NPath pp = bp.getParent();
                        if (pp != null && pp.getName().equals("templates")) {
                            pp = pp.getParent();
                            if (pp != null) {
                                pp = pp.resolve("theme");
                                return pp.toString();
                            }
                        }
                    } catch (Exception ex) {
                        log().log(NMsg.ofC("Failed to resolve template boot url from %s", templateUrl, ex).asSevere());
                        throw new IllegalArgumentException("Failed to resolve template boot url from " + templateUrl);
                    }
                }
            }
            if (vars != null) {
                String u = vars.apply(m);
                if (u == null) {
                    switch (m) {
                        case "template.title":
                            return "New Document";
                        case "template.fullName":
                        case "template.author":
                            return System.getProperty("user.name");
                        case "template.date":
                            return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                        case "template.version":
                            return "v1.0.0";
                    }
                }
                return u;
            }
            return null;
        };
        copyTexupProjectTemplate(localTemplatePath, path, vars2, true, false);
    }

    private static String resolveEmptyNtxContent() {
        return "page{\n"
                + "  ¶ Hello World\n"
                + "}";
    }

    @Override
    public NTxTemplateInfo[] getTemplates() {
        List<NTxTemplateInfo> allTemplates = new ArrayList<>();
        class Repo {

            final String name;
            final NPath path;

            public Repo(String name, NPath path) {
                this.name = name;
                this.path = path;
            }
        }
        NTxTemplateInfoLoader loader = new NTxTemplateInfoLoader();
        for (Repo repo : new Repo[]{
                new Repo("dev", NPath.ofUserHome().resolve("xprojects/nuts-world/nuts-productivity/ntexup/ntexup-templates")),
                new Repo("local", NApp.of().getSharedConfFolder().resolve("templates")),
                new Repo("user", NPath.ofUserStore(NStoreType.CONF).resolve("ntexup/templates")),
                new Repo("system", NPath.ofSystemStore(NStoreType.CONF).resolve("ntexup/templates")),
                new Repo("central-github", NPath.of("https://github.com/thevpc/ntexup-templates.git"))
        }) {
            allTemplates.addAll(loader.loadTemplateInfo(repo.name, repo.path, log()));
        }
        return allTemplates.toArray(new NTxTemplateInfo[0]);
    }

    private void copyTexupProjectTemplate(NPath from, NPath to, Function<String, String> vars, boolean dry, boolean acceptOverride) {
        try {
            copyTemplate(from, to, vars, true, false);
        } catch (Exception ex) {
            throw new NIllegalArgumentException(NMsg.ofC("unable to create ntexup project at %s. folder not empty", to));
        }
        copyTemplate(from, to, vars, false, true);
    }

    private void copyTemplate(NPath from, NPath to, Function<String, String> vars, boolean dry, boolean acceptOverride) {
        if (from.isDirectory()) {
            if (!to.exists()) {
                to.mkdirs();
            }
            if (!to.isDirectory()) {
                throw new IllegalArgumentException("cannot copy folder " + from + " to " + to);
            }
            for (NPath nPath : from.list()) {
                copyTemplate(nPath, to.resolve(nPath.getName()), vars, dry, acceptOverride);
            }
        } else if (from.isRegularFile()) {
            if (to.isRegularFile()) {
                if (!acceptOverride) {
                    throw new IllegalArgumentException("cannot copy " + from + " to " + to + ". file alreay exists");
                }
            }
            if (!dry) {
                if (NTxEngineUtils.isNTexupFile(from)) {
                    String code = from.readString();
                    to.writeString(NMsg.ofV(code, vars).toString());
                } else {
                    from.copyTo(to);
                }
            }
        } else {
            throw new IllegalArgumentException("cannot copy " + from + " to " + to);
        }
    }

//    @Override
//    public NOptional<NElement> evalExpression(NElement expression, NTxNode node, NTxVarProvider varProvider) {
//        if (expression == null) {
//            return null;
//        }
//        String baseSrc = NTxUtils.findCompilerDeclarationPath(expression).orNull();
//        NTxNodeEval ne = new NTxNodeEval(this, varProvider);
//        NElement u = ne.eval(NTxElementUtils.toElement(expression), node);
//        if (u == null) {
//            u = NElement.ofNull();
//        }
//        if (baseSrc != null) {
//            u = NTxUtils.addCompilerDeclarationPath(u, baseSrc);
//        }
//        return NOptional.ofNamed(u, "expression " + expression);
//    }

//    @Override
//    public NOptional<NElement> resolveVarValue(String varName, NTxNode node) {
//        return resolveVarValue(varName, node, null);
//    }

//    @Override
//    public NOptional<NElement> resolveVarValue(String varName, NTxNode node, NTxVarProvider varProvider) {
//        NOptional<NTxVar> v = findVar(varName, node, varProvider);
//        if (!v.isPresent()) {
//            log().log(NMsg.ofC("var not found %s", varName).asWarning(), NTxUtils.sourceOf(node));
//            return NOptional.ofNamedEmpty(NMsg.ofC("var %s", varName));
//        }
//        NElement ee = v.get().get();
//        return evalExpression(ee, node, varProvider);
//    }
//
//    public NOptional<NTxVar> findVar(String varName, NTxNode node, NTxVarProvider varProvider) {
//        NTxNodeEval ne = new NTxNodeEval(this, varProvider);
//        return ne.findVar(varName, node);
//    }

//    @Override
//    public NOptional<NTxNode> findNodeByProperty(String varName, String varValue, NTxNode node, NTxVarProvider varProvider) {
//        NTxNodeEval ne = new NTxNodeEval(this, varProvider);
//        return ne.findNodeByProperty(varName, varValue, node);
//    }

//    @Override
//    public NPath resolvePath(NElement path, NTxNode node) {
//        if (NBlankable.isBlank(path)) {
//            return null;
//        }
//        if (path.isAnyString()) {
//            String pathStr = path.asStringValue().get();
//            if (NTxGitHelper.isGithubFolder(pathStr)) {
//                return NTxGitHelper.resolveGithubPath(pathStr, log());
//            }
//            NTxSource source = NTxUtils.sourceOf(node);
//            return NTxUtils.resolvePath(path, source);
//        }
//        throw new NIllegalArgumentException(NMsg.ofC("unsupported path type", path));
//    }

    public NOptional<NTxNodeRenderer> getRenderer(String type) {
        return renderers.get(type);
    }

    @Override
    public BufferedImage renderImage(NTxCompiledPage page, NTxNodeRendererConfig config) {
        BufferedImage newImage = new BufferedImage((int) config.getWidth(), (int) config.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        renderPage(page, config, g, null, null);
        g.dispose();
        return newImage;
    }

    @Override
    public void renderPage(NTxCompiledPage page, NTxNodeRendererConfig config,
                           Graphics2D g,
                           ImageObserver imageObserver, Runnable repainter
    ) {
        initializeComponents();
        NTxNode node = page.compiledPage();
        long startTime = config.getStartTime();
        double sizeWidth = config.getWidth();
        double sizeHeight = config.getHeight();
        Map<String, Object> capabilities = config.getCapabilities();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        NTxGraphics hg = this.createGraphics(g);
        NTxNodeRenderer renderer = getRenderer(node.type()).get();
        NTxBounds2D bounds2D = NTxBounds2D.ofWidth(0, 0, sizeWidth, sizeHeight);
        NTxBounds3D bounds3D = NTxBounds3D.ofFull();

        NTxBounds2D realBounds2D = config.getRealBounds2D();
        NTxBounds3D realBounds3D = config.getRealBounds3D();

        // by default A4
        if (realBounds2D == null) {
            realBounds2D = NTxBounds2D.ofWidth(0, 0, 297E-3, 210E-3);
        }

        // by default 1m cube
        if (realBounds3D == null) {
            realBounds3D = NTxBounds3D.ofUnit();
        }

        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }
        boolean someChange = !config.isUseCache();
        DefaultNTxRendererContext context = new DefaultNTxRendererContext(
                new NTxNode[]{node}, this, hg,
                null,
                bounds2D,
                bounds2D,
                realBounds2D,
                realBounds2D,
                null,
                bounds3D, // 1
                bounds3D,
                realBounds3D,
                realBounds3D,
                someChange,
                startTime,
                capabilities, imageObserver,
                repainter, null, false, null, null, null,
                page.document().compiledDocument(),
                null,
                null,
                null,
                page.document(),
                page,
                page.pageContext(),
                itemParser()
        );
        if (someChange) {
            node.invalidateRenderCache();
        }
        renderer.render(context);
    }

    @Override
    public byte[] renderImageBytes(NTxCompiledPage page, NTxNodeRendererConfig config) {
        initializeComponents();
        BufferedImage newImage = renderImage(page, config);
        String imageTypeOk = "png";
        if (config.getCapabilities() != null) {
            Object imageType = config.getCapabilities().get("imageType");
            if (imageType instanceof String) {
                imageTypeOk = (String) imageType;
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ImageIO.write(newImage, imageTypeOk, bos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bos.toByteArray();
    }

}
