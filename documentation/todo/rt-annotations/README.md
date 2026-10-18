# Task: PDF annotation toolbar with sidecar persistence for ntexup

## Context

ntexup is a Java scientific document authoring DSL (TSON syntax) with its own
rendering layer (NTxGraphics, print mode vs. live/animated mode). Add a **real-time
annotation toolbar** for PDF display, with annotations persisted in a **sidecar
file** (the PDF itself is never modified).

## Step 0: Explore before coding (mandatory)

Before writing any code, inspect the repository and report back (briefly):

1. Where PDFs are currently rendered/displayed in ntexup (viewer component, page
   renderer, library used: PDFBox / PDF.js / other) and what UI toolkit hosts it
   (Swing / JavaFX / web).
2. How input events (mouse/keyboard) reach the viewer, and how zoom/pan/page layout
   are modeled (needed for coordinate mapping).
3. The entry points to parse and render ntexup text (including equations) outside
   a full document: what is needed to render a standalone snippet, at what cost,
   and what context it requires.
4. Existing conventions: module layout, property system (`getPropertyValue`,
   `matchesNamedPair`), color handling, serialization utilities, tests.

If no PDF viewer exists, say so and propose the minimal viewer needed before building
the toolbar. Do not invent APIs: adapt to what exists. Keep the design consistent with
existing code style.

## Functional requirements

### Tools (toolbar buttons, one active at a time)

| Tool | Behavior |
|---|---|
| Select text | Text selection on the PDF text layer (copy supported). Also acts as the default/pointer mode. |
| Straight line | Click-drag from start to end point. Shift = snap to 0/45/90°. |
| Straight arrow | Same as line, with arrowhead at end (size scales with stroke width). |
| Freehand line | Free drawing (ink). Capture points, smooth/simplify (e.g. Ramer-Douglas-Peucker + optional spline) before storing. |
| Highlight text | Highlight the selected text range (semi-transparent fill over text quads). |
| Strike text | Strikethrough line through the selected text range. |
| Squiggly text | Wavy underline under the selected text range. |
| Ellipse | Click-drag bounding box. |
| Circle | Click-drag; constrained to equal width/height (center- or corner-anchored, pick one and document it). |
| Rectangle | Click-drag bounding box. Shift = square. |
| Text | Click to place a text box, edit its **ntexup source** (see "Rich text annotations" below), commit on Ctrl+Enter/blur (Enter = newline). Editable afterwards. |

Text-markup tools (highlight / strike / squiggly) must work on **text ranges**
resolved from the PDF text layer (per-line quads), not on arbitrary rectangles.

### Hand-drawn (irregular) rendering of shapes

Annotation shapes do not have to look geometrically perfect. Line, arrow, ellipse,
circle and rectangle support a **rendering style** with two modes:

- **Clean**: exact geometry.
- **Sketchy**: hand-drawn look (slightly wobbly strokes, imperfect closure of
  ellipses/rectangles, overshooting corners, optional double stroke), in the spirit
  of rough.js. Implement the algorithm natively in Java (no JS dependency).

Rules:

- A **roughness** parameter (0 = clean, higher = messier) is exposed in the toolbar
  next to stroke width, applies to new annotations and to the selected one, and is
  remembered per tool.
- The stored geometry stays **ideal** (bounding box, endpoints, center/radii).
  The irregularity is a **render-time effect only**: resizing, moving and
  hit-testing work on the ideal shape (with tolerance), and switching Clean <->
  Sketchy is lossless.
- Rendering must be **deterministic**: each annotation stores a `seed` and
  `roughness` in the sidecar, so it looks identical on every load, zoom level and
  machine, and does not "jiggle" on repaint or while being moved/resized (only on
  an explicit "re-roll" action, optional).
- Irregularity scales with page space (points), not with screen pixels, so the
  look is stable across zoom.
- The sketchy variant also applies to the squiggly/strike markup lines if it fits
  cleanly; otherwise leave those exact.
- Freehand (ink) is already irregular by nature: it gets smoothing only, no
  roughness effect.
- Out of scope for now: recognizing a freehand gesture and snapping it to a clean
  shape. Shape tools remain drag-to-define. Mention in the design note how this
  could be added later.

### Rich text annotations (ntexup format)

Text annotations are **not plain text**: their content is written in the **same text
format ntexup already uses for document text**, so it supports everything ntexup
text supports (inline/display equations, formatting, symbols, etc.), and is rendered
by ntexup's **existing** text/equation rendering pipeline, not a new one.

- The annotation stores the **source string** (ntexup syntax) as the single source
  of truth; the rendered result is derived and cached, never persisted.
- Reuse the existing parser/renderer and its property/style resolution (FG/BG colors
  from the toolbar feed into the same style mechanism). Do not duplicate text layout
  or equation code.
- Editing: while editing, show the source in an inline editor; on commit, render it.
  Live preview while typing is desirable (debounced); a parse/render error must
  show an inline error state on the annotation (and keep the source intact), never
  crash or lose the text.
- Rendering at any zoom must stay crisp: render from vector/graphics primitives
  (NTxGraphics) at the current scale, or re-rasterize on zoom change, rather than
  scaling a bitmap. Cache keyed by (source, style, scale).
- Font size is expressed in PDF user-space points so it scales with the page.
- Any ntexup feature that depends on a document context (imports, `@define`
  templates, variables) must have a defined, documented behavior for annotations
  (e.g. an isolated minimal context by default, with an optional way to provide
  one). Flag in the design note what is and isn't available.
- Text markup tools (highlight/strike/squiggly) stay plain geometry; only the Text
  tool carries ntexup source.

### Style controls

- **FG color** (stroke / text / markup line color) and **BG color** (fill / highlight
  color) pickers, with alpha support.
- Stroke width control.
- Style applies to the active tool for new annotations, and to the **currently
  selected annotation** when one is selected (editing style of an existing one).
- Remember last-used style per tool.

### Interaction

- Annotations are real-time: shown immediately while drawing (live preview), no
  re-render of the PDF page required.
- Select an existing annotation by clicking it (hit-testing), move/resize via handles,
  delete via `Delete`.
- Undo/redo (Ctrl+Z / Ctrl+Y), at least for create / modify / delete / style change.
- Escape cancels the in-progress drawing / returns to Select.
- Works correctly across **zoom, pan, scroll and page changes**.

### Show / hide

- Toolbar toggle to show/hide all annotations (view-only flag; does not delete or
  alter stored data). The hidden state should not discard in-progress edits.

### Persistence (sidecar)

- Annotations stored in a sidecar file next to the PDF, e.g. `<name>.pdf.annots.json`
  (propose the format; JSON by default, but check whether TSON is already the
  idiomatic serialization in ntexup and justify the choice).
- The format must be **versioned** (`"version"` field) and include a reference to the
  PDF it belongs to (file name + content fingerprint, reusing ntexup's existing
  fingerprint utilities if applicable) so a mismatch (PDF replaced/changed) is
  detected and reported to the user rather than silently misplacing annotations.
- Coordinates stored **per page in PDF user space (points, origin per PDF spec)**,
  never in screen pixels, so they are zoom/DPI independent. Define and document
  page rotation and CropBox handling.
- Text-markup annotations store both the quads (geometry) and the anchored text
  snippet (for diagnostics/re-anchoring).
- Each annotation has a stable id, type, page index, geometry, style (fg, bg, alpha,
  width, roughness, seed), text content (if any: the **raw ntexup source string**, plus the ntexup
  syntax/version it was written against), created/modified timestamps.
- Autosave (debounced) + explicit save; atomic write (write temp file then rename);
  load on open; graceful handling of a missing, corrupt or newer-version sidecar
  (never crash, never overwrite a file you could not parse without backup).

## Architecture requirements

- Separate cleanly: **model** (annotation types, no UI dependency) / **store**
  (sidecar read/write, versioning) / **tools** (one class per tool, state machine for
  gestures) / **overlay renderer** (draws annotations on top of the page) /
  **toolbar UI**. Model and store must be unit-testable headless.
- Single coordinate-transform component (screen <-> page space) used everywhere.
- Annotation overlay is a layer above the PDF rendering; it must not require
  re-rasterizing the page.
- Annotation type set is extensible (new tool = new model type + tool + renderer
  entry, no changes in the store core).
- Respect ntexup's print mode vs. live mode: annotations are interactive and
  drawn in live mode; define explicitly whether they are rendered in print/export
  (default: **not** rendered unless an explicit option is enabled) and keep that
  switch simple.

## Out of scope (do not build now, but don't block)

- Embedding annotations into the PDF as standard PDF annotation objects
  (design the model so an export-to-PDF-annotations feature can be added later).
- Multi-user collaboration / sync.

## Deliverables

1. Short design note (what you found in Step 0, chosen architecture, sidecar format
   spec with a complete example JSON).
2. Implementation, in small reviewable commits (model -> store -> transform ->
   tools -> overlay -> toolbar).
3. Unit tests: sidecar round-trip, version/fingerprint mismatch handling, corrupt
   file handling, coordinate transform (zoom/rotation), freehand simplification,
   undo/redo.
4. A demo/showcase document or screen exercising every tool.

## Working rules

- Ask me only for decisions that are genuinely blocking; otherwise pick the sensible
  default, state it explicitly, and continue.
- Flag anything in this spec that is ambiguous or conflicts with the existing code
  instead of silently guessing.
- Don't add dependencies without justifying them; prefer what the project already
  uses.