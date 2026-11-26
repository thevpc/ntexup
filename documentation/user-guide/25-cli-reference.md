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
| `image` | Render to a set of images (PNG per page by default, or N-up sheets) |
| `check` | Render every page in memory to surface errors; writes nothing |
| `dump` | Dump the parsed document model to the console |
| `install-editor-syntax` | Install syntax highlighting for editors |
| `--gui` | Force GUI mode |
| `--help` | Show help |

With no command at all, ntexup opens the viewer on the current directory.

## `new`

```bash
nuts ntexup new -t=classic:medium
nuts ntexup new --template=classic
nuts ntexup new --template=classic:small --show
nuts ntexup new --template=classic --documentation
```

Supported `-t/--template` values — a **template** name optionally followed by a **layout** (the project file structure: `small` = single file, `medium` = sections as files, `large` = sections as subfolders):

- `classic:small`, `classic:medium`, `classic:large`
- `ibtihel:small`, `ibtihel:medium`, `ibtihel:large`
- `eniso:small`, `eniso:medium`, `eniso:large`
- `meridian:small`, `meridian:medium`, `meridian:large`
- a bare template name (e.g. `classic`) resolves to the recommended layout (`classic:medium`)
- a `-` is accepted as an alias separator (`classic-medium` = `classic:medium`); a plain number or `#number` picks by index in `list-templates`; a path (any value with `/` or `\`) is used verbatim

**What `new` does:** creates a new project folder (the current directory by default) from the template. `--show` / `--documentation` are **post-creation actions that add windows to the viewer** — they do not replace the creation:

- `--show` — open the newly created project in the viewer after creation
- `--documentation` — also open the ntexup documentation deck in the viewer
- `--pdf[=<out>]` — render the new project to PDF instead of showing it
- `--documentation-pdf[=<out>]` — render the documentation deck to PDF

So `nuts ntexup new -t=classic --show --documentation` creates `./main.ntx` (plus `pages/`), then opens two viewer windows: the new project and the documentation deck.

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
nuts ntexup pdf . -o out.pdf            # vector PDF (default)
nuts ntexup pdf . -o out.pdf --pdf-mode=raster --dpi=150
nuts ntexup image . -o target/img       # render every page to an image
nuts ntexup image . -o page.png --pages=2-5   # render a page range
nuts ntexup pdf . -o out.pdf --page-size=a4
```

Common options:

| Option | Effect |
|--------|--------|
| `-o` / `--output <out>` | Output file (or folder if it ends with `/`) |
| `-p` / `--pages <range>` | Page range, e.g. `2-5`, `3,5,8-10`, `all` |
| `--dpi <n>` | Image resolution; for PDF, raster resolution (a positive value selects raster mode unless `--pdf-mode` is explicit) |
| `--pdf-mode <vector\|raster>` | PDF encoding (default `vector`); aliases `native` and `image` |
| `--pdf-vector` / `--pdf-raster` | Select the PDF encoding explicitly |
| `--type` / `--format <fmt>` | Image format (png, jpg, ...) |
| `--size <WxH>` | Page size in pixels |
| `--page-size <name>` | Named page size: `a0..a6`, `letter`, `legal`, `ledger`, `executive`, `folio`, `statement` |
| `--page-width` / `--page-height <n>` | Page size per dimension |
| `--grid <colsxrows>` | Pages per output sheet: N-up pdf sheets, or `sheet-NN.png` images |
| `--page-labels` | (image) caption each page with `page N · file.ntx` (the file that produced it) |
| `--strict` | Exit 1 on warnings too (default: only on errors) |
| `--diagnostics <file>` | Write the error/warning report as json |
| `--margin[-top|bottom|left|right] <n>` | Page margins |
| `--landscape` / `--portrait` | Page orientation |
| `--show-page-number` | Draw page numbers |
| `--var-<name>=<value>` | Override a document variable |
| `--dump` | Dump the parsed model instead of rendering |

Global options (any command): `--git-provider=<jgit|system>` and `--prefer-system-git`

- `jgit` (**default**) — clone/pull `github://` includes with the embedded JGit implementation; no system `git` required.
- `system` — prefer the native `git` executable; falls back to JGit with a warning when `git` is not installed.

The same preference can be set without the flag via the `ntexup.git.provider` system property or the `NTEXUP_GIT_PROVIDER` environment variable.

## `check`

```bash
nuts ntexup check .                    # validate every page, write nothing
nuts ntexup check . --strict --diagnostics=target/diag.json
```

Renders every page in memory at low resolution (so render-time errors are caught too,
not only syntax errors) and reports. Same `--pages`, `--strict` and `--diagnostics` options
as `pdf`/`image`.

## Diagnostics and exit code

`pdf`, `image`, `html-*`, `ntxp` and `check` end with a summary:

```
ntexup: 2 error(s), 0 warning(s)
  error   [main.ntx] syntax error : missing rbrace, in page{ul{¶ a ¶ b } }...
```

and exit with code **1 when errors were logged** (with `--strict`, also when warnings were).
The engine keeps rendering after most errors, so the output file may exist even when the
command fails. `--diagnostics=FILE` writes the same report as json:

```json
{"status": "errors", "errorCount": 2, "warningCount": 0, "outputs": [],
 "diagnostics": [{"level": "error", "source": "main.ntx", "message": "..."}]}
```

## Images for review (N-up sheets)

```bash
nuts ntexup image --grid=4x2 --page-labels -o target/review/ .   # sheet-01.png = pages 1-8
```

Lays pages out 4 per row, 2 rows per image, each captioned with its page number and source
file. Sheets are capped at 2400px wide unless `--dpi` is given. With `--page-labels` alone
(no grid) each page image gets its caption.

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

0 = success; 1 = errors were logged (or warnings, with `--strict`), see *Diagnostics and exit code*. First-run messages (template repository clones, caches) are informational and do not affect the exit status.

## Global Nuts options

See `nuts --help`. Useful ones:

- `-y` — assume yes
- `-c` — clean
- `--verbose` — verbose output

## Developer / Test Notes

The repo includes `test-commands.md` and `todo.md` with maintainer scratch notes (not user documentation).
