# Equations (LaTeX)

## Overview

ntexup renders LaTeX-style math equations with the **eq** element (alias `equation`). JLaTeXmath provides the typesetting.

## Basic Usage

```tson
eq("X^2=\sin(\pi x)")
```

TSON strings are **literal-first**: backslashes are passed verbatim to the
LaTeX renderer. Write regular LaTeX commands with **single** backslashes:

```tson
eq("x \frac{-b \pm \sqrt{b^2-4ac}}{2a}")
```

Do **not** double-escape. `\\frac` makes the renderer see a `\\` (line-break)
command at the start of the math and fails with `Problem with command \ at
position 0:1`. The only legitimate use of `\\` is as a row separator inside a
matrix/array environment (`\begin{array}`, `\begin{pmatrix}`, `\begin{bmatrix}`).

When in doubt, start from `eq("x^2")` and build up (see the troubleshooting
guide).

## Unicode-friendly math

ntexup accepts superscripts/subscripts and symbol characters directly:

```tson
eq("X²=\sin(πx)")
eq("E=mc²")
```

## Sizing

```tson
eq("X^2=\sin(\pi x)", font-size: 3%P)
```

## Common LaTeX Commands

| Command | Result |
|---------|--------|
| `\frac{a}{b}` | fraction |
| `\sqrt{x}` | square root |
| `^` `{}` `_{}` | superscript, subscript |
| `\sum_{k=1}^{n}` | summation |
| `\int_{a}^{b}` | integral |
| `\sin`, `\cos`, `\pi`, `\alpha` | functions & Greek |
| `\left( \right)` | auto-sized brackets |

## Inline Equations in Text (`[[eq: ...]]`)

Embed equations inside regular text strings:

```tson
text("""
    Equation 1 = [[eq: X²=\sin(πx) ]]
    Equation 2 = [[eq: X^2=\sin \left( \pi x \right) ]]
    Equation 3 = [[eq: X^2=\sin(x) ]]
""")
```

This is the idiomatic way to mix prose and math.

## Equations in Grids / Mini Pages

```tson
grid(1, 2) {
    eq("X^2=\sin(\pi x)", draw-contour: false, font-size: 3%P)
}
```

## Style Properties

| Property | Effect |
|----------|--------|
| `font-size` | scale (points or `%P`) |
| `draw-contour` | draw math bounding box (`false` typically) |
| `color` | math color |

## Complete Slide Example

```tson
content-slide(title: "Latex Equations", variant: 3) {
    body {
        grid(4, 2, columns-weight: [1, 2]) {
            styles {
                "*":   { margin: 2 }
                "*":   { font-size: 1%P }
                source: { at: left }
            }
            column {
                eq("X^2=\sin(\pi x)", draw-contour: false, font-size: 3%P)
            }
        }
    }
}
```

## Typing Shortcuts

- `X²` works for `X^2`
- `π` works for `\pi`
- `[[eq: ...]]` required for inline math in text strings