# Components and Reuse

ntexup components are defined with `@define` and reused with a call syntax. Components support parameters with defaults, string interpolation, and a `body` slot for caller content.

## Defining a Component

```tson
@define info-badge(label, count: 1, accent: documentColors[0]) {
    group(margin: 1, size: (100, 22)) {
        rectangle(background: accent, round-corner: 4)
        text("${label}: ${count}", color: white, font-bold, font-size: 2.2%P, at: center)
    }
}
```

## Calling a Component

```tson
info-badge(label: "Pending Tasks", count: 7, accent: documentColors[0])
```

- Parameters with defaults (`count: 1`) are optional.
- Arbitrary expressions work as arguments.

## String Interpolation in Components

Use `${name}` to embed parameter values:

```tson
@define banner(titleText, subtitleText) {
    text("${titleText}")
    text("${subtitleText}")
}
```

## The `body` Slot

A component can wrap caller-provided content with `body`:

```tson
@define feature-box(title, accent: documentColors[1]) {
    group(margin: 1) {
        rectangle(background: documentSurface, color: documentLine,
                  draw-contour: true, round-corner: 6)
        rectangle(background: accent, position: (0, 0), size: (0.35%P, 100))
        text("${title}", position: (6, 14), origin: left,
             font-bold, font-size: 2.4%P, color: accent)
        group(position: (6, 32), size: (88, 62), origin: top-left) {
            body
        }
    }
}
```

Usage:

```tson
feature-box(title: "Custom Component Box", accent: documentColors[1]) {
    ul {
        • "Defined via `@define name(args){ ... }`"
        • "Embeds caller content using `body`"
        • "Reusable across slides and layouts"
    }
}
```

## Conditional Rendering with Components

```tson
@define status-badge(ok) {
    rectangle(size: (80, 30),
              background: (ok ? "green" : "red")) {
        text(ok ? "OK" : "KO")
    }
}
```

Or with an if:

```tson
@define demo(showBox, titleText) {
    page {
        text(titleText, at: top)
        if (showBox) {
            rectangle(size: (100, 60), color: documentColors[1], at: center, down: 25)
        }
    }
}
```

> Plain `page`/`body` children are absolutely positioned and overlap unless laid out with `grid` or explicit `at`/`position` — give every element a placement in custom components.

## Reusing Across Files

Organize components into a `lib/` or `03-lib/` folder and include it before pages:

```
03-lib/
└── components.ntx
```

```tson
// main.ntx
include("03-lib/*.ntx")
include("02-pages/**/*.ntx")
```

Components in a file included earlier are usable in files included later.

## Theme/Predefined Components

Themes ship built-in components. Observed in doc-slides:

| Component | Purpose |
|-----------|---------|
| `card(accentColor: ...)` | auto-styled container with accent bar |
| `kicker(text)` | small overline label inside a card |
| `stat(value:, label:)` | KPI/number box |
| `quote(author: ...)` | block quote with attribution |
| `hint-info(title)` | info callout |
| `hint-warning(title)` | warning callout |
| `panel` | light canvas panel |
| `content-slide`, `cover-slide`, `agenda-slide`, `thankyou-slide` | page scaffolding |
| `miniPage`, `miniCodeNative` / `miniCodeNTexup` | doc-slide preview helpers |

```tson
card(accentColor: documentColors[1]) {
    stat(value: "99.9%", label: "Availability SLA")
}

quote(author: "Alan Kay") {
    text("The best way to predict the future is to invent it.",
         font-size: 2.2%P, font-italic)
}

hint-warning("Warning Callout") {
    text("Highlights cautions or critical notices with warning accent colors.")
}
```

## Best Practices

- Name components with dashed/camelCase (`info-badge`, `feature-box`).
- Provide sensible defaults for optional parameters.
- Use `${name}` interpolation rather than concatenation where possible.
- Place shared components in an included library file.
- Avoid shadowing built-in node types when naming components.

## Related

- [Control Flow](20-control-flow.md) — `@define` and variables
- [Multi-file Projects](21-multi-file-includes.md) — libraries
- [Styling](17-styling-system.md) — colors and palette