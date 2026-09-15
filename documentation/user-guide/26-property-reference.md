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

## Document/Theme Variables

| Variable | Description |
|----------|-------------|
| `documentColors[i]` | 12-color palette |
| `documentColorAccent` | active palette index |
| `documentSurface` | panel color |
| `documentLine` | line color |
| `documentTitle` / `documentSubtitle` / `documentAuthor` / `documentDate` / `documentVersion` | cover metadata |