# ntexup Repository Architecture & Design Choices

This document explains **what ntexup is architecturally**, how the repository is organized, what each module is for, and which architecture styles and decisions were taken. It is written for developers who need to navigate, fix, and extend the codebase.

Related docs:
- The full engine pipeline (parse → compile → render, lazy & incremental compilation): [02-engine-pipeline.md](02-engine-pipeline.md)
- Building Java extensions: [03-extension-development.md](03-extension-development.md)
- Programmatic API + automated PDF tests: [04-programmatic-api-testing.md](04-programmatic-api-testing.md)
- Fixing common problems: [05-troubleshooting.md](05-troubleshooting.md)

---

## 1. What ntexup is

**ntexup** is a declarative, text-based **document & presentation generator**. A user writes `.ntx` files in a TSON expression language; the engine compiles them into an abstract document tree and renders it. It is a single-reactor **Java 17 / Maven** project that runs on top of the **Nuts** package manager and the **TSON** data model.

Authoring surface:

```
styles { ... }                 // CSS-like declarations
page {                         // a slide / page
    text("hello", at: top)
    eq("x^2 = \sin(x)")        // LaTeX math
    rectangle(size: (50,20), background: documentColors[4])
}
content-slide(title: "X") { body { ... } }   // theme-provided component
include("02-pages")            // modular multi-file projects
import("plantuml")             // load a Java extension at runtime
```

Internally this becomes a pipeline with **three strictly separated stages**:

```
TSON (.ntx)
   │  NTxDocStreamParser (NElementReader.ofTson)
   ▼
NElement (raw TSON model)                    ─── PARSED (but not yet nodes)
   │  NTxCompiler.compileNode / CtrNTxNodelUncompiled
   ▼
NTxNode tree + NTxCompiledDocument           ─── COMPILED (per page, lazily)
   │  NTxRendererContext / NTxGraphics
   ▼
AWT Graphics2D → PNG → PDF / HTML / Screen   ─── RENDERED
```

The single most important architectural fact: **parsing and the `NElement → NTxNode` conversion are deferred**. A whole document is stored as one raw TSON element and is converted to node objects lazily during compilation, page by page. See [02-engine-pipeline.md](02-engine-pipeline.md) for details.

---

## 2. Repository layout (read this to find your way around)

Top-level directory (`ntexup/`):

| Path | Contents |
|------|----------|
| `pom.xml` | Reactor root, parent `net.thevpc.xprojects.builders:ntexup-builder:1.0.0.0`, lists every module. **Add new modules here.** |
| `core/` | Engine + API + desktop UI/CLI |
| `lib/` | **Neutral shared libraries** (geometry). Nothing depends on extensions here. |
| `extensions/` | Java plugins (`ntexup-extension-*`) loaded at runtime via `import()`. |
| `renderers/` | Output renderers: PDF, HTML, Screen, Web. |
| `tutorials/` | Minimal runnable extension tutorial. |
| `companions/` | Standalone apps (scoreboard) not bundled as engine extensions. |
| `test/` | Sample decks + `main()` render programs (the current "tests"). |
| `app/` | Thin launcher apps (Nuts entry points) + experimental web viewer. |
| `documentation/` | This guide, the user guide (`user-guide/`), agent skills (`agents/`), specs (`specifications/`). |

All released artifacts share the version **`1.0.0.0`**. The enforced build baseline is **Java 17** (`maven.compiler.release`). Build the whole reactor with `mvn verify`.

---

## 3. Module by module

### 3.1 `core/ntexup-api` — interfaces, SPI contracts, document model

**~163 classes. Nothing is implemented here; this is the contract surface every extension compiles against.**
Dependencies: only Nuts (`net.thevpc.nuts`) + TSON element model. **Extensions depend only on this module.**

Main packages (`core/ntexup-api/src/main/java/net/thevpc/ntexup/api/`):

| Package | Role | Key types |
|---------|------|-----------|
| `document/` | Document model | `NTxDocument`, `NTxDocumentClass`, `NTxDocumentKind`, `NTxDocumentFactory`, `NTxSizeRequirements` |
| `document/node/` | Node tree | `NTxItem`, `NTxItemList`, `NTxNode`, `NTxNodeCtrl`, `NTxNodeDef`, `NTxNodePage`, `NTxNodeType` (all type constants), `DefaultNTxNode` is in engine |
| `document/style/` | Properties & styles | `NTxProp`, `NTxProperties`, `NTxProps`, `NTxPropName` (**canonical property-name constants**) |
| `document/elem2d/`, `elem3d/` | Render elements (the renderer-agnostic geometry) | `NtxElement2D`, `NTxBounds2D`, `NTxBounds3D`, `NTxPoint2D`, `NTxDouble2`, `NTxMargin`, `NTxAlign` |
| `document/security/` | Manifest/signature DTOs | `NTxManifest*`, `NTxManifestOptions` |
| `engine/` | Engine contract | `NTxEngine`, `NTxEngineTools`, `NTxCompiledDocument`, `NTxCompiledPage`, `NTxNodeBuilderContext` (**the node-builder DSL**), `ImportDependencyResult(s)`, `NTxTemplateFilter` |
| `eval/` | Values, expressions, scopes | `NTxResolutionContext` (variable scope), `NTxFunctionCallContext`, `NTxValue`, `NTxVar`, `NTxObj`, `NTxObjectEvalContext` |
| `extension/` | **SPI extension points** | `NTxNodeBuilder`, `NTxFunction` |
| `parser/` | Parsing contracts | `NTxNodeParser`, `NTxItemParser`, `NTxArgumentReader`, `NTxAllArgumentReader` |
| `renderer/` | Renderer contracts | `NTxDocumentRenderer`(+Factory/Supplier/Base), `NTxDocumentStreamRenderer`, `NTxDocumentScreenRenderer`, `NTxNodeRenderer`, `NTxRendererContext`, `NTxGraphics`, `NTxImageTypeRendererFactory`, `NTxDocumentView` |
| `renderer/text/` | Text flavors | `NTxTextRendererFlavor`, `NTxTextRendererBuilder`, `NTxTextToken`, `NTxRichTextToken`, `NTxTextOptions` |
| `source/` | Sources & change monitoring | `NTxSource`, `NTxSourceMonitor` |
| `log/` | Logging | `NTxLogger`, `NTxLog` |
| `util/` | Generic helpers | `NTxSizeRef` |

### 3.2 `core/ntexup-engine` — the implementation

**~159 classes. The concrete engine.** Everything in `net.thevpc.ntexup.engine.*`:

| Package | Role | Key types |
|---------|------|-----------|
| `engine/` | Service registries + engine core | `DefaultNTxEngine` (extends `NTxEngine`), `default-style.ntx` resource |
| `engine/impl/` | Concrete impls | `NTxCompiledDocumentImpl` (lazy page materialization, fingerprints), `NTxEngineUtils`, `NtxServiceListImpl2` + per-family lists |
| `engine/eval/` | Compiler + evaluator | `NTxCompiler` (compile walker), `NTxNodeEval`, `NTxEvalUtils`, `NTxResolutionContextImpl`, `NTxGitHelper` |
| `engine/parser/` | TSON → nodes | `NTxDocStreamParser`, `NTxStyleParser` (**COMMON_STYLE_PROPS**), `DefaultNTxDocumentItemParserFactory`, `NTxParserUtils`, `NTxNodeParserBase`; `parser/nodeparsers/` = node-specific parsers (incl. `ImportSpecialParser`, `StylesSpecialParser`), `parser/ctrlnodes/` = control node holders |
| `engine/base/nodes/` | Built-in node builders | `NTxPlainTextBuilder`, `NTxTextBuilder` (+ `text/`) |
| `engine/base/functions/` | Built-in functions | `either`, `eitherPath`, `stringJoin`, `dsteps`, `dtimes` |
| `engine/renderer/` | Common render machinery | `NTxGraphicsImpl`, `NTxNodeRendererBase` (rotation/shadow/debug pipeline), page/fragment/import/... renderers, `elem2d/` strokes & text |
| `engine/ext/` | **Builder-backed service materialization** | `NTxNodeBuilderContextImpl`, `CustomNTxNodeParserFromBuilder`, `NTxNodeRendererAsDefault/AsText/AsConverter`, `CustomNTxTextRendererFlavorFromBuilder` |
| `engine/conf/` | Configuration | `AuthorInfoConfigFile` (author.tson + EC key) |
| `engine/auth/`, `security/` | Signing & manifest audit | `AuthorshipSigner`, `NTxManifestElementMetaDataBuilder`, `Rfc3161Helper` |
| `engine/repo/` | Dependency/import resolution hooks | `ImportDependencyResult` infrastructure |
| `engine/document/` | Node/doc impls | `DefaultNTxNode`, `DefaultNTxDocument`, `NTxDocumentFactoryImpl` |

Engine meta-observations:
- Registered SPI files live in `core/ntexup-engine/src/main/resources/META-INF/services/` (13 parsers, 7 node renderers, 5 functions, 2 node builders).
- The engine loads all services through **Nuts `NServiceLoader`** using its own `NMutableClassLoader` (`DefaultNTxEngine.loadServices`, `DefaultNTxEngine.java:515`). Extensions added at runtime (via `import()`) are added to that classloader and the registries **rescan** themselves.
- The engine ships a default stylesheet resource `net/thevpc/ntexup/default-style.ntx` applied to every document root.

### 3.3 `core/ntexup-engine-full`

A thin **aggregator** module ("one jar with everything"). Only contains a test class `TestEngine.java` demonstrating `new DefaultNTxEngine()` + `loadDocument(InputStream)`. Its `pom.xml` pulls engine + geometry libs; `app/ntexup-full` depends on it.

### 3.4 `core/ntexup-desktop` — CLI + authoring GUI

The human-facing tooling, in `net.thevpc.ntexup.*`:
- `cmdline/` — `NTexupOptionsParser`, `NTexupOptionsProcessor`, and the option DTOs (`Action` enum: `SHOW_FRAME`, `SHOW_HTML`, `SHOW_DOCUMENT`, `NEW`, `GENERATE`, `LIST_TEMPLATES`, `BUILD_REPO`, `DUMP`, `EDITOR`). The CLI actions map to engine calls (e.g. `runActionGenerate` builds a PDF via `e.newPdfRenderer()`).
- `main/`, `debug/` — Swing `MainFrame`, debug panels (`NTxDebugFrame`, `NTxDocumentPanel`, `NTxNodePanel`, `NTxTsonPanel`).
- `config/` — user/viewer config persistence.

### 3.5 `lib/` — the "common libs" (do NOT duplicate this code in extensions)

| Module | Content | Depended on by |
|--------|---------|----------------|
| `lib/ntexup-lib-geometry2d` | JTS-based 2D geometry: `NtxElement2D` primitives/factory, region maths | `core/ntexup-engine` + `ntexup-extension-shapes2d` |
| `lib/ntexup-lib-geometry3d` | 3D meshes, `NTxCamera3DImpl`, `NtxGraphics3DImpl`, element-3D renderer registry | `ntexup-extension-shapes3d` + part of the engine's default import set |

**These are the canonical example of the "shared common lib" rule** (see §7 of [03-extension-development.md](03-extension-development.md)): geometry that two extensions (or an extension and the engine) need is extracted into a neutral artifact, **not** imported extension-to-extension.

### 3.6 `extensions/` — runtime Java plugins

Nine plugin modules. Each is a Maven module depending on `ntexup-api` (only `shapes3d` also depends on the engine + geometry3d). Loaded from a document with `import("name")`.

| Module | Provides via `NTxNodeBuilder` SPI | Third-party lib |
|--------|----------------------------------|-----------------|
| `ntexup-extension-shapes2d` | 36 builders: shapes, lines/arrows, containers (grid, group, row, column, ul/ol), filler (filler/void), text (ntf, source), image | `ntexup-lib-geometry2d` |
| `ntexup-extension-shapes3d` | 3D primitives + `scene3d` (registered via Nuts `NComponent` + `NtxElement3DNodeParser`, plus `NTxNodeBuilder` for several primitives) | `ntexup-lib-geometry3d` + engine |
| `ntexup-extension-plantuml` | `uml`, `nwdiag`, `gantt`, `wireframe`, ... (15 builders) | plantuml, guava |
| `ntexup-extension-plot2d` | `plot2d` builder | jfreechart |
| `ntexup-extension-latex` | `eq`/`equation` builder with inline `[[ eq: ... ]]` text flavor | jlatexmath |
| `ntexup-extension-svg` | `NTxImageTypeRendererFactory` for SVG images (Salamander rasterizer) | svg-salamander |
| `ntexup-extension-animated-gif` | `NTxImageTypeRendererFactory` for animated GIFs | animated-gif-lib |
| `ntexup-extension-common-functions` | 15 `NTxFunction`s (color transforms, array rotations) | none |
| `ntexup-extension-presenters` | `ScoreBoardBuilder` (tournament/score presenter) | none |

Section 3 of [03-extension-development.md](03-extension-development.md) explains module structure and the **complete list of SPI files**.

### 3.7 `renderers/` — output formats

| Module | Registers | Output |
|--------|-----------|--------|
| `renderers/ntexup-renderer-pdf` | `NTxDocumentRendererFactory` → `PdfDocumentRenderer` (type `"pdf"`) | Rasterizes each compiled page to PNG, embeds into an OpenPDF `PdfPTable` grid; single-node path uses Flying Saucer HTML→PDF |
| `renderers/ntexup-renderer-html` | type `"html"` | `<div class="page">` per page + `images/page-NNN.png` raster images (or a zip) |
| `renderers/ntexup-renderer-screen` | type `"screen"` | Swing `DocumentView` (JFrame + `PageView` per page), timer-driven auto-refresh when source files change |
| `renderers/ntexup-renderer-web` | **experimental** Spring Boot app | HTTP PNG service (`/api/document/images?pageNumber=...`), not production-ready |

Renderers are a thin layer: **all drawing happens into AWT `Graphics2D`** (via `NTxGraphics`), a page becomes a `BufferedImage` (`engine.renderImageBytes`), and PDF/HTML just embed those images. See [04-programmatic-api-testing.md](04-programmatic-api-testing.md).

### 3.8 `tutorials/ntexup-extension-tutorial-myshape`

The **minimal extension reference** – exactly one `NTxNodeBuilder` (`my-shape`), one SPI file, one `pom.xml`. Copy this to start a new extension.

### 3.9 `companions/nscoreboard`

A standalone Swing app ("scoreboard"). Not an ntexup extension; it models panels that are *planned* to be embedded as an ntexup component (animation support is TODO). The runtime-level equivalent of what it will become lives in `ntexup-extension-presenters` (`ScoreBoardBuilder`).

### 3.10 `test/ntexup-examples`

Sample `.ntx` decks under `src/ntexup/examples/` plus `main()` programs (`TestPdf`, `NTxExampleFromFile1/2`, `NTxExampleFromFolder`, `NTxExampleByCode1`, `NTxExampleTestBullets`, `NTxExampleTestGrid`, `TestCompareAntennas`). **This is the current de-facto functional-test harness**: there is no JUnit yet. `TestPdf` renders a whole documentation folder to `output.pdf`. See [04-programmatic-api-testing.md](04-programmatic-api-testing.md) for automating this.

### 3.11 `app/` — launchers

| Module | Role |
|--------|------|
| `app/ntexup` | Thin Nuts app launcher (`Nuts.openWorkspace().share()` + `DefaultNTxEngine`), the `nuts ntexup ...` command. |
| `app/ntexup-full` | Same, bundling engine-full + desktop. |
| `app/ntexup-viewer-backend` | Experimental Spring Boot 3 backend (JGit repo clone, serves page images, CORS for the Angular frontend). |
| `app/ntexup-viewer-frontend-ng` | Experimental Angular frontend. |

---

## 4. Architecture styles & the decisions that were taken

### 4.1 Three-stage pipeline with deferred conversion
TSON text → raw `NElement` → (lazy) node tree → compiled document → renderer-agnostic drawing. The distinction between **"parsed"** (`NElement`) and **"compiled"** (`NTxNode`) is a deliberate one: a file is converted to nodes only when its content is actually needed (document-level compile for includes/control flow, page-level compile on demand). Consequences: fast startup for huge decks, and includes can add new extensions *during* compilation.

### 4.2 Contract-first API (API vs Engine split)
`ntexup-api` is pure contracts; `ntexup-engine` is the only implementation. Extensions compile against the API only. This keeps the SPI surface small and stable, and lets alternate engines exist in principle.

### 4.3 Plugin architecture over Nuts `NServiceLoader`
- Component discovery is **not** `ServiceLoader` but Nuts' `NServiceLoader`, using the engine's mutable classloader (`DefaultNTxEngine.java:515`).
- Service families are held in registries (`NtxServiceListImpl2`) with two buckets: **BASE** (shipped in engine) and **CUSTOM** (loaded from imported extensions).
- An `import()` first adds jars to the classloader, then notifies `NTxDependencyLoadedListener`s → the registries **re-scan and grow**.
- Duplicate ids between base and custom are rejected silently (base wins).

### 4.4 Declarative node components (`NTxNodeBuilder`)
New document elements are declared with a small DSL (`id`, `parseParam`, `renderComponent`, ...). From one builder the engine **derives three services** (parser, renderer, and optionally text-flavor) and registers them (`NTxNodeBuilderList.onAfterNewService`, `engine/ext/NTxNodeBuilderContextImpl`). Authors do **not** hand-write a parser+renderer pair.

### 4.5 Raster-first rendering
Everything draws into AWT `Graphics2D` via `NTxGraphics`. PNG is the interchange format: PDF page = PNG embedded in OpenPDF; HTML page = PNG under `images/`; screen = direct Java2D. This gives perfect WYSIWYG across all outputs at the cost of raster text (acceptable for slides).

### 4.6 Lexical scoping as first-class document structure
Variable scope and rendering are orthogonal (`documentation/specifications/isolation.md`):
- `fragment` — no new scope, dissolves into parent (pure organization).
- `block` — new scope, dissolves (temporary logic).
- `group` — new scope, retained (visual container).
This maps 1:1 to the compiler (`NTxCompiler` fragment/block/group cases) and to the lazy page walker `NTxCompiledDocumentImpl.readMore()`.

### 4.7 Shared neutral libraries instead of extension→extension deps
This is a **hard rule** (see [03-extension-development.md](03-extension-development.md) §7): extensions never depend on each other. Shared geometry lives in `lib/ntexup-lib-geometry2d` / `lib/ntexup-lib-geometry3d`; the engine's `defaultDependencies()` imports all extensions + both geometry libs so a full engine works out of the box.

### 4.8 Everything is a "source" that can be fingerprinted & served remotely
Paths are `NPath`; GitHub paths (`github://user/repo/...`) are cloned lazily into a cache and rate-limited (`NTxGitHelper`). Every compiled artifact accumulates a **fingerprint** of content files + imported dependencies (including which *version* of a theme/ext was used), enabling both the Swing hot-reload (`sourceMonitor()`) and authorship manifests.

### 4.9 Authorship & provenance (not sandboxing)
Signing uses `SHA-256` + ECDSA/EC keys (`secp256r1`), RFC 3161 trusted timestamps (BouncyCastle), and manifests over `(payload, resources, dependencies)` fingerprints. **There is no bytecode sandbox**: imported extensions run with full JVM privileges — the user must `nuts install`/trust the artifact.

### 4.10 Templates as versioned themes in a separate git repo
Themes (`classic`, `eniso`, `ibtihel`, `meridian`) live in the `ntexup-templates` repository and are included via `include(github://thevpc/ntexup-templates/.../theme)` or a local clone, resolved with `eitherPath`. Templates are ordinary `.ntx` documents, not code.

### 4.11 Simple, observable things (deliberately kept small)
- No IoC container; one `DefaultNTxEngine` with constructor injection of classloaders.
- No test framework yet; verification is done by rendering PDFs from sample decks (see §2 of `04-programmatic-api-testing.md`).
- Config is minimal: `engine.getEnv/setEnv` map (`warnPageCount=1024`, `maxPageCount=65536`), `author.tson`, and the stream-renderer config DTO.

---

## 5. Where to look for a specific feature

| If you need to…  | Start at  |
|------------------|-----------|
| Understand how a `.ntx` file becomes a document | `02-engine-pipeline.md`, `NTxDocStreamParser`, `DefaultNTxEngine.loadDocument` |
| Add a new visual element (component) | `03-extension-development.md` §4; copy `tutorials/ntexup-extension-tutorial-myshape` |
| Add a new pure function usable in `.ntx` | `03-extension-development.md` §5 |
| Add inline markup like `[[ eq: ... ]]` | `03-extension-development.md` §6 |
| Register a raw parser/renderer without the builder DSL | `03-extension-development.md` §8 |
| Know which properties are shared by all elements | `03-extension-development.md` §9 + `NTxPropName` + `NTxStyleParser.COMMON_STYLE_PROPS` |
| Render a document to PDF from Java code / tests | `04-programmatic-api-testing.md` |
| Understand lazy & incremental compilation | `02-engine-pipeline.md` §4–§5 |
| Fix a build or rendering failure | `05-troubleshooting.md` |
| Navigate the node type / property vocabulary | `documentation/user-guide/27-node-type-reference.md`, `26-property-reference.md` |
