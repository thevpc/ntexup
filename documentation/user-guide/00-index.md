# ntexup User Guide

ntexup is an open-source, text-based document and presentation generator for academics, researchers, and technical communicators. It combines Markdown-like simplicity with LaTeX math, template-driven generation, and version-control-friendly `.ntx` files.

**Technology stack:** Java 17 · Nuts package manager · TSON syntax

---

## Table of Contents

### Getting Started
1. [Introduction](01-introduction.md) — What is ntexup and why use it
2. [Installation](02-installation.md) — Install Nuts, ntexup, and dependencies
3. [Quick Start](03-quick-start.md) — Create and render your first document

### Core Concepts
4. [TSON Syntax](04-tson-syntax.md) — The data format behind `.ntx` files (ntexup's document language is layered on it)
5. [Document Structure](05-document-structure.md) — Project layout, `main.ntx`, and file organization
6. [Pages and Layout](06-pages-layout.md) — Page types, orientation, and layout containers

### Content Elements
7. [Text and Typography](07-text-typography.md) — Text, fonts, alignment, rich text
8. [Lists](08-lists.md) — Ordered and unordered lists
9. [Images and Media](09-images-media.md) — PNG, JPEG, SVG, animated GIF
10. [Equations (LaTeX)](10-equations-latex.md) — Mathematical expressions with LaTeX
11. [Source Code](11-source-code.md) — Syntax-highlighted code blocks
12. [Shapes (2D)](12-shapes-2d.md) — Rectangles, circles, polygons, and more
13. [Lines and Arrows](13-lines-arrows.md) — Lines, polylines, arcs, curves, arrows
14. [3D Scenes](14-scene-3d.md) — 3D primitives, camera, transforms
15. [Plots and Charts](15-plots-charts.md) — Plot2D, functions, curves
16. [PlantUML Diagrams](16-plantuml-diagrams.md) — UML, Gantt, network, wireframes

### Styling and Theming
17. [Styling System](17-styling-system.md) — CSS-like styles, property reference
18. [Templates and Themes](18-templates-themes.md) — Using and creating themes

### Advanced Features
19. [Components and Reuse](19-components-reuse.md) — `@define`, templates, reusable components
20. [Control Flow](20-control-flow.md) — Variables, loops, conditionals, functions
21. [Multi-file Projects](21-multi-file-includes.md) — `include`, modular documents
22. [Positioning and Geometry](22-positioning-geometry.md) — `at`, `origin`, coordinates, units

### Output and Tooling
23. [Rendering and Output](23-rendering-output.md) — PDF, HTML, screen viewer, web
24. [Editor Integration](24-editor-integration.md) — Syntax highlighting for editors
25. [CLI Reference](25-cli-reference.md) — Command-line options and commands

### Reference
26. [Property Reference](26-property-reference.md) — Complete list of all style properties
27. [Node Type Reference](27-node-type-reference.md) — All element types
28. [Extension Development](28-extension-development.md) — Building custom extensions
29. [Troubleshooting](29-troubleshooting.md) — Common issues and fixes
30. [Glossary](30-glossary.md) — Terminology
