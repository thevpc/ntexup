# Task: Redesign the Goto dialog of the ntexup screen viewer into a slide navigator

## Goal

The Goto dialog is currently a bare "enter a number" dialog. Replace it with a **single unified "Go to slide" dialog** that lists slides with their index and title, lets the user filter by typing, and still supports the fast "type a number and press Enter" path.

This work is independent from, but must be compatible with, the light/dark theme work (shared themed, undecorated dialog base class, shared scrollbar styling). If that base class already exists in the codebase, build on it. If not, build this dialog so it can be moved onto it trivially: no hardcoded colors, colors read from `UIManager`/theme tokens at paint time.

## Before writing code: investigate

Do not assume structure. First:

1. Find the current Goto dialog and how it triggers navigation (which API moves the viewer to slide N, 0-based or 1-based, what happens when out of range).
2. Find how slides are modeled in the screen viewer: what is "a slide" (component, page, node?), how the total count and order are obtained, and whether the structure is flat or hierarchical (sections, sub-slides, animation steps that should NOT count as separate slides).
3. **Determine how slide titles can be resolved.** I don't know if titles are resolvable. Look for, in this order:
    - an explicit title property on the slide component (e.g. a `title` property on slide/agenda-slide style components),
    - a heading/title child node in the slide body,
    - section/chapter titles the slide belongs to,
    - anything else the model offers.
      Report what you found and which slide types have/lack titles.
4. Check whether titles are available without rendering (static property resolution) or only after layout/render. Prefer static resolution; never render slides just to get a title.
5. Summarize findings and the title-resolution strategy in a short plan before changing anything.

## Design (decided, implement this unless investigation shows it's impossible)

One dialog, one input field, one list. No separate "number mode" vs "list mode".

**Layout**
- Search-style text field at the top, list of slides below.
- Each row: index (right-aligned, fixed width) + title. Show the section name subtly if slides are grouped and it helps.
- The row for the current slide is marked and is selected/scrolled into view when the dialog opens.
- With an empty field, the full list is shown.

**Input behavior**
- **Digits only** (e.g. `12`): treat as a slide number. Select/scroll to slide 12 in the list. Rows whose index starts with the typed number are shown first or highlighted; Enter goes to the selected row. If the number is out of range, don't navigate (show a subtle invalid state, no modal error).
- **Anything else**: case-insensitive filter on title (substring match; optionally accent-insensitive, since content is often French/English/Arabic). Matching text may be highlighted in rows.
- **Mixed/ambiguous** (a title that contains digits, like "Chapter 2"): a row is shown if its index starts with the typed digits OR its title contains the typed text. An exact index match is ranked first. This is what makes typing a number and filtering by title coexist without a mode switch.
- Keyboard: Up/Down move the list selection while focus stays in the field; Enter navigates to the selected row; Escape closes; double-click on a row navigates; PageUp/PageDown scroll the list.
- Selecting the first match automatically after each keystroke, so "type number, Enter" and "type a few letters, Enter" are both one-handed fast paths.
- Optional, only if trivial in the current architecture: live preview of the highlighted slide behind the dialog, reverting on Escape. Skip it if it's not simple.

**Untitled slides**
- Never leave a blank row. Fallback chain: explicit title, then first heading/text of the slide, then section title + "(untitled)", then `Slide N`. Make the fallback chain explicit in code and unit-testable.
- Filtering must run against the resolved display title, and untitled slides must remain reachable by number.

## Implementation notes

- Introduce a small `SlideTitleResolver`/index (name it per project conventions) that produces `(index, title, section?)` entries for the document. Build it once per document load, cache it, and invalidate when the document reloads/changes. Resolution must not block the EDT for large documents (lazy or background if it's expensive; most likely it isn't).
- Keep title resolution decoupled from the dialog so the **Search dialog and other features can reuse it later**. Do not wire it into Search now.
- Filtering must be cheap per keystroke for hundreds of slides (no re-resolving titles on every keystroke).
- Use a `JList`/`JTable` with a custom renderer (or the project's existing list component), with the same themed scrollbar as the rest of the viewer. No hardcoded colors.
- The dialog should be undecorated and themed, and follow the same Escape/focus/modality conventions as the other viewer dialogs.
- Everything Swing on the EDT. Follow the project's code style, package layout, and naming. Keep changes scoped to the viewer.
- Respect 0-based vs 1-based consistently: the UI shows what the user sees in the viewer's own slide counter.

## Deliverables

1. The implementation, replacing the old number-only dialog (keep the same shortcut/menu entry that opens Goto today).
2. A short summary: how titles are resolved, which slide types have no titles and what fallback they get, and any slide types or hierarchies that needed special handling (animation steps, nested slides, sections).
3. Unit tests for the filter/rank logic (digits only, text only, mixed like "2", out-of-range, untitled slides, accent/case insensitivity) and for the title fallback chain.
4. A manual test checklist: open with many slides, type a number, type a title fragment, type digits that match both an index and a title, Up/Down/Enter/Escape, double-click, out-of-range number, current-slide marking, light and dark themes.
5. Anything you couldn't resolve cleanly, and why.