# Document Structure

## Project Layout

A standard ntexup project (classic templates) has:

```
.
├── main.ntx                 # entry point
├── 01-styles/
│   └── 001-styles.ntx       # style definitions
└── 02-pages/
    ├── 0001-intro/
    │   ├── 0001-cover.ntx
    │   └── 0010-plan.ntx
    └── 9999-conclusion/
        ├── 9901-conclusion.ntx
        └── 9999-thankyou.ntx
```

Folders/files use zero-padded numeric prefixes for ordering.

## Single File vs Folder

A document can be a **single file** (as long as it does **not** include other files):

```tson
# presentation.ntx
page {
    grid(1, 1) {
        text("Hello World")
    }
}
```

Run it with `nuts ntexup show presentation.ntx`.

When ntexup is run on a **folder**, it loads all `*.ntx` files found inside (recursively): **`main.ntx` is loaded first**, then every other file **sorted by name**. This entry behavior happens only at the root — when you run that folder directly.

Later, `include(...)`ing a folder loads its files/folders in **lexicographic order** (no `main.ntx` special case). That is why zero-padded prefixes are recommended:

```
02-pages/
├── 0001-intro/
│   ├── 0001-cover.ntx
│   └── 0010-plan.ntx
└── 0010-chapter1/
```

`0010` sorts after `0001`, so chapters render in the intended order.

## The `main.ntx` File

Real-world example (from doc-slides):

```tson
themeName = "eniso"                       // classic, eniso, ibtihel, ...
documentColorAccent = 0                   // 0..11
themeShowPalette = true
documentTitle = "ntexup"
documentSubtitle = """
       A declarative, text-based document
              & presentation generator
                 """
documentAuthor = "thevpc"
documentDate = "2026-09-13"
documentVersion = "v1.0.0.0"

include("01-styles")
include("02-pages")
```

### Structure

1. **Theme variables** — set theme and document metadata
2. **Includes** — style folders then page folders, in order

## Document Metadata

| Variable | Purpose |
|----------|---------|
| `themeName` | theme to use (`classic`, `eniso`, `ibtihel`) |
| `documentColorAccent` | active palette color (0..11) |
| `themeShowPalette` | render a palette showcase slide |
| `documentTitle` | title (cover) |
| `documentSubtitle` | subtitle (cover) |
| `documentSubsubtitle` | extra line (cover) |
| `documentAuthor` | author (cover) |
| `documentAuthorEmail` | link (cover) |
| `documentAuthorAffiliation` | affiliation (cover) |
| `documentChapter` | chapter label (cover) |
| `documentDate` | date (cover) |
| `documentVersion` | version (cover) |

## Page Files

### Cover

```tson
cover-slide()
```

### Agenda

```tson
agenda-slide(title: "Rationale") {
    body {
        ul(bullet-align: top) {
            •¶ Text-based syntax
            •¶ Version-control friendly
        }
    }
}
```

### Content slides

```tson
content-slide(title: "Hello World", variant: 2) {
    body {
        grid(4, 2, columns-weight: [1]) {
            styles {
                "*": { margin: 2 }
            }
            miniPage {
                text("Hello World")
            }
        }
    }
}
```

### Thank-you

```tson
thankyou-slide(title: "Bye")
```

## Adding a Chapter

```
02-pages/
├── 0001-intro/
├── 0010-chapter1/
│   ├── 0001-slide1.ntx
│   └── 0010-slide2.ntx
└── 9999-conclusion/
```

New folders are picked up automatically via `include("02-pages")`.

## Related

- [Multi-file Projects](21-multi-file-includes.md)
- [Templates and Themes](18-templates-themes.md)