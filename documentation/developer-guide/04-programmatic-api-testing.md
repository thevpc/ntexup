# Programmatic API, Rendering & Automated PDF Testing

How to drive the engine from Java code and how to **test automatically by rendering a PDF** (and/or page images). This is the recipe that `test/ntexup-examples` uses and that you should build your CI/test harness on.

---

## 1. The programmatic engine API in five lines

```java
import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.io.NPath;

Nuts.openWorkspace().share();                          // 1. Nuts must be initialized
NTxEngine e = new DefaultNTxEngine();                  // 2. engine (loads base services)
NTxCompiledDocument doc = e.loadDocument(NPath.of("slides.ntx"));  // 3. parse + bootstrap
NTxDocumentStreamRenderer pdf = e.newPdfRenderer().get();          // 4. "pdf" renderer
pdf.setOutput(NPath.of("out.pdf"));
pdf.render(doc);                                       // 5. lazy per-page render
```

That's the whole skeleton — it is exactly what `test/ntexup-examples/.../TestPdf.java` and `NTxExampleFromFile1.java` do. `Nuts.openWorkspace()` is required because renderer/extension resolution rides on the Nuts workspace + repository.

### 1.1 Loading variants

```java
// a single file or a whole folder (folder: *.ntx scanned, main.ntx first)
NTxCompiledDocument a = e.loadDocument(NPath.of("my-project"));     // folder
NTxCompiledDocument b = e.loadDocument(NPath.of("main.ntx"));       // file

// in-memory source
NTxCompiledDocument c = e.loadDocument(new ByteArrayInputStream(
    "page { text(\"hi\") }".getBytes()));

// remote github://
NTxCompiledDocument d = e.loadDocument(NPath.of("github://thevpc/ntexup-templates/classic/v1.0/theme"));
```

`NPath` is the Nuts path abstraction (`net.thevpc.nuts.io.NPath`) — it already understands absolute/relative paths, `~` (`NPath.ofUserHome()`), and remote forms.

### 1.2 Renderer acquisition (all on `NTxEngine`)

| Call | Returns | Notes |
|---|---|---|
| `e.newPdfRenderer()` | `NOptional<NTxDocumentStreamRenderer>` | OpenPDF-based, raster-first |
| `e.newHtmlRenderer()` | stream renderer | pages to `images/`, `.zip`/stream supported |
| `e.newScreenRenderer()` | `NTxDocumentScreenRenderer` | Swing viewer, needs a display |
| `e.newStreamRenderer("pdf")` | generic by type name | |
| `e.newRenderer(type)` | `NOptional<NTxDocumentRenderer>` | any registered type |
| `e.renderImageBytes(page, config)` | `byte[]` (PNG) | page → image, headless-safe |
| `e.renderImage(page, config)` | `BufferedImage` | ||

`NOptional` is Nuts' optional type — call `.get()` to unwrap.

### 1.3 Stream renderer config (`NTxDocumentStreamRendererConfig`)

```java
renderer.setStreamRendererConfig(new NTxDocumentStreamRendererConfig()
    .setPageWidth(1024).setPageHeight(768)      // default = A4
    .setOrientation(NTxPageOrientation.LANDSCAPE)
    .setGridX(1).setGridY(1)                     // slides per PDF page (e.g. 1x2)
    .setShowPageNumber(true).setShowFileName(true));
```

The engine normalizes config through `NTxEngineTools.validateDocumentStreamRendererConfig`.

---

## 2. Automated testing by rendering a PDF

The project currently has **no JUnit**; the "test suite" is sample decks + `main()` programs. Two complementary strategies below, both headless-friendly (no display needed for PDF/PNG).

### 2.1 Strategy A — "render everything" smoke test (fastest to adopt)

Mirror the file list in `test-commands.md` (`generate /path/to/deck`) as a Java batch program:

```java
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.engine.impl.NTxCompiledDocumentImpl;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.io.NPath;

Nuts.openWorkspace().share();
NTxEngine e = new DefaultNTxEngine();
List<NPath> decks = List.of(
    NPath.of("test/ntexup-examples/src/ntexup/examples/curr"),
    NPath.of("test/ntexup-examples/src/ntexup/simple-presentation-ibtihel"),
    NPath.of("ntexup-doc-slides")
);
for (NPath deck : decks) {
    NTxCompiledDocument doc = e.loadDocument(deck);
    if (!((NTxCompiledDocumentImpl) doc).isSuccessfullyLoaded()) {
        throw new AssertionError("failed to load " + deck);
    }
    NTxDocumentStreamRenderer pdf = e.newPdfRenderer().get();
    pdf.setOutput(deck.resolve("target/output.pdf"));
    pdf.render(doc);                                   // throws on render errors
    System.out.println("OK " + deck + " (" + doc.pages().size() + " pages)");
}
```

Assertions you can add cheaply:
- `((NTxCompiledDocumentImpl) doc).isSuccessfullyLoaded()` (a `SilentNTxLogger` decides this; a parse/compile error flips it to false),
- page count within a sane range,
- output PDF exists and is non-empty (`NPath.size()` > 0).

This catches: TSON syntax errors, unknown node types, missing includes, unresolvable extensions.

### 2.2 Strategy B — pixel-level golden image checks (catches rendering regressions)

Render each page of a reference `.ntx` to a PNG once ("golden"), store it in `src/test/resources/golden/`, then on every run re-render and compare (hash, or simple diff threshold).

```java
NTxCompiledPage p = doc.page(0);
byte[] png = e.renderImageBytes(p, new NTxNodeRendererConfig(1024, 768)
        .withAnimate(false).withPrint(true));
```

Comparing:
- cheap: `MessageDigest.getInstance("SHA-256")` over the PNG bytes;
- robust: decode with `ImageIO` and compute normalized Mean-Absolute-Error; threshold ~0 avoids CI flakes from font antialiasing.

To regenerate goldens after an intentional change, flip a system property (`-Dgolden.regenerate=true`).

> Caveat: PDF output itself embeds rasterized pages, so the golden target should be **the rendered page image**, not the PDF bytes (PDF byte layout may vary with OpenPDF/date/footer settings).

### 2.3 Strategy C — CLI-driven checks (no Java test harness at all)

The CLI already does the same work:

```bash
nuts ntexup generate tests/deck-a          # → deck-a.pdf (PDF sign-off build)
nuts ntexup show tests/deck-a              # needs a display (manual)
```

Wrap these in a CI script iterating `test-commands.md`'s list; fail if a deck produces no PDF. This is the path of least resistance for a quick nightly smoke test.

### 2.4 Interactive debug while developing a deck or extension

- `debug: true` on any node → the engine draws node bounds/IDs — great for layout debugging.
- `themeShowPalette = true` shows the 12 accent colors used by `documentColors[N]`.
- The desktop GUI (`nuts ntexup show .`) has `NTxDebugFrame` with live document/node/TSON panels.
- Engine env knobs: `warnPageCount` (1024) / `maxPageCount` (65536) — raise when a deck legitimately has many pages.

---

## 3. Headless environments (Docker/CI)

- **PDF and PNG rendering work headless** (no X11) — print rendering never opens a window.
- Only the **screen renderer** needs a display manager.
- Document it in your CI notes: the container must include `git` if any deck uses `github://` includes (the engine shells out to git for the cache clone).

Example Docker CI step:

```bash
docker run --rm eclipse-temurin:17-jre \
  bash -c "$(curl -sSL https://thevpc.net/nuts/bootstrap-container-latest.sh)
  && nuts -y install ntexup
  && git clone https://github.com/you/your-decks.git
  && cd your-decks && for d in tests/*; do nuts ntexup generate \"\$d\" || exit 1; done"
```

---

## 4. Pinning your test to a fixed engine version

Engine version is `NTxEngine.CURRENT_VERSION` (`"1.0.0.0"`). When running tests against a local build:

```bash
mvn install                                  # installs ntexup-api + engine into ~/.m2
nuts install net.thevpc.ntexup:ntexup-engine # makes import() resolution deterministic
```

An extension test deck that `import("myshape")`s needs `nuts install net.thevpc.ntexup:ntexup-extension-<name>` exactly as in [03-extension-development.md](03-extension-development.md) §2/§10.

---

## 5. A complete JUnit-ready class (copy me)

```java
package net.thevpc.ntexup.examples;

import net.thevpc.ntexup.api.engine.NTxCompiledDocument;
import net.thevpc.ntexup.api.engine.NTxEngine;
import net.thevpc.ntexup.engine.impl.DefaultNTxEngine;
import net.thevpc.ntexup.engine.impl.NTxCompiledDocumentImpl;
import net.thevpc.ntexup.api.renderer.NTxDocumentStreamRenderer;
import net.thevpc.nuts.Nuts;
import net.thevpc.nuts.io.NPath;

import static org.junit.jupiter.api.Assertions.*;

class RenderToPdfTest {
    private static NTxEngine engine;

    @BeforeAll
    static void init() {
        Nuts.openWorkspace().share();
        engine = new DefaultNTxEngine();
    }

    @Test
    void rendersDeckToPdf_withoutErrors() {
        NPath deck = NPath.of("test/ntexup-examples/src/ntexup/examples/curr");
        NTxCompiledDocument doc = engine.loadDocument(deck);
        assertTrue(((NTxCompiledDocumentImpl) doc).isSuccessfullyLoaded(), "deck must parse & compile");
        assertTrue(doc.pages().size() > 0, "deck must contain pages");

        NPath out = deck.resolve("target/out.pdf");
        NTxDocumentStreamRenderer pdf = engine.newPdfRenderer().get();
        pdf.setOutput(out);
        pdf.render(doc);
        assertTrue(out.exists() && out.size() > 0, "PDF must be produced");
    }
}
```

(Add JUnit 5 to the `test/ntexup-examples` pom — the reactor already has `maven-surefire-plugin` configured, so tests will just run on `mvn verify`.)