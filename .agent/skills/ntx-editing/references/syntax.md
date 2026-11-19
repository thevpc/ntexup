# ntexup syntax reference

Every example here was rendered with ntexup 1.2 (`nuts ntexup check` / `image`). Inside a theme
(`classic`, `eniso`, …) all extensions are already imported; in a raw deck add the
`import(...)` named in each section.

Contents: 1 TSON basics · 2 Document & pages · 3 Text · 4 Lists · 5 Layout · 6 Positioning
· 7 Shapes & strokes · 8 Lines, arrows, connectors · 9 Images · 10 Equations · 11 Source code
· 12 Diagrams (PlantUML) · 13 Plots · 14 3D · 15 Styles & classes · 16 Components
· 17 Variables, expressions, control flow · 18 Includes & imports

## 1. TSON basics

```tson
name(positional, key: value, flag){ children }   // node / call
name(args)                                       // no children
name{ children }                                 // no args
"string"  'string'  """multi
line"""                                         // all strings are literal (no \n escapes)
¶ raw text up to END OF LINE (eats any } after it: one ¶ per line)
42  3.5  12%  3%P  0xFF                          // numbers, suffixes kept as units
[1, 2, 3]      (x, y)      {k: v}                // array, tuple, object
x = 1   x := 1                                   // assign / assign-if-undefined
// line comment   /* block comment */
```

Commas between arguments/children are always optional when there is no ambiguity
(`f(a b c)` = `f(a, b, c)`); add one only to separate tokens that would otherwise merge.
Bare identifiers (`center`, `red`, `font-bold`) are names — don't quote flags.

## 2. Document & pages

```tson
page { ... }                       // explicit page
page(background: "#fafafa") { ... }
// content outside any page is auto-paginated
```

Deck = a single `.ntx` file, or a folder of `.ntx` files (any names). In a folder, `main.ntx`
(if present) is loaded first, then the other files alphabetically.

## 3. Text  (`import("text")` for ntf/source; plain text is core)

```tson
text("Hello **bold** *italic* ***both*** `code`")       // MTF markup
text("Palette ##:p2 colored## ##:/ italic## ##:_ underline## ##:+ bold##")
text("Escape \*\*literal\*\*")
text("Inline math [[eq: \frac{a}{b}]] or \(x^2\)")      // needs import("latex")
text("Value: ${x}")                                      // interpolation
plain("no markup at all", font-italic)
ntf("""##:p1 color 1## ##:p2 color 2##""")
¶ plain line, no markup
"bare string = plain text"
text("Long text …", text-wrap: wrap, text-align: justify)   // left|right|center|justify
```

Font props: `font-size: 3%P`, `font-family: monospaced`, `font-bold`, `font-italic`,
`font-underlined`, `font-strike`, `color: documentTextPrimary`.

## 4. Lists  (`import("layout")`)

```tson
ul{
    ¶ First
    ¶ Second
    ul{ ¶ nested }
}
ol{ "one" "two" }                              // quoted items may share a line
ul(bullet-align: top){                         // bullet aligned with first wrapped line
    text("long item …", text-wrap: wrap)
}
```

Theme classes to restyle items: `class-ul-item`, `class-ol-item`, `class-ul-bullet`.

## 5. Layout containers  (`import("layout")`)

```tson
group{ ... }                                  // overlay children in one box (new var scope)
grid(3, 2){ a b c d e f }                     // cols, rows; filled row by row
grid((2, 2), columns-weight: [1, 3], rows-weight: [1, 1], draw-grid){ ... }
grid(3, 2){ rectangle(colspan: 2){...} x y }  // colspan / rowspan
col(rows-weight: [22, 78]){ top bottom }      // vertical stack
row{ left right }                             // horizontal stack
fragment{ ... }                               // grouping only (no scope, no node)
block{ ... }                                  // own var scope, no node
```

## 6. Positioning & size

```tson
text("x", at: top-left)        // top, top-right, left, center, right, bottom-left, bottom, bottom-right
rectangle(position: (30, 40), origin: top-left, size: (20, 10))   // % of parent box
// no up/down/leftOf/rightOf offsets: use position + origin instead
rectangle(size: 50)            // square
rectangle(margin: 2, padding: (1, 2))
rectangle(rotate: 30)
```

Units: bare number = % of parent for position/size inside containers; `%P` = % of page
(use for `font-size`).

## 7. Shapes & strokes  (`import("shapes2d")`)

```tson
rectangle(size: (50, 20), background: documentColors[4], color: documentColors[2])
square(size: 50)  circle(size: 50)  ellipse(size: (80, 30))  triangle(size: 50)
rhombus(size: 50)  parallelogram(size: 50)  trapezoid(size: 50)
pentagon() hexagon() octagon() polygon(count: 8)
rectangle(round-corner: 4, draw-contour, line-color: documentLine)
rectangle(draw-contour, stroke: dashed)
rectangle(draw-contour, stroke: basic(width: 2, dash: [8, 4], cap: round))
rectangle(shadow: {distance: (4, 4), radius: 6, color: gray})
rectangle(background: linearGradientColor(documentColors[1], documentColors[4], 45))  // stdlib
rectangle(background: darkerColor(documentColors[0], 0.4))                           // stdlib
```

`background` (alias `background-color`) = fill; `color` = text/contour; `line-color` = contour.

## 8. Lines, arrows, connectors  (`import("shapes2d")`)

```tson
line(from: (10, 10), to: (80, 90), end-arrow: simple(), color: red, stroke: 3)
line(from: (10, 10), to: (80, 90), start-arrow: triangle(), end-arrow: triangle-full())
rectangle(name: "a", position: (2, 10), origin: top-left, size: (16, 12), draw-contour){ text("API", at: center) }
rectangle(name: "b", position: (40, 30), origin: top-left, size: (16, 12), draw-contour){ text("DB", at: center) }
connector("a", "b", end-arrow: simple(), stroke: 2)                  // auto anchors
connector(from: "a@right", to: "b@left", end-arrow: triangle())     // explicit sides
connector("a", "b", via: (24, 75), end-arrow: simple())              // polyline
connector("a", (92, 72), end-arrow: simple())                        // node -> free point
connector("a", "b", flow: dots(count: 3, size: 1.2, period: 3))      // animated (viewer only)
```

## 9. Images  (`import("image" "svg" "animated-gif")`)

```tson
image("images/logo.png", size: (50, 50), at: center)    // png jpg gif svg avif webp
```

Paths are relative to the **.ntx file containing the call**. GIFs animate only in the viewer.

## 10. Equations  (`import("latex")`)

```tson
eq("\sum_{i=1}^n i = \frac{n(n+1)}{2}", font-size: 4%P)
eq("X² = sin(πx)")                     // unicode works too
```

Write LaTeX exactly as-is; never double the backslashes.

## 11. Source code  (`import("text")`)

```tson
source(java """
public class A {
    void f() { System.out.println("x"); }
}
""", font-size: 2%P, at: top-left)
source(ntexup, file: "example.ntx")    // from a file
```

Language is a bare name right after `(`, then the code — **no comma** between them.

## 12. Diagrams  (`import("plantuml")`)

```tson
uml(at: center, size: (100, 60), """
left to right direction
actor "User" as u
rectangle App {
  usecase "Login" as UC1
}
u --> UC1
""")
gantt("""
[Design] lasts 5 days
[Code] lasts 10 days
[Code] starts at [Design]'s end
""")
```

Also `nwdiag(...)`, `wireframe(...)`. Body is plain PlantUML.

## 13. Plots  (`import("plot2d")`)

```tson
plot2d(x: dtimes(-5, 5, 100)) {
    curve { f(x): sin(x)  title: 'sin' }
    area  { f(x): "100*abs(cos(x))"  title: 'area' }
}
```

`dtimes(start, stop, count)` samples x.

## 14. 3D  (`import("shapes3d")`)

```tson
scene3d(draw-contour, real-position: (-22.5, -17.5, -12.5), real-size: (45, 35, 25),
        transform: { rotate(to: (0, 100, 0), angle: 35)  scale: 0.75 }){
    box(position: (-22.5, -17.5, -12.5), size: (45, 35, 25), background: documentColors[3],
        faces: { top: {background: documentColors[0]} })
}
```

See ntexup-doc-slides `0035-shapes3d/` for spheres, cylinders, complex scenes.

## 15. Styles & classes

```tson
styles {
    (*)       { font-size: 4%P }               // all ("*": {...} also works)
    text      { color: documentTextPrimary }   // by node type
    page      { background: "#ffffff" }
    class-card: { round-corner: 4, draw-contour }
    class-hot(card): { color: red, font-bold } // class inheriting from card
}
@(hot) text("hot")
text("hot", class: hot)
grid(2, 2){ styles: { "*": { margin: 2 } }  ... }   // scoped to this container
```

Cascade: theme → enclosing `styles` (closest wins) → inline props (always win).
A type selector (`text { color: … }`) beats a class at the same distance — put the colour
inline or drop it from the type rule if a class must override it.
Dot selectors like `".hot"` are invalid.

Theme colour variables: `documentColors[0..11]`, `documentBg`, `documentSurface`,
`documentSurfaceAlt`, `documentLine`, `documentTextPrimary`, `documentTextSecondary`,
`documentTextMuted`, `semantic{Info,Warning,Error,Success}{Color,Bg}`.

## 16. Components (`@define`)

```tson
@define badge(label, accent: blue){
    group{
        rectangle(background: accent, round-corner: 4)
        text("${label}", color: white, at: center)
    }
}
badge(label: "Hi", accent: green)

@define feature-box(title, accent: documentColors[1]){
    group{
        rectangle(background: documentSurface, draw-contour, round-corner: 6)
        text("${title}", position: (6, 14), origin: left, font-bold, color: accent)
        group(position: (6, 32), size: (88, 62), origin: top-left){
            slot                                  // caller's children go here
        }
    }
}
feature-box(title: "Results"){ ul{ "faster" "smaller" } }
```

Named slots (as in themes): define `@define body{ group{ slot } }` inside the component and
the caller writes `body{ ... }`. Put shared `@define`s in a file included before pages.

## 17. Variables, expressions, control flow

```tson
n = 3
msg = "count: " + n
ok = n > 2
twice = n * 2
text("${msg} ${twice}")                     // ${...} takes a variable NAME only, not an expression
text("Total: " + twice)                      // + concatenates strings (ntexup >= 1.2)
for(i: [1 -> 5]){ text("item ${i}") }        // inclusive range
for(i: [10 -> 0 step 5]){ ... }              // 10, 5, 0
for(i: [0, 2 -> 4, 9]){ ... }                // 0,2,3,4,9
for(name: ["a", "b"]){ text(name) }
if(n > 2){ text("big") }
```

Operators: `+ - * / %`, `== != < > <= >=` (numbers or strings), `&& || !` (short-circuit),
parentheses. No ternary `? :`. Comparisons and logic need ntexup ≥ 1.2 — older engines
log `unsupported operator` and treat the condition as false.

Header is a pair `var: domain`. `@for(...)` is NOT a loop (body rendered once, `${i}` left
literal) and `for(i in ...)` is an error.

## 18. Includes & imports

```tson
include("01-styles")                         // folder: all .ntx, sorted
include("02-pages/**/*.ntx")                 // glob
include("github://thevpc/ntexup-templates/classic/v1.0/theme")
import("stdlib" "layout" "text" "shapes2d" "latex")
```

Includes run in source order at the point of the call: anything a page uses must be
included/defined earlier.
