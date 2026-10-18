# Task: Connector features for ntexup (attachments, component references/ports, routing)

You are working on **ntexup** (Java, `NTx*` / `Ntx*` classes, NElement-based DSL). Implement three related features for **lines and arrows** (and the other connector-like primitives: `arc`, `quad-curve`, `cubic-curve`), in **both 2D and 3D**.

Work in **phases with gates**. Do not start phase N+1 before reporting phase N. Do not guess about the codebase: read first, then report findings.

---

## 0. Context

Reference implementation to study: `Element3DLineBuilder implements NtxElement3DNodeParser` (3D line). It already contains:

- a `label` mini-DSL (string, or list/parametrized container with `text`, `position`, `orientation`, `offset`, `offset-perp`, `offset-parallel`, `offset-3d`, `offset-2d`, colors, font props),
- a style cascade for label props (`line-label-*`, `label-*`, then generic `color`, `font-size`, ...),
- arrow props: `start-arrow`, `end-arrow`, `arrow`/`arrows`.

Usage examples (2D, inside `miniPage`):

```
line(from:(10,10), to:(80,90), color:red, stroke:3,
     start-arrow:triangle(), end-arrow:triangle-full())
arc(from:30, to:180, color:red, stroke:3)
quad-curve(from:(10,10), to:(80,90), ctrl:(60,30), color:red, stroke:3)
cubic-curve(from:(10,10), to:(80,90), ctrl1:(60,30), ctrl2:(30,60), color:red, stroke:3)
```

Arrow kinds seen in docs: `simple`, `triangle`, `triangle-full`, `oval`/`circle`, `oval-full`/`circle-full`, `diamond`, `diamond-full`, `rectangle`, `rectangle-full`.

Docs are authored as ntexup slides (`content-slide(...){ body{ grid(4,2,columns-weight:[1,2]){ miniPage{...} miniCodeNTexup("""...""") } } }`). Each new feature must get a doc slide in that same style.

## 1. Phase 0: Investigate (no code changes), then report

Locate and summarize, with file paths:

1. The 2D line/arc/quad/cubic builders and their relationship to the 3D builders. How much is duplicated?
2. Arrow model (`NTxArrow`), how arrowheads are oriented and placed at ends (what tangent info do they use today, especially for curves?).
3. `NtxShapes3dUtils.resolvePosition3D` and the 2D equivalent. How are `from`/`to`/`position` resolved? What is `NTxBounds2D b` here?
4. The layout pipeline: how `grid`, `miniPage` etc. assign bounds and coordinate spaces; at which point a node's final bounds are known; whether a second pass is feasible.
5. How node `id`, `class`, and `styles:{}` selectors are modeled and queried at render time (needed for refs and "avoid class X").
6. **Components must be able to move over time**: positions/bounds bound to expressions of time, e.g. `x = f(time)`. This is a confirmed requirement. **Currently a global time variable is probably NOT wired** into the expression/render context; time is read directly inside specific components (e.g. the radiation pattern component). Find out: every place time/frame is consumed today and under what name; how `rendererContext.evalExpression` gets its variables and whether a variable can be injected there; what a "frame" is in the render loop (video export, live preview, both?); whether layout is recomputed per frame or only once; and what is cached between frames today.
7. Existing tests (unit/golden image/snapshot). Which ones cover lines/arrows/labels?
8. Anything in `Element3DLineBuilder` that looks like a bug or inconsistency. Known suspects: `orientation` values map position words to angles (`center`→90, `end`→180, `screen`→180 with 3D off); `rendererContext != null` guards are followed by unguarded `rendererContext.computePropertyValue(...)`; `parseSizeElement`/`hasPhysicalDistanceUnit` are private and will be needed elsewhere. Report, do not silently "fix" behavior.

**Gate 0 deliverable:** a short findings report + a concrete proposed class/interface layout for phases 1–3, noting where you deviate from this prompt and why.

## 1b. Phase 0b: Wire a global `time` variable (prerequisite for phases 2 and 3)

Moving components and per-frame connector resolution need time to be a first-class variable of the render context. Today it is not; some components (e.g. radiation pattern) read time through their own mechanism.

- Expose a **`time`** variable (seconds, double) in the expression evaluation context, so any property can be written as an expression of it, e.g. `at:(10 + 20*sin(time), 50)`. Use exactly the name `time`; if you find an existing name in use (`t`, `frame`, ...), report it and keep it as an alias.
- Single source of truth: the renderer/context supplies the current time for the frame being rendered; components must not keep their own clocks.
- **Migrate the existing time consumers** (radiation pattern, etc.) to read the global variable, with identical output for identical inputs. Their existing DSL properties keep working (compat shim if they take a time/phase argument). Golden tests must pass unchanged.
- Rendering at a given time must be **deterministic and stateless**: `render(doc, time)` yields the same result regardless of previous frames (needed for seeking and parallel export).
- Static documents (no time-dependent expressions) must render exactly as before, with no per-frame overhead; decide how the renderer knows an expression is time-dependent (e.g. mark expressions that reference `time`) so caching and dependency tracking in phase 2 can skip static content.
- Define behavior outside an animation context (static export): `time` defaults to 0, documented.
- Tests: an expression using `time` evaluated at several times; the migrated radiation pattern matches its previous output; out-of-order evaluation gives identical results.
- Doc slide: "Time variable".

**Gate 0b:** report, tests, doc slide. Phase 1 can proceed in parallel with this one since attachments do not need time, but phases 2 and 3 must not start before this gate passes.

## 2. Target model (design decisions already made; challenge only with evidence)

A **Connector** = resolved **endpoints** + a **path geometry** + **decorations**.

- Endpoints: literal points (today) or **references** to other components (phase 2).
- Path geometry: straight, arc, quad, cubic, polyline, and later *routed*. All implement one interface, e.g. `NTxPath` / `NTxPath3D`:
    - `length()`
    - `pointAt(s)`, `tangentAt(s)`, `normalAt(s)` where `s` is **arc-length fraction in [0,1]**, never the curve parameter `t`
    - `startTangent()`, `endTangent()` (used for arrowheads)
- Decorations: start/end arrows, and **attachments** (phase 1).

`line`, `arc`, `quad-curve`, `cubic-curve` keep their DSL names and behavior; internally they become connectors with different geometry. Zero behavior change for existing documents.

## 3. Phase 1: Path evaluator + attachments

### 3.1 Path evaluator
Introduce the path interface and implement it for every existing connector primitive, 2D and 3D. Move arrowhead orientation to use it if that is not already the case. Add unit tests (points/tangents on known curves; arc-length parametrization monotonic and uniform enough, e.g. numeric inversion with a lookup table for cubics).

### 3.2 Attachments
Generalize `label` (text only) into **attachments: any component hanging off the path.**

- An attachment has: anchor `at` (arc-length fraction, percent as today's `position`), offsets, orientation, and a **child component** (usually text, but any node: shapes, images, other components).
- Offsets are expressed **in the path frame** (parallel = along tangent, perp = along normal, up = binormal/z in 3D) plus the existing absolute `offset-2d`/`offset-3d`. Reuse the existing size/unit parsing (`NTxSize`, physical units vs percent); extract it from the builder into a shared utility.
- Orientation modes: `fixed` (no rotation), `tangent` (rotate with path, optional auto-flip so text is never upside down), `screen` (billboard, 3D). Keep legacy values working through a compat mapping; document the mapping.
- Multiple attachments per connector (e.g. a label at 30% and another at 80%).
- `label:` stays as **sugar** that desugars to one text attachment, going through the same code path. Existing label style cascade (`line-label-*`, ...) must keep working unchanged. Existing rendering must stay pixel-identical (golden tests).
- Proposed syntax (adapt to DSL conventions you find in phase 0; keep it consistent with how other nodes take children):

```
line(from:(10,10), to:(80,90), end-arrow:simple()){
    attach(at:30, perp:3, orientation:tangent){ text("force") }
    attach(at:80, perp:-4){ circle(radius:2, fill:red) }
}
```

- Attachment bounds: child is laid out in a local frame, then transformed to the path frame. In 3D, decide and document how 2D children are placed (plane of the connector vs billboard).

**Gate 1:** unit + golden tests, doc slide "Line attachments", short report.

## 4. Phase 2: References, ports, late binding

Goal: a connector endpoint can reference another component and stay attached to it.

- **Reference syntax** (adapt to DSL): by component `id`, optionally with a port: e.g. `from:ref("a")`, `from:ref("a", port:"east")`, `to:ref("b", port:45deg, gap:2)`. Extend the **position resolvers** (2D and 3D) so *any* component using `from`/`to`/`position` gets this for free, not only lines.
- **Ports:** introduce a provider interface (e.g. `NTxPortProvider`) that a component can implement to expose named anchor points. Default for every component: bounding-box ports `n,s,e,w,ne,nw,se,sw,center`. Shape-specific implementations where feasible (ellipse/circle by angle, rect, etc.). Support:
    - named/fixed ports,
    - **boundary-clipped** ("floating") endpoints: ray from the component center toward the other endpoint, clipped at the outline,
    - `gap` so arrowheads do not touch the target,
    - `auto` = pick the nearest port.
- **Coordinate spaces:** each `miniPage` has its own space (e.g. 0..100). A reference to a component in another space must be transformed into a common space and back into the connector's own space. This is the main cost. Be explicit about it in your design.
- **Late binding, per frame:** connectors are resolved in a pass **after** layout and after time-dependent expressions have been evaluated for the current `time` (they must not influence layout; exclude them from layout or ensure no cycles). Resolution must be a **pure function** of (tree state, `time`), with no hidden state, so any frame can be rendered independently (seeking, scrubbing, parallel export).
- **Dependency tracking:** each connector knows the set of components it references (endpoints now, obstacles in phase 3). Between frames, re-resolve a connector only if the bounds/ports of one of its dependencies changed; otherwise reuse the cached result. Resolve in topological order when a referenced component's position itself depends on other components. Detect dependency cycles (e.g. `x=f(time)` expressions that indirectly reference a connector's output) and report them instead of looping.
- **Continuity:** while a target moves, the endpoint must move continuously. Boundary clipping and `auto` port selection must not jump when a different port becomes "nearest"; add hysteresis or document the discontinuity and offer a fixed-port option to avoid it.
- **Performance budget:** per-frame resolution of N connectors must stay cheap (no allocation-heavy work in the hot path, no re-parsing of DSL). Add a benchmark with e.g. 200 connectors over 600 frames and report timings.
- **Errors:** unknown id, ambiguous id, ref to self, connector referencing another connector (disallow in v1 or support with explicit cycle detection). Produce clear diagnostics with source location, and a defined fallback (e.g. skip drawing + warn), never a crash or silent wrong drawing.
- The word is **ports**, not "sinks", in code and docs.

**Gate 2:** tests including cross-`miniPage` references, moved-target re-resolution, error cases; doc slide "Connecting components"; short report.

## 5. Phase 3: Routing (design doc first)

Routing is a **new connector geometry** (e.g. `route(...)` or `connector(route:...)`), producing an `NTxPath` so that arrowheads and attachments from phases 1–2 work on it unchanged.

Separate two concepts, they are not the same thing:

- **Style** (how the path is shaped): `straight`, `orthogonal` (right-angle with rounded corners option), `spline`, `radial`/polar, ...
- **Constraints** (what it must respect): `avoid: class(...)` / `avoid: ids(...)`, min clearance, max bends, prefer-straight, no-overlap with other routed connectors (later).

Requirements:

- Router is a **pluggable SPI** (`NTxRouter`: input = resolved endpoints/ports, obstacles with bounds, style, constraints; output = path). Registered by strategy name.
- Constraints can be infeasible. Define behavior: best-effort + diagnostic, or fallback to style-only. Never loop or hang. Add a time/complexity budget.
- **2D only in this phase.** For 3D, either route within a plane or reject with a clear error. Do not attempt full 3D obstacle avoidance.
- Obstacles come from the same id/class query machinery as phase 2, transformed into the routing space.
- **Obstacles and endpoints move over time**, so routing runs per frame. Requirements:
    - **Caching/invalidation:** reuse the previous route when no endpoint or obstacle changed (use the phase 2 dependency tracking). Cache keys must include obstacle bounds, not just ids.
    - **Temporal coherence:** a route must not flip between topologically different paths (left of an obstacle vs right of it) from one frame to the next because of tiny movements. Add hysteresis / a switching penalty against the previous frame's route, but keep resolution a pure function: either the previous route is an explicit input of the SPI that the renderer supplies deterministically, or the router is stateless and documents that flips can occur. Decide in the design doc and justify; if seeking/scrubbing to an arbitrary frame must give the same result as playing sequentially, a stateful approach is not acceptable.
    - **Budget:** the router SPI takes a per-call time/node budget and degrades to the style-only path with a diagnostic when exceeded.
    - Add tests with an obstacle sweeping across the path (`x = f(time)`) asserting: no crossing of the obstacle at any sampled frame, no teleporting (max per-frame displacement of the path bounded), and identical output when the same frame is rendered twice or out of order.

**Step 3a (design doc, before code):** evaluate (a) a built-in orthogonal visibility-graph + A* with bend penalty, (b) integrating ELK (Java, orthogonal edge routing) behind the SPI. Compare license, dependency weight, quality, controllability, and recommend one. Wait for approval.

**Step 3b (after approval):** MVP: `orthogonal` style + `avoid` constraint + bend penalty, with tests and doc slide.

## 6. Cross-cutting rules

- Backward compatibility is a hard requirement: all existing slides and tests must render identically.
- Eliminate 2D/3D duplication where you touch code; do not add new copies.
- Keep the DSL consistent: named pairs, parametrized containers, `NTxUtils.uid` key normalization, style cascade conventions. If you must introduce new property names, list them in the report.
- Handle `null`/missing `rendererContext` consistently (see Phase 0 item 8).
- Each phase ships with: unit tests, a golden/snapshot test where rendering is involved, and a doc slide in the existing `content-slide` / `miniPage` / `miniCodeNTexup` format.
- Edge cases to cover explicitly: zero-length path, coincident endpoints, endpoints inside the target component, text upside-down on leftward lines, attachments on closed or looping paths, very short paths with arrowheads larger than the path.

## 7. Reporting

After each gate: what was done, files touched, tests added, deviations from this prompt (with reason), open questions. If something in this prompt contradicts what the codebase actually does, stop and report instead of working around it.