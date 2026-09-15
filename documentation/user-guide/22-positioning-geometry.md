# Positioning and Geometry

Coordinates start at the **top-left** of the parent; x grows right, y grows down.

## Anchor Points (`at`)

```tson
text("top-left",     at: top-left)
text("top",          at: top)
text("top-right",    at: top-right)
text("left",         at: left)
text("center",       at: center)
text("right",        at: right)
text("bottom-left",  at: bottom-left)
text("bottom",       at: bottom)
text("bottom-right", at: bottom-right)
```

## `position` + `origin`

`position` is absolute `(x, y)`; `origin` is the element's reference point (point or anchor word):

```tson
rectangle(origin: (50, 50), position: (50, 50), size: (30, 30))
rectangle(origin: (100, 100), position: (50, 50), size: (30, 30))
rectangle(origin: left, position: (6, 14))
text("...", origin: center, position: (50, 65))
```

## Offsets from Anchors

```tson
text("Below title", at: top, down: 80)
text("Above bottom", at: bottom, up: 30)
text("Left of center", at: center, leftOf: 50)
text("Right of center", at: center, rightOf: 50)
```

## Size

```tson
rectangle(size: 100)                 // square
rectangle(size: (200, 100))          // w, h
rectangle(size: (50, 25))            // points
rectangle(size: 15%P)                // % of page
text("...", font-size: 3%P)
```

## Units

| Suffix | Meaning | Example |
|--------|---------|---------|
| (none) | points | `size: 50` |
| `%` | percent of parent | `size: 50%` |
| `%P` | percent of page | `size: 15%P`, `font-size: 3%P` |

## Margin

```tson
rectangle(margin: 10)                // all sides
rectangle(margin: (5, 30))           // h, v
rectangle(margin: (5, 30, 0, 0))     // top, bottom, left, right
rectangle(margin: [10, 20])          // bracket syntax
```

## Rotation

```tson
arrow(at: center, rotate: -45)
rectangle(size: (100, 50), rotate: 45)
```

## Grid Geometry

### Grid size form

```tson
grid(2, 2)            // 2 columns, 2 rows
grid((6, 3))          // same, tuple form
```

### Weights

```tson
grid(4, 2, columns-weight: [1, 1.5], rows-weight: [1, 2])
grid(2, 3, columns-weight: [1, 1.2])
```

### Grid lines

```tson
grid(3, 2, draw-grid, line-color: documentColors[0])
```

### Spanning

```tson
rectangle(colspan: 2)
rectangle(rowspan: 2)
```

## Container Positioning Summary

| Property | Purpose |
|----------|---------|
| `at` | alignment anchor |
| `position` | absolute `(x, y)` |
| `origin` | reference point within element |
| `up/down/leftOf/rightOf` | offset from anchor |
| `size/width/height` | dimensions |
| `rotate` | degrees (clockwise) |
| `margin` | outer spacing |

## Practical Example

```tson
content-slide(title: "Positions", variant: 4) {
    body {
        grid(6, 3, columns-weight: [1, 3]) {
            styles {
                "*": { margin: 2 }
            }
            miniPage {
                rectangle(at: center, size: (30, 30),
                          background: documentColors[4],
                          color: documentColors[2])
            }
            miniPage {
                rectangle(origin: (50, 50), position: (50, 50),
                          size: (30, 30),
                          background: documentColors[4],
                          color: documentColors[2])
            }
        }
    }
}
```