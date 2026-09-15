# Rendering and Output

ntexup can render documents to multiple output formats.

## Output Formats

| Format | Renderer | Command |
|--------|----------|---------|
| On-screen (Swing) | `screen` | `nuts ntexup show <file>` |
| PDF | `pdf` | `nuts ntexup pdf ...` |
| HTML | `html` | `nuts ntexup html ...` |
| Web (browser) | `web` | `ntexup-viewer-backend` |
| Image (PNG) | image renderer | rendered internally for stubs |

## The Screen Viewer

### Show a Project

```bash
nuts ntexup show .
```

### Show a Single File

```bash
nuts ntexup show slides.ntx
```

### Viewer Features

- Live auto-refresh on file change
- Page navigation (arrow keys / mouse)
- Fullscreen
- Display source file and page index (HUD)

## PDF Generation

```bash
nuts ntexup pdf . -o output.pdf
```

### PDF Options

| Option | Effect |
|--------|--------|
| `-o FILE` | output file |
| `--page-size` | `A4`, `A3`, `LETTER`, ... |
| `--orientation` | `landscape` / `portrait` |
| `--margin` | page margins |
| `--n-up` | pages per sheet (`1`, `2`, `4`, ...) |

> In a headless container, only PDF rendering works (no viewer).

## HTML Generation

```bash
nuts ntexup html . -o out/
```

The HTML renderer:

- Emits `.html` files into a directory or a single zip
- Embeds rendered page images
- Suitable for static hosting

## Image Outputs

Each page can be rendered to an image. This is used internally for PDF/HTML, and can also be invoked programmatically:

```java
BufferedImage img = engine.renderImage(compiledPage, config);
```

## Web Viewer (Experimental)

The `ntexup-viewer-backend` is a Spring Boot application exposing a REST API + Git service for browsing repositories of `.ntx` files.

- `RepositoryController` — serves document content/pages
- `GitService` — clones and updates github-hosted documents

There is also an Angular frontend module (`ntexup-viewer-frontend-ng`).

## Command Line Usage

### List templates

```bash
nuts ntexup list-templates
```

### Create a new project

```bash
nuts ntexup new -t=classic-medium
nuts ntexup new --template=classic --show
```

### Open documentation

```bash
nuts ntexup show-doc
nuts ntexup view-doc
```

### Install editor syntax

```bash
nuts ntexup --install-syntax=vim
nuts ntexup --install-syntax=vscode
```

## References

- [Renderer modules](../ntexup/renderers/) — source for pdf, html, screen, web
- [CLI Reference](25-cli-reference.md)

## Notes and Limitations

- Animated GIFs animate on-screen only; PDF captures a static frame.
- SVG renders via the `svg` extension (`Salamander` rasterizer).
- 3D scenes render with an orthographic projection; quality settings live in the provided shapes3d extension.
- Rendering a very large document grid (many pages per sheet / big N-up) increases memory usage.