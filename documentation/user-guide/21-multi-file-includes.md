# Multi-file Projects and Includes

Large documents are organized into multiple `.ntx` files and combined with the `include` directive.

## Basic Include

```tson
include("01-styles/001-styles.ntx")
```

### Globs

```tson
include("01-styles/*.ntx")              // one level
include("02-pages/**/*.ntx")            // recursive
include("02-pages/**/*.ntx,03-lib/**/*.ntx")
```

## Remote Includes (GitHub)

```tson
include("github://thevpc/ntexup-templates/classic/v1.0/theme")
```

## Import (Extensions)

```tson
import("shapes2d")
import("plantuml", "plot2d", "latex")
```

These load Java extension modules.

## `return` Trick for File Results

Each included file can define content. To make a page the file's primary result:

```tson
// 02-pages/0001-cover.ntx
return page {
    ...
}
```

Then the parent `main.ntx` collects all returned pages.

## Recommended Layout

```
project/
├── main.ntx                # entry point: variables, includes
├── 01-styles/
│   └── 001-styles.ntx      # style definitions
├── 02-pages/
│   ├── 0001-intro/
│   │   ├── 0001-cover.ntx
│   │   └── 0010-plan.ntx
│   ├── 0010-chapter1/
│   │   ├── 0001-slide.ntx
│   │   └── 0010-slide2.ntx
│   └── 9999-conclusion/
│       ├── 9901-conclusion.ntx
│       └── 9999-thankyou.ntx
└── 03-lib/
    └── components.ntx      # reusable @define components
```

## Ordering Rules

1. Variables before use
2. Imports and includes in dependency order
3. Styles before pages
4. Components (`03-lib`) before pages that use them

Example `main.ntx`:

```tson
// Theme
themeName = "classic"
documentColorAccent = 1
include("github://thevpc/ntexup-templates/${themeName}/v1.0/theme")

// Extensions
import("shapes2d")
import("plantuml")
import("plot2d")

// Styles
include("01-styles/*.ntx")

// Components
include("03-lib/**/*.ntx")

// Pages must come last
include("02-pages/**/*.ntx")
```

## Sharing Files Across Projects

Keep shared components in a separate repo and include via GitHub:

```tson
include("github://youruser/ntexup-common/component-lib/v1.0/**/*.ntx")
```

## Splitting a Page Into Parts

You can include partial content:

```tson
// 0020-rationale.ntx
return text(md("Rationale: ..."), at: center)
```

## Relative Paths

All paths in `include` are relative to the including file (or `main.ntx`), unless prefixed with `github://`, `http://`, or `https://`.

## Caching and Updates

Remote GitHub includes are cached locally. When you update templates server-side, re-run your build or use a branch-pinned URL to force refresh.

## Variables Across Files

Variables defined in `main.ntx` before an include are visible in included files:

```tson
// main.ntx
accent = documentColors[1]
include("02-pages/0001-intro/*.ntx")

// 0001-cover.ntx can now use `accent`
rectangle(size: 100, background: accent)
```