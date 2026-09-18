# Installation

## Prerequisites

ntexup has **no strict prerequisites**. Everything below is optional / nice-to-have:

- **Git** — used by ntexup to fetch GitHub-hosted templates and includes. Recommended, but not required: remote template support may move to an embedded JGit dependency in the future, and local (offline) projects never need Git.
- **JDK 17** — convenient to have pre-installed, but not required: the Nuts runtime runs on any Java and downloads a suitable JDK 17 itself if none is available.

## Step 1: Install Nuts Package Manager

Nuts is the package manager ntexup uses to manage dependencies, extensions, and runtimes.

```bash
curl -s https://thevpc.net/nuts/install-latest.sh | bash
```

**About Java:** the Nuts bootstrap requires *some* Java virtual machine (any recent JDK/JRE). If no compatible runtime is found, Nuts automatically downloads a JDK 17 for you. You only need a pre-installed JDK 17 if you want to skip that step.

Restart your terminal for the `nuts` command to become available.

## Step 2: Install ntexup

```bash
nuts -y install ntexup
```

This downloads and installs the latest stable version of ntexup and its dependencies.

## Step 3: Verify the Installation

Render the built-in documentation to confirm everything works:

```bash
nuts ntexup documentation
```

This compiles and displays the full ntexup documentation slide deck.

---

## Imports and Extensions

Extensions are loaded with `import(...)`:

```tson
import("common-functions", "shapes2d", "plantuml")
```

`import` accepts a **dependency coordinate (GAV)** — `groupId:artifactId:version` — or a shorthand. The shorthand is resolved with these rules (from `DefaultNTxEngine`):

1. **Missing groupId** → defaults to `net.thevpc.ntexup`.
2. **artifactId without the `ntexup-extension-` prefix** → the prefix is added.
3. **Missing version** → defaults to ntexup's own current version.

```tson
import("common-functions")
// resolves to  net.thevpc.ntexup:ntexup-extension-common-functions:<version>
```

The resolved coordinate is then downloaded at runtime by ntexup **through the Nuts package manager** (via its dynamic classloader) in the current workspace.

> For reproducibility, prefer the full coordinate. Artifacts are versioned; pinning them archives your document against future changes:

```tson
import("net.thevpc.ntexup:ntexup-extension-common-functions:1.0.0.0")
import("net.thevpc.ntexup:ntexup-extension-shapes2d:1.0.0.0")
```

Common extension names: `common-functions` (color functions), `shapes2d`, `shapes3d`, `plantuml`, `plot2d`, `latex`, `svg` (SVG rasterization), `animated-gif` (animated GIFs), `presenters`.

> Building ntexup itself from source requires Maven + JDK 17 and is covered in the **developer documentation**, not this guide.

## IDE Support

For syntax highlighting in your editor, run:

```bash
nuts ntexup install-editor-syntax=vim
nuts ntexup install-editor-syntax=vscode
nuts ntexup install-editor-syntax=kate
nuts ntexup install-editor-syntax=intellij
```

You can select only the editors you use. Supported editors: `vim`, `vscode`, `gedit`, `kate`, `intellij`, `jedit`, `notepad-plus-plus`.

## Docker / Containerized Setup

ntexup can run inside a container (e.g., Cloud IDEs like Gitpod):

```bash
docker run -it --rm eclipse-temurin:17-jre bash -c "$(curl -sSL https://thevpc.net/nuts/bootstrap-container-latest.sh)"
```

Then inside the container:

```bash
nuts -y ntexup <arguments>
```

Only PDF generation works in a headless container — the Swing viewer needs a display manager. Git must be present for GitHub-hosted templates.

---

## Common Commands After Installation

| Command | Description |
|---------|-------------|
| `nuts ntexup new -t=classic` | Create a new project from the classic template |
| `nuts ntexup list-templates` | List all available templates |
| `nuts ntexup show .` | Open the current folder in the viewer |
| `nuts ntexup documentation` | Render the documentation slide deck |
| `nuts ntexup documentation-pdf -o out.pdf` | Generate the documentation deck |
| `nuts ntexup html .` | Render the current folder to HTML in a browser |
| `nuts ntexup install-editor-syntax=<editor>` | Install editor syntax highlighting |
