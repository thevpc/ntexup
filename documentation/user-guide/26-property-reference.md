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
| `columns-weight` | `[w1, w2, ...]` column expansion weights; positive entries override child weights |
| `rows-weight` | `[h1, h2, ...]` row expansion weights; positive entries override child weights |
| `col-weight` / `row-weight` | positive per-cell weight for one axis |
| `grid-weight` | positive per-cell weight for both axes; used when the matching axis weight is absent |
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
| `points` | polyline points in absolute page units (`((x1,y1), (x2,y2), ...)`), or percents of the polyline box with the `relative` flag |
| `via` | connector waypoints in the parent's coordinate space |
| `route` | connector route: sharp/polyline (default), orthogonal, or rounded; see the Connector table for aliases |
| `bend-radius` | rounded-route corner radius (default `12`), reduced to fit short adjacent segments |
| `label` | line/polyline/curve/connector label: string or `{text, position, offset, color, font-size, bold, italic, font-family}`; connectors place it along the final route |
| `flow` | flow animation: `dots(count, size, period, speed, reverse, color, static: hide)`; connectors follow the final route |

## Connector

| Property | Description |
|----------|-------------|
| `from` | named node reference `"name"`, `"name@side"`, `"name@edge(percent)"`, `"name@radial(degrees)"`, or a free `(x,y)` point in the parent's coordinate space |
| `to` | same as `from` |
| `via` | optional waypoints in the parent's coordinate space; routing determines how they are joined |
| `route` | case-insensitive mode: sharp/polyline (`straight`, `direct`, `line`, `none`, `auto`, `polyline`, `sharp`; default `auto`), orthogonal (`orthogonal`, `ortho`, `right-angle`, `elbow`), or rounded (`rounded`, `curve`, `curved`) |
| `bend-radius` | rounded corner radius (default `12`); each waypoint change of direction is rounded, with the radius reduced near short segments |

Endpoint names may be dotted (`"block.node"`) to select a node inside a named ancestor.
A bare name (or `@auto`) aims its border anchor toward the other endpoint, or toward the
first/last `via` point when present. `top/right/bottom/left(percent)` selects a point on
that edge; percentages run clockwise from the top-left and must be in `0..100`.
`radial(degrees)` uses clockwise-positive local angles (`0` right, `90` down) and
intersects the transformed layout outline. Explicit anchors also guide the opposite
automatic anchor. Hidden endpoints suppress the connector silently; unresolved names
and malformed parametric anchors log a warning.

Routing is waypoint-based and does not avoid obstacles or choose alternate corridors.
Unsupported route names warn and fall back to sharp waypoint routing. Painting, arrowheads,
labels, flow, debug bounds, and render dumps share the final routed geometry; label
`position` and flow distance are measured along that geometry, including rounded bends.

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
| `for(i: ...)` | loop |
| `if(...)` | conditional |

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