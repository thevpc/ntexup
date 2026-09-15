# Images and Media

## The `image` Element

ntexup displays raster images (PNG, JPEG, GIF) and, with the matching image modules, vector formats like SVG:

```tson
image("../../../images/image.png", size: (50, 50), at: center)
```

## Supported Formats

| Format | Notes |
|--------|-------|
| PNG | `image("../images/logo.png")` |
| JPEG | `image("../images/logo.jpg")` |
| GIF | animated on-screen; PDF renders a static frame |
| SVG | `image("../images/logo.svg")` — requires the `svg` image module |

## Properties

| Property | Description | Example |
|----------|-------------|---------|
| (positional) | path/URL (`value`, `file`, `content`, `src`) | `image("images/logo.png")`, `image(file: "logo")` |
| `size` | display size | `size: (50, 50)` |
| `at` | anchor | `at: center` |
| `position` | absolute position | `position: (10, 10)` |
| `preserve-aspect-ratio` | keep the picture ratio inside `size` (default `false`) | `size: (40, 40), preserve-aspect-ratio` |
| `transparent-color` | treat a color as transparent | `transparent-color: white` |

```tson
image("photos.png", size: (40%P, 30%P), at: center, preserve-aspect-ratio)
image("icon.*", size: (10, 10), transparent-color: white)
```

## Path Resolution

- Paths are relative to the current file (the doc-slides use `../../../images/...`).
- **Extension auto-resolution**: a path without a known image extension is tried as `.png`, `.jpg`, `.gif`, `.jpeg`, `.svg` (upper and lower case), so `image("logo")` finds the first matching `logo.<ext>`.
- A `.*` wildcard (`image("icon.*")`) resolves to the first matching format.
- Animated GIFs animate on screen; PDF captures a static frame.