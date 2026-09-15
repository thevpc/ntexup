# Developing ntexup Extensions

How to add **new functionality** to ntexup with a Java extension module:

- a new **component / node type** (e.g. a custom shape),
- a new **function** (usable in TSON expressions),
- a new **inline markup flavor** (e.g. `[[eq: ... ]]`),
- a new **output renderer** (e.g. an image format + factory).

It also covers **how extensions must share code** (common libraries, never chained deps) and the **common properties** every node supports.

Prerequisite reading: [01-repository-architecture.md](01-repository-architecture.md) and [02-engine-pipeline.md](02-engine-pipeline.md).

---

## 1. Extension concepts in one paragraph

A document declares `import("plantuml")`. At compile time the import becomes an `NDependency`, the engine **adds the extension jar to its classloader** (resolving it from the Nuts repository), then every service registry **rescans** and picks up the extension's components. Components are registered with plain **Java SPI service files** (`META-INF/services/...`) under `src/main/resources`. The three core extension points are:

| SPI file (`META-INF/services/…`) | Interface | Adds |
|---|---|---|
| `net.thevpc.ntexup.api.extension.NTxNodeBuilder` | `NTxNodeBuilder` | **node types** (a parser + renderer + optional text-flavor are derived automatically) |
| `net.thevpc.ntexup.api.extension.NTxFunction` | `NTxFunction` | **functions** callable from TSON |
| `net.thevpc.ntexup.api.renderer.text.NTxTextRendererFlavor` | `NTxTextRendererFlavor` | **inline markup flavors** (standalone; the builder path often covers this too) |

Secondary extension points:

| SPI file | Interface | Adds |
|---|---|---|
| `net.thevpc.ntexup.api.renderer.NTxNodeRenderer` | `NTxNodeRenderer` | a renderer for an existing node type (rarely needed — builders create these) |
| `net.thevpc.ntexup.api.parser.NTxNodeParser` | `NTxNodeParser` | a parser for an existing type (rarely needed) |
| `net.thevpc.ntexup.api.renderer.NTxImageTypeRendererFactory` | `NTxImageTypeRendererFactory` | a new **image format** for `image()`/drawImage |
| `net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactory` | `NTxDocumentRendererFactory` | a new **output format** (like the `pdf`/`html`/`screen` renderer modules) |
| `net.thevpc.nuts.spi.NComponent` | `NComponent` | generic Nuts components (the `shapes3d` module registers its 3D element parsers this way) |

> **Important:** the modern, recommended way is `NTxNodeBuilder`. When you use it you **do not** write separate `NTxNodeParser`/`NTxNodeRenderer` files — the engine materializes them from your builder (see §4 and §8).

---

## 2. Module skeleton

Create a Maven module (copy `tutorials/ntexup-extension-tutorial-myshape/` for a working reference).

```
extensions/ntexup-extension-<name>/
├── pom.xml
└── src/main/
    ├── java/net/thevpc/ntexup/extension/<name>/...
    └── resources/META-INF/services/          <- SPI files go here
```

`pom.xml` essentials:

```xml
<parent>
    <groupId>net.thevpc.xprojects.builders</groupId>
    <artifactId>ntexup-builder</artifactId>
    <version>1.0.0.0</version>
    <relativePath>../../pom.xml</relativePath>
</parent>
<groupId>net.thevpc.ntexup</groupId>
<artifactId>ntexup-extension-myfeature</artifactId>
<version>1.0.0.0</version>
<dependencies>
    <!-- ONLY the API. Never another extension. -->
    <dependency>
        <groupId>net.thevpc.ntexup</groupId>
        <artifactId>ntexup-api</artifactId>
        <version>1.0.0.0</version>
    </dependency>
    <!-- + third-party libs you need (plantuml, jfreechart, ...) -->
</dependencies>
```

Then:
1. Add `<module>extensions/ntexup-extension-myfeature</module>` to the root `pom.xml`.
2. Write the Java class(es) + SPI service file(s).
3. `mvn install`, then `nuts install net.thevpc.ntexup:ntexup-extension-myfeature` so `import("myfeature")` can resolve it.
4. Use it from a document: `import("myfeature")`.

> Only `ntexup-extension-shapes3d` additionally depends on `ntexup-engine` — it is the exception, and it still does not depend on any *other extension*.

---

## 3. How discovery works (base vs custom, reload on import)

`NtxServiceListImpl2` (`engine/impl/NtxServiceListImpl2.java`) keeps two buckets:

- **BASE** — services bundled in `ntexup-engine` (loaded once at engine init).
- **CUSTOM** — services from imported extensions every time an `import()` loads new jars.

On `import()` the engine calls `engine.importDependencies(...)`:
- add jars to the engine's `NMutableClassLoader`,
- fire `NTxDependencyLoadedListener` to every registry,
- each registry rescans `engine.loadServices(type)` over the enriched classloader and registers what's new.

Rules / gotchas:
- A custom service whose `id` collides with a base service is **silently rejected** (base wins).
- Node builders fan out into three registrations via `NTxNodeBuilderList.onAfterNewService` (`engine/impl/NTxNodeBuilderList.java`): the parser, the renderer, and (if `renderText()` was used) the text flavor.
- Lookups are by id or alias: `NTxEngine.getRenderer(type)`, `findFunction(name)`, `textRendererFlavor(id)`, `nodeTypeParser(id)`.

---

## 4. Adding a node type — `NTxNodeBuilder` (recommended)

### 4.1 The interface

```java
public interface NTxNodeBuilder extends NComponent {
    void build(NTxNodeBuilderContext builderContext);
}
```

### 4.2 The builder DSL (`NTxNodeBuilderContext`)

Declarative registration; see `core/ntexup-api/.../api/engine/NTxNodeBuilderContext.java` for the full surface:

**Identity**
- `id("my-shape")` — node type name used in `.ntx`
- `.ids(...)`, `.alias("...")` — extra names (aliases reachable from `.idAndAliases()`)

**Parameter (argument) rules** — `parseParam()` returns a `NamedParamAction`:

| Method | Meaning |
|---|---|
| `.matchesNamedPair("w","h","base","hat")` | accept `name: value` named pairs |
| `.matchesString()` / `.matchesName()` / `.matchesStringOrName()` | accept bare string / name literals |
| `.matchesAny()` / `.matchesAnyNonPair()` | accept anything / anything that is not a pair |
| `.matches(Predicate<NTxArgumentReader>)` | fully custom predicate |
| `.matchesMissingProperties(...)` | accept pairs whose names are not yet stored |
| `.storeName("prop")` | store arg under a different property name |
| `.storeFirstMissingName("value", ...)` | store the first bare arg into the first unset prop (used by `eq("x^2")` to set `value`) |
| `.store(StoreAction)` / `.resolvedAs(PropResolver)` | custom storage / custom prop building |
| `.asFlags()` / `.matchesNonCommonFlags()` | treat names as boolean flags / opt out of auto common-prop handling |
| `.ignoreDuplicates()` | tolerate repeated same-name pairs |
| `.matchesLeading(true)` | consume leading bare args before the first named pair |
| `.then()` / `.end()` | finish this rule chain |

**Rendering & layout hooks**
- `.renderComponent(this::render)` — `RenderAction.renderMain(NTxRendererContext)`
- `.renderText()` — inline text markup for this flavor (see §6) → returns a `RenderTextAction` with `.buildText(...)`, `.parseTokens(...)`, `.startSeparators(...)`
- `.renderConvert(RenderConvertAction)` — a converter node (not drawn itself, substitutes a child node)
- `.sizeRequirements(action)`, `.selfBounds2D(action)`, `.selfBounds3D(action)` — layout/lazy size computation
- `.processChildren(action)` — hook child processing
- `.compileNode(action)` — hook compilation of this node
- `.initializeNodeAction(action)` — init the node after creation (e.g. set `PRESERVE_ASPECT_RATIO`)
- `.parseDefaultParams()` — parse all args as common style props
- `.parseAny(NTxItemSpecialParser)` / `.parseAny(Predicate)` — fallback parsing of elements that are not the main call
- `.withToElem(String... props)` — TSON round-trip support (node → text, useful for `dump`)

### 4.3 What the engine generates from a builder

`NTxNodeBuilderContextImpl` (`engine/ext/NTxNodeBuilderContextImpl.java`) turns your builder into three services:

- `createParser()` → `CustomNTxNodeParserFromBuilder` (extends `NTxNodeParserBase`). Its `processArgument` feeds each arg through your `NamedParamAction`s, then **falls back to common-style-property handling**, so `width`, `color`, `background`, `at`, ... behave like every other node even if you did not declare them.
- `createRenderer()` → `NTxNodeRendererAsDefault` (delegates `sizeRequirements`/`selfBounds2D/3D`/`renderMain`) or `NTxNodeRendererAsText` / `NTxNodeRendererAsConverter` depending on which hook you used.
- `createTextFlavor()` → `CustomNTxTextRendererFlavorFromBuilder` (only when `renderText()` used).

Generic rendering machinery (rotation, shadow, visibility, debug box) is applied for free by `NTxNodeRendererBase` — your `renderMain` only draws the actual content.

### 4.4 Minimal component (reference tutorial)

`tutorials/ntexup-extension-tutorial-myshape/NTxMyShapeBuilder.java`:

```java
public class NTxMyShapeBuilder implements NTxNodeBuilder {
    @Override
    public void build(NTxNodeBuilderContext builderContext) {
        builderContext
            .id("my-shape")
            .parseParam().matchesNamedPair(NTxPropName.WIDTH, NTxPropName.HEIGHT, "base", "hat").end()
            .renderComponent(this::render);
    }

    public void render(NTxRendererContext rendererContext) {
        NTxNode node = rendererContext.node();
        NTxBounds2D b = rendererContext.selfBounds2D();        // computed layout box
        NTxPoint2D base = NTxValue.of(node.getPropertyValue("base")).asPoint2DOrDouble().orNull();
        // ... build an AWT GeneralPath from b, base, hat ...
        NTxGraphics g = rendererContext.graphics();
        g.draw(arrow);          // contour
        g.fill(arrow);          // fill
        rendererContext.drawContour();   // honor draw-contour: true/false
    }
}
```

Register it:

```
src/main/resources/META-INF/services/net.thevpc.ntexup.api.extension.NTxNodeBuilder
net.thevpc.ntexup.extension.tutorials.myshape.NTxMyShapeBuilder
```

Use it:

```tson
import("myshape")
my-shape(width: 100, height: 50, base: (80, 20), hat: (-1, -1))
```

### 4.5 A richer reference: `NTxCircleBuilder`

`extensions/ntexup-extension-shapes2d/.../shape/NTxCircleBuilder.java` shows:

```java
builderContext
    .id(NTxNodeType.CIRCLE)
    .renderComponent(this::renderMain)
    .initializeNodeAction(this::initializeNode);   // PRESERVE_ASPECT_RATIO = true
```

and render-time usage of the paint APIs:

```java
boolean bg = rendererContext.applyBackgroundColor();
Paint stroke = rendererContext.getLineColor(true);
rendererContext.applyContourColor(!bg);
rendererContext.withStroke(() -> { g.drawOval(...); });
rendererContext.drawContour();
```

### 4.6 The renderer context API cheat-sheet (`NTxRendererContext`)

What a `renderMain(NTxRendererContext)` sees:

| Group | Methods |
|---|---|
| Bounds | `selfBounds2D()`, `globalBounds2D()`, `parentBounds2D()`, `realBounds2D()`, 3D variants, `defaultSelfBounds2D()` |
| Capabilities | `CAPABILITY_PRINT`/"print", `CAPABILITY_ANIMATE`/"animate", `isPrint()`, `isAnimate()`, `getCapability/hasCapability/isCapability` |
| Dry runs | `isDry()` (measure-only pass — **do not cache inside dry runs**), `dryMode()` |
| Drawing | `graphics()` → `NTxGraphics`: `g.draw(Shape)`, `g.fill(Shape)`, `g.drawOval/drawRect/drawLine/drawArc`, `g.drawImage(NPath,...)`, `g.drawString(...)`, `createShape(NElement)`, transforms (`rotate/scale/setTransform`), color/paint/stroke/composite setters |
| Font | `getFont()`, `getFontFamily()`, `getFontSize()`, `isFontBold/Italic/Underlined/Strike`, `applyFont()` |
| Paint | `getForegroundColor(force)`, `getLineColor(force)`, `getColorProperty(name)`, `resolveBackgroundColor()`, `resolveStroke()`, `applyBackgroundColor()`, `applyContourColor(force)`, `applyForeground(force)`, `paintBackground(bounds)`, `paintBorderLine(bounds)`, `withStroke(Runnable)`, `drawContour()` |
| Structure | `isDrawContour()`, `isVisible()`, `getOrigin(...)`, `getPosition(...)`, `getColSpan()/getRowSpan()`, `getColumns()/getRows()`, `getRoundCornerArcs()`, `readStyleAsShadow(...)` |
| Composition | `isSomeChange()`, `withDefaultStyles(props)`, `withBuilderContext(...)`, `resolveNode(node, parentBounds)`, `pushNode/node/popNode`, `renderDetachedNode(child, relBounds)`, `compiledPage()`, `compiledDocument()`, `render()`, `sizeRequirementsOf()` |
| Values | `computePropertyValue(name, synonyms)`, `computeProperties()`, `NTxValue` coercions |

### 4.7 Value coercion helper — `NTxValue`

`NTxValue.of(node.getPropertyValue("base"))` then `.asPoint2DOrDouble()`, `.asDouble()`, `.asBoolean()`, `.asStringOrName()`, `.asInt()` is the canonical way to read raw `NElement` property values. `NTxValueByName` provides higher-level accessors (`getPadding`, `getFontInfo`, `getRotation`, `isVisible`, `selfBounds2D(size, ...)`) used by every builder.

---

## 5. Adding a function — `NTxFunction`

### 5.1 Interface

```java
public interface NTxFunction {
    String name();                                   // id used in TSON
    NElement invoke(NTxFunctionCallContext args);    // return a NElement (TSON value)
}
```

### 5.2 Calling context (`NTxFunctionCallContext`)

- `size()`, `arg(int)`, `argExpression(int)`, `args()` (lazy `NTxFunctionArg` with `src()`/`eval()`)
- `eval()` — evaluate all non-name args; `evalArg(int)`; `evalArg(int, converter, name, whenError)` — typed coercion
- `checkTooFewArgs(min)` / `checkTooManyArgs(max)`
- `scopedContext()` — the resolution context (to read/set vars, resolve paths)
- logging: `log(NMsg, NTxSource)`

### 5.3 Example — the built-in `either` (`engine/base/functions/general/NTxFunctionEither.java`)

```java
public NTxFunctionEither {
    public String name() { return "either"; }
    public NElement invoke(NTxFunctionCallContext args) {
        NElement last = null;
        for (int i = 0; i < args.size(); i++) {
            NElement u = args.evalArg(i);
            last = u;
            if (i < args.size() - 1 && u.isName() && u.equals(args.arg(i).src())) continue; // unresolved var → skip
            if (!NBlankable.isBlank(u)) return u;
        }
        return last != null ? last : NElement.ofString("");
    }
}
```

Register with:

```
src/main/resources/META-INF/services/net.thevpc.ntexup.api.extension.NTxFunction
net.thevpc.ntexup.extension.commonfunctions.colors.NTxFunctionInvertColor
net.thevpc.ntexup.extension.commonfunctions.colors.NTxFunctionColorBrightness
...
```

Use:

```tson
x := invertColor(documentColors[2])
y := darker(#445566, 0.3)
```

### 5.4 Function vs control-flow

Pure value functions return an `NElement`. **Control-flow** constructs (`include`, `import`, `for`, `if`, `define`, `:=`) are not `NTxFunction`s — they are handled as control nodes by the compiler (`NTxCompiler`). Do not try to re-implement them as functions.

---

## 6. Adding an inline text flavor (`[[flavor: ...]]`)

Two routed ways:

### 6.1 (Modern) via `renderText()` in a node builder

The `latex` extension does exactly this in `NTxEquationBuilder`:

```java
builderContext
    .id(NTxNodeType.EQUATION)              // "eq"
    .alias("equation")
    .parseParam().matchesNamedPair(NTxPropName.VALUE, NTxPropName.FILE).then()
    .parseParam().matchesAnyNonPair().storeFirstMissingName(NTxPropName.VALUE).then()
    .renderComponent(this::renderMain)
    .renderText()
        .buildText(this::buildText)          // emit rich tokens for the given text
        .parseTokens(this::parseTokens)      // tokenizer for the embedded flavor
        .startSeparators("\\(")              // extra inline triggers: \( ... \)
        .end()
    .sizeRequirements(this::sizeRequirements)
    .selfBounds2D(this::selfBounds);
```

`renderText()` → `MyRenderTextAction` holds the `buildText`/`parseTokens`/`startSeparators`; `createTextFlavor()` wraps it as `CustomNTxTextRendererFlavorFromBuilder` with:
- `type()` = builder id, `getParsePrefixes()` = `[[ <id>:`, `[[<alias>:` plus your extra separators
- `parseTokens()` = your callback (or `parseDefault(idAndAliases(), new String[0], null)`)

### 6.2 (Standalone) via `NTxTextRendererFlavor` SPI + `buildText`

```java
public interface NTxTextRendererFlavor {
    String type();
    default List<String> aliases() { return Collections.emptyList(); }
    void buildText(String text, NTxTextOptions options, NTxRendererContext ctx, NTxTextRendererBuilder builder);
    List<String> getParsePrefixes();
    List<NTxTextToken> parseTokens(NTxTextRendererFlavorParseContext ctx);
}
```

`parseTokens(NTxTextRendererFlavorParseContext)` has a mini DFA: `hasNext/len/peek/read/skip` + `parseDefault(flavorIds, startEndDelimiters, converter)`. Tokens go into `NTxRichTextToken`; an `IMAGE_PAINTER` token can embed a raster (that's how inline LaTeX is a picture inside running text). See `extensions/ntexup-extension-latex/.../eq/NTxTextRendererFlavorLatexEquation.java`.

### 6.3 How text parsing works

`NTxTextRendererBase` (engine) finds the longest matching `getParsePrefixes()` across all registered flavors at a position, calls the flavor's `parseTokens`, and your tokens are rendered between the default plain-text segments. Both the block form (`eq("...")`) and the inline form (`text("quadratic is [[eq: x^2]]")`) end up rendering through rich tokens.

---

## 7. How extensions must share code — **common libs, never chained deps**

This is a hard design rule of the project.

**Rule:** an extension must **never** depend on another extension to function. If two (or more) extensions — or an extension and the engine — need the same capability, extract it into a **neutral common library** under `lib/` (or, if it must be engine-close, under `core/`), and have the consumers depend on that common lib.

Ground truth in this repo:

| Extensions that need | Shared in | Depending on it |
|---|---|---|
| 2D geometry (JTS `NtxElement2D` factory, regions, primitives) | `lib/ntexup-lib-geometry2d` | engine + `ntexup-extension-shapes2d` |
| 3D geometry (meshes, `NTxCamera3DImpl`, `NtxGraphics3DImpl`, element-3D renderers) | `lib/ntexup-lib-geometry3d` | `ntexup-extension-shapes3d` + engine default imports |

And the negative list — cross-extension `pom.xml` facts (verified):
- `animated-gif` depends only on `ntexup-api` + `animated-gif-lib`
- `common-functions` → `ntexup-api` only (pure)
- `latex` → `ntexup-api` + jlatexmath
- `plantuml` → `ntexup-api` + plantuml + guava
- `plot2d` → `ntexup-api` + jfreechart
- `presenters` → `ntexup-api` only (pure Swing drawing)
- `shapes2d` → `ntexup-api` + `ntexup-lib-geometry2d`
- `shapes3d` → `ntexup-engine` + `ntexup-lib-geometry3d` (the one exception, still no *extension* dep)
- `svg` → `ntexup-api` + svg-salamander

Rationale:
1. **No hidden runtime ordering.** Extension A must not silently need extension B to be `import()`ed first — import order in a deck should never matter functionally.
2. **One source of truth.** A bug in shared geometry is fixed once, not patched in three extensions.
3. **Incremental loading.** `import()` resolves each dependency independently; common-lib consumers load cleanly even when the other extension isn't installed.
4. **Reactor hygiene.** The Maven reactor stays acyclic and buildable bottom-up (api → libs → engine → extensions).

If you find yourself adding `net.thevpc.ntexup:ntexup-extension-...` to your `pom.xml`, **stop** — that is the design smell. Extract a `lib/ntexup-lib-...` (or `core/`) artifact instead and add it to the root `<modules>`.

---

## 8. Registering a parser and renderer explicitly (low-level path)

You normally get this for free from a builder (§4.3). When you truly need hand-written ones:

### 8.1 Parser — `NTxNodeParser` (+ base class)

```java
public interface NTxNodeParser {
    void init(NTxEngine engine);
    String id();
    String[] aliases();
    boolean isContainer();
    NScoredCallable<NTxItem> parseNode(NTxResolutionContext context);
    void compileNode(NTxNode node, NTxResolutionContext context);   // optional
    NElement toElement(NTxNode node, boolean semantic, NTxEngine engine); // optional
    // + newNode, validateNode
}
```

If you extend `NTxNodeParserBase` (`engine/parser/NTxNodeParserBase.java`) you get common-argument handling (every common style prop auto-consumed into node properties) and the standard lifecycle for free — **this is exactly how the builder-derived parser behaves**. Register the fully-qualified class name in `src/main/resources/META-INF/services/net.thevpc.ntexup.api.parser.NTxNodeParser`, one per line (see the engine's parser SPI file for the 13 base parsers).

### 8.2 Renderer — `NTxNodeRenderer`

```java
public interface NTxNodeRenderer {
    NTxSizeRequirements sizeRequirements(NTxRendererContext ctx);
    NTxBounds2D selfBounds2D(NTxRendererContext ctx);
    NTxBounds3D selfBounds3D(NTxRendererContext ctx);
    void render(NTxRendererContext rendererContext);
    String[] types();          // node types you render
}
```

Extend `NTxNodeRendererBase` to inherit the common pipeline (rotation, shadow, debug box). Register in `.../services/net.thevpc.ntexup.api.renderer.NTxNodeRenderer`. The engine's own renderers (`page`, `fragment`, `import`, `assign`, `define`, `block`) are the reference.

> Heads-up: some extensions ship **empty placeholder SPI files** (`plantuml`, `plot2d`, `presenters`, `latex`, and a `NTxTextRendererFlavor` file) — these are leftovers of the old design; the real registrations come from their builders. Don't copy that pattern; prefer the builder path.

### 8.3 Output renderer — `NTxDocumentRendererFactory`

To add a new export format (`renderers/` modules):

```java
public interface NTxDocumentRendererFactory {
    NScoredCallable<NTxDocumentRenderer> createDocumentRenderer(NTxDocumentRendererFactoryContext context);
}
```

Factory context gives you `rendererType()` and `engine()`. Implement `NTxDocumentStreamRenderer` (write to `OutputStream`/`NPath`, with `NTxDocumentStreamRendererConfig`: page size, `gridX`/`gridY`, orientation `NTxPageOrientation`, margins, `showPageNumber/showFileName/showDate`) or `NTxDocumentScreenRenderer` (a Swing view). Register under `.../services/net.thevpc.ntexup.api.renderer.NTxDocumentRendererFactory`; then `engine.newRenderer("myfmt")` finds it. Reference: `renderers/ntexup-renderer-pdf`.

### 8.4 Image type factory — `NTxImageTypeRendererFactory`

Handles a new raster/vector file type for `image()` and `drawImage(...)`, resolved by extension from the file path. Reference: `ntexup-extension-svg`, `ntexup-extension-animated-gif`.

---

## 9. Common properties (shared by every element)

Two sources of truth:

1. `NTxStyleParser` (`engine/parser/NTxStyleParser.java`) — the working sets used at parse time:
   - `COMMON_STYLE_PROPS` — stroke, shadow, position, origin, size, name, columns, rows, colspan, col-span, rowspan, row-span, colweight, col-weight, rowweight, row-weight, grid-color, line-color, rotate, padding, margin, font-size, debug, debug-color, font-family, raised, font-bold, font-italic, font-underlined, font-strike, background-color, foreground-color, fill-background, disabled, draw-grid, columns-weight, rows-weight, preserve-aspect-ratio, threed, draw-contour, class, at, component-name, content-origin, content-position, align, width, height, text-wrap, wrap, text-align, text-halign, bullet-align + per-text-style-token colors (`source-<id>-color`, ...) + aliases (color, background, foreground, bg, fg, show, visible, fill, contour, w, h)
   - `COMMON_FLAG_STYLE_PROPS` — shadow, debug, raised, font-bold, font-italic, font-underlined, font-strike, fill-background, disabled, draw-grid, preserve-aspect-ratio, threed, draw-contour (+ show/visible/fill/contour)
   - `NTxParserUtils.isCommonStyleProperty(...)` / `isCommonStyleFlagProperty(...)` match these (normalized via `uid()`).
2. `NTxPropName` (`api/document/style/NTxPropName.java`) — canonical constants: geometry/layout (`WIDTH`, `HEIGHT`, `POSITION`, `SIZE`, `ORIGIN`, `AT`, `PADDING`, `MARGIN`, `ROTATE`, `COLSPAN/ROWSPAN`, `COLUMNS/ROWS`, `COL_WEIGHT/ROW_WEIGHT`, ...), colors/strokes (`COLOR`, `BACKGROUND_COLOR`, `LINE_COLOR`, `CONTOUR_COLOR`, `STROKE`, `DASH`, `SHADOW`, `DRAW_CONTOUR`, `ROUND_CORNER`, ...), text (`FONT_FAMILY/SIZE/BOLD/ITALIC/...`), shapes (`INNER_RADIUS`, `START_ANGLE`, `EXTENT_ANGLE`, `START_ARROW`, `END_ARROW`, `PRESERVE_ASPECT_RATIO`, ...), ctrl (`NAME`, `VALUE`, `FILE`, `LANG`, `FROM`, `TO`, `DEBUG`...).

Behavior: any named pair or bare name whose key is a common style prop is **automatically consumed and stored as a node property** by `NTxNodeParserBase.defaultProcessArgument` — including when your builder uses the fallback (`CustomNTxNodeParserFromBuilder`). Bare flags become `true`. So `rectangle(color: red, draw-contour, at: center)` works without any special parsing in your builder.

> Builder tip: when you declare `.matchesNamedPair(...)` for your *own* props, remaining common props still fall through to the default handler. Use `.matchesNonCommonFlags()` / `asFlags()` to control flag-like parsing, and `NTxValue` to coerce the stored `NElement`.

---

## 10. End-to-end checklist for a new node type

Assuming a new `my-shape` element:

1. `tutorials/...` style module under `extensions/ntexup-extension-myshape`, parent `ntexup-builder`, dep `ntexup-api`.
2. `NTxMyShapeBuilder implements NTxNodeBuilder` — `.id("my-shape")` + param rules + `.renderComponent(this::render)`.
3. `META-INF/services/net.thevpc.ntexup.api.extension.NTxNodeBuilder` listing it.
4. Root `pom.xml` `+ <module>extensions/ntexup-extension-myshape</module>`.
5. `mvn install && nuts install net.thevpc.ntexup:ntexup-extension-myshape`.
6. Document: `import("myshape")` then `<my-shape>(...)`.
7. Add a sample deck to `test/ntexup-examples` and render it to PDF (see [04-programmatic-api-testing.md](04-programmatic-api-testing.md)) to verify.

---

## 11. Related

- Node-type constants: `api/document/node/NTxNodeType.java`.
- Full user-facing property list: `documentation/user-guide/26-property-reference.md`.
- Node-type reference: `documentation/user-guide/27-node-type-reference.md`.
- Names & aliases of every builder in the repo are listed in each extension's SPI service file (see §3.6 of [01-repository-architecture.md](01-repository-architecture.md)).
- Isolation/templating semantics you must respect when your node is a container: `documentation/specifications/isolation.md`, `documentation/user-guide/19-components-reuse.md`.