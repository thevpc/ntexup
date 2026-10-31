# Lines and Arrows

ntexup provides line, curve, and arc elements with optional arrowheads. These are 2D elements from the `shapes2d` extension.

## Line

```tson
line(from: (10, 10), to: (80, 90), color: red, stroke: 3)
```

## Arrowhead Types

Arrowheads are specified with **function-call syntax**: `simple()`, `triangle()`, `triangle-full()`, `oval()`, `oval-full()`, `rectangle()`, `rectangle-full()`, `diamond()`, `diamond-full()`.

```tson
line(from: (10, 10), to: (80, 90),
     end-arrow: simple())

line(from: (10, 10), to: (80, 90),
     start-arrow: triangle(),
     end-arrow: triangle-full())

line(from: (10, 10), to: (80, 90),
     start-arrow: oval(), end-arrow: oval-full())

line(from: (10, 10), to: (80, 90),
     start-arrow: diamond(), end-arrow: diamond-full())

line(from: (10, 10), to: (80, 90),
     start-arrow: rectangle(), end-arrow: rectangle-full())
```

## Polyline

`polyline` draws a sequence of connected segments. Points are absolute page
coordinates (pixels), like the geometry primitives they are built from; unlike
`line` endpoints they are not percentages. It accepts the same
line styling and arrowhead properties as `line`, plus an optional label and flow
animation.

```tson
polyline(points: ((80, 200), (220, 120), (360, 240), (500, 160)),
         stroke: 2,
         start-arrow: simple(),
         end-arrow: triangle())
```

Points may also be written one per argument:

```tson
polyline((20, 80), (80, 40), (140, 90), end-arrow: simple())
```

## Line and polyline labels

`line`, `polyline`, and `connector` accept a `label` (or `text`) property. The value can be
a plain string or a tuple with label options:

```tson
line(from: (10, 10), to: (90, 10), label: "distance")
line(from: (10, 40), to: (90, 40),
     label: {text: "40 px", position: 35, offset: -6})
```

| Option | Meaning |
|--------|---------|
| `text` / `value` | label text |
| `position` / `pos` | position along the line, `0`..`100` (default `50`); values outside that range extrapolate beyond the ends |
| `offset` | perpendicular offset (number or size); a two-point tuple is currently interpreted as `(position, offset)` |
| `color` / `fg` | label color |
| `font-size` / `size` / `fs` | label font size |
| `bold` / `italic` / `font-family` | label font style |

The document-level properties `line-label-position`, `line-label-offset`,
`line-label-color`, `line-label-font-size`, `line-label-bold`, `line-label-italic`, and
`line-label-font-family` are also honoured (with shorter `label-*` aliases). The
`line-label-position` property now correctly moves the label anchor.

## Flow animation

`flow` animates dots travelling along a line, polyline, or connector. A bare `flow` flag
draws a single dot; use `dots(...)` to configure it:

```tson
line(from: (10, 60), to: (90, 60), end-arrow: simple(),
     flow: dots(count: 4, size: 3, period: 3))

line(from: (10, 90), to: (90, 90), flow: dots(count: 2, reverse, color: red))
```

| Option | Meaning |
|--------|---------|
| `count` | number of dots (default `1`) |
| `size` | dot radius in points (default `2`) |
| `period` | seconds for one dot to travel the full path (default `4`) |
| `reverse` | travel from end to start |
| `color` | dot color (defaults to the line color) |
| `static: hide` | omit the flow in static (PDF/image/print) output |

Flow uses the shared page animation clock, so all animated elements on a page are driven by
one frame loop. Static output (PDF, PNG, print) renders the dots at evenly spaced phase-0
positions.

## Connector

`connector` draws a line between two **named nodes**. Endpoints are clipped against the
target node outline, so the line stops at the border instead of the center.

```tson
rectangle(name: "api", at: (15, 20), size: (25, 15)) { text("API", at: center) }
rectangle(name: "db",  at: (65, 55), size: (25, 15)) { text("DB", at: center) }

connector("api", "db", end-arrow: simple())
```

The connector is rendered after the whole page has been laid out, so it may appear before
the nodes it references (forward references are supported).

### Endpoint syntax

| Form | Meaning |
|------|---------|
| `"name"` | border point on the ray between the two node centers |
| `"name@side"` | fixed side point: `left`, `right`, `top`, `bottom`, `top-left`, `top-right`, `bottom-left`, `bottom-right`, `center` |
| `(x, y)` | free point, in percent of the parent's available area (0..100) |

Endpoint arguments may also be named:

```tson
connector(from: "api@right", to: "db@left", end-arrow: triangle())
connector(from: "api", to: (75, 75), end-arrow: simple())
connector("api", "db", via: ((50, 30), (50, 70)), end-arrow: simple())
```

`via` adds intermediate points and draws a polyline; `label`, `flow`, `start-arrow`,
`end-arrow`, `arrow`, `stroke`, `dash`, and the usual line/color properties apply.

### Name scoping

Names are scoped to the enclosing named blocks. A dotted reference selects a node inside a
named ancestor:

```tson
group(name: "left") {
    rectangle(name: "x", ...)
    rectangle(name: "y", ...)
}
connector("left.x", "left.y", end-arrow: simple())
```

A bare name resolves to the nearest declaration (the candidate sharing the longest chain
of ancestors with the connector). Duplicate names in sibling groups are therefore safe.

### Hidden and unresolved targets

* If either endpoint is hidden (`visible: false`), the connector draws nothing and does not
  warn.
* If a name cannot be resolved, the connector draws nothing and logs one warning per node
  and message. Check the spelling, the enclosing named blocks, and that the target node
  actually renders (a node inside a hidden block is not registered).

### Routing

Only straight routes are implemented today. `route: straight` is accepted; any other route
value logs a warning and falls back to a straight line. Orthogonal/obstacle routing,
curved routes, and rounded corners are planned but not part of this version.

## Arc

```tson
// arc from angle 30° to 180° (angles in degrees)
arc(from: 30, to: 180, color: red, stroke: 3)
```

Note: the arc element uses `from`/`to` for the **angles** (start/end degrees).

## Quadratic Curve

```tson
quad-curve(from: (10, 10), ctrl: (60, 30), to: (80, 90), color: red, stroke: 3)
```

## Cubic Curve

```tson
cubic-curve(from: (10, 10), ctrl1: (60, 30), ctrl2: (30, 60),
            to: (80, 90), color: red, stroke: 3)
```

## Arrow

A simple arrow shape:

```tson
arrow(at: center, color: documentColors[2], rotate: -45)
```

## Stroke Styles

```tson
line(from: (0, 0), to: (100, 0), stroke: 2)
line(from: (0, 0), to: (100, 0), stroke: dashed)
line(from: (0, 0), to: (100, 0), stroke: basic(width: 3, dash: [6, 3], cap: round))
```

## Complete Examples

### UML-like connection

```tson
miniPage {
    line(from: (10, 10), to: (80, 90), end-arrow: triangle(), color: red, stroke: 3)
}
```

### Pie/arc gauges

```tson
arc(from: 0, to: 270, color: documentColors[1], stroke: 8)
```

### Diagram connectors

```tson
cubic-curve(from: (10, 10), ctrl1: (60, 30), ctrl2: (30, 60),
            to: (80, 90), color: documentColors[0], stroke: 2)
```