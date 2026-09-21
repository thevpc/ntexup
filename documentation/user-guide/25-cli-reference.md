# CLI Reference

Common ntexup commands, executed through the Nuts launcher.

## Invocation

```bash
nuts ntexup <command> [options]
```

`nuts -y` auto-confirms prompts (useful in scripts/containers).

## Commands

| Command | Description |
|---------|-------------|
| `new` | Create a new project from a template |
| `show` (alias `open`) | Render a document/folder in the Swing viewer |
| `html` | Render a document/folder to HTML in the browser |
| `documentation` | Render the bundled documentation deck |
| `documentation-pdf` | Generate the bundled documentation deck to PDF/images |
| `reopen` | Re-open the last viewed project in the viewer |
| `build-repo` | Build a local template repository |
| `list-templates` | List available templates |
| `pdf` | Render to PDF |
| `image` | Render to a set of images (PNG per page by default) |
| `dump` | Dump the parsed document model to the console |
| `install-editor-syntax` | Install syntax highlighting for editors |
| `--gui` | Force GUI mode |
| `--help` | Show help |

With no command at all, ntexup opens the viewer on the current directory.

## `new`

```bash
nuts ntexup new -t=classic-medium
nuts ntexup new --template=classic
nuts ntexup new --template=classic-small --show
nuts ntexup new --template=classic --documentation
```

Supported `-t/--template` values (theme-size):

- `classic-small`, `classic-medium`, `classic-large`
- `ibtihel-small`, `ibtihel-medium`, `ibtihel-large`
- `eniso-*` (if available)

Additional options:

- `--show` — open the viewer after creation
- `documentation` — open the documentation deck after creation
- `pdf[=<out>]` — render the new project to PDF
- `documentation-pdf[=<out>]` — render the documentation deck to PDF

## `show`

```bash
nuts ntexup show .                     # current folder
nuts ntexup show path/to/project
nuts ntexup show slides.ntx            # single file
```

Opens the live rendering viewer that refreshes on file change.

## `html`

```bash
nuts ntexup html .                # open current folder in the browser
nuts ntexup html path/to/slides.ntx
```

## `documentation`

```bash
nuts ntexup documentation
```

Shows the full documentation slides prepared by the project authors.

## `documentation-pdf`

```bash
nuts ntexup documentation-pdf -o out.pdf
```

Generates the documentation deck (same options as `pdf`/`image`).

## `reopen`

```bash
nuts ntexup reopen
```

Re-opens the most recently viewed project in the viewer.

## `build-repo`

```bash
nuts ntexup build-repo path/to/templates
```

Builds a local template repository from a folder of templates.

## `pdf` / `html` / `image`

```bash
nuts ntexup pdf . -o out.pdf            # render to PDF
nuts ntexup image . -o target/img       # render every page to an image
nuts ntexup image . -o page.png --pages=2-5   # render a page range
nuts ntexup pdf . -o out.pdf --page-size=a4
```

Common options:

| Option | Effect |
|--------|--------|
| `-o` / `--output <out>` | Output file (or folder if it ends with `/`) |
| `-p` / `--pages <range>` | Page range, e.g. `2-5`, `3,5,8-10`, `all` |
| `--dpi <n>` | Image resolution |
| `--type` / `--format <fmt>` | Image format (png, jpg, ...) |
| `--size <WxH>` | Page size in pixels |
| `--page-size <name>` | Named page size: `a0..a6`, `letter`, `legal`, `ledger`, `executive`, `folio`, `statement` |
| `--page-width` / `--page-height <n>` | Page size per dimension |
| `--grid <colsxrows>` | Split one page into a grid of images |
| `--margin[-top|bottom|left|right] <n>` | Page margins |
| `--landscape` / `--portrait` | Page orientation |
| `--show-page-number` | Draw page numbers |
| `--var-<name>=<value>` | Override a document variable |
| `--dump` | Dump the parsed model instead of rendering |

Global options (any command): `--git-provider=<jgit|system>` and `--prefer-system-git`

- `jgit` (**default**) — clone/pull `github://` includes with the embedded JGit implementation; no system `git` required.
- `system` — prefer the native `git` executable; falls back to JGit with a warning when `git` is not installed.

The same preference can be set without the flag via the `ntexup.git.provider` system property or the `NTEXUP_GIT_PROVIDER` environment variable.

## `dump`

```bash
nuts ntexup dump .                     # dump the document model
```

Prints the parsed document structure (useful for debugging).

## `install-editor-syntax`

```bash
nuts ntexup install-editor-syntax=all
nuts ntexup install-editor-syntax=vim
nuts ntexup install-editor-syntax=vscode,jedit
nuts ntexup install-editor-syntax all
```

> Note: this is a **subcommand**, the old `--install-syntax=` option no longer
> exists.

Supported editors: `vim`, `kate`, `intellij`, `gedit`, `vscode`, `jedit`,
`notepad-plus-plus` (`all` installs every family).

Editor support files live under the main project at
`documentation/integration/ntx-support/<editor>/`.

## Environment Variables

| Variable | Effect |
|----------|--------|
| `NUTS_CONTAINER_VERBOSE=1` | verbose bootstrap output in container |

## Exit Codes

Standard Nuts exit codes apply (0 = success). Warnings during first-run (template repository clones, caches) do not affect the exit status.

## Global Nuts options

See `nuts --help`. Useful ones:

- `-y` — assume yes
- `-c` — clean
- `--verbose` — verbose output

## Developer / Test Notes

The repo includes `test-commands.md` and `todo.md` with maintainer scratch notes (not user documentation).
