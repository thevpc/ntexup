# Pages and Layout

## The `page` Element

Every slide (presentation) or page (report) is a `page` node. In practice, projects use **theme components** that wrap `page`:

- `cover-slide()` — cover page
- `content-slide(title: "...", variant: 2)` — content slide
- `agenda-slide(title: "...")` — agenda/overview page
- `thankyou-slide(title: "Bye")` — closing page

```tson
// theme-based
cover-slide()

content-slide(title: "Rationale", variant: 2) {
    body {
        ul(bullet-align: top) {
            ¶ Text-based, declarative syntax
        }
    }
}
```

The component name depends on the theme (classic, meridian, lucid, eniso, ibtihel). Raw `page` is always available:

```tson
page {
    text("My Slide")
}
```

> `page` children are absolutely positioned and overlap by default — lay them out with `grid`/`group`, or give each element an explicit `at`/`position`.

## Slide Components

### Cover Slide

```tson
cover-slide()
```

The cover draws document metadata (title, subtitle, author…) set in `main.ntx`:

```tson
documentTitle = "ntexup"
documentSubtitle = "A declarative, text-based document & presentation generator"
documentAuthor = "thevpc"
documentDate = "2026-09-13"
documentVersion = "v1.1.0.0"
```

### Content Slide

```tson
content-slide(title: "Introduction", variant: 2) {
    body {
        // slide content
    }
}
```

`variant` selects alternate layouts of the theme.

### Agenda Slide

```tson
agenda-slide(title: "Agenda") {
    body {
        ul {
            ¶ Topic 1
            ¶ Topic 2
        }
    }
}
```

### Thank-you Slide

```tson
thankyou-slide(title: "Bye")
```

## Layout Containers

### `grid` — Table Grid

```tson
grid(2, 2) {
    "A" "B"
    "C" "D"
}
```

> First argument is **columns**, second is **rows**.

#### Grid with weights

```tson
grid(4, 2, columns-weight: [1, 3]) {
    ...
}
grid(2, 3, columns-weight: [1, 1.2], rows-weight: [1, 2]) {
    ...
}
```

#### Per-child grid weights

Use `grid-weight` on a child when the layout should follow the content hierarchy
instead of repeating weight arrays on the container. It sizes both the child's
column and row:

```tson
grid(2, 2) {
    rectangle(grid-weight: 3) { text("Main") }
    rectangle(grid-weight: 1) { text("Sidebar") }
    rectangle(grid-weight: 1) { text("Details") }
    rectangle(grid-weight: 2) { text("Summary") }
}
```

Use `col-weight` or `row-weight` to override just one axis. A positive matching
entry in `columns-weight` or `rows-weight` still takes precedence over all child
weights. Missing, zero, negative, and non-finite child weights fall back to the
default track weight of `1`.

#### Grid lines

```tson
grid(3, 2, draw-grid, line-color: documentColors[0]) {
    ...
}
```

#### Spanning cells

```tson
grid(3, 2, draw-grid) {
    rectangle(colspan: 2, background: documentColors[0]) {
        text("Header (colspan: 2)")
    }
    rectangle() { text("A") }
    rectangle() { text("B") }
}

grid(2, 2, draw-grid) {
    rectangle(rowspan: 2) { text("Side") }
    rectangle() { text("Top") }
    rectangle() { text("Bottom") }
}
```

#### Empty spacer cell

```tson
grid(3, 2, draw-grid) {
    rectangle(colspan: 3) { text("Banner") }
    rectangle() { text("L") }
    void()
    rectangle() { text("R") }
}
```

### `col` — Vertical Stack

```tson
col {
    text("Top", grid-weight: 22)
    text("Bottom", grid-weight: 74)
}
```

### `row` — Horizontal Row

```tson
row {
    text("Left", grid-weight: 1)
    text("Right", grid-weight: 3)
}
```

`row` and `col` are grid layouts too, so they read the same per-child
`grid-weight`, `col-weight`, and `row-weight` properties.

### `group` — Positioned Container

A `group` holds children at absolute positions; it can be placed and sized as a unit:

```tson
group(margin: 1, size: (100, 22)) {
    rectangle(background: documentColors[0], round-corner: 4)
    text("Label", at: center)
}
```

`group` **creates a variable scope** and is **retained** in the render tree.

### `fragment` — Transparent Container

`fragment` dissolves at compile time; its children become direct siblings. It introduces **no variable scope**.

```tson
fragment {
    text("A")
    text("B")
}
```

### `block` — Scoped Container (Dissolves)

`block` creates a **new variable scope** but dissolves at render time:

```tson
page {
    block {
        x = 42
        text(x)
    }
    // x is not accessible here
}
```

### `fragment` vs `group` vs `block`

| Container | New variable scope? | Renders as node? | Use case |
|-----------|:-------------------:|:----------------:|----------|
| `fragment` | No | No (dissolves) | Pure organization |
| `group` | Yes | Yes (retained) | Visual container |
| `block` | Yes | No (dissolves) | Temporary scope |

### `void`

An invisible spacer used to fill grid cells or reserve space.

```tson
grid(3, 2, draw-grid) {
    rectangle() { text("L") }
    void()
    rectangle() { text("R") }
}
```

### `miniPage` / `miniCodeNative` (theme helper)

Documentation slides use theme helpers to preview content:

```tson
miniPage {
    rectangle(at: center, size: (30, 30), background: documentColors[4])
}
miniCodeNative("""rectangle(size:(30,30))""")
```

These come with the doc-slides theme.

## Placement Properties

### `at` — Anchor Position

```tson
text("Hello", at: top-left)
text("Hello", at: top)
text("Hello", at: top-right)
text("Hello", at: left)
text("Hello", at: center)
text("Hello", at: right)
text("Hello", at: bottom-left)
text("Hello", at: bottom)
text("Hello", at: bottom-right)
```

### `position`, `origin`

```tson
rectangle(origin: (50, 50), position: (50, 50), size: (30, 30))
rectangle(origin: left, position: (6, 14))
```

`origin` may also take anchor words (`left`, `center`, `top-left`) instead of a point.

### Offsets

> There are no anchor-offset properties (`up`/`down`/`leftOf`/`rightOf` are rejected as
> `invalid argument`). To place something relative to an anchor, use `position` + `origin`:

```tson
text("Below title",     position: (50, 20), origin: top)      // ~ at: top, pushed down
text("Above bottom",    position: (50, 90), origin: bottom)
text("Left of center",  position: (30, 50), origin: right)
text("Right of center", position: (70, 50), origin: left)
```

## Size Properties

```tson
rectangle(size: 100)                 // 100x100
rectangle(size: (200, 100))          // 200 wide, 100 tall
rectangle(size: 15%P)                // 15% of page
rectangle(size: (100, 50))           // points
```

`font-size` is commonly specified as page percentage:

```tson
text("Title", font-size: 3%P)
```

## Units

| Suffix | Meaning | Example |
|--------|---------|---------|
| (none) | points | `size: 50` |
| `%` | percent of parent | `size: 50%` |
| `%P` | percent of page | `size: 15%P`, `font-size: 3%P` |

## Margin and Padding

```tson
rectangle(size: (30, 30), margin: 10)
rectangle(size: (30, 30), margin: (5, 30))        // (h, v)
rectangle(size: (30, 30), margin: (5, 30, 0, 0))  // (top, bottom, left, right)
```

Bracket syntax also accepted: `margin: [10, 20]`.

## Rotation

```tson
arrow(at: center, rotate: -45)
```

`rotate` is in degrees, clockwise.

## Combined Layout Example

```tson
content-slide(title: "Layout Showcase", variant: 4) {
    body {
        grid(4, 2, columns-weight: [1, 1.5]) {
            styles {
                "*": { margin: 2 }
            }
            miniPage {
                group {
                    rectangle(at: top, size: (50, 50), background: documentColors[4], margin: 10)
                    triangle(at: left, size: (50, 50), background: documentColors[5], margin: 10)
                }
            }
        }
    }
}
```