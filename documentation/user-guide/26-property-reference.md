# Property Reference

The complete list of node properties observed in ntexup.

## Size

| Property | Values | Description |
|----------|--------|-------------|
| `size` | `n` or `(w,h)` | width & height / single value |
| `width` / `height` | number | single dimension |
| `font-size` | number or `%P` | font scale |

## Position

| Property | Values | Description |
|----------|--------|-------------|
| `at` | anchor word | `center`, `top`, `top-left`, `bottom-right`, ... |
| `position` | `(x, y)` | absolute coordinates |
| `origin` | `(x,y)` or anchor | element reference point (`left`, `center`, `top-left`, ...) |
| `up` / `down` | number | vertical offset from anchor |
| `leftOf` / `rightOf` | number | horizontal offset from anchor |

## Font & Text

| Property | Values | Description |
|----------|--------|-------------|
| `font-family` | string | font family (`monospaced`, ...) |
| `font-size` | number or `%P` | font size |
| `font-bold` | bare flag | bold |
| `font-italic` | bare flag | italic |
| `font-underlined` | bare flag | underline |
| `color` | color | text color |
| `text-wrap` | `wrap`/... | wrapping |
| `text-align` | `left`/`center`/`right`/`justify` | alignment |
| `letter-spacing` | number | letter spacing |

## Colors

| Property | Values | Description |
|----------|--------|-------------|
| `color` | color / `#hex` | text color or contour of polygon shapes |
| `background` | color / `#hex` | fill color |
| `background-color` | color / `#hex` | fill color (2.5D solids: cylinder, donut, pie) |
| `line-color` | color / `#hex` | grid/stroke color |
| `contour-color` | color / `#hex` | 3D wireframe color |
| `grid-color` | color / `#hex` | grid line color |

## Drawing

| Property | Values | Description |
|----------|--------|-------------|
| `stroke` | `dashed` / `basic(width:, dash:, cap:)` / number | stroke style |
| `draw-contour` | bare flag | draw outline |
| `draw-grid` | bare flag | draw grid lines |
| `dash` | pattern | dash pattern |
| `round-corner` | number | corner radius |
| `shadow` | `{distance:(x,y), radius:, color:}` | drop shadow |
| `fill` | boolean | filled flag |
| `theed` / `raised` | flag | visual effect |

## Spacing

| Property | Values | Description |
|----------|--------|-------------|
| `margin` | `n` / `(h,v)` / `(t,b,l,r)` / `[h,v]` | outer spacing |
| `padding` | `n` / `(h,v)` / `(t,b,l,r)` | inner spacing (content box) |

## Layout (grid)

| Property | Description |
|----------|-------------|
| `columns-weight` | `[w1, w2, ...]` column expansion weights |
| `rows-weight` | `[h1, h2, ...]` row expansion weights |
| `col-weight` / `row-weight` | per-cell weight |
| `colspan` | columns a cell spans |
| `rowspan` | rows a cell spans |
| `draw-grid` | draw grid lines |

## Source Code

| Property | Description |
|----------|-------------|
| `source-primary/keyword/string/comments/number/separator-color` | token colors |
| `source-<token>-font-bold/italic/underlined` | token fonts |

## 3D

| Property | Description |
|----------|-------------|
| `transform` | `{ rotate(to:(x,y,z), angle:), scale:, translate() }` |
| `faces` | `{ top:, bottom:, front:, back:, left:, right: }` |
| `meridians` / `parallels` | sphere tessellation |
| `radius` | sphere radius `(x,y,z)` |
| `turns` | helix turns |

## Solid / Chart (2.5D)

| Property | Description |
|----------|-------------|
| `inner-radius` | donut inner radius |
| `start-angle` | donut/pie start angle |
| `extent-angle` | donut/pie sweep |
| `ellipse-height` | cylinder cap ellipse height |
| `segment-count` | cylinder segment count |

## Lines / Curves

| Property | Description |
|----------|-------------|
| `from` | start point (or angle for `arc`) |
| `to` | end point (or angle for `arc`) |
| `ctrl` | quadratic curve control point |
| `ctrl1` / `ctrl2` | cubic curve control points |
| `start-arrow` | `simple()`, `triangle()`, `triangle-full()`, `oval()`, `oval-full()`, `rectangle()`, `rectangle-full()`, `diamond()`, `diamond-full()`, `none` |
| `end-arrow` | same as `start-arrow` |
| `arrow` | sets both arrows at once (unless `start-arrow`/`end-arrow` is given) |
| `points` | polyline points in absolute page units (`((x1,y1), (x2,y2), ...)`) |
| `via` | connector intermediate points, in percent of the parent area |
| `route` | connector routing; only `straight` is implemented (others warn and fall back) |
| `label` | line/polyline/connector label: string or `{text, position, offset, color, font-size, bold, italic, font-family}` |
| `flow` | flow animation: `dots(count, size, period, speed, reverse, color, static: hide)` |

## Connector

| Property | Description |
|----------|-------------|
| `from` | named node reference `"name"`, `"name@side"`, or a free `(x,y)` point in percent of the parent area |
| `to` | same as `from` |
| `via` | optional intermediate points in percent of the parent area; the connector becomes a polyline |
| `route` | `straight` (default). Other values warn and fall back to straight |

Endpoint names may be dotted (`"block.node"`) to select a node inside a named ancestor.
Hidden endpoints suppress the connector silently; unresolved names log a warning.

## Plot

| Property | Description |
|----------|-------------|
| `x` | sampler `dtimes(start, stop, count)` |
| `f(x)` | function expression (trace property) |
| `title` | trace label (plot or trace property) |

## Automation / Control

| Property | Description |
|----------|-------------|
| `include(...)` | include files |
| `import(...)` | import extensions |
| `@define` | define components |
| `@for` | loop |
| `@if` | conditional |

## Async Computation

| Property | Description |
|----------|-------------|
| `pending` | `true` or a string/object: static placeholder instead of the node. A name, as in `pending: myResult`: shows an animated label until that computation finishes, then the node |
| `pending-text` | text of the animated label, or `false` to hide it |
| `pending-color` | text color of the animated label |
| `pending-background` | background of the animated label |

A plot waiting on a solver shows this label by itself. In `show`, a corner badge also lists what is still running, on every slide.

## Document/Theme Variables

| Variable | Description |
|----------|-------------|
| `documentColors[i]` | 12-color palette |
| `documentColorAccent` | active palette index |
| `documentSurface` | panel color |
| `documentLine` | line color |
| `documentTitle` / `documentSubtitle` / `documentAuthor` / `documentDate` / `documentVersion` | cover metadata |
| `documentShowRunningTasks` | `false` hides the running-tasks badge in `show` |