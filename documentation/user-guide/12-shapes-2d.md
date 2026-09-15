# Shapes (2D)

Shapes come from the `shapes2d` extension. Import it when needed:

```tson
import("shapes2d")
```

## Color Semantics

For a shape, `background` is the **fill** color and `color` is the **contour** color:

```tson
rectangle(size: (50, 20), background: documentColors[4], color: documentColors[2])
```

## Common Properties

| Property | Description |
|----------|-------------|
| `size` | `(width, height)` or single value |
| `at` / `position` / `origin` | placement |
| `background` | fill color |
| `color` | contour color |
| `line-color` | stroke color |
| `stroke` | width or style (`dashed`, `basic(width:2, dash:[8,4], cap:round)`) |
| `draw-contour` | draw outline (bare flag) |
| `margin` | outer spacing |
| `round-corner` | corner radius |
| `shadow` | `{distance:(4,4), radius:6, color:gray}` |
| `rotate` | degrees |

## Polygons

### Rectangle

```tson
rectangle(size: (100, 50), background: gold)
rectangle(size: (200, 100), round-corner: 8)
```

### Square

```tson
square(size: 50, background: documentColors[4])
```

### Triangle

```tson
triangle(size: 50, background: documentColors[4], color: documentColors[2])
```

### Rhombus (diamond)

```tson
rhombus(size: 50, background: documentColors[4])
// alias: diamond()
```

### Parallelogram

```tson
parallelogram(size: 50, background: documentColors[4])
```

### Trapezoid

```tson
trapezoid(size: 50, background: documentColors[4])
```

### Regular Polygons

```tson
pentagon(size: 50, background: documentColors[4])
hexagon(size: 50, background: documentColors[4])
heptagon(size: 50, background: documentColors[4])
octagon(size: 50, background: documentColors[4])
nonagon(size: 50, background: documentColors[4])
decagon(size: 50, background: documentColors[4])
```

### Custom Polygon

```tson
polygon(size: 50, count: 8, background: documentColors[4])

polygon(points: [(0,0), (50,0), (100,20), (80,80)],
        background: documentColors[4], color: documentColors[2])
```

## Ellipses and Circles

### Circle

```tson
circle(size: 50, background: documentColors[4])
```

### Ellipse

```tson
ellipse(size: (80, 30), background: documentColors[4])
```

## Pseudo-3D Solids

### Cylinder

```tson
cylinder(size: (30, 25), stroke: 5,
         ellipse-height: 20, segment-count: 5,
         background-color: "#00215E")
```

### Donut

```tson
donut(size: 40, stroke: 5,
      background-color: "#FC4100",
      inner-radius: 30, start-angle: 0, extent-angle: 270)
```

### Pie

```tson
pie(size: 40, stroke: 5, background-color: "#00215E",
    start-angle: 0, extent-angle: 270)
```

> Note these solids use `background-color` (with hyphen).

## Strokes

```tson
// dashed contour
rectangle(size: (60, 25), background: documentColors[4],
          draw-contour, line-color: documentColors[0], stroke: dashed)

// custom width/pattern
rectangle(size: (60, 25), draw-contour, line-color: documentColors[1],
          stroke: basic(width: 2, dash: [8, 4], cap: round))
```

## Shadows

```tson
// on a shape
rectangle(size: (55, 22), background: documentColors[0],
          color: white, shadow: { distance: (4,4), radius: 6, color: gray })

// on text
text("Elevated Text", font-bold, at: center, font-size: 3%P,
     color: documentColors[1], shadow: { distance: (3,3), radius: 4, color: gray })
```

## Gradient Colors (from `common-functions`)

```tson
rectangle(size: (35, 25),
          background: linearGradientColor(documentColors[0], documentColors[2]))

rectangle(size: (35, 25),
          background: linearGradientColor(documentColors[1], documentColors[4], 45))
```

## Lighter / Darker / Complementary / Invert / Hue Rotation

```tson
rectangle(background: lighterColor(documentColors[0], 0.4))
rectangle(background: darkerColor(documentColors[0], 0.4))
rectangle(background: complementaryColor(documentColors[1]))
rectangle(background: invertColor(documentColors[1]))
rectangle(background: rotateHueColor(documentColors[0], 120))
rectangle(background: sinebowColor(0.7))
```

## Shapes with Children (Labels)

```tson
rectangle(size: (140, 60), background: lightgoldenrod) {
    text("OK", at: center, font-bold)
}
```

## Example: Shape Gallery

```tson
content-slide(title: "Shapes", variant: 4) {
    body {
        grid(6, 3, columns-weight: [1, 3]) {
            styles {
                "*": { margin: 2 }
            }
            miniPage {
                rectangle(at: center, size: (50, 20), background: documentColors[4], color: documentColors[2])
            }
            miniPage {
                triangle(at: center, size: 50, background: documentColors[4], color: documentColors[2])
            }
            miniPage {
                square(at: center, size: 50, background: documentColors[4], color: documentColors[2])
            }
            miniPage {
                ellipse(at: center, size: (80, 30), background: documentColors[4], color: documentColors[2])
            }
            miniPage {
                hexagon(at: center, size: 50, background: documentColors[4], color: documentColors[2])
            }
            miniPage {
                polygon(at: center, size: 50, count: 8, background: documentColors[4], color: documentColors[2])
            }
        }
    }
}
```