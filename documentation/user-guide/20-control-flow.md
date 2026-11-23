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
`${...}` interpolation takes a variable name (`${y}`) or a function call (`${fixed(y, 2)}`); any other expression (`${y * 2}`) stays literal.

## String Interpolation

```tson
count = 7
text("Pending tasks: ${count}")
```

A function call is evaluated too, so a sentence can format and check the numbers it quotes
(the numeric functions below need `import("stdlib#1.2.0.0")`):

```tson
text("fr = ${fixed(div(oems_fr, 1e9), 3)} GHz, ${within-pct(oems_fr, sheen_fr, 1)} from the reference")
```

The placeholder ends at the first `}`, so the call cannot contain one; quote strings with
single quotes inside a double-quoted text (`${claim(x, '<', 1)}`). If the function is unknown
(missing import) or an argument is an unbound name, the placeholder stays literal, like an
unbound variable, and the unknown function is logged as a warning.

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
| `min`, `max` | numeric helpers (`stdlib`, see below) |

### Numeric functions (`import("stdlib#1.2.0.0")`)

For numbers computed from results (solver metrics such as `oems_fr`, `oems_minDb`).
Arrays are flattened by the aggregates. A value bound to the empty string (a solver metric
not available yet) makes the function render nothing; a non-numeric value is logged as an
error and rendered as `[name: reason]` in place of the number; NaN renders as `n/a`.

| Function | Description |
|----------|-------------|
| `fixed(x, decimals = 2)` | fixed decimals, half-up: `fixed(7.4675, 3)` → `7.468` |
| `sig(x, digits = 3)` | significant digits, trailing zeros kept: `sig(7.4, 3)` → `7.40` |
| `signed(x, decimals = 2)` | like `fixed` with an explicit sign: `+0.42` |
| `add(a, b, ...)`, `sub(a, b)`, `mul(a, b, ...)`, `div(a, b)`, `abs(x)` | arithmetic (`div` reports a zero divisor) |
| `pct-diff(value, ref)` | signed relative difference in percent, `100 (value - ref) / abs(ref)` |
| `min(...)`, `max(...)`, `mean(...)` | over numbers and arrays; NaN if any value is NaN |
| `spread(...)`, `spread-pct(...)` | `max - min`, and the same in percent of the mean |
| `claim(value, op, bound, decimals?)` | checked claim: renders the value if `value op bound` holds (`op`: `'<' '<=' '>' '>=' '==' '!='` or `lt le gt ge eq ne`), else `[CLAIM FAILED: ...]` plus a render error |
| `within-pct(value, ref, tol, decimals = 2)` | checked claim: renders `pct-diff` as `+0.42%` if `abs(pct-diff) <= tol`, else `[CLAIM FAILED: ...]` plus a render error |

```tson
text("OpenEMS lies ${within-pct(oems_fr, sheen_fr, 1)} from Sheen's resonance;
the three solvers spread by ${fixed(spread-pct(oems_fr, scuff_fr, palace_fr), 1)}%.")
```

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
