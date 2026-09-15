# Quick Start

This guide walks you through creating your first ntexup presentation from scratch.

## Setup

Follow the [Installation guide](02-installation.md) first, then continue here.

## Create a Project

```bash
mkdir my-first-project
cd my-first-project
nuts ntexup new -t=classic
```

This generates a project with the classic theme:

```
.
├── 01-styles
│   └── 001-styles.ntx
├── 02-pages
│   ├── 0001-intro
│   │   ├── 0001-cover.ntx
│   │   └── 0010-plan.ntx
│   └── 9999-conclusion
│       ├── 9901-conclusion.ntx
│       └── 9999-thankyou.ntx
└── main.ntx
```

## Open the Viewer

```bash
nuts ntexup show .
```

This launches the Swing viewer. It **auto-refreshes** whenever you save changes to any `.ntx` file.

## Anatomy of `main.ntx`

```tson
themeName = "classic"
documentColorAccent = 1

include("github://thevpc/ntexup-templates/${themeName}/v1.0/theme")
include("01-styles")
include("02-pages")
```

The theme variables configure the look; `include` stitches styles and pages together.

## Anatomy of a Page

A single slide/page file:

```tson
content-slide(title: "My First Slide", variant: 2) {
    body {
        grid(1, 2) {                    // 1 column, 2 rows
            text("Hello, ntexup!", font-size: 3%P)
            text("This is my first presentation.")
        }
    }
}
```

Or a raw page — but note that `page` children are **absolutely positioned** by default and would draw on top of each other, so wrap content in a layout container such as `grid`:

```tson
page {
    grid(1, 4) {                    // 1 column, 4 rows (vertical stack)
        text("Why ntexup?")
        ¶ This is a paragraph
        eq("x = \\frac{-b \\pm \\sqrt{b^2-4ac}}{2a}", at: center)
        rectangle(size: (100, 60), color: cornflowerblue, at: center)
    }
}
```

Each `grid` entry gets its own cell, so nothing overlaps. See [Pages and Layout](06-pages-layout.md) for `grid`, `columns-weight`/`rows-weight`, and `colspan`/`rowspan`.

## Try a Live Example

Create `demo.ntx` with this content:

```tson
import("shapes2d")

content-slide(title: "Welcome", variant: 2) {
    body {
        grid(1, 5) {                    // 1 column, 5 rows -> one element per cell
            text("Welcome to ntexup!", font-size: 4%P, font-bold, at: center)
            circle(size: 80, background: gold, at: center)
            ¶ First item
            ¶ Second item
            eq("e^{i\\pi} + 1 = 0", at: center)
        }
    }
}
```

## Render to PDF

```bash
nuts ntexup pdf . -o output.pdf
```

## Render to HTML

```bash
nuts ntexup html . -o output/
```

The above commands assume your CLI build supports them; if not, use the on-screen viewer:

```bash
nuts ntexup show .
```

Then render it:

```bash
nuts ntexup show demo.ntx
```

## Next Steps

- Read the [TSON Syntax](04-tson-syntax.md) reference
- Explore the [Shapes documentation](12-shapes-2d.md)
- Learn about the [Styling System](17-styling-system.md)
- Check the [Rendering options](23-rendering-output.md)