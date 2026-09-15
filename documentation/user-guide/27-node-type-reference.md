# Node Type Reference

Complete catalog of element (node) types recognized by ntexup, organized by category.

## Document

| Node | Description |
|------|-------------|
| `page` | a slide or page |
| `pageGroup` | group of pages (auto splitting) |

## Layout Containers

| Node | Description |
|------|-------------|
| `group` | positioned container; has variable scope |
| `fragment` | transparent container; no scope; dissolves |
| `block` | scoped container; dissolves |
| `flow` | inline flow layout |
| `grid` | table grid |
| `row` | horizontal stack |
| `col` / `column` | vertical stack |
| `hcenter` | horizontal centering |
| `vcenter` | vertical centering |
| `filler` | expands to fill remaining space |
| `void` | invisible spacer |

## Shapes (2D)

| Node | Description |
|------|-------------|
| `rectangle` | rectangle (optionally rounded) |
| `square` | square |
| `circle` | circle |
| `ellipse` | ellipse |
| `rhombus` | rhombus (alias `diamond`) |
| `triangle` | triangle |
| `pentagon` | regular pentagon |
| `hexagon` | regular hexagon |
| `heptagon` | regular heptagon |
| `octagon` | regular octagon |
| `nonagon` | regular nonagon |
| `decagon` | regular decagon |
| `rhombus` | rhombus / diamond |
| `trapezoid` | trapezoid |
| `parallelogram` | parallelogram |
| `polygon` | arbitrary polygon (points) |

### Solids (2.5D)

| Node | Description |
|------|-------------|
| `cylinder` | shaded cylinder (`ellipse-height`, `segment-count`) |
| `donut` | donut/ring with slices (`inner-radius`, `start-angle`, `extent-angle`) |
| `pie` | pie slice (`start-angle`, `extent-angle`) |

## Lines & Curves

| Node | Description |
|------|-------------|
| `line` | straight segment with arrowheads (`from`, `to`) |
| `arc` | arc between angles (`from`, `to` degrees) |
| `quad-curve` | quadratic Bezier (`ctrl`) |
| `cubic-curve` | cubic Bezier (`ctrl1`, `ctrl2`) |
| `arrow` | arrow shape |

## Text

| Node | Description |
|------|-------------|
| `text` | text content |
| `plain` | unformatted text |
| `ntf` | NTx Text Format (flavored) |
| `source` / `src` | syntax-highlighted code |
| `equation` / `eq` | LaTeX equation |

## Lists

| Node | Description |
|------|-------------|
| `ul` / `unordered-list` | bulleted list |
| `ol` / `ordered-list` | numbered list |

## Media

| Node | Description |
|------|-------------|
| `image` | PNG/JPEG/SVG/GIF image |

## Diagrams (plantuml extension)

| Node | Description | Example |
|------|-------------|---------|
| `uml` | UML (use-case/class/sequence/activity) | `uml(at:center, size:(100,50) """...""")` |
| `gantt` | Gantt charts | `gantt(size:(95,70), at:center """...""")` |
| `nwdiag` | network diagrams | `nwdiag(size:(100,50), at:center """...""")` |
| `wireframe` | UI wireframes | `wireframe(size:(90,30), at:center """...""")` |
| `mindmap` | mind maps | `plantuml` |
| `wbs` | work breakdown | `plantuml` |
| `regex` | regex diagrams | `plantuml` |
| `ebnf` | grammar diagrams | `plantuml` |
| `chronology` | chronology | `plantuml` |
| `chen` | ER diagrams | `plantuml` |
| `ditaa` | ASCII diagrams | `plantuml` |

Requires `import("plantuml")`.

## 3D (shapes3d extension)

| Node | Description |
|------|-------------|
| `scene3d` | 3D scene root (`transform`, `draw-contour`) |
| `box` | cuboid with per-face `faces` colors |
| `sphere` | UV sphere (`radius`, `meridians`, `parallels`) |
| `pyramid` | pyramid with per-face `faces` colors |
| `helix` | spiral (`radius`, `height`, `turns`, `line-color`) |
| `prism` | prism |
| `surface` | parametric surface |
| `concat` | concatenated primitives |
| 3D equivalents | `line3d`, `polyline3d`, `polygon3d`, `triangle3d`, `arc3d` |

Requires `import("shapes3d")`.

## Plots (plot2d extension)

| Node | Description |
|------|-------------|
| `plot2d` | plot container (`x: dtimes(a,b,c)`) |
| `curve` | line trace with `f(x)` and `title` |
| `area` | filled-area trace with `f(x)` and `title` |

Requires `import("plot2d")`.

## Control Directives

| Node | Description |
|------|-------------|
| `assign` | set a variable (`x = 42`) |
| `@for` | loop |
| `if` | conditional |
| `@define` | component/function definition |
| `include` | include a file |
| `import` | import an extension module |
| `return` | produce a result |

## Components (from themes/templates)

| Node | Description |
|------|-------------|
| `cover-slide` | cover page |
| `content-slide` | content page with `body` |
| `agenda-slide` | agenda page |
| `thankyou-slide` | closing page |
| `card(accentColor:)` | container with accent bar |
| `kicker(...)` | overline label in cards |
| `stat(value:, label:)` | KPI number box |
| `quote(author:)` | quote with attribution |
| `hint-info(...)` / `hint-warning(...)` | callouts |
| `panel` | light canvas container |
| `miniPage` | doc-slide preview page |
| `miniCodeNative` | doc-slide code preview |

> The exact set of theme components depends on the theme — inspect the theme's `.ntx` files.

## Model Reference (how types map)

Internal node type constants (API):

```
PAGE_GROUP, PAGE, FRAGMENT, GROUP, BLOCK, FLOW, GRID, ROW, COLUMN
RECTANGLE, SQUARE, CIRCLE, ELLIPSE, TRIANGLE, PENTAGON, HEXAGON,
HEPTAGON, OCTAGON, NONAGON, DECAGON, RHOMBUS, TRAPEZOID, PARALLELOGRAM,
SPHERE, ELLIPSOID, CYLINDER, DONUT, PIE
LINE, POLYLINE, POLYGON, QUAD_CURVE, CUBIC_CURVE, ARC, ARROW
ORDERED_LIST, UNORDERED_LIST
PLAIN, TEXT, IMAGE, SOURCE, NTF, VOID, FILLER, SCENE3D
CTRL_ASSIGN, CTRL_FOR, CTRL_DEFINE, CTRL_INCLUDE, CTRL_IMPORT,
CTRL_IF, CTRL_EXPR, CTRL_CALL
PLOT2D, EQUATION
```