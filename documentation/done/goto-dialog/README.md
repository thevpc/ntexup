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
---

# Implementation notes (completed)

## Findings from the investigation

1. **Two Goto entry points, both bare number prompts**: the context menu item
   (`DocumentPopupMenu` → `JOptionPane.showInputDialog("Page Number?")`) and the
   HUD slide counter (`PresentationHud`). Both parsed an int and called
   `documentView.showPage(v - 1)`.
2. **`DocumentView.showPage(int)` is 0-based** and clamps out of range
   (`index >= count → count-1`, `index < 0 → 0`). The viewer's own counter
   (`getPageUserIndex()`) is 1-based, so the UI shows `pageIndex + 1`.
3. **A slide is an `NTxCompiledPage`** (`NTxNode` tree); the viewer keeps a flat
   `PageView` list. Total = `getPagesCount()`. There are no sub-slides or
   animation steps to exclude; `page-group` is a parser-level aggregator, not a
   separate viewer slide.
4. **Titles are resolvable statically, without rendering.** Verified against the
   41-page `ntexup-doc-slides` deck by loading it and walking
   `page.compiledPage()`:
    - the `title` **property is never on the page root** — the themes
      (`content-slide`, `agenda-slide`, `thankyou-slide`, …) consume the
      `title` parameter and interpolate it into a `text(...)` node;
    - a node with style class `title` exists only on the cover;
    - the **first `text` node resolves 41/41 pages, and is the true title for
      40/41** — this is the workhorse of the fallback chain.

## Title-resolution strategy

`SlideTitleResolver` extracts three candidates from the compiled node tree
(one walk, no render):

| Priority | Source | Present in practice |
|---|---|---|
| 1 | explicit `title` property on the page root | almost never |
| 2 | text of the first node with style class `title` | cover slides |
| 3 | text of the first `text` node | nearly all slides |
| 4 | `section + " (untitled)"` | only if 1–3 are all empty |
| 5 | `Slide N` (N = the viewer's 1-based number) | last resort, never blank |

`section` is derived from the source path's immediate parent folder, stripped of
any ordering prefix (`.../0040-elements/0010-text/0110-plain.ntx` → `text`),
falling back to the file stem. Sections are only displayed per row when the deck
spans more than one section (`NTxSlideTitleIndex.isGrouped()`), so single-folder
decks stay uncluttered.

Slide types that have no title of their own (e.g. a slide built purely from a
`text`-less component) still get a stable label via the fallback chain and stay
reachable by number.

## Filter / rank

`SlideGotoFilter` implements the single-field model:

- **empty** → the full list in document order (current slide selected);
- **digits only** → match on the 1-based index: exact match ranked first, then
  index-prefix matches, then title matches; if the number matches no index at all
  the result is flagged `outOfRange` (subtle invalid underline, no modal, Enter
  does nothing);
- **anything else** → case- and diacritic-insensitive substring match on the
  resolved title, reusing the shared `NTxHighlights.normalize` so it stays
  consistent with Find;
- **mixed** (e.g. `2` matching both index 2 and a slide titled "Chapter 2") →
  index matches ranked first, title-only matches after.

Filtering is O(n) string work per keystroke over pre-resolved titles — no title
re-resolution, no debounce needed.

## Files

| File | Action |
|---|---|
| `.../screen/navigation/SlideTitleResolver.java` | **Create** — extraction + pure fallback chain + section derivation |
| `.../screen/navigation/NTxSlideTitleIndex.java` | **Create** — cached `(pageIndex, title, section)` outline |
| `.../screen/navigation/SlideGotoFilter.java` | **Create** — pure filter/rank |
| `.../screen/navigation/SlideGotoDialog.java` | **Create** — frameless navigator (mirrors `SlideSearchDialog`) |
| `.../screen/navigation/SlideGotoListRenderer.java` | **Create** — index + title + section row renderer |
| `.../screen/DocumentView.java` | Modify — outline cache, `openGotoDialog()`, Ctrl+G/Cmd+G, ESC, reload invalidation, dialog-click guard |
| `.../screen/DocumentPopupMenu.java` | Modify — "Goto Page..." → "Go to Slide... (Ctrl+G)" |
| `.../screen/components/PresentationHud.java` | Modify — page-counter click opens the navigator |
| `renderers/ntexup-renderer-screen/pom.xml` | Modify — JUnit 5 test deps |
| `src/test/java/.../navigation/*Test.java` | **Create** — 23 unit tests |

The outline is built once per document load on `DocumentView.ASYNC_LOADER` (the
same single-thread executor as reloads and the Find index, so they serialize),
cached, and invalidated in the reload path. It is deliberately decoupled from the
dialog (and from Find) so other features can reuse it later.

## Tests

`SlideTitleResolverTest` (13) — extraction from real `DefaultNTxNode` trees and
the full fallback chain; `SlideGotoFilterTest` (10) — blank/digits/exact/prefix/
out-of-range/zero/text/case/diacritic/null. `mvn -o -pl
renderers/ntexup-renderer-screen test` → **23 tests, 0 failures**. The whole
outline was also validated end-to-end against the 41-page `ntexup-doc-slides`
deck (all titles resolved, sections meaningful, `grouped=true`).

## Manual test checklist

1. Open a many-slide deck (`nuts ntexup show ntexup-doc-slides/`), press **Ctrl+G**
   (or right-click → *Go to Slide...*, or click the HUD slide counter).
2. Empty field shows every slide; the current slide's row is marked (`▸`, accent
   number) and scrolled into view.
3. Type a number (`12`) → slide 12 selected first; **Enter** jumps and closes.
4. Type a title fragment (`shape`, `diag`) → matching slides listed; **Enter**
   jumps.
5. Type digits matching both an index and a title (`2`) → index match first.
6. **↑/↓** move the selection while focus stays in the field; **Enter** jumps;
   **Esc** closes; **PageUp/PageDown** scroll the list; **double-click** a row
   jumps.
7. Type an out-of-range number (`99`) → subtle invalid underline, no modal, Enter
   is a no-op; clear the field to recover.
8. Move to another slide, reopen → the new current slide is marked/selected.
9. Click on the deck while the dialog is open → it dismisses without also
   navigating the deck.
10. Reload (`F5`) with the dialog open → it re-indexes and refreshes.
11. Accent-insensitive: `schema` finds `Schéma`.
12. Light/dark: **not yet applicable** — see below.

## Unresolved / deferred

- **Theming.** The task asked for no hardcoded colors, but per the agreed plan
  this dialog deliberately mirrors the current Find dialog's dark, frameless
  look (colors are centralized as named constants in `SlideGotoDialog` /
  `SlideGotoListRenderer` for a one-place swap). The light/dark LAF work
  (`todo/ligh-dark-laf`) will fold both dialogs onto a shared themed base and a
  themed scrollbar. The Goto dialog currently uses the default `JScrollPane`
  scrollbar, same as Find.
- **Title quality** depends on the theme putting the title in the first `text`
  node of the page. A theme that renders a kicker/breadcrumb as the first text
  would mislabel slides; today's `classic` theme does not. This is the same
  assumption the Find feature already makes.
- **Live preview** behind the dialog was skipped, as allowed: `showPage` is not
  trivially reversible on Escape without extra state.
- The outline resolves all pages up-front (forcing the lazy page compile that the
  viewer already performs at load), rather than lazily per visible row — cheap
  for hundreds of slides and consistent with the Find index.
