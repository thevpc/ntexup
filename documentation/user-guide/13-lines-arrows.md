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

## Line, polyline, curve and connector labels

`line`, `polyline`, `quad-curve`, `cubic-curve`, and `connector` accept a `label` (or `text`)
property. The value can be a plain string or a tuple with label options:

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

`flow` animates dots travelling along a line, polyline, curve, or connector. A bare `flow`
flag draws a single dot; use `dots(...)` to configure it:

```tson
line(from: (10, 60), to: (90, 60), end-arrow: simple(),
     flow: dots(count: 4, size: 3, period: 3))

line(from: (10, 90), to: (90, 90), flow: dots(count: 2, reverse, color: red))

// same rate on connectors of different length
line(from: (10, 30), to: (40, 30), flow: dots(count: 3, speed: 20))
line(from: (50, 30), to: (95, 30), flow: dots(count: 5, speed: 20))
```

| Option | Meaning |
|--------|---------|
| `count` | number of dots (default `1`) |
| `size` | dot head radius in device pixels; never smaller than twice the line width |
| `period` | seconds for one dot to travel the full path (default `4`) |
| `speed` | page units per second; overrides `period` so the crossing time grows with the path |
| `reverse` | travel from end to start |
| `color` | dot color (defaults to the line color) |
| `static: hide` | omit the flow in static (PDF/image/print) output |

`period` fixes how long a crossing takes, so a short connector shows its dots crawling and a
long one shows them racing. `speed` instead fixes the rate — a dot at `speed: 20` crosses a
28-unit connector in 1.4s and a 66-unit one in 3.3s — and is what to use when several
connectors of different lengths must look like one flow.

Each dot is drawn as a round head with a short trail tapering behind it, so the direction of
travel is legible even on a still frame. The trail never reaches past the anchor the dot came
from. Dots also fade in and out over the first and last 12% of the path rather than popping
into existence on the node border or under the arrowhead.

Flow uses the shared page animation clock, so all animated elements on a page are driven by
one frame loop. Static output (PDF, PNG, print) renders the dots at evenly spaced phase-0
positions, offset by half a spacing so none of them sits on an anchor.

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
| `"name"` | automatic border point aimed toward the other endpoint, or toward the first/last `via` point when present |
| `"name@side"` | fixed side point: `left`, `right`, `top`, `bottom`, `top-left`, `top-right`, `bottom-left`, `bottom-right`, `center`; `auto` selects the automatic border point |
| `(x, y)` | free point in the parent's coordinate space |

Endpoint arguments may also be named:

```tson
connector(from: "api@right", to: "db@left", end-arrow: triangle())
connector(from: "api", to: (75, 75), end-arrow: simple())
connector("api", "db", via: ((50, 30), (50, 70)), end-arrow: simple())
```

`via` adds waypoints between the resolved endpoints. The `route` mode determines whether
those waypoints are joined by sharp segments, adjusted into orthogonal legs, or rounded
at each junction. Without `via`, a connector is a straight line between its anchors.
`label`, `flow`, `start-arrow`, `end-arrow`, `arrow`, `stroke`, `dash`, and the usual
line/color properties apply to every route.

Free endpoints and every `via` tuple use the same coordinate rules: unsuffixed numbers
and `%` are percentages of the parent bounds, `%P` is a percentage of the page, and `px`
is a pixel offset from the parent origin. A multi-waypoint array follows these rules too;
it is not interpreted as raw page coordinates.

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

`route` selects how the connector passes through its `via` waypoints. Route names are
case-insensitive.

| Mode | Aliases | Behavior |
|------|---------|----------|
| Sharp / polyline | `straight`, `direct`, `line`, `none`, `auto`, `polyline`, `sharp` | Joins anchors and waypoints with straight segments. `auto` is the default; with `via`, it is a polyline rather than a single straight line. |
| Orthogonal | `orthogonal`, `ortho`, `right-angle`, `elbow` | Adjusts waypoints into horizontal/vertical legs where possible. |
| Rounded | `rounded`, `curve`, `curved` | Rounds each change of direction at a waypoint. Multiple waypoints produce multiple rounded junctions. |

`bend-radius` sets the rounded corner radius (default `12`). The radius is reduced near
short adjacent segments so neighboring corners do not overlap. For example:

```tson
connector(from: "api@right", to: "db@left",
          via: ((42, 30), (55, 30), (55, 62)),
          route: "rounded", bend-radius: 8,
          label: {text: "routed", position: 50, offset: 0},
          flow: dots(count: 3, period: 3), end-arrow: simple())
```

Painting, arrowheads, label placement, flow particles and trails, debug bounds, and render
dumps all use the same final routed point list. In particular, label `position` is measured
along the rounded path, and flow follows its bends rather than the straight chord between
the endpoints.

This is **waypoint routing, not obstacle avoidance**: the engine does not inspect other
nodes, choose alternate corridors, or move waypoints around obstacles. Orthogonal and
rounded modes only transform the supplied waypoints. Unsupported route names (for example,
`dijkstra`) log a warning and fall back to sharp waypoint routing.

## Arc

```tson
// arc from angle 30° to 180° (angles in degrees)
arc(from: 30, to: 180, color: red, stroke: 3)
```

Note: the arc element uses `from`/`to` for the **angles** (start/end degrees).

## Quadratic Curve

```tson
quad-curve(from: (10, 10), ctrl: (60, 30), to: (80, 90), color: red, stroke: 3)
quad-curve(from: (10, 10), ctrl: (60, 30), to: (80, 90),
           end-arrow: simple(), label: "quad", flow: dots(count: 2))
```

Like lines and polylines, curves accept `label` and `flow`; the label anchor and flow
particles follow the curve rather than its chord.

## Cubic Curve

```tson
cubic-curve(from: (10, 10), ctrl1: (60, 30), ctrl2: (30, 60),
            to: (80, 90), color: red, stroke: 3)
cubic-curve(from: (10, 10), ctrl1: (60, 30), ctrl2: (30, 60),
            to: (80, 90), end-arrow: simple(), label: "cubic")
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