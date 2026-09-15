# Text and Typography

## Raw Line String (`¶`)

`¶` introduces a **raw single-line string** that runs to the end of the line — no quoting, no escapes, no special characters, everything is literal:

```tson
¶ First paragraph
¶ Second paragraph
```

A `¶` line is **equivalent to `text("...")`** — both render the same plain text:

```tson
¶ Hello World
```

renders exactly like `text("Hello World")`. Reach for quoted strings (escaping + multi-line), `¶¶` (multi-line raw paragraphs), or `text()` (rich-text markers) when you need more than a single plain line:

```tson
¶¶ Line 1 of a paragraph
¶¶ Line 2 of the same paragraph

text("Hello **World**")   // markers are processed here
```

Anything **not wrapped in a component** — bare `¶` lines and bare quoted strings alike — renders as plain text (no markers):

```tson
¶ Welcome to ntexup        // plain text
"Welcome to ntexup"        // plain text (quoted, but bare)
text("Welcome to ntexup")  // explicit component
```

## The `text` Element

The explicit text component — same plain content as a raw line, but it also processes rich-text markers (see below):

```tson
text("Hello, World!")
text("Hello, **World**!")
```

A bare string child renders as text too:

```tson
grid(2,2) {
    "Hello World"
}
```

## `plain` — Simple Text

`plain` renders raw text without markdown flavoring (multi-line strings keep their formatting):

```tson
plain("Hello World", font-size: 10)
plain("Hello World", font-italic, font-size: 10)
plain("""
Hello
World
""", font-bold, font-size: 10)
plain("""
Hello  World
This    Is    Me
""", font-family: monospaced, font-size: 5)
```

## `ntf` — NTx Text Format

`ntf` renders the NTx flavored markup, including palette and style tokens:

```tson
ntf("""
##Hello## ### World ###
""")

ntf("""
##:p1 color 1## ##:p2 color 2##
##:/ italic##   ##:info hint##
""")
```

`##:p1##`..`##:p10##` use the theme palette colors; `##:/ italic##` applies italic.

The NTF markup is specified at <https://github.com/thevpc/nuts/blob/master/documentation/specifications/ntf.md>.

## Rich Text Markers

Rich-text markers are processed **inside text components** (`text()`, and `ntf()` for the NTF flavor) — never inside bare `¶` or bare quoted strings:

| Marker | Meaning |
|--------|---------|
| `**text**` | bold |
| `__text__` | italic |
| `#Title 1#` | heading level 1 |
| `##Title 2##` | heading level 2 |
| `###Title 3###` | heading level 3 |
| `[[eq: X^2=\sin(\pi x)]]` | inline equation |
| `[[ntf: ##:p1: hello##]]` | inline ntf flavor |

```tson
text("""
    **Bold X**
    __Italic Y__
    #Title 1#
    ##Title 2##
    ###Title 3###
""")
```

## Text Properties

| Property | Description | Example |
|----------|-------------|---------|
| `font-family` | font family | `font-family: monospaced` |
| `font-size` | size (points or `%P`) | `font-size: 3%P` |
| `font-bold` | bold (bare flag) | `font-bold` |
| `font-italic` | italic (bare flag) | `font-italic` |
| `font-underlined` | underline | `font-underlined` |
| `color` | text color | `color: documentColors[1]` |
| `text-wrap` | wrapping | `text-wrap: wrap` |
| `text-align` | alignment | `text-align: justify` |
| `margin` | spacing around | `margin: [10, 20]` |

```tson
text("Hello World", font-italic)
text("Hello World", font-bold)
text("Hello World", font-bold, color: documentColors[1], font-size: 2%P)
```

## Text Wrap

Use `text-wrap: wrap` with a width-bounded parent:

```tson
text("""
    Left aligned: wraps cleanly at the specified width boundaries.
""", text-wrap: wrap, text-align: left)

text("""
    Justified text layout distributes available space evenly
    across all words on each line.
""", text-wrap: wrap, text-align: justify)
```

## Text Alignment

```tson
text("Left", text-wrap: wrap, text-align: left)
text("Right", text-wrap: wrap, text-align: right)
text("Centered", text-wrap: wrap, text-align: center)
text("Justified", text-wrap: wrap, text-align: justify)
```

## Inline Equations in Text

Melee text (mixed prose + formulas):

```tson
text("""
    Melee text supports mixed plain words, **bold text**,
    and inline LaTeX formulas like [[eq: E=mc^2 ]]
    or [[eq: a^2+b^2=c^2 ]] with smooth wrapping.
""", text-wrap: wrap, text-align: justify)
```

## Mixing Inline Flavors

Inline `[[eq: ...]]` and `[[ntf: ...]]` renderers can be mixed freely with plain text and other markers in a **single string**:

```tson
text("expression [[eq: E = mc^2]] [[ntf: ##:p1: hello##]] world", text-wrap: wrap)
```

- `[[eq: ...]]` — inline LaTeX equation (JLaTeXmath)
- `[[ntf: ...]]` — inline NTF markup (palette/`###` tokens, italic …)

Both are processed by the text flavor, exactly like the other rich-text markers.

## Positioning Text

```tson
text("At center", at: center)
text("Top-left", at: top-left)
text("Above bottom", at: bottom, up: 30)
```

## Theme Typography Helpers

Themes provide heading/component helpers when available:

```tson
card(accentColor: documentColors[0]) {
    kicker("CARD COMPONENT")
    ul {
        • "Auto-styled container"
        • "Integrated left accent bar"
    }
}
```

See [Components](19-components-reuse.md) for theme components like `quote`, `panel`, `hint-info`, `hint-warning`, `stat`.

## Styling Text via Styles Blocks

```tson
styles {
    "*":    { font-size: 2%P }
    "title":{ font-size: 4%P, font-bold }
    source: { at: left }
}
```

## Example: Complete Typography Slide

```tson
content-slide(title: "Text", variant: 3) {
    body {
        grid(4, 2, columns-weight: [1, 2]) {
            styles {
                "*": { margin: 2 }
                source: { at: left }
            }
            miniPage {
                text("""
                    **Bold X**
                    __Italic Y__
                    #Title 1#
                    ##Title 2##
                    ###Title 3###
                """)
            }
            miniPage {
                text("""
                    Equation = [[eq: X^2=\sin(\pi x) ]]
                """)
            }
        }
    }
}
```