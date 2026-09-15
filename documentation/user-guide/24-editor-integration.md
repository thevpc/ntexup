# Editor Integration

ntexup ships syntax highlighting for common editors under
`ntexup/documentation/integration/ntx-support/`.

## Supported Editors

Repository folders (in the main ntexup project):

```text
documentation/integration/ntx-support/
├── gedit/            language.lang + readme.md
├── intellij/         language.xml  + readme.md
├── jedit/            language.xml
├── kate/             language.xml  + readme.md
├── notepad-plus-plus/ language.xml
├── vim/              ftdetect + syntax .vim + readme.md
└── vscode/           language-configuration.json
                      language.tmLanguage.json + readme.md
```

Language name: **NTexUp** — files `*.ntx`.

## Auto Install

From the main ntexup project directory:

```bash
nuts ntexup --install-syntax=vim
nuts ntexup --install-syntax=vscode
nuts ntexup --install-syntax=gedit
nuts ntexup --install-syntax=kate
nuts ntexup --install-syntax=intellij
```

Run only for the editor(s) you use.

## Manual Install

### Vim / Neovim

```text
put under $HOME/.vim/syntax/ntexup.vim
put under $HOME/.vim/ftdetect/ntexup.vim
```

### VS Code

Manual install is awkward — prefer `--install-syntax=vscode`.

### Other editors

Each `ntx-support/<editor>/` folder contains the language definition files and a `readme.md` with instructions.

## What Gets Highlighted

TSON keywords, strings, numbers, comments, and structure of `.ntx` files.

## Cloud IDEs

Works in Gitpod / GitHub Codespaces — run the install command inside the workspace:

```bash
nuts ntexup --install-syntax=vscode
```

## Related

- [Syntax](04-tson-syntax.md)
- [Node Reference](27-node-type-reference.md)