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