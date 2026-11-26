---
name: ntx-editing
description: Author, edit, debug and render ntexup documents (.ntx slide decks written in TSON). Use whenever a task touches a .ntx file or an ntexup deck (a folder of .ntx files, typically main.ntx + 01-styles/ + 02-pages/), asks for slides/presentations made with ntexup, or asks to produce/check a PDF or images of such a deck. Covers syntax, themes/templates, components, and the render-and-verify loop.
license: MIT
compatibility: Requires the `nuts` CLI on PATH (ntexup runs as `nuts ntexup ...`), JDK 17+. PDF/PNG rendering works headless; the live viewer needs a display.
metadata:
  author: thevpc
  version: "2.0"
---

# Editing ntexup (.ntx) documents

ntexup turns `.ntx` files (TSON syntax: `name(args){ children }`) into slides,
rendered to a live viewer, PDF, PNG or HTML. **Never trust a deck edit until you have
rendered it and looked at the pages**. The engine keeps going after most errors and still
writes the PDF/PNG with the broken element missing, so rely on the exit code and the
error summary, then look at the images.

Detailed references (read on demand, not up front):
- [references/syntax.md](references/syntax.md) — every construct with verified examples
- [references/troubleshooting.md](references/troubleshooting.md) — error messages → fixes
- Upstream user guide: `ntexup/documentation/user-guide/*.md` in the ntexup repo
  (https://github.com/thevpc/ntexup) — `26-property-reference.md`, `27-node-type-reference.md`
- Real decks to copy patterns from: https://github.com/thevpc/ntexup-doc-slides
- TSON spec: https://github.com/thevpc/tson/blob/main/SPEC.md

## 1. Setup check

```bash
nuts --version || curl -s https://thevpc.net/nuts/install-latest.sh | bash   # then restart the shell
nuts -Zy ntexup --help >/dev/null && echo ok                                 # installs ntexup on first use
```

Always pass `-y` (non-interactive) to nuts from an agent; without it nuts may prompt and hang.
On the maintainer's machine always use `-Zy` (`-Z` resets the workspace so freshly
built jars are picked up; it fixes `NoSuchMethodError`/stale-runtime failures).
Needs ntexup ≥ 1.2.0 for `check`, the error summary/exit code, `--diagnostics` and
labelled image sheets (older versions exit 0 even when errors were logged).

## 2. The edit → render → inspect loop (do this every time)

```bash
# 1. fast validation: renders every page in memory, writes nothing
nuts -Zy ntexup check DECK

# 2. look at it: all pages as labelled contact sheets (4x2 pages per image)
nuts -Zy ntexup image --grid=4x2 --page-labels -o target/check/ DECK     # -> sheet-01.png ...

# 3. zoom on a suspicious page at full size
nuts -Zy ntexup image --pages=7 -o target/check/p7.png DECK
```

Every render/check ends with `ntexup: N error(s), M warning(s)` plus one line per
problem (`error   [file.ntx] message`), and **exits 1 when errors were logged**.
Add `--strict` to also fail on warnings, and `--diagnostics=out.json` for a
machine-readable report (`status`, counts, `outputs`, `diagnostics[{level,source,message}]`).

Then **open the sheet images and look**: overflow, overlap, empty areas, wrong colours,
literal `${var}` or `**` left in text. Each page is captioned `page N · file.ntx`, the
file that produced it (for theme components, the file that *called* the component),
which is the file to edit.

| Goal | Command |
|---|---|
| Validate only | `nuts -Zy ntexup check DECK` (`--pages=2-5`, `--strict`, `--diagnostics=f.json`) |
| PNG per page | `nuts -Zy ntexup image -o out/ DECK` (`--pages=2-5`, `--dpi=N`, `--format=jpg`, `--page-labels`) |
| N-up sheets | `nuts -Zy ntexup image --grid=COLSxROWS --page-labels -o out/ DECK` |
| PDF | `nuts -Zy ntexup pdf -o out/deck.pdf DECK` (`--page-size=a4`, `--landscape`, `--grid=2x2`; `--pages` is ignored by pdf) |
| HTML | `nuts -Zy ntexup html DECK` (→ `./dist/html`) |
| Live viewer (needs X11, auto-reloads) | `nuts -Zy ntexup show DECK` |
| Parsed model | `nuts -Zy ntexup dump DECK` |
| Override a variable | `--var-documentColorAccent=3` |

`DECK` is a single `.ntx` file or a folder: all its `.ntx` files are loaded, any names allowed;
`main.ntx`, if present, comes first and the others follow alphabetically.
Pages compile lazily: an error inside one `page{}` only shows up when that page renders,
so always render **all** pages before declaring success.

## 3. Starting a deck

```bash
mkdir my-talk && cd my-talk
nuts -Zy ntexup new -t=classic:medium     # template: classic|meridian|lucid|eniso|ibtihel ; layout: small|medium|large
nuts -Zy ntexup list-templates
```

- `small` = one `main.ntx`; `medium` = `main.ntx` + `pages/`; `large` = `main.ntx` + `01-styles/` + `02-pages/<NNNN-section>/<NNNN-slide>.ntx`.
- Files/folders are zero-padded (`0010-…`) because **includes resolve in sorted source order**:
  styles and `@define`s must be included before the pages using them.
- Theme without scaffolding: `include("github://thevpc/ntexup-templates/classic/v1.0/theme")`
  (cloned with embedded JGit into the nuts cache; first run needs network).

`main.ntx` sets identity variables **before** the theme include (theme uses `:=` defaults):

```tson
documentTitle = "My Talk"
documentSubtitle = "..."
documentAuthor = "Jane Doe"
documentAuthorEmail = "jane@example.com"
documentAuthorAffiliation = "Lab"
documentChapter = "Chapter 1"
documentDate = "2026-11-19"
documentColorAccent = 3          // 0..11, rotates the palette
include("github://thevpc/ntexup-templates/classic/v1.0/theme")
include("01-styles")             // large layout
include("02-pages")
```

## 4. Theme slide components (classic & friends)

```tson
cover-slide()                                  // uses document* variables
agenda-slide(title: "Plan"){
    body{
        ol{
            ¶ Intro
            ¶ Results
        }
    }
}
content-slide(title: "My Slide"){ text("Direct content") }             // implicit default body
content-slide(title: "One col"){ body{ ... } }                        // explicit layout
content-slide(title: "Two cols"){ body2a{ ... } body2b{ ... } }       // 2 columns
content-slide(title: "Three"){ body3a{...} body3b{...} body3c{...} }
conclusion-slide(title: "Conclusion"){ body{ ul{ ¶ point } } }
thankyou-slide()
```

Direct content uses the component's opt-in `@default-body(body, body2a, ...)` metadata and goes to the first listed body. Explicit listed layouts remain available; definitions can also use bare `slot`.

Theme helper components (all bundled themes; classic, meridian and eniso also have `section-slide(title, number:, subtitle:)`): `card`, `kicker(text)`, `divider`, `stat(value, label)`,
`quote(author:)`, `panel`, `callout(title, accentColor, bgColor)`,
`hint-info(title)` / `hint-warning` / `hint-error` / `hint-success`, `image-fig(src, title:, legend:)`.
Read the theme file for exact args before using one:
`~/.cache/nuts/**/ntexup-templates/<theme>/v1.0/theme/*.ntx` or the ntexup-templates repo.

Without a theme, write raw `page{ ... }` and `import(...)` the extensions yourself (see §6).

## 5. Core syntax cheat-sheet (all verified by rendering)

```tson
// comments: // line   /* block */
x = 10                 // assign ;  x := 10  assigns only if x is undefined (defaults)
label = "n=" + x       // arithmetic and string + work; ${...} below takes a NAME only
page {
    text("Hello **bold** *italic* ***both*** `code` [[eq: \frac{a}{b}]] ${x}", at: top, font-size: 5%P)
    ¶ raw one-line text, no markup, runs to end of line
    eq("\sum_{i=1}^n i = \frac{n(n+1)}{2}", at: center)          // LaTeX, no double escaping
    circle(size: 20, background: documentColors[2], position: (25, 60), origin: center)
    grid(2, 2){ "a" "b" "c" "d" }                                  // cols, rows ; children fill row by row
    ul{
        ¶ one          // ¶ runs to END OF LINE: one item per line, never ¶ a ¶ b }
        ol{ "nested" "items" }   // quoted strings can share a line
    }
}
for(i: [1 -> 3]){ text("item ${i}") }          // pair header ; [a -> b step s] ; [0, 2 -> 4, 9]
if(x > 5 && x != 7){ text("big") }            // == != < > <= >= && || ! ; no ternary
@define badge(label, accent: blue){            // reusable component
    group{ rectangle(background: accent, round-corner: 4)  text("${label}", color: white, at: center) }
}
badge(label: "Hi", accent: green)
styles{
    (*) { font-size: 4%P }                     // or "*": {...}
    text { color: documentTextPrimary }
    @hot { color: red, font-bold }            // declare a class ...
}
@hot text("hot")                               // ... use it (or text("hot", class: hot))
source(java """
class A {}
""", font-size: 2%P)                           // lang is a bare name, no comma before the code
```

Rules that bite:
- **Strings are literal**: `\n` is two characters, `\frac` needs no escaping. Use a real newline
  or `"""…"""` for multi-line. Only the closing quote char can be escaped.
- **Markup only inside `text()`** (MTF format): `**b**`, `*i*`, `***bi***`, `` `code` ``,
  `##:p3 palette color##`, `##:/ italic##`, `##:_ underline##`, `##:+ bold##`, `\*` to escape.
  `__x__` and `~~x~~` are NOT markup. Bare `"..."` / `¶` lines are plain text.
- **`¶` eats the rest of the line**, closing braces included. `ul{ ¶ a ¶ b }` on one line
  is ONE item and leaves `{` unclosed — later pages silently nest inside it. One `¶` per line,
  or use quoted strings. (ntexup ≥1.2 logs `syntax error : missing rbrace`.)
- **Flags are bare names**: `font-bold`, `draw-contour` — never quote them.
- **Units**: no suffix = points; `%` = % of parent; `%P` = % of page (use `%P` for fonts).
  `position`/`size` numbers inside a container are percentages of it.
- **Loops**: `for(i: …){}` only. `@for(...)` silently renders the body once with `${i}` literal;
  `for(i in …)` errors.
- **Classes**: declare `@name { ... }` or `@child(base1, base2) { ... }` in `styles`; apply with
  `@name`, repeated annotations, `@(a, b)`, or `class: [a, b]`. Legacy `class-*` declarations and
  dot selectors (`".x"`) are rejected.
- **Imports**: every node type comes from an extension; `undefined node ul` means a missing
  `import("layout")`. Themes import everything; raw decks need e.g.
  `import("stdlib" "layout" "text" "shapes2d" "latex" "plot2d" "plantuml" "image" "svg")`.
- Image paths are relative to the **current .ntx file**, not `main.ntx`.
- Use theme variables instead of hex: `documentColors[0..11]`, `documentSurface`,
  `documentLine`, `documentTextPrimary`, `documentBg`.

## 6. Where to find more

| Need | Look at |
|---|---|
| Shapes, arrows, connectors, 3D, plots, PlantUML, images, tables | [references/syntax.md](references/syntax.md) |
| An error message | [references/troubleshooting.md](references/troubleshooting.md) |
| Every property of an element | upstream `26-property-reference.md` |
| A working example of a feature | ntexup-doc-slides `02-pages/0040-elements/**` |

When unsure of a construct: copy a pattern from the doc deck, render a 1-page scratch
deck with `ntexup check` / `image`, then port it. Do not invent syntax from memory.

## 7. Done checklist

1. `nuts -Zy ntexup check DECK` exits 0 (`ntexup: 0 error(s)`).
2. You looked at every changed page (labelled sheets); nothing clipped/overlapping/literal.
3. Page count is what you expect.
4. If the user wants a deliverable: `nuts -Zy ntexup pdf -o out/deck.pdf DECK` (exit 0) and
   report the PDF path.
5. If you hit an engine bug (crash, documented syntax not working), say so with the
   minimal repro deck — and fix ntexup itself if you are working in its repo.
