---
name: "ntexup-develop-extensions"
description: "Develops and extends the ntexup codebase itself: adds Java extensions (node types/components, functions, inline text flavors), registers parsers/renderers, follows the common-libraries dependency rule, and verifies changes by automatically rendering PDFs. Invoke when the user wants to add new ntexup functionality, fix engine runtime issues, or understand the compile/render pipeline."
---

# NTexUp Extension & Engine Development

This skill supports an agent doing **developer work on the ntexup repository** (not authoring decks — for decks use the `ntexup-presentation` skill). It lets you add new components/functions/flavors, navigate the engine, and verify everything by **rendering a PDF automatically**.

Authoritative references (read before/while working):
- `$ntexup_repository_root/documentation/developer-guide/00-index.md`
- `$ntexup_repository_root/documentation/developer-guide/01-repository-architecture.md`
- `$ntexup_repository_root/documentation/developer-guide/02-engine-pipeline.md`
- `$ntexup_repository_root/documentation/developer-guide/03-extension-development.md`
- `$ntexup_repository_root/documentation/developer-guide/04-programmatic-api-testing.md`
- `$ntexup_repository_root/documentation/developer-guide/05-troubleshooting.md`

`$ntexup_repository_root` = the folder containing `pom.xml` (the `ntexup-builder` reactor root).

---

## 1. Orientation (30 seconds)

Modules: `core/ntexup-api` (interfaces/SPI — **extensions depend ONLY on this**), `core/ntexup-engine` (the implementation), `lib/ntexup-lib-geometry2d|3d` (shared common libs), `extensions/ntexup-extension-*` (plugins), `renderers/ntexup-renderer-*` (PDF/HTML/screen/web), `tutorials/` (minimal extension), `test/ntexup-examples` (sample decks + `main()` render programs), `app/` (launchers), `companions/` (standalone apps).

Pipeline (three stages):
```
TSON .ntx ──NTxDocStreamParser──▶ NElement (raw, one CtrNTxNodelUncompiled per file)
        ──NTxCompiler.compileNode──▶ NTxNode tree → NTxCompiledDocument (pages lazy)
        ──NTxRendererContext / NTxGraphics (AWT Graphics2D)──▶ PNG → PDF/HTML/Screen
```

Key lazy facts you must respect when debugging:
- **Deferred parse:** a whole `.ntx` file is held as one raw `NElement` (`CtrNTxNodelUncompiled`); `NElement → NTxNode` conversion happens at compile time.
- **Page boundary:** document compilation stops at a `page{...}` node (`NTxCompiler.compileNode`); page children compile **on demand** via `NTxCompiledDocumentImpl.readMore()` (auto-pagination: content without an explicit `page` becomes a `PendingAutoPage`).
- **Includes:** resolved in source order at the `include(...)` point. The included file's non-page content compiles eagerly there; `page`s inside it are scheduled and compiled lazily.
- **Imports:** `import("x")` → `DefaultNTxEngine.importDependencies` → jar added to the engine's `NMutableClassLoader` → every service registry rescans (`NtxServiceListImpl2`).

---

## 2. Standard workflow

1. Identify what the user wants:
   - visual element / component → `NTxNodeBuilder` (§3)
   - pure function usable in TSON → `NTxFunction` (§4)
   - inline markup like `[[eq: ...]]` → `renderText()` flavor (§5)
   - whole new output format → `NTxDocumentRendererFactory` (§6)
   - image file format → `NTxImageTypeRendererFactory` (§6)
2. **Read the matching section of `documentation/developer-guide/03-extension-development.md`**, then the tutorial module `tutorials/ntexup-extension-tutorial-myshape/` for a working skeleton.
3. Create/extend the Maven module; follow the common-libraries rule (§7).
4. **Verify by rendering to PDF** (§8). Never finish without a render check.

---

## 3. Adding a node type / component — `NTxNodeBuilder` (recommended)

Write `build(NTxNodeBuilderContext)` using the DSL:

```java
public class NTxMyShapeBuilder implements NTxNodeBuilder {
    @Override
    public void build(NTxNodeBuilderContext c) {
        c.id("my-shape")                                        // node type name
         .parseParam().matchesNamedPair(NTxPropName.WIDTH, NTxPropName.HEIGHT, "base", "hat").end()
         .renderComponent(this::render);
    }
    void render(NTxRendererContext ctx) {
        NTxBounds2D b = ctx.selfBounds2D();                     // layout box
        NTxPoint2D base = NTxValue.of(ctx.node().getPropertyValue("base")).asPoint2DOrDouble().orNull();
        NTxGraphics g = ctx.graphics();
        g.draw(shape); g.fill(shape);
        ctx.drawContour();
    }
}
```

DSL cheat-sheet (`NTxNodeBuilderContext`):
- identity: `id()`, `ids()`, `alias()`
- params: `.parseParam()` → `.matchesNamedPair(...)`, `.matchesString()/matchesName()/matchesStringOrName()`, `.matchesAnyNonPair()`, `.storeName()`/`.storeFirstMissingName()`, `.asFlags()`, `.matchesNonCommonFlags()`, `.then()`/`.end()`
- render: `.renderComponent(this::render)`; inline text: `.renderText().buildText(...).parseTokens(...).startSeparators("\\(").end()`
- layout: `.sizeRequirements(...)`, `.selfBounds2D(...)`, `.selfBounds3D(...)`
- lifecycle: `.initializeNodeAction(...)`, `.processChildren(...)`, `.compileNode(...)`, `.parseDefaultParams()`, `.withToElem(...)`

The engine **derives parser + renderer + optional text-flavor** from the builder (`NTxNodeBuilderContextImpl` → `CustomNTxNodeParserFromBuilder` / `NTxNodeRendererAsDefault` + `CustomNTxTextRendererFlavorFromBuilder`). Common props are auto-consumed by the derived parser — you don't need `at`, `size`, `color` in your param rules.

Register: `src/main/resources/META-INF/services/net.thevpc.ntexup.api.extension.NTxNodeBuilder` listing the class.

Render-time helpers you can use in `RenderAction`: `applyBackgroundColor()`, `getLineColor(true)`, `applyContourColor(!bg)`, `withStroke(() -> ...)`, `drawContour()`, `withDefaultStyles(...)`. Coerce any raw prop with `NTxValue`, `NTxValueByName`.

---

## 4. Adding a function — `NTxFunction`

```java
public class MyFunction implements NTxFunction {
    public String name() { return "myFunction"; }               // id in TSON
    public NElement invoke(NTxFunctionCallContext args) {
        NElement a = args.evalArg(0);
        args.checkTooFewArgs(1);
        return NElement.of...;                                  // return a TSON value
    }
}
```

Context API: `size()`, `arg(i)`, `evalArg(i)`, `evalArg(i, converter, name, errSupplier)`, `checkTooFewArgs/TooManyArgs`, `scopedContext()`, `log(...)`.
Register: `META-INF/services/net.thevpc.ntexup.api.extension.NTxFunction`.
Reference: `NTxFunctionEither` and the color functions in `ntexup-extension-common-functions`.
Do **not** re-implement control flow (`include/import/for/if/define`) as functions — that's the compiler's job.

---

## 5. Adding an inline text flavor

Modern path — chain in the node builder (see `NTxEquationBuilder` in the `latex` extension):

```java
.renderText()
    .buildText(this::buildText)        // rich-token emitter
    .parseTokens(this::parseTokens)    // tokenizer for [[my: ...]]
    .startSeparators("\\(")            // extra inline delimiters
    .end()
```

Standalone path: implement `NTxTextRendererFlavor` (`type()`, `buildText(...)`, `getParsePrefixes()`, `parseTokens(NTxTextRendererFlavorParseContext)`) and register under `META-INF/services/net.thevpc.ntexup.api.renderer.text.NTxTextRendererFlavor`. Emit `NTxRichTextToken`, including `IMAGE_PAINTER` tokens for embedded rasters.

---

## 6. Low-level registration (parser / renderer / output / image)

Usually unnecessary — a builder already creates these. When genuinely needed:

| Want | Interface | SPI file |
|---|---|---|
| Parse a node type manually | `NTxNodeParser` (extend `NTxNodeParserBase` for free common-prop handling) | `net.thevpc.ntexup.api.parser.NTxNodeParser` |
| Render a node type manually | `NTxNodeRenderer` (extend `NTxNodeRendererBase`) | `net.thevpc.ntexup.api.renderer.NTxNodeRenderer` |
| New output format | `NTxDocumentRendererFactory` → `NTxDocumentStreamRenderer` / `NTxDocumentScreenRenderer` | `net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactory` |
| New image file format | `NTxImageTypeRendererFactory` | `net.thevpc.ntexup.api.renderer.NTxImageTypeRendererFactory` |
| 3D elements / generic Nuts components | `NComponent` + `NtxElement3DNodeParser` | `net.thevpc.nuts.spi.NComponent` (how `shapes3d` works) |

**Do not ship empty placeholder SPI files** — some extensions still carry them from an older design; prefer the builder path.

---

## 7. Dependency rule: common libs, NEVER chained extensions

- An extension **must never** depend on another extension (`net.thevpc.ntexup:ntexup-extension-*` in your pom → design smell).
- Shared code goes into a neutral artifact under `lib/` (geometry2d/3d are the precedent) or `core/`; add it to the reactor root `<modules>`.
- Extension poms depend on: `ntexup-api` + third-party libs; exception: `shapes3d` also depends on `ntexup-engine` + `ntexup-lib-geometry3d`.
- Rationale: no import-order coupling, single source of truth, clean acyclic reactor, independent resolution.

---

## 8. Verify by automatically rendering a PDF (mandatory)

Run this from `test/ntexup-examples` (classpath must include reactor artifacts):

```java
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.engine.impl.NTxCompiledDocumentImpl;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.io.NPath;

Nuts.openWorkspace().share();
NTxEngine e = new DefaultNTxEngine();
NTxCompiledDocument doc = e.loadDocument(NPath.of(".../deck-path"));
if (!((NTxCompiledDocumentImpl) doc).isSuccessfullyLoaded()) throw new AssertionError("load failed");
int pages = doc.pages().size();                                  // forces lazy page compile
NTxDocumentStreamRenderer pdf = e.newPdfRenderer().get();
pdf.setOutput(NPath.of(".../out.pdf"));
pdf.render(doc);                                                 // throws on render error
```

Checklist:
1. `Nuts.openWorkspace().share()` first; create `new DefaultNTxEngine()`.
2. `loadDocument` a folder (auto `*.ntx`, `main.ntx` first) or a file.
3. Assert `successfullyLoaded()` — catches TSON/compile errors.
4. Walk `doc.pages()` — forces the lazy `readMore()` path and surfaces page-level errors.
5. Render to PDF; assert output exists and non-empty.
6. For visual regressions of a specific page: `byte[] png = e.renderImageBytes(page, new NTxNodeRendererConfig(w, h).withAnimate(false).withPrint(true));`

Headless/CI: PDF+PNG work without a display; screen renderer needs X11; containers need `git` for `github://` includes. To make `import("myext")` resolvable in the test run: `mvn install` then `nuts install net.thevpc.ntexup:ntexup-extension-<name>`.

Batch harness idea: iterate every deck in `test-commands.md`, render each to PDF, fail on load/render errors. Optional golden-image checks: SHA-256/pixel-diff of `renderImageBytes` output vs stored goldens.

---

## 9. Common properties (every element supports these)

Sources of truth: `NTxPropName` (`api/document/style/`) and `COMMON_STYLE_PROPS`/`COMMON_FLAG_STYLE_PROPS` (`engine/parser/NTxStyleParser.java`). Any pair/flag matching these is auto-consumed as a node property, so your builder's own param rules only need to declare custom props.

Groups: geometry/layout (`at`, `position`, `size`, `origin`, `margin`, `padding`, `rotate`, `columns/rows`, `colspan/rowspan`, ...), color/stroke (`color`, `background(-color)`, `line-color`, `contour-color`, `stroke`, `dash`, `shadow`, `draw-contour`, `round-corner`, ...), text (`font-family`, `font-size`, `font-bold/italic/underlined/strike`, `text-align`, ...), flags (`fill-background`, `draw-grid`, `preserve-aspect-ratio`, `threed`, `raised`, `disabled`, `debug`, `show`, `visible`, `class`).

---

## 10. Golden rules

1. Extensions depend on `ntexup-api` (+ 3rd-party). Never on another extension. Put shared code in `lib/`.
2. Every new module goes in the root `pom.xml` `<modules>`; version stays `1.0.0.0`.
3. New visual elements use `NTxNodeBuilder` — don't hand-write parser+renderer pairs.
4. Use `NTxValue`/`NTxValueByName` for property coercion; use `selfBounds2D()` for geometry, never hardcoded pixels.
5. Respect lazy compilation: syntax errors inside a `page{}` may not surface until that page renders — tests must walk all pages.
6. Verify with an automated PDF render (§8) before declaring done.
7. Understand "source order" semantics of `include`/`import` (§1) when users report ordering bugs — it's documented architecture, not a bug, and the fix is `main.ntx` ordering (styles/components before pages).

---

## 11. Escalation / diagnostics reference

| Symptom | Pointer |
|---|---|
| Node compiles but renders wrong bounds | check `sizeRequirements`/`selfBounds2D`; bounds come from `selfBounds2D()` |
| "Unknown property" for `font-bold` | user quoted a bare flag — keep it bare |
| Node type not found at runtime | `import()` missing, artifact not `nuts install`ed, or id collides (base wins silently) |
| Included file not visible | `include` compiles in source order — move `include` earlier |
| Extension x calls extension y behavior | violates rule §7 — extract `lib/` |
| Build fails | run `mvn verify` from reactor root; check `.mvn` settings; confirm module listed in root pom |

For deeper fixes, read `documentation/developer-guide/01-repository-architecture.md` §5 ("where to look for a specific feature") and `05-troubleshooting.md` §7 (source map for stack traces).