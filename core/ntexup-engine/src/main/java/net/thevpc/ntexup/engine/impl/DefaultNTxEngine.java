package net.thevpc.ntexup.engine.impl;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.thevpc.ntexup.api.document.NTxDocumentFactory;
import net.thevpc.ntexup.api.document.elem2d.NTxBounds2;
import net.thevpc.ntexup.api.document.node.*;
import net.thevpc.ntexup.api.document.style.DefaultNTxNodeSelector;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.engine.*;
import net.thevpc.ntexup.api.eval.NTxVarProvider;
import net.thevpc.ntexup.api.renderer.text.NTxTextRendererFlavor;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.engine.eval.*;
import net.thevpc.ntexup.engine.log.DefaultNTxLogger;
import net.thevpc.ntexup.api.log.NTxLogger;
import net.thevpc.ntexup.api.eval.NTxCompilePageContext;
import net.thevpc.ntexup.api.document.*;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.eval.NTxFunctionArg;
import net.thevpc.ntexup.api.eval.NTxVar;
import net.thevpc.ntexup.api.parser.*;
import net.thevpc.ntexup.api.renderer.*;
import net.thevpc.ntexup.api.util.NTxUtils;
import net.thevpc.ntexup.engine.renderer.DefaultNTxNodeRendererContext;
import net.thevpc.ntexup.engine.util.NTxElementUtils;
import net.thevpc.ntexup.engine.document.DefaultNTxNode;
import net.thevpc.ntexup.engine.ext.NTxNodeBuilderContextImpl;
import net.thevpc.ntexup.engine.log.NTxMessageList;
import net.thevpc.ntexup.engine.log.SilentNTxLogger;
import net.thevpc.ntexup.engine.parser.*;
import net.thevpc.ntexup.engine.parser.resources.NTxSourceFactory;
import net.thevpc.ntexup.engine.renderer.NTxDocumentRendererFactoryContextImpl;
import net.thevpc.ntexup.engine.document.NTxPropCalculator;
import net.thevpc.ntexup.engine.document.NTxDocumentFactoryImpl;
import net.thevpc.ntexup.engine.renderer.NTxGraphicsImpl;
import net.thevpc.nuts.*;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.ext.NExtensions;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.util.*;

import javax.imageio.ImageIO;


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
    private NTxPropCalculator propCalculator = new NTxPropCalculator();
    private NTxFunctionList functions;
    private NTxMessageList log = new NTxMessageList();
    NtxTextFlavorList textFlavors;
    private NMutableClassLoader classLoader;
    private List<NTxDependencyLoadedListener> dependencyLoadedListeners = new ArrayList<>();
    NTxNodeRendererList renderers;
    private NTxImageTypeRendererFactoryList imageTypeRendererFactoryList;

    public DefaultNTxEngine() {
        addLog(new DefaultNTxLogger());
        tools = new MyNTxEngineTools(this);

        classLoader = NExtensions.of().createMutableClassLoader(Thread.currentThread().getContextClassLoader());
        textFlavors = new NtxTextFlavorList(this);
        functions = new NTxFunctionList(this);
        documentRendererFactories = new NTxDocumentRendererFactoryList(this);
        builderContexts = new NTxNodeBuilderList(this);
        nodeTypeFactories = new NTxNodeParserList(this);
        nodeParserFactories = new NTxNodeParserFactoryList(this);
        renderers = new NTxNodeRendererList(this);
        imageTypeRendererFactoryList = new NTxImageTypeRendererFactoryList(this);

        textFlavors.build();
        functions.build();
        documentRendererFactories.build();
        builderContexts.build();
        nodeTypeFactories.build();
        nodeParserFactories.addCustom(new DefaultNTxDocumentItemParserFactory());
        nodeParserFactories.build();
        renderers.build();
        imageTypeRendererFactoryList.build();
    }

    @Override
    public void dump() {
        dump(x -> log().log(x));
    }

    @Override
    public void dump(Consumer<NMsg> out) {
        out.accept(NMsg.ofC("NTexup Engine"));
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
    public void addNTxDependencyLoadedListener(NTxDependencyLoadedListener listener) {
        this.dependencyLoadedListeners.add(listener);
    }

    public <S> List<S> loadServices(Class<S> serviceClass) {
        return NCollections.list(ServiceLoader.load(serviceClass, classLoader.asClassLoader()));
    }

    public boolean importDependencies(String... deps) {
        boolean u = classLoader.loadDependencies(Arrays.stream(deps).map(x -> NDependency.of(x)).toArray(NDependency[]::new));
        if (u) {
            for (NTxDependencyLoadedListener d : dependencyLoadedListeners) {
                d.onLoadDependencyLoaded();
            }
        }
        return u;
    }

    @Override
    public boolean importDefaultDependencies() {
        return importDependencies(
                "net.thevpc.ntexup:ntexup-extension-plantuml:0.8.6.0",
                "net.thevpc.ntexup:ntexup-extension-animated-gif:0.8.6.0",
                "net.thevpc.ntexup:ntexup-extension-svg:0.8.6.0",
                "net.thevpc.ntexup:ntexup-extension-shapes2d:0.8.6.0",
                "net.thevpc.ntexup:ntexup-extension-shapes3d:0.8.6.0",
                "net.thevpc.ntexup:ntexup-extension-plot2d:0.8.6.0",
                "net.thevpc.ntexup:ntexup-extension-presenters:0.8.6.0",
                "net.thevpc.ntexup:ntexup-extension-latex:0.8.6.0"
        );
    }

    @Override
    public NTxEngineTools tools() {
        return tools;
    }

    public NOptional<NTxTextRendererFlavor> textRendererFlavor(String id) {
        return textFlavors.get(id);
    }

    public List<NTxTextRendererFlavor> textRendererFlavors() {
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
    public NOptional<NTxFunction> findFunction(NTxItem node, String name, NTxFunctionArg... args) {
        NTxItem p = node;
        while (p != null) {
            if (p instanceof NTxNode) {
                for (NTxFunction f : ((NTxNode) p).nodeFunctions()) {
                    if (NNameFormat.equalsIgnoreFormat(f.name(), name)) {
                        return NOptional.of(f);
                    }
                }
            }
            p = p.parent();
        }
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
    public NOptional<NTxItem> newNode(NElement element, NTxNodeFactoryParseContext ctx) {
        NAssert.requireNonNull(ctx, "context");
        NAssert.requireNonNull(element, "element");
        if (ctx.source() == null) {
            throw new IllegalArgumentException("unexpected source null");
        }
        NTxNodeFactoryParseContext newContext = new DefaultNTxNodeFactoryParseContext(
                ctx.document()
                , element
                , this
                ,
                Arrays.asList(ctx.nodePath())
                , ctx.source()
        );
        NOptional<NTxItem> optional = NCallableSupport.resolve(
                        nodeParserFactories.list().stream()
                                .map(x -> x.parseNode(newContext)),
                        () -> NMsg.ofC("missing support for node from type '%s' value '%s'", element.type().id(), NTxUtils.snippet(element)))
                .toOptional();
        if (optional.isPresent()) {
            NTxItem nTxItem = optional.get();
            if (nTxItem instanceof NTxNode) {
                NTxSource s = nTxItem.source();
                if (s == null) {
                    throw new IllegalArgumentException("unexpected source null");
                }
            }
        }
        return optional;
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
        NTxDocumentRendererFactoryContext ctx = new NTxDocumentRendererFactoryContextImpl(this, type);
        return NCallableSupport.resolve(
                        documentRendererFactories().stream()
                                .map(x -> x.<NTxDocumentStreamRenderer>createDocumentRenderer(ctx)),
                        () -> NMsg.ofC("missing StreamRenderer %s", type))
                .toOptional();
    }

    private List<NTxDocumentRendererFactory> documentRendererFactories() {
        return documentRendererFactories.list();
    }

    @Override
    public List<NTxNodeParser> nodeTypeFactories() {
        return new ArrayList<>(nodeTypeFactories0().values());
    }

    public List<NTxNodeBuilderContextImpl> builderContexts() {
        return builderContexts.builderContexts();
    }

    private Map<String, NTxNodeParser> nodeTypeFactories0() {
        return nodeTypeFactories.map();
    }


    public NTxNode newDefaultNode(String id) {
        return new DefaultNTxNode(id);
    }

    public NOptional<NTxNodeParser> nodeTypeParser(String id) {
        id = NStringUtils.trim(id);
        if (!id.isEmpty()) {
            NOptional<NTxNodeParser> a = nodeTypeFactories.get(id);
            if (a.isPresent()) {
                return a;
            }
            id = NTxUtils.uid(id);
            switch (id) {
                case NTxNodeType.CTRL_CALL:
                    return NOptional.of(
                            new NTxNodeParserBase(true, NTxNodeType.CTRL_CALL) {

                            }
                    );
                case NTxNodeType.CTRL_ASSIGN:
                    return NOptional.of(
                            new NTxNodeParserBase(true, NTxNodeType.CTRL_ASSIGN) {

                            }
                    );
                case NTxNodeType.CTRL_EXPR:
                    return NOptional.of(
                            new NTxNodeParserBase(false, NTxNodeType.CTRL_EXPR) {

                            }
                    );
                case NTxNodeType.CTRL_NAME:
                    return NOptional.of(
                            new NTxNodeParserBase(false, NTxNodeType.CTRL_NAME) {

                            }
                    );
            }
        }
        return NOptional.ofNamedEmpty("node type parser for NodeType " + id);
    }


    public NTxDocumentLoadingResult compileDocument(NTxDocument document) {
        return new NTxCompiler(this).compile(document);
    }

    @Override
    public List<NTxNode> compilePageNode(NTxNode node, NTxDocument document) {
        return compilePageNode(node, new NTxCompilePageContextImpl(this, document));
    }

    @Override
    public List<NTxNode> compilePageNode(NTxNode node, NTxCompilePageContext context) {
        return new NTxCompiler(this).compilePageNode(node, context);
    }

    @Override
    public List<NTxNode> compileItem(NTxItem node, NTxCompilePageContext context) {
        List<NTxNode> all = new ArrayList<>();
        if (node instanceof NTxNode) {
            all.addAll(compilePageNode((NTxNode) node, context));
        } else if (node instanceof NTxItemList) {
            for (NTxItem i : ((NTxItemList) node).getItems()) {
                all.addAll(compileItem(i, context));
            }
        } else if (
                node instanceof NTxStyleRule
                        || node instanceof NTxNodeDef
                        || node instanceof NTxProp
        ) {
            //just ignore
        }
        return all;
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
        NAssert.requireNonNull(path, "path");
        synchronized (this) {
            if (NTxGitHelper.isGithubFolder(path.toString())) {
                path = NTxGitHelper.resolveGithubPath(path.toString(), log());
            }
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
                            "main." + FILE_EXT,
                    }) {
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
        NTxDocument docd = documentFactory().ofDocument(source);
        docd.sourceMonitor().add(source);
        docd.root().setSource(source);
        NTxNodeFactoryParseContext newContext = new DefaultNTxNodeFactoryParseContext(
                docd,
                doc, this,
                new ArrayList<>(),
                source
        );

        NOptional<NTxItem> r = newNode(doc, newContext);
        if (r.isPresent()) {
            docd.root().append(r.get());
            return NOptional.of(docd);
        }
        log().log(NMsg.ofC("invalid %s", r.getMessage().get()).asSevere());
        return NOptional.of(docd);
    }


    private NOptional<NTxItem> loadNode0(NTxNode into, NPath path, NTxDocument document) {
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
        return newNode(c, new DefaultNTxNodeFactoryParseContext(
                document,
                null,
                this,
                parents,
                source
        ));
    }

    @Override
    public NElement toElement(NTxDocument doc) {
        NTxNode r = doc.root();
        return nodeTypeParser(r.type()).get().toElem(r);
    }


    @Override
    public NElement toElement(NTxNode node) {
        return nodeTypeParser(node.type()).get().toElem(node);
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
            if (x instanceof DefaultNTxNodeSelector) {
                DefaultNTxNodeSelector y = (DefaultNTxNodeSelector) x;
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

    @Override
    public void createProject(NPath path, NPath projectUrl, Function<String, String> vars) {
        NAssert.requireNonNull(path, "path");
        NAssert.requireNonNull(projectUrl, "projectUrl");
        if (NTxGitHelper.isGithubFolder(projectUrl.toString())) {
            projectUrl = NTxGitHelper.resolveGithubPath(path.toString(), null);
        }
        if (!projectUrl.exists()) {
            throw new IllegalArgumentException("invalid project " + projectUrl);
        }
        NPath finalProjectUrl = projectUrl;
        Function<String, String> vars2 = m -> {
            switch (m) {
                case "template.templateBootUrl":
                    return finalProjectUrl.toString();
                case "template.templateUrl": {
                    try {
                        NPath bp = finalProjectUrl;
                        NPath pp = bp.getParent();
                        if (pp != null && pp.getName().equals("boot")) {
                            pp = pp.getParent();
                            if (pp != null) {
                                pp = pp.resolve("dist");
                                return pp.toString();
                            }
                        }
                    } catch (Exception ex) {
                        log().log(NMsg.ofC("Failed to resolve template boot url from %s", finalProjectUrl, ex).asSevere());
                        throw new IllegalArgumentException("Failed to resolve template boot url from " + finalProjectUrl);
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
        copyTemplate(projectUrl, path, vars2);
    }

    @Override
    public NTxTemplateInfo[] getTemplates() {
        List<NTxTemplateInfo> allTemplates = new ArrayList<>();
        class Repo {
            String name;
            NPath path;

            public Repo(String name, NPath path) {
                this.name = name;
                this.path = path;
            }
        }
        NTxTemplateInfoLoader loader = new NTxTemplateInfoLoader();
        for (Repo repo : new Repo[]{
                new Repo("dev", NPath.ofUserHome().resolve("xprojects/nuts-world/nuts-productivity/ntexup-templates/main")),
                new Repo("local", NApp.of().getSharedConfFolder().resolve("templates")),
                new Repo("user", NPath.ofUserStore(NStoreType.CONF).resolve("ntexup/templates")),
                new Repo("user", NPath.ofSystemStore(NStoreType.CONF).resolve("ntexup/templates")),
                new Repo("central-github", NPath.of("github://thevpc/ntexup-templates/main"))
        }) {
            loader.loadTemplateInfo(repo.name, repo.path.resolve("ntexup-repository.tson"), log());
        }
        return allTemplates.toArray(new NTxTemplateInfo[0]);
    }


    private void copyTemplate(NPath from, NPath to, Function<String, String> vars) {
        if (from.isDirectory()) {
            if (!to.exists()) {
                to.mkdirs();
            }
            if (!to.isDirectory()) {
                throw new IllegalArgumentException("cannot copy folder " + from + " to " + to);
            }
            for (NPath nPath : from.list()) {
                copyTemplate(nPath, to.resolve(nPath.getName()), vars);
            }
        } else if (from.isRegularFile()) {
            if (NTxEngineUtils.isNTexupFile(from)) {
                String code = from.readString();
                to.writeString(NMsg.ofV(code, vars).toString());
            } else {
                from.copyTo(to);
            }
        } else {
            throw new IllegalArgumentException("cannot copy " + from + " to " + to);
        }
    }

    @Override
    public NElement evalExpression(NElement expression, NTxNode node, NTxVarProvider varProvider) {
        if (expression == null) {
            return null;
        }
        String baseSrc = NTxUtils.findCompilerDeclarationPath(expression).orNull();
        NTxNodeEval ne = new NTxNodeEval(this, varProvider);
        NElement u = ne.eval(NTxElementUtils.toElement(expression), node);
        if (u == null) {
            u = NElement.ofNull();
        }
        if (baseSrc != null) {
            u = NTxUtils.addCompilerDeclarationPath(u, baseSrc);
        }
        return u;
    }

    @Override
    public NElement resolveVarValue(String varName, NTxNode node, NTxVarProvider varProvider) {
        NOptional<NTxVar> v = findVar(varName, node, varProvider);
        if (!v.isPresent()) {
            log().log(NMsg.ofC("var not found %s", varName).asWarning(), NTxUtils.sourceOf(node));
            return null;
        }
        NElement ee = v.get().get();
        return evalExpression(ee, node, varProvider);
    }

    public NOptional<NTxVar> findVar(String varName, NTxNode node, NTxVarProvider varProvider) {
        NTxNodeEval ne = new NTxNodeEval(this, varProvider);
        return ne.findVar(varName, node);
    }

    @Override
    public NPath resolvePath(NElement path, NTxNode node) {
        if (NBlankable.isBlank(path)) {
            return null;
        }
        if (path.isAnyString()) {
            String pathStr = path.asStringValue().get();
            if (NTxGitHelper.isGithubFolder(pathStr)) {
                return NTxGitHelper.resolveGithubPath(pathStr, log());
            }
            NTxSource source = NTxUtils.sourceOf(node);
            return NTxUtils.resolvePath(path, source);
        }
        throw new NIllegalArgumentException(NMsg.ofC("unsupported path type", path));
    }


    public NOptional<NTxNodeRenderer> getRenderer(String type) {
        return renderers.get(type);
    }


    @Override
    public BufferedImage renderImage(NTxCompiledPage page, NTxNodeRendererConfig config) {
        NTxNode node = page.compiledPage();
        int sizeWidth = config.getWidth();
        int sizeHeight = config.getHeight();
        Dimension dimension = new Dimension(sizeWidth, sizeHeight);
        Map<String, Object> capabilities = config.getCapabilities();
        BufferedImage newImage = new BufferedImage(sizeWidth, sizeHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        NTxGraphics hg = this.createGraphics(g);
        NTxNodeRenderer renderer = getRenderer(node.type()).get();
        DefaultNTxNodeRendererContext context = new DefaultNTxNodeRendererContext(node, this, hg, null,
                new NTxBounds2(0, 0, dimension.getWidth(), dimension.getHeight()),
                new NTxBounds2(0, 0, dimension.getWidth(), dimension.getHeight()),
                page, true, System.currentTimeMillis(), capabilities, null, null, null, false);
        renderer.render(context);
        g.dispose();
        return newImage;
    }

    @Override
    public byte[] renderImageBytes(NTxCompiledPage page, NTxNodeRendererConfig config) {
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
