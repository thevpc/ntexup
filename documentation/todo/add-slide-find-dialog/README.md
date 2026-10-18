# Feature Request: Theme-Coherent Slide Find / Search Dialog with CTRL-F, Slide Jumping, Multi-Search Highlighting & Reset

## 1. Overview & Goal

Add an in-viewer search, highlight, and navigation feature ("Find in Slides") accessible via the **`CTRL-F`** keyboard shortcut (and HUD / context menu) in the screen viewer (`ntexup show`).

When invoked, it displays a **frameless, custom-styled popup / dialog** (undecorated, no default OS window header) that:
1. Is visually coherent with the presentation theme (translucent modern overlay, rounded corners, clean slate / accent styling).
2. Provides an instant search input field and an interactive result list showing matching slides (with page number, slide title, matched snippet, and source file name).
3. Supports text search, math/LaTeX dialect recognition, and semantic / approximate matching.
4. Allows jumping directly to any matching slide (via Enter, Click, or Arrow Keys).
5. **Highlights matched occurrences directly on the rendered slide canvas** with distinct colors for multiple searches.
6. **Resets / clears all active highlights upon pressing `ESC`** (when the search dialog is closed).

---

## 2. Requirements & UI/UX Specification

### A. Invocation, Dismissal & Highlight Reset
- **Shortcut**: `Ctrl+F` (or `Cmd+F` on macOS).
- **Secondary access**:
  - Search button (`🔍`) in `PresentationHud` alongside slide navigation and PDF export.
  - "Find..." item in `DocumentPopupMenu`.
- **Dismissal & ESC Behavior**:
  - When the Find dialog is open: pressing `Escape` closes the dialog while retaining the active search highlights on the slides.
  - When the Find dialog is closed and highlights are active: pressing `Escape` **clears and resets all active highlights**.
  - Clicking outside the dialog area dismisses the dialog (keeping active highlights).

### B. In-Slide Text Highlighting & Multi-Search Colors
- **On-Canvas Highlight Overlay**:
  - Matched search terms are highlighted on the rendered slide view using an overlay layer (`SearchHighlightLayer` implementing `NTxDocumentLayer`).
  - Text positions / bounds are tracked or derived from text rendering nodes so highlight rectangles wrap around matching words / math expressions with semi-transparent tinted backgrounds.
- **Multiple Search Queries / Distinct Colors**:
  - Supports adding multiple concurrent search filters / terms (e.g., query 1 = `"microstrip"`, query 2 = `"patch"`).
  - Each distinct search term is assigned a unique palette color (e.g., Palette: Soft Amber `#f59e0b40`, Emerald Cyan `#10b98140`, Coral Pink `#f43f5e40`, Violet `#8b5cf640`, Sky Blue `#0ea5e940`).
  - Highlights from multiple search queries can co-exist on the same slide simultaneously.
  - The dialog displays active search chips/tags with their corresponding highlight color swatch and a remove (`✕`) button.

### C. Visual Design & Theme Coherence
- **Frameless Window**: Use an undecorated `JDialog` or in-frame layered popup (`JLayeredPane.POPUP_LAYER` over `DocumentView`).
- **Styling**:
  - **No OS window header / title bar** (undecorated).
  - Background: Dark translucent modern glass (e.g. `rgba(15, 23, 42, 0.92)` / Slate 900) matching `PresentationHud` or theme-adaptive neutral surface.
  - Border: Subtle rounded border (12–16px corner radius) with 1px border stroke (`rgba(255, 255, 255, 0.15)`).
  - Input field: Clean borderless search bar with search icon (`🔍`), active query chips, placeholder text (`"Search slides, text, equations..."`), clear button (`✕`), and custom caret/selection colors.
  - Result items:
    - Display slide index: `Slide <N> of <Total>`
    - Display slide title / section kicker
    - Text snippet with search term highlight in the corresponding query color
    - Source file path / basename (e.g. `0041-antenna-patch.ntx`)
    - Hover & active selection states with accent color highlight.

### D. Search Capabilities
1. **Full-Text Search**:
   - Extract searchable text from all slide nodes (`text`, `title`, `source`, `card`, `ul/ol`, `kicker`, `button`, etc.).
   - Extract metadata: slide titles, notes, source file names, section headers.
2. **LaTeX / Math Dialect Support**:
   - Recognize math blocks (`eq(...)`, `latex(...)`, `$..$`, `\frac`, `\alpha`, `\lambda`, etc.).
   - Support searching both raw LaTeX markup (e.g. `\lambda_0`, `\epsilon_r`) and normalized mathematical terms (e.g. `lambda`, `eps`, `epsilon`).
3. **Semantic / Fuzzy Matching**:
   - Case-insensitive substring matching.
   - Word tokenization and multi-keyword search (AND / OR).
   - Diacritic / accent-insensitive matching (e.g. `schema` matches `schéma`).
   - Prefix and fuzzy distance matching (e.g., Levenshtein / Damerau-Levenshtein threshold or n-gram ranking).

### E. Navigation & Jump Behavior
- **Keyboard navigation**:
  - `Down` / `Up` arrow keys navigate results list.
  - `Enter` jumps to the selected slide and closes the dialog (keeping highlights on the slide).
  - Typing in search updates results and canvas highlights in real time (instant filtering / debounced ~100ms).
- **Mouse navigation**:
  - Clicking a result immediately navigates `documentView.showPage(selectedPage)` and closes the dialog.

---

## 3. Architecture & Technical Design

### A. Document Text Indexing (`NTxSlideIndex`)
Create a lightweight search index built or updated when the document is compiled/loaded:
```java
public class NTxSlideIndex {
    public static class SlideEntry {
        public int pageIndex;
        public String title;
        public String sourceFileName;
        public String rawText;
        public List<String> mathExpressions;
        public List<TextOccurrence> occurrences; // text + bounding boxes for highlight
    }

    public List<SlideSearchResult> search(String query);
}
```
- Traverses `NTxCompiledDocument` / `NTxCompiledPage` and extracts:
  - Slide title (`title` property on `PAGE` / `content-slide`).
  - Text from `NTxNode` tree recursively (handling `TEXT`, `SOURCE`, `EQ`, custom components).
  - Source file path from `NTxSource` / `compilerDeclarationPath`.

### B. Highlighting Manager (`SlideHighlightManager`) & Layer (`SearchHighlightLayer`)
- **`SlideHighlightManager`**:
  - Stores active search terms and their assigned color:
    ```java
    public class HighlightQuery {
        private String term;
        private Color highlightColor;
    }
    ```
  - Predefined multi-color palette for successive queries:
    1. Gold/Amber: `rgba(245, 158, 11, 0.40)`
    2. Emerald: `rgba(16, 185, 129, 0.40)`
    3. Cyan: `rgba(6, 182, 212, 0.40)`
    4. Rose/Pink: `rgba(244, 63, 94, 0.40)`
    5. Purple: `rgba(139, 92, 246, 0.40)`
  - Methods: `addQuery(String)`, `removeQuery(String)`, `clearAll()`, `hasActiveHighlights()`.
- **`SearchHighlightLayer`** (`NTxDocumentLayer`):
  - Added to `DocumentViewContentPanel.layers`.
  - When `SlideHighlightManager.hasActiveHighlights()`, renders translucent rounded highlight rectangles over matched word bounds on `currentShowingPage`.

### C. Theme-Coherent Custom Dialog (`SlideSearchDialog`)
- Built using Swing with custom painting (`paintComponent`), without default Swing Look-and-Feel decoration:
  ```java
  public class SlideSearchDialog extends JDialog {
      public SlideSearchDialog(JFrame owner, DocumentView documentView) {
          super(owner, false);
          setUndecorated(true);
          setBackground(new Color(0, 0, 0, 0)); // Transparent window for rounded corners
          ...
      }
  }
  ```
- Uses custom rendered `JList` or custom scrollable cards with theme-matching background and typography.

### D. Key Event Wiring
In `DocumentView.prepareContentPane()`:
```java
contentPane.addKeyListener(new KeyAdapter() {
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_F) {
            openFindDialog();
            e.consume();
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (highlightManager.hasActiveHighlights()) {
                highlightManager.clearAll();
                contentPane.repaint();
                e.consume();
                return;
            }
            if (isFullScreen) {
                setFullScreen(false);
            }
        }
        ...
    }
});
```

---

## 4. Key Files to Modify / Create

| File / Component | Action | Role |
|---|---|---|
| `ntexup-renderer-screen/.../DocumentView.java` | Modify | Bind `Ctrl+F` and `ESC` highlight reset, manage search dialog lifecycle. |
| `ntexup-renderer-screen/.../DocumentViewContentPanel.java` | Modify | Register `SearchHighlightLayer` in layers list. |
| `ntexup-renderer-screen/.../components/PresentationHud.java` | Modify | Add search button (`🔍`) in HUD. |
| `ntexup-renderer-screen/.../DocumentPopupMenu.java` | Modify | Add "Find... (Ctrl+F)" action. |
| `ntexup-renderer-screen/.../search/SlideSearchDialog.java` | **Create** | Undecorated, theme-coherent floating search dialog. |
| `ntexup-renderer-screen/.../search/NTxSlideSearcher.java` | **Create** | Search engine extracting text, math, titles, and performing matching. |
| `ntexup-renderer-screen/.../search/SlideHighlightManager.java` | **Create** | Manages multi-search queries and assigned highlight colors. |
| `ntexup-renderer-screen/.../search/SearchHighlightLayer.java` | **Create** | `NTxDocumentLayer` that paints colored highlight boxes on slide canvas. |
| `ntexup-renderer-screen/.../search/SlideSearchResultRenderer.java` | **Create** | Custom cell/card renderer displaying slide number, title, highlights, and source. |

---

## 5. Verification Plan

1. **Viewer Smoke Test**:
   - Run `nuts ntexup show ntexup-doc-slides/` or any test presentation.
   - Press `Ctrl+F`.
   - Verify that the frameless, dark translucent search dialog appears centered near the top of the viewer.
2. **Text Search & Highlight Test**:
   - Type `"patch"`.
   - Matching slides are listed in the popup and occurrences on the visible slide are highlighted with Color #1 (e.g. Amber).
3. **Multi-Search Test**:
   - Add a second query (e.g. `"feed"`).
   - Verify that occurrences of `"feed"` are highlighted in Color #2 (e.g. Emerald), while `"patch"` stays highlighted in Amber.
4. **LaTeX / Math Dialect Test**:
   - Type math query like `\lambda`, `lambda`, `epsilon`, `fr`.
   - Search must be intelligent to understand that `\lambda` and  `λ` are comparable (and other symbols)
   - Verify slides containing equations or math notation are found and highlighted.
5. **Jump Test**:
   - Use Arrow Down + Enter or click a result.
   - Verify the viewer immediately transitions to that slide, the dialog closes, and highlights remain visible on the jumped slide.
6. **Reset / Clear Test**:
   - With highlights visible and dialog closed, press `ESC`.
   - Verify all highlights vanish immediately.
