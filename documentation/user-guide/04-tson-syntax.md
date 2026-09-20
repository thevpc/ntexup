# TSON Syntax

TSON (**Type Safe Object Notation**) is a versatile, human-readable **data format** — a strict superset of JSON (spec: github.com/thevpc/tson). ntexup uses TSON as the syntax of `.ntx` files and builds its document language on top of it: node calls (`text(...)`), variables, loops, conditions, imports, and reusable components are ntexup-level features expressed through TSON, not TSON itself.

## Literal-First Rule

TSON uses a **literal-first** parsing rule. When TSON encounters a name followed by content, it first tries literal (constant) interpretation — a bare word is a string (`color: red`), a number is a number — and only then checks functions/variables.

## Basic Constructs

### Plain Text Strings

```tson
"Hello World"
text("Hello World")
```

### Triple-Quoted Multi-line Strings

```tson
"""
Hello
World
"""
```

### Raw Line String (`¶`)

The mark `¶` introduces a **raw, single-line string literal** that consumes everything up to the end of the line:

```tson
¶ Hello World               // one line only
```

`¶` needs no quotes and has no interpretation whatsoever: no escapes and no special characters — what you type is exactly what renders.

Every other string form is **quoted**:

```tson
"Hello World"

"Hello
World"                       // quoted strings may span several lines
```

Quoted strings differ from `¶` in two ways: they can span **multiple lines**, and they support **escaping** — a character is written literally by **repeating the separator** (e.g. `""` for a literal quote inside `"..."`).

For multi-line raw text, use the **`¶¶`** marker on every consecutive line (common indentation is stripped):

```tson
¶¶ Line 1 of a long text.
¶¶ Line 2 of the same text.
```

Text that is **not wrapped into a component** (bare `¶` lines, bare quoted strings) is rendered as **plain text** — no formatting, no markers. To get rich-text formatting, wrap the string in a component such as `text()`:

```tson
¶ Hello World            // plain text
"Hello World"            // plain text (quoted, but still bare)
text("Hello **World**")  // component -> markers are processed
```

### Lists (bullet markers)

TSON bullet markers denote list items; ntexup renders them as `ul`/`ol`. A run of markers is a list even **without** an explicit `ul`/`ol` component:

```tson
• Item one
• Item two
```

| Markers | Semantic | Rendered as |
|---------|----------|-------------|
| `•` `●` (and `[.]` `[..]`) | unordered | `ul` |
| `▪` `■` (and `[#]`) | ordered | `ol` |

Nest lists by **repeating the marker**:

```tson
▪ Step 1
▪▪ Sub-step A
▪▪ Sub-step B
▪ Step 2
```

Cross-prefix nesting is allowed (an ordered item can contain unordered sub-items and vice-versa).

### Numbers

```tson
size: 50
size: (100, 100)
```

### Booleans / Bare Flags

A bare word property acts as `true`:

```tson
rectangle(fill)
text("Bold", font-bold)
grid(draw-grid)
```

Or with explicit value:

```tson
rectangle(fill: false)
text("Bold", font-bold: true)
```

### Comments

```tson
// single-line comment
# also a comment
```

### Object / Map Literal

```tson
shadow: { distance: (4,4), radius: 6, color: gray }
transform: { scale: 0.75, rotate: (0, 45, 0) }
```

### Array Literal

```tson
columns-weight: [1, 1.5, 2]
points: [(0,0), (50,0), (100,20)]
```

### Range Literal

```tson
[1 -> 5]             // 1,2,3,4,5
[1 -> 10 step 2]     // 1,3,5,7,9
```

## Document Directives

### Document Structure

An `.ntx` file is a plain TSON document — no root wrapper required. A typical `main.ntx`:

```tson
themeName = "eniso"
documentColorAccent = 0

include("01-styles")
include("02-pages")
```

### Assignments

- `x = value` — **unconditional** assignment, always sets `x`
- `x := value` — **assign-if-not-defined**: only sets `x` if it is currently undefined. Used to declare defaults that can be overridden *before* the include defining them.

```tson
// in a theme — defaults
documentTitle := "My Document"
themeShowPalette := false

// in main.ntx — override BEFORE the include
documentTitle = "ntexup"
documentColorAccent = 3
include("github://thevpc/ntexup-templates/eniso/v1.0/theme")
// theme's `documentColorAccent := 0` no longer applies (already defined)
```

### Variables

```tson
x = 42
str = "hello"
b = true
lst = [1,2,3]
obj = { a: 1 }
```

Reference later:

```tson
text(str)
text(x + 1)
@if (x > 40) { ... }
```

## Elements (Nodes)

A node is a named element with optional properties and children.

### Node with Properties

```tson
rectangle(size: (100, 60), color: red, background: documentColors[4])
```

### Node with Properties and Children

```tson
group(at: center) {
    circle(size: 50, background: blue)
    text("Centered", at: center)
}
```

### Bare String as Child

A bare string child is rendered as text:

```tson
grid(2,2) {
    "Hello"
    "World"
}
```

### Positional Arguments

```tson
grid(2, 3)          // 2 columns, 3 rows
source(java, """...""")
eq("x^2=\sin(\pi x)")
```

### Style/Prop Shorthands (bare flags)

```tson
circle(filled, size: 50)
rectangle(draw-contour)
text("Bold", font-bold)
```

## Rich Text Markers (inside text components)

Rich-text markers are processed **inside text components** (`text()`, `ntf()`, and inside `ul`/`ol` items wrapped in `text()`). Bare `¶` or bare quoted strings render as plain text — markers are **not** processed there:

```tson
¶ Hello ##World##              // literal: "Hello ##World##"
"Hello ##World##"              // literal (bare string)
text("Hello ##World##")        // marker -> subtitle
```

| Marker | Meaning |
|--------|---------|
| `**text**` | bold |
| `__text__` | italic |
| `#text#` | title level 1 |
| `##text##` | title level 2 |
| `###text###` | title level 3 |
| `####text####` | title level 4 |
| `[[eq: X^2=\sin(\pi x)]]` | inline LaTeX equation |
| `[[ntf: ##:p1: hello##]]` | inline NTF flavor |

Inline equation:

```tson
text("""
    Equation 1 = [[eq: X^2=\sin(\pi x)]]
    Bold and [[eq: E=mc^2]] inline
""")
```

## Functions

### Calling Functions

```tson
text("Area = " + (10 * 10))
background: linearGradientColor(documentColors[0], documentColors[2])
```

### ntexup built-in / extension functions

| Function | Description |
|----------|-------------|
| `dtimes(start, stop, count)` | generate x samples for plots |
| `linearGradientColor(c1, c2[, angle])` | color gradient |
| `lighterColor(c, amount)` | lighten a color |
| `darkerColor(c, amount)` | darken a color |
| `complementaryColor(c)` | complementary color |
| `invertColor(c)` | invert a color |
| `rotateHueColor(c, degrees)` | rotate hue |
| `sinebowColor(t)` | sinebow palette color |
| `either(a, b)` | first non-null |

## Expressions

```tson
x = 10
y = x * 2 + 5
text("Total: " + y)
```

Operators: `+ - * / %`, comparisons, `&& || !`, ternary `cond ? a : b`.

## String Interpolation (`${...}`)

```tson
count = 7
text("Pending: ${count}")
```

Used extensively in components:

```tson
@define info-badge(label, count: 1) {
    text("${label}: ${count}")
}
```

## Conditions

```tson
if (x > 10) {
    text("x is greater than 10")
}
```

## Loops

```tson
for (i : [1, 2, 3]) {
    circle(size: 50, at: (i * 30, 0))
}

for (i : [1 -> 5]) {
    text(i)
}
```

## Reusable Components

### Define a Component

```tson
@define info-badge(label, count: 1, accent: documentColors[0]) {
    group(margin: 1, size: (100, 22)) {
        rectangle(background: accent, round-corner: 4)
        text("${label}: ${count}", color: white, font-bold, at: center)
    }
}
```

### Call a Component

```tson
info-badge(label: "Pending Tasks", count: 7, accent: documentColors[1])
```

### `body` Slot

Components can embed caller content with `body`:

```tson
@define feature-box(title, accent: documentColors[1]) {
    group(margin: 1) {
        rectangle(background: documentSurface, color: documentLine, draw-contour: true, round-corner: 6)
        rectangle(background: accent, position: (0, 0), size: (0.35%P, 100))
        text("${title}", position: (6, 14), origin: left, font-bold, color: accent)
        group(position: (6, 32), size: (88, 62), origin: top-left) {
            body
        }
    }
}

feature-box(title: "My Box") {
    ul {
        "Content here"
    }
}
```

## Includes and Imports

```tson
include("01-styles/*.ntx")
include("02-pages/**/*.ntx")
include("github://thevpc/ntexup-templates/${themeName}/v1.0/theme")
```

```tson
import("shapes2d")
import("plantuml")
```

## Styles Block

```tson
styles {
    "all":       { font-size: 10, color: black }
    class-title: { font-size: 48, bold: true }
    class-note:  { color: blue }
    source:      { at: left }
}
```

## Theme Variables

Themes expose variables available in every document:

| Variable | Meaning |
|----------|---------|
| `documentColors[0..11]` | 12-color accent palette |
| `documentColorAccent` | index of the active accent |
| `documentSurface` | surface (panel) color |
| `documentLine` | line/border color |
| `themeName` | active theme |

## Data Model

A document (a tree of nodes) contains **nodes**, each with:

- `type` — element type (`page`, `circle`, `grid`, ...)
- `props` — named properties (`color`, `size`, `at`, ...)
- `children` — nested elements or strings

## Key Token Notes

- `:` (`color: red`) — property assignment in a node
- `=` (`x = 42`) — variable definition or assignment
- `:=` — assign-if-not-defined (default values, overridable before includes)
- `%` — percentage of the parent component (e.g., `size: 15%`); equivalent to no suffix because all sizes are relative
- `%P` — percentage of page (e.g., `font-size: 3%P`, `size: 15%P`)
- `¶` — raw single-line string (ends at end of line; no escapes/markers)
- `¶¶` — raw multi-line paragraph (prefix each line with `¶¶`; no escapes/markers, common indentation stripped)
- `•` / `●` — unordered list marker, rendered as `ul` (repeat for nesting: `••`, `•••`)
- `▪` / `■` — ordered list marker, rendered as `ol` (repeat for nesting: `▪▪`, `▪▪▪`)