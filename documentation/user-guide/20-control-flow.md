# Control Flow

ntexup's TSON supports variables, loops, conditionals, expressions, includes, and custom functions.

## Variables

### Assignment (`=`) vs Assign-if-Defined (`:=`)

| Operator | Meaning |
|----------|---------|
| `x = value` | **unconditional** assignment — always sets `x` |
| `x := value` | **assign-if-not-defined** — only sets `x` if it is currently undefined (null) |

`:=` is the way to declare **default values** that a caller can override *before* the include that sets them.

```tson
// In a theme/library file — defaults
documentTitle := "My Document"
documentColorAccent := 0
themeShowPalette := false
```

```tson
// In main.ntx — overrides applied BEFORE the include
themeName = "eniso"
documentColorAccent = 3            // beats the theme default `:= 0`
documentTitle = "ntexup"
include("github://thevpc/ntexup-templates/${themeName}/v1.0/theme")
// theme's `documentColorAccent := 0` is ignored: already defined (3)
```

If `main.ntx` leaves the variable unset, the theme's `:=` default is used instead.

### Palette Variables

```tson
documentColorAccent = 1        // 0..11 active accent index
accentColor = documentColors[documentColorAccent]
```

## Expressions

```tson
x = 10
y = x * 2 + 5
text("Total: " + y)
```

Operators: `+ - * / %` (`+` also concatenates strings: `"Total: " + y`), comparisons `== != < > <= >=` (numbers or strings), `&& || !` (short-circuit).
There is no ternary operator (`a ? b : c` is reported as `unsupported operator`); use `if`.
`${...}` interpolation takes a variable name only (`${y}`), not an expression (`${y * 2}` stays literal).

## String Interpolation

```tson
count = 7
text("Pending tasks: ${count}")
```

## Loops

```tson
for (i: [1, 2, 3]) {
    circle(size: 50, at: (i * 30, 0))
}

for (i: [1 -> 5]) {
    text("item ${i}")
}
```

The loop header is a **pair** `var: domain` (`for(i in ...)` is rejected with `unexpected for(...)`; and no `@`: an
`@for` annotation is silently ignored and its body is emitted once, uninterpolated).

Range `[1 -> 5]` yields 1..5 (inclusive; `[5 -> 1]` counts down); add `step`:
`[0 -> 100 step 10]`. A range can be mixed with plain items: `[0, 2 -> 4, 9]`.

### Nested Loops

```tson
for (row: [0 -> 2]) {
    for (col: [0 -> 3]) {
        ...
    }
}
```

## Conditionals

```tson
if (x > 10) {
    text("x is greater than 10")
}
```

## Functions

### Expression helpers

| Function | Description |
|----------|-------------|
| `dtimes(start, stop, count)` | sample points for plots |
| `either(a, b)` | first non-null argument |
| `min`, `max` | numeric helpers |

### Color functions (`import("stdlib")`)

| Function | Description |
|----------|-------------|
| `linearGradientColor(c1, c2, angle?)` | gradient |
| `lighterColor(c, amt)` / `darkerColor(c, amt)` | shade |
| `complementaryColor(c)` / `invertColor(c)` | invert/complement |
| `rotateHueColor(c, deg)` | hue rotation |
| `sinebowColor(t)` | palette |

## Includes and Imports

```tson
include("01-styles/*.ntx")
include("02-pages/**/*.ntx")
include("github://thevpc/ntexup-templates/${themeName}/v1.0/theme")

import("shapes2d", "plantuml", "plot2d")
```

## Scope (fragment / block / group)

| Container | New variable scope? | Renders as node? |
|-----------|:-------------------:|:----------------:|
| `fragment` | No (leaks) | No (dissolves) |
| `block` | Yes (isolated) | No (dissolves) |
| `group` | Yes (isolated) | Yes |

```tson
x = 0
block {
    x = 10        // isolated
}
text(x)           // 0
```

## Complete Example

```tson
themeName = "eniso"
documentColorAccent = 2                 // override theme's `:=` default
include("github://thevpc/ntexup-templates/${themeName}/v1.0/theme")

titles = ["Introduction", "Methods", "Results"]

for (i: [0 -> 2]) {
    content-slide(title: "Chapter ${i + 1}") {
        body {
            text(titles[i], font-size: 4%P, font-bold)
        }
    }
}
```

## Custom Functions via `@define`

```tson
@define add(a, b) {
    a + b
}

text(add(3, 4))
```
