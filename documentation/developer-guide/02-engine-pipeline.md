# The ntexup Engine Pipeline: Parse, Compile, Render

Covers the end-to-end lifecycle of a document and the two Advanced topics people usually ask about:
**how lazy / incremental compilation works** and **how included chunks are compiled and when**.

All class names are `net.thevpc.ntexup.*`. Paths are relative to the repository root.

---

## 1. Stage zero — engine bootstrap

`DefaultNTxEngine` (`core/ntexup-engine/src/main/java/net/thevpc/ntexup/engine/impl/DefaultNTxEngine.java`) is the only `NTxEngine` implementation. Constructing it triggers `initializeComponents()` which:

1. wires the logger, `NTxPropCalculator`, and `MyNTxEngineTools`;
2. creates the service registries: `NTxNodeParserList`, `NTxNodeTypeFactoryList`, `NTxNodeRendererList`, `NTxFunctionList`, `NtxTextFlavorList`, `NTxNodeBuilderList`, `NTxImageTypeRendererFactoryList`, `documentRendererFactories()`;
3. runs `build(new NDefinition[0], false)` on each → loads the **BASE** services from the engine classpath via `NServiceLoader`;
4. adds `DefaultNTxDocumentItemParserFactory` (the generic element parser);
5. builds the engine's own `NMutableClassLoader` (`getEngineClassLoader()`) — this classloader is what `import()` later feeds.

> Discovery is **not** `java.util.ServiceLoader`. Services are loaded through Nuts `NServiceLoader.of(serviceClass, null, classLoader.asClassLoader()).loadAll(...)` (`DefaultNTxEngine.java:515`). This is what allows runtime-dynamic extension loading.

---

## 2. Stage one — parse: `.ntx` (TSON) → raw `NElement`

TSON is read with the Nuts TSON reader, not with a custom tokenizer:

1. `DefaultNTxEngine.loadDocument(NPath)` (`DefaultNTxEngine.java:843`):
   - GitHub paths (`github://...`) are first resolved by cloning into the Nuts cache (`NTxGitHelper`). Clone/pull is delegated to a `NTxGitProvider` selected by `NTxGitProviderFactory` — **JGit** by default (embedded, no system `git` needed), or the native `git` executable when requested via `engine.setEnv("git.provider", "system")` / the `--git-provider system` CLI option (falls back to JGit if `git` is missing).
   - A directory is scanned for `*.ntx` (recursively, one level for folder loads via `NTxEngineUtils.isNTexupFile`), `main.ntx` is forced first, then alphabetical order (extension-insensitive comparator). Each file is loaded through `loadNode(...)` and appended to the document root.
   - A single file is parsed by `NTxDocStreamParser`.
2. `NTxDocStreamParser` (`engine/parser/NTxDocStreamParser.java`) reads the whole file with `NElementReader.ofTson().read(...)` → an `NElement`, tags every element with its **origin file** (`addCompilerDeclarationPathAnnotations`, used later for error reporting), and rewrites `if / elseif / else` sibling chains into a single nested `if{ cond, trueBloc, falseBloc }` control element.
3. `elementToDocument` (`DefaultNTxEngine.java:1019`) does something surprising at first: **the entire document is wrapped in a single `CtrNTxNodeUncompiled` node holding the raw `NElement`**, and that single node is appended to the root (a `page-group`).

### Why "one node for the whole file"?

The `NElement → NTxNode` conversion is **deliberately deferred**. From now on the document is a lazy stream:
- At **document-compile time**, the compiler re-parses the raw element and walks the real statements.
- At **page-compile time**, pages are materialized on demand (see §4).

Net effect: opening/loading a huge deck costs only a TSON parse; no intermediate objects are built for content that may never be shown.

---

## 3. Stage two — compile the document (not the pages)

`NTxCompiler` (`engine/eval/NTxCompiler.java`) drives this.

### 3.1 `compileDocument` (`NTxCompiler.java:45`)
- Copies the raw document, clears the root children, builds a root `NTxResolutionContextImpl`.
- Runs a `FillDocumentCompileNodeVisitor` over each root child via `compileNode`.
- Bookkeeping: every control construct that *succeeded* is left in place; errors are counted (a `SilentNTxLogger` decides `successfullyLoaded`).

### 3.2 The `compileNode` dispatcher (`NTxCompiler.java:105`) — key rules

- **Page boundary rule (the core of laziness):** when the walker enters a node of type `PAGE` and was *not* already inside a page, it **stops** — `visitor.visitNode(node, context); return;` (`NTxCompiler.java:129`). Page *children are not compiled during document compilation*.
- `CtrNTxNodeUncompiled` (raw TSON) → calls `engine.parseNode(raw, context, ...)` right there, then re-dispatches (`NTxCompiler.java:135`). This is where deferred parse finally happens for document-level statements.
- Control nodes are expanded by type:
  - `CTRL_IF` → eval condition, take true/false child block (`compileNodeTree_if`, `:604`).
  - `CTRL_FOR` → eval expression, iterate (one new child-context per iteration bound with `withVar(...)`), parse each body element (`compileNodeTree_for`, `:630`).
  - `CTRL_ASSIGN` / `CTRL_ASSIGN_DEFAULT` → set a variable in the resolution context (`=` unconditional, `:=` only if undefined).
  - `CTRL_NAME` → variable lookup (unroll arrays into nodes) with fallback to a plain-text node.
  - `CTRL_EXPR` → evaluate expression, parse the result element into the parent.
  - `CTRL_DEFINE` → register a named component definition (`NTxNodeDef`).
  - `CTRL_CALL` → three-way resolution: (a) named definition → **inline the definition body** with parameter binding + caller body as `COMPONENT_BODY_VAR_NAME` (`_process_call_node`, `:429`); (b) a function → call it; (c) a node type → node parser.
  - `CTRL_INCLUDE` / `CTRL_IMPORT` → see §5.
  - `FRAGMENT` → transparent (same scope) inline; `BLOCK` → new scope then dissolve; `PAGE` / `PAGE_GROUP` / `GROUP` → new scope, children re-walked (`compileNodeTree_container`).
- Style rules and `@define`s encountered at any level are collected into the root (`visitRule`, `visitDefinition`).

### 3.3 Scopes — `NTxResolutionContext`

`NTxResolutionContextImpl` (`engine/eval/NTxResolutionContextImpl.java`) is the lexical environment. Each `pushNode`/`withVar` creates a child context copying the parent maps — that is **the whole scoping mechanism**. `getVar` resolution order:
current context → parent chain → compiled document globals → dotted-name walk over `NTxObj` graphs → built-ins (`HOME`, `USERNAME`) → JVM system properties.

See `documentation/specifications/isolation.md` for the fragment/group/block contract.

---

## 4. Lazy & incremental compilation

### 4.1 Lazy pages: `NTxCompiledDocumentImpl.readMore()` (`engine/impl/NTxCompiledDocumentImpl.java:390`)

The compiled document does **not** hold a list of fully compiled pages. It holds:

- `unparsed` — a stack of `(node, context)` pairs left to materialize,
- `compiledPages` — already-materialized `NTxCompiledPage`s,
- a fingerprint/source-monitor used later.

`pages()` / `page(n)` iterate through a **pull model**: asking for the next page calls `readMore()`, which pops the `unparsed` stack and applies these rules:

| Node type encountered | Behavior in `readMore()` |
|---|---|
| `PAGE` | flush current `PendingAutoPage` (if any) as a compiled page, then push this page as a new compiled page. **Stops** (returns true). |
| `CTRL_ASSIGN` / `_ASSIGN_DEFAULT` / `CTRL_DEFINE` | executed at document level, kept as `pendingInstr` so each page carries its preceding instructions. |
| `PAGE_GROUP` / `BLOCK` | new child context; children pushed on the stack. |
| `FRAGMENT` | same context; children pushed. |
| `CTRL_CALL` **inside a page** | accumulated into the current `PendingAutoPage` (this is **auto-pagination**: body content not wrapped in an explicit `page` becomes an implicit page). |
| `CTRL_CALL` **outside a page** | compiled now (`engine.compileNode`) and the resulting nodes pushed back. |

The stack pop order means the tree is walked **depth-first, lazily, one page at a time**. Two guards, configurable via engine env `warnPageCount` (default 1024) and `maxPageCount` (default 65536), warn/stop runaway decks.

This is also where **in-page content (leaf nodes, shapes, text) is finally parsed and where only those nodes are 'kept'**. The `NTxCompiledPage` holds its own context snapshot, so rendering a page is self-contained.

### 4.2 Per-page render caching

`DefaultNTxNode` carries a `renderCache`. `DefaultNTxEngine.renderPage(...)` respects it and only invalidates when cache use is disabled (`renderImage(..., useCache)`). Node-level rendering that is expensive (rasters for equation/plantuml/plot2d) profits from this when the same page re-renders.

### 4.3 Incremental re-render (the Swing viewer)

- Every loaded/parsed/rendered source is registered with the document's `sourceMonitor()` (`NTxSourceMonitor`), including each included file.
- `NTxCompiledDocumentImpl.addMonitoredSource(path)` tracks them; `addSourceFingerprintPart` records raw bytes for change detection.
- `DocumentView` (`renderers/ntexup-renderer-screen`) runs a `java.util.Timer` that polls `compiledDocument.sourceMonitor().changed()` and re-renders the affected page thumbnails — hence the "live preview" behavior when you edit a `.ntx` file.
- The **authoring GUI** (`core/ntexup-desktop`) additionally offers explicit reload/debug panels.

> Note: incremental compilation currently works at **file level** (source file changed → re-render). There is no fine-grained AST cache-keyed recompilation yet — the document is recompiled and pages re-materialized, but only the visible pages are re-rendered to images.

---

## 5. How included chunks & imports are compiled (and when)

This is the "how included chunks are compiled and when" answer.

### 5.1 `include()` — content chunks (`compileNodeTree_include`, `NTxCompiler.java` around `:540`)

When the compiler reaches a `CTRL_INCLUDE` node (created by the include parser):

1. **Resolve** the argument with `context.resolvePath(...)` → supports `eitherPath(a, b)` (local first, then GitHub), `github://...` (cloned via `NTxGitHelper`), relative/absolute `NPath`.
2. **Glob**: for a directory use `**/*.ntx`; a `.ntx` file is used as-is. Multiple comma-separated patterns are also allowed.
3. **Sort**: `NTxEngineUtils.comparePaths` (extension-insensitive) so `0010-...` < `0020-...`; `main.ntx` is *not* re-added for includes.
4. **For each file**: `engine.loadNode(node, path, compiledDocument)` parses the file → returns **a `CtrNTxNodeUncompiled`** (raw element again!) → `new DispatchCompileNodeVisitor(visitor).visitItem(item, context)` → the included file's top-level content is **compiled immediately in the current context** (same page-boundary rule applies: a `page` inside the include is *not* descended into, non-page statements are compiled now).
5. **Register** the file with `addMonitoredSource(nPath)` (for hot reload) and, in a boot context, `addSourceFingerprintPart(...)`.

**Timing summary — when is an included chunk compiled?**
- The include *directive* is resolved strictly in source order, at the moment the walker reaches it, using the context of its position in `main.ntx`.
- Each included file's *non-page* statements compile **eagerly at that point** (this is how `01-styles` and `03-lib` components become available to later includes).
- Any `page{...}` inside an included file is only **scheduled** — it is pushed into the lazy page pipeline and compiled on demand by `readMore()` when the viewer/renderer reaches that page number.
- `CtrNTxNodeUncompiled` indirection means the parse of each included file is itself deferred until the compiler asks for its nodes.

### 5.2 `import()` — Java extensions (`compileNodeTree_import`, `NTxCompiler.java:587`)

`import("plantuml")` is a *load-time* operation:

1. Arguments are evaluated to strings; resolved through `DefaultNTxEngine.importDependencies(...)` (`DefaultNTxEngine.java:528`).
2. **Name defaulting**: `plantuml` → group `net.thevpc.ntexup`, artifact `ntexup-extension-plantuml`, version `NTxEngine.CURRENT_VERSION`. `import("defaults")` expands to the engine's `defaultDependencies()` (geometry2d/3d + all nine extensions).
3. The resolved `NDependency`s are `classLoader.add(...)`'d — the jars are pulled from the **Nuts repository** and appended to the running engine's classpath.
4. Every `NTxDependencyLoadedListener` (that is, every service registry) is notified → registries **rescan** `NServiceLoader` and register the new services as CUSTOM entries (`onLoadDependencyLoaded → build(deps, true)`).
5. The loaded dependency is recorded into the compiled document's **dependency fingerprint** (`addDependencyFingerprintPart`), so a manifest can prove which extension version produced a document.

> `import()` is processed during document compilation, before the next includes — so an `import("plantuml")` must appear before a `page` that uses `uml(...)` if the document is laid out that way. Because `pages` are only *scheduled* (not compiled) during document compilation, an `import()` *after* a page also works for page content in practice; in-page content isn't compiled until `readMore()`/render.

---

## 6. Stage three — render

1. `NTxCompiledDocument.pages()` / `page(i)` materializes the required pages (§4.1).
2. A page is rasterized: `DefaultNTxEngine.renderPage(page, config)` creates an ARGB `BufferedImage`, sets antialiasing/rendering hints, builds a `DefaultNTxRendererContext`, and calls `renderer.render(context)` → `renderImageBytes` returns PNG bytes.
3. Node rendering dispatch: `engine.getRenderer(node.type())` → `NTxNodeRendererList` → `NTxNodeRenderer.render(NTxRendererContext)`. The engine's base renderers for `page`, `fragment`, `group`, `block`, `import`, `assign`, `define` come from the engine SPI file.
4. Generic nodes ship through `NTxNodeRendererBase` which gives rotation, opacity/shadow and debug-box handling for free; the builder-derived renderers (`NTxNodeRendererAsDefault/AsText/AsConverter`) delegate to the callbacks registered by `NTxNodeBuilder`.
5. Text goes through `NTxTextRendererBase`, which splits text on flavors (`[[eq: ...]]`, `\( ... \)`, etc.) and emits rich tokens (`NTxRichTextToken`), including **image-painter** tokens for inline equations.
6. `NTxGraphicsImpl` is the only `NTxGraphics`; it wraps a `Graphics2D` and converts high-level `NtxElement2D` primitives (line/quad/cubic/polygon + arrowheads) into AWT calls and `drawImage(...)` dispatch (`engine.imageTypeRendererFactories()` → SVG / GIF / PlantUML / LaTeX factories).

### The three output paths at a glance

| Output | Mechanism |
|--------|-----------|
| **PDF** | each page → PNG (≈200 dpi, `withPrint(true)`) → OpenPDF `PdfPTable` grid (configurable `gridX x gridY` slides per A4 sheet, page-number footer via `PageNumberEvent`). Single-node variant goes HTML→PDF via Flying Saucer. |
| **HTML** | `<div class="page">` per page; raster images under `images/page-NNN.png`; zip/stream option. |
| **Screen** | Swing `DocumentView` with per-page `PageView` components (16:9 reference size), timer-driven hot reload. |

---

## 7. NTxEngine programmatic recipe (the five-line summary)

```java
Nuts.openWorkspace().share();                 // 1. init Nuts (required)
NTxEngine e = new DefaultNTxEngine();         // 2. create engine (loads base services)
NTxCompiledDocument doc = e.loadDocument(NPath.of("slides.ntx")); // 3. parse + bootstrap compilation
NTxDocumentStreamRenderer r = e.newPdfRenderer().get();          // 4. pick renderer
r.setOutput(NPath.of("out.pdf"));
r.render(doc);                                 // 5. render (lazily pulls each page)
```

Full recipes (including headless PNG rendering and batch-test harnesses) are in [04-programmatic-api-testing.md](04-programmatic-api-testing.md).
