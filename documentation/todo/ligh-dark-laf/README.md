# Task: Light/Dark look-and-feel for the ntexup screen viewer

## Goal

Add a coherent **light / dark theme** to the ntexup screen viewer (the Swing-based presentation/document viewer). Every Swing surface the viewer owns must follow the active theme: the main viewer chrome, the context menu, and all dialogs (PDF export, Goto, Search, and any other dialog you find). Also fix the ugly scrollbar in the Search dialog.

## Before writing code: investigate

Do not assume structure. First:

1. Locate the screen viewer module and list every Swing top-level and popup it creates: `JFrame`, `JDialog`, `JWindow`, `JPopupMenu`, `JOptionPane`, `JFileChooser`, tooltips, custom-painted panels.
2. Identify how the Search, Goto and PDF dialogs are built today (look-and-feel, decorations, colors, fonts, scroll panes).
3. Check how colors are currently set (hardcoded `Color` constants, `UIManager` keys, custom `paintComponent`). Hardcoded colors are the main thing that will break theming.
4. Check the Java version and the existing dependency/module setup (Maven, JPMS descriptors if any) before adding dependencies.
5. Summarize your findings in a short plan before changing anything.

## Requirements

### 1. Theme support
- Two themes: **light** and **dark**, with a single source of truth (a small `Theme`/`ViewerTheme` abstraction or equivalent) that the whole viewer reads from.
- Switchable at runtime (menu entry and/or shortcut, plus a way to set the initial theme programmatically/CLI/config, whichever fits the existing conventions). Switching must update all open windows live, without restart (`SwingUtilities.updateComponentTreeUI` or the LAF library's equivalent on every open window).
- Persist the choice if the viewer already has a preferences mechanism; otherwise don't invent a new one, just expose the setting cleanly.

### 2. Everything follows the theme
- Main viewer window and its toolbars/status bar/panels.
- The **context menu** and all `JPopupMenu`/`JMenu` items, separators, accelerators, icons.
- **PDF, Goto, Search dialogs** and any other dialog discovered in step 1.
- Tooltips, scrollbars, text fields, combo boxes, buttons, checkboxes, tables/lists if present.
- No component may keep a hardcoded light-only (or dark-only) color. Replace hardcoded colors with `UIManager` keys or theme tokens. Custom-painted components must read colors from the theme at paint time (not cache them at construction), so live switching works.
- Rendered **document content** (slides/pages) is a separate concern from UI chrome: do not recolor the document itself unless it already has a notion of viewer background. State clearly in your summary what you did there.

### 2.b GoTo
Change Goto to be able to list slides (show indexed titles, donnow if titles are resolvable, and how)

### 3. Dialogs must look the same as the main viewer
- The dialogs should be **headerless / undecorated** (no native OS title bar) so they match the viewer's styling instead of showing a native light title bar over a dark UI. I'm not sure whether "headless" in my notes meant undecorated windows or something else: treat undecorated, theme-styled dialogs as the intent, and verify against how the viewer's main window is decorated.
- If undecorated, you must still provide: a themed title/drag area, a close affordance, Escape to close, correct focus behavior, and sane behavior on multi-monitor/HiDPI setups. Don't regress keyboard handling or modality.
- Prefer one shared base class/factory for these dialogs so theming, decoration and Escape/close behavior live in one place instead of being copy-pasted three times.

### 4. Search dialog scrollbar
- The current scroll pane/scrollbar in the Search dialog is ugly and ignores the UI design. Fix it so it matches the theme (thin, themed track/thumb, themed hover/pressed states, no default Metal-looking arrows).
- Apply the same scrollbar styling to every other scroll pane in the viewer, not only Search, so it's consistent.

### 5. FlatLaf: evaluate, then decide
- I'm open to using **FlatLaf** (FlatLightLaf / FlatDarkLaf), but it's not mandatory. Evaluate it against a hand-rolled `UIManager`-based approach and pick one with a short justification (dependency weight, module/JPMS compatibility, scrollbar/popup/dialog styling coverage, runtime theme switching via `FlatLaf.setup`/`updateUI`).
- If FlatLaf is chosen: use its built-in light/dark themes as the base, customize through `UIManager` defaults/`FlatLaf.registerCustomDefaultsSource` rather than patching components, and make sure custom-painted components still work with it.
- If not: implement the themes via `UIManager` defaults on top of the current LAF, and explain why FlatLaf was rejected.
- Either way, the Search dialog should end up consistent with the rest, whichever approach "we did in the search dialog" originally used. If the Search dialog currently has its own bespoke styling, fold it into the shared theme mechanism rather than keeping a second styling path.

## Constraints

- Keep changes scoped to the screen viewer; don't refactor unrelated modules.
- Follow the project's existing code style, package layout, and naming.
- No behavior regressions: existing shortcuts, search/goto/export flows, and rendering performance must stay as they are.
- Everything Swing-related must run on the EDT.
- Don't add dependencies without stating them and why.

## Deliverables

1. The implementation.
2. A short summary: what was found in the investigation, the FlatLaf decision, the list of windows/components now themed, and anything intentionally left out (e.g. document content colors).
3. A manual test checklist I can run: toggle light/dark with every dialog open and closed, context menu in both themes, Search dialog with many results (scrollbar), HiDPI, Escape/focus behavior on the undecorated dialogs.
4. Note any component you found that could not be themed cleanly and why.