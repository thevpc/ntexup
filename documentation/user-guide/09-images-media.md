# Images and Media

## The `image` Element

ntexup displays PNG, JPEG, GIF, SVG, AVIF, and WebP images.

```tson
image("../../../images/image.png", size: (50, 50), at: center)
```

## Supported Formats (from doc-slides)

| Format | Usage |
|--------|-------|
| PNG | `image("../images/image.png", size:(50,50), at:center)` |
| JPEG | `image("../images/image.jpg", ...)` |
| GIF | `image("../images/image.gif", ...)` |
| SVG | `image("../images/image.svg", ...)` — requires `svg` extension |
| AVIF | `image("../images/image.avif", ...)` |
| WebP | `image("../images/image.webp", ...)` |

## Properties

| Property | Description | Example |
|----------|-------------|---------|
| (positional) | path/URL | `image("images/logo.png")` |
| `size` | display size | `size: (50, 50)` |
| `at` | anchor | `at: center` |
| `position` | absolute position | `position: (10, 10)` |

## Example: Grid of Formats

```tson
grid(4, 4, columns-weight: [1, 3]) {
    image("../../../images/image.png", size: (50, 50), at: center)
    image("../../../images/image.jpg", size: (50, 50), at: center)
    image("../../../images/image.gif", size: (50, 50), at: center)
    image("../../../images/image.svg", size: (50, 50), at: center)
    image("../../../images/image.avif", size: (50, 50), at: center)
    image("../../../images/image.webp", size: (50, 50), at: center)
}
```

## Notes

- Paths are relative to the current file (the doc-slides use `../../../images/...`).
- Animated GIFs animate on screen; PDF captures a static frame.
- SVG/AVIF/WebP support requires the corresponding image extension modules (`svg` for SVG).