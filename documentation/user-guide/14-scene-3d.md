# 3D Scenes

The `shapes3d` extension provides 3D scene rendering with primitives, camera transform, and per-face colors.

Import it:

```tson
import("shapes3d")
```

## The `scene3d` Element

```tson
scene3d(draw-contour) {
    box(size: (45, 35, 25), background: documentColors[3])
}
```

## Transform (camera & object)

```tson
scene3d(
    draw-contour,
    transform: {
        rotate(to: (0, 100, 0), angle: 35)
        scale: 0.75
        translate(-30, -41, -30)
    }
) {
    // primitives
}
```

| transform key | Description |
|---------------|-------------|
| `rotate(to: (x,y,z), angle: deg)` | rotation around vector |
| `scale: v` | uniform or `(x,y,z)` scale |
| `translate(x, y, z)` | offset |

## Primitives

### Box (with per-face colors)

```tson
box(position: (-22.5, -17.5, -12.5),
    size: (45, 35, 25),
    background: documentColors[3],
    faces: {
        top:   { background: documentColors[0] }
        front: { background: documentColors[4] }
        right: { background: documentColors[1] }
    }
)
```

### UV Sphere

```tson
sphere(radius: (28, 28, 28),
       meridians: 24, parallels: 16,
       background: documentColors[2],
       contour-color: documentColors[10])
```

### Pyramid (with per-face colors)

```tson
pyramid(position: (-22.5, -20, -17.5),
        size: (45, 40, 35),
        background: documentColors[7],
        faces: {
            front: { background: documentColors[4] }
            right: { background: documentColors[1] }
            left:  { background: documentColors[0] }
        }
)
```

### Helix

```tson
helix(position: (0, -22, 0),
      radius: (20, 20, 20),
      height: 45,
      turns: 3.5,
      line-color: documentColors[0],
      stroke: basic(width: 2))
```

## Layering Primitives

3D scenes compose naturally — boxes stack, spheres float above:

```tson
scene3d(draw-contour, transform: {
    rotate(to: (0, 100, 0), angle: 30)
    translate(-30, -41, -30)
    scale: 0.65
}) {
    // Base pedestal
    box(size: (60, 12, 60), position: (0,0,0),
        background: documentColors[10],
        faces: { top: { background: documentColors[1] } })
    // Second tier
    box(size: (44, 14, 44), position: (8, 12, 8),
        background: documentColors[5],
        faces: { top: { background: documentColors[6] } })
    // Crowning pyramid
    pyramid(size: (32, 28, 32), position: (14, 26, 14),
            faces: {
                front: { background: documentColors[0] }
                right: { background: documentColors[3] }
                left:  { background: documentColors[4] }
            })
    // Floating orb
    sphere(radius: (16, 16, 16), position: (30, 66, 30),
           meridians: 24, parallels: 16,
           background: documentColors[2])
}
```

## Helix Monument

```tson
scene3d(draw-contour, transform: {
    rotate(to: (0, 100, 0), angle: 25)
    translate(-25, -41, -25)
    scale: 0.65
}) {
    box(size: (50, 8, 50), position: (0,0,0),
        background: documentColors[10],
        faces: { top: { background: documentColors[5] } })
    helix(position: (25, 8, 25),
          radius: (18, 18, 18),
          height: 55, turns: 3.0,
          line-color: documentColors[0],
          stroke: basic(width: 2))
    box(size: (10, 50, 10), position: (20, 8, 20),
        background: documentColors[7])
    sphere(radius: (16, 16, 16), position: (25, 66, 25),
           meridians: 24, parallels: 16,
           background: documentColors[4])
}
```

## Primitive Properties

| Property | Description |
|----------|-------------|
| `position` | placement `(x, y, z)` |
| `size` | extents `(w, h, d)` |
| `radius` | UV sphere radius `(x, y, z)` |
| `background` | base color |
| `faces` | `{ top:, bottom:, front:, back:, left:, right: }` per-face colors |
| `contour-color` | wireframe color |
| `meridians` / `parallels` | sphere tessellation |
| `turns` | helix turns |
| `line-color` | helix line color |
| `stroke` | stroke style (`basic(width: 2)`) |

## Example: Isometric Layout

```tson
content-slide(title: "3D Primitives", variant: 4) {
    body {
        grid(4, 2, columns-weight: [1, 1.8]) {
            styles {
                "*": { margin: 2 }
                source: { at: left }
            }
            miniPage {
                scene3d(draw-contour, transform: { scale: 0.75 }) {
                    box(position: (-22.5, -17.5, -12.5),
                        size: (45, 35, 25),
                        background: documentColors[3])
                }
            }
        }
    }
}
```