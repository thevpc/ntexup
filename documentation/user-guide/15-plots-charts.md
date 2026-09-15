# Plots and Charts

The `plot2d` extension provides 2D plots of functions. Import it:

```tson
import("plot2d")
```

## Basic Plot (curve)

```tson
plot2d(x: dtimes(-5, 5, 100)) {
    curve {
        f(x): sin(x)
        title: 'sin'
    }
}
```

`dtimes(start, stop, count)` produces the x samples.

## Plots with Multiple Traces

```tson
plot2d(x: dtimes(-5, 5, 100)) {
    curve {
        f(x): sin(x)
        title: 'sin'
    }
    area {
        f(x): cos(x)
        title: 'cos'
    }
}
```

## Area Plot

```tson
plot2d(x: dtimes(-10, 10, 500)) {
    area {
        f(x): "100*abs(cos(pow(abs(x),1.5))/(1+0.2*abs(x)))"
        title: "my function"
    }
}
```

## Plot Elements

| Element | Description |
|---------|-------------|
| `curve` | line chart of `f(x)` |
| `area` | area chart of `f(x)` |

Each trace defines:

| Property | Description |
|----------|-------------|
| `f(x)` | the function expression to plot |
| `title` | legend label |

## Function Syntax

Expressions support standard math functions:

```
sin(x) cos(x) tan(x)
sqrt(x) pow(x, n) abs(x)
exp(x) log(x)
```

Strings also accepted: `f(x): "100*abs(cos(...))"`.

## Complete Example

```tson
content-slide(title: "Plot lines", variant: 8) {
    body {
        grid(2, 1, columns-weight: [1, 3]) {
            styles {
                "*": { margin: 2 }
                source: { at: left }
            }
            miniPage {
                plot2d(x: dtimes(-10, 10, 500)) {
                    area {
                        f(x): "100*abs(cos(pow(abs(x),1.5))/(1+0.2*abs(x)))"
                        title: "my function"
                    }
                }
            }
            miniPage {
                plot2d(x: dtimes(-5, 5, 100)) {
                    curve { f(x): sin(x) title: 'sin' }
                    area  { f(x): cos(x) title: 'cos' }
                }
            }
        }
    }
}
```

## In Grids and Groups

Plots compose with any layout:

```tson
grid(2, 2, draw-contour, background: white) {
    plot2d(x: dtimes(-5, 5, 100)) {
        curve { f(x): sin(x) title: 'sin' }
    }
}
```

## Notes

- Ranges use `dtimes` sampler (start, stop, count).
- `curve` draws lines; `area` fills beneath the curve.
- The plot honors theme colors when no explicit colors are provided.