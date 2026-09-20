# Troubleshooting: Fixing ntexup Issues

A practical guide to diagnosing and fixing problems — at the **language** level (`.ntx`), the **build** level (Maven), the **engine** level, and the **extension** level. Each symptom lists likely causes and the fix, with pointers into the source.

---

## 1. Debugging toolkit (use these first)

| Tool | Purpose |
|------|---------|
| `nuts ntexup show .` | live viewer; the fastest way to see what actually parsed/compiled/renderered |
| `debug: true` (or `debug: 2`) on a node | draw bounds box around the node + its children — layout/geometry problems |
| `themeShowPalette = true` | visualize the 12 `documentColors[N]` |
| `nuts ntexup dump <file>` | CLI action `DUMP` — prints the node tree as TSON (`engine.toElement(doc, false)`), shows what the compiler produced |
| Render a single page to PNG (`engine.renderImageBytes(page, ...)`) | isolate a rendering error to one page |
| `NUTS_DEBUG=*` / higher Nuts log level | see dependency/extension resolution tries and failures |
| Desktop debug GUI | `NTxDebugFrame` (`core/ntexup-desktop/debug/`) with document/node/TSON panels |

Errors that come out of rendering are tagged with the **origin file** (the parser annotates every element with its source path — `DefaultNTxEngine`/`NTxDocStreamParser`), so a stack trace usually cites the offending `.ntx` file directly.

---

## 2. Common rendering / authoring problems

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `theme not found` / `eitherPath` error | Theme repo not reachable (no git, no network, bad cache) | Use the `github://thevpc/ntexup-templates/...` form (default in scaffolds), or point `alternativeLocalThemeRepository` at a valid clone; ensure `git` is installed in containers. |
| Equation renders blank / wrong | Over-escaped backslashes (JSON habit) | TSON is literal-first: write `\frac`, never `\\frac`. Drop backslashes first, build up. |
| `\n` shows literally | TSON does **not** process escape sequences | Press Enter for a real newline; don't rely on `\n`. |
| Windows path mangled `C:\...` | extra backslashes | Write the path as-is (`C:\Users\x`); no escaping. |
| Image not found | wrong relative path | Paths are relative to the **current `.ntx` file**. From `02-pages/...` you need `../../../images/foo.png`. |
| Colors look off | hardcoded hex instead of theme vars | Use `documentColors[N]`, `documentSurface`, `documentTextPrimary`, ... |
| Content clipped / huge | absolute pixels instead of `%P` | `%P` = % of parent; `font-size: 2.5%P`, sizes as percentages. |
| `font-bold` / `draw-contour` "unknown property" | **quoted** the bare flag instead of writing it bare | Write `font-bold`, not `"font-bold"` (they are TSON identifiers, presence = true). |
| My `@define` component not found | definition not seen before use | All `@define` go in `01-styles/*.ntx` (included before `02-pages`), or before the pages that use them. |
| Grid cells empty | wrong `grid(cols, rows)` count | `grid(C, R)` then fill C×R children row-by-row; missing children render empty. |
| `expecting }` / "unexpected character" | unbalanced braces / TSON syntax edge | Count braces; prefer `"""..."""` for LaTeX/code (no escaping inside). `¶`/`^` issues → fall back to `"""..."""`. |
| Unresolved var confusingly | you used `:=` where `=` was meant | `:=` sets only if unset; `=` forces. For user-facing vars in `main.ntx` use `=`. |

More authoring troubleshooting: `documentation/user-guide/29-troubleshooting.md`.

---

## 3. Build problems (Maven reactor)

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Build picks wrong Maven mirror | workstation-specific settings.xml | The repo pins its repo settings in `.mvn/`; run `mvn verify` from the repo root and don't rely on external mirrors. |
| Java version error | engine needs Java 17+ | Use JDK 17 (Temurin/OpenJDK); `maven.compiler.release` is 17. |
| Module not found / reactor cycle | forgot to add new module to root `pom.xml` | Add `<module>extensions/ntexup-extension-<x></module>` to `/pom.xml`. |
| Extension can't be `import()`ed | artifact not installed in Nuts | `mvn install` **then** `nuts install net.thevpc.ntexup:ntexup-extension-<x>`. |
| Compilation `cannot find symbol` in an extension | only depends on `ntexup-api` but uses engine internals | Keep it API-only. If you truly need engine classes, that's usually a sign the feature belongs in `core/`, not an extension (see `03-extension-development.md` §7). |
| Slow cold build | downloading from maven.thevpc.net | Expected on first build; afterwards artifacts are cached. |

---

## 4. Engine & extension problems

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| New node type "unknown" in deck | extension not loaded / not installed / import missing | Add `import("<your-ext>")` **before** usage; `nuts install net.thevpc.ntexup:ntexup-extension-<x>`; check `nuts ntexup dump` to confirm the node parsed. |
| Custom node id collides with a built-in | base service wins silently | Pick a unique id (`ntexup-registry` rejects duplicates silently, `NtxServiceListImpl2`). |
| `import()` succeeds but function still missing | dependency fingerprint / stale rescan | The registry rescans on load; if the jar was installed mid-run, restart the process/engine. |
| Extension loads but rendering throws `Segmentation/distance` weirdness | you read props with raw `getPropertyValue` instead of `NTxValue` | Use `NTxValue.of(...)` coercions; raw `NElement` values are still expression-form (`%P`, vars). |
| My builder's named params not accepted | declared `.matchesNamedPair` but args passed bare | Use `.matchesAnyNonPair().storeFirstMissingName(...)` for positional args (see `NTxEquationBuilder`). |
| Builder renders but ignore `at`/`size` | you didn't call `rendererContext.selfBounds2D()` consistently / forgot `sizeRequirements` | Implement `.sizeRequirements(...)` + `.selfBounds2D(...)` or rely on the default; read bounds from `selfBounds2D()`, not hardcoded coords. |
| Inline `[[eq: ...]]` not parsed | flavor not registered for the import | `renderText().startSeparators(...)` in the builder registers the prefix flavors; ensure `renderText()` was chained. |
| Old-style flavor file present but inert | legacy empty SPI files in some extensions | Register via the builder path (`createTextFlavor`), not the standalone flavor file. |
| GitHub include hangs | cached clone + rate-limit (5 min between pulls) | `resolveGithubPath.lastPull` throttle; force refresh by waiting or clearing the cache folder. |

---

## 5. Diagnosing lazy-compilation quirks (they look like bugs)

Deliberate architecture that can surprise you:

1. **Includes are compiled in source order, at the point of `include(...)`.** A component defined in a file included *after* a page that uses it won't exist when that page's **document-level** content runs. Rule: styles/components before pages (`main.ntx` ordering, `21-multi-file-includes.md`).
2. **Pages compile lazily on demand.** A syntax error inside `page{}` may only surface when the renderer actually reaches that page index — not at load time. `dump` forces nothing; rendering (or `renderImage` on the page) does. Check `successfullyLoaded()` (document-level) *and* actually walk all pages in tests.
3. **Fragment vs block vs group scoping.** Variables "leak" out of `fragment`, are trapped by `block`/`group`. If a variable behaves oddly, suspect the container kind: `documentation/specifications/isolation.md`.
4. **`import()` is global**, applied at compile time of the document — put it anywhere before needing the types; import order between different extensions should not matter by design (extensions never depend on each other).
5. **Raster-first rendering**: PDF/HTML are built from page PNGs. If something renders differently in PDF vs viewer, it's a PNG/AWT rasterization issue, not separate logic — compare the intermediate `renderImage` PNG first.

---

## 6. Nuts/repository issues

| Symptom | Fix |
|---------|-----|
| `repository template not found 'local'` warnings at scaffold time | Harmless; ntexup falls back to cloning `ntexup-templates` into the Nuts cache. |
| `ntexup: command not found` | Restart the terminal after installing Nuts (`curl -s https://thevpc.net/nuts/install-latest.sh | bash`). |
| Installed template missing / stale | Force template repo refresh (delete the cached clone under Nuts cache, or use a branch-pinned `github://` URL). |
| `Nuts.openWorkspace()` fails in headless CI | Ensure Nuts workspace init completes (writes `~/.nuts`); add `-y` for auto-confirm. |

---

## 7. Where the real errors are thrown (source map for stack traces)

| If the stack shows… | Look at |
|---|---|
| `NTxCompiler.compileNodeTree_*` | a control-structure bug at compile time (include/import/for/if/assign) |
| `CtrNTxNodeUncompiled` + `parseNode` | deferred parse failed for the raw TSON at that position |
| `NTxCompiledDocumentImpl.readMore` | page materialization (auto-pagination / boundary) issue |
| `NTxNodeRendererBase.render` / your `renderMain` | rendering of one node type (rotation/shadow wrapper around your code) |
| `NTxGraphicsImpl.drawImage` | image decoding/dispatch (SVG/GIF/PNG factory) failed |
| `NtxServiceListImpl2.*` | service registration/discovery (missing SPI file, duplicate id) |
| `NTxGitHelper` | GitHub include/clone resolution |
| `PdfDocumentRenderer` | PDF layout/embedding (grid, page numbering, image embedding) |
