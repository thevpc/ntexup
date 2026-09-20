# Styling System

ntexup uses CSS-like style rules. Styles select nodes by **type**, **class**, or the universal selector, and cascade from themes → styles blocks → inline properties.

## Inline Styles

```tson
text("Hello", color: red, font-size: 24, font-bold)
```

## The `styles` Block

```tson
styles {
    "*":    { font-size: 10, color: black }   // or `(*)`
    source: { font-size: 3%P, background: "#0a192f", font-family: monospaced }
    page:   { background: "#ffffff" }
    class-bordered:        { stroke: basic(width: 2, dash: [8,4]) }
    class-card-item(bordered): { margin: (4, 0), at: left }
}
```

Selectors:

| Selector | Meaning |
|----------|---------|
| `"*"` (or `(*)`) | all elements |
| `page`, `source`, `text`, ... | by node type |
| `class-my-item` | **declares** a reusable style class |
| `class-my-item(base1, base2)` | declares a class inheriting from the listed base classes |
| `table-row(header\|even\|odd)` | table rows (`even`/`odd` count over body rows only) |
| `table-column(n)` | cells of the 1-based column `n` |
| `table-cell(row: r, col: c)` | the cell at 1-based row `r` / column `c` |

Legacy dot selectors (`.H1`) are no longer supported; declare `class-H1:` in a
`styles` block and apply it with `@(H1)`.

Nodes opt into a class with a class annotation:

```tson
@(card-item) text("Hello")
```

or with the `class` property:

```tson
text("Hello", class: card-item)
```

Classes resolve like regular rules with a single-level flattening: the closest
enclosing `styles` block wins, `class-a(b, c)` merges its base classes (declared
left to right, later base wins) plus its own properties, and the result behaves
like one virtual rule at the usage site. Inline node properties always win, and
an explicit type selector (e.g. `text { ... }`) beats a class at the same
distance.

## Theme Digital Colors

Themes expose a **12-color accent palette** through `documentColors`:

```tson
text("Accent 1", color: documentColors[0])
text("Accent 2", color: documentColors[1])
rectangle(size: (30, 30), background: documentColors[4], color: documentColors[2])
```

| Variable | Meaning |
|----------|---------|
| `documentColors[0..11]` | palette colors |
| `documentSurface` | panel/surface color |
| `documentLine` | line color |

You can set the active accent in `main.ntx`:

```tson
documentColorAccent = 1   // 0..11
```

## Color Functions

From the `common-functions` extension (`import("common-functions")`):

| Function | Usage |
|----------|-------|
| `linearGradientColor(c1, c2[, angle])` | gradient fill |
| `lighterColor(color, amount)` | lighten |
| `darkerColor(color, amount)` | darken |
| `complementaryColor(color)` | complementary |
| `invertColor(color)` | invert |
| `rotateHueColor(color, degrees)` | hue rotation |
| `sinebowColor(t)` | sinebow palette |

```tson
rectangle(background: linearGradientColor(documentColors[1], documentColors[4], 45))
rectangle(background: darkerColor(documentColors[0], 0.4))
rectangle(background: complementaryColor(documentColors[1]))
```

## Property Reference by Category

### Typography

| Property | Example |
|----------|---------|
| `font-family` | `font-family: monospaced` |
| `font-size` | `font-size: 2%P` |
| `font-bold` | bare flag |
| `font-italic` | bare flag |
| `font-underlined` | bare flag |
| `text-wrap` | `text-wrap: wrap` |
| `text-align` | `text-align: justify` |

### Colors

| Property | Example |
|----------|---------|
| `color` | text/contour color |
| `background` | fill color |
| `background-color` | fill color (solids) |
| `line-color` | stroke color |
| `grid-color` | grid lines (with `draw-grid`) |

### Drawing

| Property | Example |
|----------|---------|
| `stroke` | `stroke: dashed` / `stroke: basic(width: 2, dash: [8,4], cap: round)` |
| `draw-contour` | bare flag |
| `draw-grid` | bare flag (grids) |
| `round-corner` | `round-corner: 8` |
| `shadow` | `shadow: { distance:(4,4), radius:6 }` |
| `dash` | pattern |

### Sizing / Positioning

| Property | Example |
|----------|---------|
| `size` | `size: (100, 50)` |
| `at` | `at: center` |
| `position` | `position: (50, 50)` |
| `origin` | `origin: left` |
| `margin` | `margin: 10` / `margin: (5, 30)` / `margin: (5,30,0,0)` |
| `padding` | `padding: (1%P, 0.5%P)` |
| `rotate` | `rotate: -45` |

### Grid Layout

| Property | Example |
|----------|---------|
| `columns-weight` | `columns-weight: [1, 3]` |
| `rows-weight` | `rows-weight: [1, 2]` |
| `row-weight` / `col-weight` | per-cell weight |
| `colspan` | `rectangle(colspan: 2)` |
| `rowspan` | `rectangle(rowspan: 2)` |
| `draw-grid` | `grid(3, 2, draw-grid)` |
| `line-color` | grid line color |

## Precedence

1. Theme default styles (lowest)
2. User `styles` blocks
3. Node inline properties
4. Style classes

Later-defined style blocks override earlier ones.

## Font Sizing in Page Context

Font sizes are often relative to the page using `%P`:

```tson
text("Title", font-size: 4%P)
text("Body", font-size: 2%P)
text("Caption", font-size: 1%P)
```

## Debugging Bounds

Draw the computed bounds of elements with `debug`:

```tson
group(debug: true) {
    circle(size: 60, color: red)
}
```

(Requires the renderer debug support; if unsupported it is ignored.)

## Example: Complete Override

```tson
styles {
    // Base
    "*": { font-family: "SansSerif", font-size: 2%P }

    // Page
    page: { background: documentSurface }

    // Content
    class-title: { font-size: 4%P, font-bold, color: documentColors[1] }
    ul:    { font-size: 2%P, margin: 2 }
    source:{ font-size: 3%P, background: "#0a192f", at: left }
}

@(title) text("Title")
@(title) text("Nested title")
```

## Source-Code Token Colors

The `source` element uses themed token properties:

| Property | Description |
|----------|-------------|
| `source-primary-color` | base text color |
| `source-keyword-color` | keywords (optional `-font-bold`) |
| `source-string-color` | strings |
| `source-comments-color` | comments |
| `source-number-color` | numbers |
| `source-separator-color` | punctuation |