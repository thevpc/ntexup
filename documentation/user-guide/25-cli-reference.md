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
| `show` | Render a document/folder in the Swing viewer |
| `show-doc` | Render the bundled documentation deck |
| `view-doc` | Interactive documentation browser |
| `list-templates` | List available templates |
| `pdf` | Render to PDF |
| `html` | Render to HTML |
| `--install-syntax=<editor>` | Install syntax highlighting (vim, kate, intellij, gedit, vscode, jedit, notepad-plus-plus) |
| `--help` | Show help |

## `new`

```bash
nuts ntexup new -t=classic-medium
nuts ntexup new --template=classic
nuts ntexup new --template=classic-small --show
nuts ntexup new --template=classic --show-doc
```

Supported `-t/--template` values (theme-size):

- `classic-small`, `classic-medium`, `classic-large`
- `ibtihel-small`, `ibtihel-medium`, `ibtihel-large`
- `eniso-*` (if available)

## `show`

```bash
nuts ntexup show .                     # current folder
nuts ntexup show path/to/project
nuts ntexup show slides.ntx            # single file
```

Opens the live rendering viewer that refreshes on file change.

## `show-doc` / `view-doc`

```bash
nuts ntexup show-doc
nuts ntexup view-doc
```

Shows the full documentation slides prepared by the project authors.

## `pdf`

```bash
nuts ntexup pdf . -o out.pdf
```

## `html`

```bash
nuts ntexup html . -o out/
```

## `list-templates`

```bash
nuts ntexup list-templates
```

Prints template IDs and their source URLs.

## Editor syntax

```bash
nuts ntexup --install-syntax=vim
nuts ntexup --install-syntax=kate
nuts ntexup --install-syntax=intellij
nuts ntexup --install-syntax=gedit
nuts ntexup --install-syntax=vscode
nuts ntexup --install-syntax=jedit
nuts ntexup --install-syntax=notepad-plus-plus
```

Editor support files live under the main project at `documentation/integration/ntx-support/<editor>/`.

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