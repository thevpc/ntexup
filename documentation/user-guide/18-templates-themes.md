# Templates and Themes

Templates and themes provide ready-to-use structures and styles. They come from the [ntexup-templates](https://github.com/thevpc/ntexup-templates) repository.

## Available Templates

| Template | Description |
|-------|-------------|
| `classic` | Default template, 12-accent-color palette |
| `ibtihel` | Alternative template style |
| `eniso` | Another template variant |

Each template has three layout variants (a "layout" is the starter file structure):

| Layout | Description |
|------|-------------|
| `small` | Single-file project, quick tests |
| `medium` | Multi-file project, standard split |
| `large` | Complex project with many sections |

## Using a Theme

Add to `main.ntx`:

```tson
themeName = "classic"
include("github://thevpc/ntexup-templates/${themeName}/v1.0/theme")
// ... your content
```

## Theme Variables

The theme starts every variable with an **assign-if-not-defined** default (`:=`):

```tson
// inside the theme file (eniso/v1.0/theme/eniso.ntx)
documentTitle := "My Document"
documentColorAccent := 0
themeShowPalette := false
documentColors := arrayLeftRotate(["#26355D", ...], either((documentColorAccent - 1), 0))
```

Because `:=` only applies when the variable is **undefined**, you override any theme default in `main.ntx` by setting it *before* the include:

```tson
themeName = "eniso"
documentColorAccent = 2      // expected accent index (0..11)
documentTitle = "My Book"
themeShowPalette = true
include("github://thevpc/ntexup-templates/${themeName}/v1.0/theme")
```

| Variable | Effect |
|----------|--------|
| `themeName` | selects theme folder |
| `documentTitle` / `documentSubtitle` / `documentAuthor` / `documentDate` / `documentVersion` | cover metadata |
| `documentColorAccent` | accent color index in palette (0..11) |
| `documentColors` | the 12-color palette array |
| `documentBg` / `documentSurface` / `documentLine` / `documentText*` | theme colors |
| `themeShowPalette` | show a demo palette slide |

## Creating a New Project from a Template

```bash
# single file
nuts ntexup new --template=classic:small

# multi-file (recommended)
nuts ntexup new --template=classic:medium

# complex/large
nuts ntexup new --template=classic:large
```

With other templates:

```bash
nuts ntexup new --template=ibtihel:medium
nuts ntexup new --template=eniso:small
```

## The Template Structure

A template typically provides:

```
classic/v1.0/
├── theme/
│   ├── images/            # theme icons (warning, error, success)
│   ├── avatars/           # default avatars
│   └── classic.ntx        # theme definitions
└── templates/
    ├── medium/
    │   ├── main.ntx
    │   └── pages/
    │       ├── 0001-plan.ntx
    │       ├── 0010-slide.ntx
    │       └── 9901-conclusion.ntx
```

## Theme Components

Themes define reusable components that you can call in pages:

```tson
return page {
    hcenter {
        vcenter {
            title(text: "Cover")
        }
    }
}
```

Common theme components (from `classic`):

| Component | Purpose |
|-----------|---------|
| `title(...)` | cover/heading with accent bar |
| `caption(...)` | small italic caption |
| `pageFooter(...)` | footer with page number |
| `code(...)` | code block wrapper |
| `table(...)` | styled table |
| `quote(...)` | block quote |

> Exact names vary by theme — inspect the theme `.ntx` files for the full component list.

## Palette Slide

The `themeShowPalette=true` variable adds a slide that visualizes the 12 accent colors:

![Palette example](images/palette.png)

(Image placeholder — the palette slide is rendered at build time.)

## Creating Your Own Theme

A theme is just a folder of `.ntx` files:

```
mytheme/v1.0/
└── theme/
    └── mytheme.ntx
```

`mytheme.ntx`:

```tson
// Theme base styles
styles {
    page:         { background: #fdf6e3, font-family: "SansSerif" }
    class-title:  { font-size: 4%P, font-bold, color: #b58900 }
    class-body:   { font-size: 1.5%P, color: #073642 }
}

// Theme components
@define myTitle(textValue) {
    text(textValue, @(title), at: top-left)
}

@define myPageFooter(index) {
    text("Page " + index, at: bottom-right, font-size: 1%P)
}
```

Host it on GitHub and reference it:

```tson
include("github://youruser/yourrepo/mytheme/v1.0/theme")
```

Or keep it local:

```tson
include("themes/mytheme/*.ntx")
```

## Theme Inheritance

Set a theme as base and add your own overrides:

```tson
themeName = "classic"
documentColorAccent = 1
include("github://thevpc/ntexup-templates/${themeName}/v1.0/theme")

// Your overrides AFTER the theme include:
styles {
    title: { font-size: 3%P }
}
```

Later definitions win.

## Troubleshooting

- **"repository template not found"** — benign warnings on first run.
- **Theme not found** — network access to GitHub required (the clone uses an embedded JGit provider; the system `git` tool is not needed unless you request `--git-provider system`).
- **Wrong template name** — check `nuts ntexup list-templates`.

## Related

- [Document Structure](05-document-structure.md) — folder layout with templates
- [Components](19-components-reuse.md) — `@define` details
- [Installation](02-installation.md) — cloning templates