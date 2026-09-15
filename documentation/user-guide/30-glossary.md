# Glossary

## A

**Anchor** — a reference point (`at`) used to place an element relative to its parent/page edges or corners (e.g., `center`, `top-left`).

## B

**Block** — a scoped container that isolates variables and dissolves at render time (children promoted to parent).

## C

**Component** — a reusable piece of content defined with `@define`, accepting parameters and optionally a body.

**Compilation** — the process of turning `.ntx` (TSON) source into a `NTxCompiledDocument` of pages ready for rendering.

**CSS-like styles** — ntexup's styling model: rules selecting by type/class with `{ property: value }`.

## E

**Element / Node** — a document item such as `text`, `circle`, `grid`, `page`. Nodes have type, properties, and children.

**Equation** — a LaTeX-rendered formula (`eq(...)`).

**Expression** — a TSON value that computes something (`x * 2 + 1`).

**Extension** — a Java module registered via SPI that adds node builders, functions, or renderers.

## F

**Flavor / Text Flavor** — a text markup dialect (Markdown-like, LaTeX, NTF) that the text renderer can parse.

**Fragment** — a transparent, non-scoping container that dissolves at compile time.

**Function** — a named callable available in expressions, e.g. `either(...)`, `md(...)`.

## G

**Glob** — a wildcard pattern for including files (`*.ntx`, `**/*.ntx`).

**Grid** — a table-style layout container with columns, rows, spans, and weights.

**Group** — a positioned container; scoping and retained in the render tree.

## I

**Import** — loading a Java extension module (`import("plantuml")`).

**Include** — textually combining another `.ntx` file or glob into the current document (`include("...")`).

## L

**Literal-first** — TSON parsing rule: try the constant interpretation of a token before treating it as a function/variable.

**Loop** — `@for (i in [1 -> 5])` repeated element generation.

## M

**Margin** — spacing outside an element's bounds.

**Multiple-file project** — a project organized into `01-styles`, `02-pages`, `03-lib` folders combined in `main.ntx`.

## N

**node** — see Element.

**NTF** — NTx Text Format, a lightweight markup used by the `ntf` element.

**ntexup** — the tool itself: a text-based document & presentation generator.

**Nuts** — the Java package manager that installs and runs ntexup.

## O

**Ordered list (`ol`)** — a numbered list.

**Origin** — the reference point of an element within its own bounds.

## P

**Page** — a slide in presentations; a page in reports. The container of all content.

**Padding** — spacing inside an element bounds, before children.

**Palette** — a set of 12 accent colors in themes; `documentColorAccent` selects one.

**Parser** — component that reads TSON into document elements.

**PlantUML** — external tool integrated for diagrams (UML, gantt, wireframe, etc.).

**Plot** — `plot2d` chart of functions/data.

**Position** — absolute coordinates `(x, y)` of an element.

## R

**Renderer** — module producing an output (screen, PDF, HTML, web, image).

**Renderer context** — state during rendering: bounds, styles, flags.

## S

**Selector** — the "who" part of a style rule (`page`, `(.class)`, `*`).

**Scene3D** — a container for 3D primitives with camera and transform.

**Source** — a syntax-highlighted code block element.

**SPI (Service Provider Interface)** — Java mechanism used by extensions to register builders/functions.

**Style rule** — `selector: { properties }`.

**Styles block** — a `styles { ... }` section declaring multiple rules.

## T

**Template** — a ready-made project structure or a reusable content pattern (`@define`).

**Theme** — a collection of styles, components, images shared via `include`.

**TSON** — Type Safe Object Notation; a versatile data format and strict superset of JSON (spec: github.com/thevpc/tson). ntexup uses it as the syntax of `.ntx` files; loops, conditions, functions, and components are ntexup features layered on top of TSON.

## U

**Unordered list (`ul`)** — a bulleted list.

## V

**Variable** — a named value (`x = 42`) usable in expressions.

**Assign-if-not-defined (`:=`)** — sets a variable only if it is undefined; used to declare default values that an earlier/upstream assignment can override before the include.

## Z

**Zero-padding** — numeric folder/file prefixes (`0001-`, `0010-`) that control ordering.