# Introduction

## What is ntexup?

ntexup is an open-source, text-based document and presentation generator. It creates professional, math-heavy, and reusable teaching materials from simple text files.

Documents are written in `.ntx` files using [TSON syntax](04-tson-syntax.md) — a versatile, human-readable data format that is a strict superset of JSON. ntexup compiles these files into rendered pages that can be displayed on screen, exported to PDF, or published as HTML.

## Why ntexup?

| Tool            | Limitation                                                                 |
|-----------------|----------------------------------------------------------------------------|
| **PowerPoint**  | Manual, slow for math equations, not version-control friendly              |
| **LaTeX**       | Overkill for simple slides, complex setup, dependency issues               |
| **Asciidoctor** | Ruby environment problems, limited equation rendering, no built-in figures |

ntexup solves these problems by providing:

- **Text-based, declarative** — focus on content, not layout
- **Math-friendly** — built-in LaTeX equation rendering
- **Diagram-ready** — PlantUML, plot2d, shapes, 3D scenes
- **Template-driven** — reusable components, themes, and styles
- **Git-friendly** — plain text files, full version control support
- **Extensible** — Java plugin system for custom elements

## Use Cases

- **Teaching materials** — modular slides, reusable chapters
- **Scientific presentations** — math equations, diagrams, plots
- **Technical documentation** — PDF and web-ready output
- **Version-controlled content** — ideal for Git workflows

## Key Features

- TSON-based syntax; ntexup layers functions, loops, conditions, and variables on top of the data format
- CSS-like theming and styling
- Multi-file support with `include`, including remote GitHub files
- 2D and 3D shapes, charts, diagrams
- LaTeX math equations
- Source code syntax highlighting
- Multiple output formats: PDF, HTML, Swing viewer
- Extensible via Java plugins

## Related Projects

| Project                                                          | Description                   |
|------------------------------------------------------------------|-------------------------------|
| [ntexup](https://github.com/thevpc/ntexup)                       | Core tool and runtime         |
| [ntexup-templates](https://github.com/thevpc/ntexup-templates)   | Reusable themes and templates |
| [ntexup-doc-slides](https://github.com/thevpc/ntexup-doc-slides) | Documentation slide deck      |
| [TSON](https://github.com/thevpc/tson)                           | TSON syntax specification     |
| [Nuts](https://github.com/thevpc/nuts)                           | Package manager               |
