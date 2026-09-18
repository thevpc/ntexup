---
name: "ntexup-presentation"
description: "Creates and edits ntexup presentations (.ntx files). Invoke when user wants to build, modify, or manage text-based slide decks using the ntexup declarative generator."
---

# NTexup Presentation Builder

This skill equips an agent to create, edit, and manage **ntexup** presentations — declarative, text-based slide decks written in `.ntx` (TSON syntax). ntexup excels at math-heavy, diagram-rich, and reusable teaching/research materials.

## Authoritative TSON Syntax Reference

The underlying format of every `.ntx` file is **TSON v2.0** (Type Safe Object Notation). The canonical spec is maintained at:

- **TSON SPEC.md**: <https://github.com/thevpc/tson/blob/main/SPEC.md>

Read the spec whenever a low-level TSON question comes up (e.g., "does `\n` mean newline?", "how do suffixes like `%P` work?", "what's the difference between `=` and `:=`?"). For ntexup-presentations, the most relevant TSON features are highlighted in §21 of this skill — you do NOT need to read the full 1200-line spec to write slides.

## Authoritative NTXUP Syntax Reference

The underlying format of every `.ntx` file uses component definitions as defined in

`$ntexup_repository_root/documentation/user-guide/`

---

## 1. When to Invoke This Skill

Invoke this skill when the user wants to:
- Create a **new** ntexup presentation (blank, from a template, or for a specific topic)
- **Edit** an existing `.ntx` presentation: add/remove/reorder slides, modify content, restyle
- Apply or switch **themes/templates** (classic, eniso, ibtihel, meridian)
- Insert specific elements: **equations**, **UML diagrams**, **shapes**, **plots**, **images**, **tables**, **custom components**
- Restructure a single-file deck into a multi-file modular project (or vice versa)
- Build/render the deck: open viewer, export PDF, or install editor syntax highlighting
- Understand or troubleshoot `.ntx` syntax errors

---

## 2. Prerequisites & Installation Check

Before doing anything else, verify the toolchain is available. If missing, help the user install:

```bash
# 1. Install Nuts package manager (shell pipe)
curl -s https://thevpc.net/nuts/install-latest.sh | bash
# Restart terminal after this so `nuts` is on PATH

# 2. Install ntexup via Nuts
nuts -y install ntexup

# 3. Verify installation + show built-in documentation slides
nuts ntexup documentation

# 4. (Optional) Install editor syntax highlighting for ntx files
nuts ntexup --install-editor-syntax vim kate intellij gedit vscode notepad++
```

Quick verification one-liner:

```bash
nuts ntexup --version
```

---

## 3. Creating a New Presentation

ntexup ships three project "sizes" (scaffolds) per theme. Pick the smallest one that satisfies the user's complexity.

Available themes: `classic` (safe default), `eniso`, `ibtihel`, `meridian`

### 3.1 Small — Single-File (Quick Tests / Short Decks)

```bash
nuts ntexup new --template=classic-small
```

Produces a single `main.ntx` file. Best for 1–10 slides, prototyping, or throwaway demos.

### 3.2 Medium — Multi-File (Recommended for Most Decks)

```bash
nuts ntexup new --template=classic-medium
```

Produces a folder with separate slide files. Best for 10–40 slide teaching / research decks.

### 3.3 Large — Complex / Modular (Full Courses)

```bash
nuts ntexup new --template=classic-large
```

Produces a well-organized project with `01-styles/`, `02-pages/`, `images/`, etc. Best for courses, conference-long content, or decks that will be maintained over months.

> **Recommendation for agents**: Always scaffold with `classic-medium` unless the user explicitly says the deck is tiny or huge. You can refactor the structure later.

### 3.4 Change Theme After Scaffolding

Edit the first lines of `main.ntx`:

```tson
themeName = "meridian"                 // classic | eniso | ibtihel | meridian
documentColorAccent = 3                // 0..11 — rotates the 12-color palette
documentTitle = "My Great Talk"
documentSubtitle = "Subtitle goes here"
documentAuthor = "Jane Doe"
documentAuthorEmail = "jane@uni.edu"
documentAuthorAffiliation = "Some Lab"
documentDate = "2026-09-15"
documentVersion = "v1.0"
themeShowPalette = true                // set true when debugging colors
```

---

## 4. Standard Project Layout (Large / Medium Template)

Understand this structure when editing an existing project. Agent MUST maintain consistency with it.

```
my-presentation/
├── main.ntx                    # ENTRY POINT — theme vars + includes
├── 01-styles/
│   └── 001-styles.ntx          # imports theme + local @define helpers
├── 02-pages/                   # slides live here; 1 file ≈ 1 slide or section
│   ├── 0001-cover/
│   │   └── 0001-page-cover.ntx
│   ├── 0010-intro/
│   │   └── 0010-agenda.ntx
│   ├── 0020-background/
│   │   ├── 0021-problem.ntx
│   │   └── 0022-related-work.ntx
│   ├── 0030-methodology/
│   │   ├── 0031-approach.ntx
│   │   └── 0032-algorithm.ntx
│   ├── 0040-results/
│   │   ├── 0041-charts.ntx
│   │   └── 0042-comparison.ntx
│   └── 9999-conclusion/
│       └── 9999-page-conclusion.ntx
└── images/                     # static assets — PNG, JPG, SVG, GIF, AVIF, WEBP
    ├── logo.svg
    └── diagram.png
```

**Conventions the agent MUST follow**:
- Slide folders/files are **zero-padded numbered** (4 digits → `0031`, not `31`) so alphabetical order == slide order.
- Group related slides under one numbered folder (e.g. `0030-methodology/` holds multiple `.ntx` files).
- `main.ntx` ends with `include("01-styles")` then `include("02-pages")`. ntexup auto-discovers nested `.ntx` files alphabetically when including a directory.

---

## 5. Main Entry Point (`main.ntx`) Anatomy

Every presentation starts with `main.ntx`. Structure:

```tson
// (A) Theme & identity variables — SET THESE FIRST
themeName = classic
alternativeLocalThemeRepository:= "$HOME/xprojects/nuts-world/nuts-productivity/ntexup/ntexup-templates"
alternativeRemoteThemeRepository:= "github://thevpc/ntexup-templates"

documentColorAccent = 0                   // 0..11
themeShowPalette = false
documentTitle = "ntexup"
documentSubtitle = "A declarative, text-based document & presentation generator"
documentSubsubtitle = "Open Source"
documentAuthor = "thevpc"
documentAuthorEmail = "https://github.com/thevpc/ntexup"
documentAuthorAffiliation = "Open Source Community"
documentChapter = "Chapter 1"
documentDate = "2026-09-13"
documentVersion = "v1.0.0.0"

// (B) Load styles (includes the theme + local custom definitions)
include("01-styles")

// (C) Load all slides (directory include — auto sorts alphabetically)
include("02-pages")
```

Notes:
- `:=` means **default-assign** (won't override if the variable is passed in from outside).
- `=` means **hard-assign** (always sets). Use `=` for user-facing document variables so they are predictable.
- Variables like `documentColors[0..11]`, `documentSurface`, `documentLine`, `documentTextPrimary` are provided by the theme — reuse them inside slides instead of hard-coded hex values to stay theme-compliant.

---

## 6. Slide / Page Types

Themes provide four high-level slide "components". Use them instead of raw `page{}` when possible — they guarantee consistent headers, footers, and padding.

### 6.1 Cover Slide
```tson
cover-slide()    // no body needed — reads documentTitle / Author / Date from main.ntx
```

### 6.2 Agenda / TOC Slide
```tson
agenda-slide(title: "Rationale"){
    body{
        ul(bullet-align: top){
            •¶ Point one
            •¶ Point two
            •¶ Point three
        }
    }
}
```

### 6.3 Content Slide (Workhorse — use for ~80% of slides)
```tson
content-slide(title: "My Section Title", variant: 2){
    body{
        // ... slide content here ...
        text("Hello", at: top)
    }
}
```
- `variant` controls small header/shape decorations (1..N — varies by theme). Try 2, 3, 4, 6, 7 and pick what looks good.

### 6.4 Conclusion / Thank-You Slide
```tson
conclusion-slide()
```
Or for a custom one:
```tson
content-slide(title: "Thank You!", variant: 7){
    body{
        text("Questions?", at: center, font-size: 8%P, font-bold, color: documentColors[0])
    }
}
```

---

## 7. Core Content Elements

Everything inside a slide body uses TSON function-call syntax: `functionName(arg1, arg2, namedArg: value){ children }`.

### 7.1 Text

| Element | Usage |
|---------|-------|
| `plain("text")` | Raw text, no formatting interpretation |
| `text("text")`  | Formatted text — recognizes `##bold##`, `###italic###`, `####bold-italic####`, `[[eq: ...]]` embedded equations, and Markdown-like bullets |
| `¶ "text"`      | Paragraph shorthand inside lists or groups (use `•` for bullet + `¶` for the item) |
| `""" multi-line """` | A TSON triple-quoted string; newlines and indentation are preserved |

**Inline formatting** inside `text()` or triple-quoted strings:
```tson
"""
Normal text,
##bold text##,
###italic text###,
####bold italic####,
and a math expression [[eq: E = mc^2]] embedded inline.
"""
```

**Positioning**:
```tson
text("Top", at: top)
text("Bottom-Left", at: bottom-left)
text("Exact", position: (30, 40), origin: top-left)   // x=30% y=40% of parent
```

**Font attributes** (combine freely):
```tson
plain("Hello", font-size: 3%P, font-bold, font-italic, font-family: monospaced, color: documentColors[2])
```
> Prefer percentage-of-parent units `%P` over absolute pixels so text scales if the user resizes the viewer.

### 7.2 Lists — Bullets & Numbered

```tson
// Unordered list
ul{
    ¶ First item
    ¶ Second item
    ul{
        ¶ Nested sub-item
    }
    ¶ Third item
}

// Ordered list
ol{
    ¶ Step one
    ¶ Step two
}

// With bullet glyph prefix (common pattern in existing decks)
ul{
    •¶ Hello ##World##
    •¶ Hello ###World###
}
```
- Use `bullet-align: top` inside `ul()` so bullets align with the first line of wrapped items.

### 7.3 Equations (LaTeX Math)

Two forms — standalone or inline-embedded.

```tson
// Standalone block equation
eq("X^2 = \sin(\pi x)", font-size: 4%P)

// Unicode-friendly form also works
eq("X² = sin(πx)")

// Embedded inside a text string
text("The quadratic is [[eq: x = \frac{-b \pm \sqrt{b^2-4ac}}{2a}]] — remember it.")
```

When in a triple-quoted string, backslashes don't need extra escaping — a huge plus for LaTeX.

### 7.4 Shapes (2D)

All shapes share common named args: `at`, `position`, `size` (single number or `(w,h)` tuple), `background`, `color` (stroke), `rotate`, `margin`, `padding`, `round-corner`, `draw-contour: true/false`.

```tson
rectangle(at: center, size: (50, 20), background: documentColors[4], color: documentColors[2])
square(at: center, size: 50, background: documentColors[4])
circle(at: center, size: 50, background: documentColors[4])
ellipse(at: center, size: (80, 30), background: documentColors[4])
triangle(at: center, size: 50, background: documentColors[4])
rhombus(at: center, size: 50)                  // diamond
parallelogram(at: center, size: 50)
trapezoid(at: center, size: 50)
```

Full list of available shapes: `rectangle`, `square`, `circle`, `ellipse`, `triangle`, `rhombus` / `diamond`, `parallelogram`, `trapezoid`, `line`, `arrow`, `polygon`, `pie`, `donut`, `cylinder`, and the extensions in `shapes2d`/`shapes3d` packages (auto-imported by modern themes).

### 7.5 Images

```tson
image("images/logo.png", size: (50, 50), at: center)
image("images/photo.jpg", size: 40, at: top-right, margin: 10)
image("images/diagram.svg", size: (60, 60))
image("images/animated.gif", size: 60)
```

Supported formats: PNG, JPG, SVG, GIF (animated), AVIF, WEBP.

Paths: relative to the **current `.ntx` file**. If in doubt, go up `../../../images/foo.png` as the reference slides do.

### 7.6 Layout Containers

Nest elements inside these to build structure.

#### group — scoping container (creates visual box + variable scope)
```tson
group(background: documentSurface, draw-contour: true, line-color: documentLine, margin: 5, padding: 10){
    text("Inside a card", at: top)
    rectangle(size: (80, 40), at: bottom, background: documentColors[0])
}
```

#### grid — rows × columns table
```tson
// 2 columns × 3 rows
grid(2, 3, columns-weight: [1, 2], rows-weight: [1, 1, 3]){
    styles:{ "*": {margin: 2} }
    "cell (1,1)"
    "cell (1,2)"
    "cell (2,1)"
    "cell (2,2)"
    "cell (3,1)"
    "cell (3,2)"
}

// tuple syntax — grid((cols, rows))
grid((2,2)){
    rectangle(background: documentColors[0])
    triangle(background: documentColors[1])
    text("Hi")
    circle(background: documentColors[2])
}
```
- `columns-weight` / `rows-weight` arrays are relative sizes.

#### column / col — stack vertically
```tson
col(rows-weight: [22, 78]){
    text("Header row", at: top)
    text("Body row — takes 78% height")
}
```

#### fragment / block — logical (not visual) containers

| Container | New Variable Scope? | Kept in Render Tree? | Use When… |
|-----------|---------------------|----------------------|-----------|
| `fragment{}` | **No** — leaks up | **No** — dissolves | Grouping readability only |
| `block{}`    | **Yes** — isolated | **No** — dissolves  | Local temp vars / loops, no extra visual div |
| `group{}`    | **Yes** — isolated | **Yes** — retained   | Visual box + logical isolation |

### 7.7 Source Code with Syntax Highlighting

```tson
source(
    "java",                              // or: python, typescript, ntexup, bash, …
    """
    public class Hello {
        public static void main(String[] a){
            System.out.println("Hi");
        }
    }
    """,
    font-size: 2.8%P,
    at: top-left
)
```
The `source` element auto-applies a dark theme + keyword/string/comment colors by default. Override via `styles { source { ... } }` if needed.

---

## 8. Styling System (CSS-like)

Global scoped styles go in a `styles{}` block anywhere (typically in `01-styles/001-styles.ntx`, but also valid inline inside a slide or group).

```tson
styles{
    (*){                                  // wildcard — matches everything
        font-size: 4.5%P
        font-family: "SansSerif"
        color: documentTextPrimary
    }

    text{
        margin: 2
    }

    source{
        font-family: "monospaced"
        background: "#0f172a"
        color: "#f8fafc"
        padding: (1%P, 0.5%P)
    }

    ".big-red" {                           // class selector — use with style: arg
        font-size: 8%P
        color: red
        font-bold
    }
}
```

Apply a named class:
```tson
text("Important!", style: ".big-red", at: center)
```

**Theme variables you should always prefer over hard-coded colors:**

```tson
documentColors[0..11]   // rotating accent palette — user picks via documentColorAccent
documentBg              // page background
documentSurface         // card / panel surface (e.g. group bg)
documentSurfaceAlt      // alternate panel surface
documentLine            // borders and dividers
documentTextPrimary     // main body text
documentTextSecondary   // muted body text
documentTextMuted       // very muted text
semanticInfoColor / semanticInfoBg
semanticWarningColor / semanticWarningBg
semanticErrorColor / semanticErrorBg
semanticSuccessColor / semanticSuccessBg
```

---

## 9. Custom Reusable Components — `@define`

This is one of ntexup's most powerful features. Always look for an excuse to `@define` when the same pattern repeats across 2+ slides.

### 9.1 Simple Component with Args

```tson
// Definition (put in 01-styles/001-styles.ntx so it's global)
@define info-badge(label, count: 1, accent: documentColors[0]){
    group(margin: 1, size: (100, 22)){
        rectangle(background: accent, round-corner: 4)
        text("${label}: ${count}", color: white, font-bold, font-size: 2.2%P, at: center)
    }
}

// Usage — anywhere after the definition
info-badge(label: "Open Issues", count: 7, accent: documentColors[4])
info-badge(label: "PRs", count: 3)                             // uses defaults
```

### 9.2 Wrapper Component (accepts `body` from caller)

```tson
@define feature-box(title, accent: documentColors[1]){
    group(margin: 1){
        rectangle(background: documentSurface, color: documentLine, draw-contour: true, round-corner: 6)
        rectangle(background: accent, position: (0, 0), size: (0.35%P, 100))
        text("${title}", position: (6, 14), origin: left, font-bold, font-size: 2.4%P, color: accent)
        group(position: (6, 32), size: (88, 62), origin: top-left){
            body                                             // <-- caller content lands here
        }
    }
}

// Usage: wrap content inside the braces
feature-box(title: "Key Results", accent: documentColors[2]){
    ul{
        ¶ Accuracy improved by 12%
        ¶ Latency halved
        ¶ Training data doubled
    }
}
```

### 9.3 Slides + Components Workflow

1. User asks for a new section with 3 similar slides.
2. **Agent first**: inspect `01-styles/001-styles.ntx` → add a `@define` wrapper matching the visual pattern.
3. **Agent then**: create 3 `.ntx` files that invoke the component with different content.
4. **Result**: 3 consistent slides; if the design needs changing, the agent edits only the `@define`.

---

## 10. Diagrams (PlantUML Integration)

Modern themes already `import("plantuml")`. You can use these directly:

### 10.1 UML
```tson
uml(at: center, size: (100, 60),
    """
    left to right direction
    actor "User" as u
    rectangle "App" {
      usecase "Login" as UC1
      usecase "Logout" as UC2
    }
    u --> UC1
    u --> UC2
    """
)
```

### 10.2 Other Diagram Types

Use the matching element name — syntax inside the string is pure PlantUML:

| Element | Diagram Kind |
|---------|-------------|
| `uml()` | Use-case / Class / Sequence / Component / State / Deployment / Activity |
| `nwdiag()` | Network diagrams |
| `gantt()` | Gantt charts |
| `wireframe()` | UI wireframes |
| (plus any other PlantUML type supported by the installed PlantUML version) |

Example Gantt:
```tson
gantt(at: center, size: (95, 80),
    """
    [Design] lasts 5 days
    [Code] lasts 10 days
    [Code] starts at [Design]'s end
    """
)
```

---

## 11. Plots (Plot2D)

`import("plot2d")` is auto-included by modern themes.

```tson
plot2d(size: (80, 60), at: center,
    title: "f(x) = sin(x) vs cos(x)",
    x: "[-PI, PI]",
    y: "[-1.2, 1.2]"
){
    curve("sin(x)", color: documentColors[0])
    curve("cos(x)", color: documentColors[1])
}
```

---

## 12. Loops, Conditionals, Expressions

Because `.ntx` is TSON (not a markup), you get real logic.

### 12.1 Variables & Expressions
```tson
totalSlides := 12
current := 3

group{
    text("Progress: ${current} / ${totalSlides}", at: top)
    rectangle(size: ("${(current/totalSlides)*100}", 5), at: bottom-left, background: documentColors[0])
}
```

### 12.2 For Loops
```tson
for(i: [1 -> 5]){
    content-slide(title: "Iteration ${i}"){
        body{
            text("Slide number ${i}", at: center)
            eq("x^{${i}} + y^{${i}} = z^{${i}}", font-size: 4%P)
        }
    }
}
```

### 12.3 If / Else
```tson
showAppendix := true

if(showAppendix){
    include("03-appendix")
} else {
    // nothing
}
```

---

## 13. Includes (Multi-File Composability)

`include()` is how you split a deck across files — and it even works across Git repos.

```tson
// Include a directory — auto-discovers all .ntx files in alphabetical order
include("02-pages")

// Include a single file (extension optional)
include("02-pages/0010-intro/0010-agenda.ntx")

// Include from a GitHub remote (no local clone needed!)
include("github://thevpc/ntexup-templates/classic/v1.0/theme")

// Conditional path resolution — try local first, fall back to remote
include(
    eitherPath(
        "$alternativeLocalThemeRepository/${themeName}/v1.0/theme"
        "$alternativeRemoteThemeRepository/${themeName}/v1.0/theme"
    )
)
```

Agent convention for path hygiene:
- **New project**: always use directory includes (`include("02-pages")`) so adding a file doesn't require editing `main.ntx`.
- **Editing an existing project**: keep the same include style the project already uses; don't reformat unless asked.

---

## 14. Extensions (Java Plugins)

Modern themes already import these at the top of the theme `.ntx` file. You do NOT need to re-import unless writing a theme from scratch:

```tson
import("common-functions")
import("plantuml")
import("animated-gif")
import("svg")
import("shapes2d")
import("shapes3d")
import("plot2d")
import("presenters")
import("latex")
```

If the user wants something custom that ntexup can't express declaratively, mention that extensions are Java-based, auto-resolved by Nuts, and should be registered in an `import(...)` statement. For 99% of decks, the built-ins suffice.

---

## 15. Editing Workflow — Step-by-Step for Agents

When the user says "edit my presentation", follow this checklist:

### Step 1 — Locate the project root
Look for a `main.ntx` (or `*.ntx` with `themeName` / `include("01-styles")`). The root is the directory containing `main.ntx`.
- Reference: [main.ntx](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup-doc-slides/main.ntx)

### Step 2 — Identify theme & color accent
Read `main.ntx` top lines. Record:
- `themeName`
- `documentColorAccent`
- `documentTitle` / author info to update if requested

### Step 3 — Find the slide to modify
If user says "edit the methodology slide":
- Look under `02-pages/` for a folder like `0030-methodology/`
- Open the `.ntx` inside; each file typically contains exactly one call to `content-slide(...)` / `cover-slide()` etc.

If user says "add a new slide between Results and Conclusion":
- Note the last folder in `02-pages/` before `9999-conclusion/` (e.g. `0040-results/` → highest prefix `0040`)
- Create a new folder with the next decade prefix: `0050-my-new-section/`
- Inside, create `0051-my-slide.ntx` containing one `content-slide(title: "..."){ body{ ... } }`
- Because we include the `02-pages` directory in `main.ntx`, no other edits are needed.

### Step 4 — Apply styling rules
Always:
- Use `documentColors[N]` / `documentSurface` / etc. instead of hex literals unless the user explicitly requests a brand color.
- Use `%P` units for font-size and sizes so slides scale.
- Wrap repeated patterns in a `@define` inside `01-styles/001-styles.ntx`.

### Step 5 — Render and verify
After every batch of edits:
```bash
cd /path/to/project
nuts ntexup show .           # opens Swing viewer — interactive check
# OR headless:
nuts ntexup pdf              # produces output.pdf for quick sanity check
```

If rendering fails with an error message, jump to §18 Troubleshooting.

---

## 16. Common Tasks — Full Recipes

### 16.1 Add a New "Methodology" Section (3 Slides)

```bash
# assuming we're in project root with 02-pages containing up to 0040-*
mkdir -p 02-pages/0050-methodology
```

File `02-pages/0050-methodology/0051-overview.ntx`:
```tson
content-slide(title: "Methodology Overview", variant: 3){
    body{
        grid(2, 1, columns-weight: [1, 1]){
            styles:{"*":{margin:3}}
            feature-box(title: "Step 1 — Data", accent: documentColors[0]){
                ul{ ¶ Clean ¶ Normalize ¶ Augment }
            }
            feature-box(title: "Step 2 — Model", accent: documentColors[1]){
                ul{ ¶ Backbone ¶ Head ¶ Loss }
            }
        }
    }
}
```

File `02-pages/0050-methodology/0052-algorithm.ntx`:
```tson
content-slide(title: "Training Algorithm", variant: 4){
    body{
        source("python", """
for epoch in range(EPOCHS):
    for batch in loader:
        y_hat = model(batch.x)
        loss = criterion(y_hat, batch.y)
        opt.zero_grad(); loss.backward(); opt.step()
""", at: top, font-size: 2.5%P)
    }
}
```

File `02-pages/0050-methodology/0053-complexity.ntx`:
```tson
content-slide(title: "Complexity Analysis", variant: 2){
    body{
        eq("T(n) = O(n \log n) \quad \text{time}, \quad S(n) = O(n) \quad \text{space}",
            at: center, font-size: 5%P)
    }
}
```

Done — `include("02-pages")` in `main.ntx` will pick them up in order.

### 16.2 Replace Cover Title + Author

Open `main.ntx` and edit variables:
```tson
documentTitle = "Faster Neural ODEs"
documentSubtitle = "A practical approach to continuous-depth models"
documentAuthor = "Ada Lovelace"
documentAuthorEmail = "ada@analytical.engine"
documentAuthorAffiliation = "Analytical Engine Lab"
documentDate = "2026-09-20"
```

### 16.3 Switch Theme from Classic → Meridian + Change Accent

```tson
themeName = "meridian"
documentColorAccent = 5         // try 0..11; use themeShowPalette=true to visualize
```

### 16.4 Insert a Comparison Table (Grid of Shapes)

```tson
content-slide(title: "Comparison of Approaches", variant: 3){
    body{
        grid(4, 4, columns-weight: [2, 1, 1, 1]){
            styles:{
                "*":{draw-contour:true, color:documentLine, background:documentSurface, margin:1, padding: 2%P}
                ".header":{background: documentColors[0], color: white, font-bold}
            }
            text("Approach", style: ".header")  text("Speed", style: ".header")  text("Accuracy", style: ".header")  text("Cost", style: ".header")
            text("Baseline")                    text("★★☆")                        text("★★★")                          text("★☆☆")
            text("Ours")                        text("★★★")                        text("★★★")                          text("★★☆")
            text("Competitor X")                text("★☆☆")                        text("★★★")                          text("★★★")
        }
    }
}
```

### 16.5 Add an Architecture Diagram Slide

```tson
content-slide(title: "System Architecture", variant: 7){
    body{
        uml(at: center, size: (95, 85), """
!theme plain
skinparam componentStyle rectangle
component "Client" as C
database "Postgres" as DB
queue "Kafka" as K
component "API Server" as API
component "ML Worker" as ML

C --> API : REST
API --> DB : read/write
API --> K  : publish
ML  --> K  : consume
ML  --> DB : write predictions
""")
    }
}
```

---

## 17. Rendering & Distribution

| Command | What it does |
|---------|-------------|
| `nuts ntexup show .` | Open interactive Swing viewer with hot-reload (use during editing) |
| `nuts ntexup pdf` | Render to `output.pdf` in project root (for distribution / print) |
| `nuts ntexup documentation` | Open the official example/reference slide deck |
| `nuts ntexup --install-editor-syntax vscode` | Install syntax highlighting for `.ntx` in supported editors (vim, kate, intellij, gedit, vscode, notepad++) |

Typical edit loop:
1. open terminal → `nuts ntexup show .` (leave viewer running)
2. edit `.ntx` files
3. viewer auto-refreshes (or press reload hotkey)
4. when happy → `nuts ntexup pdf` to ship

---

## 18. Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `theme not found` or `eitherPath` error | Theme folder not reachable | Ensure `alternativeLocalThemeRepository` points to a valid ntexup-templates clone, or just use the `github://…` form (it's the default in scaffolded projects). |
| LaTeX equation renders blank or misaligned | Unsupported command OR over-escaped backslashes | Rule of thumb from §20.1: **TSON has literal-first strings — never double-escape.** `\frac` is written exactly `\frac`, not `\\frac`. Start with `eq("x^2")` and build up. |
| `\n` showing as literal two chars in text | Expected JSON-style escape processing | §20.1: TSON does NOT process `\n` → newline. Press **Enter** inside the string for a real newline. |
| Windows path `C:\Users\...` mangled | Added extra backslashes out of JSON habit | §20.1: write `"C:\Users\Config"` **exactly as-is**. No `\\` needed. |
| Image not showing | Wrong relative path | Path is relative to the **current `.ntx` file** not `main.ntx`. From deep folders you often need `../../../images/foo.png`. |
| Colors look off / not theme-matched | Hard-coded hex where theme vars should be used | Replace `#ff0000` → `documentColors[4]`; replace `#ffffff` → `documentBg`; use `themeShowPalette=true` temporarily to see all 12 accent colors. |
| Content outside slide / clipped | `%P` not used; absolute pixels | §20.4 explains suffixes. Convert `font-size: 20` → `font-size: 3%P` (% of Parent). Use `at:` / `position:` with percentages. |
| `font-bold` / `draw-contour` flagged as unknown | Stringified bare identifiers | §20.5: these are valid TSON Identifiers (naked Names). Do NOT quote them. Write `font-bold` not `"font-bold"`. Presence = true. |
| Variable seems to ignore my override in main.ntx | Used `:=` (default-assign) instead of `=` | §20.7: `:=` means "set only if currently undefined". Use plain `=` for user-facing identity values in `main.ntx` so they always win. |
| `@define` component not found | Definition in wrong file / not included | Put all `@define` blocks in `01-styles/001-styles.ntx` (which is loaded before `02-pages`). |
| Grid cells empty / missing content | Wrong grid dimensions | Remember `grid(COLS, ROWS)` then list children left-to-right, top-to-bottom. If you declare 2×3=6 cells but only provide 4 children, the last 2 render empty. |
| Render error: "expecting }" | Unbalanced TSON braces | Count `{` vs `}` in the last file you edited. Prefer `"""..."""` (§20.2) for LaTeX/multi-line strings — no brace or backslash escaping needed inside. |
| "Unexpected character" near `¶` or `^` | Using valid TSON syntax the older ntexup engine rejects | Rare — fall back to `"""..."""` for text blocks and `//` / `/* */` for comments. Report a bug if a spec-valid construct (per TSON SPEC.md) fails in ntexup. |

---

## 19. Reference Examples (Explore These First)

When the agent is unsure how to express something, **read these files** — they are authoritative, real examples used as the official documentation deck:

| Topic | File |
|-------|------|
| Main entry point (vars + includes) | [main.ntx](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup-doc-slides/main.ntx) |
| Styles include + `@define` helpers | [001-styles.ntx](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup-doc-slides/01-styles/001-styles.ntx) |
| Hello world / text formatting | [0020-hello-world.ntx](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup-doc-slides/02-pages/0030-general-form/0020-hello-world.ntx) |
| Plain text variants | [0110-plain.ntx](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup-doc-slides/02-pages/0040-elements/0010-text/0110-plain.ntx) |
| Bullets (ul / ol + nesting) | [0170-bullets.ntx](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup-doc-slides/02-pages/0040-elements/0010-text/0170-bullets.ntx) |
| LaTeX equations | [0140-eq.ntx](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup-doc-slides/02-pages/0040-elements/0010-text/0140-eq.ntx) |
| Shapes (rect, circle, triangle, …) | [0120-shapes.ntx](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup-doc-slides/02-pages/0040-elements/0020-base/0120-shapes.ntx) |
| Layouts (group / grid / weighted rows) | [0160-layout.ntx](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup-doc-slides/02-pages/0040-elements/0020-base/0160-layout.ntx) |
| Images (all formats) | [0140-images.ntx](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup-doc-slides/02-pages/0040-elements/0050-images/0140-images.ntx) |
| UML diagrams (PlantUML) | [0140-diag-uml.ntx](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup-doc-slides/02-pages/0040-elements/0070-diagrams/0140-diag-uml.ntx) |
| Custom components (`@define` + body) | [0110-custom-components.ntx](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup-doc-slides/02-pages/0040-elements/0080-components/0110-custom-components.ntx) |
| Classic theme (variables + styles + slide components) | [classic.ntx](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup-templates/classic/v1.0/theme/classic.ntx) |
| Scoping rules: fragment vs group vs block | [isolation.md](file:///home/vpc/xprojects/nuts-world/nuts-productivity/ntexup/ntexup/documentation/specifications/isolation.md) |
| **TSON v2.0 syntax spec** (strings, numbers, suffixes, comments, `:=` vs `=`, data model) | <https://github.com/thevpc/tson/blob/main/SPEC.md> |

Agent rule-of-thumb: **if in doubt, copy an exact pattern from one of the above files and adapt the content**. It is safer than inventing syntax from memory. For any low-level "how does TSON parse X?" question, open the TSON SPEC.md link above — it is the source of truth for the `.ntx` file format.

---

## 20. TSON Core Behaviors — Know These or Regret It

This section condenses the high-impact rules from the [TSON SPEC.md](https://github.com/thevpc/tson/blob/main/SPEC.md) that are relevant to `.ntx` authoring. If something "looks like it should work but doesn't", 9 times out of 10 it is one of these rules.

### 20.1 The Literal-First Rule (No Magic Backslashes!)

**TSON treats every character inside string delimiters as literal text.** The sequence `\n` is **two characters** (backslash + n), not a newline. Standard escape sequences `\n`, `\t`, `\r` are **NOT** interpreted by the TSON parser.

Consequences for ntexup authors:
- If you want a **real newline** in a string, insert a literal newline (press Enter).
- If you want a real backslash, just type `\`. Do NOT write `\\`.
- **LaTeX loves this**: `\frac`, `\sqrt`, `\sin`, `\pi` can all be written as-is inside any TSON string, with no escaping.
- **Paths love this**: `"C:\Users\Config"` or `"~/docs/my file with spaces.txt"` are written exactly as they appear — no `\\`.

The ONLY case where escaping applies: the **terminal quote character** of a quoted string may be escaped with `\` (e.g., `"He said \"hi\""` inside a double-quoted string). Prefer a different quote style before reaching for `\"`.

### 20.2 All Quoted Strings Are Multi-Line by Default

There is no difference between "single-line" and "multi-line" string literals in TSON. Every quoted form — `"..."`, `'...'`, `` `...` ``, `"""..."""`, `'''...'''` — accepts literal newlines.

Agent convention:
- Short text: prefer `"..."`.
- Code / LaTeX / multi-line text with many quotes: prefer `"""..."""` (triple-double). Inside triple quotes, you can freely use single `"` characters without escaping.
- Strings containing `"""` somewhere (rare): use `'''...'''` or `^id{...^id}` custom delimiters.

### 20.3 Quote Style Menu (when to use which)

| TSON Form | Escaping of Terminal Quote | Best Used For |
|-----------|---------------------------|---------------|
| `"..."` | `\"` | Standard strings, variable values (`"hello"`) |
| `'...'` | `\'` | Strings containing `"` that you don't want to escape |
| `` `...` `` | `` \` `` | Shell snippets, Markdown |
| `"""..."""` | `\"""` | SQL, LaTeX blocks, source code, multi-paragraph text (**default choice for large text**) |
| `'''...'''` | `\'''` | Multi-line text containing `"""` |
| `^foo{...^foo}` | None (custom delimiter) | Embedding TSON inside TSON, or anything that already contains every quote style |
| `¶ text` | N/A | Short single-paragraph note; ends at newline |
| `¶¶ text` | N/A | Multi-paragraph doc-comment; consecutive lines |

### 20.4 Number Literals + Unit Suffixes (How `%P` Works)

Numbers in TSON support optional **type annotations** (`_s32`, `_u64`, `_sN`, `_f64`) and **suffixes** (`%P`, `ms`, `GHz`, `%`, anything letter/`%`/`_`).

The suffix is preserved as metadata alongside the numeric value. It does NOT change the parsed number. It is the **application's responsibility** (here: ntexup) to interpret the suffix.

In ntexup, the critical suffix is:
- `%P` — **Percentage of Parent**. E.g., `font-size: 3%P` means "3% of the parent container's principal axis dimension". This is why ntexup text scales.

Other common number forms:
```tson
42                    // integer
-3.14                 // float
1e6                   // scientific notation
0xFF                  // hex
0b1010                // binary
2025-12-01            // native LocalDate (TSON literal-intelligence)
12_s32                // explicit 32-bit signed int
12.0_sN               // arbitrary-precision BigDecimal
100ms                 // suffix "ms" — app interprets as milliseconds
50%                   // suffix "%"
3+4i                  // complex literal (contiguous — no spaces!)
```

Complex-number whitespace rule:
- `3+4i` → one complex literal (bonded, contiguous)
- `3 + 4i` → three tokens: integer `3`, operator `+`, pure imaginary `4i`. The parser treats this as a flat math expression, not a constant.

### 20.5 Identifiers (Bare Names)

TSON identifiers are unquoted symbolic names. They may contain:
- Start: Unicode letter, `_`, `$`
- Continue: Unicode letters, digits, `_`, `$`, `-` (hyphen), `.` (dot)
- Must NOT start or end with `-` or `.`; no consecutive separators (`--`, `..`, `-..`)

Valid examples you will see in ntexup:
```tson
themeName                     // camelCase
documentColorAccent           // camelCase
font-size                     // kebab-case — valid!
font-family                   // kebab-case
font-bold                     // flag-style identifier (a "naked" TsonName)
01-styles                     // legal (has letter, doesn't start with digit)
$var                          // dollar-prefixed
π                             // Unicode letters are fine
café.menu                     // Unicode + dot fine
```

This is why `font-bold` and `draw-contour` can be used **as named bare arguments** without quotes: they are valid TSON identifiers that ntexup treats as boolean flags (presence = true).

### 20.6 The Data Model (Quick)

Every node in a `.ntx` file is one of:

| TSON Node Kind | How it appears in `.ntx` | ntexup use |
|----------------|--------------------------|------------|
| **Primitive**  | `42`, `3%P`, `"hello"`, `true`, `null`, `2025-12-01` | Argument values, text content |
| **Name** (naked id) | `font-bold`, `draw-contour`, `at`, `center` | Boolean flags (presence = true); enum-style values (`at: top`) |
| **Pair** (key-value) | `key: value`, `name: arg` | Named function args (`text(..., at: top)`); object fields |
| **Container — Braced `{}`** | `{ ... }` | Function body, styles block, group children, slide body |
| **Container — Bracketed `[]`** | `[1, 2, 3]`, `[1 -> 5]` | Arrays (`columns-weight: [1, 2]`); ranges in `for` |
| **Container — Parenthesized `()`** | `(50, 20)`, `(x, y)` | Tuples (`size: (50, 20)`); function arg lists |
| **Annotation `@`** | `@define foo(args) { body }` | Custom component definitions (`@define` is the main one you'll see) |

### 20.7 Why `:=` vs `=` Matters (Variable Assignment Operators)

In ntexup you will see both:
```tson
documentTitle = "My Title"               // hard-assign
alternativeLocalThemeRepository:= "..."   // default-assign
```

Per TSON conventions adopted by ntexup:
- `=` (plain equals) — **unconditional assignment**. Always sets the variable; overrides whatever came before. Use for the user-facing "contract" variables in `main.ntx` so the user's value always wins.
- `:=` (colon-equals) — **default-assign / "set if unset"**. Only assigns if the variable is currently undefined. This lets `main.ntx` declare fallbacks that can be overridden from a higher level (e.g., if the user passes `documentColorAccent=5` externally, the `:= 0` in main.ntx will respect it).

Agent convention: copy the convention already in use in a file. Do **not** rewrite all `:=` to `=` in an existing theme file — you will break external overrides.

### 20.8 TSON is a Superset of JSON

Valid JSON is valid TSON. This means:
- If an agent or user pastes a JSON snippet into a `.ntx` file, it will parse.
- `"strings"`, `[arrays]`, `{objects}`, `true/false/null`, numbers all work the same.
- **Differences that actually matter** (TSON adds, JSON does not have): trailing commas allowed; comments; unquoted identifiers; number suffixes; multiple quote styles; `¶` paragraphs; `^id{}` custom delim; `@annotations`; `:=` operator; tuples via `()`.

### 20.9 Comments in TSON

Three comment styles — use them freely (they are preserved but do not affect rendering):

```tson
// Line comment — to end of line

/* Block comment
   can span
   multiple lines */

¶¶ Doc-comment paragraph
   (consecutive ¶¶ lines merge;
    typically used before a definition)
```

---

## 21. Agent Decision Heuristics (Quick)

| User says… | Agent does… |
|------------|-------------|
| "Create a new presentation about …" | Pick `classic-medium` template → create → add content slides under `02-pages/NNNN-topic/`. |
| "Fix my broken deck" | Run `nuts ntexup show .` → read error → fix offending file (usually unbalanced braces or wrong path). |
| "Make it look nicer" | Switch `themeName` to another one; bump `documentColorAccent`; wrap repeated content in styled `group` / `feature-box`. |
| "I need a diagram of …" | Use `uml()`, `nwdiag()`, `gantt()`, or `wireframe()` with inline PlantUML string. |
| "This part repeats across 5 slides" | Extract a `@define` component in `01-styles/001-styles.ntx` → rewrite the 5 slides to call it. |
| "Add math" | Prefer `eq()` standalone; for inline math use `[[eq: …]]` inside a text/triple-quoted string. |
| "Can you restructure?" | Move files under `02-pages/NNNN-folder/` keeping zero-padded prefix order; nothing else needs changing if `include("02-pages")` is used. |
| "Ship it" | Run `nuts ntexup pdf` → confirm `output.pdf` renders cleanly. |
