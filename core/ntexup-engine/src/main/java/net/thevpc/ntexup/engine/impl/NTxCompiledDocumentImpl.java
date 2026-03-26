package net.thevpc.ntexup.engine.impl;

import net.thevpc.ntexup.api.document.NTxDocument;
import net.thevpc.ntexup.api.document.node.NTxNode;
import net.thevpc.ntexup.api.document.node.NTxNodeDef;
import net.thevpc.ntexup.api.document.node.NTxNodeType;
import net.thevpc.ntexup.api.document.security.NTxManifest;
import net.thevpc.ntexup.api.document.security.NTxManifestOptions;
import net.thevpc.ntexup.api.document.security.NTxManifestResourceType;
import net.thevpc.ntexup.api.document.style.NTxProp;
import net.thevpc.ntexup.api.document.style.NTxStyleRule;
import net.thevpc.ntexup.api.engine.*;
import net.thevpc.ntexup.api.eval.NTxObj;
import net.thevpc.ntexup.api.eval.NTxResolutionContext;
import net.thevpc.ntexup.api.eval.NTxVar;
import net.thevpc.ntexup.api.extension.NTxFunction;
import net.thevpc.ntexup.api.source.NTxSource;
import net.thevpc.ntexup.api.source.NTxSourceMonitor;
import net.thevpc.ntexup.engine.document.DefaultNTxDocument;
import net.thevpc.ntexup.engine.document.NTxSourceMonitored;
import net.thevpc.ntexup.engine.eval.NTxCompiler;
import net.thevpc.ntexup.engine.security.NTxManifestElementMetaDataBuilder;
import net.thevpc.nuts.artifact.NDefinition;
import net.thevpc.nuts.elem.*;
import net.thevpc.nuts.io.NClosable;
import net.thevpc.nuts.io.NDigest;
import net.thevpc.nuts.io.NPath;
import net.thevpc.nuts.text.NMsg;
import net.thevpc.nuts.util.*;

import java.time.Instant;
import java.util.*;

import net.thevpc.ntexup.api.document.security.NTxManifestResource;

public class NTxCompiledDocumentImpl implements NTxCompiledDocument {
    public static final int DEFAULT_MAX_PAGE_COUNT = 1024 * 64;
    public static final int DEFAULT_WARN_PAGE_COUNT = 1024;
    private final NTxDocument rawDocument;
    private NTxDocument document;
    private final NTxEngine engine;
    private final List<NTxCompiledPage> compiledPages = new ArrayList<>();
    private Throwable currentThrowable;
    private final Deque<NTxNodeAndContext> unparsed = new ArrayDeque<>();
    private final List<NTxNodeAndContext> trailingInstrs = new ArrayList<>();
    private int warnPageCount = DEFAULT_WARN_PAGE_COUNT;
    private int maxPageCount = DEFAULT_MAX_PAGE_COUNT;
    private boolean maxExceeded;
    private boolean allPagesLoaded;
    private final NTxSourceMonitor resources = new NTxSourceMonitored();
    private boolean successfullyLoaded = true;
    private final FingerprintBuilder fingerPrintBuilder = new FingerprintBuilder();
    private final Map<String, NTxObj> globalObjects = new HashMap<>();

    public class FingerprintBuilder {
        private final Map<String, NDefinition> effectiveDependencies = new HashMap<>();
        private final Map<String, NTxManifestResource> effectiveResources = new LinkedHashMap<>();
        private final LinkedHashMap<String, DefaultNTxDocument.NamedPart> contentFiles = new LinkedHashMap<>();
        private final List<byte[]> sourceFingerprintSources = new ArrayList<>();

        public FingerprintBuilder addLoadedDependency(NDefinition def) {
            effectiveDependencies.put(def.getId().getShortName(), def);
            return this;
        }

        public Map<String, NDefinition> getEffectiveDependencies() {
            return effectiveDependencies;
        }

        public Map<String, NTxManifestResource> getEffectiveResource() {
            return effectiveResources;
        }

        public FingerprintBuilder addContent(NPath path) {
            String normalized = path.normalize().toString();
            if (!contentFiles.containsKey(normalized)) {
                NPath s = document.source().path().orNull();
                if (s != null && path.isEqOrDeepChildOf(s)) {
                    NOptional<String> r = path.toRelative(s);
                    if (!r.isEmpty()) {
                        if (!contentFiles.containsKey(normalized)) {
                            contentFiles.put(normalized, new DefaultNTxDocument.NamedPart(
                                    r.get(),
                                    path.readBytes()
                            ));
                            return this;
                        }
                    }
                }
                contentFiles.put(normalized, new DefaultNTxDocument.NamedPart(
                        path.toString(),
                        path.readBytes()
                ));
            }
            return this;
        }

        public void addSource(byte[] bytes) {
            sourceFingerprintSources.add(bytes);
        }

        public List<byte[]> getSourceFingerprintSources() {
            return sourceFingerprintSources;
        }

        public LinkedHashMap<String, DefaultNTxDocument.NamedPart> getContentFiles() {
            return contentFiles;
        }

        public void addResource(NPath pp, String pathStr) {
            // If we haven't tracked this logical path yet
            if (!effectiveResources.containsKey(pathStr)) {
                NTxManifestResourceType t = NTxManifestResourceType.LOCAL;
                String sval = (pathStr != null) ? pathStr : pp.toString();
                String pps = pp.toString();

                // 1. Protocol Identification
                if (pps.startsWith("http:") || pps.startsWith("https:")) {
                    // Strict check for non-portable hostnames
                    if (pps.contains("://localhost") ||
                            pps.contains("://127.") ||
                            pps.contains("://192.168.")) {
                        t = NTxManifestResourceType.DETACHED;
                    } else {
                        t = NTxManifestResourceType.EXTERNAL;
                    }
                }
                // 2. File System Boundary Identification
                else {
                    NPath sp = document.source().path().orNull();
                    // A resource is LOCAL only if it exists within the project folder
                    if (sp != null && pp.isFile() && pp.isEqOrDeepChildOf(sp)) {
                        t = NTxManifestResourceType.LOCAL;
                        // Store as relative path to ensure the ZIP remains portable
                        sval = pp.toRelative(sp).toString();
                    } else {
                        // It's an absolute path outside the project: cannot be easily bundled
                        t = NTxManifestResourceType.DETACHED;
                    }
                }

                // 3. Register with Hash
                effectiveResources.put(pathStr, new NTxManifestResource()
                        .setFingerprint(NDigest.of().addSource(pp).computeString())
                        .setType(t)
                        .setLastVisited(Instant.now())
                        .setValue(sval)
                );
            }
        }
    }

    public NTxCompiledDocumentImpl(NTxDocument rawDocument, NTxEngine engine) {
        this.rawDocument = rawDocument;
        this.engine = engine;
        int _warnPageCount = this.engine.getEnv("warnPageCount").flatMap(x -> NLiteral.of(x).asInt()).orElse(DEFAULT_WARN_PAGE_COUNT);
        int _maxPageCount = this.engine.getEnv("maxPageCount").flatMap(x -> NLiteral.of(x).asInt()).orElse(DEFAULT_MAX_PAGE_COUNT);
        if (_maxPageCount < _warnPageCount) {
            _maxPageCount = _warnPageCount;
        }
        if (_warnPageCount <= 0) {
            _warnPageCount = DEFAULT_WARN_PAGE_COUNT;
        }
        if (_maxPageCount <= 0) {
            _maxPageCount = DEFAULT_MAX_PAGE_COUNT;
        }
        if (_maxPageCount < _warnPageCount) {
            _maxPageCount = _warnPageCount;
        }
        this.maxPageCount = _maxPageCount;
        this.warnPageCount = _warnPageCount;
    }

    @Override
    public NOptional<NTxObj> getGlobalObject(String name) {
        return NOptional.ofNamed(globalObjects.get(name), name);
    }

    @Override
    public NTxCompiledDocument setGlobalObject(String name, NTxObj obj) {
        if (obj == null) {
            globalObjects.remove(name);
        } else {
            globalObjects.put(name, obj);
        }
        return this;
    }

    public boolean isSuccessfullyLoaded() {
        return successfullyLoaded;
    }

    public NTxCompiledDocumentImpl setSuccessfullyLoaded(boolean successfullyLoaded) {
        this.successfullyLoaded = successfullyLoaded;
        return this;
    }

    public void addDependencyFingerprintPart(NDefinition dependency) {
        fingerPrintBuilder.addLoadedDependency(dependency);
    }

    public void addSourceFingerprintPart(String name, byte[] bytes) {
        fingerPrintBuilder.addSource(bytes);
    }

    public FingerprintBuilder getFingerPrintBuilder() {
        return fingerPrintBuilder;
    }

    public void addMonitoredSource(NPath path) {
        sourceMonitor().add(path);
        fingerPrintBuilder.addContent(path);
    }

    @Override
    public NTxSourceMonitor sourceMonitor() {
        return resources;
    }

    @Override
    public NTxSource source() {
        return rawDocument.source();
    }

    @Override
    public NTxDocument document() {
        if (document == null) {
            try {
                document = new NTxCompiler(engine()).compileDocument(this).get();
                unparsed.push(new NTxNodeAndContext(document.root(), null));
            } catch (Exception ex) {
                engine.log().log(NMsg.ofC("compile document failed %s", ex));
                this.currentThrowable = ex;
            }
        }
        if (document == null) {
            document = engine.documentFactory().ofDocument(null);
        }
        return document;
    }

    @Override
    public boolean isCompiled() {
        return document != null;
    }

    @Override
    public NTxDocument rawDocument() {
        return rawDocument;
    }

    @Override
    public String title() {
        if (rawDocument == null) {
            return "New Document";
        }
        NTxSource source = rawDocument.root().source();

        if (source == null) {
            return ("New Document");
        } else {
            return (String.valueOf(source));
        }
    }

    @Override
    public NTxEngine engine() {
        return engine;
    }

    @Override
    public List<NTxCompiledPage> pages() {
        return NCollections.list(pagesIterator());
    }

//    public NOptional<NTxManifestVerificationResult> verifyManifestElement(NElement element) {
//        return new NTxManifestElementMetaDataBuilder(this, engine).verifyManifest(element);
//    }

    public void saveManifest(NTxManifestOptions options) {

    }

    public NTxManifest computeManifest(NTxManifestOptions options) {
        return new NTxManifestElementMetaDataBuilder(this, engine).computeManifestElement(options);
    }

    @Override
    public NElement toElement(boolean semantic) {
        NObjectElementBuilder ob = NElement.ofObjectBuilder();
        ob.add("pages",
                NElement.ofArray(
                        pages().stream().map(x -> x.toElement(semantic)).toArray(NElement[]::new)
                )
        );
        return ob.build();
    }

    public NOptional<NTxCompiledPage> page(int index) {
        if (index < 0) {
            return NOptional.ofNamedEmpty("page " + index);
        }
        // check cache first before iterating
        if (index < compiledPages.size()) {
            return NOptional.of(compiledPages.get(index));
        }
        return NClosable.callWith(pagesIterator(), it -> {
            int currIndex = 0;
            while (it.hasNext()) {
                NTxCompiledPage o = it.next();
                if (index == currIndex) {
                    return NOptional.of(o);
                }
                currIndex++;
            }
            return NOptional.ofNamedEmpty("page " + index);
        });
    }

    @Override
    public Iterator<NTxCompiledPage> pagesIterator() {
        return new Iterator<NTxCompiledPage>() {
            int index = 0;

            @Override
            public boolean hasNext() {
                while (true) {
                    if (index < compiledPages.size()) {
                        return true;
                    } else {
                        if (!readMore()) {
                            onAfterLoadingAllPages();
                            return false;
                        }
                    }
                }
            }

            @Override
            public NTxCompiledPage next() {
                NTxCompiledPage p = compiledPages.get(index);
                index++;
                return p;
            }
        };
    }

    private class PendingAutoPage {
        NTxNode newPage;
        public NTxResolutionContext context;

        public PendingAutoPage(NTxResolutionContext context) {
            this.context = context;
            newPage = engine.documentFactory().ofPage();
            newPage.setParent(context.node());
        }

        public void addChild(NTxNodeAndContext part) {
            if (newPage.source() == null) {
                newPage.setSource(part.node.source());
            }
            newPage.add(part.node);
        }
    }

    private boolean readMore() {
        if (maxExceeded) {
            return false;
        }
        List<NTxNodeAndContext> pendingInstr = new ArrayList<>();
        PendingAutoPage pendingAutoPage = null;
        MyNTxPageCompileListener onCompile = new MyNTxPageCompileListener();
        while (!unparsed.isEmpty()) {
            NTxNodeAndContext part = unparsed.pop();
            if (part.node.isDisabled()) {
                continue;
            }
            switch (part.node.type()) {
                case NTxNodeType.PAGE: {
                    if (pendingAutoPage != null) {
                        safeAddPage(new NTxCompiledPageImpl(pendingAutoPage.newPage, this, compiledPages.size(), pendingAutoPage.context, pendingInstr, onCompile));
                        pendingInstr.clear();
                        pendingAutoPage = null;
                    }
                    safeAddPage(new NTxCompiledPageImpl(part.node, this, compiledPages.size(), part.context, pendingInstr, onCompile));
                    pendingInstr.clear();
                    return true;
                }
                case NTxNodeType.CTRL_ASSIGN:
                case NTxNodeType.CTRL_ASSIGN_DEFAULT:
                case NTxNodeType.CTRL_DEFINE: {
                    if (pendingAutoPage != null) {
                        safeAddPage(new NTxCompiledPageImpl(pendingAutoPage.newPage, this, compiledPages.size(), pendingAutoPage.context, pendingInstr, onCompile));
                        pendingInstr.clear();
                        pendingAutoPage = null;
                    }
                    part.run(document(), engine, this, null, false);
                    pendingInstr.add(part);
                    break;
                }
                case NTxNodeType.PAGE_GROUP: {
                    if (pendingAutoPage != null) {
                        safeAddPage(new NTxCompiledPageImpl(pendingAutoPage.newPage, this, compiledPages.size(), pendingAutoPage.context, pendingInstr, onCompile));
                        pendingInstr.clear();
                        pendingAutoPage = null;
                    }
                    NTxResolutionContext c = engine.newContext(part.node, document, this, null, part.context);
                    List<NTxNode> children = part.node.children();
                    for (int i = children.size() - 1; i >= 0; i--) {
                        unparsed.push(new NTxNodeAndContext(children.get(i), c));
                    }
                    break;
                }
                case NTxNodeType.BLOCK: {
                    NTxResolutionContext c = engine.newContext(part.node, document, this, null, part.context);
                    List<NTxNode> children = part.node.children();
                    for (int i = children.size() - 1; i >= 0; i--) {
                        unparsed.push(new NTxNodeAndContext(children.get(i), c));
                    }
                    break;
                }
                case NTxNodeType.FRAGMENT: {
                    NTxResolutionContext c = part.context;
                    List<NTxNode> children = part.node.children();
                    for (int i = children.size() - 1; i >= 0; i--) {
                        unparsed.push(new NTxNodeAndContext(children.get(i), c));
                    }
                    break;
                }
                case NTxNodeType.CTRL_CALL: {
                    if (part.context.inPage()) {
                        if (pendingAutoPage == null) {
                            pendingAutoPage = new PendingAutoPage(part.context);
                            pendingAutoPage.addChild(part);
                        } else if (pendingAutoPage.context != part.context) {
                            safeAddPage(new NTxCompiledPageImpl(pendingAutoPage.newPage, this, compiledPages.size(), pendingAutoPage.context, pendingInstr, onCompile));
                            pendingInstr.clear();

                            pendingAutoPage = new PendingAutoPage(part.context);
                            pendingAutoPage.addChild(part);
                            return true;
                        } else {
                            pendingAutoPage.addChild(part);
                        }
                    } else {
                        NTxResolutionContext c = part.context.copy();
                        List<NTxNode> pushMe = new ArrayList<>();
                        c.doWithChild(part.node, null, cc -> {
                            engine.compileNode(cc, new CompileNodeVisitor() {
                                @Override
                                public void visitNode(NTxNode node, NTxResolutionContext context) {
                                    pushMe.add(node);
                                }

                                @Override
                                public void visitRule(NTxStyleRule a, NTxResolutionContext context) {
                                    part.node.addRule(a);
                                }

                                @Override
                                public void visitDefinition(NTxNodeDef a, NTxResolutionContext context) {
                                    pushMe.add(a);
                                }

                                @Override
                                public void visitFunction(NTxFunction a, NTxResolutionContext context) {

                                }

                                @Override
                                public void visitProperty(NTxProp a, NTxResolutionContext context) {
                                    part.node.setProperty(a);
                                }

                                @Override
                                public void visitVar(String varName, NTxVar nTxVar, NTxResolutionContext context) {

                                }
                            });
                        });
                        for (int i = pushMe.size() - 1; i >= 0; i--) {
                            NTxNodeAndContext pp = new NTxNodeAndContext(pushMe.get(i), c);
                            if (pp.node == part.node) {
                                engine.log().log(NMsg.ofC("unable to compile %s", part.node));
                                if (pendingAutoPage == null) {
                                    pendingAutoPage = new PendingAutoPage(pp.context);
                                    pendingAutoPage.addChild(pp);
                                } else if (pendingAutoPage.context != pp.context) {
                                    safeAddPage(new NTxCompiledPageImpl(pendingAutoPage.newPage, this, compiledPages.size(), pendingAutoPage.context, pendingInstr, onCompile));
                                    pendingInstr.clear();

                                    pendingAutoPage = new PendingAutoPage(pp.context);
                                    pendingAutoPage.addChild(pp);
                                    return true;
                                } else {
                                    pendingAutoPage.addChild(pp);
                                }
                            } else {
                                unparsed.push(pp);
                            }
                        }
                    }
                    break;
                }
                case NTxNodeType.GROUP:
                default: {
                    if (pendingAutoPage == null) {
                        pendingAutoPage = new PendingAutoPage(part.context);
                        pendingAutoPage.addChild(part);
                    } else if (pendingAutoPage.context != part.context) {
                        safeAddPage(new NTxCompiledPageImpl(pendingAutoPage.newPage, this, compiledPages.size(), pendingAutoPage.context, pendingInstr, onCompile));
                        pendingInstr.clear();

                        pendingAutoPage = new PendingAutoPage(part.context);
                        pendingAutoPage.addChild(part);
                        return true;
                    } else {
                        pendingAutoPage.addChild(part);
                    }
                }
            }
        }
        if (pendingAutoPage != null) {
            safeAddPage(new NTxCompiledPageImpl(pendingAutoPage.newPage, this, compiledPages.size(), pendingAutoPage.context, pendingInstr, onCompile));
            pendingInstr.clear();
            pendingAutoPage = null;
            return true;
        }
        trailingInstrs.addAll(pendingInstr);
        return false;
    }

    private void onAfterLoadingAllPages() {
        if (!allPagesLoaded) {
            allPagesLoaded = true;
//            System.out.println(computeManifest().toPrettyString());
        }
    }

    private boolean safeAddPage(NTxCompiledPageImpl a) {
        compiledPages.add(a);
// soft limit — warn but continue
        if (compiledPages.size() > warnPageCount) {
            this.engine.log().log(NMsg.ofC("page count %d exceeds warning threshold", compiledPages.size()).asWarning());
        }
// hard limit — stop generating
        if (compiledPages.size() > maxPageCount) {
            this.engine.log().log(NMsg.ofC("page count %d exceeds maximum, stopping", compiledPages.size()).asError());
            maxExceeded = true;
            return false; // in readMore()
        }
        return true;
    }

    public void onBeforeCompileImpl(NTxCompiledPage a) {
        for (int i = 0; i < compiledPages.size(); i++) {
            if (i < a.index()) {
                NTxCompiledPageImpl nTxCompiledPage = (NTxCompiledPageImpl) compiledPages.get(i);
                nTxCompiledPage.initialize();
            } else {
                break;
            }
        }
    }

    public void onAfterCompileImpl(NTxCompiledPage a) {
        for (int i = 0; i < compiledPages.size(); i++) {
            if (!compiledPages.get(i).isCompiled()) {
                return;
            }
        }
        if (!unparsed.isEmpty()) {
            return;
        }
        for (NTxNodeAndContext trailingInstr : trailingInstrs) {
            trailingInstr.run(document, engine, this, a, false);
        }
    }

    @Override
    public Throwable currentThrowable() {
        return currentThrowable;
    }

    private class MyNTxPageCompileListener implements NTxPageCompileListener {
        @Override
        public void onBeforeCompile(NTxCompiledPage a) {
            onBeforeCompileImpl(a);
        }

        @Override
        public void onAfterCompile(NTxCompiledPage a) {
            onAfterCompileImpl(a);
        }
    }
}
