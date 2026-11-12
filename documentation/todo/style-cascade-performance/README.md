# Task: Style cascade & render-performance overhaul

You are working on **ntexup** (Java 25, `NTx*` / `Ntx*` classes, TSON/NElement DSL).
This document is the single source of truth for the work. **Update the
[Progress log](#progress-log) after every phase and every benchmark run.**
Do not mark a phase done unless its **Gate** passes.

Work in **phases with gates**. Do not start phase N+1 before reporting phase N.
Do not guess about the codebase: read first, measure, then change.

---

## 0. Non-negotiables

1. **Every change is tested.** No behavioural change without a test that fails
   before and passes after.
2. **No visual regression.** Golden images and full-deck renders must be
   pixel-identical after every phase unless the phase explicitly says otherwise.
3. **One phase per commit/PR.** Never mix a refactor with a semantic change.
4. **Keep a legacy path while proving equivalence.** For phases 2–3, keep the
   old uncached resolver available behind `-Dntexup.style.legacy=true` until the
   equivalence test reports zero mismatches on both benchmark decks. Then delete it.
5. **Measure before and after** with the same harness, same deck, same JVM flags.
   Record both numbers in the progress log.
6. **Performance targets are gates**, not aspirations. If a gate fails, stop and
   report; do not paper over it.
7. Do not change public API signatures unless a phase requires it; prefer
   package-private/internal additions.

---

## 1. Objective

`ntexup show` is slow to open and slow to navigate. Investigation shows the
dominant cost is **not** page compilation and **not** the renderer primitives: it
is the **style/property cascade being recomputed for every property lookup, on
every render**, plus allocation churn around it.

Targets (cumulative, measured on `D2` below):

| Metric | Baseline | Target |
|---|---|---|
| first paint (`show`) | ~5.6 s | ≤ 4.2 s |
| cold render / page | 98 ms | ≤ 45 ms |
| warm render / page | 19.6 ms | ≤ 10 ms |
| cold full 440-page pass | 43.1 s | ≤ 20 s |
| warm full 440-page pass | 8.6 s | ≤ 4.5 s |
| pixel diff | — | 0 |

---

## 2. Benchmark decks & harness

Two decks, both used for every gate:

- **D1** — `ntexup-doc-slides` (46 pages, 2,403 nodes): small, fast sanity deck.
- **D2** — `/home/vpc/education-world/education/modules/antenna-and-propagation/ntx/antenna-and-propagation`
  (440 pages, 20,732 nodes, 29 vars + 58–91 definitions per page): the realistic
  primary deck.

### 2.1 Harness to port into the repo

The investigation used throw-away classes under `/tmp/opencode`; the harnesses
are now ported to
`test/ntexup-test/src/test/java/net/thevpc/ntexup/test/perf/` as `main`-based
classes (not JUnit, so CI does not pay for them):

- `NTxColdBreak` — startup phases (`Nuts.require`, engine ctor, parse,
  `document()`, `pages()`, first page render).
- `NTxColdOnly` — one cold render pass with a JFR recording (`cold.jfr`).
- `NTxWarmOnly` — warm the node caches, then one measured warm pass with JFR
  (`warm.jfr`).
- `NTxRenderAll` — per-page cold/warm/render timing + p50/p90/p99/max.
- `NTxRulesProbe` — pages, nodes, depth, rules on path, class usage.
- `NTxClassProbe` — class/rule structural stats.
- `NTxCtxProbe` — vars/definitions/functions per page context.
- `NTxGoldenCapture` — deterministic per-page PNGs, PDFs, and SHA-256 manifests
  for the two baseline decks.

The harness is run from the `ntexup` repository root. Build the test classes and
full runtime classpath once, then run a harness with the same classpath:

```bash
D1=/home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup-doc-slides/main.ntx
D2=/home/vpc/education-world/education/modules/antenna-and-propagation/ntx/antenna-and-propagation/main.ntx
mvn -q -pl test/ntexup-test -am -DskipTests test-compile
mvn -q -f app/ntexup-full/pom.xml dependency:build-classpath \
  -DincludeScope=runtime -Dmdep.outputFile=/tmp/ntexup-full-cp.txt
SOURCE_CLASSES="$(find . -type d -path '*/target/classes' -print | sort | paste -sd:)"
CP="test/ntexup-test/target/test-classes:$SOURCE_CLASSES:$(cat /tmp/ntexup-full-cp.txt)"
java -Dnuts.term=false -Xmx4g -cp "$CP" \
  net.thevpc.ntexup.test.perf.NTxRenderAll "$D2" 1920 1080
java -Dnuts.term=false -Xmx4g -cp "$CP" \
  net.thevpc.ntexup.test.perf.NTxColdBreak "$D2" 1920 1080
java -Dnuts.term=false -Xmx4g -cp "$CP" \
  net.thevpc.ntexup.test.perf.NTxColdOnly "$D2" 1920 1080 /tmp/D2-cold.jfr
java -Dnuts.term=false -Xmx4g -cp "$CP" \
  net.thevpc.ntexup.test.perf.NTxWarmOnly "$D2" 1920 1080 /tmp/D2-warm.jfr
java -Dnuts.term=false -Xmx4g -cp "$CP" \
  net.thevpc.ntexup.test.perf.NTxRulesProbe "$D2"
java -Dnuts.term=false -Xmx4g -cp "$CP" \
  net.thevpc.ntexup.test.perf.NTxClassProbe "$D2"
java -Dnuts.term=false -Xmx4g -cp "$CP" \
  net.thevpc.ntexup.test.perf.NTxCtxProbe "$D2"
```

Run those commands with `$D1` as well for the small-deck measurements. Capture
both baseline decks with:

```bash
java -Dnuts.term=false -Xmx4g -cp "$CP" \
  net.thevpc.ntexup.test.perf.NTxGoldenCapture "$D1" D1 \
  test/ntexup-test/src/test/resources/golden-cascade/D1 1920 1080
java -Dnuts.term=false -Xmx4g -cp "$CP" \
  net.thevpc.ntexup.test.perf.NTxGoldenCapture "$D2" D2 \
  test/ntexup-test/src/test/resources/golden-cascade/D2 1920 1080
```

### 2.2 JFR attribution recipe

```bash
/usr/lib64/jvm/java-25-openjdk-25/bin/jfr print \
  --events jdk.ExecutionSample --stack-depth 96 cold.jfr > samples-cold.txt
# attribute: responsible ntexup frame, leaf frame, "% containing <method>"
```

Record the top-10 responsible frames and the "% containing" list in the log.

---

## 3. Measured baseline (2026-11-11)

### 3.1 Startup — D2

| Phase | ms |
|---|---|
| `Nuts.require` | 548 |
| engine ctor | 14 |
| `loadDocument` (parse) | 93 |
| `document()` (compile + imports) | **3387** |
| `pages()` drain (440 pages) | **1424** |
| first page render | ~100 |
| **first paint** | **~5.6 s** |

Same engine with imports already resolved: `document()` = 422 ms → **~3.0 s of
startup is Nuts dependency resolution** (13 sequential `importDependencies` in the
eniso theme, O(N²) `NClassLoaderBase.buildInternal`).

### 3.2 Render — D2, 1920×1080

| Pass | total | avg/page | p50 | p90 | p99 | max |
|---|---|---|---|---|---|---|
| cold (`useCache=false`) | 43.1 s | **98 ms** | 84 | 169 | 256 | 803 |
| warm (`useCache=true`) | 8.6 s | **19.6 ms** | — | — | — | — |

### 3.3 Cold attribution (34,040 samples)

| share | where |
|---|---|
| **53.7 %** | stacks containing `NTxPropCalculator.computePropertyMagnitude` |
| 17.7 % | `DefaultNTxNodeSelector.acceptNode` (responsible) |
| **21.4 %** | `HashMap$HashIterator` leaf — 8.8 % inside `acceptNode`, 2.0 % in `getClassDefNames` |
| 10.7 % | `NTxUtils.uid` → `NNameFormat.format` + `toLowerCase` |
| 10.0 % | `NTxResolutionContextImpl.evalExpression` |
| 6.7 % | `getClassDefNames()` (new `HashSet` per call) |
| 4.7 % | `NTxProperties.get` (eager message + `NOptional`) |
| 4.2 % | `NTxUtils.uids` |
| 3.7 % | `NTxValue.asElementArray` |
| 3.1 % | `NTxResolutionContextImpl.<init>` |
| 2.7 % | `NMsg` construction (per-render logging) |
| 1.7 % | `addDependency` |

`NTxValueByName` appears in **82.8 %** of cold samples.

### 3.4 Warm attribution (6,574 samples)

| share | where |
|---|---|
| **35.9 %** | cascade still |
| 11.7 % | `acceptNode` |
| 11.7 % | plot2d resampling |
| 8.0 % | `evalExpression` |
| 7.7 % | `NTxResolutionContextImpl.<init>` |
| 7.4 % | `uid` |
| 6.5 % | `getClassDefNames` |
| 3.1 % | `NTxProperties.get` |
| 2.6 % | `uids` |

### 3.5 Why

- The three aggregate caches issue **25 property lookups per node**; with 20,732
  nodes that is **~518,000 cascade walks per pass**, each scanning ~5 ancestors
  × ~17 rules → **~44 million `acceptNode` calls per pass**.
- `DefaultNTxNodeSelector.items` is a `HashSet`; `acceptNode` allocates an
  iterator per call. `getClassDefNames()` allocates a new `HashSet` per rule.
- `NTxProperties.get(String...)` builds `"style " + Arrays.asList(names)` on
  **every hit**.
- `NTxUtils.uid` runs `NNameFormat.LOWER_KEBAB_CASE.format(strip(name))` on every
  lookup, although internal property names are already lower-kebab.
- `DefaultNTxRendererContext extends NTxResolutionContextImpl`; each node render
  copies **29 vars + 58–91 definitions** (`putAll`) and concatenates a `uid`
  string.

---

## 4. Root-cause map (read these before coding)

| Concern | File | Notes |
|---|---|---|
| cascade entry | `core/ntexup-engine/.../document/NTxPropCalculator.java` | `computePropertyMagnitude` :233, `computeClassFoldAll` :203, `flattenClassDef` :170, `_HStyleRuleResult2` :71 |
| cascade precedence | `core/ntexup-api/.../style/NTxStyleMagnitude.java` | distance → selector specificity → index (later wins) → score (dead) |
| selector matching | `core/ntexup-api/.../style/DefaultNTxNodeSelector.java` | `items` is `HashSet`; `getClassDefNames` :64 allocates |
| props container | `core/ntexup-api/.../style/NTxProperties.java` | `get(String...)` :60 eager message; last synonym wins |
| renderer lookup | `core/ntexup-engine/.../renderer/DefaultNTxRendererContext.java` | `computePropertyValue` :808, `computeProperties` :791 |
| aggregate caches | `core/ntexup-api/.../eval/NTxValueByName.java` | 25 lookups in `getNodeCommonNoCache` / `getNodeSizeNoCache` / `getNodeFontNoCache` |
| context copy | `core/ntexup-engine/.../eval/NTxResolutionContextImpl.java` | ctor :52 copies vars/defs/functions; `uid` string concat |
| render entry / invalidation | `core/ntexup-engine/.../impl/DefaultNTxEngine.java` | `renderPage` :1401, `someChange` :1433, `invalidateRenderCache` :1461, double render in `renderImage` :1370 |
| node cache | `core/ntexup-engine/.../document/DefaultNTxNode.java` | `invalidateRenderCache` :1062 recursive; `getAndSetRenderCache` :1041 |
| control flow | `core/ntexup-engine/.../eval/NTxCompiler.java` | `compileNodeTree_if` :609, `compileNodeTree_for` :635, `compileNodeTree_import` :592 |
| import parser | `core/ntexup-engine/.../parser/nodeparsers/ImportSpecialParser.java` | args evaluated at compile time |
| if parser | `core/ntexup-engine/.../parser/nodeparsers/IfSpecialParser.java` | `$condition` + `$else` |
| for parser | `core/ntexup-engine/.../parser/nodeparsers/ForSpecialParser.java` | var + iterable; compile-time unroll |
| viewer render lane | `renderers/ntexup-renderer-screen/.../PageView.java` | `onShow`/`forceRender` :202, `prefetch` :205, `PAGE_RENDER_LOADER` |
| viewer document | `renderers/ntexup-renderer-screen/.../DocumentView.java` | `reloadDocumentSync` :775, 1 s `checkResourcesChanged` :298 |
| plot2d resampling | `extensions/ntexup-extension-plot2d/.../expr/NTxPlotNExprResolver.java` | per-point context allocation |
| text shadow | `core/ntexup-engine/.../util/NTx2DUtils0.java` | `tint` :43, `drawShadowed` :149 |

---

## 5. Phase plan

### Phase 0 — Baseline, harness, test scaffolding (no production code)

- [x] Port the perf harness (section 2.1) into `test/ntexup-test`.
- [x] Capture baseline numbers for D1 and D2 (startup, cold, warm, JFR).
- [x] Capture **baseline PNGs** for every page of D1 and D2 at 1920×1080
      (`renderImage`, `animate=false`, `print=true`), store hashes + images under
      `test/ntexup-test/src/test/resources/golden-cascade/`.
- [x] Capture baseline PDFs for D1 and D2.
- [x] Add the new test classes as **disabled/skeleton** JUnit tests:
      `TestCascadeControlFlow`, `TestCascadeAnimation`, `TestCascadeEquivalence`,
      `TestCascadeLoops` (see section 6).
- [x] Record the exact commands to reproduce each number in this README.

**Gate 0:** baseline table + images committed; harness reproducible by one command.

---

### Phase 1 — Low-risk allocation fixes

No algorithm change; behaviour must be byte-identical.

- [x] **`uid` fast path.** Add a bounded/static cache in `NTxUtils.uid(String)` or a
      fast path: if the string is already lower-kebab (`[a-z0-9-]+`), return it.
      Keep `uid` semantics identical for every existing property name.
- [x] **`uids()` fast path.** For a single non-null name, avoid `LinkedHashSet` +
      array allocation.
- [x] **Lazy `NOptional` message** in `NTxProperties.get(String...)` and
      `get(String)`: build the diagnostic string only on miss.
- [x] **Cache `getClassDefNames()`** on `DefaultNTxNodeSelector` (computed once in
      the constructor, immutable). Return an unmodifiable set.
- [x] **Remove per-lookup list copies** in `_HStyleRuleResult2s` and
      `flattenClassDef` (`toList()`/`toArray()` per call).
- [x] **Kill per-render logging** that is unconditional (plot2d `FUNCTION_X`,
      `NChronometer`), or gate it behind a debug property.

Tests:

- [x] Unit: `uid("Fill-Color") == uid("fill-color")`, idempotent, and all
      `NTxPropName` constants map to themselves.
- [x] Unit: `NTxProperties.get("a","b")` still returns **b** when both exist
      (last-synonym-wins is the current contract — pin it).
- [x] Unit: `getClassDefNames()` equality with the old implementation.
- [x] Golden: D1 + D2 pixel-identical.

**Gate 1:** all existing tests pass; golden diff = 0; cold ≥ 15 % faster,
warm ≥ 10 % faster.

---

### Phase 2 — Computed-style pass (the big win)

- [x] Add a per-node computed style result:
      `Map<String, NTxStyleAndMagnitude>` (or a compact equivalent) computed
      **once per node**, top-down, when the page is first rendered.
- [x] Do **not** compute it during `pages()`; compute lazily on first render of
      the page so `show` opening does not get slower.
- [x] Store it on `DefaultNTxNode` in a dedicated field/slot, **not** in
      `renderCache` (which `invalidateRenderCache` nukes).
- [x] `computePropertyMagnitude` becomes: computed-map lookup → fall back to the
      renderer-context `defaultStyles` → miss. Keep `engine.getDefaultStyles()`
      in the computed pass.
- [x] `computePropertiesMagnitude` uses the same computed map (fix the current
      divergence: the plural path ignores `engine.getDefaultStyles()` and
      renumbers candidates).
- [x] Preserve exact precedence: own prop → class fold → nearest ancestor →
      specificity → declaration index (later wins) → score. No reinterpretation.
- [x] Structural invalidation: per-node structural epochs are captured along
      the node's ancestor path, so edits invalidate only affected subtrees;
      property, class, parent, rule, and mutable rule-style changes are covered.
      Document recompilation copies nodes with empty caches. No theme-reload
      API exists in the engine today.
- [x] Keep the renderer-context `defaultStyles` fallback dynamic (it is per
      renderer call, e.g. text defaults).

Tests:

- [x] Port/extend `TestStyleSystem` precedence matrix (single base, multi-base,
      deep chain, usage-site order, instance wins, nearest scope, specificity
      beats class, recency tie-break, `*`, `!`, `and`, table selectors).
- [x] **Equivalence test** (`TestCascadeEquivalence`): for every node and every
      property name on D1 and D2, compare the new computed value with the legacy
      resolver (`-Dntexup.style.legacy=true`). Assert **0 mismatches**.
- [x] Golden: D1 + D2 pixel-identical, cold and warm.
- [ ] Control-flow tests from section 6 must pass with the cache on.

**Gate 2:** equivalence 0 mismatches; golden diff = 0; cold ≥ 45 % faster
(98 → ≤ 54 ms/page); warm ≥ 25 % faster.

---

### Phase 3 — Value folding & epoch invalidation

- [ ] Split each property value into **static fold** (literals, `either()`,
      arithmetic on constants, document-scoped immutable vars) and **dynamic
      residue** (`%P`, `%R`, percent units, `time`, animation vars, futures).
- [ ] Cache the folded value per `(node, prop, varEpoch)`; re-apply only the
      residue per render.
- [x] `evalExpression` short-circuits unannotated primitive number, boolean, and
      null literals. Reuse the context-bound evaluator for expressions that
      still require evaluation; strings are never bypassed.
- [ ] Wire `varEpoch`: a var assignment (`@assign`, animation variable) bumps it
      and invalidates only the nodes that depend on that var (use
      `DefaultNTxDependencyGraph`).
- [ ] Never fold: `%P`/`%R`/percent, `time`, animation state, async futures,
      anything read through `NTxValueByName` that resolves to a dynamic value.
- [ ] Remove the legacy resolver only after Gate 2 equivalence is green.

Tests:

- [ ] Same node, same prop, two renders at the same time → identical bytes.
- [ ] Time-dependent page at t=0, t=1, t=2 → different bytes (cache does not
      freeze time).
- [ ] Resize (e.g. 1920×1080 → 1280×720) → `%P` values change; golden matches the
      legacy resolver at the new size.
- [ ] Var reassignment invalidates only affected nodes (assert unrelated nodes
      keep the same cached identity/value).
- [ ] Golden: static decks byte-identical.

**Gate 3:** animation tests pass; static golden diff = 0; warm ≥ 35 % faster than
baseline; no `time`/`%P` leakage.

---

### Phase 4 — Per-node context copy

- [ ] Stop copying `vars`/`definitions`/`functions` per node render. Share
      immutable maps; copy-on-write only when a scope actually adds a binding.
- [ ] Replace the per-context `uid` string concatenation with a lazy/structural
      identity (only materialise it when logged).
- [ ] Preserve scoping semantics: nested scopes, shadowing, `for`/`if` scopes.

Tests:

- [ ] `TestVariablesAnywhere` (existing) unchanged.
- [ ] New scoping tests: nested `for` + `if`, variable shadowing, variable used
      after a loop, `@let` inside a component body.
- [ ] Golden: D1 + D2 identical.

**Gate 4:** scoping tests pass; warm ≥ 40 % faster than baseline; golden diff = 0.

---

### Phase 5 — Renderer-level caches

- [ ] plot2d: cache sampled geometry keyed by
      `(function expression, params, x-range, width, height, timeEpoch)`. Do not
      cache across time for animated plots.
- [ ] Text shadow: cache the tinted + blurred bitmap keyed by
      `(text, font, size, color, blur, shadow params)`.
- [ ] PlantUML: cache the rendered diagram keyed by `(source, size, params)`.
- [ ] Remove/gate per-render logging and `NChronometer` allocations.

Tests:

- [ ] plot2d golden at several `startTime` values; identical at the same time.
- [ ] Shadow golden; identical after cache warm.
- [ ] `TestHtmlStaticExport` animation tests keep passing.
- [ ] D1 + D2 golden identical (static).

**Gate 5:** plot-heavy pages ≥ 30 % faster warm; animation frames still differ;
golden diff = 0.

---

### Phase 6 — Startup, imports, viewer

- [ ] Batch imports: hoist all `import(...)` args in a compilation scope and call
      `engine.importDependencies(...)` once. Preserve evaluation order and
      conditional-import semantics.
- [ ] Conditional imports: `import(either(cond,"a","b"))`, `import(if(...))`, and
      `import` inside an `if` true/false block must keep working. Add tests.
- [ ] Viewer: use `pagesIterator()` lazily; create `PageView` on first visit,
      keep a small window; stop building 440 components at open.
- [ ] Viewer: prefetch at the canonical render size (not 1024×768), then scale on
      paint; remove the unconditional `forceRender()` on `onShow` when the cached
      image is still valid.
- [ ] Resource monitor: replace the 1 s full-source scan (`checkResourcesChanged`)
      with a `WatchService` or a debounced current-page check.
- [ ] `renderImage`: avoid the double render when futures are pending (await
      first, render once).

Tests:

- [ ] Import tests: unconditional, conditional via `either`, conditional via
      `if` block, unknown import error, order sensitivity.
- [ ] Startup benchmark: first paint ≤ 4.2 s on D2.
- [ ] Viewer smoke test: open D2, navigate first/last/random, resize, no hang.

**Gate 6:** first paint ≤ 4.2 s; import tests pass; no viewer regression.

---

### Phase 7 — Docs & close-out

- [ ] Document the cache model (structural epoch, var epoch, size epoch, frame
      epoch) in `documentation/developer-guide/02-engine-pipeline.md`.
- [ ] Add a "style resolution" section with the precedence table and the
      cache-invalidation rules.
- [ ] Final benchmark table (D1 + D2, before/after).
- [ ] Move this folder to `documentation/done/style-cascade-performance/`.

**Gate 7:** docs updated; final numbers recorded; all tests green.

---

## 6. Required tests (explicit scope)

### 6.1 `TestCascadeControlFlow` — `if` / conditional nodes

- [ ] `if(true){ ... }` renders the true block only.
- [ ] `if(false){ ... }` renders nothing; `if(false){...}else{...}` renders the
      else block.
- [ ] Nested `if` inside `for`, and `if` whose condition depends on the loop var.
- [ ] `if` used to choose between two property values / two nodes with classes.
- [ ] Conditional node inside a styled component: the winning property is the
      same with the cache on/off.
- [ ] Assert **both** the node structure and the rendered PNG.

### 6.2 `TestCascadeControlFlow` — conditional imports

- [ ] `import(either(flag, "ext-a", "ext-b"))` resolves the selected extension.
- [ ] `import` inside `if(true){...}` is compiled; inside `if(false){...}` is not.
- [ ] Import order is preserved; duplicate imports are idempotent.
- [ ] Missing/unknown import still produces the same error as before.
- [ ] After batching (Phase 6), the resolved dependency set is identical.

### 6.3 `TestCascadeLoops` — `for` (and `while` status)

- [ ] `for(i:[1->2]){ ... }` produces the expected number of nodes and values.
- [ ] `for` over an array, over a computed range, nested `for`.
- [ ] `for` + `if` (skip some iterations), `for` producing classed nodes.
- [ ] Variable scoping: the loop var is not visible after the loop; shadowing
      works.
- [ ] **`while` is not implemented today.** There is no `CTRL_WHILE` in
      `NTxNodeType`, no parser, and no `while` grammar. Add a test that documents
      the current behaviour (compile error / unknown node) and a task:
      - [ ] Confirm with the user whether `while` is in scope. If yes, add
            `CTRL_WHILE` as a **compile-time** loop with a mandatory max-iteration
            guard and the same tests as `for`. If no, keep the documentation test.
- [ ] Cache equivalence for every loop test (cached vs legacy resolver).

### 6.4 `TestCascadeAnimation`

- [ ] Render a time-dependent page (e.g. `flow:dots(period:...)` from
      `ntexup-doc-slides/.../0180-connector-animation.ntx`) at `startTime` t0 and
      t1 with `animate=true`; assert the images differ.
- [ ] Render the same page twice at the same `startTime`; assert byte-identical.
- [ ] `animate=false` renders exactly one deterministic frame, identical to the
      Phase-0 baseline.
- [ ] `pageElapsedSeconds()` / `requestFrame()` path: a test node that requests a
      frame still gets one; no frame is requested when `animate=false` or the page
      is hidden.
- [ ] Existing `TestHtmlStaticExport` animation tests
      (`animatesMovingPagesAndKeepsSettledPagesAtOneFrame`,
      `animateFalseFreezesEveryPageOnItsFirstFrame`) keep passing.
- [ ] Animation must not be frozen by the style cache: assert property values that
      depend on `time` are re-evaluated every frame.

### 6.5 `TestCascadeEquivalence` — the safety net

- [ ] For D1 and D2, walk every node and every property name; compare
      `computePropertyMagnitude` (cached) with the legacy resolver
      (`-Dntexup.style.legacy=true`). Assert 0 mismatches.
- [ ] Render every page cold and warm; compare PNG bytes with the Phase-0
      baseline. Assert 0 differing pixels.
- [ ] Render D1 and D2 to PDF; compare SHA-256 with the baseline.

---

## 7. Regression gate (run before closing every phase)

```bash
# 1. Build + unit/integration tests
cd ntexup
mvn -q -pl core/ntexup-api,core/ntexup-engine -am test
mvn -q -pl test/ntexup-test -am test

# 2. Plain-main style suite (use the CP defined in section 2.1)
java -Dnuts.term=false -Xmx4g -cp "$CP" \
  net.thevpc.ntexup.test.TestStyleSystem

# 3. Golden sample decks
mvn -q -pl test/ntexup-test -am test -Dtest=TestSampleDecksRender

# 4. Animation
mvn -q -pl test/ntexup-test -am test -Dtest=TestHtmlStaticExport

# 5. Full-deck cold/warm timings (golden pixel comparison is in the cascade test)
java -Dnuts.term=false -Xmx4g -cp "$CP" \
  net.thevpc.ntexup.test.perf.NTxRenderAll "$D1" 1920 1080
java -Dnuts.term=false -Xmx4g -cp "$CP" \
  net.thevpc.ntexup.test.perf.NTxRenderAll "$D2" 1920 1080

# 6. Equivalence
mvn -q -pl test/ntexup-test -am test -Dtest=TestCascadeEquivalence
```

Checklist:

- [ ] All commands exit 0.
- [ ] Golden diff = 0.
- [ ] Equivalence mismatches = 0.
- [ ] Benchmark table updated in the progress log.
- [ ] No new warnings/errors in the render log (`ERROR`, `WARNING`, stack traces).
- [ ] `git status` contains only the phase's intended files.

---

## 8. Progress log

Append a new entry after every phase and every benchmark run. Use this format:

```
### YYYY-MM-DD — Phase X.Y — <status: in progress | done | blocked>

Changed:
- file:line — what and why

Tests:
- command → result (pass/fail, count)

Benchmarks (D2, 1920x1080):
- first paint: X ms (baseline Y)
- cold/page: X ms (baseline 98)
- warm/page: X ms (baseline 19.6)
- cold total: X s (baseline 43.1)
- warm total: X s (baseline 8.6)
- golden diff: 0 / N pixels
- equivalence mismatches: 0

Notes / blockers:
- ...
```

### 2026-11-11 — Phase 0.0 — baseline captured (investigation, outside repo)

Changed:
- none (investigation only)

Tests:
- ad-hoc harnesses under `/tmp/opencode` (not yet ported)

Benchmarks (D2 = antenna-and-propagation, 440 pages, 1920x1080):
- first paint: ~5.6 s
- cold/page: 98 ms (p50 84, p90 169, p99 256, max 803)
- warm/page: 19.6 ms
- cold total: 43.1 s
- warm total: 8.6 s
- golden diff: n/a (no baseline images captured yet)
- equivalence mismatches: n/a

Notes / blockers:
- `while` node does not exist; decision needed (section 6.3).
- Harness must be ported into the repo and baseline images captured before
  Phase 1 starts.

### 2026-11-12 — Phase 0.1 — done

Changed:
- `test/ntexup-test/src/test/java/net/thevpc/ntexup/test/perf/` — ported the
  seven requested main-based harnesses and added deterministic PNG/PDF capture.
- `test/ntexup-test/src/test/java/net/thevpc/ntexup/test/` — added disabled
  scaffolds for the four planned cascade test suites.
- this README — documented reproducible classpath, benchmark, and capture
  commands.

Tests:
- `mvn -q -pl test/ntexup-test -am -DskipTests test-compile` → pass.
- `mvn -q -pl test/ntexup-test -am test` → 50 tests, 2 failures, 4 skipped.
  Failures: `TestRenderBackends.htmlRendererHandlesConnectorsAndDecorations`
  (empty HTML output) and `TestConnectorIntegration.loopedNodesCanBeReferencedByChainedConnectors`
  (expected node was null).
- `TestStyleSystem` plain-main suite → all 149 assertions passed.
- `TestSampleDecksRender` and `TestHtmlStaticExport` → passed (1 and 9 tests).
- `NTxGoldenCapture` on D1 → captured 46 PNGs, PDF, and SHA-256 manifest.
- `NTxGoldenCapture` on D2 → captured 440 PNGs, PDF, and SHA-256 manifest.
- `sha256sum -c` for D1 and D2 manifests → pass.
- `NTxRulesProbe`, `NTxClassProbe`, and `NTxCtxProbe` on D1 and D2 → pass;
  counts match the stated deck sizes and D2 context sizes.

Benchmarks (D1 and D2, 1920x1080):
- `NTxColdBreak`: Nuts.require 691 ms; engine ctor 13 ms; parse 146 ms;
  document compile/imports 1182 ms; pages drain 240 ms; first page render 179 ms.
- `NTxRenderAll`: cold total 4603 ms (100.1 ms/page), p50 60.5 ms, p90
  221.7 ms, p99/max 580.6 ms; warm total 1450 ms (31.5 ms/page), p50 9.2 ms,
  p90 46.5 ms, p99 327.0 ms.
- `NTxColdOnly` JFR pass (elapsed excludes recording finalization): 5037 ms
  (109.5 ms/page), recording
  `/tmp/ntx-d1-cold.jfr`.
- `NTxWarmOnly` JFR pass (elapsed excludes recording finalization): 1442 ms
  (31.4 ms/page), recording
  `/tmp/ntx-d1-warm.jfr`.
- D2 `NTxColdBreak`: Nuts.require 712 ms; engine ctor 14 ms; parse 149 ms;
  document compile/imports 1522 ms; pages drain 1402 ms; first page render
  200 ms (≈4.0 s total; local dependency cache was already warm).
- D2 `NTxRenderAll`: cold total 38.76 s (88.1 ms/page), p50 78.6 ms, p90
  156.2 ms, p99 254.5 ms, max 861.2 ms; warm total 8.09 s (18.4 ms/page),
  p50 12.8 ms, p90 31.2 ms, p99 58.5 ms, max 645.1 ms.
- D2 `NTxColdOnly` JFR pass (elapsed excludes recording finalization): 40.20 s
  (91.4 ms/page), recording
  `/tmp/ntx-d2-cold.jfr`.
- D2 `NTxWarmOnly` JFR pass (elapsed excludes recording finalization): 7666 ms
  (17.4 ms/page), recording
  `/tmp/ntx-d2-warm.jfr`.
- D2 JFR attribution (31,703 cold / 5,810 warm samples): cold top responsible
  frames were `DefaultNTxNodeSelector.acceptNode` 18.6%, `NTxUtils.uid` 12.1%,
  `NTxResolutionContextImpl.evalExpression` 9.7%, `getClassDefNames` 6.8%,
  `NTxProperties.get` 5.2%, `NTxValue.asElementArray` 3.7%,
  `NTxUtils.uids` 3.5%, plot2d `compileFunctionX` 2.2%,
  `_HStyleRuleResult2` 2.1%, and `computePropertyMagnitude` 2.0%.
- Cold leaf frames: `HashMap$HashIterator.nextNode` 12.1%,
  `HashMap$HashIterator.<init>` 10.8%, `NNameFormat.parse` 6.3%, and
  `HashMap.putVal` 6.2%.
- Cold/warm samples containing `computePropertyMagnitude`: 54.8% / 38.0%;
  `acceptNode`: 19.7% / 15.0%; `NTxValueByName`: 83.5% / 48.5%;
  `evalExpression`: 13.0% / 9.3%; `uid`: 15.5% / 10.3%;
  `NTxProperties.get`: 5.2% / 2.9%; `getClassDefNames`: 6.8% / 6.7%.
- Baseline images and PDFs are stored in the committed Phase 0 changes.
- golden diff: n/a (baseline capture).
- equivalence mismatches: n/a.

Notes / blockers:
- Gate 0 passed: baseline table/images and harness reproduction instructions
  are committed. The general regression run still reports the two pre-existing
  failures listed above; neither is in the cascade code changed by Phase 0.
- Baseline renders emit existing diagnostics: D1 `scoreboard` invalid-argument
  and null-font messages; D2 reports missing U+27C2 glyphs in DejaVuSans.
  These need to be recorded as baseline behavior, not treated as phase regressions.

### 2026-11-12 — Phase 1.0 — implementation complete; gate blocked

Changed:
- `core/ntexup-api/.../NTxUtils.java` — added the canonical lower-kebab return
  fast path and one-name `uids()` fast path.
- `core/ntexup-api/.../NTxProperties.java` — deferred diagnostic construction
  until misses and exposed a live read-only values view to avoid list/array
  copies in cascade traversal.
- `core/ntexup-api/.../DefaultNTxNodeSelector.java` — cached immutable class
  definition names.
- `core/ntexup-engine/.../NTxPropCalculator.java` — traversed property views
  directly while preserving map order and property precedence.
- `extensions/ntexup-extension-plot2d/.../NTxDrawContextRenderCompiler.java`
  — removed unconditional per-render function timing/logging.
- `test/ntexup-test/.../TestStyleCascadeAllocations.java` — added four
  regression tests for UID normalization, one-name `uids`, last-synonym
  precedence/lazy miss messages, and cached immutable class names.

Tests:
- `mvn -q -pl test/ntexup-test -am -DskipTests test-compile` → pass.
- `mvn -q -pl test/ntexup-test -am -Dtest=TestStyleCascadeAllocations
  -Dsurefire.failIfNoSpecifiedTests=false test` → 4 passed.
- `TestStyleSystem` → 149 assertions passed.
- `mvn -q -pl test/ntexup-test -am test` → 54 tests, 2 failures, 4 skipped.
  The same two failures already present before Phase 1 remain:
  `TestRenderBackends.htmlRendererHandlesConnectorsAndDecorations` and
  `TestConnectorIntegration.loopedNodesCanBeReferencedByChainedConnectors`.
- D1/D2 golden capture → all 46 + 440 PNG hashes match the committed baseline.
  PDF page counts and file sizes match; binary PDF hashes differ.
- `git diff --check` → pass.

Benchmarks (D1 and D2, 1920x1080; `NTxRenderAll`):
- D2 cold: 29.308 s / 66.6 ms per page (baseline 38.76 s / 88.1 ms; 24.4%
  faster); p50/p90/p99/max = 58.0/115.6/273.8/709.1 ms.
- D2 warm: 6.138 s / 14.0 ms per page (baseline 8.09 s / 18.4 ms; 24.2%
  faster); p50/p90/p99/max = 10.0/23.0/38.8/484.1 ms.
- D1 cold/warm: 4.229/1.427 s (baseline 4.603/1.450 s).
- Golden diff: 0/486 PNGs; equivalence mismatches: n/a.

Notes / blockers:
- The D2 cold and warm improvement thresholds and pixel-golden checks passed.
  Gate 1 remains blocked because its full-suite requirement is not met: the two
  pre-existing, unrelated test failures still fail. Phase 1 is not marked done,
  and Phase 2 has not started.

### 2026-11-12 — Phase 2.0 — computed-style cache implemented; gate blocked

Changed:
- `core/ntexup-engine/.../DefaultNTxNode.java` — added dedicated computed-style
  cache storage and per-node structural epochs; cache entries validate the
  node-to-root epoch path, so mutations invalidate only dependent subtrees.
- `core/ntexup-engine/.../NTxPropCalculator.java` — computes a lazy candidate
  index on first property lookup, preserving own/class/ancestor/default-style
  precedence and alias selection. Singular and plural lookups now share it;
  the plural path includes engine defaults. Removed the stale class-flatten
  memo after class flattening moved to the per-node pass. The old uncached
  resolver remains available with `-Dntexup.style.legacy=true`.
- `core/ntexup-api/.../NTxNode.java`, `NTxProperties.java`, and
  `DefaultNTxStyleRule.java` — invalidate the owning node's structural epoch
  when a mutable style-rule property changes.
- `test/ntexup-test/.../TestCascadeEquivalence.java` — checks cached vs legacy
  results, plural vs legacy singular results, and cache invalidation for node,
  class, and rule-style changes. With deck paths supplied, it checks every
  compiled node against every property name found in the deck and default
  styles.

Tests:
- `mvn -q -pl test/ntexup-test -am -Dtest=TestCascadeEquivalence \
  -Dntexup.test.d1="$D1" -Dntexup.test.d2="$D2" \
  -Dsurefire.failIfNoSpecifiedTests=false test` → 3 passed; 0 mismatches.
  D1: 2,403 nodes × 77 properties = 185,031 comparisons.
  D2: 20,732 nodes × 88 properties = 1,824,416 comparisons.
- `TestStyleCascadeAllocations` → 4 passed.
- `TestStyleSystem` → 149 assertions passed.
- `TestSampleDecksRender` and `TestHtmlStaticExport` → passed.
- Full `mvn -q -pl test/ntexup-test -am test` → 56 tests, 2 failures,
  4 skipped. Only the same pre-existing HTML-backend and connector-loop tests
  fail.
- Golden capture → all 486 D1/D2 PNG hashes match the Phase 0 baseline.
- D1 PDF extracted text matches; page counts and byte sizes match for both
  decks, but PDF SHA-256 values differ.

- D2 cold: 18.803 s / 42.7 ms per page (baseline 38.76 s / 88.1 ms; 51.5%
  faster); p50/p90/p99/max = 32.6/65.2/231.9/751.9 ms.
- D2 warm: 5.238 s / 11.9 ms per page (baseline 8.09 s / 18.4 ms; 35.3%
  faster); p50/p90/p99/max = 8.5/17.7/65.6/488.2 ms.
- D1 cold/warm: 3.671/1.463 s (Phase 1 4.229/1.427 s).
- Golden diff: 0/486 PNGs; equivalence mismatches: 0.

Notes / blockers:
- The D2 cold and warm performance thresholds, exact equivalence, and PNG
  goldens pass. Gate 2 remains blocked: the full suite retains the two known
  unrelated failures, and the explicit `TestCascadeControlFlow` /
  `TestCascadeLoops` scaffolds are still disabled.
- As in Phase 1, regenerated PDF binary hashes differ despite matching page
  counts and byte sizes; the Phase 2 visual gate is the per-page PNG hashes.

### 2026-11-12 — Phase 3.0 — literal fast-path trial rejected

Before changing Phase 3 code, the current Phase 2 tree measured on D2 at
1920×1080 with `NTxRenderAll`: cold 20.095 s total (45.7 ms/page), warm
5.802 s total (13.2 ms/page).

Tried bypassing `NTxNodeEval` for unannotated primitive literals in
`NTxResolutionContextImpl.evalExpression`, with focused tests for literal
identity and string interpolation. The measured pass was faster (cold 13.464 s,
30.6 ms/page; warm 5.014 s, 11.4 ms/page), but it was not behavior-preserving:
the D1/D2 PNG hash comparison found 37/46 and 87/440 differing pages. The
rendered differences include missing text backgrounds and code highlighting.
The trial and its tests were reverted; no Phase 3 optimization is retained.
After reverting, recapturing D1 matched its committed PNG hashes.

**Status:** The broad literal shortcut was rejected, but a narrower primitive
fast path is retained below. No dynamic-value folding or `varEpoch` caching has
been implemented. Phase 2 remains gate-blocked by missing control-flow coverage
and the two previously reported suite failures.

### 2026-11-12 — Phase 3.1 — primitive evaluation fast path

Changed:
- `NTxResolutionContextImpl` now returns unannotated number, boolean, and null
  elements directly, matching `NTxNodeEval`'s behavior for these primitive
  groups. Strings and all annotated elements continue through the evaluator.
- Reuse a lazily created `NTxNodeEval` per resolution context. The evaluator
  holds no mutable state and reads the current context on each call.
- `TestCascadeValueFolding` verifies primitive identity and confirms the reused
  evaluator observes a reassigned string variable.

Validation:
- `TestCascadeValueFolding` and `TestCascadeEquivalence` pass; D1 and D2
  comparisons report 0 mismatches (185,031 and 1,824,416 comparisons).
- D1 and D2 PNG manifests match all 486 committed Phase 0 hashes.
- Full suite: 58 tests, 2 failures, 4 skipped. The only failures remain the
  known HTML backend and connector-loop tests.
- D2 `NTxRenderAll` at 1920×1080: immediately before this change, cold/warm
  20.095/5.802 s; after the change, two runs were 18.596/5.419 s and
  18.346/5.158 s. Earlier Phase 2 runs were 18.803/5.238 s, so the apparent
  improvement over the immediate pre-change run is within run-to-run variance
  and is not treated as an established gain.

**Status:** Partial Phase 3 implementation only. The dynamic residue split,
folded-value cache, and dependency-scoped `varEpoch` invalidation remain open.
No animation/resize leakage tests have been added, so Gate 3 is not passed.

---

## 9. Open questions

1. **`while`**: not implemented. Confirm whether to add it or document it as
   unsupported. (Section 6.3.)
2. **Cascade precedence**: proximity currently beats specificity. Keep as-is or
   align with CSS? Caching will freeze the current behaviour, so decide **before**
   Phase 2. Add a precedence test matrix either way.
3. **Synonym order** in `NTxProperties.get(String...)`: last match wins
   (`fg` overrides `foreground-color`). Keep or reverse? Pin with a test.
4. **`computeProperties()` (plural)** is effectively unused by core renderers and
   diverges from the singular path. Delete it or wire it to the new computed map?
5. **Viewer prefetch size**: render at canonical size and scale, or re-render per
   actual size? Phase 6 assumes canonical + scale; confirm quality expectations.

---

## 10. Definition of done

- [ ] All phases and gates complete.
- [ ] D2 first paint ≤ 4.2 s, cold ≤ 45 ms/page, warm ≤ 10 ms/page.
- [ ] Golden diff = 0 on D1 and D2.
- [ ] Equivalence mismatches = 0.
- [ ] Control-flow, conditional-import, loop, and animation tests green.
- [ ] Docs updated; this folder moved to `documentation/done/`.
